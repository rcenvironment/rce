/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.headless.internal;

import java.io.File;
import java.io.FilenameFilter;

import de.rcenvironment.core.component.execution.api.SingleConsoleRowsProcessor;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionService.DeletionBehavior;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionService.DisposalBehavior;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * Implementation of {@link HeadlessWorkflowExecutionContext}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (minor change)
 */
public class HeadlessWorkflowExecutionContextImpl implements HeadlessWorkflowExecutionContext {

    private File wfFile;

    private File placeholdersFile;

    private File logDirectory;

    private TextOutputReceiver textOutputReceiver;

    private SingleConsoleRowsProcessor singleConsoleRowsProcessor;

    private DisposalBehavior disposeBehavior = DisposalBehavior.OnExpected;

    private DeletionBehavior deletionBehavior = DeletionBehavior.OnExpected;

    private boolean abortIfWorkflowUpdateRequired = false;

    private boolean isCompactOutput = false;

    @Override
    public File getWorkflowFile() {
        return wfFile;
    }

    @Override
    public File getPlaceholdersFile() {
        return placeholdersFile;
    }

    @Override
    public File getLogDirectory() {
        return logDirectory;
    }
    
    @Override
    public File[] getLogFiles() {
        return getLogDirectory().listFiles(new FilenameFilter() {
            
            
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("workflow.log");
            }
        });
    }

    @Override
    public TextOutputReceiver getTextOutputReceiver() {
        return textOutputReceiver;
    }

    @Override
    public SingleConsoleRowsProcessor getSingleConsoleRowReceiver() {
        return singleConsoleRowsProcessor;
    }

    @Override
    public DisposalBehavior getDisposalBehavior() {
        return disposeBehavior;
    }

    @Override
    public boolean shouldAbortIfWorkflowUpdateRequired() {
        return abortIfWorkflowUpdateRequired;
    }

    @Override
    public boolean isCompactOutput() {
        return isCompactOutput;
    }

    @Override
    public DeletionBehavior getDeletionBehavior() {
        return deletionBehavior;
    }

    public void setWfFile(File wfFile) {
        this.wfFile = wfFile;
    }

    public void setPlaceholdersFile(File placeholdersFile) {
        this.placeholdersFile = placeholdersFile;
    }

    public void setLogDirectory(File logDirectory) {
        this.logDirectory = logDirectory;
    }

    public void setTextOutputReceiver(TextOutputReceiver textOutputReceiver) {
        this.textOutputReceiver = textOutputReceiver;
    }

    public void setSingleConsoleRowsProcessor(SingleConsoleRowsProcessor singleConsoleRowsProcessor) {
        this.singleConsoleRowsProcessor = singleConsoleRowsProcessor;
    }

    public void setDisposeBehavior(DisposalBehavior disposeBehavior) {
        this.disposeBehavior = disposeBehavior;
    }

    public void setDeletionBehavior(DeletionBehavior delete) {
        this.deletionBehavior = delete;
    }

    public void setAbortIfWorkflowUpdateRequired(boolean abortIfWorkflowUpdateRequired) {
        this.abortIfWorkflowUpdateRequired = abortIfWorkflowUpdateRequired;
    }

    public void setIsCompactOutput(boolean isCompactOutput) {
        this.isCompactOutput = isCompactOutput;
    }

}
