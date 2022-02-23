/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.integration;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ComponentEventAnnouncement;
import de.rcenvironment.core.component.execution.api.ComponentEventAnnouncementDispatcher;
import de.rcenvironment.core.component.execution.api.ComponentLog;
import de.rcenvironment.core.component.model.impl.ToolIntegrationConstants;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.toolkit.utils.text.TextLinesReceiver;

/**
 * Test cases for {@link CommonToolIntegratorComponent}.
 * 
 * @author Doreen Seider
 */
public class CommonToolIntegratorComponentTest {

    private static final String FAILED_TO_CREATE_FILE_WITH_VERIFICATION_KEY = "Failed to create file with verification key";

    private static final String FILE_WITH_VERIFICATION_KEY_CREATED = "File with verification key created";

    private static final String WAITING_FOR_APPROVAL = "Waiting for approval";

    private static final String VERIFICATION_TOKEN = "some-token";

    private static File tempDir;

    /**
     * Creates a temporary directory used by this unit tests.
     * 
     * @throws IOException on unexpected error
     */
    @BeforeClass
    public static void createTempDirectory() throws IOException {
        TempFileServiceAccess.setupUnitTestEnvironment();

        TempFileService tempFileService = TempFileServiceAccess.getInstance();
        tempDir = tempFileService.createManagedTempDir();
    }

    /**
     * Deletes the temporary directory used by this unit tests.
     * 
     * @throws IOException on unexpected error
     */
    @AfterClass
    public static void deleteTempDirectory() throws IOException {
        TempFileService tempFileService = TempFileServiceAccess.getInstance();
        tempFileService.disposeManagedTempDirOrFile(tempDir);
    }

    /**
     * Tests creation of verification key file.
     * 
     * @throws IOException on expected error
     * @throws ComponentException on expected error
     */
    @Test
    public void testVerificationTokenFileHandling() throws IOException, ComponentException {

        ComponentLog compLogMock = EasyMock.createStrictMock(ComponentLog.class);
        Capture<String> logMessageCapture = Capture.newInstance(CaptureType.ALL);
        compLogMock.componentInfo(EasyMock.capture(logMessageCapture));
        EasyMock.expectLastCall().times(2);
        EasyMock.replay(compLogMock);

        ComponentContext compCtxMock = createComponentContextMock(true, false, compLogMock, null);
        CommonToolIntegratorComponent comp = instantiateAndSetupComponent(compCtxMock);
        comp.handleVerificationToken(VERIFICATION_TOKEN);

        EasyMock.verify(compLogMock);
        EasyMock.verify(compCtxMock);

        assertTrue(logMessageCapture.getValues().get(0).contains(FILE_WITH_VERIFICATION_KEY_CREATED));
        assertTrue(logMessageCapture.getValues().get(1).contains(WAITING_FOR_APPROVAL));

        String fileConent = FileUtils.readFileToString(new File(tempDir, "verification-key"));
        assertTrue(fileConent.startsWith(StringUtils.format("Verification key: %s", VERIFICATION_TOKEN)));
        assertTrue(fileConent.contains("How to verify tool results?"));
    }

    /**
     * Tests if component only fails if neither verification token file could be stored nor the email with the verification token could be
     * sent.
     * 
     * @throws IOException on expected error
     * @throws ComponentException on expected error
     */
    @Test
    public void testVerificationTokenInFailureCase() throws IOException, ComponentException {

        // file succeeds, mail fails
        ComponentLog compLogMock = EasyMock.createStrictMock(ComponentLog.class);
        Capture<String> logInfoMessageCapture = Capture.newInstance(CaptureType.ALL);
        compLogMock.componentInfo(EasyMock.capture(logInfoMessageCapture));
        EasyMock.expectLastCall().times(2);
        EasyMock.replay(compLogMock);

        ComponentEventAnnouncementDispatcher compEveAnnDispatcherMock = createComponentEventAnnouncementDispatcher(false);
        ComponentContext compCtxMock = createComponentContextMock(true, true, compLogMock, compEveAnnDispatcherMock);
        CommonToolIntegratorComponent comp = instantiateAndSetupComponent(compCtxMock);
        comp.handleVerificationToken(VERIFICATION_TOKEN);

        verifyVerificationTokenRelatedMocks(compLogMock, compEveAnnDispatcherMock, compCtxMock);
        
        assertTrue(logInfoMessageCapture.getValues().get(0).contains(FILE_WITH_VERIFICATION_KEY_CREATED));
        assertTrue(logInfoMessageCapture.getValues().get(1).contains(WAITING_FOR_APPROVAL));
        
        // file succeeds, mails fails
        EasyMock.reset(compLogMock);
        EasyMock.reset(compEveAnnDispatcherMock);
        EasyMock.reset(compCtxMock);
        Capture<String> logErrorMessageCapture = Capture.newInstance(CaptureType.ALL);
        logInfoMessageCapture = Capture.newInstance(CaptureType.ALL);
        compLogMock.componentError(EasyMock.capture(logErrorMessageCapture));
        EasyMock.expectLastCall().once();
        compLogMock.componentInfo(EasyMock.capture(logInfoMessageCapture));
        EasyMock.expectLastCall().times(2);
        EasyMock.replay(compLogMock);

        compEveAnnDispatcherMock = createComponentEventAnnouncementDispatcher(true);
        compCtxMock = createComponentContextMock(false, true, compLogMock, compEveAnnDispatcherMock);
        comp = instantiateAndSetupComponent(compCtxMock);
        comp.handleVerificationToken(VERIFICATION_TOKEN);
        
        verifyVerificationTokenRelatedMocks(compLogMock, compEveAnnDispatcherMock, compCtxMock);
        
        assertTrue(logErrorMessageCapture.getValues().get(0).contains(FAILED_TO_CREATE_FILE_WITH_VERIFICATION_KEY));
        assertTrue(logInfoMessageCapture.getValues().get(0).contains("Email with verification key sent"));
        assertTrue(logInfoMessageCapture.getValues().get(1).contains(WAITING_FOR_APPROVAL));

        // file fails, mails fails

        // file succeeds, mails fails
        EasyMock.reset(compLogMock);
        logErrorMessageCapture = Capture.newInstance(CaptureType.ALL);
        logInfoMessageCapture = Capture.newInstance(CaptureType.ALL);
        compLogMock.componentError(EasyMock.capture(logErrorMessageCapture));
        EasyMock.replay(compLogMock);

        compEveAnnDispatcherMock = createComponentEventAnnouncementDispatcher(false);
        compCtxMock = createComponentContextMock(false, true, compLogMock, compEveAnnDispatcherMock);
        comp = instantiateAndSetupComponent(compCtxMock);
        try {
            comp.handleVerificationToken(VERIFICATION_TOKEN);
            fail("Exception expected");
        } catch (ComponentException e) {
            assertTrue(e.getMessage().contains(""));
        }
        
        verifyVerificationTokenRelatedMocks(compLogMock, compEveAnnDispatcherMock, compCtxMock);
        
        assertTrue(logErrorMessageCapture.getValues().get(0).contains(FAILED_TO_CREATE_FILE_WITH_VERIFICATION_KEY));
    }

    private void verifyVerificationTokenRelatedMocks(ComponentLog compLogMock,
        ComponentEventAnnouncementDispatcher compEveAnnDispatcherMock, ComponentContext compCtxMock) {
        EasyMock.verify(compLogMock);
        EasyMock.verify(compEveAnnDispatcherMock);
        EasyMock.verify(compCtxMock);
    }

    private ComponentContext createComponentContextMock(boolean tokenFileCreationShouldSucceed, boolean emailEnabled,
        ComponentLog compLogMock, ComponentEventAnnouncementDispatcher compEveAnnDispatcherMock) throws IOException {
        final String compName = "some comp name";
        final int executionCount = 7;
        final String wfName = "some wf name";
        final String mailAddress = "some_name@mail.de";

        ComponentContext compCtxMock = EasyMock.createStrictMock(ComponentContext.class);
        EasyMock.expect(compCtxMock.getService(ComponentEventAnnouncementDispatcher.class)).andStubReturn(compEveAnnDispatcherMock);
        if (tokenFileCreationShouldSucceed) {
            EasyMock.expect(compCtxMock.getConfigurationValue(ToolIntegrationConstants.KEY_VERIFICATION_TOKEN_LOCATION))
                .andStubReturn(tempDir.getAbsolutePath());
        } else {
            File invalidTokenLocation = new File(tempDir, "file");
            invalidTokenLocation.createNewFile();
            EasyMock.expect(compCtxMock.getConfigurationValue(ToolIntegrationConstants.KEY_VERIFICATION_TOKEN_LOCATION))
                .andStubReturn(invalidTokenLocation.getAbsolutePath());
        }

        if (emailEnabled) {
            EasyMock.expect(compCtxMock.getConfigurationValue(ToolIntegrationConstants.KEY_VERIFICATION_TOKEN_RECIPIENTS))
                .andStubReturn(mailAddress);
        } else {
            EasyMock.expect(compCtxMock.getConfigurationValue(ToolIntegrationConstants.KEY_VERIFICATION_TOKEN_RECIPIENTS))
                .andStubReturn(null);
        }
        EasyMock.expect(compCtxMock.getLog()).andStubReturn(compLogMock);
        EasyMock.expect(compCtxMock.getComponentName()).andStubReturn(compName);
        EasyMock.expect(compCtxMock.getExecutionCount()).andStubReturn(executionCount);
        EasyMock.expect(compCtxMock.getWorkflowInstanceName()).andStubReturn(wfName);
        EasyMock.replay(compCtxMock);

        return compCtxMock;
    }

    private ComponentEventAnnouncementDispatcher createComponentEventAnnouncementDispatcher(boolean succeeding) {
        ComponentEventAnnouncementDispatcher compEveAnnDispatcherMock =
            EasyMock.createStrictMock(ComponentEventAnnouncementDispatcher.class);
        EasyMock.expect(compEveAnnDispatcherMock.dispatchWorkflowEventAnnouncementViaMail(EasyMock.anyObject(String[].class),
            EasyMock.anyObject(ComponentEventAnnouncement.class), EasyMock.anyObject(TextLinesReceiver.class))).andStubReturn(succeeding);
        EasyMock.replay(compEveAnnDispatcherMock);
        return compEveAnnDispatcherMock;
    }

    private CommonToolIntegratorComponent instantiateAndSetupComponent(ComponentContext compCtxMock) {
        CommonToolIntegratorComponent comp = new CommonToolIntegratorComponent();
        comp.setComponentContext(compCtxMock);
        return comp;
    }
}
