/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.headless.api;

import java.io.File;

import de.rcenvironment.core.component.execution.api.SingleConsoleRowsProcessor;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionService.DeletionBehavior;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionService.DisposalBehavior;
import de.rcenvironment.core.component.workflow.execution.headless.internal.ExtendedHeadlessWorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.headless.internal.HeadlessWorkflowExecutionContextImpl;
import de.rcenvironment.core.utils.common.CrossPlatformFilenameUtils;
import de.rcenvironment.core.utils.common.InvalidFilenameException;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * Build {@link HeadlessWorkflowExecutionContext} instances.
 * 
 * @author Doreen Seider
 * 
 */
public class HeadlessWorkflowExecutionContextBuilder {

    private HeadlessWorkflowExecutionContextImpl headlessWfExeCtx;

    /**
     * @param wfFile workflow file
     * @param logDirectory set the location for workflow log files
     * @throws InvalidFilenameException 
     */
    public HeadlessWorkflowExecutionContextBuilder(File wfFile) throws InvalidFilenameException {
        headlessWfExeCtx = new HeadlessWorkflowExecutionContextImpl();
        headlessWfExeCtx.setWfFile(wfFile);
        CrossPlatformFilenameUtils.throwExceptionIfFilenameNotValid(wfFile.getName());
    }
    
    public HeadlessWorkflowExecutionContextBuilder setLogDirectory(File logDirectory) {
        headlessWfExeCtx.setLogDirectory(logDirectory);
        return this;
    }

    /**
     * @param placeholdersFile an optional JSON file containing componentId->(key->value) placeholder values; pass <code>null</code> if no
     *        one to consider
     * @return {@link HeadlessWorkflowExecutionContext} to support method chaining
     */
    public HeadlessWorkflowExecutionContextBuilder setPlaceholdersFile(File placeholdersFile) {
        headlessWfExeCtx.setPlaceholdersFile(placeholdersFile);
        return this;
    }

    /**
     * @param outputReceiver an optional {@link TextOutputReceiver} to write status messages to
     * @return {@link HeadlessWorkflowExecutionContext} to support method chaining
     */
    public HeadlessWorkflowExecutionContextBuilder setTextOutputReceiver(TextOutputReceiver outputReceiver) {
        headlessWfExeCtx.setTextOutputReceiver(outputReceiver);
        return this;
    }

    /**
     * @param outputReceiver an optional {@link TextOutputReceiver} to write status messages to
     * @param isCompactOutput <code>true</code> if output should be compact. Default is <code>false</code>
     * @return {@link HeadlessWorkflowExecutionContext} to support method chaining
     */
    public HeadlessWorkflowExecutionContextBuilder setTextOutputReceiver(TextOutputReceiver outputReceiver, boolean isCompactOutput) {
        headlessWfExeCtx.setTextOutputReceiver(outputReceiver);
        headlessWfExeCtx.setIsCompactOutput(isCompactOutput);
        return this;
    }

    /**
     * @param consoleRowsProcessor an optional listener for all received ConsoleRows
     * @return {@link HeadlessWorkflowExecutionContext} to support method chaining
     */
    public HeadlessWorkflowExecutionContextBuilder setSingleConsoleRowsProcessor(SingleConsoleRowsProcessor consoleRowsProcessor) {
        headlessWfExeCtx.setSingleConsoleRowsProcessor(consoleRowsProcessor);
        return this;
    }

    /**
     * @param disposalBehavior the {@link DisposalBehavior}. Default is {@link DisposalBehavior#OnFinished}
     * @return {@link HeadlessWorkflowExecutionContext} to support method chaining
     */
    public HeadlessWorkflowExecutionContextBuilder setDisposalBehavior(DisposalBehavior disposalBehavior) {
        headlessWfExeCtx.setDisposeBehavior(disposalBehavior);
        return this;
    }

    /**
     * @param delete the {@link DeletionBehavior}. Default is {@link DeletionBehavior#OnFinished}
     * @return {@link HeadlessWorkflowExecutionContext} to support method chaining
     */
    public HeadlessWorkflowExecutionContextBuilder setDeletionBehavior(DeletionBehavior delete) {
        headlessWfExeCtx.setDeletionBehavior(delete);
        return this;
    }

    /**
     * @param value set to true to refuse execution of workflow files that would need to be updated first
     */
    public void setAbortIfWorkflowUpdateRequired(boolean value) {
        headlessWfExeCtx.setAbortIfWorkflowUpdateRequired(value);
    }

    /**
     * @return An {@link HeadlessWorkflowExecutionContext} as configured by preceding method calls
     */
    public HeadlessWorkflowExecutionContext build() {
        return headlessWfExeCtx;
    }
    
    /**
     * @return An {@link ExtendedHeadlessWorkflowExecutionContext} as configured by preceding method calls
     */
    public ExtendedHeadlessWorkflowExecutionContext buildExtended() {
        final HeadlessWorkflowExecutionContext parent = this.build();
        
        final ExtendedHeadlessWorkflowExecutionContext retval = new ExtendedHeadlessWorkflowExecutionContext();

        retval.setWfFile(parent.getWorkflowFile());
        retval.setLogDirectory(parent.getLogDirectory());
        retval.setPlaceholdersFile(parent.getPlaceholdersFile());
        retval.setTextOutputReceiver(parent.getTextOutputReceiver());
        retval.setIsCompactOutput(parent.isCompactOutput());
        retval.setSingleConsoleRowsProcessor(parent.getSingleConsoleRowReceiver());
        retval.setDisposeBehavior(parent.getDisposalBehavior());
        retval.setDeletionBehavior(parent.getDeletionBehavior());
        retval.setAbortIfWorkflowUpdateRequired(parent.shouldAbortIfWorkflowUpdateRequired());

        return retval;
    }

}
