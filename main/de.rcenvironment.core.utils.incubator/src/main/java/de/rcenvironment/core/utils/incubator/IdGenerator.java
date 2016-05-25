/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator;

import java.util.UUID;

/**
 * Utility class for generating common types of (usually random) identifiers.
 * 
 * @author Robert Mischke
 */
public final class IdGenerator {

    private IdGenerator() {
        // prevent instantiation
    }

    /**
     * @return the String form of a random (type 4) UUID, with the separating dashes removed; in
     *         effect, this is a cryptographically strong random 32-character hex string that
     *         adheres to the bit field structure of a type 4 UUID
     * 
     * @see {@link UUID#randomUUID()}
     */
    public static String randomUUIDWithoutDashes() {
        return randomUUID().replace("-", "");
    }

    /**
     * @return the String form of a random (type 4) UUID; it consists of 36 hex characters
     * 
     * @see {@link UUID#randomUUID()}
     */
    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }
}
