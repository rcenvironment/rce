/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.datamanagement.stateful;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.datamanagement.commons.MetaDataKeys;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;

/**
 * A stateful version of {@link ComponentDataManagementService}. Implementations will usually store a relevant
 * {@link ComponentInstanceInformation} internally.
 * 
 * This interface allows clients to use implementations like {@link SimpleComponentDataManagementService} and also pass them as parameters,
 * while still allowing replacement with mock objects for test runs or integration tests.
 * 
 * @author Robert Mischke
 */
public interface StatefulComponentDataManagementService {

    /**
     * Creates a reference from a local file and automatically sets component-related metadata.
     * 
     * The following {@link MetaDataKeys} are automatically filled in:
     * <ul>
     * <li>COMPONENT_CONTEXT_UUID</li>
     * <li>COMPONENT_CONTEXT_NAME</li>
     * <li>COMPONENT_UUID</li>
     * <li>COMPONENT_NAME</li>
     * <li>FILENAME (if the "filename" parameter is not null; see below)</li>
     * </ul>
     * 
     * TODO add parameter for automatic/custom/empty associated filename?
     * 
     * TODO add revision parameter?
     * 
     * TODO add parameter to add custom metadata?
     * 
     * @param file the local file
     * @param filename either a custom filename to attach to the reference, or the constant
     *        {@link ComponentDataManagementService#SAME_FILENAME} to use the filename of the local file, or "null" to attach no filename
     * @return the created reference
     * @throws IOException on a local I/O or data management error
     * 
     */
    String createTaggedReferenceFromLocalFile(File file, String filename) throws IOException;

    /**
     * 
     * @param object the object
     * @return the created reference
     * @throws IOException on a local I/O or data management error
     */
    String createTaggedReferenceFromString(String object) throws IOException;

    /**
     * Copies the data "body" identified by a data management reference to a local file.
     * 
     * @param reference the reference
     * @param targetFile the local file to write to
     * @param platforms The platforms to try to fetch data from
     * @throws IOException on a local I/O or data management error
     */
    void copyReferenceToLocalFile(String reference, File targetFile, Collection<NetworkDestination> platforms) throws IOException;

    /**
     * Copies the data "body" identified by a data management reference to a local file.
     * 
     * @param reference the reference
     * @param targetFile the local file to write to
     * @param platform the node to fetch the data from
     * @throws IOException on a local I/O or data management error
     */
    void copyReferenceToLocalFile(String reference, File targetFile, NetworkDestination platform) throws IOException;

    /**
     * Retrieved the String "body" identified by a data management reference.
     * 
     * @param reference the reference
     * @param nodeId The node to try to fetch data from
     * @return the retrieved String
     * @throws IOException on a local I/O or data management error
     */
    String retrieveStringFromReference(String reference, InstanceNodeSessionId nodeId) throws IOException;

    /**
     * Creates a "history" point in the data management with appropriate metadata entries.
     * 
     * @param historyData the {@link Serializable} object that represents the history entry; is decoded by an appropriate subtree builder
     *        (see de.rcenvironment.rce.gui.datamanagement.browser.spi package for details)
     * @param userInfoText a user description for this history entry; used as title for GUI entries representing this history entry
     * @throws IOException on a data management error
     */
    void addHistoryDataPoint(Serializable historyData, String userInfoText) throws IOException;

    /**
     * Creates {@link FileReferenceTD} object from given file by creating a new data management reference.
     * 
     * @param file given file
     * @param filename name of file
     * @return {@link FileReferenceTD}
     * @throws IOException if given file doesn't exist or on data management error
     */
    FileReferenceTD createFileReferenceTDFromLocalFile(File file, String filename) throws IOException;

    /**
     * Creates {@link DirectoryReferenceTD} object from given directory by creating a new data management reference.
     * 
     * @param dir given directory
     * @param dirname name of directory
     * @return {@link DirectoryReferenceTD}
     * @throws IOException if given directory doesn't exist, is no directory, or on data management error
     */
    DirectoryReferenceTD createDirectoryReferenceTDFromLocalDirectory(File dir, String dirname) throws IOException;

    /**
     * Retrieves a file from the data management referred by the given {@link FileReferenceTD}.
     * 
     * @param fileReference {@link FileReferenceTD}
     * @param targetFile local target file
     * @param node source {@link InstanceNodeSessionId}
     * @throws IOException on a local I/O or data management error
     */
    void copyFileReferenceTDToLocalFile(FileReferenceTD fileReference, File targetFile, InstanceNodeSessionId node) throws IOException;

    /**
     * Retrieves a directory from the data management referred by the given {@link DirectoryReferenceTD}.
     * 
     * @param dirReference {@link DirectoryReferenceTD}
     * @param targetDir local target directory
     * @param node source {@link InstanceNodeSessionId}
     * @throws IOException on a local I/O or data management error
     */
    void copyDirectoryReferenceTDToLocalDirectory(DirectoryReferenceTD dirReference, File targetDir, InstanceNodeSessionId node)
        throws IOException;

}
