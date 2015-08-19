/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator;

/**
 * Service that tracks {@link ListenerProvider}s, and registers listener services on their behalf.
 * This allows service implementations to provide listener implementations without having to
 * subclass them directly (see Mantis #9423).
 * 
 * @author Robert Mischke
 */
public interface ListenerRegistrationService {

    /**
     * Processes a new {@link ListenerProvider}. Each listener defined by
     * {@link ListenerProvider#defineListeners()} will be registered as an OSGi service.
     * 
     * @param listenerProvider the new {@link ListenerProvider}
     */
    void registerListenerProvider(ListenerProvider listenerProvider);

    /**
     * Removes a previously registered {@link ListenerProvider}. Each listener defined by
     * {@link ListenerProvider#defineListeners()} will be unregistered.
     * 
     * @param listenerProvider the {@link ListenerProvider} to remove
     */
    void unregisterListenerProvider(ListenerProvider listenerProvider);

}
