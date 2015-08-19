/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import de.rcenvironment.core.authentication.User;
import de.rcenvironment.core.authorization.AuthorizationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.datamanagement.commons.MetaDataSet;

/**
 * A data management API that provides more higher-level operations than the existing interfaces (like {@link FileDataService} or
 * {@link MetaDataService}). Another difference is that, by default, UUIDs are passed as Strings instead of java UUID objects. This relaxes
 * the assumptions about what is used as a data reference, which simplifies mocking scenarios (for example, by using path representations of
 * temporary files as data references).
 * 
 * @author Robert Mischke
 * @author Doreen Seider
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
     * @param user user representation
     * @param file the local file
     * @param additionalMetaData additional metadata key/value pairs to add to the automatically generated metadata; can be null if not
     *        required
     * @param nodeId the identifier of the platform to create the data management entry on; if null, the local platform is used
     * @return the unique String reference to the created data management entry; its internal format is implementation-dependent
     * @throws IOException on I/O errors in the data management, or related to the given file
     * @throws AuthorizationException if the user or the extension has no create permission (copied from {@link FileDataService})
     * @throws InterruptedException on thread interruption
     */
    String createReferenceFromLocalFile(User user, File file, MetaDataSet additionalMetaData, // CheckStyle
        NodeIdentifier nodeId) throws IOException, AuthorizationException, InterruptedException;

    /**
     * Creates a new data management entry with the utf-8 byte array form of the given String and returns a new and unique String id for it.
     * For this new data management entry, the common managed metadata values are set automatically; additional metadata can be provided in
     * an optional {@link MetaDataSet}. See [MetaDataKeys.Managed] for a list of the managed entries.
     * 
     * @param user user representation
     * @param object the object to serialize
     * @param additionalMetaData additional metadata key/value pairs to add to the automatically generated metadata; can be null if not
     *        required
     * @param nodeId the identifier of the platform to create the data management entry on; if null, the local platform is used
     * @return the unique String reference to the created data management entry; its internal format is implementation-dependent
     * @throws IOException on I/O errors in the data management, or related to the given file
     * @throws AuthorizationException if the user or the extension has no create permission (copied from {@link FileDataService})
     * @throws InterruptedException on thread interruption
     */
    String createReferenceFromString(User user, String object, MetaDataSet additionalMetaData, // CheckStyle
        NodeIdentifier nodeId) throws IOException, AuthorizationException, InterruptedException;

    /**
     * Creates a new data management entry with the contents of the given directory and returns a new and unique String id for it. For this
     * new data management entry, the common managed metadata values are set automatically; additional metadata can be provided in an
     * optional {@link MetaDataSet}. See [MetaDataKeys.Managed] for a list of the managed entries.
     * 
     * Note that the name of the local directory is not automatically added to the metadata of the new entry. If this is desired, create an
     * appropriate entry in the {@link MetaDataSet} passed to this method.
     * 
     * @param user user representation
     * @param dir the local directory
     * @param additionalMetaData additional metadata key/value pairs to add to the automatically generated metadata; can be null if not
     *        required
     * @param nodeId the identifier of the platform to create the data management entry on; if null, the local platform is used
     * @return the unique String reference to the created data management entry; its internal format is implementation-dependent
     * @throws IOException on I/O errors in the data management, or related to the given directory
     * @throws AuthorizationException if the user or the extension has no create permission (copied from {@link FileDataService})
     * @throws InterruptedException on thread interruption
     */
    String createReferenceFromLocalDirectory(User user, File dir, MetaDataSet additionalMetaData, // CheckStyle
        NodeIdentifier nodeId) throws IOException, AuthorizationException, InterruptedException;

    /**
     * Writes the data referenced by the given string id stored on a given platform to a local file.
     * 
     * @param user user representation
     * @param reference the String id referencing a data management entry, as created, for example, by
     *        {@link #createReferenceFromLocalFile(User, File, MetaDataSet, NodeIdentifier)} ; its internal format is
     *        implementation-dependent
     * @param targetFile the local file to copy the referenced data to
     * @param nodeId platform where the data is stored
     * @throws IOException on I/O errors in the data management, or related to the given file
     * @throws AuthorizationException if the user or the extension has no read permission (copied from {@link FileDataService})
     */
    void copyReferenceToLocalFile(User user, String reference, File targetFile, // CheckStyle
        NodeIdentifier nodeId) throws IOException, AuthorizationException;

    /**
     * Writes the data referenced by the given string id stored on a given platform to a local file.
     * 
     * @param user user representation
     * @param reference the String id referencing a data management entry, as created, for example, by
     *        {@link #createReferenceFromLocalFile(User, File, MetaDataSet, NodeIdentifier)} ; its internal format is
     *        implementation-dependent
     * @param targetFile the local file to copy the referenced data to
     * @param platforms platforms where the data is queried
     * @throws IOException on I/O errors in the data management, or related to the given file
     * @throws AuthorizationException if the user or the extension has no read permission (copied from {@link FileDataService})
     */
    void copyReferenceToLocalFile(User user, String reference, File targetFile, // CheckStyle
        Collection<NodeIdentifier> platforms) throws IOException, AuthorizationException;

    /**
     * Retrieves a directory from the data management referred by the given {@link DirectoryReferenceTD}.
     * 
     * @param user the user logged in
     * @param reference the String id referencing a data management entry, as created, for example, by
     *        {@link #createReferenceFromLocalFile(User, File, MetaDataSet, NodeIdentifier)} ; its internal format is
     *        implementation-dependent
     * @param targetDir local target directory
     * @param node source {@link NodeIdentifier}
     * @throws IOException on a local I/O or data management error
     */
    void copyReferenceToLocalDirectory(User user, String reference, File targetDir, NodeIdentifier node)
        throws IOException;

    /**
     * Retrieves the String referenced by the given string id.
     * 
     * @param user the user logged in
     * @param reference the String id referencing a data management entry, as created, for example, by
     *        {@link #createReferenceFromString(User, String, MetaDataSet, NodeIdentifier)}; its internal format is implementation-dependent
     * @param nodeId {@link NodeIdentifier} to try to fetch data from
     * @return the retrieved String
     * @throws IOException on I/O errors in the data management, or related to the given file
     * @throws AuthorizationException if the user or the extension has no read permission (copied from {@link FileDataService})
     */
    String retrieveStringFromReference(User user, String reference, NodeIdentifier nodeId) throws IOException, AuthorizationException;

}
