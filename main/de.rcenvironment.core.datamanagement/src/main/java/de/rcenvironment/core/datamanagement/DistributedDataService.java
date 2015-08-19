/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement;

import de.rcenvironment.core.datamanagement.commons.DataReference;

/**
 * Service that provides easy access to all data by calling remote {@link DataService}s in the distributed system.
 * 
 * @author Doreen Seider
 */
public interface DistributedDataService {

    /**
     * Deletes a whole local or remote {@link DataReference} with all {@link Revision}s.
     * 
     * @param dataReference DataReference that shall be deleted.
     */
    void deleteReference(DataReference dataReference);

}
