/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement;

import java.io.IOException;
import java.io.InputStream;

import de.rcenvironment.core.authorization.AuthorizationException;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.datamanagement.commons.DataReference;
import de.rcenvironment.core.datamanagement.commons.MetaData;
import de.rcenvironment.core.datamanagement.commons.MetaDataSet;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Service that provides easy access to all file data by calling remote {@link RemotableFileDataService}s in the distributed system.
 * 
 * @author Doreen Seider
 * @author Brigitte Boden
 */

public interface FileDataService {

    /**
     * Returns the {@link InputStream} of the given revision of the given {@link DataReference}.
     * 
     * @param dataReference {@link DataReference} which contains the needed revision.
     * @return {@link InputStream} of the given revision.
     * @throws AuthorizationException If the user has no read permission.
     * @throws CommunicationException in case of communication error
     */
    InputStream getStreamFromDataReference(DataReference dataReference)
        throws AuthorizationException, CommunicationException;

    /**
     * Returns the {@link InputStream} of the given revision of the given {@link DataReference}.
     * 
     * @param dataReference {@link DataReference} which contains the needed revision.
     * @param decompress if set to true (default), the decompressed version of the object will be used, if it is stored in compressed form.
     * @return {@link InputStream} of the given revision.
     * @throws AuthorizationException If the user has no read permission.
     * @throws CommunicationException in case of communication error
     */
    InputStream getStreamFromDataReference(DataReference dataReference, boolean decompress)
        throws AuthorizationException, CommunicationException;

    /**
     * Creates a new {@link DataReference} from the given {@link InputStream} on the platform represented by the given
     * {@link NetworkDestination}. The new {@link DataReference} will contain the given {@link MetaData} and reserved {@link MetaData} which
     * will be set automatically.
     * 
     * @param inputStream InputStream that shall be saved.
     * @param metaDataSet MetaDataSet that shall be saved.
     * @param platform {@link NetworkDestination} of the platform to store the reference. If <code>null</code> the new reference will be
     *        created on the local platform.
     * @return DataReference for the given InputStream and MetaData.
     * @throws AuthorizationException If the user or the extension has no create permission.
     * @throws IOException on upload failure
     * @throws InterruptedException on thread interruption
     * @throws CommunicationException in case of communication error
     */
    DataReference newReferenceFromStream(InputStream inputStream, MetaDataSet metaDataSet,
        NetworkDestination platform) throws AuthorizationException, IOException, InterruptedException, CommunicationException;

    /**
     * Creates a new {@link DataReference} from the given {@link InputStream} on the platform represented by the given
     * {@link NodeIdentifier}. The new {@link DataReference} will contain the given {@link MetaData} and reserved {@link MetaData} which
     * will be set automatically.
     * 
     * @param inputStream InputStream that shall be saved.
     * @param metaDataSet MetaDataSet that shall be saved.
     * @param platform {@link NodeIdentifier} of the platform to store the reference. If <code>null</code> the new reference will be created
     *        on the local platform.
     * @param alreadyCompressed if the file is already compressed (if set, no compression will be applied)
     * @return DataReference for the given InputStream and MetaData.
     * @throws AuthorizationException If the user or the extension has no create permission.
     * @throws IOException on upload failure
     * @throws InterruptedException on thread interruption
     * @throws CommunicationException in case of communication error
     */
    DataReference newReferenceFromStream(InputStream inputStream, MetaDataSet metaDataSet,
        NetworkDestination platform, boolean alreadyCompressed) throws AuthorizationException, IOException, InterruptedException,
        CommunicationException;

    /**
     * Deletes a whole local or remote {@link DataReference} with all {@link Revision}s.
     * 
     * @param dataReference DataReference that shall be deleted.
     * @throws CommunicationException on communication error
     */
    void deleteReference(DataReference dataReference) throws CommunicationException;

    /**
     * Deletes a whole local {@link DataReference} with all {@link Revision}s.
     * 
     * @param binaryReferenceKey Key of the binary reference that shall be deleted.
     * @throws RemoteOperationException standard remote operation exception
     */
    void deleteReference(String binaryReferenceKey) throws RemoteOperationException;

}
