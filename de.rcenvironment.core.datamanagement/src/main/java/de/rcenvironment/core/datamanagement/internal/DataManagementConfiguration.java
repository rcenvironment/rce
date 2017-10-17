/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.internal;

/**
 * Configuration of this bundle.
 * 
 * @author Frank Kautz
 * @author Juergen Klein
 * @author Tobias Menden
 */
public class DataManagementConfiguration {

    private String metaDataBackend = "de.rcenvironment.core.datamanagement.backend.metadata.derby";

    private String fileDataBackend = "de.rcenvironment.core.datamanagement.backend.data.efs";

    public void setFileDataBackend(String value) {
        this.fileDataBackend = value;
    }

    public void setMetaDataBackend(String value) {
        this.metaDataBackend = value;
    }

    public String getMetaDataBackend() {
        return metaDataBackend;
    }

    public String getFileDataBackend() {
        return fileDataBackend;
    }

}
