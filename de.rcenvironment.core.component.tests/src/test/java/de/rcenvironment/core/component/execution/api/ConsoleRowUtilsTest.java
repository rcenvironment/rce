/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.textstream.TextStreamWatcher;

/**
 * Test cases for {@link ConsoleRowUtils}.
 * 
 * @author Doreen Seider
 */
public class ConsoleRowUtilsTest {

    private final String firstConsoleLine = "first line";

    private final String secondConsoleLine = "second line";

    private final String consoleLines = firstConsoleLine + "\n" + secondConsoleLine;

    private ComponentLog componentLog;

    private Capture<String> firstConsoleLineCapture;
    
    private Capture<String> secondConsoleLineCapture;

    /**
     * Set up test objects.
     */
    @Before
    public void setUp() {
        
        firstConsoleLineCapture = new Capture<String>();
        secondConsoleLineCapture = new Capture<String>();

        componentLog = EasyMock.createStrictMock(ComponentLog.class);
        componentLog.toolStdout(EasyMock.capture(firstConsoleLineCapture));
        componentLog.toolStdout(EasyMock.capture(secondConsoleLineCapture));
        EasyMock.replay(componentLog);
        
    }

    /** Test. */
    @Test
    public void testLogToWorkflowConsole() {

        InputStream inputStream = IOUtils.toInputStream(consoleLines);
        TextStreamWatcher logToWorkflowConsole = ConsoleRowUtils.logToWorkflowConsole(componentLog, inputStream,
            ConsoleRow.Type.TOOL_OUT, null, false);
        logToWorkflowConsole.waitForTermination();
        IOUtils.closeQuietly(inputStream);
        
        assertEquals(firstConsoleLine, firstConsoleLineCapture.getValue());
        assertEquals(secondConsoleLine, secondConsoleLineCapture.getValue());
    }

    /**
     * Test.
     * 
     * @throws IOException on error
     */
    @Test
    public void testLogToWorkflowConsoleWithFile() throws IOException {

        TempFileServiceAccess.setupUnitTestEnvironment();
        TempFileService tempFileUtils = TempFileServiceAccess.getInstance();
        File tempFile = null;
        try {
            tempFile = tempFileUtils.createTempFileWithFixedFilename("log.txt");
        } catch (IOException e) {
            fail("Cannot create temp file for log file testing.");
        }

        InputStream inputStream = IOUtils.toInputStream(consoleLines);
        TextStreamWatcher logToWorkflowConsole = ConsoleRowUtils.logToWorkflowConsole(componentLog, inputStream,
            ConsoleRow.Type.TOOL_OUT, tempFile, false);
        logToWorkflowConsole.waitForTermination();
        IOUtils.closeQuietly(inputStream);
        
        assertEquals(firstConsoleLine, firstConsoleLineCapture.getValue());
        assertEquals(secondConsoleLine, secondConsoleLineCapture.getValue());

        String content = null;
        try {
            content = FileUtils.readFileToString(tempFile);
            assertEquals(consoleLines, content);
        } catch (IOException e1) {
            fail("Reading log file's content failed");
        } finally {
            if (tempFile != null) {
                try {
                    tempFileUtils.disposeManagedTempDirOrFile(tempFile);
                } catch (IOException e) {
                    fail("Cannot delete temp file for log file testing.");
                }
            }
        }
    }

}
