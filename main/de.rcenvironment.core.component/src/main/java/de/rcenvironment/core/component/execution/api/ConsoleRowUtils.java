/*
 * Copyright (C) 2006-2016 DLR, Germany
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

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.execution.api.ConsoleRow.Type;
import de.rcenvironment.core.toolkitbridge.transitional.TextStreamWatcherFactory;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.common.textstream.TextStreamWatcher;

/**
 * Convenient methods for workflow console usage.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 * 
 * Note: The console row handling evolved over time and that can be seen in the code. Functionality related to console rows are kind
 * of patched together.
 * Functionalities are:
 * - sending console rows to the workflow controller
 * - fetching console rows by the GUI (workflow console view), waiting for them at the time the component terminates as they might be
 * delayed due to batching them when sending
 * - storing them in the data management bundle together in a file per component run
 * 
 * --seid_do
 * 
 */
public final class ConsoleRowUtils {

    private static final Log LOGGER = LogFactory.getLog(ConsoleRowUtils.class);

    private ConsoleRowUtils() {}

    /**
     * Send text to workflow console. Note that if you use this method with a {@link LocalApacheCommandLineExecutor} it must be called
     * *after* the executor was started. After that, the returned {@link TextStreamWatcher} must be stored and *after* the
     * waitForTermination() method of the executor, the waitForTermination() from the watcher must be called.
     * 
     * @param componentLog {@link ComponentLog} instance of the calling component
     * @param inputStream contains text to send
     * @param consoleType stderr or stdour
     * @param logFile optional file to log to as well, <code>null</code> for no file logging
     * @param append optional flag only considered if logFile not null. Append lines to file if true; otherwise overwrite file
     * @return the created {@link TextStreamWatcher} for calling the waitForTermination method
     */
    public static TextStreamWatcher logToWorkflowConsole(final ComponentLog componentLog, final InputStream inputStream,
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
                switch (consoleType) {
                case TOOL_OUT:
                    componentLog.toolStdout(line);
                    break;
                case TOOL_ERROR:
                    componentLog.toolStderr(line);
                    break;
                default:
                    throw new IllegalArgumentException("Console row type not supported: " + consoleType);
                }
            }

            @Override
            public void onFinished() {}

            @Override
            public void onFatalError(Exception e) {}

        }

        TextStreamWatcher watcher = TextStreamWatcherFactory.create(inputStream, new WorkflowConsoleOutputReceiver());

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

    /**
     * Central method to define/assemble ConsoleRow-related notification ids.
     * 
     * @param nodeIdString common part: a node id
     * @param specificIdString context-specific part/id; usually (always?) the workflow id
     * @return the assembled notification id
     */
    public static String composeConsoleNotificationId(LogicalNodeId nodeIdString, String specificIdString) {
        return StringUtils.format(ConsoleRow.NOTIFICATION_ID_PREFIX_CONSOLE_EVENT + "%s:%s", nodeIdString.getInstanceNodeIdString(),
            specificIdString);
    }

}
