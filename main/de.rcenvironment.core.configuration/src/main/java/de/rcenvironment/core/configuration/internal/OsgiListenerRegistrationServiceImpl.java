/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.internal;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.FrameworkUtil;

import de.rcenvironment.core.utils.incubator.ListenerDeclaration;
import de.rcenvironment.core.utils.incubator.ListenerProvider;
import de.rcenvironment.core.utils.incubator.ListenerRegistrationService;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryPublisherAccess;

/**
 * {@link ListenerRegistrationService} implementation that uses the global {@link ServiceRegistry} to register listeners.
 * 
 * TODO use an internal {@link OsgiServiceRegistryAccessFactory} instead of the global {@link ServiceRegistry}?
 * 
 * @author Robert Mischke
 */
public class OsgiListenerRegistrationServiceImpl implements ListenerRegistrationService {

    private final Map<ListenerProvider, ServiceRegistryPublisherAccess> sraMap = new HashMap<>();

    private final OsgiServiceRegistryAccessFactory serviceRegistryAccessFactory;

    private final Log log = LogFactory.getLog(getClass());

    public OsgiListenerRegistrationServiceImpl() {
        serviceRegistryAccessFactory = new OsgiServiceRegistryAccessFactory(FrameworkUtil.getBundle(this.getClass()).getBundleContext());
    }

    @Override
    public void registerListenerProvider(ListenerProvider listenerProvider) {
        synchronized (sraMap) {
            if (sraMap.get(listenerProvider) != null) {
                throw new IllegalStateException("Duplicate registration of ListenerProvider " + listenerProvider);
            }
            ServiceRegistryPublisherAccess sra = serviceRegistryAccessFactory.createPublisherAccessFor(listenerProvider);
            sraMap.put(listenerProvider, sra);
            for (ListenerDeclaration declaration : listenerProvider.defineListeners()) {
                final Class<?> clazz = declaration.getServiceClass();
                final Object implementation = declaration.getImplementation();
                if (!clazz.isInstance(implementation)) {
                    throw new IllegalStateException("Invalid ListenerDeclaration: implementation does not implement declared interface "
                        + clazz + ": " + implementation);
                }
                log.debug("Registering " + clazz.getSimpleName() + " listener on behalf of " + listenerProvider);
                sra.registerService(clazz, implementation);
            }
        }
    }

    @Override
    public void unregisterListenerProvider(ListenerProvider listenerProvider) {
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
