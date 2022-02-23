/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.common.variables.legacy;

import java.io.Serializable;


/**
 * Represents a variable bound to a script or other variable-aware tool or service.
 *
 * @author Arne Bachmann
 */
@Deprecated
public class BoundVariable extends TypedValue implements Serializable {
    
    private static final long serialVersionUID = -6487587122044693273L;

    private final String name;
    
    
    /**
     * Copy constructor copies name, type, <b>and</b> value.
     * 
     * @param from Where to copy from
     */
    public BoundVariable(final BoundVariable from) {
        super(from);
        this.name = from.name;
    }
    
    /**
     * Create a new bound variable, guessing data type and setting the value.
     * 
     * @param value The autoboxesd primitive to set
     */
    public BoundVariable(final String name, final Serializable value) {
        super(value);
        this.name = name;
    }
    
    /**
     * Creates new variable with a default value.
     * 
     * @param name Name
     * @param type Type
     */
    public BoundVariable(final String name, final VariableType type) {
        super(type);
        this.name = name;
    }
    
    /**
     * Initializes a new bound variable with all three parameters.
     * 
     * @param name The name
     * @param type The type
     * @param value The value in string representation
     */
    public BoundVariable(final String name, final VariableType type, final String value) {
        super(type, value);
        this.name = name;
    }
    
    /**
     * Set a String value and return the whole new object instance.
     * 
     * @param stringValue String to set.
     * @return the whole new object instance.
     */
    @Override
    public BoundVariable setStringValue(final String stringValue) {
        super.setStringValue(stringValue);
        return this;
    }
    
    /**
     * Set a Integer value and return the whole new object instance.
     * 
     * @param intValue Integer to set.
     * @return the whole new object instance.
     */
    @Override
    public BoundVariable setIntegerValue(final long intValue) {
        super.setIntegerValue(intValue);
        return this;
    }
    
    /**
     * Set a Double value and return the whole new object instance.
     * 
     * @param realValue Double to set.
     * @return the whole new object instance.
     */
    @Override
    public BoundVariable setRealValue(final double realValue) {
        super.setRealValue(realValue);
        return this;
    }
    
    /**
     * Set a Boolean value and return the whole new object instance.
     * 
     * @param booleanValue Boolean to set.
     * @return the whole new object instance.
     */
    @Override
    public BoundVariable setLogicValue(final boolean booleanValue) {
        super.setLogicValue(booleanValue);
        return this;
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {
        return name + ": " + super.toString(); 
    }
    
}
