/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration;

import java.io.IOException;

import org.eclipse.equinox.security.storage.ISecurePreferences;

/**
 * An abstract service providing access to a set of key-value maps (each represented by a {@link SecureStorageSection}) which are persisted
 * within the current RCE profile.
 *
 * @author Robert Mischke
 */
public interface SecureStorageService {

    /**
     * Provides access to a section of the Secure Storage.
     * 
     * @param id the section's id; each section represents a separate key-value map within the secure storage
     * @return secure storage as {@link ISecurePreferences} object
     * @throws IOException on error
     */
    SecureStorageSection getSecureStorageSection(String id) throws IOException;
}
