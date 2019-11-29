/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.common.channel.legacy;

import java.io.Serializable;


/**
 * Different types of channels.
 *
 * @author Markus Kunde
 */
@Deprecated
public enum ChannelDataTypes {
    /** Data content is String. */
    STRING, 
    /** Data content is Double. */
    DOUBLE,
    /** Data content is Long. */
    LONG,
    /** Data content is Integer. */
    INTEGER,
    /** Data content is Boolean. */
    BOOLEAN,
    /** Data content is Object implements Serializable. */
    SERIALIZABLE,
    /** Data content is DataManagementFileReference. */
    FILEREFERENCE,
    /** Data content is Variant Array. */
    VARIANTARRAY;
    
    @Override 
    public String toString() {
        //only capitalize the first letter
        String s = super.toString();
        return s.substring(0, 1) + s.substring(1).toLowerCase();
    }
    
    public static String[] getTypeNames() {
        return new String[] { ChannelDataTypes.STRING.toString(), 
                              ChannelDataTypes.DOUBLE.toString(),
                              ChannelDataTypes.LONG.toString(),
                              ChannelDataTypes.INTEGER.toString(),
                              ChannelDataTypes.BOOLEAN.toString(),
                              ChannelDataTypes.SERIALIZABLE.toString(),
                              ChannelDataTypes.FILEREFERENCE.toString(),
                              ChannelDataTypes.VARIANTARRAY.toString()};
    }
    
    /**
     * Gives back the enum value of a string.
     * 
     * @param s string representation or class name to convert to enum
     * @return enum value or null if not matching
     */
    public static ChannelDataTypes toEnum(final String s) {
        ChannelDataTypes resType = null;
        if (s.equals(ChannelDataTypes.STRING.toString()) || s.equals(String.class.getName())) {
            resType = STRING;
        } else if (s.equals(ChannelDataTypes.DOUBLE.toString()) || s.equals(Double.class.getName())) {
            resType = DOUBLE;
        } else if (s.equals(ChannelDataTypes.LONG.toString()) || s.equals(Long.class.getName())) {
            resType = LONG;
        } else if (s.equals(ChannelDataTypes.INTEGER.toString()) || s.equals(Integer.class.getName())) {
            resType = INTEGER;
        } else if (s.equals(ChannelDataTypes.BOOLEAN.toString()) || s.equals(Boolean.class.getName())) {
            resType = BOOLEAN;
        } else if (s.equals(ChannelDataTypes.SERIALIZABLE.toString()) || s.equals(Serializable.class.getName())) {
            resType = SERIALIZABLE;
        } else if (s.equals(ChannelDataTypes.FILEREFERENCE.toString()) || s.equals(DataManagementFileReference.class.getName())) {
            resType = FILEREFERENCE;
        } else if (s.equals(ChannelDataTypes.VARIANTARRAY.toString()) || s.equals(VariantArray.class.getName())) {
            resType = VARIANTARRAY;
        }
        
        return resType;
    }
    
    /**
     * Returns java class names of Excel data types.
     * 
     * @param edt specific ExcelDataTypes
     * @return java class name
     */
    public static String toClassName(ChannelDataTypes edt) {
        String name = null;
        switch(edt) {
        case STRING:
            name = String.class.getName();
            break;
        case DOUBLE:
            name = Double.class.getName();
            break;
        case LONG:
            name = Long.class.getName();
            break;
        case INTEGER:
            name = Integer.class.getName();
            break;
        case BOOLEAN:
            name = Boolean.class.getName();
            break;
        case SERIALIZABLE:
            name = Serializable.class.getName();
            break;
        case FILEREFERENCE:
            name =  DataManagementFileReference.class.getName();
            break;
        case VARIANTARRAY:
            name = VariantArray.class.getName();
            break;
        default:
            break;
        }
        
        return name;
    }
}
