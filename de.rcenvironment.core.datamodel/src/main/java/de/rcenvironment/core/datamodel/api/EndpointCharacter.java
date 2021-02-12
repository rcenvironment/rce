/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.api;

/**
 * Defines whether an endpoint is meant to be connected to a component of the same loop level or to a component of the next upper loop
 * level.
 * 
 * @author Doreen Seider
 * 
 *         TODO (p2) 8.1.0: Rename to LoopLevel.* (LoopLevelDirection, LoopLevelDifferential, etc. (nothing of them is really nice, thus
 *         keep it open by using .*)) - seid_do
 */
public enum EndpointCharacter {

    /** Connected to same loop. */
    SAME_LOOP("From same loop level", "To same loop level"),

    /** Connected to upper loop. */
    OUTER_LOOP("From upper loop level", "To upper loop level");

    /** Value for {@link EndpointCharacter#SAME_LOOP}. */
    public static final String VALUE_SAME_LOOP = "sameLoop";

    /** Value for {@link EndpointCharacter#OUTER_LOOP}. */
    public static final String VALUE_OUTER_LOOP = "outerLoop";

    private final String displayNameInput;

    private final String displayNameOutput;

    /**
     * @param displayName name shown in UIs
     */
    EndpointCharacter(String displayNameInput, String displayNameOutput) {
        this.displayNameInput = displayNameInput;
        this.displayNameOutput = displayNameOutput;
    }

    /**
     * Creates {@link EndpointCharacter} instances from the value name used for endpoint definition.
     * 
     * @param valueName value name used for endpoint definition
     * @return {@link EndpointCharacter} or <code>null</code> if given value is unknown
     */
    public static EndpointCharacter fromEndpointDefinitionValue(String valueName) {
        switch (valueName) {
        case VALUE_SAME_LOOP:
            return EndpointCharacter.SAME_LOOP;
        case VALUE_OUTER_LOOP:
            return EndpointCharacter.OUTER_LOOP;
        default:
            return null;
        }
    }

    /**
     * @param endpointType {@link EndpointType} the display name is used for
     * @return display name of {@link EndpointCharacter} based on the given {@link EndpointType}
     */
    public String getDisplayName(EndpointType endpointType) {
        switch (endpointType) {
        case INPUT:
            return displayNameInput;
        case OUTPUT:
            return displayNameOutput;
        default:
            throw new IllegalArgumentException("Endpint type unknown: " + endpointType);
        }
    }
}
