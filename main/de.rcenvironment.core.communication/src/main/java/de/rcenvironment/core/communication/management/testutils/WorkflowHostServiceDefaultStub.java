/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.communication.management.testutils;

import java.util.HashSet;
import java.util.Set;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.management.WorkflowHostService;

/**
 * Default stub for {@link WorkflowHostService}.
 * 
 * This class (and subclasses of it) is intended for test scenarios where an instance of
 * {@link WorkflowHostService} is required, but where the exact calls to this instance are not relevant.
 * If they are relevant and should be tested, create a mock instance instead (for example, with the
 * EasyMock library).
 * 
 * @author Doreen Seider
 */
public class WorkflowHostServiceDefaultStub implements WorkflowHostService {

    @Override
    public Set<NodeIdentifier> getWorkflowHostNodes() {
        return new HashSet<>();
    }

    @Override
    public Set<NodeIdentifier> getWorkflowHostNodesAndSelf() {
        return new HashSet<>();
    }

}
