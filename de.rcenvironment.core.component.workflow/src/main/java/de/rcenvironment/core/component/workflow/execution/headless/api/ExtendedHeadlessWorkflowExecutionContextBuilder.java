/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.execution.headless.api;

import java.io.File;

import de.rcenvironment.core.component.workflow.execution.headless.internal.ExtendedHeadlessWorkflowExecutionContext;
import de.rcenvironment.core.utils.common.InvalidFilenameException;

/**
 * Build {@link HeadlessWorkflowExecutionContext} instances for remote access service.
 *
 * @author Brigitte Boden
 */
public class ExtendedHeadlessWorkflowExecutionContextBuilder extends HeadlessWorkflowExecutionContextBuilder {

    public ExtendedHeadlessWorkflowExecutionContextBuilder(File wfFile, File logDirectory) throws InvalidFilenameException {
        super(wfFile, logDirectory);
    }

    @Override
    public HeadlessWorkflowExecutionContext build() {
        return new ExtendedHeadlessWorkflowExecutionContext(super.build());
    }

    
}
