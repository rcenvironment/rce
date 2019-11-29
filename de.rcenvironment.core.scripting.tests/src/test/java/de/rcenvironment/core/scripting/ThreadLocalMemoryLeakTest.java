/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.scripting;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.python.jsr223.PyScriptEngine;
import org.python.jsr223.PyScriptEngineFactory;

/**
 * Test producing a memory leak with Jython 2.5.2+ from: http://bugs.jython.org/issue2026.
 * 
 * @author Doreen Seider
 */
public class ThreadLocalMemoryLeakTest {

    /**
     * Instantiates script engine objects using thread pool where thread local maps get recycled. Intended for manual testing only.
     * 
     * @throws InterruptedException on error
     */
    @Ignore
    @Test
    public void testScriptingLeakDetail() throws InterruptedException {
        final Log log = LogFactory.getLog(ThreadLocalMemoryLeakTest.class);
        
        ExecutorService pool = Executors.newSingleThreadExecutor();

        int i = 0;
        while (true) {
            i++;
            if (i % 10 == 0) {
                log.debug("Iteration: " + i);
            }

            pool.submit(new Runnable() {
                @Override
                public void run() {
                    PyScriptEngineFactory factory = new PyScriptEngineFactory();
                    PyScriptEngine engine = (PyScriptEngine) factory.getScriptEngine();
                    try {
                        engine.eval("s = 'hello' + ' world'");
                    } catch (ScriptException e) {
                        log.error("script execution fails", e);
                    }
                }
            });
            Thread.sleep(10);
        }
    }
    
}
