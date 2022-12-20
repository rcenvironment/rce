/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.eventlog.api;

/**
 * Various constants related to event logging, e.g. common or reserved attribute values.
 *
 * @author Robert Mischke
 */
public final class EventLogConstants {

    /**
     * A generic "true"/"yes" value.
     * 
     * Currently logged as a string value to simplify log parsing. This constant should be used instead of a custom String to allow
     * centralized migration to a different value if necessary.
     */
    public static final String TRUE_VALUE = "yes";

    /**
     * A generic "false"/"no" value.
     * 
     * Currently logged as a string value to simplify log parsing. This constant should be used instead of a custom String to allow
     * centralized migration to a different value if necessary.
     */
    public static final String FALSE_VALUE = "no";

    private EventLogConstants() {}

    /**
     * @param value a boolean value
     * @return the appropriate string constant for the boolean value
     */
    public static String trueFalseValueFromBoolean(boolean value) {
        if (value) {
            return TRUE_VALUE;
        } else {
            return FALSE_VALUE;
        }
    }

}
