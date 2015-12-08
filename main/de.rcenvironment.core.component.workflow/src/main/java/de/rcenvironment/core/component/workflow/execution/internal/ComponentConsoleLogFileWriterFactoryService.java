/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;


/**
 * Creates {@link ComponentsConsoleLogFileWriter} instances. One instance is responsible for all of the component's log of one workflow.
 * 
 * @author Doreen Seider
 */
public interface ComponentConsoleLogFileWriterFactoryService {

    /**
     * Creates {@link ComponentsConsoleLogFileWriter} instance, which handles the console log files of all the given components.
     * 
     * @param wfDataStorageBridge {@link WorkflowExecutionStorageBridge} instance related to the components' workflow
     * @return new {@link ComponentsConsoleLogFileWriter} instance
     */
    ComponentsConsoleLogFileWriter createComponentConsoleLogFileWriter(WorkflowExecutionStorageBridge wfDataStorageBridge);
    
}
