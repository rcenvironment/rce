/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.types.api;

import java.util.Date;

import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * From the user perspective, this data type represents a file and provides access to its content,
 * an file name and possibly other file metadata. Technically, the file content will
 * typically be represented as a data management reference.
 * 
 * @author Robert Mischke
 * @author Doreen Seider
 */
public interface FileReferenceTD extends TypedDatum {

    /**
     * @return data management reference to the file
     */
    String getFileReference();

    /**
     * @return file name
     */
    String getFileName();
    
    /**
     * @return file size
     */
    long getFileSizeInBytes();
    
    /**
     * @return last modified
     */
    Date getLastModified();

    /**
     * Sets the file size of the referenced file.
     * 
     * @param filesize the file size
     */
    void setFileSize(long filesize);

    /**
     * Sets the last modified date of the referenced file.
     * 
     * @param lastModified Date of the last modification
     */
    void setLastModified(Date lastModified);
    
}
