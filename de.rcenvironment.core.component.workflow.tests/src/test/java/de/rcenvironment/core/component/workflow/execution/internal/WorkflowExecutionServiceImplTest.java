/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileException;
import de.rcenvironment.core.component.workflow.execution.spi.WorkflowDescriptionLoaderCallback;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescriptionPersistenceHandlerTestUtils;
import de.rcenvironment.core.component.workflow.update.api.PersistentWorkflowDescriptionUpdateUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Test cases for {@link WorkflowExecutionServiceImpl}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (minor fix)
 */
public class WorkflowExecutionServiceImplTest {

    private TempFileService tempFileService = TempFileServiceAccess.getInstance();

    private File tempDir;

    /**
     * Temp file environment setup.
     * 
     * @throws IOException on unexpected error
     */
    @BeforeClass
    public static void setUpTempFileEnvironment() throws IOException {
        TempFileServiceAccess.setupUnitTestEnvironment();
    }

    /**
     * Creates a temp directory used for workflow test files.
     * 
     * @throws IOException on unexpected error
     */
    @Before
    public void setUp() throws IOException {
        tempDir = tempFileService.createManagedTempDir();
        WorkflowDescriptionPersistenceHandlerTestUtils.initializeStaticFieldsOfWorkflowDescriptionPersistenceHandler();
    }

    /**
     * Deletes the temp directory created in {@link #setUp()}.
     * 
     * @throws IOException on unexpected error
     */
    @After
    public void tearDown() throws IOException {
        tempFileService.disposeManagedTempDirOrFile(tempDir);
    }

    /**
     * Tests error case: Workflow file cannot be parsed completely. Some parts are skipped. Expected behavior: Original workflow file is
     * copied into a backup file and it only contains the valid parts of the workflow description now.
     * 
     * @throws WorkflowFileException on unexpected error
     * @throws IOException on unexpected error
     */
    @Test
    public void testLoadWorkflowDescriptionFromFile() throws WorkflowFileException, IOException {

        LogicalNodeId logicalNodeIdMock = EasyMock.createStrictMock(LogicalNodeId.class);

        final PlatformService platformServiceMock = EasyMock.createStrictMock(PlatformService.class);
        EasyMock.expect(platformServiceMock.getLocalDefaultLogicalNodeId()).andStubReturn(logicalNodeIdMock);
        EasyMock.replay(platformServiceMock);

        WorkflowDescriptionPersistenceHandlerTestUtils.createWorkflowDescriptionPersistenceHandlerTestInstance();

        WorkflowExecutionServiceImpl wfExeService = new WorkflowExecutionServiceImpl();
        wfExeService.bindPersistentWorkflowDescriptionLoaderService(new PersistentWorkflowDescriptionLoaderServiceImpl());

        File wfFileOrigin = new File(tempDir, "test_origin.wf");
        File wfFile = new File(tempDir, "test.wf");
        FileUtils.writeStringToFile(wfFileOrigin,
            IOUtils.toString(getClass().getResourceAsStream("/workflows_unit_test/Parsing_error.wf")));
        FileUtils.copyFile(wfFileOrigin, wfFile);

        String expectedBackupFileName =
            PersistentWorkflowDescriptionUpdateUtils.getFilenameForBackupFile(wfFile) + WorkflowConstants.WORKFLOW_FILE_ENDING;
        WorkflowDescriptionLoaderCallback callbackMock = EasyMock.createStrictMock(WorkflowDescriptionLoaderCallback.class);
        EasyMock.expect(callbackMock.arePartlyParsedWorkflowConsiderValid()).andReturn(true);
        callbackMock.onWorkflowFileParsingPartlyFailed(expectedBackupFileName);
        EasyMock.expectLastCall();
        EasyMock.replay(callbackMock);

        wfExeService.loadWorkflowDescriptionFromFile(wfFile, callbackMock);

        EasyMock.verify(callbackMock);

        File backupFile = new File(tempDir, expectedBackupFileName);
        assertTrue(backupFile.exists());

        FileUtils.contentEquals(wfFileOrigin, backupFile);
        assertTrue(FileUtils.sizeOf(wfFile) < FileUtils.sizeOf(wfFileOrigin));

        WorkflowDescription wd =
            wfExeService.loadWorkflowDescriptionFromFile(wfFile, EasyMock.createStrictMock(WorkflowDescriptionLoaderCallback.class));
        assertEquals(0, wd.getWorkflowLabels().size());
    }

}
