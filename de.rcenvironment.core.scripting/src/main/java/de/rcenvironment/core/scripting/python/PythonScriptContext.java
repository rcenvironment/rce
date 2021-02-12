/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.scripting.python;

import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;

/**
 * Implementation of {@link ScriptContext} for the PythonScriptEngine.
 * 
 * @author Sascha Zur
 */
public class PythonScriptContext implements ScriptContext {

    private Map<String, Object> attributes = new HashMap<String, Object>();

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Object getAttribute(String name, int scope) {
        return null;
    }

    @Override
    public int getAttributesScope(String arg0) {
        return 0;
    }

    @Override
    public Bindings getBindings(int arg0) {
        return null;
    }

    @Override
    public Writer getErrorWriter() {
        return null;
    }

    @Override
    public Reader getReader() {
        return null;
    }

    @Override
    public List<Integer> getScopes() {
        return null;
    }

    @Override
    public Writer getWriter() {
        return null;
    }

    @Override
    public Object removeAttribute(String arg0, int arg1) {
        return null;
    }

    @Override
    public void setAttribute(String name, Object value, int scope) {
        attributes.put(name, value);
    }

    @Override
    public void setBindings(Bindings arg0, int arg1) {
    }

    @Override
    public void setErrorWriter(Writer arg0) {
    }

    @Override
    public void setReader(Reader arg0) {
    }

    @Override
    public void setWriter(Writer arg0) {
    }

}
