/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.authorization.cryptography.internal;

import javax.crypto.SecretKey;

import de.rcenvironment.core.authorization.cryptography.api.SymmetricKey;

/**
 * Default {@link SymmetricKey} implementation.
 *
 * @author Robert Mischke
 */
public class SymmetricKeyImpl implements SymmetricKey {

    private final SecretKey secretKey;

    private final String encodedForm;

    public SymmetricKeyImpl(SecretKey secretKey, String encodedForm) {
        this.secretKey = secretKey;
        this.encodedForm = encodedForm;
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }

    @Override
    public String getEncodedForm() {
        return encodedForm;
    }

}
