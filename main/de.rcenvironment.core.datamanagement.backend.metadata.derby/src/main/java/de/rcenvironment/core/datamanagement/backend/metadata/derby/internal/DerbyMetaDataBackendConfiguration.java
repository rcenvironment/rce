/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
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
