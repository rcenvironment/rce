/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.script.common.registry;

import java.util.List;

import de.rcenvironment.core.utils.scripting.ScriptLanguage;

/**
 * Registry for receiving executors for different scripting languages.
 * 
 * @author Sascha Zur
 */
public interface ScriptExecutorFactoryRegistry {

    /**
     * Adds the given {@link ScriptExecutorFactory} to the list of all executors registered.
     * 
     * @param factory new factory
     */
    void addScriptExecutorFactory(ScriptExecutorFactory factory);

    /**
     * Removes the given {@link ScriptExecutorFactory} from the list of all factories registered.
     * 
     * @param algFactory to remove
     */
    void removeScriptExecutorFactory(ScriptExecutorFactory algFactory);

    /**
     * Returns an executor for the script language that factory is for.
     * 
     * @param language the executor must provide
     * @return executor instance
     */
    ScriptExecutor requestScriptExecutor(ScriptLanguage language);

    /**
     * Returns a list with all current registered executor languages, used to fill the gui.
     * 
     * @return list of string with script language names
     */
    List<ScriptLanguage> getCurrentRegisteredExecutorLanguages();

}
