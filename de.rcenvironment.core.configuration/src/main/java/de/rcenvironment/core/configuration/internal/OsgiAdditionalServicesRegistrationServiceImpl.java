/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.internal;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.FrameworkUtil;

import de.rcenvironment.core.toolkitbridge.api.StaticToolkitHolder;
import de.rcenvironment.core.utils.common.service.AdditionalServiceDeclaration;
import de.rcenvironment.core.utils.common.service.AdditionalServicesProvider;
import de.rcenvironment.core.utils.common.service.AdditionalServicesRegistrationService;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryPublisherAccess;
import de.rcenvironment.toolkit.modules.statistics.api.CounterCategory;
import de.rcenvironment.toolkit.modules.statistics.api.StatisticsFilterLevel;
import de.rcenvironment.toolkit.modules.statistics.api.StatisticsTrackerService;

/**
 * {@link AdditionalServicesRegistrationService} implementation that uses the global {@link ServiceRegistry} to register the requested
 * additional services.
 * 
 * TODO review: use an internal {@link OsgiServiceRegistryAccessFactory} instead of the global {@link ServiceRegistry}?
 * 
 * @author Robert Mischke
 */
public class OsgiAdditionalServicesRegistrationServiceImpl implements AdditionalServicesRegistrationService {

    private final Map<AdditionalServicesProvider, ServiceRegistryPublisherAccess> sraMap = new HashMap<>();

    private final OsgiServiceRegistryAccessFactory serviceRegistryAccessFactory;

    private final Log log = LogFactory.getLog(getClass());

    private final CounterCategory counterCategory;

    public OsgiAdditionalServicesRegistrationServiceImpl() {
        serviceRegistryAccessFactory = new OsgiServiceRegistryAccessFactory(FrameworkUtil.getBundle(this.getClass()).getBundleContext());
        counterCategory =
            StaticToolkitHolder.getService(StatisticsTrackerService.class).getCounterCategory(
                "Additional Service registrations via AdditionalServicesProvider API", StatisticsFilterLevel.DEVELOPMENT);
    }

    @Override
    public void registerAdditionalServicesProvider(AdditionalServicesProvider additionalServicesProvider) {
        synchronized (sraMap) {
            if (sraMap.get(additionalServicesProvider) != null) {
                throw new IllegalStateException("Duplicate registration of ListenerProvider " + additionalServicesProvider);
            }
            ServiceRegistryPublisherAccess sra = serviceRegistryAccessFactory.createPublisherAccessFor(additionalServicesProvider);
            sraMap.put(additionalServicesProvider, sra);
            for (AdditionalServiceDeclaration declaration : additionalServicesProvider.defineAdditionalServices()) {
                final Class<?> clazz = declaration.getServiceClass();
                final Object implementation = declaration.getImplementation();
                if (!clazz.isInstance(implementation)) {
                    throw new IllegalStateException("Invalid ListenerDeclaration: implementation does not implement declared interface "
                        + clazz + ": " + implementation);
                }
                log.debug("Registering " + clazz.getSimpleName() + " listener on behalf of " + additionalServicesProvider);
                sra.registerService(clazz, implementation);
                counterCategory.count(clazz.getName());
            }
        }
    }

    @Override
    public void unregisterAdditionalServicesProvider(AdditionalServicesProvider listenerProvider) {
        synchronized (sraMap) {
            final ServiceRegistryPublisherAccess sra = sraMap.get(listenerProvider);
            if (sra == null) {
                throw new IllegalStateException("No registration found for ListenerProvider " + listenerProvider);
            }
            sraMap.remove(listenerProvider);
            log.debug("Disposing listeners of " + listenerProvider);
            sra.dispose();
        }
    }
}
