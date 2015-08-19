/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.parametricstudy.gui.view;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link PropertyChangeListener} translating the changes in the java bean it is registered for to
 * change requests on its given target java bean, thus passing through / synchronizing changes.
 * 
 * @author Christian Weiss
 */
public class PipedPropertyChangeListener implements PropertyChangeListener {

    /**
     * A converter translates value types between the to attributes.
     */
    public interface Converter {

        /**
         * Convert.
         * 
         * @param object the object
         * @return the object
         */
        Object convert(Object object);

        /**
         * Invert.
         * 
         * @param object the object
         * @return the object
         */
        Object invert(Object object);

    }

    /**
     * An abstract {@link Converter} class.
     */
    public abstract static class AbstractConverter implements Converter {

        /**
         * {@inheritDoc}
         * 
         * @see de.rcenvironment.components.parametricstudy.gui.view.PipedPropertyChangeListener.Converter#convert(java.lang.Object)
         */
        @Override
        public Object convert(Object object) {
            return object;
        }

        /**
         * {@inheritDoc}
         * 
         * @see de.rcenvironment.components.parametricstudy.gui.view.PipedPropertyChangeListener.Converter#invert(java.lang.Object)
         */
        @Override
        public Object invert(Object object) {
            return object;
        }

    }

    /** The target java bean. */
    private final Object target;

    /** The getter name mappings. */
    private final Map<String, String> getterNameMappings = new HashMap<String, String>();

    /** The setter name mappings. */
    private final Map<String, String> setterNameMappings = new HashMap<String, String>();

    /** The converter mappings. */
    private final Map<String, Converter> converterMappings = new HashMap<String, Converter>();

    /**
     * Instantiates a new piped property change listener.
     * 
     * @param target the target
     */
    public PipedPropertyChangeListener(final Object target) {
        this.target = target;
    }

    /**
     * Adds the name mapping.
     * 
     * @param from the from
     * @param to the to
     */
    public void addNameMapping(final String from, final String to) {
        addGetterNameMapping(from, to);
        addSetterNameMapping(from, to);
    }

    /**
     * Adds the getter name mapping.
     * 
     * @param from the from
     * @param to the to
     */
    public void addGetterNameMapping(final String from, final String to) {
        getterNameMappings.put(from, to);
    }

    /**
     * Adds the setter name mapping.
     * 
     * @param from the from
     * @param to the to
     */
    public void addSetterNameMapping(final String from, final String to) {
        setterNameMappings.put(from, to);
    }

    /**
     * Adds the converter mapping.
     * 
     * @param propertyName the property name
     * @param converter the converter
     */
    public void addConverterMapping(final String propertyName,
            final Converter converter) {
        converterMappings.put(propertyName, converter);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent event) {
        String propertyName = event.getPropertyName();
        Object value = event.getNewValue();
        if (converterMappings.containsKey(propertyName)) {
            value = converterMappings.get(propertyName).convert(value);
        }
        final Object currentValue;
        try {
            currentValue = getValue(propertyName);
        } catch (NoSuchFieldError e) {
            return;
        }
        if (currentValue == value) {
            return;
        }
        Class<?> parameterType = Object.class;
        if (value != null) {
            parameterType = value.getClass();
        }
        final Method setter = getSetter(propertyName, parameterType);
        if (setter == null) {
            return;
        }
        try {
            setter.invoke(target, value);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the value.
     * 
     * @param propertyName the property name
     * @return the value
     * @throws NoSuchFieldError the no such field error
     */
    private Object getValue(final String propertyName) throws NoSuchFieldError {
        String realPropertyName = propertyName;
        if (getterNameMappings.containsKey(propertyName)) {
            realPropertyName = getterNameMappings.get(propertyName);
        }
        try {
            final Method getter = getGetter(realPropertyName);
            if (getter == null) {
                return new NoSuchFieldError();
            }
            return getter.invoke(target);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the getter.
     * 
     * @param propertyName the property name
     * @return the getter
     */
    private Method getGetter(final String propertyName) {
        if (!propertyName.matches("[_a-zA-Z][_a-zA-Z0-9]*")) {
            return null;
        }
        final String[] prefixes = new String[] { "get", "is" };
        final String suffix = propertyName.substring(0, 1).toUpperCase()
                + propertyName.substring(1);
        Method getter = null;
        for (final String prefix : prefixes) {
            final String getterName = prefix + suffix;
            try {
                try {
                    getter = target.getClass().getMethod(getterName);
                    break;
                } catch (NoSuchMethodException e) {
                    e = null;
                }
            } catch (SecurityException e) {
                throw new RuntimeException(e);
            }
        }
        return getter;
    }

    /**
     * Gets the setter.
     * 
     * @param propertyName the property name
     * @param parameterType the parameter type
     * @return the setter
     */
    private Method getSetter(final String propertyName,
            final Class<?> parameterType) {
        String realPropertyName = propertyName;
        if (setterNameMappings.containsKey(propertyName)) {
            realPropertyName = setterNameMappings.get(propertyName);
        }
        if (!realPropertyName.matches("[_a-zA-Z][_a-zA-Z0-9]*")) {
            return null;
        }
        final String setterName = "set"
                + realPropertyName.substring(0, 1).toUpperCase()
                + realPropertyName.substring(1);
        Method setter = null;
        try {
            for (final Method method : target.getClass().getMethods()) {
                if (!setterName.equals(method.getName()) //
                    || method.getParameterTypes().length != 1) {
                    continue;
                }
                final Class<?> methodParameterType = method.getParameterTypes()[0];
                if (methodParameterType.isAssignableFrom(parameterType)) {
                    setter = method;
                    break;
                } else if (methodParameterType.isPrimitive()) {
                    // FIXME
                    setter = method;
                }
            }
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        }
        return setter;
    }

}
