/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.scripting.python;

import org.junit.Test;

/**
 * Test for {@link PythonScriptEngine}.
 * 
 * @author Tobias Brieden
 */
public class PythonScriptEngineTest {

    /**
     * Tests if the execution can be canceled before a call to createNewExecutor.
     */
    @Test
    public void testCancelBeforeCreateNewExecutor() {
        PythonScriptEngine engine = new PythonScriptEngine();
        engine.cancel();
        engine.createNewExecutor(null);
    }
}
