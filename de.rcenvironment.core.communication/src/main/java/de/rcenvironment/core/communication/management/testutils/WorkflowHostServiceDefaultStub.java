/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.management.testutils;

import java.util.HashSet;
import java.util.Set;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.management.WorkflowHostService;

/**
 * Default stub for {@link WorkflowHostService}.
 * 
 * This class (and subclasses of it) is intended for test scenarios where an instance of {@link WorkflowHostService} is required, but where
 * the exact calls to this instance are not relevant. If they are relevant and should be tested, create a mock instance instead (for
 * example, with the EasyMock library).
 * 
 * @author Doreen Seider
 * @author Robert Mischke (8.0.0 id adaptations)
 */
public class WorkflowHostServiceDefaultStub implements WorkflowHostService {

    @Override
    public Set<InstanceNodeSessionId> getWorkflowHostNodes() {
        return new HashSet<>();
    }

    @Override
    public Set<LogicalNodeId> getLogicalWorkflowHostNodes() {
        return new HashSet<>();
    }

    @Override
    public Set<InstanceNodeSessionId> getWorkflowHostNodesAndSelf() {
        return new HashSet<>();
    }

    @Override
    public Set<LogicalNodeId> getLogicalWorkflowHostNodesAndSelf() {
        return new HashSet<>();
    }
}
