/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.internal;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.rcenvironment.core.component.api.UserComponentIdMappingService;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;

/**
 * {@link UserComponentIdMappingServiceImpl} test.
 *
 * @author Robert Mischke
 */
@RunWith(Parameterized.class)
public class UserComponentIdMappingServiceImplTest {

    private final String expectedInternal;

    private final String expectedExternal;

    private final UserComponentIdMappingService service;

    public UserComponentIdMappingServiceImplTest(String expectedInternal, String expectedExternal) {
        this.expectedInternal = expectedInternal;
        this.expectedExternal = expectedExternal;

        service = new UserComponentIdMappingServiceImpl();
        service.registerBuiltinComponentMapping("de.rcenvironment.cpacswriter", "CPACS Writer");
        service.registerBuiltinComponentMapping("de.rcenvironment.doe.v2", "Design of Experiments");
    }

    /**
     * @return the parameters to run each test with; returning all archive formats to test (currently .zip and .tgz)
     */
    @Parameters(name = " {0} <-> {1} ")
    public static List<Object[]> parameters() {
        return Arrays.asList(new Object[][] {
            { "de.rcenvironment.integration.common.SimpleExample", "common/SimpleExample" },
            { "de.rcenvironment.integration.cpacs.SimpleExample", "cpacs/SimpleExample" },
            { "de.rcenvironment.integration.cpacs.tool With various_ allowed _ 2chars2", "cpacs/tool With various_ allowed _ 2chars2" },
            { "de.rcenvironment.cpacswriter", "rce/CPACS Writer" }, // example of registered name mapping for a built-in component
            { "de.rcenvironment.doe.v2", "rce/Design of Experiments" }, // special case of a built-in component
            { "de.rcenvironment.integration.workflow.SimpleExample", "workflow/SimpleExample" },
            { "de.rcenvironment.integration.workflow.Simple Example With Space", "workflow/Simple Example With Space" }
        });
    }

    /**
     * Default fromExternalToInternalId() mapping behavior.
     * 
     * @throws OperationFailureException on mapping failure
     */
    @Test
    public void externalToInternal() throws OperationFailureException {
        assertEquals(expectedInternal, service.fromExternalToInternalId(expectedExternal));
    }

    /**
     * Default fromInternalToExternalId() mapping behavior.
     * 
     * @throws OperationFailureException on mapping failure
     */
    @Test
    public void internalToExternal() throws OperationFailureException {
        assertEquals(expectedExternal, service.fromInternalToExternalId(expectedInternal));
    }

}
