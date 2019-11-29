/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.script.common.registry;

import de.rcenvironment.core.utils.scripting.ScriptLanguage;

/**
 * Interface for a factory, that registers a new Script Executor for a specific script language.
 * 
 * @author Sascha Zur
 */
public interface ScriptExecutorFactory {

    /**
     * Returns the language this factory creates executors for.
     * 
     * @return instance of ScriptExecutor
     */
    ScriptLanguage getSupportingScriptLanguage();

    /**
     * Returns an executor for the script language that factory is for.
     * 
     * @return instance of executor instance
     */
    ScriptExecutor createScriptExecutor();
}
