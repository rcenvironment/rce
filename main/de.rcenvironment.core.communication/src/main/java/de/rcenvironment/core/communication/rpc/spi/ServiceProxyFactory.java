/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.spi;

import java.io.Serializable;
import java.lang.reflect.UndeclaredThrowableException;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;

/**
 * Service for creating proxies for remote OSGi services.
 * 
 * @author Dirk Rossow
 * @author Heinrich Wendel
 * @author Doreen Seider
 * @author Robert Mischke
 */
public interface ServiceProxyFactory extends Serializable {

    /**
     * Creates a proxy for a remote OSGi service. Every invocation of a method of the proxy object can throw a
     * {@link CommunicationException} wrapped into an {@link UndeclaredThrowableException}.
     * 
     * @param nodeId {@link NodeIdentifier} where the remote service is hosted.
     * @param serviceIface Interface of the desired service.
     * @param ifaces Interfaces of the expected return object. null if no additional interfaces expected.
     * @return The proxy.
     */
    Object createServiceProxy(NodeIdentifier nodeId, Class<?> serviceIface, Class<?>[] ifaces);

}
