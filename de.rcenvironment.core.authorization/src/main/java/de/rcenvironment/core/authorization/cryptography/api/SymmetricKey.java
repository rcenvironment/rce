/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.authorization.cryptography.api;

/**
 * A wrapper for a symmetric encryption key with direct access to its encoded/serialized form.
 *
 * @author Robert Mischke
 */
public interface SymmetricKey {

    /**
     * @return the transport-safe encoded string form of this key
     */
    String getEncodedForm();
}
