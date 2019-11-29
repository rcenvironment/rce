/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.types.api;

import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * From the user perspective, this data type represents a directory of files. The technical
 * representation is one data management references.
 * 
 * @author Robert Mischke
 * @author Doreen Seider
 */
public interface DirectoryReferenceTD extends TypedDatum {
    
    /**
     * @return data management reference to the directory
     */
    String getDirectoryReference();

    /**
     * @return directory name
     */
    String getDirectoryName();
    
    /**
     * @return directory size
     */
    long getDirectorySizeInBytes();

    /**
     * Sets the directory size of the referenced directory.
     * 
     * @param filesize the directory size
     */
    void setDirectorySize(long filesize);

}
