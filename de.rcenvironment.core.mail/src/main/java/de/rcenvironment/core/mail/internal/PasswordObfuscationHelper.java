/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.mail.internal;

import de.rcenvironment.core.utils.encryption.EncryptionFactory;
import de.rcenvironment.core.utils.encryption.PassphraseBasedEncryption;
import de.rcenvironment.core.utils.encryption.EncryptionFactory.PassphraseBasedEncryptionAlgorithm;

/**
 * Helper class to obfuscate passwords with a fixed passphrase.
 *
 * @author Tobias Rodehutskors
 */
public final class PasswordObfuscationHelper {

    /**
     * TODO Should we be this honest?
     * 
     * This is the passphrase that is used to obfuscate the stored SMTP mail server password.
     */
    private static final String PASSPHRASE = "K2sVlb1THNqkvbTnCVhW";

    private static PassphraseBasedEncryption encryption = null;

    private PasswordObfuscationHelper() {

    }

    private static synchronized void init() {
        if (encryption == null) {
            EncryptionFactory factory = new EncryptionFactory();
            encryption = factory.createPassphraseBasedEncryption(PassphraseBasedEncryptionAlgorithm.AES);
        }
    }

    /**
     * @param password The password that should be obfuscated.
     * @return A obfuscated version of the given password or null if the password is null.
     */
    public static String obfuscate(String password) {
        init();

        if (password == null) {
            return null;
        }

        return encryption.encrypt(password, PASSPHRASE);
    }

    /**
     * @param obfuscatedPassword The obfuscated password that should be deobfuscated.
     * @return The original password or null if the obfuscatedPassword is null.
     */
    public static String deobfuscate(String obfuscatedPassword) {
        init();

        if (obfuscatedPassword == null) {
            return null;
        }

        return encryption.decrypt(obfuscatedPassword, PASSPHRASE);
    }

}
