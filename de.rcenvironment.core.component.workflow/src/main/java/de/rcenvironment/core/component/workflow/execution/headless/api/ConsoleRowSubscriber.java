/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.headless.api;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.spi.CallbackMethod;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.execution.headless.internal.ExtendedHeadlessWorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.internal.ConsoleRowFormatter;
import de.rcenvironment.core.notification.DefaultNotificationSubscriber;
import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.notification.NotificationSubscriber;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Subscriber for ConsoleRow events, including workflow life-cycle events.
 * 
 * Note: this subscriber should work for remote subscription now, but this was no tested yet
 * 
 * @author Sascha Zur
 * @author Robert Mischke
 * @author Doreen Seider
 */
public class ConsoleRowSubscriber extends DefaultNotificationSubscriber implements Closeable {

    private static final String LOG_FILE_SUFFIX = ".log";

    private static final String TEMP_FILE_SUFFIX = ".tmp";

    private static final long serialVersionUID = 1233786794388388297L;

    private final transient File finalLogFileDestination;

    private final transient File tempLogFileLocation;

    private final transient ExtendedHeadlessWorkflowExecutionContext workflowExecutionContext;

    private final transient Log log = LogFactory.getLog(getClass());

    private final transient ConsoleRowFormatter consoleRowFormatter = new ConsoleRowFormatter();

    private transient BufferedWriter bufferedLogWriter;

    public ConsoleRowSubscriber(ExtendedHeadlessWorkflowExecutionContext context, File logDirectory) {
        this.workflowExecutionContext = context;
        String consoleLogName = "workflow";
        finalLogFileDestination = new File(logDirectory, consoleLogName + LOG_FILE_SUFFIX);
        if (finalLogFileDestination.exists()) {
            log.warn("Log file does already exists; overwriting: " + finalLogFileDestination.getAbsolutePath());
            finalLogFileDestination.delete();
            if (finalLogFileDestination.exists()) {
                log.warn("Failed to delete existing log file " + finalLogFileDestination.getAbsolutePath());
            }
        }
        tempLogFileLocation = new File(logDirectory, consoleLogName + LOG_FILE_SUFFIX + TEMP_FILE_SUFFIX);
        if (!tempLogFileLocation.exists()) {
            try {
                if (!tempLogFileLocation.createNewFile()) {
                    throw new IOException("createNewFile() returned false");
                }
            } catch (IOException e) {
                Logger.getAnonymousLogger().warning("Could not create " + tempLogFileLocation.getAbsolutePath());
            }
        }
        bufferedLogWriter = setupLogWriter(tempLogFileLocation);
    }

    @Override
    public Class<?> getInterface() {
        return NotificationSubscriber.class;
    }

    /**
     * Inserts a single line to the generated log file, which will be automatically terminated with the platform-specific line separator.
     * Typically used to insert meta information in the log file. Note that currently, no error is generated if log file writing is not
     * enabled at all, ie the writer is null (for any reason).
     * 
     * @param content the text data to insert
     */
    public void insertMetaInformation(String content) {
        synchronized (this) {
            if (bufferedLogWriter != null) {
                try {
                    bufferedLogWriter.write(consoleRowFormatter.toMetaInformationLine(content));
                } catch (IOException e) {
                    log.error("Error inserting this text line into the workflow log: " + content);
                }
            }
        }
    }

    @Override
    @CallbackMethod
    public void processNotification(Notification n) {
        // String notificationId = n.getHeader().getNotificationIdentifier();
        // log.debug("Received console row notification, id=" + notificationId);

        Serializable body = n.getBody();
        if (!(body instanceof ConsoleRow)) {
            log.error("Unexpected notification type on ConsoleRow channel: body class is " + body.getClass());
            return;
        }
        ConsoleRow row = (ConsoleRow) body;

        // TODO this synchronizes for each single notification and blocks the caller; improve
        // for performance - misc_ro
        synchronized (this) {
            if (bufferedLogWriter != null) {
                try {
                    // bufferedLogWriter.write((notificationId + ": " + body) +
                    bufferedLogWriter.write(consoleRowFormatter.toSingleWorkflowLogFileFormat(row));
                } catch (IOException e) {
                    log.error("Closing log file " + tempLogFileLocation
                        + " after error; the file will not be moved to its final location", e);
                    IOUtils.closeQuietly(bufferedLogWriter);
                    bufferedLogWriter = null;
                }
            } else {
                if (row.getType() == ConsoleRow.Type.LIFE_CYCLE_EVENT
                    && (row.getPayload().equals(StringUtils.escapeAndConcat(
                        ConsoleRow.WorkflowLifecyleEventType.NEW_STATE.name(), WorkflowState.DISPOSING.name()))
                        || row.getPayload().equals(StringUtils.escapeAndConcat(
                            ConsoleRow.WorkflowLifecyleEventType.NEW_STATE.name(), WorkflowState.DISPOSED.name())))) {
                    return;
                }
                // should not occur
                log.warn("Workflow log writer is already closed; ignoring event " + row.getType() + ":" + row.getPayload());
            }
        }
        if (row.getType() == ConsoleRow.Type.LIFE_CYCLE_EVENT) {
            log.debug("Received workflow life-cycle event: " + row.getPayload());
            if (ConsoleRow.WorkflowLifecyleEventType.WORKFLOW_FINISHED.name().equals(row.getPayload())) {
                workflowExecutionContext.reportConsoleOutputTerminated();
            }
        }

        workflowExecutionContext.reportConsoleRowReceived(row);
    }

    @Override
    public void close() {
        synchronized (this) {
            if (bufferedLogWriter != null) {
                log.debug("Closing temporary log file " + tempLogFileLocation);
                try {
                    bufferedLogWriter.close();
                    if (!finalLogFileDestination.isFile() && tempLogFileLocation.renameTo(finalLogFileDestination)
                        && finalLogFileDestination.isFile()) {
                        log.debug("Completed workflow log file " + finalLogFileDestination);
                    } else {
                        log.warn("Failed to move log file " + tempLogFileLocation + " to final destination " + finalLogFileDestination);
                    }
                } catch (IOException e) {
                    log.warn("Exception while closing log file " + tempLogFileLocation, e);
                }
                bufferedLogWriter = null;
            } else {
                // should not occur
                log.warn("close() called on already-closed workflow log writer");
            }
        }
    }

    private BufferedWriter setupLogWriter(File logFile) {
        BufferedWriter fw = null;
        try {
            fw = new BufferedWriter(new FileWriter(logFile));
        } catch (IOException e) {
            log.error("Failed to open log file " + logFile + " for writing", e);
        }
        return fw;
    }
}
