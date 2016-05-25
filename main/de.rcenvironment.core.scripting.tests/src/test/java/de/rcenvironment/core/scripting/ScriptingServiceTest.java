/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.scripting;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.rcenvironment.core.utils.scripting.ScriptLanguage;

/**
 * Test for {@link ScriptingService}.
 *
 * @author Christian Weiss
 * @author Robert Mischke (made JavaScript and Jython support mandatory)
 */
public abstract class ScriptingServiceTest {

    private static ScriptLanguage[] languages;

    private ScriptingService service;
    
    private final Log log = LogFactory.getLog(getClass());
    
    protected abstract ScriptingService getService();
    
    /** BeforeClass. */
    @BeforeClass
    public static void setupBeforeClass() {
        languages = ScriptLanguage.values();
    }

    /** Before. */
    @Before
    public void setUp() {
        service = getService();
    }

    /** Test. */
    @Test
    public void testSupportsScriptLanguages() {
        for (final ScriptLanguage language : languages) {
            Assert.assertTrue("No support for script language " + language.getName(), service.supportsScriptLanguage(language));
        }
    }

    /** Test. */
    @Test
    public void testCreateScriptEngines() {
        for (final ScriptLanguage language : languages) {
            if (service.supportsScriptLanguage(language)) {
                final ScriptEngine engine = service.createScriptEngine(language);
                Assert.assertNotNull(engine);
            } else {
                // this will fail the "testSupportsScriptLanguage" test already, so only log a warning here
                log.warn("No support for script language " + language.getName() + ", skipping engine creation test");
            }
        }
    }

    /**
     * Basic JavaScript test.
     * 
     * @throws ScriptException {@link ScriptException}
     */
    @Test
    public void testJavaScriptEngine() throws ScriptException {
        final ScriptLanguage js = ScriptLanguage.JavaScript;
        Assume.assumeTrue(service.supportsScriptLanguage(js));
        final ScriptEngine jsEngine = service.createScriptEngine(js);
        // TODO make more JavaScript-specific
        jsEngine.eval("answer = 42");
        final Object answer = jsEngine.get("answer");
        Assert.assertNotNull(answer);
        final int theAnswer = 42;
        // under Linux answer is object of type Double, under Windows it is of type Double
        // to satisfy both of them the inconvenient term is used
        Assert.assertEquals(new Double(theAnswer), Double.valueOf(answer.toString()));
    }
    
    /**
     * Basic Jython test.
     * 
     * @throws ScriptException {@link ScriptException}
     */
    @Test
    public void testJythonEngine() throws ScriptException {
        final ScriptLanguage jython = ScriptLanguage.Jython;
        Assume.assumeTrue(service.supportsScriptLanguage(jython));
        final ScriptEngine jythonEngine = service.createScriptEngine(jython);
        // TODO make more Jython-specific
        jythonEngine.eval("answer = 42");
        final Object answer = jythonEngine.get("answer");
        Assert.assertNotNull(answer);
        final int theAnswer = 42;
        Assert.assertEquals(new Integer(theAnswer), answer);
    }
}
