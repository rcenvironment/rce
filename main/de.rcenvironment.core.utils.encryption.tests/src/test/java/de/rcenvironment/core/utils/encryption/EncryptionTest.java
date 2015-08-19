/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.utils.encryption;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.Key;

import org.junit.Test;

import de.rcenvironment.core.utils.encryption.EncryptionFactory.KeyBasedEncryptionAlgorithm;
import de.rcenvironment.core.utils.encryption.EncryptionFactory.PassphraseBasedEncryptionAlgorithm;

/**
 * Tests for the whole rcenvironment.cor.utils.encryption package.
 * 
 * @author Sascha Zur
 */
public class EncryptionTest {

    private static final String TEST_TEXT = "hallo";

    private static final String PASSPHRASE = "test";

    /**
     * Test.
     */
    @Test
    public void testPassphraseBasedEncryptAndDecrypt() {
        
        EncryptionFactory factory = new EncryptionFactory();
        
        for (PassphraseBasedEncryptionAlgorithm algorithm : PassphraseBasedEncryptionAlgorithm.values()) {
            
            PassphraseBasedEncryption encryption = factory.createPassphraseBasedEncryption(algorithm);
            String encryptedText = encryption.encrypt(TEST_TEXT, PASSPHRASE);
            String decryptedText = encryption.decrypt(encryptedText, PASSPHRASE);
            
            assertFalse(encryptedText.equals(TEST_TEXT));
            assertTrue(decryptedText.equals(TEST_TEXT));
        }
    }

    /**
     * Test.
     */
    @Test
    public void testKeyBasedEncryptAndDecrypt() {
        
        EncryptionFactory factory = new EncryptionFactory();
        
        for (KeyBasedEncryptionAlgorithm algorithm : KeyBasedEncryptionAlgorithm.values()) {
        
            Key[] keyPair = new KeyPairFactory().createKeyPair(algorithm);
            
            KeyBasedEncryption encryption = factory.createKeyBasedEncryption(algorithm);
            String encryptedText = encryption.encrypt(TEST_TEXT, keyPair[1]);
            String decryptedText = encryption.decrypt(encryptedText, keyPair[0]);
            
            assertFalse(encryptedText.equals(TEST_TEXT));
            assertTrue(decryptedText.equals(TEST_TEXT));
        }
        
    }
}
