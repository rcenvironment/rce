/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.spi;


/**
 * Callback interface used to announce certain events when loading a workflow file.
 * 
 * @author Doreen Seider
 */
public interface WorkflowDescriptionLoaderCallback {

    /**
     * Called if the workflow file was updated non-silently (this might include silent update).
     * 
     * @param message the message describing the error. Can be re-used for other output receivers like the command console or gui dialogs
     * @param backupFilename the name of the backup file, which is always created for non-silent workflow file updates
     */
    void onNonSilentWorkflowFileUpdated(String message, String backupFilename);
    
    /**
     * Called if the workflow file was updated silently.
     * 
     * @param message the message describing the error. Can be re-used for other output receivers like the command console or gui dialogs
     */
    void onSilentWorkflowFileUpdated(String message);
    
    /**
     * Called if parsing the final (after updates are applied) workflow file failed for some parts and if it is still a valid but reduced
     * workflow.
     * 
     * @param backupFilename the name of the backup file, which is always created for non-silent workflow file updates
     */
    void onWorkflowFileParsingPartlyFailed(String backupFilename);
    
    /**
     * @return <code>true</code> if workflows that could not be parsed successfully (some parts are skipped) are considered valid
     */
    boolean arePartlyParsedWorkflowConsiderValid(); // TODO no actual callback method; should be a parameter (next to the callback) passed
                                                    // to the method
    
}
