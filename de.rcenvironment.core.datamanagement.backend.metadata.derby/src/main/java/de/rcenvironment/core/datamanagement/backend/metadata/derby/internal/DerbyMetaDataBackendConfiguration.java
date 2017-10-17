/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.backend.metadata.derby.internal;

/**
 * Provides the configuration of this bundle. 
 *
 * @author Juergen Klein
 * @author Tobias Menden
 */
public class DerbyMetaDataBackendConfiguration {

    private String databaseUrl = "";

    public void setDatabaseUrl(String value) {
        this.databaseUrl = value;
    }

    public String getDatabaseURL() {
        return databaseUrl;
    }
}
