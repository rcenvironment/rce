/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.spi.DistributedComponentKnowledgeListener;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.service.AdditionalServiceDeclaration;
import de.rcenvironment.core.utils.common.service.AdditionalServicesProvider;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Implementation of {@link ComponentExecutionPermitsService}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (migrated service and listener setup)
 */
@Component
public class ComponentExecutionPermitsServiceImpl implements ComponentExecutionPermitsService,
    AdditionalServicesProvider {

    private DistributedComponentKnowledgeService componentKnowledgeService;

    // component id -> semaphore object
    private Map<String, ResizableSemaphore> semaphores = null;

    private synchronized void updateSemaphores(DistributedComponentKnowledge componentKnowledge) {
        if (semaphores == null) {
            semaphores = Collections.synchronizedMap(new HashMap<String, ResizableSemaphore>());
        }
        for (DistributedComponentEntry entry : componentKnowledge.getAllInstallations()) {
            ComponentInstallation compInstallation = entry.getComponentInstallation();
            if (compInstallation.getMaximumCountOfParallelInstances() != null) {
                if (!semaphores.containsKey(compInstallation.getInstallationId())) {
                    semaphores.put(compInstallation.getInstallationId(),
                        new ResizableSemaphore(compInstallation.getMaximumCountOfParallelInstances()));
                } else {
                    semaphores.get(compInstallation.getInstallationId())
                        .updateMaximumPermits(compInstallation.getMaximumCountOfParallelInstances());
                }
            }
        }
    }

    @Override
    public synchronized Future<Boolean> acquire(final String componentIdentifier, final String executionIdentifier) {
        if (semaphores == null) {
            updateSemaphores(componentKnowledgeService.getCurrentSnapshot());
        }
        final ResizableSemaphore semaphore = semaphores.get(componentIdentifier);
        return ConcurrencyUtils.getAsyncTaskService().submit(new Callable<Boolean>() {

            @TaskDescription("Acquire component execution permit")
            @Override
            public Boolean call() throws Exception {
                boolean aquired = false;
                if (semaphore != null) {
                    try {
                        semaphore.acquire();
                        aquired = true;
                    } catch (InterruptedException e) {
                        LogFactory.getLog(getClass())
                            .debug(StringUtils.format("Interupted while waiting for execution permit for component '%s' - %s",
                                componentIdentifier, executionIdentifier));
                    }
                }
                return aquired;
            }
        }, StringUtils.format("Waiting for execution permit for component '%s' - %s", componentIdentifier, executionIdentifier));
    }

    @Override
    public synchronized void release(final String componentIdentifier) {
        if (semaphores == null) {
            updateSemaphores(componentKnowledgeService.getCurrentSnapshot());
        }
        if (semaphores.containsKey(componentIdentifier)) {
            semaphores.get(componentIdentifier).release();
        }
    }

    @Override
    public Collection<AdditionalServiceDeclaration> defineAdditionalServices() {
        ArrayList<AdditionalServiceDeclaration> result = new ArrayList<>();
        result
            .add(new AdditionalServiceDeclaration(DistributedComponentKnowledgeListener.class, new DistributedComponentKnowledgeListener() {

                @Override
                public void onDistributedComponentKnowledgeChanged(DistributedComponentKnowledge newState) {
                    updateSemaphores(newState);
                }

            }));
        return result;
    }

    // test access method
    protected void simulateOnDistributedComponentKnowledgeChanged(DistributedComponentKnowledge componentKnowledge) {
        updateSemaphores(componentKnowledge);
    }

    /**
     * Resizable {@link Semaphore} that allows to increase and decrease maximum permits.
     * 
     * @author Doreen Seider
     */
    private class ResizableSemaphore {

        private static final int MINUS_ONE = -1;

        private final ReducableSemaphore semaphore;

        private int maxPermits;

        protected ResizableSemaphore(int maxPermits) {
            semaphore = new ReducableSemaphore(maxPermits);
            this.maxPermits = maxPermits;
        }

        protected void acquire() throws InterruptedException {
            semaphore.acquire();
        }

        protected synchronized void release() {
            if (semaphore.availablePermits() < maxPermits) {
                semaphore.release();
            }
        }

        protected void updateMaximumPermits(int newMaxPermits) {
            int diff = maxPermits - newMaxPermits;
            if (diff < 0) {
                semaphore.release(diff * MINUS_ONE);
            } else if (diff > 0) {
                semaphore.reducePermits(diff);
            }
            maxPermits = newMaxPermits;
        }
    }

    /**
     * {@link Semaphore} that allows to decrease permits.
     */
    private class ReducableSemaphore extends Semaphore {

        private static final long serialVersionUID = 5372099537410330875L;

        protected ReducableSemaphore(int maxPermits) {
            super(maxPermits, true);
        }

        @Override
        protected void reducePermits(int reduction) {
            super.reducePermits(reduction);
        }
    }

    @Reference
    protected void bindDistributedComponentKnowledgeService(DistributedComponentKnowledgeService service) {
        componentKnowledgeService = service;
    }

}
