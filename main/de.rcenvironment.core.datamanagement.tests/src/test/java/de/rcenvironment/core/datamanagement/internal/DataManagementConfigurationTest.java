/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.internal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Test cases for {@link DataManagementConfiguration}.
 * 
 * @author Doreen Seider
 */
public class DataManagementConfigurationTest {

    private String catalogBackend = "de.rcenvironment.core.datamanagement.backend.catalog.derby";

    private String fileDataBackend = "de.rcenvironment.core.datamanagement.backend.data.efs";

    private DataManagementConfiguration dmConfig = new DataManagementConfiguration();

    /**
     * Test.
     */
    @Test
    public void test() {

        TempFileServiceAccess.setupUnitTestEnvironment();
        assertEquals(catalogBackend, dmConfig.getMetaDataBackend());
        assertEquals(fileDataBackend, dmConfig.getFileDataBackend());

        dmConfig.setMetaDataBackend(fileDataBackend);
        assertEquals(fileDataBackend, dmConfig.getMetaDataBackend());
        dmConfig.setFileDataBackend(catalogBackend);
        assertEquals(catalogBackend, dmConfig.getFileDataBackend());
    }
}
