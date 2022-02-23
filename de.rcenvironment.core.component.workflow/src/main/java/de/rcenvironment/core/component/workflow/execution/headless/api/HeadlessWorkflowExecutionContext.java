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
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * Encapsulates data used for headless workflow execution.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (added fail-on-update flag)
 */
public interface HeadlessWorkflowExecutionContext {

    /**
     * @return the workflow file
     */
    File getWorkflowFile();

    /**
     * @return JSON file containing componentId->(key->value) placeholder values. Is optional and can be <code>null</code>
     */
    File getPlaceholdersFile();

    /**
     * @return location for workflow log files. Is optional and can be <code>null</code>
     */
    File getLogDirectory();

    /**
     * @return aggregated workflow log files (temporary and final ones if existent). Is optional and can be <code>null</code>
     */
    File[] getLogFiles();

    /**
     * @return the {@link TextOutputReceiver} to write status messages to. Is optional and can be <code>null</code>
     */
    TextOutputReceiver getTextOutputReceiver();

    /**
     * @return listener for all received {@link ConsoleRow}s. Is optional and can be <code>null</code>
     */
    SingleConsoleRowsProcessor getSingleConsoleRowReceiver();

    /**
     * @return {@link DisposalBehavior}
     */
    DisposalBehavior getDisposalBehavior();

    /**
     * @return {@link DeletionBehavior}
     */
    DeletionBehavior getDeletionBehavior();

    /**
     * Enables a fail-fast mechanism for workflow files generated from internal templates, where a required update indicates that the
     * template is out of date and should be updated instead.
     * 
     * @return true if workflow execution should abort if the workflow file would require an update
     */
    boolean shouldAbortIfWorkflowUpdateRequired();

    /**
     * @return <code>true</code> if output should be compact. Default is <code>false</code>
     */
    boolean isCompactOutput();

}
