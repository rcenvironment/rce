/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement;

import de.rcenvironment.core.datamanagement.commons.DataReference;

/**
 * Common interface methods of the data services. This interface is not intended to be used by the user directly. Users should use the
 * {@link DataReference} type dependent services instead, e.g. {@link FileDataService} for DataReferenceType.fileObject.
 * 
 * @author Sandra Schroedter
 * @author Juergen Klein
 * 
 */
public interface DataService {

    /**
     * Deletes a whole local {@link DataReference} with all {@link Revision}s.
     * 
     * @param binaryReferenceKey Key of the binary reference that shall be deleted.
     */
    void deleteReference(String binaryReferenceKey);

}
