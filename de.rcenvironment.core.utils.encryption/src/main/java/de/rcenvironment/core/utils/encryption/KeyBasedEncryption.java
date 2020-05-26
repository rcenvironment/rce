/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.encryption;

import java.security.Key;

/**
 * Encrypts and decrypts texts based on public-private-key encryption.
 * 
 * @author Doreen Seider
 */
public interface KeyBasedEncryption {

    /**
     * Encrypts given text (base64 encoded).
     * 
     * @param text text to encrypt (Base64 encoded)
     * @param publicKey public key to use for encryption
     * @return encrypted text or <code>null</code> if encrypting failed
     */
    String encrypt(String text, Key publicKey);
    
    /**
     * Decrypts given text (base64 encoded).
     * 
     * @param text text to decrypt (Base64 encoded)
     * @param privateKey private key to use for encryption
     * @return decrypted text or <code>null</code> if decrypting failed
     */
    String decrypt(String text, Key privateKey);

}
