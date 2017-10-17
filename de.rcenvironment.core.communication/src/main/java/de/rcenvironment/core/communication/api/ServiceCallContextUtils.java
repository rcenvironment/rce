/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.api;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;
import de.rcenvironment.core.communication.rpc.internal.ServiceCallContextImpl;
import de.rcenvironment.toolkit.modules.concurrency.api.threadcontext.ThreadContext;
import de.rcenvironment.toolkit.modules.concurrency.api.threadcontext.ThreadContextBuilder;
import de.rcenvironment.toolkit.modules.concurrency.api.threadcontext.ThreadContextHolder;
import de.rcenvironment.toolkit.modules.concurrency.api.threadcontext.ThreadContextMemento;
import de.rcenvironment.toolkit.modules.concurrency.api.threadcontext.ThreadContextNameProvider;

/**
 * Allows externally available service methods (and methods transitively called by them) to access the context of the remote service call
 * that triggered their invocation. This is especially useful in cases where the {@link LogicalNodeId} or the {@link LogicalNodeSessionId}
 * under which a service was called is relevant for its behavior.
 * 
 * @author Robert Mischke
 */
public final class ServiceCallContextUtils {

    private static final ThreadContextNameProvider THREAD_CONTEXT_NAME_PROVIDER = new ThreadContextNameProvider() {

        @Override
        public String getName(ThreadContext context) {
            final ServiceCallContext scc = context.getAspect(ServiceCallContext.class);
            return scc.toString();
        }
    };

    private ServiceCallContextUtils() {}

    /**
     * @return The {@link ServiceCallContext} for the current {@link Thread}, or null if no context is available. Note that asynchronous
     *         tasks do not inherit the {@link ServiceCallContext} of their initiating {@link Thread} yet; this behavior will be added
     *         later.
     */
    public static ServiceCallContext getCurrentServiceCallContext() {
        return ThreadContextHolder.getCurrentContextAspect(ServiceCallContext.class);
    }

    /**
     * Creates and registers a {@link ServiceCallContext} for the current {@link Thread}.
     * 
     * @param caller the {@link LogicalNodeSessionId} used while invoking the local service method
     * @param target the {@link LogicalNodeSessionId} under which the service method was called
     * @param serviceName the name of the invoked service
     * @param methodName the name of the invoked method
     * @return a {@link ThreadContextMemento} to restore the previous {@link ThreadContext} with later (using
     *         {@link ThreadContextMemento#restore()})
     */
    public static ThreadContextMemento attachServiceCallDataToThreadContext(final LogicalNodeSessionId caller,
        final LogicalNodeSessionId target, String serviceName, String methodName) {
        // construct object
        final ServiceCallContext serviceCallContext = new ServiceCallContextImpl(caller, target, serviceName, methodName);
        // attach to current thread's context
        final ThreadContext newThreadContext = ThreadContextBuilder.fromCurrent()
            .setAspect(ServiceCallContext.class, serviceCallContext)
            .setNameProvider(THREAD_CONTEXT_NAME_PROVIDER)
            .build();
        return ThreadContextHolder.setCurrentContext(newThreadContext);
    }
}
