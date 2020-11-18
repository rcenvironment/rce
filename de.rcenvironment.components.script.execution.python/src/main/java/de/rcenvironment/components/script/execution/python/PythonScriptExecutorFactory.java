/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.script.execution.python;

import de.rcenvironment.components.script.common.registry.ScriptExecutor;
import de.rcenvironment.components.script.common.registry.ScriptExecutorFactory;
import de.rcenvironment.components.script.execution.python.internal.PythonScriptExecutor;
import de.rcenvironment.core.utils.scripting.ScriptLanguage;

/**
 * Factory for the Python script language executor.
 * 
 * @author Sascha Zur
 */
public class PythonScriptExecutorFactory implements ScriptExecutorFactory{

    public ScriptLanguage getSupportingScriptLanguage() {
        return ScriptLanguage.Python;
    }

    public ScriptExecutor createScriptExecutor() {
        return new PythonScriptExecutor();
    }

}
