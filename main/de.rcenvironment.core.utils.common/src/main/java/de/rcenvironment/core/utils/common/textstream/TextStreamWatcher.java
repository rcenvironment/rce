/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.textstream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.TeeInputStream;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * A utility class to read and forward line-based text data from an {@link InputStream}. As it requires an {@link AsyncTaskService} to run,
 * an instance must be provided via the constructor. If available in the project scope, consider using
 * de.rcenvironment.core.toolkitbridge.transitional.TextStreamWatcherFactory for convenience.
 * 
 * TODO consider reworking this class to a plain {@link Runnable} to avoid this dependency, which would also allow using this class with a
 * plain Java {@link Executor}.
 * 
 * TODO add more specific unit tests? currently covered indirectly by executor tests
 * 
 * @author Robert Mischke
 */
public class TextStreamWatcher {

    private final TextOutputReceiver[] receivers;

    private InputStream inputStream;

    private BufferedReader bufferedReader;

    /**
     * An optional file where the input stream is mirrored to.
     */
    private File logFile;

    private volatile Future<?> watcherTaskFuture;

    private volatile boolean streamClosed = false;

    private volatile FileOutputStream logFileStream;

    private final AsyncTaskService asyncTaskService;

    /**
     * The actual {@link Runnable} that performs the output capture. Uses the outer class fields for delegation.
     * 
     * @author Robert Mischke
     */
    private final class WatcherRunnable implements Runnable {

        @Override
        @TaskDescription("Text stream watching/reading")
        public void run() {
            for (TextOutputReceiver receiver : receivers) {
                receiver.onStart();
            }
            String line;
            try {
                while ((line = bufferedReader.readLine()) != null) {
                    for (TextOutputReceiver receiver : receivers) {
                        receiver.addOutput(line);
                    }
                }
                for (TextOutputReceiver receiver : receivers) {
                    receiver.onFinished();
                }
            } catch (IOException e) {
                for (TextOutputReceiver receiver : receivers) {
                    try {
                        receiver.onFatalError(e);
                    } catch (RuntimeException e1) {
                        // catch this to make sure the other "onException" handlers are called
                        LogFactory.getLog(getClass()).error("Exception in onException() callback", e1);
                    }
                }
            }

            IOUtils.closeQuietly(bufferedReader);
            streamClosed = true;
        }
    }

    /**
     * Creates an instance (which is not started automatically; see {@link #start()}).
     * 
     * @param input the {@link InputStream} to read from
     * @param receivers the {@link TextOutputReceiver} to send the generated events to
     */
    public TextStreamWatcher(InputStream input, AsyncTaskService asyncTaskService, TextOutputReceiver... receivers) {
        this.receivers = receivers;
        this.inputStream = input;
        this.asyncTaskService = asyncTaskService;

        // guard against "null" listeners
        for (TextOutputReceiver r : receivers) {
            if (r == null) {
                throw new IllegalArgumentException("A 'null' receiver was passed as an argument");
            }
        }
    }

    /**
     * Defines that the raw output should be mirrored to the given file. The target file will be created, lines appended or overwritten if
     * it already exists. This method is meant to be called no more than once; repeated calls will cause a {@link IllegalStateException}.
     * 
     * @param file the target file to write to
     * @param append append lines to file if true; otherwise overwrite file if already exists
     * 
     * @throws IOException on I/O errors while setting up the log file
     */
    public void enableLogFile(File file, boolean append) throws IOException {
        if (watcherTaskFuture != null) {
            throw new IllegalStateException("Already started");
        }
        // sanity check
        if (logFile != null) {
            throw new IllegalStateException("Log file was already defined");
        }
        logFile = file;
        logFileStream = new FileOutputStream(logFile, append);
        // true = auto close stream
        inputStream = new TeeInputStream(inputStream, logFileStream, true);
    }

    /**
     * Returns the file set by {@link #enableLogFile(File)}, or null if none was set.
     * 
     * @return the defined log file
     */
    public File getLogFile() {
        return logFile;
    }

    /**
     * Starts the watcher thread.
     * 
     * @return the "self" instance for convenient chaining
     */
    public TextStreamWatcher start() {
        this.bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        watcherTaskFuture = asyncTaskService.submit(new WatcherRunnable());
        return this;
    }

    /**
     * Blocks until the stream watcher thread has terminated.
     * 
     */
    public void waitForTermination() {
        try {
            watcherTaskFuture.get();
        } catch (InterruptedException e) {
            LogFactory.getLog(getClass()).debug("Interrupted while waiting for stream watcher task to finish");
        } catch (ExecutionException e) {
            LogFactory.getLog(getClass()).warn("Exception while waiting for stream watcher task to finish", e);
        }

        if (logFileStream != null) {
            IOUtils.closeQuietly(logFileStream);
        }
    }

    /**
     * Interrupts the stream watcher thread.
     */
    public void cancel() {
        if (watcherTaskFuture == null) {
            throw new IllegalStateException("Watcher task was not started yet");
        }
        watcherTaskFuture.cancel(true);
    }

    /**
     * Returns whether the watched output stream has ended, either by reaching EOF or because an exception has occurred.
     * 
     * @return true if the watched stream has ended
     */
    public boolean isStreamClosed() {
        return streamClosed;
    }
}
