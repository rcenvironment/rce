/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.mail.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.not;

import org.junit.Test;

/**
 * Tests for {@link PasswordObfuscationHelper}.
 *
 * @author Tobias Rodehutskors
 */
public class PasswordObfuscationHelperTest {

    /**
     * Tests the obfuscation of passwords.
     */
    @Test
    public void testPasswordObfuscation() {
        assertEquals("GNrWIbalKRJ1CM6tdp7IAQ==", PasswordObfuscationHelper.obfuscate("testpw"));
    }

    /**
     * Tests the deobfuscation of passwords.
     */
    @Test
    public void testPasswordDeobfuscation() {
        assertEquals("testpw", PasswordObfuscationHelper.deobfuscate("GNrWIbalKRJ1CM6tdp7IAQ=="));
    }
    
    /**
     * Tests the obfuscation and deobfuscation of passwords.
     */
    @Test
    public void testPasswordObfuscationAndDeobfuscation() {
        
        String originalPassword = "test123";
        String obfuscatedPassword = PasswordObfuscationHelper.obfuscate(originalPassword);
        assertThat(originalPassword, not(obfuscatedPassword));
        String deobfuscatedPassword = PasswordObfuscationHelper.deobfuscate(obfuscatedPassword);
        assertEquals(originalPassword, deobfuscatedPassword);
    }
}
