/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement;

import java.util.Collection;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.datamanagement.commons.DataReference;

/**
 * Service that provides easy access to {@link DataReference}s by calling remote {@link DataReferenceService}s in the distributed system.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 * @author Jan Flink
 */

public interface DataReferenceService {

    /**
     * Retrieves a {@link DataReference} for a given key from the Catalog.
     * 
     * @param dataReferenceKey The key of the {@link DataReference} to return.
     * @param platform The {@link InstanceNodeSessionId} of the platform to query. If <code>null</code> the reference will be gotten from
     *        the local platform.
     * @return the found {@link DataReference} as a clone.
     * @throws CommunicationException in case of communication error
     */
    DataReference getReference(String dataReferenceKey, NetworkDestination platform) throws CommunicationException;

    /**
     * Retrieves a {@link DataReference} for a given key by querying the Catalogs of the statically configured "known" platforms.
     * 
     * @param dataReferenceKey The key of the {@link DataReference} to return.
     * @return the found {@link DataReference} as a clone.
     * @throws CommunicationException in case of communication error
     */
    @Deprecated // undefined storage locations are not used anymore; remove from code base
    DataReference getReference(String dataReferenceKey) throws CommunicationException;

    /**
     * Retrieves a {@link DataReference} for a given key by querying the Catalogs of the given platforms.
     * 
     * @param dataReferenceKey The key of the {@link DataReference} to return.
     * @param platforms the ids of the platforms to query
     * @return the found {@link DataReference} as a clone.
     * @throws CommunicationException in case of communication error
     */
    @Deprecated // undefined storage locations are not used anymore; remove from code base
    DataReference getReference(String dataReferenceKey, Collection<? extends NetworkDestination> platforms) throws CommunicationException;
}
