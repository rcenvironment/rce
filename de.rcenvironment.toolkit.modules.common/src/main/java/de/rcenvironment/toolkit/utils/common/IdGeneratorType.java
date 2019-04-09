/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.toolkit.utils.common;

/**
 * Selects the preference between speed and security when generating random ids.
 * 
 * @author Robert Mischke
 */
public enum IdGeneratorType {

    /**
     * Generates ids quickly, e.g. for unit testing.
     */
    FAST,

    /**
     * Generates ids securely, e.g. for secret tokens/nonces.
     */
    SECURE;
}
