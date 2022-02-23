/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.internal;

import java.io.IOException;
import java.util.Objects;

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.StorageException;

import de.rcenvironment.core.configuration.SecureStorageSection;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;

/**
 * An implementation of {@link SecureStorageSection} backed by Eclipse's {@link ISecurePreferences}.
 *
 * @author Robert Mischke
 */
public class SecureStorageSectionSecurePreferencesImpl implements SecureStorageSection {

    private static final String ERROR_MESSAGE_NO_NULL_KEYS_ALLOWED = "Secure Storage keys must not be null";

    /**
     * Unit tests have shown that the Eclipse Secure Preferences API sometimes returns a garbled value instead of failing when the provided
     * store password is incorrect, or was changed. To detect and handle these cases, all non-null values are given this prefix, which is
     * verified on re-reading values, and obviously stripped to return the actual value.
     * 
     * The current length of 4 characters gives an approximate 1 out of 4 billion (~256^4) risk of accidentally matching anyway, which
     * should be acceptable. (The number is not precise due to the garbled data being mapped to unicode in an unknown way.)
     */
    private static final String VALUE_VERIFICATION_PREFIX = "val:";

    private static final String NULL_REPLACEMENT_VALUE = "null";

    private final String sectionId;

    private final ISecurePreferences securePrefsNode;

    public SecureStorageSectionSecurePreferencesImpl(String sectionId, ISecurePreferences securePrefsNode) {
        this.sectionId = sectionId;
        this.securePrefsNode = securePrefsNode;
    }

    @Override
    public void store(String key, String data) throws OperationFailureException {
        Objects.requireNonNull(key, ERROR_MESSAGE_NO_NULL_KEYS_ALLOWED);
        try {
            final String valueToStore;
            if (data != null) {
                valueToStore = VALUE_VERIFICATION_PREFIX + data;
            } else {
                valueToStore = NULL_REPLACEMENT_VALUE;
            }
            securePrefsNode.put(key, valueToStore, true); // true = encrypted
            // write changes to disk; by default, Eclipse only persists updated entries on a clean shutdown
            securePrefsNode.flush();
        } catch (StorageException | IOException e) {
            throw new OperationFailureException("Failed to write secure storage entry " + entryDescription(key) + ": " + e.toString());
        }
    }

    @Override
    public String read(String key, String defaultValue) throws OperationFailureException {
        Objects.requireNonNull(key, ERROR_MESSAGE_NO_NULL_KEYS_ALLOWED);
        try {
            final String storedValue = securePrefsNode.get(key, null); // use null as default for absent key
            if (storedValue == null) {
                // key was not present at all, so return the default value
                // TODO there is no unit test for this code path
                return defaultValue;
            } else if (NULL_REPLACEMENT_VALUE.equals(storedValue)) {
                // null was explicitly stored, so do not use the default value
                return null;
            } else if (storedValue.startsWith(VALUE_VERIFICATION_PREFIX)) {
                // standard case: the prefix was found as expected, so remove it and return the actual value
                return storedValue.substring(VALUE_VERIFICATION_PREFIX.length());
            } else {
                // error case: the underlying Eclipse API returned a garbled value instead of failing when a wrong password is provided
                throw new OperationFailureException("Failed to read secure storage entry " + entryDescription(key)
                    + ": the storage backend returned a value, but it did not pass the consistency check; "
                    + "this is typically caused by a wrong or changed storage password");
            }
        } catch (StorageException e) {
            throw new OperationFailureException("Failed to read secure storage entry " + entryDescription(key) + ": " + e.toString());
        }
    }

    @Override
    public void delete(String key) throws OperationFailureException {
        Objects.requireNonNull(key, ERROR_MESSAGE_NO_NULL_KEYS_ALLOWED);
        try {
            securePrefsNode.remove(key);
            // write changes to disk; by default, Eclipse only persists updated entries on a clean shutdown
            securePrefsNode.flush();
        } catch (IOException e) {
            throw new OperationFailureException("Failed to delete secure storage entry " + entryDescription(key) + ": " + e.toString());
        }
    }

    @Override
    public String[] listKeys() {
        return securePrefsNode.keys();
    }

    private String entryDescription(String key) {
        return key + " in section " + sectionId;
    }

}
