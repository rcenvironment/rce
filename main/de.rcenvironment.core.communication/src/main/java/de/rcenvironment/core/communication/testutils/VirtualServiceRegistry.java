/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.testutils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.utils.common.AutoCreationMap;
import de.rcenvironment.core.utils.incubator.DebugSettings;

/**
 * An helper class to set up, bind and activate OSGi-DS service instances for integration testing. Note that only dependency binding and
 * service activation is supported; unbinding and deactivation is not.
 * 
 * @author Robert Mischke
 */
public class VirtualServiceRegistry {

    private List<VirtualService> unboundServices = new ArrayList<VirtualServiceRegistry.VirtualService>();

    private AutoCreationMap<Class<?>, List<VirtualService>> activatedServices =
        new AutoCreationMap<Class<?>, List<VirtualService>>() {

            @Override
            protected List<VirtualService> createNewEntry(Class<?> key) {
                return new ArrayList<VirtualService>();
            }
        };

    private final boolean verboseLogging = DebugSettings.getVerboseLoggingEnabled(getClass());

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Encapsulation of a registered service instance.
     * 
     * @author Robert Mischke
     */
    private final class VirtualService {

        private final Object implementation;

        private final boolean expectActivator;

        private final Class<?>[] serviceClasses;

        private final Map<Class<?>, Method> bindMethods = new HashMap<Class<?>, Method>();

        VirtualService(Object implementation, boolean expectActivator, Class<?>[] serviceClasses) {
            this.implementation = implementation;
            this.expectActivator = expectActivator;
            this.serviceClasses = serviceClasses;
            final Method[] methods = implementation.getClass().getMethods();
            for (Method method : methods) {
                if (method.getName().startsWith("bind") && method.getParameterTypes().length == 1) {
                    Class<?> type = method.getParameterTypes()[0];
                    if (verboseLogging) {
                        log.debug("Found bind method: " + implementation.getClass().getName() + "." + method.getName() + "() -> "
                            + type.getName());
                    }
                    bindMethods.put(type, method);
                }
            }
        }

        public Class<?>[] getServiceClasses() {
            return serviceClasses;
        }

        public Object getImplementation() {
            return implementation;
        }

        public Map<Class<?>, Method> getBindMethods() {
            return bindMethods;
        }

    }

    /**
     * Registers an already-initialized (activated) service instance. Used to provide external service instances to managed instances.
     * 
     * @param implementation the service implementation
     * @param serviceClasses the service interfaces to register this implementation for
     */
    public void registerProvidedService(Object implementation, Class<?>... serviceClasses) {
        final VirtualService service = new VirtualService(implementation, false, serviceClasses);
        for (Class<?> clazz : serviceClasses) {
            activatedServices.get(clazz).add(service);
        }
    }

    /**
     * Registers an unbound service instance. This is the main method to register services by.
     * 
     * The given service instance is expected to have an activator method; if it does not, use the method variant with a boolean parameter
     * to prevent a runtime exception.
     * 
     * This is a convenience shortcut for calling {@link #registerManagedService(Object, true, Class...)}.
     * 
     * @param implementation the service implementation
     * @param serviceClasses the service interfaces to register this implementation for
     */
    public void registerManagedService(Object implementation, Class<?>... serviceClasses) {
        registerManagedService(implementation, true, serviceClasses);
    }

    /**
     * Registers an unbound service instance. This is the main method to register services by.
     * 
     * @param implementation the service implementation
     * @param expectActivator true if this service is expected to have an activator; if this expectation is not met either way, an runtime
     *        exception is thron. logged
     * @param serviceClasses the service interfaces to register this implementation for
     */
    public void registerManagedService(Object implementation, boolean expectActivator, Class<?>... serviceClasses) {
        final VirtualService service = new VirtualService(implementation, expectActivator, serviceClasses);
        unboundServices.add(service);
    }

    /**
     * Returns a service that was registered for the given interface class, if it exists.
     * 
     * @param <T> the service interface class (for generics)
     * @param clazz the service interface class
     * @return a service instance that was registered for the given interface, or null if none exists
     */
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> clazz) {
        final List<VirtualService> list = activatedServices.get(clazz);
        if (list.isEmpty()) {
            return null;
        }
        // TODO check for uniqueness?
        return (T) list.get(0).getImplementation();
    }

    /**
     * Returns all services that were registered for the given interface class.
     * 
     * TODO apply generics on return value?
     * 
     * @param clazz the service interface class
     * @return all service instances that were registered for the given interface; may be empty
     */
    public Collection<?> getServices(Class<?> clazz) {
        return new ArrayList<Object>();
    }

    /**
     * Attempts to bind dependencies of managed services with already-activated or externally provided services, and then activate them,
     * until no more change is possible.
     * 
     * TODO add "all services activated" return value?
     */
    public void bindAndActivateServices() {
        VirtualService activatedService;
        do {
            activatedService = null;
            for (VirtualService service : unboundServices) {
                boolean satisfied = checkDependencies(service);
                if (satisfied) {
                    bindDependencies(service);
                    activate(service);
                    activatedService = service;
                    break;
                }
            }
            if (activatedService != null) {
                unboundServices.remove(activatedService);
                for (Class<?> serviceClass : activatedService.getServiceClasses()) {
                    activatedServices.get(serviceClass).add(activatedService);
                }
            }

        } while (activatedService != null);
        if (!unboundServices.isEmpty()) {
            log.warn("After bindAndActivateServices(), there are still unbound services left:");
            for (VirtualService service : unboundServices) {
                log.warn("  " + service.getImplementation().getClass());
                for (Class<?> depClass : service.getBindMethods().keySet()) {
                    String warningSuffix = "";
                    if (getService(depClass) == null) {
                        warningSuffix = " [unsatisfied]";
                    }
                    log.warn("    -> depends on " + depClass.getName() + warningSuffix);
                }
                for (Class<?> serviceClass : service.getServiceClasses()) {
                    log.warn("    <- provides " + serviceClass.getName());
                }
            }
            throw new RuntimeException("Failed to bind and activate all services; see log output for details");
        }
    }

    private boolean checkDependencies(VirtualService service) {
        boolean satisfied = true;
        for (Map.Entry<Class<?>, Method> bindMethod : service.getBindMethods().entrySet()) {
            final Class<?> type = bindMethod.getKey();
            if (getService(type) == null) {
                satisfied = false;
                // log.debug("Service " + service.getImplementation().getClass().getName()
                // + " has unsatisfied dependency, waiting for " + type.getName());
                break;
            }
        }
        return satisfied;
    }

    private void bindDependencies(VirtualService service) {
        if (verboseLogging) {
            log.debug("Dependencies of service " + service.getImplementation().getClass().getName() + " are satisfied, starting to bind");
        }
        for (Map.Entry<Class<?>, Method> bindMethod : service.getBindMethods().entrySet()) {
            final Class<?> type = bindMethod.getKey();
            final Object dependency = getService(type);
            if (verboseLogging) {
                log.debug("Binding dependency of service " + service.getImplementation().getClass().getName()
                    + " with instance of type " + dependency.getClass().getName());
            }
            Exception error = null;
            try {
                bindMethod.getValue().invoke(service.getImplementation(), dependency);
            } catch (IllegalArgumentException e) {
                error = e;
            } catch (IllegalAccessException e) {
                error = e;
            } catch (InvocationTargetException e) {
                error = e;
            }
            if (error != null) {
                throw new RuntimeException("Error calling bind method", error);
            }
        }
    }

    private void activate(VirtualService service) {
        Exception error = null;
        final Object implementation = service.getImplementation();
        Class<? extends Object> implementationClass = implementation.getClass();
        try {
            try {
                // TODO support activate(BundleContext) as well; requires definition of a virtual BundleContext first, though - misc_ro
                final Method noArgs = implementationClass.getMethod("activate", new Class<?>[0]);
                if (!service.expectActivator) {
                    throw new IllegalArgumentException("Activator found in " + implementationClass.getName()
                        + ", but it was specified to not have one");
                }
                if (verboseLogging) {
                    log.debug("Activating service " + implementationClass.getName());
                }
                noArgs.invoke(implementation);
            } catch (NoSuchMethodException e) {
                if (service.expectActivator) {
                    throw new IllegalArgumentException("No activator method found in " + implementationClass.getName()
                        + ", but it was specified to have one");
                }
                return;
            }
        } catch (IllegalArgumentException e) {
            error = e;
        } catch (SecurityException e) {
            error = e;
        } catch (IllegalAccessException e) {
            error = e;
        } catch (InvocationTargetException e) {
            error = e;
        }
        if (error != null) {
            throw new RuntimeException("Error activating service", error);
        }
    }
}
