/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.execution.api.ConsoleRow.Type;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.common.textstream.TextStreamWatcher;

/**
 * Convenient methods for workflow console usage.
 * 
 * @author Doreen Seider
 */
public final class ConsoleRowUtils {

    private static final Log LOGGER = LogFactory.getLog(ConsoleRowUtils.class);

    private ConsoleRowUtils() {}

    /**
     * Send text to workflow console. Note that if you use this method with a
     * {@link LocalApacheCommandLineExecutor} it must be called *after* the executor was started.
     * After that, the returned {@link TextStreamWatcher} must be stored and *after* the
     * waitForTermination() method of the executor, the waitForTermination() from the watcher must
     * be called.
     * 
     * @param componentContext componentContext {@link ComponentContext} of the calling component
     * @param inputStream contains text to send
     * @param consoleType stderr or stdour
     * @param logFile optional file to log to as well, <code>null</code> for no file logging
     * @param append optional flag only considered if logFile not null. Append lines to file if
     *        true; otherwise overwrite file
     * @return the created {@link TextStreamWatcher} for calling the waitForTermination method
     */
    public static TextStreamWatcher logToWorkflowConsole(final ComponentContext componentContext, final InputStream inputStream,
        final Type consoleType, final File logFile, boolean append) {

        
        /**
         * Sends each console line to the workflow console.
         * 
         * @author Doreen Seider
         */
        class WorkflowConsoleOutputReceiver implements TextOutputReceiver {

            @Override
            public void onStart() {}

            @Override
            public void addOutput(String line) {
                componentContext.printConsoleLine(line, consoleType);
            }

            @Override
            public void onFinished() {}

            @Override
            public void onFatalError(Exception e) {}

        }

        TextStreamWatcher watcher = new TextStreamWatcher(inputStream, new WorkflowConsoleOutputReceiver());
        
        if (logFile != null) {
            try {
                watcher.enableLogFile(logFile, append);
            } catch (IOException e) {
                LOGGER.error("setting up log file failed: " + logFile.getAbsolutePath(), e);
            }
        }
        watcher.start();
        return watcher;
    }

}
