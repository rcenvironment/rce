/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.headless.api;

import java.io.File;

import de.rcenvironment.core.component.execution.api.SingleConsoleRowsProcessor;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionService.DeletionBehavior;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionService.DisposalBehavior;
import de.rcenvironment.core.component.workflow.execution.headless.internal.HeadlessWorkflowExecutionContextImpl;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * Build {@link HeadlessWorkflowExecutionContext} instances.
 * 
 * @author Doreen Seider
 * 
 */
public final class HeadlessWorkflowExecutionContextBuilder {

    private HeadlessWorkflowExecutionContextImpl headlessWfExeCtx;

    /**
     * @param wfFile workflow file
     * @param logDirectory set the location for workflow log files
     */
    public HeadlessWorkflowExecutionContextBuilder(File wfFile, File logDirectory) {
        headlessWfExeCtx = new HeadlessWorkflowExecutionContextImpl();
        headlessWfExeCtx.setWfFile(wfFile);
        headlessWfExeCtx.setLogDirectory(logDirectory);
    }

    /**
     * @param placeholdersFile an optional JSON file containing componentId->(key->value)
     *        placeholder values; pass <code>null</code> if no one to consider
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
     * @param isCompactOutput <code>true</code> if output should be compact. Default is
     *        <code>false</code>
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
     * @param disposalBehavior the {@link DisposalBehavior}. Default is
     *        {@link DisposalBehavior#OnFinished}
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
     * @return {@link HeadlessWorkflowExecutionContext} to support method chaining
     */
    public HeadlessWorkflowExecutionContext build() {
        return headlessWfExeCtx;
    }

}
