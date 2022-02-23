/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.model.api;

import de.rcenvironment.core.component.model.impl.ComponentInterfaceImpl;
import de.rcenvironment.core.component.model.impl.ComponentRevisionImpl;

/**
 * Creates {@link ComponentRevision} objects.
 * 
 * @author Doreen Seider
 */
public class ComponentRevisionBuilder {

    private ComponentRevisionImpl componentRevision;
    
    public ComponentRevisionBuilder() {
        componentRevision = new ComponentRevisionImpl();
    }
    
    /**
     * @param componentInterface related {@link ComponentInterface}
     * @return builder object for method chaining purposes
     */
    public ComponentRevisionBuilder setComponentInterface(ComponentInterface componentInterface) {
        componentRevision.setComponentInterface((ComponentInterfaceImpl) componentInterface);
        return this;
    }
    
    /**
     * @param className component's class name
     * @return builder object for method chaining purposes
     */
    public ComponentRevisionBuilder setClassName(String className) {
        componentRevision.setClassName(className);
        return this;
    }
    
    /**
     * @return {@link ComponentRevision} object built
     */
    public ComponentRevision build() {
        return componentRevision;
    }
}
