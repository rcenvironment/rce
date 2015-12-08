/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.update.api;

import java.io.File;

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
        String backupBasicFilename = filenameWithoutFileExtension + "_backup";
        String backupFilename = backupBasicFilename;
        int i = 1;
        while (new File(orignalFile.getParentFile(), backupFilename + ".wf").exists()) {
            backupFilename = backupBasicFilename + " (" + i++ + ")";
        }
        return backupFilename;
    }
}
