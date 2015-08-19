/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.utils.common.configuration;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource2;

/**
 * A class adapting a usual Java Bean to the {@link IPropertySource2} interface to be included e.g.
 * in the property view of eclipse.
 * 
 * @author Christian Weiss
 */
public abstract class BeanPropertySourceAdapter implements IPropertySource2 {

    private final Object bean;

    private final BeanInfo beanInfo;

    private final Map<String, java.beans.PropertyDescriptor> beanDescriptors = new HashMap<String, java.beans.PropertyDescriptor>();

    private final Map<String, IPropertyDescriptor> propertyDescriptors = new HashMap<String, IPropertyDescriptor>();

    private final Map<Object, Object> defaultValues = new HashMap<Object, Object>();

    private final Map<Object, Boolean> sets = new HashMap<Object, Boolean>();

    private IPropertyDescriptor[] propertyDescriptorsArray;

    public BeanPropertySourceAdapter(final Object source) {
        this.bean = source;
        try {
            beanInfo = Introspector.getBeanInfo(source.getClass());
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
        parse();
    }

    protected BeanInfo getBeanInfo() {
        return beanInfo;
    }

    private void parse() {
        for (final java.beans.PropertyDescriptor descriptor : beanInfo
                .getPropertyDescriptors()) {
            final String name = descriptor.getName();
            if (!isProperty(descriptor)) {
                continue;
            }
            final Class<?> propertyType = descriptor.getPropertyType();
            final IPropertyDescriptor propertyDescriptor;
            propertyDescriptor = createPropertyDescriptor(propertyType, name,
                    descriptor.getDisplayName());
            if (propertyDescriptor == null) {
                continue;
            }
            addProperty(descriptor, propertyDescriptor);
        }
        updatePropertyDescriptors();
    }

    protected IPropertyDescriptor createPropertyDescriptor(
            final Class<?> propertyType, final String name,
            final String displayName) {
        return PropertyDescriptorFactory.createPropertyDescriptor(propertyType,
                name, displayName);
    }

    protected boolean isProperty(final PropertyDescriptor descriptor) {
        if ("class".equals(descriptor.getName()) || descriptor.getPropertyType() == null) {
            return false;
        }
        return true;
    }

    private void addProperty(final PropertyDescriptor descriptor,
            final IPropertyDescriptor propertyDescriptor) {
        final String name = descriptor.getName();
        addDescriptor(descriptor);
        propertyDescriptors.put(name, propertyDescriptor);
        updatePropertyDescriptors();
    }

    protected void removeProperty(final PropertyDescriptor descriptor) {
        final String name = descriptor.getName();
        removeDescriptor(descriptor);
        propertyDescriptors.remove(name);
        updatePropertyDescriptors();
    }

    private void updatePropertyDescriptors() {
        this.propertyDescriptorsArray = propertyDescriptors.values().toArray(
                new IPropertyDescriptor[0]);
    }

    protected Object getBean() {
        return bean;
    }

    @Override
    public Object getEditableValue() {
        return bean.toString();
    }

    protected boolean isManaged(final Object id) {
        final String key = id.toString();
        return hasDescriptor(key);
    }

    private boolean hasDescriptor(final Object id) {
        final String key = id.toString();
        return beanDescriptors.containsKey(key);
    }

    private java.beans.PropertyDescriptor getDescriptor(final Object id) {
        final String key = id.toString();
        return beanDescriptors.get(key);
    }

    private void addDescriptor(final java.beans.PropertyDescriptor descriptor) {
        final String name = descriptor.getName();
        beanDescriptors.put(name, descriptor);
    }

    private void removeDescriptor(final java.beans.PropertyDescriptor descriptor) {
        final String name = descriptor.getName();
        beanDescriptors.remove(name);
    }

    @Override
    public IPropertyDescriptor[] getPropertyDescriptors() {
        return propertyDescriptorsArray;
    }

    protected IPropertyDescriptor getPropertyDescriptor(
            final String propertyName) {
        return propertyDescriptors.get(propertyName);
    }

    @Override
    public Object getPropertyValue(Object id) {
        final String key = id.toString();
        final java.beans.PropertyDescriptor descriptor = getDescriptor(key);
        Object result = null;
        if (descriptor != null) {
            final Object value = getValue(descriptor);
            result = value;
        }
        return result;
    }

    private Object getValue(final java.beans.PropertyDescriptor descriptor) {
        try {
            return descriptor.getReadMethod().invoke(bean);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isPropertyResettable(Object id) {
        return true;
    }

    @Override
    public void resetPropertyValue(Object id) {
        if (isPropertySet(id)) {
            setPropertyValue(id, defaultValues.get(id));
            defaultValues.remove(id);
            sets.put(id, false);
        }
    }

    @Override
    public void setPropertyValue(Object id, Object value) {
        final String key = id.toString();
        final java.beans.PropertyDescriptor descriptor = getDescriptor(key);
        if (descriptor != null) {
            final Class<?> propertyType = descriptor.getPropertyType();
            Object newValue = value;
            if (value != null) {
                if (String.class.equals(propertyType)) {
                    newValue = value.toString();
                } else if (Number.class.isAssignableFrom(propertyType)) {
                    newValue = createValue(propertyType, value.toString());
                } else if (Boolean.class.equals(propertyType)
                        || boolean.class.equals(propertyType)) {
                    if (value instanceof Boolean) {
                        newValue = (Boolean) value;
                    }
                } else if (propertyType.isPrimitive()) {
                    newValue = createValue(propertyType, value.toString());
                }
            }
            setValue(descriptor, newValue);
            if (!isPropertySet(id)) {
                defaultValues.put(id, getPropertyValue(id));
                sets.put(id, true);
            }
        }
    }

    private Object createValue(Class<?> type, Object... args) {
        Class<?>[] argsTypes = new Class[args.length];
        for (int index = 0; index < args.length; ++index) {
            argsTypes[index] = args[index].getClass();
        }
        Object value;
        try {
            value = type.getConstructor(argsTypes).newInstance(args);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return value;
    }

    private void setValue(final java.beans.PropertyDescriptor descriptor,
            final Object value) {
        final Method setter = descriptor.getWriteMethod();
        try {
            setter.invoke(bean, value);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isPropertySet(final Object id) {
        return sets.get(id) != null && sets.get(id);
    }

}
