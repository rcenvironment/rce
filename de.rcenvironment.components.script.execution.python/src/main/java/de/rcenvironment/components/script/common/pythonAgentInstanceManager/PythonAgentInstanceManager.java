/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.script.common.pythonAgentInstanceManager;

import java.io.IOException;

import de.rcenvironment.components.script.common.pythonAgentInstanceManager.internal.PythonAgent;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.utils.executor.LocalApacheCommandLineExecutor;

/**
 * 
 * Service to manage python instances which will be used to execute python scripts during the workflow.
 * 
 * @author Adrian Stock
 *
 */
public interface PythonAgentInstanceManager {
    
    /**
     * Starts a python instance with the given installation path.
     * 
     * @param pythonInstallationPath which will be used to start the instance.
     * @param compCtx to show the output of the python agent on the console (will be removed later).
     * @return a {@link PythonAgent} which communicates with the python instance.
     * @throws IOException if the agent couldn't be initialized.
     */
    PythonAgent getAgent(String pythonInstallationPath, ComponentContext compCtx) throws IOException;
    
    /**
     * If the object calling this method was the last user of the given agent, the agent is shut down. Otherwise, its usage counter is
     * decremented. The caller can determine which of these cases applies via the return argument.
     * 
     * @param agent which communicates with the instance which shall be shut down.
     * @return true if the agent was indeed stopped, false if the agent is still in use by some other client of this manager
     */
    boolean stopAgent(PythonAgent agent);
    
    /**
     * Creates a new executor to start threads.
     * 
     * @return executor.
     */
    LocalApacheCommandLineExecutor createNewExecutor();
}
