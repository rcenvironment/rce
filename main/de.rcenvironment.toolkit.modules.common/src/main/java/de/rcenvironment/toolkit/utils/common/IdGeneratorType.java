/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
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
