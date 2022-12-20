/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.integration.workflowintegration.editor.mappingtreenodes;

/**
 * The type of the {@link MappingNode}.
 * 
 * @author Kathrin Schaffert
 */
public enum MappingType {

    /** Inputs. */
    INPUT("Input"),
    /** Outputs. */
    OUTPUT("Output"),
    /** Property.*/
    PROPERTY("Property");
    
    private final String displayName;

    MappingType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return this.displayName;
    }

}
