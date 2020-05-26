/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.variables.legacy;


/**
 * Represents the type of a bound variable.
 *
 * @author Arne Bachmann
 */
@Deprecated
public enum VariableType {
    
    /**
     * Anything that can be represented by a string.
     */
    String,
    
    /**
     * Integer number.
     */
    Integer,
    
    /**
     * Floating point number.
     */
    Real,
    
    /**
     * True or false.
     */
    Logic,
    
    /**
     * Date.
     */
    Date,
    
    /**
     * A file reference.
     */
    File,
    
    /**
     * Empty type for non-defined array cells and Excel cells.
     */
    Empty,
    
    /**
     * Special marker for RLE.
     */
    RLE;

}
