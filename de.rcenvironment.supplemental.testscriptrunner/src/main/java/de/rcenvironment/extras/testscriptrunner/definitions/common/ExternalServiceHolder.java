/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.definitions.common;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.instancemanagement.InstanceManagementService;

/**
 * A simple stub that is either instantiated by the OSGi-DS framework and injected actual services, or instantiated by integration test
 * setups and injected test service mocks. Note that the implicit singleton nature may cause problems when running integration tests
 * concurrently with the same JVM if the test mocks are not equivalent.
 *
 * @author Robert Mischke
 */
@Component
public class ExternalServiceHolder {

    private static InstanceManagementService instanceManagementService;

    /**
     * OSGi bind method.
     * 
     * @param newService the new service instance
     */
    @Reference
    public void bindInstanceManagementService(InstanceManagementService newService) {
        instanceManagementService = newService;
    }

    public static InstanceManagementService getInstanceManagementService() {
        return instanceManagementService;
    }

}
