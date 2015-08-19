/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator.configuration;

import java.beans.EventSetDescriptor;
import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Map;

/**
 * The implementation class for {@link ConfigurationInfo}.
 * 
 * @author Christian Weiss
 */
public class ConfigurationInfoImpl implements ConfigurationInfo {

    /** The mapping of property names to {@link PropertyDescriptor}s. */
    private final Map<String, PropertyDescriptor> descriptors = new HashMap<String, PropertyDescriptor>();

    /** The mapping of property names to {@link ConfigurationProperty}s. */
    private final Map<String, ConfigurationProperty> properties = new HashMap<String, ConfigurationProperty>();

    /**
     * Adds a Java Bean property to the set of properties.
     * 
     * @param descriptor the Java Bean property
     */
    /* default */void addProperty(final PropertyDescriptor descriptor, final EventSetDescriptor propertyChangeEventSetDescriptor) {
        final String propertyName = descriptor.getName();
        descriptors.put(propertyName, descriptor);
        ConfigurationProperty property = new ConfigurationPropertyImpl(descriptor);
        properties.put(propertyName, property);

    }

    /**
     * {@inheritDoc}
     * 
     * @see de.rcenvironment.core.utils.incubator.configuration.ConfigurationInfo#getPropertyNames()
     */
    @Override
    public String[] getPropertyNames() {
        return properties.keySet().toArray(new String[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see de.rcenvironment.core.utils.incubator.configuration.ConfigurationInfo#getProperty(java.lang.String)
     */
    @Override
    public ConfigurationProperty getProperty(String propertyName) {
        if (!properties.containsKey(propertyName)) {
            throw new IllegalArgumentException();
        }
        return properties.get(propertyName);
    }

}
