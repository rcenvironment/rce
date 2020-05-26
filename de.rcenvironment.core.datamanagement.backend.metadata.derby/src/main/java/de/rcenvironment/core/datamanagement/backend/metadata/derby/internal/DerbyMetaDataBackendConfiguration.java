/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
