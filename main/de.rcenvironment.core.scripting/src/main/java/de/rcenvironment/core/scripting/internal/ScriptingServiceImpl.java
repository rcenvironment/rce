/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.scripting.internal;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.python.jsr223.PyScriptEngineFactory;

import de.rcenvironment.core.scripting.ScriptingService;
import de.rcenvironment.core.scripting.python.PythonScriptEngineFactory;
import de.rcenvironment.core.utils.scripting.ScriptLanguage;
import de.rcenvironment.core.utils.scripting.ScriptLanguage.NoEngineException;

/**
 * Default implementation of {@link ScriptingService}.
 * 
 * @author Christian Weiss
 * @author Robert Mischke (make Jython available from OSGi import)
 * @author Sascha Zur (get script engine by name not extention, because jython and python have the
 *         same)
 */
public class ScriptingServiceImpl implements ScriptingService {

    private final ScriptEngineManager engineManager = new ScriptEngineManager();

    public ScriptingServiceImpl() {
        // assertion: Jython should not be registered yet
        if (!supportsScriptLanguage(ScriptLanguage.Jython)) {
            engineManager.registerEngineName(ScriptLanguage.Jython.getName(), new PyScriptEngineFactory());
        }

        if (!supportsScriptLanguage(ScriptLanguage.Python)) {
            engineManager.registerEngineName(ScriptLanguage.Python.getName(), new PythonScriptEngineFactory());
        }
    }

    @Override
    public boolean supportsScriptLanguage(final ScriptLanguage language) {
        final String name = language.getName();
        final boolean result = supportsScriptLanguageByName(name);
        return result;
    }

    @Override
    public ScriptEngine createScriptEngine(final ScriptLanguage language) throws NoEngineException {
        assert supportsScriptLanguage(language);
        final String name = language.getName();
        try {
            final ScriptEngine result = engineManager.getEngineByName(name);
            return result;
        } catch (NoEngineException e) {
            throw new NoEngineException(language, e);
        }
    }

    protected boolean supportsScriptLanguageByName(final String name) {
        final ScriptEngine engine = engineManager.getEngineByName(name);
        final boolean result = engine != null;
        return result;
    }

    protected boolean supportsScriptLanguageExtension(final String extension) {
        final ScriptEngine engine = engineManager.getEngineByExtension(extension);
        final boolean result = engine != null;
        return result;
    }

    protected ScriptEngine createScriptEngineByExtension(final String extension) {
        assert ScriptLanguage.getByExtension(extension) != null;
        assert supportsScriptLanguageExtension(extension);
        final ScriptEngine result = engineManager.getEngineByExtension(extension);
        if (result == null) {
            final ScriptLanguage language = ScriptLanguage.getByExtension(extension);
            throw new NoEngineException(language);
        }
        return result;
    }

    protected ScriptEngine createScriptEngineByName(final String name) {
        assert ScriptLanguage.getByName(name) != null;
        assert supportsScriptLanguageByName(name);
        final ScriptEngine result = engineManager.getEngineByName(name);
        if (result == null) {
            final ScriptLanguage language = ScriptLanguage.getByName(name);
            throw new NoEngineException(language);
        }
        return result;
    }
}
