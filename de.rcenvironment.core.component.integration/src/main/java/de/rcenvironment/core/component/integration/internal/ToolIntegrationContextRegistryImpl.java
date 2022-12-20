/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.integration.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import de.rcenvironment.core.component.integration.IntegrationContext;
import de.rcenvironment.core.component.integration.ToolIntegrationContextRegistry;
import de.rcenvironment.core.configuration.CommandLineArguments;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Implementation of {@link ToolIntegrationContextRegistry}.
 * 
 * @author Sascha Zur
 * @author Robert Mischke (rework/cleanup; moved actual processing into ToolIntegrationService)
 */
@Component
public class ToolIntegrationContextRegistryImpl implements ToolIntegrationContextRegistry {

    private static final long DISCOVERY_WAIT_TIME_MSEC = 1000;

    // Optional to allow "poison pill" pattern; null elements are not permitted
    private final BlockingDeque<Optional<IntegrationContext>> initializationQueue = new LinkedBlockingDeque<>();

    private final Map<String, IntegrationContext> contextsById = new HashMap<>();

    private final Map<String, IntegrationContext> contextsByType = new HashMap<>();

    private final AsyncTaskService asyncTaskService = ConcurrencyUtils.getAsyncTaskService();

    private boolean completionTimerStarted;

    private final Log log = LogFactory.getLog(getClass());

    @Activate
    protected void activate() {
        if (CommandLineArguments.isDoNotStartComponentsRequested()) {
            // prevent unnecessary wait if component loading is disabled; send termination signal immediately
            enqueueTerminationSignal();
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind = "removeToolIntegrationContext")
    protected synchronized void addToolIntegrationContext(IntegrationContext newContext) {
        final String contextId = newContext.getContextId();
        final String lowerCaseType = newContext.getContextTypeString().toLowerCase();
        if (contextsById.containsKey(contextId)) {
            return;
        }
        contextsById.put(contextId, newContext);
        contextsByType.put(lowerCaseType, newContext);

        log.debug("Registered integration context for id " + contextId + " and lowercase type " + lowerCaseType);
        initializationQueue.add(Optional.of(newContext));

        if (!completionTimerStarted) {
            asyncTaskService.scheduleAfterDelay(new Runnable() {

                @Override
                @TaskDescription("Signal end of ToolIntegrationContext discovery")
                public void run() {
                    log.debug("Sending initialization termination signal");
                    enqueueTerminationSignal();
                }
            }, DISCOVERY_WAIT_TIME_MSEC);
            completionTimerStarted = true;
        }
    }

    protected synchronized void removeToolIntegrationContext(IntegrationContext oldContext) {
        final String contextId = oldContext.getContextId();
        final String lowerCaseType = oldContext.getContextTypeString().toLowerCase();
        contextsById.remove(contextId);
        contextsByType.remove(lowerCaseType);
    }

    @Override
    public IntegrationContext fetchNextUninitializedToolIntegrationContext() {
        try {
            Optional<IntegrationContext> element = initializationQueue.take();
            if (element.isPresent()) {
                return element.get();
            } else {
                return null;
            }
        } catch (InterruptedException e) {
            log.debug("Interrupted while waiting for the next " + IntegrationContext.class.getSimpleName() + " to initialize");
            return null;
        }
    }

    @Override
    public synchronized Collection<IntegrationContext> getAllIntegrationContexts() {
        return contextsById.values();
    }

    @Override
    public synchronized IntegrationContext getToolIntegrationContextById(String contextId) {
        IntegrationContext result = contextsById.get(contextId);
        if (result == null) {
            log.warn("Returning integration context 'null' for requested id '" + contextId + "'");
        }
        return result;
    }

    @Override
    public synchronized IntegrationContext getToolIntegrationContextByType(String type) {
        IntegrationContext result = contextsByType.get(type.toLowerCase());
        if (result == null) {
            log.warn("Returning integration context 'null' for requested type '" + type + "'");
        }
        return result;
    }

    @Override
    public synchronized boolean hasTIContextMatchingPrefix(String toolId) {
        for (IntegrationContext context : contextsById.values()) {
            if (toolId.startsWith(context.getPrefixForComponentId())) {
                return true;
            }
        }
        return false;
    }

    private void enqueueTerminationSignal() {
        initializationQueue.add(Optional.empty()); // poison pill pattern
    }

}
