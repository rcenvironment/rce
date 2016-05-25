/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.encryption;

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import de.rcenvironment.core.utils.encryption.EncryptionFactory.KeyBasedEncryptionAlgorithm;

/**
 * Creates key pair for key-based encryption.
 * 
 * @author Phillip Rohde
 */
public class KeyPairFactory {
    
    private static final int RSA_ENCRYPTION_SIZE = 2048;
    
    /**
     * Create private-public key pair.
     * 
     * @param algorithm key-based encryption algorithm to use
     * @return key pair as array: first element is private key, second one is public
     */
    public Key[] createKeyPair(KeyBasedEncryptionAlgorithm algorithm) {
        
        Key[] keyPair = new Key[2];
        
        KeyPairGenerator keygen;
        try {
            keygen = KeyPairGenerator.getInstance(algorithm.name());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("encryption algorithm RSA not supported");
        }
        keygen.initialize(RSA_ENCRYPTION_SIZE); // TAKES A LONG TIME BUT IS SECURE
        KeyPair rsaKeys = keygen.genKeyPair();
        keyPair[0] = rsaKeys.getPrivate();
        keyPair[1] = rsaKeys.getPublic();
        
        return keyPair;
    }
}
