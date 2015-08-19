/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement;

import java.io.IOException;
import java.io.InputStream;

import de.rcenvironment.core.authentication.User;
import de.rcenvironment.core.authorization.AuthorizationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.datamanagement.commons.DataReference;
import de.rcenvironment.core.datamanagement.commons.MetaData;
import de.rcenvironment.core.datamanagement.commons.MetaDataSet;

/**
 * Service that provides easy access to all file data by calling remote {@link FileDataService}s in the distributed system.
 * 
 * @author Doreen Seider
 */
public interface DistributedFileDataService extends DistributedDataService {

    /**
     * Returns the {@link InputStream} of the given revision of the given {@link DataReference}.
     * 
     * @param user The {@link User} of the user.
     * @param dataReference {@link DataReference} which contains the needed revision.
     * @return {@link InputStream} of the given revision.
     * @throws AuthorizationException If the user has no read permission.
     */
    InputStream getStreamFromDataReference(User user, DataReference dataReference)
        throws AuthorizationException;

    /**
     * Creates a new {@link DataReference} from the given {@link InputStream} on the platform represented by the given
     * {@link NodeIdentifier}. The new {@link DataReference} will contain the given {@link MetaData} and reserved {@link MetaData} which
     * will be set automatically.
     * 
     * @param user The {@link User} of the user.
     * @param inputStream InputStream that shall be saved.
     * @param metaDataSet MetaDataSet that shall be saved.
     * @param platform {@link NodeIdentifier} of the platform to store the reference. If <code>null</code> the new reference will be created
     *        on the local platform.
     * @return DataReference for the given InputStream and MetaData.
     * @throws AuthorizationException If the user or the extension has no create permission.
     * @throws IOException on upload failure
     * @throws InterruptedException on thread interruption
     */
    DataReference newReferenceFromStream(User user, InputStream inputStream, MetaDataSet metaDataSet,
        NodeIdentifier platform) throws AuthorizationException, IOException, InterruptedException;

}
