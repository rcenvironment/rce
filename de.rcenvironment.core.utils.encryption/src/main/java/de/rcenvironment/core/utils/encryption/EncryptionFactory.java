/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.encryption;

import de.rcenvironment.core.utils.encryption.internal.DefaultEncryption;

/**
 * Creates {@link PassphraseBasedEncryption} instances and {@link KeyBasedEncryption} instances for
 * different encryption algorithms. Supported ones are declared in
 * {@link PassphraseBasedEncryptionAlgorithm} and {@link KeyBasedEncryptionAlgorithm}.
 * 
 * @author Doreen Seider
 */
public class EncryptionFactory {

    /**
     * Supported passphrase-based encryption algorithms.
     * 
     * @author Doreen Seider
     */
    public enum PassphraseBasedEncryptionAlgorithm implements EncryptionAlgorithm {
        /** AES. */
        AES,
        /** Blowfish. */
        Blowfish;

        @Override
        public String getName() {
            return name();
        }
    }

    /**
     * Supported key-based encryption algorithms.
     * 
     * @author Doreen Seider
     */
    public enum KeyBasedEncryptionAlgorithm implements EncryptionAlgorithm {
        /** RSA. */
        RSA;

        @Override
        public String getName() {
            return name();
        }
    }

    /**
     * Describes an encryption algorithm.
     * 
     * @author Doreen Seider
     */
    public interface EncryptionAlgorithm {

        /**
         * @return name of the encryption algorithm
         */
        String getName();
    }

    /**
     * Create a {@link PassphraseBasedEncryption} instance for given encryption algorithm.
     * 
     * @param algorithm encryption algorithm to support
     * @return {@link PassphraseBasedEncryption} instance supporting given algorithm
     */
    public PassphraseBasedEncryption createPassphraseBasedEncryption(PassphraseBasedEncryptionAlgorithm algorithm) {

        PassphraseBasedEncryption encryption;

        switch (algorithm) {
        case AES:
            encryption = new DefaultEncryption(PassphraseBasedEncryptionAlgorithm.AES);
            break;
        case Blowfish:
            encryption = new DefaultEncryption(PassphraseBasedEncryptionAlgorithm.Blowfish);
            break;
        default:
            throw new IllegalArgumentException("desired passphrase-based enryption algorithm not supported: " + algorithm
                + " -  supported ones are: " + PassphraseBasedEncryptionAlgorithm.AES + ", " + PassphraseBasedEncryptionAlgorithm.Blowfish);
        }
        return encryption;
    }

    /**
     * Create a {@link KeyBasedEncryption} instance for given encryption algorithm.
     * 
     * @param algorithm encryption algorithm to support
     * @return {@link KeyBasedEncryption} instance supporting given algorithm
     */
    public KeyBasedEncryption createKeyBasedEncryption(KeyBasedEncryptionAlgorithm algorithm) {

        KeyBasedEncryption encryption;

        switch (algorithm) {
        case RSA:
            encryption = new DefaultEncryption(KeyBasedEncryptionAlgorithm.RSA);
            break;
        default:
            throw new IllegalArgumentException("desired passphrase-based enryption algorithm not supported: " + algorithm
                + " -  supported ones are: " + PassphraseBasedEncryptionAlgorithm.AES + ", " + PassphraseBasedEncryptionAlgorithm.Blowfish);
        }
        return encryption;
    }

}
