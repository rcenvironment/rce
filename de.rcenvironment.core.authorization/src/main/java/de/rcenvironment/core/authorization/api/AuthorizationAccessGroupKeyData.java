/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.authorization.api;

import de.rcenvironment.core.authorization.cryptography.api.SymmetricKey;

/**
 * A holder for cryptographic key material of an {@link AuthorizationAccessGroup}; currently, a symmetric key.
 *
 * @author Robert Mischke
 */
public class AuthorizationAccessGroupKeyData {

    private final SymmetricKey symmetricKey;

    public AuthorizationAccessGroupKeyData(SymmetricKey symmetricKey) {
        this.symmetricKey = symmetricKey;
    }

    public SymmetricKey getSymmetricKey() {
        return symmetricKey;
    }

    public String getEncodedStringForm() {
        return symmetricKey.getEncodedForm();
    }

}
