/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.integration;

/**
 * Enum to handle different {@link IntegrationContext} types.
 * 
 * @author Kathrin Schaffert
 */
public enum IntegrationContextType {

    /**
     * The context of an integrated common tool.
     */
    COMMON("Common"),

    /**
     * The context of an integrated CPACS tool.
     */
    CPACS("CPACS"),

    /**
     * The context of an integrated workflow.
     */
    WORKFLOW("Workflow");

    private final String type;

    IntegrationContextType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }

}