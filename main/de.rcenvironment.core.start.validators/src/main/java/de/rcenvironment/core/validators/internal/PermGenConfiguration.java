/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.validators.internal;

/**
 * Configuration for the PermGenMinimumValidator.
 * @author Sascha Zur
 *
 */
public class PermGenConfiguration {
    
    private String minimumPermGenSize;

    public String getMinimumPermGenSize() {
        return minimumPermGenSize;
    }

    public void setMinimumPermGenSize(String minimumPermGenSize) {
        this.minimumPermGenSize = minimumPermGenSize;
    }

}
