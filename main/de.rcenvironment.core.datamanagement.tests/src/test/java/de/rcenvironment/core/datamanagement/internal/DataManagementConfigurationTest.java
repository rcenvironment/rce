/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
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

    private String metaDataBackend = "de.rcenvironment.core.datamanagement.backend.metadata.derby";

    private String fileDataBackend = "de.rcenvironment.core.datamanagement.backend.data.efs";

    private DataManagementConfiguration dmConfig = new DataManagementConfiguration();

    /**
     * Test.
     */
    @Test
    public void test() {

        TempFileServiceAccess.setupUnitTestEnvironment();
        assertEquals(metaDataBackend, dmConfig.getMetaDataBackend());
        assertEquals(fileDataBackend, dmConfig.getFileDataBackend());

        dmConfig.setMetaDataBackend(fileDataBackend);
        assertEquals(fileDataBackend, dmConfig.getMetaDataBackend());
        dmConfig.setFileDataBackend(metaDataBackend);
        assertEquals(metaDataBackend, dmConfig.getFileDataBackend());
    }
}
