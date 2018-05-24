/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement.internal;

/**
 * A configuration change entry.
 * 
 * @author Robert Mischke (redesign of ConfigurationPropertiesKey<T> by David Scholz)
 */
class InstanceConfigurationOperationDescriptor {

    // TODO consider replacing this with an enum
    private final String flag;

    private final Object[] parameters;

    InstanceConfigurationOperationDescriptor(String flag, Object... parameters) {
        this.flag = flag;
        this.parameters = parameters;
    }

    public String getFlag() {
        return flag;
    }

    public Object getSingleParameter() {
        return parameters[0];
    }

    public Object[] getParameters() {
        return parameters;
    }

}
