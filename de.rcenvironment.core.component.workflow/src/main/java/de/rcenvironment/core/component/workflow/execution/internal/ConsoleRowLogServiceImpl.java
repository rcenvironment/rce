/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.workflow.execution.api.ConsoleRowLogService;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Default {@link ConsoleRowLogService} implementation.
 * 
 * @author Robert Mischke
 */
public class ConsoleRowLogServiceImpl implements ConsoleRowLogService {

    /**
     * The number of characters that the log buffer may accumulate before a warning message is logged. Added to check whether background
     * buffering with a low-priority writer thread consumes too much memory in long-running, high-CPU-load workflows.
     */
    private static final int BUFFERED_CHARACTER_COUNT_WARNING_THRESHOLD = 2 * 1024 * 1024; // arbitrary

    // only needs to be defined to enable; no specific value needed
    private static final String SYSTEM_PROPERTY_FOR_ACTIVATION = "rce.internal.writeCombinedConsoleLogs";

    private LinkedBlockingQueue<ConsoleRow> outputQueue;

    private Writer fileWriter;

    private AtomicInteger bufferedCharacterCount = new AtomicInteger();

    private volatile BackgroundLogWriterTask backgroundWriterTask;

    private volatile Future<?> backgroundTaskFuture;

    private ConfigurationService configurationService;

    private File logFile;

    private boolean enabled = false;

    private final Log log = LogFactory.getLog(getClass());

    /**
     * A background task to write log output to a file. The executing thread's priority is set to the given value, and reset after logging
     * has finished.
     * 
     * @author Robert Mischke
     */
    // TODO rework to reusable stand-alone class? - misc_ro
    private final class BackgroundLogWriterTask implements Runnable {

        private final int threadPriority;

        private final Writer writer;

        private boolean anythingLogged;

        private BackgroundLogWriterTask(Writer writer, int threadPriority) {
            this.writer = writer;
            this.threadPriority = threadPriority;
        }

        @Override
        @TaskDescription("Background log writing")
        public void run() {
            final Thread currentThread = Thread.currentThread();
            final int originalPriority = currentThread.getPriority();
            currentThread.setPriority(threadPriority);
            try {
                runLogging();
            } finally {
                // reset to previous value
                currentThread.setPriority(originalPriority);
            }
        };

        private void runLogging() {
            final Thread currentThread = Thread.currentThread();
            final ConsoleRowFormatter consoleRowFormatter = new ConsoleRowFormatter();
            anythingLogged = false;
            try {
                while (!currentThread.isInterrupted()) {
                    ConsoleRow row = outputQueue.take();
                    // subtract length of contained test string
                    modifyCharacterCount(-row.getPayload().length());
                    try {
                        // TODO add an explicit flush mechanism to ensure rows are on disk after a
                        // given time?
                        writer.append(consoleRowFormatter.toCombinedLogFileFormat(row));
                        anythingLogged = true;
                    } catch (IOException e) {
                        log.error(e);
                        break;
                    }
                }
            } catch (InterruptedException e) {
                log.debug("Background log writer interrupted");
            }
            try {
                writer.close();
                // TODO bad encapsulation; improve when reworking this class
                if (!anythingLogged) {
                    logFile.delete();
                }
            } catch (IOException e) {
                log.error(e);
            }
        }
    }

    /**
     * Constructor.
     * 
     * @param filename the filename to log to
     * @param autoCloseOnShutdown if true, a shutdown hook will be registered that calls {@link #close()}
     * @throws IOException if creating the log file failed
     */
    public ConsoleRowLogServiceImpl() throws IOException {
        outputQueue = new LinkedBlockingQueue<ConsoleRow>();
    }

    /**
     * OSGi-DS lifecycle method. Starts background logging of accumulated log lines.
     */
    public void activate() {
        enabled = (System.getProperty(SYSTEM_PROPERTY_FOR_ACTIVATION) != null);
        if (!enabled) {
            log.debug("Combined workflow console log is disabled");
            return;
        }

        // TODO improve filename, locking/uniqueness etc.
        String logFileName = StringUtils.format("console.combined.%d.log", System.currentTimeMillis());
        File outputDir = configurationService.getConfigurablePath(ConfigurablePathId.PROFILE_OUTPUT);
        logFile = new File(outputDir, logFileName);
        try {
            fileWriter = new BufferedWriter(new FileWriter(logFile));
            // TODO use the thread pool instead?
            backgroundWriterTask = new BackgroundLogWriterTask(fileWriter, Thread.MIN_PRIORITY);
            backgroundTaskFuture =
                ConcurrencyUtils.getAsyncTaskService().submit(backgroundWriterTask, "Common ConsoleRow log " + logFile.getAbsolutePath());
            log.debug("Logging combined workflow console output to " + logFileName + " (NOTE: may not capture all output yet)"); // TODO 5.0
        } catch (IOException e) {
            log.error("Failed to set up background console logging to " + logFileName, e);
        }
    }

    /**
     * OSGi-DS lifecycle method. Stops logging and closes the output file.
     * 
     * Note: Closing the log file may happen asynchronously.
     */
    public void deactivate() {
        if (!enabled) {
            return;
        }
        if (backgroundTaskFuture != null) {
            backgroundTaskFuture.cancel(true);
        }
    }

    /**
     * Enqueues a {@link ConsoleRow} to log. This method is thread-safe.
     * 
     * @param row the {@link ConsoleRow} to log
     */
    public void append(ConsoleRow row) {
        // add the length of contained payload; note that this is not an exact measure of log output to write
        modifyCharacterCount(row.getPayload().length());
        outputQueue.add(row);
    }

    /**
     * Modifies the counter that keeps track of how many characters are stored in the background buffer. Also checks against the defined
     * size limit.
     * 
     * @param delta the "delta" to add to the counter; may be negative to decrement the counter
     */
    private void modifyCharacterCount(int delta) {
        int newTotal = bufferedCharacterCount.addAndGet(delta);
        if (delta > 0) {
            if (newTotal >= BUFFERED_CHARACTER_COUNT_WARNING_THRESHOLD) {
                log.warn(StringUtils.format("Background log buffer has grown to %d characters", newTotal));
            }
        } else {
            // consistency check
            if (newTotal < 0) {
                log.error("Integrity violation: buffer count decremented below zero");
            }
        }
    }

    /**
     * Enqueues {@link ConsoleRow} entries to log. This method is thread-safe, although there is no guarantee that lists of
     * {@link ConsoleRow}s passed by concurrent calls are appended as uninterrupted sequences.
     * 
     * @param rows the {@link ConsoleRow}s to log
     */
    @Override
    public void processConsoleRows(List<ConsoleRow> rows) {
        if (!enabled) {
            return;
        }
        // add total string length to counter
        int charCount = 0;
        for (ConsoleRow row : rows) {
            charCount += row.getPayload().length();
        }
        modifyCharacterCount(charCount);
        // add to buffer
        outputQueue.addAll(rows);
    }

    /**
     * OSGi-DS injection method.
     * 
     * @param newInstance the new service instance
     */
    public void bindConfigurationService(ConfigurationService newInstance) {
        this.configurationService = newInstance;
    }

}
