/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.model.api;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.communication.testutils.PlatformServiceDefaultStub;

/**
 * Creates a {@link WorkflowDescriptionPersistenceHandler} instances and injects a {@link PlatformService} default test instance.
 * 
 * @author Doreen Seider
 */
public final class WorkflowDescriptionPersistenceHandlerTestUtils {

    private WorkflowDescriptionPersistenceHandlerTestUtils() {}
    
    /**
     * Creates {@link WorkflowDescriptionPersistenceHandler} instance and injects a {@link PlatformService} default test instance.
     * 
     * @return {@link WorkflowDescriptionPersistenceHandler} instance with {@link PlatformService} injected
     */
    public static WorkflowDescriptionPersistenceHandler createWorkflowDescriptionPersistenceHandlerTestInstance() {
        WorkflowDescriptionPersistenceHandler handler = new WorkflowDescriptionPersistenceHandler();
        handler.bindPlatformService(new PlatformServiceDefaultStub() {
            private LogicalNodeId nodeId = NodeIdentifierTestUtils.createTestDefaultLogicalNodeId();
            
            @Override
            public LogicalNodeId getLocalDefaultLogicalNodeId() {
                return nodeId;
            }
            
        });
        return handler;
    }
    
    /**
     * Sets a {@link PlatformService} default test instance as the static field of the {@link WorkflowDescriptionPersistenceHandler}.
     */
    public static void initializeStaticFieldsOfWorkflowDescriptionPersistenceHandler() {
        createWorkflowDescriptionPersistenceHandlerTestInstance();
    }
    
    
    
}
