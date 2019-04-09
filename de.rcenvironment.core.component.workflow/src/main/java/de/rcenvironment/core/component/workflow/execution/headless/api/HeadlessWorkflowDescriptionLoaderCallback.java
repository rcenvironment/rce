/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.headless.api;

import de.rcenvironment.core.component.workflow.execution.spi.WorkflowDescriptionLoaderCallback;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * Headless-specific implementation of {@link WorkflowDescriptionLoaderCallback}.
 * 
 * @author Doreen Seider
 */
public class HeadlessWorkflowDescriptionLoaderCallback implements WorkflowDescriptionLoaderCallback {

    private TextOutputReceiver outputReceiver;
    
    public HeadlessWorkflowDescriptionLoaderCallback(TextOutputReceiver outputReceiver) {
        this.outputReceiver = outputReceiver;
    }

    @Override
    public void onNonSilentWorkflowFileUpdated(String message, String backupFilename) {
        outputReceiver.addOutput(message);
    }

    @Override
    public void onSilentWorkflowFileUpdated(String message) {
        outputReceiver.addOutput(message);
    }

    @Override
    public void onWorkflowFileParsingPartlyFailed(String backupFilename) {}

    @Override
    public boolean arePartlyParsedWorkflowConsiderValid() {
        return false;
    }

}
