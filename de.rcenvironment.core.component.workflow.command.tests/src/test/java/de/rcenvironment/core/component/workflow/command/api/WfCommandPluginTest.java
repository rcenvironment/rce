/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.command.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.rcenvironment.core.authentication.Session;
import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.common.CommandException.Type;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileException;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowDescriptionLoaderCallback;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionService;
import de.rcenvironment.core.component.workflow.update.api.PersistentWorkflowDescription;
import de.rcenvironment.core.component.workflow.update.api.PersistentWorkflowDescriptionUpdateService;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.common.textstream.receivers.LoggingTextOutReceiver;

/**
 * {@link WfCommandPlugin} test.
 * 
 * @author Sascha Zur
 * @author Jan Flink
 * @author Robert Mischke
 */
public class WfCommandPluginTest {

    private static final String MESSAGE_EXPECTED_COMMAND_EXCEPTION = "Expected command exception";

    private static final String STRING_RUN = "run";

    private static final String STRING_WF = "wf";

    private static final Log LOGGER = LogFactory.getLog(WfCommandPluginTest.class);

    private WfCommandPlugin wfCommandPlugin;

    private HeadlessWorkflowExecutionService workflowExecutionService;

    /**
     * Creates a Session for testing.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        Session.create("dummyUser", 1);
        TempFileServiceAccess.setupUnitTestEnvironment();
    }

    /**
     * Common test setup.
     */
    @Before
    public void setUp() {
//        workflowExecutionService = new HeadlessWorkflowExecutionServiceImpl();
//        workflowExecutionService.bindWorkflowExecutionService(EasyMock.createNiceMock(WorkflowExecutionService.class));
//        workflowExecutionService.bindDistributedNotificationService(EasyMock.createNiceMock(DistributedNotificationService.class));
//        workflowExecutionService.bindPlatformService(EasyMock.createNiceMock(PlatformService.class));
        wfCommandPlugin = new WfCommandPlugin();
        wfCommandPlugin.bindWorkflowExecutionService(EasyMock.createNiceMock(HeadlessWorkflowExecutionService.class));
    }

    /**
     * Test sending the "wf run" command without a filename token. The expected behaviour is an exception that signals a syntax error to the
     * user
     */
    @Test
    public void testWFRunCommandWithoutFilename() {
        List<String> tokens = new ArrayList<String>();
        tokens.add(STRING_WF);
        tokens.add(STRING_RUN);

        TextOutputReceiver outputReceiver = EasyMock.createNiceMock(LoggingTextOutReceiver.class);

        EasyMock.replay(outputReceiver);

        // invoke
        try {
            wfCommandPlugin.execute(new CommandContext(tokens, outputReceiver, null));
            fail("Exception expected");
        } catch (CommandException e) {
            // test exception parameter(s)
            assertEquals("Unexpected CommandException sub-type", CommandException.Type.SYNTAX_ERROR, e.getType());
        }

        EasyMock.verify(outputReceiver);
    }

    /**
     * Test sending the "wf run" command with an invalid filename token. The expected behaviour are response lines containing the words
     * "not existent" and "failed" (case insensitive)
     * 
     * @throws CommandException on unexpected command errors
     */
    @Test
    public void testWFRunCommandWithInvalidFilename() throws CommandException {
        final String testFilename = "invalidFilename";

        List<String> tokens = new ArrayList<String>();
        tokens.add(STRING_WF);
        tokens.add(STRING_RUN);
        tokens.add(testFilename);
        TextOutputReceiver outputReceiver = EasyMock.createNiceMock(LoggingTextOutReceiver.class);

        // define mock expectation
        Capture<String> capture = Capture.newInstance(CaptureType.ALL);
        outputReceiver.addOutput(EasyMock.capture(capture));
        EasyMock.expectLastCall().anyTimes();
        EasyMock.replay(outputReceiver);

        // invoke
        try {
            wfCommandPlugin.execute(new CommandContext(tokens, outputReceiver, null));
            fail(MESSAGE_EXPECTED_COMMAND_EXCEPTION);
        } catch (CommandException e) {
            assertEquals(Type.EXECUTION_ERROR, e.getType());
            String message = e.getMessage();
            assertTrue("Unexpected reponse text (should contain 'does not exist'): " + message, message.toLowerCase()
                .contains("does not exist"));
            assertTrue("Unexpected reponse text (should contain the test filename): " + message,
                message.contains(testFilename));
        }

        // test callback parameter(s)
        assertEquals("Expected no text output", 0, capture.getValues().size());

        EasyMock.verify(outputReceiver);
    }

    /**
     * Test sending the "wf run" command with an invalid file. The expected behaviour is a response line containing the words
     * "loading workflow file failed" (case insensitive)
     * 
     * @throws CommandException on unexpected command errors
     * @throws WorkflowFileException  on unexpected errors
     */
    @Test
    public void testWFRunCommandWithInvalidFile() throws CommandException, WorkflowFileException {

        // create invalid workflow testfile
        File dir = null;
        File testfile = null;
        try {
            dir = TempFileServiceAccess.getInstance().createManagedTempDir();
            testfile = new File(dir, "testfile.wf");
            FileUtils.touch(testfile);
        } catch (IOException e) {
            LOGGER.error("", e);
        }

        Assert.assertNotNull(testfile);
        Assert.assertTrue(testfile.exists());
        Assert.assertTrue(testfile.isFile());

        final String errorMessage = "failed";
//        WorkflowExecutionService wfExeService = EasyMock.createNiceMock(WorkflowExecutionService.class);
//        EasyMock.expect(wfExeService.loadWorkflowDescriptionFromFileConsideringUpdates(EasyMock.isA(File.class), 
//            EasyMock.isA(HeadlessWorkflowDescriptionLoaderCallback.class))).andThrow(new WorkflowFileException(errorMessage)).anyTimes();
//        EasyMock.replay(wfExeService);
        //workflowExecutionService.bindWorkflowExecutionService(wfExeService);
        
        List<String> tokens = new ArrayList<String>();
        tokens.add(STRING_WF);
        tokens.add(STRING_RUN);
        tokens.add(testfile.getAbsolutePath());
        TextOutputReceiver outputReceiver = EasyMock.createNiceMock(LoggingTextOutReceiver.class);

        // define mock expectation
        Capture<String> capture = Capture.newInstance(CaptureType.ALL);
        outputReceiver.addOutput(EasyMock.capture(capture));
        EasyMock.expectLastCall().anyTimes();
        EasyMock.replay(outputReceiver);

        // invoke
        wfCommandPlugin.execute(new CommandContext(tokens, outputReceiver, null));

        // test callback parameter(s)
        assertEquals(2, capture.getValues().size());
        assertTrue(capture.getValues().get(0).contains(errorMessage));
        assertTrue(capture.getValues().get(1).contains("not executed"));

        EasyMock.verify(outputReceiver);

        try {
            TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(dir);
        } catch (IOException e) {
            LOGGER.error("", e);
        }
    }

    /**
     * FIXME: Not done yet.
     * 
     * @throws CommandException on unexpected command errors
     */
    @Test
    public void testWFRunCommand() throws CommandException {

        File dir = null;
        File testfile = null;
        try {
            dir = TempFileServiceAccess.getInstance().createManagedTempDir();
            testfile = new File(dir, "testfile.wf");
            FileUtils.copyInputStreamToFile(WfCommandPluginTest.class
                .getResourceAsStream("/workflows_automated_with_placeholders/Python.wf"), testfile);
        } catch (IOException e) {
            LOGGER.error("", e);
        }

        Assert.assertNotNull(testfile);
        Assert.assertTrue(testfile.exists());
        Assert.assertTrue(testfile.isFile());

        PersistentWorkflowDescriptionUpdateService pwdUpdateServiceMock = EasyMock
            .createNiceMock(PersistentWorkflowDescriptionUpdateService.class);
        // define mock expectation
        EasyMock
            .expect(
                pwdUpdateServiceMock.isUpdateForWorkflowDescriptionAvailable(EasyMock.anyObject(PersistentWorkflowDescription.class),
                    EasyMock.anyBoolean())).andReturn(false).anyTimes();
        EasyMock.replay(pwdUpdateServiceMock);

        List<String> tokens = new ArrayList<String>();
        tokens.add(STRING_WF);
        tokens.add(STRING_RUN);
        tokens.add(" \"" + testfile.getAbsolutePath() + " \"");
        
        TextOutputReceiver outputReceiver = EasyMock.createNiceMock(LoggingTextOutReceiver.class);

        // define mock expectation
        Capture<String> capture = Capture.newInstance(CaptureType.ALL);
        outputReceiver.addOutput(EasyMock.capture(capture));
        EasyMock.expectLastCall().anyTimes();
        EasyMock.replay(outputReceiver);

        // invoke
        try {
            wfCommandPlugin.execute(new CommandContext(tokens, outputReceiver, null));
            fail(MESSAGE_EXPECTED_COMMAND_EXCEPTION);
        } catch (CommandException e) {
            assertEquals(Type.EXECUTION_ERROR, e.getType());
        }

        // test callback parameter(s)
        // String capturedText = capture.getValues().toString();

        // assertTrue("Unexpected reponse text (should contain 'loading workflow file failed')",
        // capturedText.toLowerCase().contains("loading workflow file failed"));

        EasyMock.verify(outputReceiver);

        try {
            TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(dir);
        } catch (IOException e) {
            LOGGER.error("", e);
        }
    }

}
