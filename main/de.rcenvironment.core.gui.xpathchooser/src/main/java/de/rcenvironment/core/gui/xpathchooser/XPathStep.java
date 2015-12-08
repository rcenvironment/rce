/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.xpathchooser;


/**
 * Abstract base class for all steps, i.e. AttributeAxis, NodeTests and Predicates.
 * 
 * @author Heinrich Wendel
 * @author Markus Kunde
 */
public abstract class XPathStep {

    /**
     * String representation of this step.
     */
    protected String xValue = "";

    /**
     * Sets the value.
     * @param value The value to set.
     */
    public void setValue(String value) {
        this.xValue = value;
    }

    /**
     * Returns the value without the predicate part.
     * @return Returns the value.
     */
    public String getValue() {
        return xValue;
    }
    
}
