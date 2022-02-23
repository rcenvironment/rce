/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.common.variables.legacy;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;


/**
 * Super class for BoundVariable, also used in VariantArray.
 *
 * @author Arne Bachmann
 */
@Deprecated
public class TypedValue implements Serializable {
    
    /**
     * Static empty instance.
     */
    public static final TypedValue EMPTY = new TypedValue(VariableType.Empty);

    /**
     * Make it serializable.
     */
    private static final long serialVersionUID = 8955894133896567250L;

    /**
     * Type of the value.
     */
    private final VariableType type;
    
    /**
     * The value in string representation, or null for Empty type.
     */
    private String value;
    
    
    /**
     * Copy constructor copies name, type, <b>and</b> value.
     * 
     * @param from Where to copy from
     */
    public TypedValue(final TypedValue from) {
        this.type = from.type;
        this.value = from.value;
    }
    
    /**
     * Create a new bound variable, guessing data type and setting the value.
     * 
     * @param value The autoboxesd primitive to set
     */
    public TypedValue(final Serializable value) {
        this.type = determineType(value);
        this.value = determineValue(value, type);
    }
    
    /**
     * Creates new variable with a default value.
     * 
     * @param name Name
     * @param type Type
     */
    public TypedValue(final VariableType type) {
        this.type = type;
        setDefaultValue();
    }
    
    /**
     * Initializes a new bound variable with all three parameters.
     * 
     * @param type The type
     * @param value The value in string representation
     */
    public TypedValue(final VariableType type, final String value) {
        this.type = type;
        this.value = value;
    }
    
    /**
     * Initializes a new bound variable from a class type and a string representation of its value.
     * 
     * @param type The type to determine
     * @param value The value
     */
    public TypedValue(final Class<? extends Serializable> type, final String value) {
        this.type = determineType(type);
        setValueFromString(value);
    }
    
    /**
     * Set a String value and return the whole new object instance.
     * Warning: This doesn't try to determine or change the data type!
     * 
     * @param stringValue String to set.
     * @return the whole new object instance.
     */
    public TypedValue setStringValue(final String stringValue) {
        this.value = stringValue;
        return this;
    }
    
    /**
     * Set a Integer value and return the whole new object instance.
     * 
     * @param intValue Integer to set.
     * @return the whole new object instance.
     */
    public TypedValue setIntegerValue(final long intValue) {
        this.value = Long.toString(intValue);
        return this;
    }
    
    /**
     * Set a Double value and return the whole new object instance.
     * 
     * @param realValue Double to set.
     * @return the whole new object instance.
     */
    public TypedValue setRealValue(final double realValue) {
        this.value = Double.toString(realValue);
        return this;
    }
    
    /**
     * Set a Boolean value and return the whole new object instance.
     * 
     * @param booleanValue Boolean to set.
     * @return the whole new object instance.
     */
    public TypedValue setLogicValue(final boolean booleanValue) {
        if (booleanValue) {
            this.value = "true";
        } else {
            this.value = "false";
        }
        return this;
    }
    
    public VariableType getType() {
        return type;
    }
    
    public String getStringValue() {
        return value;
    }
    
    public long getIntegerValue() {
        return Long.parseLong(value.replaceAll("[lL]", "").replaceAll("[\\.,].*$", ""));
    }
    
    public double getRealValue() {
        return Double.parseDouble(value.replaceAll("[dD]", "").replaceAll(",", "."));
    }
    
    /**
     * Returns the logical representation of the value.
     * 
     * @return logical representation or <code>false</code> if it has none.
     */
    public boolean getLogicValue() {
        if (value == null) {
            return false;
        }
        if (value.equalsIgnoreCase("true")) {
            return true;
        }
        return false;
    }
    
    /**
     * Returns the value.
     * 
     * @return the value.
     */
    public Serializable getValue() {
        Serializable result = null;
        if (value != null) {
            switch (type) {
            case Real:
                result = getRealValue();
                break;
            case Integer:
                result = getIntegerValue();
                break;
            case Logic:
                result = getLogicValue();
                break;
            case Empty:
                result = null;
                break;
            default:
            case String:
                result = getStringValue();
                break;
            }
        }
        return result;
    }
    
    /**
     * Set the value of the bound variable from an autoboxed primitive.
     * 
     * @param aValue The float, double, String, long, int, boolean, ... to set
     * @return self
     * @exception IllegalArgumentException If the determined type differs from the current one
     */
    public TypedValue setValue(final Serializable aValue) throws IllegalArgumentException {
        final VariableType varType = determineType(aValue);
        if (varType != this.type) {
            throw new IllegalArgumentException("Could not set value to given type: expected " + type.toString() + " but found "
                + varType.toString());
        }
        value = determineValue(aValue, varType);
        return this;
    }

    /**
     * Setter.
     * 
     * @param aValue The value to set as {@link String}.
     * @return self
     */
    public TypedValue setValueFromString(final String aValue) {
        switch (type) {
        case Logic:
            setStringValue(aValue);
            setLogicValue(getLogicValue()); // neat trick
            break;
        case Integer:
            setStringValue(aValue);
            setIntegerValue(getIntegerValue());
            break;
        case Real:
            setStringValue(aValue);
            setRealValue(getRealValue());
            break;
        case Empty:
            setStringValue(null);
            break;
        default:
        case String:
            setStringValue(aValue);
            break;
        }
        return this;
    }
    
    /**
     * Setter.
     * 
     * @return The object itself
     */
    public TypedValue setEmptyValue() {
        setStringValue(null);
        return this;
    }
    
    /**
     * Sets a default value according to type.
     */
    private void setDefaultValue() {
        switch (type) {
        case Integer:
            value = "0";
            break;
        case Real:
            value = "0.0";
            break;
        case Logic:
            value = "False";
            break;
        case Empty:
            value = "";
            break;
        default:
        case String:
            value = "";
            break;
        }
    }

    /**
     * Try to determine the script engines bound return type.
     * 
     * @param valueToDetermine The binding value as returned from the scripting engine
     * @return The detected type
     * throws IllegalArgumentException If cannot determine the type
     */
    private static VariableType determineType(final Serializable valueToDetermine) {
        VariableType varType = null;
        if (valueToDetermine == null) {
            varType = VariableType.Empty;
        } else if (valueToDetermine instanceof String) {
            varType = VariableType.String;
        } else if ((valueToDetermine instanceof Integer)
                || (valueToDetermine instanceof Long)
                || (valueToDetermine instanceof BigInteger)) {
            varType = VariableType.Integer;
        } else if ((valueToDetermine instanceof Double)
                || (valueToDetermine instanceof Float)) {
            varType = VariableType.Real;
        } else if (valueToDetermine instanceof Boolean) {
            varType = VariableType.Logic;
        } else if (valueToDetermine instanceof Date) {
            varType = VariableType.Date;
        } else {
            throw new IllegalArgumentException("Could not determine data type in script binding detection");
        }
        
        return varType;
    }
    
    /**
     * Determine the type from a class.
     * 
     * @param typeToDetermine The type to determine
     * @return The type of variable we think it might be
     */
    private static VariableType determineType(final Class<? extends Serializable> typeToDetermine) {
        VariableType varType = null;
        if (typeToDetermine == null) {
            varType = VariableType.Empty;
        } else if (typeToDetermine == String.class) {
            varType = VariableType.String;
        } else if ((typeToDetermine == Integer.class)
                || (typeToDetermine == Long.class)
                || (typeToDetermine == BigInteger.class)) {
            varType = VariableType.Integer;
        } else if ((typeToDetermine == Double.class)
                || (typeToDetermine == Float.class)) {
            varType = VariableType.Real;
        } else if (typeToDetermine == Boolean.class) {
            varType = VariableType.Logic;
        } else {
            throw new IllegalArgumentException("Could not determine data type in script binding detection");
        }
        return varType;
    }
    
    /**
     * Determine the java value in string representation of the given binding value.
     * 
     * @param valueToDetermine The value as returned by the script engine
     * @param varType The type assumed
     * @return The value as a string
     * @exception IllegalArgumentException If value cannot be converted in desired type
     */
    private String determineValue(final Serializable valueToDetermine, final VariableType varType) {
        String returnValue = null;
        
        try {
            switch (varType) {
            case Integer:
                if (valueToDetermine instanceof Integer) {
                    returnValue = ((Integer) valueToDetermine).toString();
                } else if (valueToDetermine instanceof Long) {
                    returnValue = ((Long) valueToDetermine).toString();
                } else if (valueToDetermine instanceof BigInteger) {
                    returnValue = ((BigInteger) valueToDetermine).toString();
                }
                break;
            case Real:
                if (valueToDetermine instanceof Double) {
                    returnValue = ((Double) valueToDetermine).toString();
                } else if (valueToDetermine instanceof Float) {
                    returnValue = ((Float) valueToDetermine).toString();
                }
                break;
            case Logic:
                returnValue = ((Boolean) valueToDetermine).toString(); // contains Java writing
                break;
            case Date:
                returnValue = ((Date) valueToDetermine).toString();
                break;
            case Empty:
                returnValue = null;
                break;
            default:
            case String:
                returnValue = (String) valueToDetermine;
            }
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("Could not parse numeric value due to unknown format");
        } catch (final NullPointerException e) {
            throw new IllegalArgumentException("Could not determine data value due to null pointer error");
        }
//        if (returnValue == null)  {
//            throw new IllegalArgumentException("Could not determine data value  in script binding detection");            
//        } else {
        return returnValue;
// }
    }

    @Override
    public String toString() {
        return type.name() + " = " + value; 
    }

}
