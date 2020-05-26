/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.backend.data.efs.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;


/**
 * Test cases for {@link EFSDataBackendConfiguration}.
 *
 * @author Doreen Seider
 */
public class EFSDataBackendConfigurationTest {

    /** Test. */
    @Test
    public void test() {
        EFSDataBackendConfiguration configuration = new EFSDataBackendConfiguration();
        assertNotNull(configuration.getEfsStorage());
        assertEquals("", configuration.getEfsStorage());
        String storage = "superStorage";
        configuration.setEfsStorage(storage);
        assertEquals(storage, configuration.getEfsStorage());
    }

}
