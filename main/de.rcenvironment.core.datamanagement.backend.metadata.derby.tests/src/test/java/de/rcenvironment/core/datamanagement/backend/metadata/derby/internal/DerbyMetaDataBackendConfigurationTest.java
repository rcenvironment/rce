/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
