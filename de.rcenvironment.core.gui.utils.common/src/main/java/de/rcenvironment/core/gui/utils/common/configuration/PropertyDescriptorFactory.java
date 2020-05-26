/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.utils.common.configuration;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.views.properties.ColorPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

/**
 * A factory providing easy means to create {@link IPropertyDescriptor} instances to represent
 * properties of registered types, whereas property types for standard types (<code>Boolean, String,
 * Number, Enum</code>) are already provided.
 * 
 * @author Christian Weiss
 */
public final class PropertyDescriptorFactory {

    private static final Map<Class<?>, Class<? extends IPropertyDescriptor>> MAPPING =
        new HashMap<Class<?>, Class<? extends IPropertyDescriptor>>();

    static {
        MAPPING.put(Boolean.class, BooleanPropertyDescriptor.class);
        MAPPING.put(String.class, TextPropertyDescriptor.class);
        MAPPING.put(Number.class, TextPropertyDescriptor.class);
        MAPPING.put(Enum.class, EnumPropertyDescriptor.class);
        MAPPING.put(org.eclipse.swt.graphics.Color.class,
                ColorPropertyDescriptor.class);
        MAPPING.put(org.eclipse.swt.graphics.RGB.class,
                ColorPropertyDescriptor.class);
    }

    private PropertyDescriptorFactory() {
        //
    }

/**
     * Creates a new {@link IPropertyDescriptor> instance for the given property type.
     *
     * @param propertyType the property type
     * @param name the name of the property
     * @param displayName the display name of the property
     * @return the {@link IPropertyDescriptor> instance
     */
    public static IPropertyDescriptor createPropertyDescriptor(
            final Class<?> propertyType, final String name,
            final String displayName) {
        for (Map.Entry<Class<?>, Class<? extends IPropertyDescriptor>> entry : MAPPING
                .entrySet()) {
            final Class<?> baseClass = entry.getKey();
            if (baseClass.isAssignableFrom(propertyType)) {
                final Class<? extends IPropertyDescriptor> descriptorClass = entry
                        .getValue();
                try {
                    try {
                        final Constructor<? extends IPropertyDescriptor> constructor = descriptorClass
                                .getConstructor(Class.class, Object.class,
                                        String.class);
                        final IPropertyDescriptor descriptor = constructor
                                .newInstance(propertyType, name, displayName);
                        return descriptor;
                    } catch (NoSuchMethodException e) {
                        final Constructor<? extends IPropertyDescriptor> constructor = descriptorClass
                                .getConstructor(Object.class, String.class);
                        final IPropertyDescriptor descriptor = constructor
                                .newInstance(name, displayName);
                        return descriptor;
                    }
                } catch (SecurityException e) {
                    throw new RuntimeException(e);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException(e);
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        final IPropertyDescriptor result;
        if (String.class.equals(propertyType)
                || Number.class.isAssignableFrom(propertyType)) {
            result = new TextPropertyDescriptor(name, displayName);
        } else if (Boolean.class.equals(propertyType)
                || boolean.class.equals(propertyType)) {
            result = new BooleanPropertyDescriptor(name, displayName);
        } else if (propertyType.isPrimitive()) {
            result = new TextPropertyDescriptor(name, displayName);
        } else {
            result = null;
        }
        return result;
    }
}
