/*
 * Copyright 2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides central access to the active {@link SystemIntegrationAdapter}. This is intended for live operation, and therefore holds a
 * singleton-style instance of the adapter.
 * <p>
 * As a side effect, it is not possible to "reset" or replace the adapter for now when testing the system monitoring services. If this is
 * ever needed, appropriate setters should be added to the service implementations to allow injecting adapter instances there, instead of
 * changing this entry point approach, as the latter may lead to confusing behavior if the old adapter is still "cached" in any services.
 *
 * @author Robert Mischke
 */
public final class SystemIntegrationEntryPoint {

    private static final SystemIntegrationEntryPoint INSTANCE = new SystemIntegrationEntryPoint();

    private final SystemIntegrationAdapter adapter;

    private SystemIntegrationEntryPoint() {
        final Log log = LogFactory.getLog(getClass());
        log.debug("Initialized the OSHI library for system monitoring");
        adapter = new OSHISystemIntegrationAdapter();
    }

    public static SystemIntegrationAdapter getAdapter() {
        return INSTANCE.adapter;
    }
}
