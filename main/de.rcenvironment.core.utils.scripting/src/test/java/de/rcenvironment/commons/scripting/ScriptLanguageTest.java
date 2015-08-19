/*
 * Copyright (C) 2006-2011 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.commons.scripting;

import java.util.UUID;

import javax.script.ScriptEngineManager;

import junit.framework.Assert;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test for {@link ScriptLanguage}.
 *
 * @author Christian Weiss
 *
 */
public class ScriptLanguageTest {
    
    private static ScriptLanguage[] languages;

    /** BeforeClass. */
    @BeforeClass
    public static void setupBeforeClass() {
        languages = ScriptLanguage.values();
    }

    /** Test. */
    @Test
    public void testValues() {
        Assume.assumeNotNull((Object) languages);
        for (final ScriptLanguage language : languages) {
            Assert.assertNotNull(language.getName());
            Assert.assertFalse(language.getName().isEmpty());
            Assert.assertNotNull(language.getExtension());
            Assert.assertFalse(language.getExtension().isEmpty());
        }
    }

    /** Test. */
    @Test
    public void testGetLanguageByName() {
        Assume.assumeNotNull((Object) languages);
        for (final ScriptLanguage language : languages) {
            final String name = language.getName();
            final ScriptLanguage language2 = ScriptLanguage.getByName(new String(name));
            Assert.assertNotNull(language2);
            Assert.assertEquals(name, language2.getName());
        }
    }

    /** Test. */
    @Test(expected = IllegalArgumentException.class)
    public void testGetLanguageByNameForFailureNull() {
        try {
            ScriptLanguage.getByName(null);
        } catch (final AssertionError e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** Test. */
    @Test(expected = IllegalArgumentException.class)
    public void testGetLanguageByNameForFailureEmpty() {
        try {
            ScriptLanguage.getByName("");
        } catch (final AssertionError e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** Test. */
    @Test(expected = IllegalArgumentException.class)
    public void testGetLanguageByNameForFailureRandom() {
        try {
            ScriptLanguage.getByName("FooBarLang");
        } catch (final AssertionError e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** Test. */
    @Test
    public void testGetLanguageByExtension() {
        Assume.assumeNotNull((Object) languages);
        for (final ScriptLanguage language : languages) {
            final String extension = language.getExtension();
            final ScriptLanguage language2 = ScriptLanguage.getByExtension(new String(extension));
            Assert.assertNotNull(language2);
            Assert.assertEquals(extension, language2.getExtension());
        }
    }

    /** Test. */
    @Test(expected = IllegalArgumentException.class)
    public void testGetLanguageByExtensionForFailure() {
        Assume.assumeNotNull((Object) languages);
        ScriptLanguage.getByExtension(UUID.randomUUID().toString());
    }

    /** Test. */
    @Test
    @Ignore("functionality is platform-dependant, thus removed until actually required")
    public void testGetLanguageByNameAlternateName() {
        final ScriptEngineManager engineManager = new ScriptEngineManager();
        Assume.assumeTrue(engineManager.getEngineByName("JavaScript") != null
                && engineManager.getEngineByName("ECMAScript") != null);
        final ScriptLanguage language = ScriptLanguage.getByName("ECMAScript");
        Assert.assertNotNull(language);
    }

    /** Test. */
    @Test(expected = IllegalArgumentException.class)
    public void testGetLanguageByExtensionForFailureNull() {
        try {
            ScriptLanguage.getByExtension(null);
        } catch (final AssertionError e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** Test. */
    @Test(expected = IllegalArgumentException.class)
    public void testGetLanguageByExtensionForFailureEmpty() {
        try {
            ScriptLanguage.getByExtension("");
        } catch (final AssertionError e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** Test. */
    @Test(expected = IllegalArgumentException.class)
    public void testGetLanguageByExtensionForFailureRandom() {
        try {
            ScriptLanguage.getByExtension(".fbl");
        } catch (final AssertionError e) {
            throw new IllegalArgumentException(e);
        }
    }

}
