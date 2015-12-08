/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;


/**
 * Implementation of {@link ComponentConsoleLogFileWriterFactoryService}.
 * 
 * @author Doreen Seider
 */
public class ComponentConsoleLogFileWriterFactoryServiceImpl implements ComponentConsoleLogFileWriterFactoryService {

    @Override
    public ComponentsConsoleLogFileWriter createComponentConsoleLogFileWriter(WorkflowExecutionStorageBridge wfDataStorageBridge) {
        return new ComponentsConsoleLogFileWriter(wfDataStorageBridge);
    }
    
}
