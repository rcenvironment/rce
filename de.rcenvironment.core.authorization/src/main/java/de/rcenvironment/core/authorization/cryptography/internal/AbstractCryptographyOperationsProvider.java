/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.authorization.cryptography.internal;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.Charsets;

import de.rcenvironment.core.authorization.cryptography.api.CryptographyOperationsProvider;
import de.rcenvironment.core.authorization.cryptography.api.SymmetricKey;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;

/**
 * Abstract class with default implementations of provider-independent methods.
 *
 * @author Robert Mischke
 */
public abstract class AbstractCryptographyOperationsProvider implements CryptographyOperationsProvider {

    public AbstractCryptographyOperationsProvider() {
        super();
    }

    @Override
    public String encodeByteArray(byte[] input) {
        return Base64.encodeBase64URLSafeString(input);
    }

    @Override
    public String encryptAndEncodeByteArray(SymmetricKey key, byte[] input) throws OperationFailureException {
        return encodeByteArray(encrypt(key, input));
    }

    @Override
    public String encryptAndEncodeString(SymmetricKey key, String input) throws OperationFailureException {
        return encodeByteArray(encrypt(key, input.getBytes(Charsets.UTF_8)));
    }

    @Override
    public byte[] decodeAndDecryptByteArray(SymmetricKey key, String input) throws OperationFailureException {
        return decrypt(key, decodeByteArray(input));
    }

    @Override
    public String decodeAndDecryptString(SymmetricKey key, String input) throws OperationFailureException {
        return new String(decrypt(key, decodeByteArray(input)), Charsets.UTF_8);
    }

    @Override
    public byte[] decodeByteArray(String input) {
        return Base64.decodeBase64(input);
    }

}
