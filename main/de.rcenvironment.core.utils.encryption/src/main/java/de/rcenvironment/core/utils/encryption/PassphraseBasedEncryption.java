/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.encryption;

/**
 * Encrypts and decrypts texts based on passphrase encryption.
 * 
 * @author Doreen Seider
 */
public interface PassphraseBasedEncryption {

    /**
     * Encrypts given text (base64 encoded).
     * 
     * @param text text to encrypt (Base64 encoded)
     * @param passphrase passphrase to use for encryption
     * @return encrypted text or <code>null</code> if encrypting failed
     */
    String encrypt(String text, String passphrase);
    
    /**
     * Decrypts given text (base64 encoded).
     * 
     * @param text text to decrypt (Base64 encoded)
     * @param passphrase passphrase to use for encryption
     * @return decrypted text or <code>null</code> if decrypting failed
     */
    String decrypt(String text, String passphrase);

}
