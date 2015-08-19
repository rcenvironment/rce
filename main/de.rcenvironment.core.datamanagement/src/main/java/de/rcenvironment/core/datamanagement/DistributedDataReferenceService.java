/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement;

import java.util.Collection;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.datamanagement.commons.DataReference;

/**
 * Service that provides easy access to {@link DataReference}s by calling remote {@link DataReferenceService}s in the distributed system.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 * @author Jan Flink
 */

public interface DistributedDataReferenceService {

    /**
     * Retrieves a {@link DataReference} for a given key from the Catalog.
     * 
     * @param dataReferenceKey The key of the {@link DataReference} to return.
     * @param platform The {@link NodeIdentifier} of the platform to query. If <code>null</code> the reference will be gotten from the local
     *        platform.
     * @return the found {@link DataReference} as a clone.
     */
    DataReference getReference(String dataReferenceKey, NodeIdentifier platform);

    /**
     * Retrieves a {@link DataReference} for a given key by querying the Catalogs of the statically configured "known" platforms.
     * 
     * @param dataReferenceKey The key of the {@link DataReference} to return.
     * @return the found {@link DataReference} as a clone.
     */
    DataReference getReference(String dataReferenceKey);

    /**
     * Retrieves a {@link DataReference} for a given key by querying the Catalogs of the given platforms.
     * 
     * @param dataReferenceKey The key of the {@link DataReference} to return.
     * @param platforms the ids of the platforms to query
     * @return the found {@link DataReference} as a clone.
     */
    DataReference getReference(String dataReferenceKey, Collection<NodeIdentifier> platforms);
}
