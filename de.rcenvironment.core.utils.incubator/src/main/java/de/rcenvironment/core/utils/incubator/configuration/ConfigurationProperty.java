/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator.configuration;

import de.rcenvironment.core.utils.incubator.configuration.annotation.Configurable;

/**
 * The property of a configuration in a {@link ConfigurationInfo}.
 * 
 * @author Christian Weiss
 */
public interface ConfigurationProperty {

    /**
     * Returns the {@link Configurable.ValueProvider}.
     * 
     * @return the {@link Configurable.ValueProvider}
     */
    Configurable.ValueProvider getValueProvider();

    /**
     * Returns the {@link Configurable.LabelProvider}.
     * 
     * @return the {@link Configurable.LabelProvider}
     */
    Configurable.LabelProvider getLabelProvider();

    /**
     * Returns the type of this property.
     * 
     * @return the type
     */
    Class<?> getType();

    /**
     * Gets the value.
     * 
     * @param object the object
     * @return the value
     * @throws IllegalArgumentException the illegal argument exception
     */
    Object getValue(Object object) throws IllegalArgumentException;

    /**
     * Sets the value of this property.
     * 
     * @param object the object
     * @param value the value
     * @throws IllegalArgumentException the illegal argument exception
     */
    void setValue(Object object, Object value) throws IllegalArgumentException;

}
