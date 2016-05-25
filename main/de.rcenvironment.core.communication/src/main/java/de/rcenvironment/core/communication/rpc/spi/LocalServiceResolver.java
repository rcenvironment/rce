/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.spi;

/**
 * Resolves remote service requests by name against local service implementations.
 * 
 * @author Robert Mischke
 */
public interface LocalServiceResolver {

    /**
     * @param serviceName the requested service's name
     * @return the wrapped matching local service instance, if it exists; otherwise, null
     */
    Object getLocalService(String serviceName);

}
