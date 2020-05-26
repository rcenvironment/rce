/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import de.rcenvironment.core.authorization.AuthorizationException;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.datamanagement.commons.MetaDataSet;

/**
 * A data management API that provides more higher-level operations than the existing interfaces (like {@link RemotableFileDataService} or
 * {@link RemotableMetaDataService}). Another difference is that, by default, UUIDs are passed as Strings instead of java UUID objects. This
 * relaxes the assumptions about what is used as a data reference, which simplifies mocking scenarios (for example, by using path
 * representations of temporary files as data references).
 * 
 * @author Robert Mischke
 * @author Doreen Seider
 * @author Brigitte Boden
 */
public interface DataManagementService {

    // TODO adapt Checkstyle to not complain if a javadoc @link is set to MetaDataKeys.Managed

    /**
     * Creates a new data management entry with the contents of the given file and returns a new and unique String id for it. For this new
     * data management entry, the common managed metadata values are set automatically; additional metadata can be provided in an optional
     * {@link MetaDataSet}. See [MetaDataKeys.Managed] for a list of the managed entries.
     * 
     * Note that the name of the local file is not automatically added to the metadata of the new entry. If this is desired, create an
     * appropriate entry in the {@link MetaDataSet} passed to this method.
     * 
     * @param file the local file
     * @param additionalMetaData additional metadata key/value pairs to add to the automatically generated metadata; can be null if not
     *        required
     * @param nodeId the identifier of the platform to create the data management entry on; if null, the local platform is used
     * @return the unique String reference to the created data management entry; its internal format is implementation-dependent
     * @throws IOException on I/O errors in the data management, or related to the given file
     * @throws AuthorizationException if the user or the extension has no create permission (copied from {@link RemotableFileDataService})
     * @throws InterruptedException on thread interruption
     * @throws CommunicationException in case of communication error
     */
    String createReferenceFromLocalFile(File file, MetaDataSet additionalMetaData, // CheckStyle
        NetworkDestination nodeId) throws IOException, AuthorizationException, InterruptedException, CommunicationException;

    /**
     * Creates a new data management entry with the contents of the given file and returns a new and unique String id for it. For this new
     * data management entry, the common managed metadata values are set automatically; additional metadata can be provided in an optional
     * {@link MetaDataSet}. See [MetaDataKeys.Managed] for a list of the managed entries.
     * 
     * Note that the name of the local file is not automatically added to the metadata of the new entry. If this is desired, create an
     * appropriate entry in the {@link MetaDataSet} passed to this method.
     * 
     * @param file the local file
     * @param additionalMetaData additional metadata key/value pairs to add to the automatically generated metadata; can be null if not
     *        required
     * @param nodeId the identifier of the platform to create the data management entry on; if null, the local platform is used
     * @param alreadyCompressed if the file is already compressed (if set, no compression will be applied)
     * @return the unique String reference to the created data management entry; its internal format is implementation-dependent
     * @throws IOException on I/O errors in the data management, or related to the given file
     * @throws AuthorizationException if the user or the extension has no create permission (copied from {@link RemotableFileDataService})
     * @throws InterruptedException on thread interruption
     * @throws CommunicationException in case of communication error
     */
    String createReferenceFromLocalFile(File file, MetaDataSet additionalMetaData, // CheckStyle
        NetworkDestination nodeId, boolean alreadyCompressed) throws IOException, AuthorizationException, InterruptedException,
        CommunicationException;

    /**
     * Creates a new data management entry with the utf-8 byte array form of the given String and returns a new and unique String id for it.
     * For this new data management entry, the common managed metadata values are set automatically; additional metadata can be provided in
     * an optional {@link MetaDataSet}. See [MetaDataKeys.Managed] for a list of the managed entries.
     * 
     * @param object the object to serialize
     * @param additionalMetaData additional metadata key/value pairs to add to the automatically generated metadata; can be null if not
     *        required
     * @param nodeId the identifier of the platform to create the data management entry on; if null, the local platform is used
     * @return the unique String reference to the created data management entry; its internal format is implementation-dependent
     * @throws IOException on I/O errors in the data management, or related to the given file
     * @throws AuthorizationException if the user or the extension has no create permission (copied from {@link RemotableFileDataService})
     * @throws InterruptedException on thread interruption
     * @throws CommunicationException in case of communication error
     */
    String createReferenceFromString(String object, MetaDataSet additionalMetaData, // CheckStyle
        NetworkDestination nodeId) throws IOException, AuthorizationException, InterruptedException, CommunicationException;

    /**
     * Creates a new data management entry with the contents of the given directory and returns a new and unique String id for it. For this
     * new data management entry, the common managed metadata values are set automatically; additional metadata can be provided in an
     * optional {@link MetaDataSet}. See [MetaDataKeys.Managed] for a list of the managed entries.
     * 
     * Note that the name of the local directory is not automatically added to the metadata of the new entry. If this is desired, create an
     * appropriate entry in the {@link MetaDataSet} passed to this method.
     * 
     * @param dir the local directory
     * @param additionalMetaData additional metadata key/value pairs to add to the automatically generated metadata; can be null if not
     *        required
     * @param nodeId the identifier of the platform to create the data management entry on; if null, the local platform is used
     * @return the unique String reference to the created data management entry; its internal format is implementation-dependent
     * @throws IOException on I/O errors in the data management, or related to the given directory
     * @throws AuthorizationException if the user or the extension has no create permission (copied from {@link RemotableFileDataService})
     * @throws InterruptedException on thread interruption
     * @throws CommunicationException in case of communication error
     */
    String createReferenceFromLocalDirectory(File dir, MetaDataSet additionalMetaData, // CheckStyle
        NetworkDestination nodeId) throws IOException, AuthorizationException, InterruptedException, CommunicationException;

    /**
     * Writes the data referenced by the given string id stored on a given platform to a local file.
     * 
     * @param reference the String id referencing a data management entry, as created, for example, by
     *        {@link #createReferenceFromLocalFile(File, MetaDataSet, NetworkDestination)} ; its internal format is implementation-dependent
     * @param targetFile the local file to copy the referenced data to
     * @param nodeId platform where the data is stored
     * @throws IOException on I/O errors in the data management, or related to the given file
     * @throws CommunicationException in case of communication error
     */
    void copyReferenceToLocalFile(String reference, File targetFile, // CheckStyle
        NetworkDestination nodeId) throws IOException, CommunicationException;

    /**
     * Writes the data referenced by the given string id stored on a given platform to a local file.
     * 
     * @param reference the String id referencing a data management entry, as created, for example, by
     *        {@link #createReferenceFromLocalFile(File, MetaDataSet, NetworkDestination)} ; its internal format is implementation-dependent
     * @param targetFile the local file to copy the referenced data to
     * @param nodeId platform where the data is stored
     * @param decompress if set to true (default), the decompressed version of the object will be used, if it is stored in compressed form.
     * @throws IOException on I/O errors in the data management, or related to the given file
     * @throws CommunicationException in case of communication error
     */
    void copyReferenceToLocalFile(String reference, File targetFile, // CheckStyle
        NetworkDestination nodeId, boolean decompress) throws IOException, CommunicationException;

    /**
     * Writes the data referenced by the given string id stored on a given platform to a local file.
     * 
     * @param reference the String id referencing a data management entry, as created, for example, by
     *        {@link #createReferenceFromLocalFile(File, MetaDataSet, NodeIdentifier)} ; its internal format is implementation-dependent
     * @param targetFile the local file to copy the referenced data to
     * @param platforms platforms where the data is queried
     * @throws IOException on I/O errors in the data management, or related to the given file
     * @throws CommunicationException in case of communication error
     */
    @Deprecated // undefined storage locations are not used anymore; remove from code base
    void copyReferenceToLocalFile(String reference, File targetFile, // CheckStyle
        Collection<? extends NetworkDestination> platforms) throws IOException, CommunicationException;

    /**
     * Retrieves a directory from the data management referred by the given {@link DirectoryReferenceTD}.
     * 
     * @param reference the String id referencing a data management entry, as created, for example, by
     *        {@link #createReferenceFromLocalFile(File, MetaDataSet, NetworkDestination)} ; its internal format is implementation-dependent
     * @param targetDir local target directory
     * @param node source {@link NetworkDestination}
     * @throws IOException on a local I/O or data management error
     * @throws CommunicationException in case of communication error
     */
    void copyReferenceToLocalDirectory(String reference, File targetDir, NetworkDestination node)
        throws IOException, CommunicationException;

    /**
     * Retrieves the String referenced by the given string id.
     * 
     * @param reference the String id referencing a data management entry, as created, for example, by
     *        {@link #createReferenceFromString(String, MetaDataSet, NetworkDestination)}; its internal format is implementation-dependent
     * @param nodeId {@link NetworkDestination} to try to fetch data from
     * @return the retrieved String
     * @throws IOException on I/O errors in the data management, or related to the given file
     * @throws CommunicationException in case of communication error
     */
    String retrieveStringFromReference(String reference, NetworkDestination nodeId) throws IOException,
        CommunicationException;

}
