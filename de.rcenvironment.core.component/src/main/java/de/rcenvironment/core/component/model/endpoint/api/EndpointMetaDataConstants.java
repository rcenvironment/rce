/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.model.endpoint.api;

/**
 * Constants used in JSON files to describe endpoint meta data.
 * 
 * @author Doreen Seider
 */
public final class EndpointMetaDataConstants {

    /**
     * Visibility level of meta data.
     * 
     * @author Doreen Seider
     */
    public enum Visibility {
        /** Only configurable by developer. */
        developerConfigurable,
        /** Configurable by developer and user. */
        userConfigurable,
        /** Configurable by developer and user and additional show to user in read-only mode. */
        shown;
    }

    /** Type of meta datum. */
    public static final String TYPE_TEXT = "text";

    /** Type of meta datum. */
    public static final String TYPE_SHORTTEXT = "short_text";

    /** Type of meta datum. */
    public static final String TYPE_BOOL = "bool";

    /** Type of meta datum. */
    public static final String TYPE_INT = "int";

    /** Type of meta datum. */
    public static final String TYPE_FLOAT = "float";
    
    /** Type of meta datum. */
    public static final String TYPE_FLOAT_GREATER_ZERO = "float_greater_zero";

    /** Placeholder for any possible value. */
    public static final String PLACEHOLDER_ANY_POSSIBLE_VALUE = "*";


    private EndpointMetaDataConstants() {}

}
