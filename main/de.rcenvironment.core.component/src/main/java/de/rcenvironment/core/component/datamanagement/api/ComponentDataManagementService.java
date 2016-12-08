/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.datamanagement.api;

import java.io.File;
import java.io.IOException;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;

/**
 * TODO see {@link StatefulComponentDataManagementService} for javadoc until the API is settled.
 * 
 * @author Robert Mischke
 * @author Sascha Zur
 * @author Doreen Seider
 */
public interface ComponentDataManagementService {

    /**
     * A constant used in several methods as a "magic" filename parameter to denote that an automatically derived filename should be used.
     * See method javadocs for details.
     */
    String SAME_FILENAME = "*ATTACH_AUTOMATIC_FILENAME*";

    /**
     * TODO see {@link StatefulComponentDataManagementService} for javadoc until the API is settled.
     * 
     * @param componentContext {@link ComponentContext} of the calling component
     * @param file the local file
     * @param filename either a custom filename to attach to the reference, or the constant
     *        {@link ComponentDataManagementService#SAME_FILENAME} to use the filename of the local file, or "null" to attach no filename
     * @return the created reference
     * @throws IOException on a local I/O or data management error
     */
    String createTaggedReferenceFromLocalFile(ComponentContext componentContext, File file, String filename)
        throws IOException;

    /**
     * 
     * @param stringValue given {@link String}
     * @param componentContext {@link ComponentContext} of the calling component
     * @return the created reference
     * @throws IOException on a local I/O or data management error
     */
    String createTaggedReferenceFromString(ComponentContext componentContext, String stringValue) throws IOException;

    /**
     * Copies the data "body" identified by a data management reference to a local file.
     * 
     * @param reference the reference
     * @param targetFile the local file to write to
     * @param nodeId The node to try to fetch data from
     * @throws IOException on a local I/O or data management error
     */
    void copyReferenceToLocalFile(String reference, File targetFile, ResolvableNodeId nodeId)
        throws IOException;

    /**
     * Retrieved the String "body" identified by a data management reference.
     * 
     * @param reference the reference
     * @param nodeId The node to try to fetch data from
     * @return the retrieved String
     * @throws IOException on a local I/O or data management error
     * @throws CommunicationException in case of communication error
     */
    String retrieveStringFromReference(String reference, ResolvableNodeId nodeId) throws IOException, CommunicationException;

    /**
     * Creates {@link FileReferenceTD} object from given file by creating a new data management reference.
     * 
     * @param componentContext {@link ComponentContext} of the calling component
     * @param file given file
     * @param filename name of file
     * @return {@link FileReferenceTD}
     * @throws IOException if given file doesn't exist or on data management error
     */
    FileReferenceTD createFileReferenceTDFromLocalFile(ComponentContext componentContext, File file, String filename)
        throws IOException;

    /**
     * Creates {@link DirectoryReferenceTD} object from given directory by creating a new data management reference.
     * 
     * @param componentContext {@link ComponentContext} of the calling component
     * @param dir given directory
     * @param dirname name of directory
     * @return {@link DirectoryReferenceTD}
     * @throws IOException if given directory doesn't exist, is no directory, or on data management error
     */
    DirectoryReferenceTD createDirectoryReferenceTDFromLocalDirectory(ComponentContext componentContext, File dir,
        String dirname) throws IOException;

    /**
     * Retrieves a file from the data management referred by the given {@link FileReferenceTD}.
     * 
     * @param componentContext {@link ComponentContext} of the calling component
     * @param fileReference {@link FileReferenceTD}
     * @param targetFile local target file
     * @throws IOException on a local I/O or data management error
     */
    void copyFileReferenceTDToLocalFile(ComponentContext componentContext, FileReferenceTD fileReference, File targetFile)
        throws IOException;

    /**
     * Retrieves a directory from the data management referred by the given {@link DirectoryReferenceTD}.
     * 
     * @param componentContext {@link ComponentContext} of the calling component
     * @param dirReference {@link DirectoryReferenceTD}
     * @param targetDir local target directory
     * @throws IOException on a local I/O or data management error
     */
    void copyDirectoryReferenceTDToLocalDirectory(ComponentContext componentContext, DirectoryReferenceTD dirReference, File targetDir)
        throws IOException;

    /**
     * Retrieves a directory from the data management refer
     * 
     * the given {@link DirectoryReferenceTD}.
     * 
     * @param dirReference {@link DirectoryReferenceTD}
     * @param targetDir local target directory
     * @param node source {@link ResolvableNodeId}
     * @throws IOException on a local I/O or data management error
     */
    void copyDirectoryReferenceTDToLocalDirectory(DirectoryReferenceTD dirReference, File targetDir, ResolvableNodeId node)
        throws IOException;

}
