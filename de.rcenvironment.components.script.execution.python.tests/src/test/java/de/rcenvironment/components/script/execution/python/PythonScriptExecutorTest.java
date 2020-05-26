/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.script.execution.python;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import javax.script.ScriptEngine;

import org.easymock.EasyMock;
import org.junit.Before;

import de.rcenvironment.components.script.execution.python.internal.PythonScriptExecutor;
import de.rcenvironment.components.script.execution.testutils.ScriptExecutorTest;
import de.rcenvironment.core.component.testutils.ComponentContextMock;
import de.rcenvironment.core.scripting.python.PythonScriptEngine;
import de.rcenvironment.core.utils.executor.LocalApacheCommandLineExecutor;
import de.rcenvironment.core.utils.scripting.ScriptLanguage;

/**
 * 
 * @author Sascha Zur
 */
public class PythonScriptExecutorTest extends ScriptExecutorTest {

    /**
     * Common setup.
     * 
     * @throws IOException e
     */
    @Before
    public void setUp() throws IOException {
        context = new ComponentContextMock();
        executor = new PythonScriptExecutor();
    }

    @Override
    protected ScriptLanguage getScriptLanguage() {
        return ScriptLanguage.Python;
    }

    @Override
    protected void testPrepareHook() {

    }

    @Override
    protected ScriptEngine getScriptingEngine() {
        PythonScriptEngine pythonScriptEngine = EasyMock.createNiceMock(PythonScriptEngine.class);
        EasyMock.expect(pythonScriptEngine.getCloseOutputChannelsList()).andReturn(new LinkedList<String>());
        LocalApacheCommandLineExecutor executor = EasyMock.createNiceMock(LocalApacheCommandLineExecutor.class);
        EasyMock.expect(executor.getWorkDir()).andReturn(new File("")).anyTimes();
        EasyMock.replay(executor);
        EasyMock.expect(pythonScriptEngine.getExecutor()).andReturn(executor).anyTimes();
        return pythonScriptEngine;
    }
}
