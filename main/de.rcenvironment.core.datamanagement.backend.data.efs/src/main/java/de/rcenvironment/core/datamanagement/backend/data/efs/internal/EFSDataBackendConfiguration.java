/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.backend.data.efs.internal;

/**
 * Provides the configuration of the bundle.
 * 
 * @author Juergen Klein
 * @author Tobias Menden
 * @author Sascha Zur
 */
public class EFSDataBackendConfiguration {

    private String dataServiceStorageRoot = "";

    private boolean useGZipFormat = true;

    public String getEfsStorage() {
        return dataServiceStorageRoot;
    }

    public void setEfsStorage(String value) {
        this.dataServiceStorageRoot = value;
    }

    public boolean getUseGZipFormat() {
        return useGZipFormat;
    }

    public void setUseGZipFormat(boolean useGZipFormat) {
        this.useGZipFormat = useGZipFormat;
    }

}
