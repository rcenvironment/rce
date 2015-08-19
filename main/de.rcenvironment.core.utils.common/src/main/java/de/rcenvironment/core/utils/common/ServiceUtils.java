/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

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
     * @param properties The map with the properties.
     * @return the properties filter string.
     */
    public static String constructFilter(Map<String, String> properties) {

        String filter = null;
        StringBuffer filterBuffer = new StringBuffer();

        if (properties != null && properties.size() > 0) {
            filterBuffer.append("(&");

            Map<String, String> serviceProperties = properties;
            for (String key : serviceProperties.keySet()) {
                filterBuffer.append("(" + key + "=" + serviceProperties.get(key) + ")");
            }

            filterBuffer.append(")");
            filter = new String(filterBuffer);
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
