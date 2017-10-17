/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.script.execution.jython;

import java.io.File;
import java.io.IOException;

import javax.script.ScriptEngine;

import org.easymock.EasyMock;
import org.junit.Before;

import de.rcenvironment.components.script.execution.testutils.ScriptExecutorTest;
import de.rcenvironment.core.component.testutils.ComponentContextMock;
import de.rcenvironment.core.scripting.ScriptingUtils;
import de.rcenvironment.core.utils.scripting.ScriptLanguage;

/**
 * 
 * @author Sascha Zur
 */
public class JythonScriptExecutorTest extends ScriptExecutorTest {

    /**
     * Common setup.
     * 
     * @throws IOException e
     */
    @Before
    public void setUp() throws IOException {
        context = new ComponentContextMock(); 
        executor = new JythonScriptExecutor();
    }

    @Override
    protected void testPrepareHook() {
        // TODO this is not a valid Jython location. Why is this working?
        ScriptingUtils.setJythonPath(new File(""));
    }

    @Override
    protected ScriptLanguage getScriptLanguage() {
        return ScriptLanguage.Jython;
    }

    @Override
    protected ScriptEngine getScriptingEngine() {
        ScriptEngine engineMock = EasyMock.createNiceMock(ScriptEngine.class);
        return engineMock;
    }

}
