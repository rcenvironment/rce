/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.service;

/**
 * Service that tracks {@link AdditionalServicesProvider}s, and registers the declared additional services on their behalf. This allows
 * service implementations to provide service implementations without having to subclass them directly (see Mantis #9423).
 * 
 * @author Robert Mischke
 */
public interface AdditionalServicesRegistrationService {

    /**
     * Processes a new {@link AdditionalServicesProvider}. Each service implementation defined by
     * {@link AdditionalServicesProvider#defineAdditionalServices()} will be registered as an OSGi service.
     * 
     * @param additionalServicesProvider the new {@link AdditionalServicesProvider}
     */
    void registerAdditionalServicesProvider(AdditionalServicesProvider additionalServicesProvider);

    /**
     * Removes a previously registered {@link AdditionalServicesProvider}. Each service implementation defined by
     * {@link AdditionalServicesProvider#defineAdditionalServices()} will be unregistered.
     * 
     * @param additionalServicesProvider the {@link AdditionalServicesProvider} to remove
     */
    void unregisterAdditionalServicesProvider(AdditionalServicesProvider additionalServicesProvider);

}
