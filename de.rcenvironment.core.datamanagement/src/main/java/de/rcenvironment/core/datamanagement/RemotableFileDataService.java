/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement;

import java.io.IOException;
import java.io.InputStream;

import de.rcenvironment.core.datamanagement.commons.DataReference;
import de.rcenvironment.core.datamanagement.commons.MetaDataSet;
import de.rcenvironment.core.utils.common.rpc.RemotableService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Interface for the RCE user data system for file support.
 * 
 * @author Sandra Schroedter
 * @author Juergen Klein
 * @author Robert Mischke (chunked upload)
 * @author Brigitte Boden
 */
@RemotableService
public interface RemotableFileDataService {

    /**
     * Returns the InputStream of the given revision of dataReference.
     * 
     * @param dataReference DataReference which contains the needed revision.
     * @param calledFromRemote <code>true</code> if this method is called from remote node, otherwise <code>false</code>
     * @return InputStream of the given revision
     * @throws RemoteOperationException standard remote operation exception
     */
    InputStream getStreamFromDataReference(DataReference dataReference, Boolean calledFromRemote)
        throws RemoteOperationException;

    /**
     * Returns the InputStream of the given revision of dataReference.
     * 
     * @param dataReference DataReference which contains the needed revision.
     * @param calledFromRemote <code>true</code> if this method is called from remote node, otherwise <code>false</code>
     * @param decompress if set to true (default), the decompressed version of the object will be used, if it is stored in compressed form.
     * @return InputStream of the given revision
     * @throws RemoteOperationException standard remote operation exception
     */
    InputStream getStreamFromDataReference(DataReference dataReference, Boolean calledFromRemote, Boolean decompress)
        throws RemoteOperationException;

    /**
     * Creates a new DataReference from the given inputStream on the Platform targetDataManagement. The new dataReference will contain the
     * given MetaData and reserved MetaData, that the DataInterface adds automatically.
     * 
     * @param inputStream InputStream that shall be saved.
     * @param metaDataSet MetaDataSet that shall be saved.
     * @return DataReference for the given InputStream and MetaData.
     * @throws RemoteOperationException standard remote operation exception
     */
    DataReference newReferenceFromStream(InputStream inputStream, MetaDataSet metaDataSet)
        throws RemoteOperationException;

    /**
     * Creates a new DataReference from the given inputStream on the Platform targetDataManagement. The new dataReference will contain the
     * given MetaData and reserved MetaData, that the DataInterface adds automatically.
     * 
     * @param inputStream InputStream that shall be saved.
     * @param metaDataSet MetaDataSet that shall be saved.
     * @param alreadyCompressed if the file is already compressed (if set, no compression will be applied)
     * @return DataReference for the given InputStream and MetaData.
     * @throws RemoteOperationException standard remote operation exception
     */
    DataReference newReferenceFromStream(InputStream inputStream, MetaDataSet metaDataSet, Boolean alreadyCompressed)
        throws RemoteOperationException;

    /**
     * Initializes an id/handle that subsequent {@link #appendToUpload(String, byte[])} calls can use to upload data. The normal use case is
     * that this method is called via RPC from the uploader's node.
     * 
     * @return the generated upload id
     * @throws IOException if the upload could not be set up (for example, no disk space left at receiver)
     * @throws RemoteOperationException standard remote operation exception
     */
    String initializeUpload() throws IOException, RemoteOperationException;

    /**
     * Appends a chunk of data to a virtual file identified by an upload id.The normal use case is that this method is called once or more
     * via RPC from the uploader's node.
     * 
     * @param id the assigned upload id
     * @param data the byte array to append
     * @return the total number of bytes written so far
     * 
     * @throws IOException on I/O errors on the receiver's side
     * @throws RemoteOperationException standard remote operation exception
     */
    long appendToUpload(String id, byte[] data) throws IOException, RemoteOperationException;

    /**
     * Signals that all data has been written via {@link #appendToUpload(String, byte[])} and initiates the asynchronous conversion into a
     * {@link DataReference} with the given {@link MetaDataSet} attached. This conversion is performed asynchronously to avoid RPC timeouts
     * on large uploads. Use {@link #pollUploadForDataReference(String)} to fetch the generated {@link DataReference}.
     * 
     * @param id the assigned upload id
     * @param metaDataSet the data management metadata to append to the {@link DataReference}; the behaviour is the same as
     *        {@link #newReferenceFromStream(InputStream, MetaDataSet)}
     * @throws IOException on I/O errors on the receiver's side
     * @throws RemoteOperationException standard remote operation exception
     */
    void finishUpload(String id, MetaDataSet metaDataSet) throws IOException, RemoteOperationException;

    /**
     * Signals that all data has been written via {@link #appendToUpload(String, byte[])} and initiates the asynchronous conversion into a
     * {@link DataReference} with the given {@link MetaDataSet} attached. This conversion is performed asynchronously to avoid RPC timeouts
     * on large uploads. Use {@link #pollUploadForDataReference(String)} to fetch the generated {@link DataReference}.
     * 
     * @param id the assigned upload id
     * @param metaDataSet the data management metadata to append to the {@link DataReference}; the behaviour is the same as
     *        {@link #newReferenceFromStream(InputStream, MetaDataSet)}
     * @param alreadyCompressed if the file is already compressed (if set, no compression will be applied)
     * @throws IOException on I/O errors on the receiver's side
     * @throws RemoteOperationException standard remote operation exception
     */
    void finishUpload(String id, MetaDataSet metaDataSet, Boolean alreadyCompressed)
        throws IOException, RemoteOperationException;

    /**
     * Attempt to fetch the generated {@link DataReference} for a given upload id. If the reference is not available yet, null is returned.
     * 
     * TODO add {@link IOException} to signal a permanent conversion failure? - misc_ro
     * 
     * @param id the assigned upload id
     * @return the generated {@link DataReference}, or null if it is not available yet
     * @throws IOException on asynchronous I/O errors on the receiver's side
     * @throws RemoteOperationException standard remote operation exception
     */
    DataReference pollUploadForDataReference(String id) throws IOException, RemoteOperationException;

    /**
     * Perform an upload as a single synchronous operation. Is called via RPC from the uploader's node. Suitable for very small pieces of
     * data.
     * 
     * @param data the byte array to append
     * @param metaDataSet the data management metadata to append to the {@link DataReference};
     * @return the generated {@link DataReference}
     * @throws IOException IOException on I/O errors on the receiver's side
     * @throws RemoteOperationException standard remote operation exception
     * @throws RemoteOperationException standard remote operation exception
     */
    DataReference uploadInSingleStep(byte[] data, MetaDataSet metaDataSet) throws IOException, RemoteOperationException;

    /**
     * Perform an upload as a single synchronous operation. Is called via RPC from the uploader's node. Suitable for very small pieces of
     * data.
     * 
     * @param data the byte array to append
     * @param metaDataSet the data management metadata to append to the {@link DataReference};
     * @param alreadyCompressed if the file is already compressed (if set, no compression will be applied)
     * @return the generated {@link DataReference}
     * @throws IOException IOException on I/O errors on the receiver's side
     * @throws RemoteOperationException standard remote operation exception
     * @throws RemoteOperationException standard remote operation exception
     */
    DataReference uploadInSingleStep(byte[] data, MetaDataSet metaDataSet, Boolean alreadyCompressed) throws IOException,
        RemoteOperationException;

    /**
     * Deletes a whole local {@link DataReference} with all {@link Revision}s.
     * 
     * @param binaryReferenceKey Key of the binary reference that shall be deleted.
     * @throws RemoteOperationException standard remote operation exception
     */
    void deleteReference(String binaryReferenceKey) throws RemoteOperationException;
}
