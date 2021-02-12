/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.utils.common.configuration;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.Platform;

/**
 * A custom {@IAdapterManager} to facilitate a multi mapping of source to target
 * classes.
 * 
 * @author Christian Weiss
 */
public final class AdapterManager implements IAdapterManager {

    /** The single instance of this class. */
    private static final AdapterManager INSTANCE = new AdapterManager();

    /** The Constant mappings. */
    private static final Map<Class<?>, List<IAdapterFactory>> MAPPINGS = new HashMap<Class<?>, List<IAdapterFactory>>();

    /**
     * Instantiates a new adapter manager.
     */
    private AdapterManager() {
        //
    }

    /**
     * Gets the single instance of AdapterManager.
     * 
     * @return single instance of AdapterManager
     */
    public static AdapterManager getInstance() {
        return INSTANCE;
    }

    /**
     * Compute adapter types.
     * 
     * @param adaptableClass the adaptable class
     * @return the string[]
     */
    @SuppressWarnings("rawtypes")
    @Override
    public String[] computeAdapterTypes(final Class adaptableClass) {
        return Platform.getAdapterManager().computeAdapterTypes(adaptableClass);
    }

    /**
     * Compute class order.
     * 
     * @param clazz the clazz
     * @return the class[]
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Class[] computeClassOrder(final Class clazz) {
        return Platform.getAdapterManager().computeClassOrder(clazz);
    }

    /**
     * Gets the adapter.
     * 
     * @param adaptable the adaptable
     * @param adapterType the adapter type
     * @return the adapter
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Object getAdapter(final Object adaptable, final Class adapterType) {
        Object result = null;
        if (adaptable == null || adapterType == null) {
            throw new IllegalArgumentException();
        }
        final Class<?> adaptableType = adaptable.getClass();
        if (adapterType.isAssignableFrom(adaptableType)) {
            result = adaptable;
        } else {
            for (final Map.Entry<Class<?>, List<IAdapterFactory>> entry : MAPPINGS
                    .entrySet()) {
                final Class<?> entryType = entry.getKey();
                if (entryType.isAssignableFrom(adaptableType)) {
                    final List<IAdapterFactory> factories = entry.getValue();
                    for (final IAdapterFactory factory : factories) {
                        for (final Class<?> supportedAdapterType : factory
                                .getAdapterList()) {
                            if (adapterType.isAssignableFrom(supportedAdapterType)) {
                                final Object adapter = factory.getAdapter(
                                        adaptable, supportedAdapterType);
                                if (adapter != null) {
                                    result = adapter;
                                }
                            }
                        }
                    }
                }
            }
        }
        if (result == null) {
            result = Platform.getAdapterManager().getAdapter(adaptableType, adapterType);
        }
        return result;
    }

    /**
     * Gets the adapter.
     * 
     * @param adaptable the adaptable
     * @param adapterTypeName the adapter type name
     * @return the adapter
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Object getAdapter(final Object adaptable,
            final String adapterTypeName) {
        try {
            final Class adapterType = Class.forName(adapterTypeName);
            return getAdapter(adaptable, adapterType);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Checks for adapter.
     * 
     * @param adaptable the adaptable
     * @param adapterTypeName the adapter type name
     * @return true, if successful
     */
    @Override
    public boolean hasAdapter(final Object adaptable,
            final String adapterTypeName) {
        return getAdapter(adaptable, adapterTypeName) != null;
    }

    /**
     * Query adapter.
     * 
     * @param adaptable the adaptable
     * @param adapterTypeName the adapter type name
     * @return the int
     */
    @Override
    public int queryAdapter(final Object adaptable, final String adapterTypeName) {
        if (getAdapter(adaptable, adapterTypeName) != null) {
            return IAdapterManager.LOADED;
        } else {
            return IAdapterManager.NONE;
        }
    }

    /**
     * Load adapter.
     * 
     * @param adaptable the adaptable
     * @param adapterTypeName the adapter type name
     * @return the object
     */
    @Override
    public Object loadAdapter(final Object adaptable,
            final String adapterTypeName) {
        return null;
    }

    /**
     * Register adapters.
     * 
     * @param factory the factory
     * @param adaptable the adaptable
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void registerAdapters(final IAdapterFactory factory,
            final Class adaptable) {
        if (MAPPINGS.get(adaptable) == null) {
            MAPPINGS.put(adaptable, new LinkedList<IAdapterFactory>());
        }
        final List<IAdapterFactory> mapping = MAPPINGS.get(adaptable);
        if (!mapping.contains(factory)) {
            mapping.add(factory);
        }
    }

    /**
     * Unregister adapters.
     * 
     * @param factory the factory
     */
    @Override
    public void unregisterAdapters(final IAdapterFactory factory) {
        for (final Map.Entry<Class<?>, List<IAdapterFactory>> entry : MAPPINGS
                .entrySet()) {
            entry.getValue().remove(factory);
        }
    }

    /**
     * Unregister adapters.
     * 
     * @param factory the factory
     * @param adaptable the adaptable
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void unregisterAdapters(final IAdapterFactory factory,
            final Class adaptable) {
        if (MAPPINGS.get(adaptable) == null //
            || !MAPPINGS.get(adaptable).contains(factory)) {
            return;
        }
        MAPPINGS.get(adaptable).remove(factory);
    }

}
