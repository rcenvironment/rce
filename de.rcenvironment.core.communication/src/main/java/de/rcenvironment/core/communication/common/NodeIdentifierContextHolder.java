/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.api.NodeIdentifierService;

/**
 * A static holder used to provide a required service instance to (potentially hidden/transitive) deserialiation calls of
 * {@link CommonIdBase} subclasses.
 * 
 * @author Robert Mischke
 */
public final class NodeIdentifierContextHolder {

    private static final String SERVICE_CLASS_NAME = NodeIdentifierService.class.getSimpleName();

    private static final transient ThreadLocal<NodeIdentifierService> sharedServiceThreadLocal = new ThreadLocal<>();

    private static volatile NodeIdentifierService defaultNodeIdentifierService;

    private static final Log sharedLog = LogFactory.getLog(NodeIdentifierContextHolder.class);

    public NodeIdentifierContextHolder() {}

    /**
     * Sets the service instance that should be used for deserializing id objects within the current {@link Thread}. Using this approach
     * instead of a singleton is important for proper unit/integration testing.
     * 
     * @param serviceInstance the service instance to set
     * @return the former service instance, if any; useful for restoring a previous context after an operation
     */
    public static NodeIdentifierService setDeserializationServiceForCurrentThread(NodeIdentifierService serviceInstance) {
        NodeIdentifierService previous = sharedServiceThreadLocal.get();
        sharedServiceThreadLocal.set(serviceInstance);
        return previous;
    }

    /**
     * Retrieves the service instance that should be used for deserializing id objects within the current {@link Thread}. Using this
     * approach instead of a singleton is important for proper unit/integration testing.
     * 
     * @return the service instance set for the current {@link Thread}
     */
    public static NodeIdentifierService getRawDeserializationServiceForCurrentThread() {
        return sharedServiceThreadLocal.get();
    }

    /**
     * Retrieves the service instance that should be used for deserializing id objects within the current {@link Thread}. Using this
     * approach instead of a singleton is important for proper unit/integration testing.
     * 
     * @return the service instance set for the current {@link Thread}
     */
    public static NodeIdentifierService getDeserializationServiceForCurrentThread() {
        final NodeIdentifierService result = sharedServiceThreadLocal.get();
        if (result == null) {
            if (defaultNodeIdentifierService != null) {
                // TODO >8.0 switch to ThreadContext once it is available in all threading contexts
                // sharedLog.debug("Using the global default " + SERVICE_CLASS_NAME + " instance; consider setting this explicitly through "
                // + NodeIdentifierContextHolder.class.getSimpleName());
                return defaultNodeIdentifierService;
            }
            // TODO stopgap solution until a more generic approach is implemented
            NodeIdentifierService testNodeIdentifierService = NodeIdentifierTestUtils.getTestNodeIdentifierService();
            if (testNodeIdentifierService != null) {
                sharedLog.warn("There is no " + SERVICE_CLASS_NAME
                    + " instance registered for the current thread; falling back to the default test instance");
                return testNodeIdentifierService;
            }
            throw new IllegalStateException(
                "There is no " + SERVICE_CLASS_NAME
                    + " instance registered for the current thread, which is required to deserialize distributed node identifiers, "
                    + "and there is no global default instance (typical in test contexts); use "
                    + NodeIdentifierContextHolder.class.getName() + " methods to fix this");
        }
        return result;
    }

    /**
     * OSGi-DS bind method; the "live" service instance is created and injected here by OSGi-DS. This service instance is used as a fallback
     * when the current thread has no explicit service set. This prevents live code from failing in rarely-used code parts.
     * 
     * In integration testing, no such default instance is available, which causes an exception to be thrown. This is intentional to locate
     * these places and add explicit thread context setting there.
     * 
     * @param newInstance the new service instance
     */
    public static synchronized void bindNodeIdentifierService(NodeIdentifierService newInstance) {
        if (defaultNodeIdentifierService != null) {
            // ensure only one static service is ever called for consistency
            throw new IllegalStateException("Tried to set a NodeIdentifierService instance, but it was already defined");
        }
        defaultNodeIdentifierService = newInstance;
        LogFactory.getLog(NodeIdentifierContextHolder.class).debug("Injected live " + newInstance.getClass() + " instance");
    }

    /**
     * Transitional method to prevent CheckStyle from complaining that this method should have a private constructor, which must however be
     * public to allow static OSGi-DS injection (sigh...). This will disappear once no more code uses this factory outside unit/integration
     * tests.
     */
    public void transitionalDummy() {}
}
