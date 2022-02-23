/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.update.api;

import java.io.File;

import de.rcenvironment.core.component.workflow.api.WorkflowConstants;

/**
 * Provides convenient methods.
 * 
 * @author Doreen Seider
 */
public final class PersistentWorkflowDescriptionUpdateUtils {

    private PersistentWorkflowDescriptionUpdateUtils() {}
    
    /**
     * @param orignalFile file to create backup for
     * @return filename for backup file
     */
    public static String getFilenameForBackupFile(File orignalFile) {
        String filenameWithoutFileExtension = orignalFile.getName().substring(0, orignalFile.getName().lastIndexOf("."));
        String backupBasicFilename = filenameWithoutFileExtension + WorkflowConstants.WORKFLOW_FILE_BACKUP_SUFFIX;
        String backupFilename = backupBasicFilename;
        int i = 1;
        while (new File(orignalFile.getParentFile(), backupFilename + WorkflowConstants.WORKFLOW_FILE_ENDING).exists()) {
            backupFilename = backupBasicFilename + " (" + i++ + ")";
        }
        return backupFilename;
    }
}
