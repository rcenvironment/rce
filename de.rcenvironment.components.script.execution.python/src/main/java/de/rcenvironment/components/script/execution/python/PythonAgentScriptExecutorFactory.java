/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.script.execution.python;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.components.script.common.pythonAgentInstanceManager.PythonAgentInstanceManager;
import de.rcenvironment.components.script.common.registry.ScriptExecutor;
import de.rcenvironment.components.script.common.registry.ScriptExecutorFactory;
import de.rcenvironment.components.script.execution.python.internal.PythonAgentScriptExecutor;
import de.rcenvironment.core.utils.scripting.ScriptLanguage;

/**
 * Factory to create {@link PythonAgentScriptExecutor} objects.
 * 
 * @author Adrian Stock
 *
 */
@Component
public class PythonAgentScriptExecutorFactory implements ScriptExecutorFactory {

    private PythonAgentInstanceManager pythonInstanceManager;

    @Override
    public ScriptLanguage getSupportingScriptLanguage() {
        return ScriptLanguage.PythonExp;
    }

    @Override
    public ScriptExecutor createScriptExecutor() {
        return new PythonAgentScriptExecutor(pythonInstanceManager);
    }

    /**
     * Binds the {@linkPythonInstanceManager} so that the {@linkPythonAgentScriptExecutor} objects created within this factory
     * have access to the python instances.
     * 
     * @param newInstance of the PythonInstanceManager.
     */
    @Reference
    public void bindPythonInstanceManager(PythonAgentInstanceManager newInstance) {
        this.pythonInstanceManager = newInstance;
    }

    /**
     * Unbinds the {@linkPythonInstanceManager}.
     * 
     * @param oldInstance which will be unbound.
     */
    public void unbindPythonInstanceManager(PythonAgentInstanceManager oldInstance) {
        this.pythonInstanceManager = null;
    }
}
