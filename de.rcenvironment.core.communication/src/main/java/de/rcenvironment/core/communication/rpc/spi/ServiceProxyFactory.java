/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.spi;

import java.io.Serializable;
import java.lang.reflect.UndeclaredThrowableException;

import de.rcenvironment.core.communication.api.ReliableRPCStreamHandle;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.ResolvableNodeId;

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
     * @param nodeId the arbitrary-type node id to call the remote service at
     * @param serviceIface Interface of the desired service.
     * @param ifaces Interfaces of the expected return object. null if no additional interfaces expected.
     * @param reliableRPCStreamHandle the handle of the Reliable RPC Stream to use for all resulting RPCs, if this mechanism should be used;
     *        set this to null to use standard single-attempt RPCs
     * @return The proxy.
     */
    Object createServiceProxy(ResolvableNodeId nodeId, Class<?> serviceIface, Class<?>[] ifaces,
        ReliableRPCStreamHandle reliableRPCStreamHandle);

}
