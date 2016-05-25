/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */


package de.rcenvironment.core.component.workflow.execution.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.LogFactory;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.component.execution.api.ComponentExecutionException;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.ConsoleRow.Type;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Test cases for {@link ComponentsConsoleLogFileWriter}.
 * 
 * @author Doreen Seider
 */
public class ComponentsConsoleLogFileWriterTest {

    private static final int DELAY = 600;

    /**
     * Tests if the files, written and added to the data management contains the correct {@link ConsoleRow} payloads.
     * @throws ComponentExecutionException on unexpected errors
     * @throws IOException on unexpected errors
     * @throws InterruptedException on unexpected errors
     * @throws WorkflowExecutionException on unexpected errors
     */
    @Test
    public void testFilesWritten() throws ComponentExecutionException, IOException, InterruptedException, WorkflowExecutionException {
        
        TempFileServiceAccess.setupUnitTestEnvironment();
        
        WorkflowExecutionStorageBridge wfDataManagementStorage = EasyMock.createNiceMock(WorkflowExecutionStorageBridge.class);
        LogFileCapture fileCapture1 = new LogFileCapture();
        Capture<String> fileNameCapture1 = new Capture<>();
        Capture<String> compRunIdCapture1 = new Capture<>();
        wfDataManagementStorage.addComponentCompleteLog(EasyMock.capture(fileCapture1), EasyMock.capture(fileNameCapture1), 
            EasyMock.capture(compRunIdCapture1));
        
        LogFileCapture fileCapture2 = new LogFileCapture();
        Capture<String> fileNameCapture2 = new Capture<>();
        Capture<String> compRunIdCapture2 = new Capture<>();
        wfDataManagementStorage.addComponentErrorLog(EasyMock.capture(fileCapture2), EasyMock.capture(fileNameCapture2), 
            EasyMock.capture(compRunIdCapture2));
        
        LogFileCapture fileCapture3 = new LogFileCapture();
        Capture<String> fileNameCapture3 = new Capture<>();
        wfDataManagementStorage.addWorkflowErrorLog(EasyMock.capture(fileCapture3), EasyMock.capture(fileNameCapture3));
        EasyMock.replay(wfDataManagementStorage);

        final String wfName = "wf:1";
        final String wfNameForFile = "wf_1";
        final String compName = "comp 1";
        final String compNameForFile = "comp_1";
        final String compExeId = "comp exe 1";
        final String payloadStdOut = "payload out";
        final String payloadStdErr = "payload err";
        final String formatVersion = "Log file format version";
        final String compRunId = "run id 1";
        final String compExeCount = "2";
        ComponentsConsoleLogFileWriter writer = new ComponentsConsoleLogFileWriter(wfDataManagementStorage);
        writer.initializeWorkflowLogFile();
        writer.initializeComponentLogFile(compExeId);
        writer.addComponentConsoleRow(createConsoleRow(Type.TOOL_OUT, compExeId, compName, wfName, payloadStdOut));
        writer.addComponentConsoleRow(createConsoleRow(Type.TOOL_ERROR, compExeId, compName, wfName, payloadStdErr));
        writer.addComponentConsoleRow(createWriteComponentLogToDMConsoleRow(compExeId, compName, wfName, compRunId, compExeCount));
        writer.addWorkflowConsoleRow(createWriteWorkflowLogToDMConsoleRow(wfName));
        
        Thread.sleep(DELAY);
        
        assertTrue(fileCapture1.hasCaptured());
        assertTrue(fileNameCapture1.hasCaptured());
        assertTrue(compRunIdCapture1.hasCaptured());
        
        assertEquals(0, FileUtils.sizeOf(fileCapture1.getValue()));
        List<String> logLines = FileUtils.readLines(fileCapture1.copiedLogFile);
        assertEquals(3, logLines.size());
        assertTrue(logLines.get(0).contains(payloadStdOut));
        assertTrue(logLines.get(1).contains(payloadStdErr));
        assertTrue(logLines.get(2).contains(formatVersion));
        assertTrue(fileNameCapture1.getValue().equals(compNameForFile + "-" + compExeCount + "_compl.log"));
        assertEquals(compRunId, compRunIdCapture1.getValue());
        
        assertTrue(fileCapture2.hasCaptured());
        assertTrue(fileNameCapture2.hasCaptured());
        assertTrue(compRunIdCapture2.hasCaptured());
        
        assertEquals(0, FileUtils.sizeOf(fileCapture2.getValue()));
        logLines = FileUtils.readLines(fileCapture2.copiedLogFile);
        assertEquals(2, logLines.size());
        assertTrue(logLines.get(0).contains(payloadStdErr));
        assertTrue(logLines.get(1).contains(formatVersion));
        assertTrue(fileNameCapture2.getValue().equals(compNameForFile + "-" + compExeCount + "_err.log"));
        assertEquals(compRunId, compRunIdCapture1.getValue());
        
        assertTrue(fileCapture3.hasCaptured());
        assertTrue(fileNameCapture3.hasCaptured());
        
        assertEquals(0, FileUtils.sizeOf(fileCapture3.getValue()));
        logLines = FileUtils.readLines(fileCapture3.copiedLogFile);
        assertEquals(2, logLines.size());
        assertTrue(logLines.get(0).contains(payloadStdErr));
        assertTrue(logLines.get(1).contains(formatVersion));
        assertTrue(fileNameCapture3.getValue().equals(wfNameForFile + "-err.log"));
        
        writer.flushAndDisposeLogFiles();
        
        Thread.sleep(DELAY);
        
        assertFalse(fileCapture1.getValue().exists());
        assertFalse(fileCapture2.getValue().exists());
        assertFalse(fileCapture3.getValue().exists());
    }
    
    private ConsoleRow createConsoleRow(Type type, String compExeId, String compName, String wfName, String payload) {
        ConsoleRow consoleRow = EasyMock.createNiceMock(ConsoleRow.class);
        EasyMock.expect(consoleRow.getType()).andReturn(type).anyTimes();
        EasyMock.expect(consoleRow.getComponentIdentifier()).andReturn(compExeId).anyTimes();
        EasyMock.expect(consoleRow.getComponentName()).andReturn(compName).anyTimes();
        EasyMock.expect(consoleRow.getWorkflowName()).andReturn(wfName).anyTimes();
        EasyMock.expect(consoleRow.getPayload()).andReturn(payload).anyTimes();
        EasyMock.replay(consoleRow);
        return consoleRow;
    }
    
    private ConsoleRow createWriteComponentLogToDMConsoleRow(String compExeId, String compName, String wfName,
        String compRunId, String compExeCount) {
        String payload3 = StringUtils.escapeAndConcat(ConsoleRow.WorkflowLifecyleEventType.COMPONENT_LOG_FINISHED.name(), 
            compRunId, compExeCount);
        return createConsoleRow(Type.LIFE_CYCLE_EVENT, compExeId, compName, wfName, payload3);
    }
    
    private ConsoleRow createWriteWorkflowLogToDMConsoleRow(String wfName) {
        return createConsoleRow(Type.LIFE_CYCLE_EVENT, null, null, wfName,
            ConsoleRow.WorkflowLifecyleEventType.WORKFLOW_LOG_FINISHED.name());
    }
    
    /**
     * Custom capture to copy the captured {@link File} if value is set.
     *
     * @author Doreen Seider
     */
    protected class LogFileCapture extends Capture<File> {
        
        private static final long serialVersionUID = -7974017366664173839L;
        
        protected File copiedLogFile;
        
        @Override
        public void setValue(File value) {
            super.setValue(value);
            copiedLogFile = new File(value.getAbsolutePath() + "_c");
            try {
                FileUtils.copyFile(value, copiedLogFile);
            } catch (IOException e) {
                LogFactory.getLog(getClass()).error("Failed to copy log file", e);
            }
        }
    }
    
}
