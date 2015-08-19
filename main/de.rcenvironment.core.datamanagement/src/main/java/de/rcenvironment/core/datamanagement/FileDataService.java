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
import de.rcenvironment.core.datamanagement.commons.DataReference;
import de.rcenvironment.core.datamanagement.commons.MetaDataSet;

/**
 * Interface for the RCE user data system for file support.
 * 
 * @author Sandra Schroedter
 * @author Juergen Klein
 * 
 */
public interface FileDataService extends DataService {

    /**
     * Returns the InputStream of the given revision of dataReference.
     * 
     * @param proxyCertificate The {@link User} of the user.
     * @param dataReference DataReference which contains the needed revision.
     * @param calledFromRemote <code>true</code> if this method is called from remote node, otherwise <code>false</code>
     * @return InputStream of the given revision
     * @throws AuthorizationException If the user or the extension has no read permission.
     */
    InputStream getStreamFromDataReference(User proxyCertificate, DataReference dataReference, Boolean calledFromRemote)
        throws AuthorizationException;

    /**
     * Creates a new DataReference from the given inputStream on the Platform targetDataManagement. The new dataReference will contain the
     * given MetaData and reserved MetaData, that the DataInterface adds automatically.
     * 
     * @param proxyCertificate The {@link User} of the user.
     * @param inputStream InputStream that shall be saved.
     * @param metaDataSet MetaDataSet that shall be saved.
     * @return DataReference for the given InputStream and MetaData.
     * @throws AuthorizationException If the user or the extension has no create permission.
     */
    DataReference newReferenceFromStream(User proxyCertificate, InputStream inputStream, MetaDataSet metaDataSet)
        throws AuthorizationException;

    /**
     * Initializes an id/handle that subsequent {@link #appendToUpload(User, String, byte[])} calls can use to upload data. The normal use
     * case is that this method is called via RPC from the uploader's node.
     * 
     * @param user user data for authorization
     * @return the generated upload id
     * @throws IOException if the upload could not be set up (for example, no disk space left at receiver)
     */
    String initializeUpload(User user) throws IOException;

    /**
     * Appends a chunk of data to a virtual file identified by an upload id.The normal use case is that this method is called once or more
     * via RPC from the uploader's node.
     * 
     * @param user user data for authorization
     * @param id the assigned upload id
     * @param data the byte array to append
     * @return the total number of bytes written so far
     * 
     * @throws IOException on I/O errors on the receiver's side
     */
    long appendToUpload(User user, String id, byte[] data) throws IOException;

    /**
     * Signals that all data has been written via {@link #appendToUpload(User, String, byte[])} and initiates the asynchronous conversion
     * into a {@link DataReference} with the given {@link MetaDataSet} attached. This conversion is performed asynchronously to avoid RPC
     * timeouts on large uploads. Use {@link #pollUploadForDataReference(User, String)} to fetch the generated {@link DataReference}.
     * 
     * @param user user data for authorization
     * @param id the assigned upload id
     * @param metaDataSet the data management metadata to append to the {@link DataReference}; the behaviour is the same as
     *        {@link #newReferenceFromStream(User, InputStream, MetaDataSet)}
     * @throws IOException on I/O errors on the receiver's side
     */
    void finishUpload(User user, String id, MetaDataSet metaDataSet) throws IOException;

    /**
     * Attempt to fetch the generated {@link DataReference} for a given upload id. If the reference is not available yet, null is returned.
     * 
     * TODO add {@link IOException} to signal a permanent conversion failure? - misc_ro
     * 
     * @param user user data for authorization
     * @param id the assigned upload id
     * @return the generated {@link DataReference}, or null if it is not available yet
     * @throws IOException on asynchronous I/O errors on the receiver's side
     */
    DataReference pollUploadForDataReference(User user, String id) throws IOException;
}
