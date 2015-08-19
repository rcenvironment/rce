/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc;

import java.io.Serializable;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Map;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;

/**
 * Service for creating proxies for remote OSGi services.
 * 
 * @author Dirk Rossow
 * @author Heinrich Wendel
 * @author Doreen Seider
 * @author Robert Mischke (refactoring for 3.0.0)
 */
public interface ServiceProxyFactory extends Serializable {

    /**
     * Creates a proxy for a remote OSGi service. Every invocation of a method of the proxy object
     * can throw a {@link CommunicationException} wrapped into an
     * {@link UndeclaredThrowableException}.
     * 
     * @param nodeId {@link NodeIdentifier} where the remote service is hosted.
     * @param serviceIface Interface of the desired service.
     * @param ifaces Interfaces of the expected return object. null if no additional interfaces
     *        expected.
     * @param serviceProperties The desired properties of the remote service. The properties will
     *        coupled by a logical And. null if no properties desired.
     * @return The proxy.
     */
    Object createServiceProxy(NodeIdentifier nodeId, Class<?> serviceIface, Class<?>[] ifaces,
        Map<String, String> serviceProperties);

    /**
     * Creates a proxy for a remote OSGi service. Every invocation of a method of the proxy object
     * can throw a {@link CommunicationException} wrapped into an
     * {@link UndeclaredThrowableException}.
     * 
     * @param nodeId {@link NodeIdentifier} where the remote service is hosted.
     * @param serviceIface Interface of the desired service.
     * @param ifaces Interfaces of the expected return object.
     * @param serviceProperties The desired properties of the remote service as LDAP name.
     * @return The proxy.
     */
    Object createServiceProxy(NodeIdentifier nodeId, Class<?> serviceIface, Class<?>[] ifaces, String serviceProperties);

}
