/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.utils.common.configuration;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySource2;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import de.rcenvironment.core.gui.utils.common.configuration.ConfigurationViewerContentProvider.ArraySource;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.configuration.ConfigurationInfo;
import de.rcenvironment.core.utils.incubator.configuration.ConfigurationIntrospector;
import de.rcenvironment.core.utils.incubator.configuration.annotation.Configurable;
import de.rcenvironment.core.utils.incubator.configuration.annotation.Configuration;

/**
 * A {@link IConfigurationSource} to wrap configuration beans to be displayed in a
 * {@link ConfigurationViewer}.
 * 
 * @author Christian Weiss
 */
public class BeanConfigurationSourceAdapter extends BeanPropertySourceAdapter
        implements IConfigurationSource {

    /**
     * The {@link IAdapterFactory} responsible for generating the
     * {@link BeanConfigurationSourceAdapter}s.
     * 
     * @author Christian Weiss
     */
    public static final class Factory implements IAdapterFactory {

        /** The adapter classes provided through {@link BeanConfigurationSourceAdapter}. */
        private static final Class<?>[] ADAPTER_CLASSES = new Class[] { IPropertySource.class, IPropertySource2.class,
            IConfigurationSource.class };

        /** The Constant applicable. */
        private static final ThreadLocal<Boolean> APPLICABLE = new ThreadLocal<Boolean>();

        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.core.runtime.IAdapterFactory#getAdapterList()
         */
        @SuppressWarnings("rawtypes")
        @Override
        public Class[] getAdapterList() {
            return ADAPTER_CLASSES;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.core.runtime.IAdapterFactory#getAdapter(java.lang.Object,
         *      java.lang.Class)
         */
        @SuppressWarnings("rawtypes")
        @Override
        public Object getAdapter(final Object adaptableObject,
                final Class adapterType) {
            boolean configuration = false;
            Class<?> clazz = adaptableObject.getClass();
            while (clazz.getSuperclass() != null) {
                if (clazz.isAnnotationPresent(Configuration.class)) {
                    configuration = true;
                    break;
                }
                clazz = clazz.getSuperclass();
            }
            if (!configuration //
                || (APPLICABLE.get() != null && !APPLICABLE.get())) {
                return null;
            }
            APPLICABLE.set(false);
            try {
                final Object adapter = AdapterManager.getInstance().getAdapter(
                        adaptableObject, adapterType);
                if (adapter != null) {
                    return adapter;
                }
            } finally {
                APPLICABLE.set(true);
            }
            return new BeanConfigurationSourceAdapter(adaptableObject);
        }

    }

    static {
        AdapterManager.getInstance().registerAdapters(new Factory(),
            Object.class);
        AdapterManager.getInstance().registerAdapters(
            new ArrayConfigurationSourceAdapter.Factory(), ArraySource.class);
        AdapterManager.getInstance().registerAdapters(
            new ListConfigurationSourceAdapter.Factory(), List.class);
    }

    /** The {@link ConfigurationInfo} instance describing the configuration bean. */
    private final ConfigurationInfo configurationInfo;

    /** The mapping from property names to {@link IPropertyDescriptor}s. */
    private final Map<String, IPropertyDescriptor> configurationPropertyDescriptors = new HashMap<String, IPropertyDescriptor>();

    /** The {@link IPropertyDescriptor} representing the configuration properties. */
    private IPropertyDescriptor[] configurationPropertyDescriptorsArray;

    /**
     * The constructor.
     * 
     * @param source the configuration bean
     */
    public BeanConfigurationSourceAdapter(final Object source) {
        super(source);
        configurationInfo = ConfigurationIntrospector
                .getConfigurationInfo(source.getClass());
        parse();
    }

    /**
     * Initializes the {@link AdapterManager} with the generic {@link IAdapterFactory}s.
     */
    public static void initialize() {
        //
    }

    /**
     * Parses the backing configuration bean.
     */
    private void parse() {
        final List<IPropertyDescriptor> configurationDescriptors = new LinkedList<IPropertyDescriptor>();
        configurationPropertyDescriptors.clear();
        for (final String propertyName : configurationInfo.getPropertyNames()) {
            // if the property is managed by the BeanPropertySourceAdapter use its
            // PropertyDescriptor, otherwise it is a complex configuration property which has to be
            // handled appropriately
            if (super.isManaged(propertyName)) {
                configurationDescriptors.add(getPropertyDescriptor(propertyName));
            } else {
                final PropertyDescriptor descriptor;
                // get a ValueProvider, if present to determine whether its supposed to be a fixed
                // set of select options displayed in a select widget
                final Configurable.ValueProvider valueProvider = configurationInfo
                        .getProperty(propertyName).getValueProvider();
                // if there is no ValueProvider, it is an ordinary property,
                // otherwise it is a property which shall be manipulated through a select widget
                if (valueProvider == null) {
                    descriptor = new PropertyDescriptor(propertyName, propertyName);
                } else {
                    SelectionPropertyDescriptor.ValueProvider valueProvider2 = null;
                    if (valueProvider != null) {
                        valueProvider.setObject(getBean());
                        valueProvider2 = new SelectionPropertyDescriptor.ValueProvider() {

                            @Override
                            public Object[] getValues() {
                                return valueProvider.getValues();
                            }

                        };
                    }
                    descriptor = new SelectionPropertyDescriptor(propertyName, propertyName, valueProvider2);
                }
                // prepare the LabelProvider for the property
                final Configurable.LabelProvider labelProvider = configurationInfo
                    .getProperty(propertyName).getLabelProvider();
                final ILabelProvider propertyLabelProvider = new LabelProvider() {

                    @Override
                    public String getText(final Object element) {
                        return labelProvider.getLabel(element);
                    }

                };
                descriptor.setLabelProvider(propertyLabelProvider);
                configurationDescriptors.add(descriptor);
                configurationPropertyDescriptors.put(propertyName, descriptor);
            }
        }
        configurationPropertyDescriptorsArray = configurationDescriptors
                .toArray(new IPropertyDescriptor[configurationDescriptors
                        .size()]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see de.rcenvironment.core.gui.utils.common.configuration.IConfigurationSource#getConfigurationPropertyDescriptors()
     */
    @Override
    public IPropertyDescriptor[] getConfigurationPropertyDescriptors() {
        return configurationPropertyDescriptorsArray;
    }

    /**
     * {@inheritDoc}
     * 
     * @see de.rcenvironment.core.gui.utils.common.configuration.BeanPropertySourceAdapter#isManaged(java.lang.Object)
     */
    @Override
    protected boolean isManaged(final Object id) {
        if (super.isManaged(id)) {
            return true;
        }
        final String key = id.toString();
        return configurationInfo.getProperty(key) != null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see de.rcenvironment.core.gui.utils.common.configuration.BeanPropertySourceAdapter#setPropertyValue(java.lang.Object,
     *      java.lang.Object)
     */
    @Override
    public void setPropertyValue(Object id, Object value) {
        if (super.isManaged(id)) {
            super.setPropertyValue(id, value);
            return;
        }
        final String propertyName = id.toString();
        configurationInfo.getProperty(propertyName).setValue(getBean(), value);
    }

    /**
     * {@inheritDoc}
     * 
     * @see de.rcenvironment.core.gui.utils.common.configuration.BeanPropertySourceAdapter#getPropertyValue(java.lang.Object)
     */
    @Override
    public Object getPropertyValue(Object id) {
        if (super.isManaged(id)) {
            return super.getPropertyValue(id);
        }
        final String propertyName = id.toString();
        Object result = configurationInfo.getProperty(propertyName).getValue(
                getBean());
        return result;
    }

    /**
     * An {@link IConfigurationSource} to adapt arrays of values to the {@link ConfigurationViewer}.
     */
    public static final class ArrayConfigurationSourceAdapter implements
            IConfigurationSource {

        /**
         * The {@link IAdapterFactory} class to generate instances of
         * {@link ArrayConfigurationSourceAdapter} out of arrays.
         */
        public static final class Factory implements IAdapterFactory {

            /** The Constant ADAPTER_CLASSES. */
            private static final Class<?>[] ADAPTER_CLASSES = new Class[] { IPropertySource.class, IPropertySource2.class,
                IConfigurationSource.class };

            /** The Constant applicable. */
            private static final ThreadLocal<Boolean> APPLICABLE = new ThreadLocal<Boolean>();

            /**
             * {@inheritDoc}
             * 
             * @see org.eclipse.core.runtime.IAdapterFactory#getAdapterList()
             */
            @SuppressWarnings("rawtypes")
            @Override
            public Class[] getAdapterList() {
                return ADAPTER_CLASSES;
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.eclipse.core.runtime.IAdapterFactory#getAdapter(java.lang.Object,
             *      java.lang.Class)
             */
            @SuppressWarnings("rawtypes")
            @Override
            public Object getAdapter(final Object adaptableObject,
                    final Class adapterType) {
                boolean applicable = true;
                if ((APPLICABLE.get() != null && !APPLICABLE.get())) {
                    applicable = false;
                }
                if (adaptableObject.getClass().getComponentType() == null //
                    && !(adaptableObject instanceof ArraySource)) {
                    applicable = false;
                }
                Object result = null;
                if (applicable) {
                    APPLICABLE.set(false);
                    try {
                        final Object adapter = Platform.getAdapterManager()
                            .getAdapter(adaptableObject, adapterType);
                        if (adapter != null) {
                            return adapter;
                        }
                    } finally {
                        APPLICABLE.set(true);
                    }
                    if (adaptableObject.getClass().getComponentType() != null) {
                        final Object[] array = (Object[]) adaptableObject;
                        result = new ArrayConfigurationSourceAdapter(array);
                    } else if (adaptableObject instanceof ArraySource) {
                        final ArraySource<?> arraySource = (ArraySource<?>) adaptableObject;
                        result = new ArrayConfigurationSourceAdapter(arraySource);
                    } else {
                        throw new AssertionError();
                    }
                }
                return result;
            }

        }

        /** The base array. */
        private final Object[] array;

        /** The {@link ArraySource}. */
        private final ArraySource<?> arraySource;

        /**
         * Instantiates a new {@link ArrayConfigurationSourceAdapter}.
         * 
         * @param array the array
         */
        public ArrayConfigurationSourceAdapter(final Object[] array) {
            this.array = array;
            this.arraySource = null;
        }

        /**
         * Instantiates a new {@link ArrayConfigurationSourceAdapter}.
         * 
         * @param array the array
         */
        public ArrayConfigurationSourceAdapter(final ArraySource<?> arraySource) {
            this.array = null;
            this.arraySource = arraySource;
        }

        private Object[] getArray() {
            if (array != null) {
                return array;
            } else {
                return arraySource.getValue();
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.ui.views.properties.IPropertySource#getEditableValue()
         */
        @Override
        public Object getEditableValue() {
            final Object[] arrayRef = getArray();
            return StringUtils.format("%s [%d]", arrayRef.getClass().getComponentType()
                    .getSimpleName(), arrayRef.length);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.ui.views.properties.IPropertySource2#isPropertySet(java.lang.Object)
         */
        @Override
        public boolean isPropertySet(Object id) {
            return true;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.ui.views.properties.IPropertySource#getPropertyValue(java.lang.Object)
         */
        @Override
        public Object getPropertyValue(Object id) {
            final Object[] arrayRef = getArray();
            final Integer index = (Integer) id;
            return arrayRef[index];
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.ui.views.properties.IPropertySource2#isPropertyResettable(java.lang.Object)
         */
        @Override
        public boolean isPropertyResettable(Object id) {
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.ui.views.properties.IPropertySource#resetPropertyValue(java.lang.Object)
         */
        @Override
        public void resetPropertyValue(Object id) {
            //
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.ui.views.properties.IPropertySource#getPropertyDescriptors()
         */
        @Override
        public IPropertyDescriptor[] getPropertyDescriptors() {
            final Object[] arrayRef = getArray();
            IPropertyDescriptor[] descriptors = new IPropertyDescriptor[arrayRef.length];
            for (int index = 0; index < descriptors.length; ++index) {
                descriptors[index] = new PropertyDescriptor(new Integer(
                            index), StringUtils.format("[%d]", index));
            }
            return descriptors;
        }

        /**
         * {@inheritDoc}
         * 
         * @see de.rcenvironment.core.gui.utils.common.configuration.IConfigurationSource#getConfigurationPropertyDescriptors()
         */
        @Override
        public IPropertyDescriptor[] getConfigurationPropertyDescriptors() {
            return getPropertyDescriptors();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.ui.views.properties.IPropertySource#setPropertyValue(java.lang.Object,
         *      java.lang.Object)
         */
        @Override
        public void setPropertyValue(Object id, Object value) {
            final Object[] arrayRef = getArray();
            final Integer index = (Integer) id;
            arrayRef[index] = value;
        }

    }

    /**
     * An {@link IConfigurationSource} to adapt {@link List}s of values to the
     * {@link ConfigurationViewer}.
     */
    public static final class ListConfigurationSourceAdapter implements
            IConfigurationSource {

        /**
         * The {@link IAdapterFactory} class to generate instances of
         * {@link ArrayConfigurationSourceAdapter} out of arrays.
         */
        public static final class Factory implements IAdapterFactory {

            /** The Constant ADAPTER_CLASSES. */
            private static final Class<?>[] ADAPTER_CLASSES = new Class[] { IPropertySource.class, IPropertySource2.class,
                IConfigurationSource.class };

            /** The Constant applicable. */
            private static final ThreadLocal<Boolean> APPLICABLE = new ThreadLocal<Boolean>();

            /**
             * {@inheritDoc}
             * 
             * @see org.eclipse.core.runtime.IAdapterFactory#getAdapterList()
             */
            @SuppressWarnings("rawtypes")
            @Override
            public Class[] getAdapterList() {
                return ADAPTER_CLASSES;
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.eclipse.core.runtime.IAdapterFactory#getAdapter(java.lang.Object,
             *      java.lang.Class)
             */
            @SuppressWarnings("rawtypes")
            @Override
            public Object getAdapter(final Object adaptableObject,
                    final Class adapterType) {
                if ((APPLICABLE.get() != null && !APPLICABLE.get())) {
                    return null;
                }
                APPLICABLE.set(false);
                try {
                    final Object adapter = Platform.getAdapterManager()
                            .getAdapter(adaptableObject, adapterType);
                    if (adapter != null) {
                        return adapter;
                    }
                } finally {
                    APPLICABLE.set(true);
                }
                final List<?> list = (List<?>) adaptableObject;
                return new ListConfigurationSourceAdapter(list);
            }

        }

        /** The base list. */
        private final List<?> list;

        /**
         * Instantiates a new {@link ListConfigurationSourceAdapter}.
         * 
         * @param list the list
         */
        public ListConfigurationSourceAdapter(final List<?> list) {
            this.list = list;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.ui.views.properties.IPropertySource#getEditableValue()
         */
        @Override
        public Object getEditableValue() {
            return StringUtils.format("? [%d]", list.size());
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.ui.views.properties.IPropertySource2#isPropertySet(java.lang.Object)
         */
        @Override
        public boolean isPropertySet(Object id) {
            return true;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.ui.views.properties.IPropertySource#getPropertyValue(java.lang.Object)
         */
        @Override
        public Object getPropertyValue(Object id) {
            final Integer index = (Integer) id;
            return list.get(index);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.ui.views.properties.IPropertySource2#isPropertyResettable(java.lang.Object)
         */
        @Override
        public boolean isPropertyResettable(Object id) {
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.ui.views.properties.IPropertySource#resetPropertyValue(java.lang.Object)
         */
        @Override
        public void resetPropertyValue(Object id) {
            //
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.ui.views.properties.IPropertySource#getPropertyDescriptors()
         */
        @Override
        public IPropertyDescriptor[] getPropertyDescriptors() {
            final IPropertyDescriptor[] descriptors = new IPropertyDescriptor[list.size()];
            for (int index = 0; index < descriptors.length; ++index) {
                descriptors[index] = new PropertyDescriptor(new Integer(
                            index), StringUtils.format("[%d]", index));
            }
            return descriptors;
        }

        /**
         * {@inheritDoc}
         * 
         * @see de.rcenvironment.core.gui.utils.common.configuration.IConfigurationSource#getConfigurationPropertyDescriptors()
         */
        @Override
        public IPropertyDescriptor[] getConfigurationPropertyDescriptors() {
            return getPropertyDescriptors();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.ui.views.properties.IPropertySource#setPropertyValue(java.lang.Object,
         *      java.lang.Object)
         */
        @Override
        public void setPropertyValue(Object id, Object value) {
            throw new UnsupportedOperationException();
        }

    }

}
