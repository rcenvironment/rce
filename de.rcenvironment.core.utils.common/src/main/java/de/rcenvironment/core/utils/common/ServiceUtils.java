/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Class providing utility methods used for handling services.
 * 
 * @author Doreen Seider
 */
public final class ServiceUtils {

    private ServiceUtils() {}

    /**
     * Constructs the properties filter by the given properties map.
     * 
     * Note: This method is only used by test code, consider to remove it as it might got obsolete. --seid_do
     *
     * @param properties The map with the properties.
     * @return the properties filter string or <code>null</code> in case of empty or <code>null</code> properties map.
     * 
     */
    public static String constructFilter(Map<String, String> properties) {

        String filter = null;
        StringBuilder filterBuilder = new StringBuilder();

        if (properties != null && properties.size() > 0) {
            filterBuilder.append("(&");

            Map<String, String> serviceProperties = properties;
            for (Entry<String, String> entry : serviceProperties.entrySet()) {
                filterBuilder.append("(" + entry.getKey() + "=" + entry.getValue() + ")");
            }

            filterBuilder.append(")");
            filter = new String(filterBuilder);
        }

        return filter;
    }

    /**
     * Creates a proxy instance for the given interface that throws an exception when any method is invoked.
     * 
     * @param iface Java interface to create the Null object for.
     * @param <T> Same as iface.
     * @return the proxy instance.
     */
    @SuppressWarnings("unchecked")
    public static <T> T createFailingServiceProxy(final Class<T> iface) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] { iface },
            new InvocationHandler() {

                @Override
                public Object invoke(Object proxy, Method method, Object[] parameters) throws Throwable {
                    throw new IllegalStateException("Service not available: " + iface.getCanonicalName());
                }
            });
    }
}
