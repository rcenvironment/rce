/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.backend.metadata.derby.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Test cases for {@link DerbyMetaDataBackendConfiguration}.
 * 
 * @author Juergen Klein
 * @author Tobias Menden
 */
public class DerbyMetaDataBackendConfigurationTest {

    private DerbyMetaDataBackendConfiguration catalogConfig;

    private String databaseURL = "";

    /** Test. */
    @Test
    public void test() {
        catalogConfig = new DerbyMetaDataBackendConfiguration();
        assertTrue(catalogConfig.getDatabaseURL().isEmpty());
        catalogConfig.setDatabaseUrl(databaseURL);
        assertEquals(databaseURL, catalogConfig.getDatabaseURL());
    }
}
