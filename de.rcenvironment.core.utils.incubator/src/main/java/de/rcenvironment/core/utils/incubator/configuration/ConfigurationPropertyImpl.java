/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator.configuration;

import java.beans.IndexedPropertyDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;

import de.rcenvironment.core.utils.incubator.configuration.annotation.Configurable;

/**
 * The default implementation class for {@link ConfigurationProperty}.
 * 
 * @author Christian Weiss
 */
public class ConfigurationPropertyImpl implements ConfigurationProperty {

    private static final Configurable.LabelProvider DEFAULT_LABEL_PROVIDER = new Configurable.LabelProvider() {

        @Override
        public void setObject(final Object object) {
            //
        }

        @Override
        public String getLabel(final Object object) {
            return object.toString();
        }

    };

    private final PropertyDescriptor descriptor;

    private final Class<?> type;

    private final boolean indexed;

    private final Class<? extends Configurable.ValueProvider> valueProviderType;

    private final Class<? extends Configurable.LabelProvider> labelProviderType;

    public ConfigurationPropertyImpl(final PropertyDescriptor descriptor) {
        this(descriptor, //
            descriptor.getReadMethod().getAnnotation(Configurable.class).labelProvider(), //
            descriptor.getReadMethod().getAnnotation(Configurable.class).valueProvider());
    }

    public ConfigurationPropertyImpl(final PropertyDescriptor descriptor,
            final Class<? extends Configurable.LabelProvider> labelProviderType,
            final Class<? extends Configurable.ValueProvider> valueProviderType) {
        this.descriptor = descriptor;
        this.labelProviderType = labelProviderType;
        this.valueProviderType = valueProviderType;
        this.indexed = descriptor instanceof IndexedPropertyDescriptor;
        if (indexed) {
            this.type = descriptor.getReadMethod().getReturnType()
                    .getComponentType();
        } else {
            this.type = descriptor.getReadMethod().getReturnType();
        }
    }

    @Override
    public Configurable.ValueProvider getValueProvider() {
        if (valueProviderType == ConfigurationConstants.NO_VALUE_PROVIDER) {
            return null;
        }
        try {
            final Configurable.ValueProvider choiceProvider = valueProviderType.newInstance();
            return choiceProvider;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Configurable.LabelProvider getLabelProvider() {
        if (labelProviderType == ConfigurationConstants.NO_LABEL_PROVIDER) {
            return DEFAULT_LABEL_PROVIDER;
        }
        try {
            final Configurable.LabelProvider choiceProvider = labelProviderType.newInstance();
            return choiceProvider;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    @Override
    public Object getValue(final Object object) throws IllegalArgumentException {
        try {
            return descriptor.getReadMethod().invoke(object);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setValue(final Object object, final Object value) throws IllegalArgumentException {
        try {
            descriptor.getWriteMethod().invoke(object, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

}
