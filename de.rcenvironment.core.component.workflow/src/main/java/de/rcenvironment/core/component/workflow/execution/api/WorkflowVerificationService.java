/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.workflow.execution.api;

import java.io.File;

import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

public interface WorkflowVerificationService {

    boolean preValidateWorkflow(TextOutputReceiver outputReceiver, File wfFile, boolean printNonErrorProgressMessages);

    WorkflowVerificationBuilder getVerificationBuilder();

}
