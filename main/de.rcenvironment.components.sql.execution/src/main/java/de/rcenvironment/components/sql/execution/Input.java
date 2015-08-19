/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.sql.execution;

import java.io.Serializable;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * Holds an {@link Input} value and all corresponding information.
 * 
 * @author Doreen Seider
 */
public class Input implements Serializable {

    private static final long serialVersionUID = 4562978720420314084L;

    private String name;

    private final DataType type;

    private TypedDatum value;
    
    private String workflowIdentifier;
    
    private String componentIdentifier;
    
    private int number;
    
    /**
     * Constructor.
     * 
     * @param type Type of containing value.
     * @param newCompInstanceId The Id of the owning Component.
     * @param newName The name of the {@link Input}.
     */
    public Input(String newName, DataType newType, TypedDatum newValue,
        String newWorkflowIdentifier, String newComponentIdentifier, int number) {
        name = newName;
        type = newType;
        // TODO check
        value = newValue;
        workflowIdentifier = newWorkflowIdentifier;
        componentIdentifier = newComponentIdentifier;
        this.number = number;
    }
    /**
     * Clones the input.
     * @see java.lang.Object#clone()
     * @return an exact copy
     */
    public Input clone() {
        return new Input(name, type, value, workflowIdentifier, componentIdentifier, number);
    }
    
    public TypedDatum getValue() {
        return value;
    }
    
    public void setValue(TypedDatum value) {
        // TODO check
        this.value = value;
    }

    public DataType getType() {
        return type;
    }

    public String getName() {
        return name;
    }
    
    public String getWorkflowIdentifier() {
        return workflowIdentifier;
    }
    
    public String getComponentIdentifier() {
        return componentIdentifier;
    }
    
    public int getNumber() {
        return number;
    }
    
}
