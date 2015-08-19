/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.internal;


/**
 * Example POJO to test configuration service.
 *
 * @author Heinrich Wendel
 */
public class DummyConfiguration {

    /** Boolean test value. */
    private boolean booleanValue = false;
    
    /** String test value. */
    private String stringValue = "123";
    
    /**
     * Getter.
     * @return Returns the booleanValue.
     */
    public boolean isBooleanValue() {
        return booleanValue;
    }

    /**
     * Setter.
     * @param booleanValue The booleanValue to set.
     */
    public void setBooleanValue(boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    /**
     * Getter.
     * @return Returns the stringValue.
     */
    public String getStringValue() {
        return stringValue;
    }

    /**
     * Setter.
     * @param stringValue The stringValue to set.
     */
    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }    
    
}
