/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.execution.api.PersistentWorkflowDescriptionLoaderService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileException;
import de.rcenvironment.core.component.workflow.execution.spi.WorkflowDescriptionLoaderCallback;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescriptionPersistenceHandler;
import de.rcenvironment.core.component.workflow.update.api.PersistentWorkflowDescription;
import de.rcenvironment.core.component.workflow.update.api.PersistentWorkflowDescriptionUpdateService;
import de.rcenvironment.core.component.workflow.update.api.PersistentWorkflowDescriptionUpdateUtils;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Default {@link PersistentWorkflowDescriptionLoaderService} implementation. These methods were extraced out of
 * {@link WorkflowExecutionServiceImpl} to improve cohesion.
 *
 * @author Doreen Seider
 * @author Robert Mischke
 */
@Component
public class PersistentWorkflowDescriptionLoaderServiceImpl implements PersistentWorkflowDescriptionLoaderService {

    private static final String FAILED_TO_LOAD_WORKFLOW_FILE = "Failed to load workflow file: ";

    private PersistentWorkflowDescriptionUpdateService wfUpdateService;

    private final Log log = LogFactory.getLog(getClass());

    @Override
    public WorkflowDescription loadWorkflowDescriptionFromFile(File wfFile, WorkflowDescriptionLoaderCallback callback)
        throws WorkflowFileException {
        try {
            int wfVersion = readWorkflowVersionNumber(wfFile);
            if (wfVersion > WorkflowConstants.CURRENT_WORKFLOW_VERSION_NUMBER) {
                throw new WorkflowFileException(FAILED_TO_LOAD_WORKFLOW_FILE + wfFile.getAbsolutePath()
                    + StringUtils.format(". Its version (%d) is older than the expected"
                        + " one (%d). Most likely reason: Internal error on workflow update.",
                        wfVersion, WorkflowConstants.CURRENT_WORKFLOW_VERSION_NUMBER));
            }
            WorkflowDescriptionPersistenceHandler wdPesistenceHandler = new WorkflowDescriptionPersistenceHandler();
            WorkflowDescription wd;
            try (InputStream fileInputStream = new FileInputStream(wfFile)) {
                wd = wdPesistenceHandler.readWorkflowDescriptionFromStream(fileInputStream);
            } catch (WorkflowFileException e) {
                if (e.getParsedWorkflowDescription() != null && callback.arePartlyParsedWorkflowConsiderValid()) {
                    // backup the orginal workflow file and overwrite it with the reduced but valid workflow description
                    String backupFilename =
                        PersistentWorkflowDescriptionUpdateUtils.getFilenameForBackupFile(wfFile)
                            + WorkflowConstants.WORKFLOW_FILE_ENDING;
                    FileUtils.copyFile(wfFile, new File(wfFile.getParentFile().getAbsolutePath(), backupFilename));
                    wd = e.getParsedWorkflowDescription();
                    try (FileOutputStream fos = new FileOutputStream(wfFile);
                        ByteArrayOutputStream baos = wdPesistenceHandler.writeWorkflowDescriptionToStream(wd)) {
                        baos.writeTo(fos);
                    }
                    callback.onWorkflowFileParsingPartlyFailed(backupFilename);
                } else {
                    throw e;
                }
            }
            return wd;
        } catch (IOException | RuntimeException e) {
            throw new WorkflowFileException(FAILED_TO_LOAD_WORKFLOW_FILE + wfFile.getAbsolutePath(), e);
        }
    }

    @Override
    public WorkflowDescription loadWorkflowDescriptionFromFileConsideringUpdates(
        File wfFile, WorkflowDescriptionLoaderCallback callback, boolean abortIfWorkflowUpdateRequired) throws WorkflowFileException {

        try {
            int wfVersion = readWorkflowVersionNumber(wfFile);
            if (wfVersion > WorkflowConstants.CURRENT_WORKFLOW_VERSION_NUMBER) {
                throw new WorkflowFileException(FAILED_TO_LOAD_WORKFLOW_FILE + wfFile.getAbsolutePath()
                    + StringUtils.format(". Its version (%d) is newer than the expected"
                        + " one (%d). Most likely reason: it was opened with a newer version of RCE before.",
                        wfVersion, WorkflowConstants.CURRENT_WORKFLOW_VERSION_NUMBER));

            }

            try (InputStream fileInputStream = new FileInputStream(wfFile)) {

                PersistentWorkflowDescription persistentDescription =
                    wfUpdateService.createPersistentWorkflowDescription(
                        IOUtils.toString(fileInputStream, WorkflowConstants.ENCODING_UTF8));

                boolean updateRequired =
                    wfUpdateService.isUpdateForWorkflowDescriptionAvailable(persistentDescription, false);
                boolean nonSilentUpdateRequired = updateRequired;

                if (updateRequired && abortIfWorkflowUpdateRequired) {
                    throw new WorkflowFileException(
                        "The workflow file "
                            + wfFile.getAbsolutePath()
                            + " would require an update before execution, but the 'fail on required update' flag has been set. "
                            + "Typically, this means that it was generated from an internal template which should be updated.");
                }

                if (!nonSilentUpdateRequired) {
                    updateRequired = wfUpdateService
                        .isUpdateForWorkflowDescriptionAvailable(persistentDescription, true);
                }
                if (updateRequired) {
                    String backupFilename = null;
                    if (nonSilentUpdateRequired) {
                        backupFilename = PersistentWorkflowDescriptionUpdateUtils.getFilenameForBackupFile(wfFile) + ".wf";
                        FileUtils.copyFile(wfFile, new File(wfFile.getParentFile().getAbsolutePath(), backupFilename));
                    }
                    try {
                        updateWorkflow(persistentDescription, wfFile);
                        onWorkflowFileUpdated(wfFile, !nonSilentUpdateRequired, backupFilename, callback);
                    } catch (IOException | RuntimeException e) {
                        if (nonSilentUpdateRequired) {
                            throw new WorkflowFileException(StringUtils.format("Failed to update workflow file: %s. Backup file "
                                + "was generated: %s.", wfFile.getAbsolutePath(), backupFilename), e);
                        } else {
                            throw new WorkflowFileException(StringUtils.format("Failed to update workflow file: %s.",
                                wfFile.getAbsolutePath()), e);
                        }
                    }
                }
            }
            return loadWorkflowDescriptionFromFile(wfFile, callback);
        } catch (IOException e) {
            throw new WorkflowFileException(FAILED_TO_LOAD_WORKFLOW_FILE + wfFile.getAbsolutePath(), e);
        }
    }

    @Override
    public WorkflowDescription loadWorkflowDescriptionFromFileConsideringUpdates(File wfFile,
        WorkflowDescriptionLoaderCallback callback) throws WorkflowFileException {
        // delegate
        return loadWorkflowDescriptionFromFileConsideringUpdates(wfFile, callback, false);
    }

    @Reference
    protected void bindPersistentWorkflowDescriptionUpdateService(PersistentWorkflowDescriptionUpdateService newService) {
        wfUpdateService = newService;
    }

    private int readWorkflowVersionNumber(File wfFile) throws IOException {
        try (InputStream fileInputStream = new FileInputStream(wfFile)) {
            return new WorkflowDescriptionPersistenceHandler().readWorkflowVersionNumber(fileInputStream);
        }
    }

    /**
     * Invokes the update of the workflow description and stores the updated workflow description in the specified file.
     */
    private void updateWorkflow(PersistentWorkflowDescription persWfDescr, File file) throws IOException {
        try (InputStream tempInputStream = IOUtils.toInputStream(wfUpdateService
            .performWorkflowDescriptionUpdate(persWfDescr).getWorkflowDescriptionAsString(), StandardCharsets.UTF_8)) {
            FileUtils.write(file, IOUtils.toString(tempInputStream));
        }
    }

    private void onWorkflowFileUpdated(File wfFile, boolean silentUpdate, String backupFilename,
        WorkflowDescriptionLoaderCallback callback) {
        if (silentUpdate) {
            String message =
                StringUtils.format("'%s' was updated (silently) (full path: %s)", wfFile.getName(), wfFile.getAbsolutePath());
            log.debug(message);
            callback.onSilentWorkflowFileUpdated(message);
        } else {
            String message =
                StringUtils.format("'%s' was updated (non-silently); backup file generated: %s (full path: %s)", wfFile.getName(),
                    backupFilename, wfFile.getAbsolutePath());
            log.debug(message);
            callback.onNonSilentWorkflowFileUpdated(message, backupFilename);
        }
    }

}
