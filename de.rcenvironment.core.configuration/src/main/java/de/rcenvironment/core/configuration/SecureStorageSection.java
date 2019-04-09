/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration;

import de.rcenvironment.core.utils.common.exception.OperationFailureException;

/**
 * Represents a section of an abstract "secure storage". Each section provides a String-to-String key-value map.
 * <p>
 * TODO (p2) open design decision: are "write uncommitted" operations needed, too?
 *
 * @author Robert Mischke
 */
public interface SecureStorageSection {

    /**
     * Sets a key-value entry, potentially overwriting any previous value.
     * 
     * @param key the String key
     * @param data the String data to store; null values are permitted
     * @throws OperationFailureException on failure
     */
    void store(String key, String data) throws OperationFailureException;

    /**
     * Reads the stored String value for the given key. If no such value exists, the given default value is returned instead.
     * 
     * @param key the key to read the value for
     * @param defaultValue the value to return if no data exists for the given key
     * @return the String value stored for the given value, or the default value if no such value exists; may return null if null was
     *         stored, or specified as the default value
     * @throws OperationFailureException on failure
     */
    String read(String key, String defaultValue) throws OperationFailureException;

    /**
     * Deletes the stored value for the given key, if one exists. If no such value exists, the method does nothing and exits normally.
     * 
     * @param key the key to delete
     * @throws OperationFailureException on failure
     */
    void delete(String key) throws OperationFailureException;

    /**
     * Lists the available keys in this storage section.
     * 
     * TODO for now, this returns an array of Strings to match the previous API; consider different return types
     * 
     * @return the list of keys
     */
    String[] listKeys();

}
