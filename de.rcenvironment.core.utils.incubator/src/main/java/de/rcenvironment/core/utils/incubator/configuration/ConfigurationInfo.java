/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator.configuration;

/**
 * A {@link ConfigurationInfo} holds all the information about a configuration, basically its
 * {@link ConfigurationProperty}s.
 * 
 * @author Christian Weiss
 */
public interface ConfigurationInfo {

    /**
     * Gets the property names.
     * 
     * @return the property names
     */
    String[] getPropertyNames();

    /**
     * Gets the property.
     * 
     * @param propertyName the property name
     * @return the property
     */
    ConfigurationProperty getProperty(String propertyName);

}
