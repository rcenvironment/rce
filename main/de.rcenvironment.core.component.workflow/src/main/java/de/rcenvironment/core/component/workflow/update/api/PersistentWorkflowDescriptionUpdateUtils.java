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
     * @param orignalFilename file name of file to create backup for
     * @return filename for backup file
     */
    public static String getFilenameForBackupFile(String orignalFilename) {
        String filenameWithoutFileExtension = orignalFilename.substring(0, orignalFilename.lastIndexOf("."));
        String backupBasicFilename = filenameWithoutFileExtension + "_backup";
        String backupFilename = backupBasicFilename;
        int i = 1;
        while (new File(backupFilename + ".wf").exists()) {
            backupFilename = backupBasicFilename + " (" + i++ + ")";
        }
        return backupFilename;
    }
}
