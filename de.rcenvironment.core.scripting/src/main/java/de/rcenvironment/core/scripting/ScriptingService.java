/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.scripting;

import javax.script.ScriptEngine;

import de.rcenvironment.core.utils.scripting.ScriptLanguage;
import de.rcenvironment.core.utils.scripting.ScriptLanguage.NoEngineException;

/**
 * Service interface to be implemented by providers providing access to scripting.
 * 
 * @author Christian Weiss
 */
public interface ScriptingService {

    /**
     * Returns whether a {@link ScriptEngine} for the given extension is registered.
     * 
     * @param language the language to be supported
     * @return true, if a {@link ScriptEngine} for the given extension is registered
     */
    boolean supportsScriptLanguage(ScriptLanguage language);

    /**
     * Creates and returns a {@link ScriptEngine} instance handling scripts of the specified
     * {@link ScriptLanguage}.
     * 
     * @param language the {@link ScriptLanguage} to be supported by the returned
     *        {@link ScriptEngine}
     * @return a {@link ScriptEngine} supporting scripts of the specified {@link ScriptLanguage}
     * @throws NoEngineException if no {@link ScriptEngine} is registered, can be checked via
     *         {@link #hasScriptEngine()}
     */
    ScriptEngine createScriptEngine(ScriptLanguage language) throws NoEngineException;

}
