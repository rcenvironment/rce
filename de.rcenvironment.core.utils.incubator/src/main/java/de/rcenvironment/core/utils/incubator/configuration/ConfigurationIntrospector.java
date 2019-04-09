/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator.configuration;

import java.beans.BeanInfo;
import java.beans.EventSetDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;

import de.rcenvironment.core.utils.incubator.configuration.annotation.Configurable;

/**
 * The {@link ConfigurationIntrospector} class provides a standard way for tools to learn about the
 * {@link ConfigurationProperty}s supported by a Java Bean.
 * 
 * @author Christian Weiss
 */
public final class ConfigurationIntrospector {

    /** The single instance of this class. */
    private static final ConfigurationIntrospector INSTANCE = new ConfigurationIntrospector();

    /**
     * Instantiates a new {@link ConfigurationIntrospector}.
     */
    private ConfigurationIntrospector() {
        //
    }

    /**
     * Introspects the given class an results the {@link ConfigurationInfo} thereof.
     * 
     * @param clazz the configuration class
     * @return the {@link ConfigurationInfo}
     */
    public static ConfigurationInfo getConfigurationInfo(final Class<?> clazz) {
        return INSTANCE.parse(clazz);
    }

    /**
     * Returns the {@link BeanInfo} of the given class.
     * 
     * @param clazz the Java Bean class
     * @return the {@link BeanInfo}
     */
    private BeanInfo getBeanInfo(final Class<?> clazz) {
        try {
            return Introspector.getBeanInfo(clazz);
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Parses given class an results the {@link ConfigurationInfo} thereof.
     * 
     * @param clazz the configuration class
     * @return the {@link ConfigurationInfo}
     */
    private ConfigurationInfo parse(final Class<?> clazz) {
        final ConfigurationInfoImpl result = new ConfigurationInfoImpl();
        final BeanInfo beanInfo = getBeanInfo(clazz);
        EventSetDescriptor propertyChangeEventSetDescriptor = null;
        for (final EventSetDescriptor descriptor : beanInfo.getEventSetDescriptors()) {
            if (PropertyChangeListener.class.isAssignableFrom(descriptor.getListenerType())) {
                propertyChangeEventSetDescriptor = descriptor;
            }
        }
        for (final PropertyDescriptor descriptor : beanInfo
                .getPropertyDescriptors()) {
            if (!isConfigurable(descriptor)) {
                continue;
            }
            result.addProperty(descriptor, propertyChangeEventSetDescriptor);
        }
        return result;
    }

    /**
     * Checks if the given Java Bean property shall be considered to represent a
     * {@link ConfigurationProperty}.
     * 
     * @param descriptor the Java Bean {@link PropertyDescriptor}
     * @return true, if is property might be a {@link ConfigurationProperty}
     */
    private boolean isProperty(final PropertyDescriptor descriptor) {
        if ("class".equals(descriptor.getName()) || descriptor.getPropertyType() == null) {
            return false;
        }
        return true;
    }

    /**
     * Checks if the given Java Bean property is a {@link ConfigurationProperty}.
     * 
     * @param descriptor the Java Bean {@link PropertyDescriptor}
     * @return true, if is property might be a {@link ConfigurationProperty}
     */
    private boolean isConfigurable(final PropertyDescriptor descriptor) {
        if (!isProperty(descriptor)) {
            return false;
        }
        if (descriptor.getReadMethod().isAnnotationPresent(Configurable.class)) {
            final Configurable configurable = descriptor.getReadMethod()
                    .getAnnotation(Configurable.class);
            return configurable.value();
        }
        return false;
    }

}
