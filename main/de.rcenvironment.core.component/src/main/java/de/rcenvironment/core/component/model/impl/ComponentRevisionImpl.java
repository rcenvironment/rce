/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.impl;

import java.io.Serializable;

import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.api.ComponentRevision;

/**
 * A writable {@link ComponentRevision} implementation.
 * 
 * @author Robert Mischke
 */
public class ComponentRevisionImpl implements ComponentRevision, Serializable {

    // Note: Contributes only a few things at this point; will be filled in later
    private ComponentInterfaceImpl componentInterface;
    
    private String className;

    @Override
    public ComponentInterface getComponentInterface() {
        return componentInterface;
    }

    public void setComponentInterface(ComponentInterfaceImpl componentInterface) {
        this.componentInterface = componentInterface;
    }

    @Override
    public String toString() {
        return String.format("ComponentRevision(ci=%s)", componentInterface);
    }

    @Override
    public String getClassName() {
        return className;
    }
    
    public void setClassName(String className) {
        this.className = className;
    }
}
