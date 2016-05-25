/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.spi.DistributedComponentKnowledgeListener;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;

/**
 * Implementation of {@link ComponentExecutionPermitsService}.
 * 
 * @author Doreen Seider
 */
public class ComponentExecutionPermitsServiceImpl implements ComponentExecutionPermitsService,
    DistributedComponentKnowledgeListener {
    
    private DistributedComponentKnowledgeService componentKnowledgeService;
    
    // component id -> semaphore object
    private Map<String, ResizableSemaphore> semaphores = null;

    @Override
    public void onDistributedComponentKnowledgeChanged(DistributedComponentKnowledge newState) {
        updateSemaphores(newState);
    }

    private synchronized void updateSemaphores(DistributedComponentKnowledge componentKnowledge) {
        if (semaphores == null) {
            semaphores = Collections.synchronizedMap(new HashMap<String, ResizableSemaphore>());
        }
        for (ComponentInstallation compInstallation : componentKnowledge.getAllInstallations()) {
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
            updateSemaphores(componentKnowledgeService.getCurrentComponentKnowledge());
        }
        final ResizableSemaphore semaphore = semaphores.get(componentIdentifier);
        return SharedThreadPool.getInstance().submit(new Callable<Boolean>() {

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
            updateSemaphores(componentKnowledgeService.getCurrentComponentKnowledge());
        }
        if (semaphores.containsKey(componentIdentifier)) {
            semaphores.get(componentIdentifier).release();
        }
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
    
    protected void bindDistributedComponentKnowledgeService(DistributedComponentKnowledgeService service) {
        componentKnowledgeService = service;
    }

}
