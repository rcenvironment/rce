/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.execution.api.BatchedConsoleRowsProcessor;
import de.rcenvironment.core.component.execution.api.BatchingConsoleRowsForwarder;
import de.rcenvironment.core.component.execution.api.ComponentExecutionException;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.ConsoleRow.Type;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Writes {@link ConsoleRow}s into a log files and stores them into the data management.
 * 
 * @author Doreen Seider
 */
public class ComponentsConsoleLogFileWriter {
    
    private static final String VERSION = "1.0";
    
    private static final String UNDERSCORE = "_";
    
    private static final String ALLOWED_CHARACTERS = "[^a-zA-Z0-9.-]";
    
    private final WorkflowExecutionStorageBridge wfDataManagementStorage;
    
    private AtomicReference<BatchingConsoleRowsForwarder> errorWorkflowConsoleRowWriter = new AtomicReference<>();

    private final Map<String, BatchingConsoleRowsForwarder> completeConsoleRowWriters
        = Collections.synchronizedMap(new HashMap<String, BatchingConsoleRowsForwarder>());
    
    private final Map<String, BatchingConsoleRowsForwarder> errorConsoleRowWriters
        = Collections.synchronizedMap(new HashMap<String, BatchingConsoleRowsForwarder>());

    private AtomicReference<CountDownLatch> logFilesDisposedLatch = new AtomicReference<>();
    
    protected ComponentsConsoleLogFileWriter(WorkflowExecutionStorageBridge wfDataManagementStorage) {
        this.wfDataManagementStorage = wfDataManagementStorage;
    }
    
    protected void initializeWorkflowLogFile() throws IOException {
        errorWorkflowConsoleRowWriter.set(new BatchingConsoleRowsForwarder(new BactchedWorkflowErrorLogFileWriter("")));
        logFilesDisposedLatch.set(new CountDownLatch(1));
    }

    protected void initializeComponentLogFile(String compExeId) throws IOException {
        logFilesDisposedLatch.set(new CountDownLatch((int) logFilesDisposedLatch.get().getCount() + 2));
        completeConsoleRowWriters.put(compExeId, new BatchingConsoleRowsForwarder(new ComponentCompleteLogFileWriter(compExeId)));
        errorConsoleRowWriters.put(compExeId, new BatchingConsoleRowsForwarder(new ComponentErrorLogFileWriter(compExeId)));
    }

    protected void addComponentConsoleRow(ConsoleRow consoleRow) {
        if (completeConsoleRowWriters.containsKey(consoleRow.getComponentIdentifier())) {
            completeConsoleRowWriters.get(consoleRow.getComponentIdentifier()).onConsoleRow(consoleRow);
        } else {
            LogFactory.getLog(getClass()).error(StringUtils.format(
                "Failed to add console row to component's complete log file: %s", consoleRow.getPayload()));
        }
        if (consoleRow.getType() == Type.TOOL_ERROR || consoleRow.getType() == Type.COMPONENT_ERROR
            || consoleRow.getType() == Type.LIFE_CYCLE_EVENT) {
            if (errorConsoleRowWriters.containsKey(consoleRow.getComponentIdentifier())) {
                errorConsoleRowWriters.get(consoleRow.getComponentIdentifier()).onConsoleRow(consoleRow);
            } else {
                LogFactory.getLog(getClass()).error(StringUtils.format(
                    "Failed to add console row to component's error log file: %s", consoleRow.getPayload()));
            }
            errorWorkflowConsoleRowWriter.get().onConsoleRow(consoleRow);
        }
    }
    
    protected void addWorkflowConsoleRow(ConsoleRow consoleRow) {
        errorWorkflowConsoleRowWriter.get().onConsoleRow(consoleRow);
    }
    
    protected void disposeLogFiles() {
        for (BatchingConsoleRowsForwarder writer : completeConsoleRowWriters.values()) {
            writer.onConsoleRow(null); // should be improved by dedicated ConsoleRow instance
        }
        for (BatchingConsoleRowsForwarder writer : errorConsoleRowWriters.values()) {
            writer.onConsoleRow(null); // should be improved by dedicated ConsoleRow instance
        }
        errorWorkflowConsoleRowWriter.get().onConsoleRow(null); // should be improved by dedicated ConsoleRow instance
        
        try {
            boolean terminated;
            synchronized (this) {
                terminated = logFilesDisposedLatch.get().await(10, TimeUnit.SECONDS);
            }
            if (!terminated) {
                LogFactory.getLog(getClass()).error("Time out exceeded while waiting for log files written");
            }
        } catch (InterruptedException e) {
            LogFactory.getLog(getClass()).error("Failed to wait log files written", e);
        }
    }
    
    
    /**
     * Writes the {@link ConsoleRow}s to the complete log file of a component in the RCE temp directory and stores the file in the RCE data
     * management on dedicated {@link ConsoleRow}s.
     * 
     * @author Doreen Seider
     */
    protected class ComponentCompleteLogFileWriter extends AbstractBatchedComponentLogFileWriter {
        
        protected ComponentCompleteLogFileWriter(String exeId) throws IOException {
            super(exeId);
        }

        @Override
        protected void storeLogFileInDataManagement(ConsoleRow triggerConsoleRow) {
            try {
                String[] payload = StringUtils.splitAndUnescape(triggerConsoleRow.getPayload());
                String logFileName = StringUtils.format("%s-%s_compl.log", triggerConsoleRow.getComponentName()
                    .replaceAll(ALLOWED_CHARACTERS, UNDERSCORE), payload[2]);
                wfDataManagementStorage.addComponentCompleteLog(logFile, logFileName, payload[1]);
            } catch (ComponentExecutionException e) {
                logErrorOnWritingToDMFailure(e, triggerConsoleRow);
            }
        }
        
        @Override
        protected String formatConsoleRow(ConsoleRow consoleRow) {
            return consoleRowFormatter.toComponentCompleteLogFileFormat(consoleRow);
        }
    }
    
    /**
     * Writes the {@link ConsoleRow}s to the error log file of a component in the RCE temp directory and stores the file in the RCE data
     * management on dedicated {@link ConsoleRow}s.
     * 
     * @author Doreen Seider
     */
    protected class ComponentErrorLogFileWriter extends AbstractBatchedComponentLogFileWriter {
        
        protected ComponentErrorLogFileWriter(String exeId) throws IOException {
            super(exeId);
        }

        @Override
        protected void storeLogFileInDataManagement(ConsoleRow triggerConsoleRow) {
            try {
                String[] payload = StringUtils.splitAndUnescape(triggerConsoleRow.getPayload());
                String logFileName = StringUtils.format("%s-%s_err.log", triggerConsoleRow.getComponentName()
                    .replaceAll(ALLOWED_CHARACTERS, UNDERSCORE), payload[2]);
                wfDataManagementStorage.addComponentErrorLog(logFile, logFileName, payload[1]);
            } catch (ComponentExecutionException e) {
                logErrorOnWritingToDMFailure(e, triggerConsoleRow);
            }
        }
        
        @Override
        protected String formatConsoleRow(ConsoleRow consoleRow) {
            return consoleRowFormatter.toComponentErrorLogFileFormat(consoleRow);
        }
    }
    
    /**
     * Writes the {@link ConsoleRow}s to the error log file of a workflow in the RCE temp directory and stores the file in the RCE data
     * management on dedicated {@link ConsoleRow}s.
     * 
     * @author Doreen Seider
     */
    protected class BactchedWorkflowErrorLogFileWriter extends AbstractBatchedLogFileWriter {
        
        protected BactchedWorkflowErrorLogFileWriter(String exeId) throws IOException {
            super(exeId);
        }

        @Override
        protected void storeLogFileInDataManagement(ConsoleRow triggerConsoleRow) {
            try {
                String logFileName = StringUtils.format("%s-err.log", triggerConsoleRow.getWorkflowName()
                    .replaceAll(ALLOWED_CHARACTERS, UNDERSCORE));
                wfDataManagementStorage.addWorkflowErrorLog(logFile, logFileName);
            } catch (WorkflowExecutionException e) {
                log.error(StringUtils.format(FAILED_TO_STORE_LOG_FILE_IN_DM, 
                    triggerConsoleRow.getWorkflowName(), triggerConsoleRow.getWorkflowIdentifier(), logFile.getName()), e);
            }
        }

        @Override
        protected boolean isTriggerForWritingLogFileToDM(ConsoleRow consoleRow) {
            return consoleRow.getType() == ConsoleRow.Type.LIFE_CYCLE_EVENT
                && consoleRow.getPayload().startsWith(ConsoleRow.WorkflowLifecyleEventType.WORKFLOW_LOG_FINISHED.name());
        }
        
        @Override
        protected String formatConsoleRow(ConsoleRow consoleRow) {
            return consoleRowFormatter.toWorkflowErrorLogFileFormat(consoleRow);
        }
    }
    
    /**
     * Writes the {@link ConsoleRow}s to a log file in the RCE temp directory and stores the file in the RCE data management on dedicated
     * {@link ConsoleRow}s.
     * 
     * @author Doreen Seider
     */
    protected abstract class AbstractBatchedComponentLogFileWriter extends AbstractBatchedLogFileWriter {

        protected AbstractBatchedComponentLogFileWriter(String exeId) throws IOException {
            super(exeId);
        }
        
        @Override
        protected boolean isTriggerForWritingLogFileToDM(ConsoleRow consoleRow) {
            return consoleRow.getType() == ConsoleRow.Type.LIFE_CYCLE_EVENT
                && consoleRow.getPayload().startsWith(ConsoleRow.WorkflowLifecyleEventType.COMPONENT_LOG_FINISHED.name());
        }
        
        protected void logErrorOnWritingToDMFailure(ComponentExecutionException e, ConsoleRow consoleRow) {
            log.error(StringUtils.format(FAILED_TO_STORE_LOG_FILE_IN_DM, 
                consoleRow.getWorkflowName(), consoleRow.getWorkflowIdentifier(), logFile.getName()), e);
        }
        
    }
    
    /**
     * Writes the {@link ConsoleRow}s to a log file in the RCE temp directory and stores the file in the RCE data management on dedicated
     * {@link ConsoleRow}s.
     * 
     * @author Doreen Seider
     */
    protected abstract class AbstractBatchedLogFileWriter implements BatchedConsoleRowsProcessor {
        
        protected static final String FAILED_TO_STORE_LOG_FILE_IN_DM = "Failed to store log file in data management"
            + " - workflow '%s' (%s): %s";
        
        private static final String FAILED_TO_CREATE_TEMPORARY_LOG_FILE = "Failed to create temporary log file: ";

        private static final String FAILED_TO_CLEAR_TEMPORARY_LOG_FILE = "Failed to clear temporary log file: ";

        private static final String FAILED_TO_DELETE_TEMPORARY_LOG_FILE = "Failed to delete temporary log file: ";

        protected final Log log = LogFactory.getLog(getClass());

        protected final ConsoleRowFormatter consoleRowFormatter = new ConsoleRowFormatter();

        protected File logFile;

        private final TempFileService tempFileService;
        
        private final String exeId;
        
        protected AbstractBatchedLogFileWriter(String exeId) throws IOException {
            tempFileService = TempFileServiceAccess.getInstance();
            logFile = tempFileService.createTempFileFromPattern("*");
            this.exeId = exeId;
        }

        @Override
        public void processConsoleRows(ConsoleRow[] consoleRows) {

            List<String> logFileEntries = new ArrayList<>();
            
            for (ConsoleRow consoleRow : consoleRows) {
                if (consoleRow == null) {
                    if (FileUtils.sizeOf(logFile) != 0) {
                        printLogFileNotEmptyError(logFile);
                    }
                    disposeLogFile();
                    return;
                } else if (isTriggerForWritingLogFileToDM(consoleRow)) {
                    writeConsoleRowsToFile(logFileEntries);
                    if (FileUtils.sizeOf(logFile) > 0) {
                        writeVersionToFile();
                        storeLogFileInDataManagement(consoleRow);
                        clearLogFile();
                        logFileEntries.clear();
                    }
                } else if (isMatchingConsoleRow(consoleRow)) {
                    logFileEntries.add(formatConsoleRow(consoleRow));
                }
            }
            writeConsoleRowsToFile(logFileEntries);
        }
        
        protected abstract boolean isTriggerForWritingLogFileToDM(ConsoleRow consoleRow);
        
        protected boolean isMatchingConsoleRow(ConsoleRow consoleRow) {
            return consoleRow.getType() != Type.LIFE_CYCLE_EVENT;
        }
        
        protected abstract String formatConsoleRow(ConsoleRow consoleRow);
        
        private void printLogFileNotEmptyError(File file) {
            String fileContent = "[not available]";
            try (FileInputStream inputStream = new FileInputStream(logFile)) {
                fileContent = IOUtils.toString(inputStream);
            } catch (IOException e) {
                log.debug(StringUtils.format("Failed to get content of log file %s; cause: &s", logFile.getAbsolutePath(), e.toString()));
            }
            log.error(StringUtils.format("Request to dispose non-empty log file (related to %s), means that"
                + " the content get lost as it it was not stored to the data management before; file: %s; size: %d, content: %s",
                exeId, file.getAbsolutePath(), FileUtils.sizeOf(file), fileContent));
        }
        
        private void writeConsoleRowsToFile(List<String> logFileEntries) {
            try {
                FileUtils.writeLines(logFile, logFileEntries, true);
            } catch (IOException e) {
                log.error("Failed to add a console log row to the log file: " + logFile, e);
            }
        }

        private void writeVersionToFile() {
            try {
                FileUtils.writeStringToFile(logFile, StringUtils.format("[Log file format version: %s]\n", VERSION), true);
            } catch (IOException e) {
                log.error("Failed to add a console log row to the log file: " + logFile, e);
            }
        }
        
        private void disposeLogFile() {
            try {
                tempFileService.disposeManagedTempDirOrFile(logFile);
            } catch (IOException e) {
                log.error(FAILED_TO_DELETE_TEMPORARY_LOG_FILE + logFile.getAbsolutePath(), e);
            }
            logFilesDisposedLatch.get().countDown();
        }
        
        protected abstract void storeLogFileInDataManagement(ConsoleRow triggerConsoleRow);
        
        private void clearLogFile() {
            try {
                FileUtils.writeStringToFile(logFile, "", false);
            } catch (IOException e) {
                log.error(FAILED_TO_CLEAR_TEMPORARY_LOG_FILE + logFile.getAbsolutePath(), e);
                try {
                    tempFileService.disposeManagedTempDirOrFile(logFile);
                } catch (IOException e1) {
                    log.error(FAILED_TO_DELETE_TEMPORARY_LOG_FILE + logFile.getAbsolutePath(), e);
                } finally {
                    try {
                        logFile = tempFileService.createTempFileFromPattern("*");
                    } catch (IOException e1) {
                        log.error(FAILED_TO_CREATE_TEMPORARY_LOG_FILE + logFile.getAbsolutePath(), e);
                    }
                }
            }
        }
        
    }
}
