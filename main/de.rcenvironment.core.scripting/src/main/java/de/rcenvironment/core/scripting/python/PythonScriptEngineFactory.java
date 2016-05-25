/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.scripting.python;

import java.util.LinkedList;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import de.rcenvironment.core.utils.scripting.ScriptLanguage;

/**
 * Implementation of {@link ScriptEngineFactory} for providing a {@link PythonScriptEngine}.
 * 
 * @author Sascha Zur
 */
public class PythonScriptEngineFactory implements ScriptEngineFactory {

    @Override
    public String getEngineName() {
        return ScriptLanguage.Python.toString();
    }

    @Override
    public String getEngineVersion() {
        return "1.0";
    }

    @Override
    public List<String> getExtensions() {
        List<String> extensionList = new LinkedList<String>();
        extensionList.add(".py");
        extensionList.add(".pyc");
        return extensionList;
    }

    @Override
    public String getLanguageName() {
        return ScriptLanguage.Python.toString();
    }

    @Override
    public String getLanguageVersion() {
        return "1.0";
    }

    @Override
    public String getMethodCallSyntax(String arg0, String arg1, String... arg2) {
        return null;
    }

    @Override
    public List<String> getMimeTypes() {
        return null;
    }

    @Override
    public List<String> getNames() {
        return null;
    }

    @Override
    public String getOutputStatement(String arg0) {
        return null;
    }

    @Override
    public Object getParameter(String arg0) {
        return null;
    }

    @Override
    public String getProgram(String... arg0) {
        return null;
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return new PythonScriptEngine();
    }

}
