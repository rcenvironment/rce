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
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * Encapsulates data used for headless workflow execution.
 * 
 * @author Doreen Seider
 */
public interface HeadlessWorkflowExecutionContext {

    /**
     * @return the workflow file
     */
    File getWorkflowFile();

    /**
     * @return JSON file containing componentId->(key->value) placeholder values. Is optional and
     *         can be <code>null</code>
     */
    File getPlaceholdersFile();

    /**
     * @return location for workflow log files. Is optional and can be <code>null</code>
     */
    File getLogDirectory();

    /**
     * @return the {@link TextOutputReceiver} to write status messages to. Is optional and can be
     *         <code>null</code>
     */
    TextOutputReceiver getTextOutputReceiver();

    /**
     * @return listener for all received {@link ConsoleRow}s. Is optional and can be
     *         <code>null</code>
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
     * @return <code>true</code> if output should be compact. Default is <code>false</code>
     */
    boolean isCompactOutput();

}
