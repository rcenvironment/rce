/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
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
