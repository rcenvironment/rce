/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.testutils.ComponentTestUtils;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;

/**
 * Tests for {@link ComponentExecutionPermitsServiceImpl}.
 * 
 * @author Doreen Seider
 * 
 */
public class ComponentExecutionPermitsServiceImplTest {

    private static final int TEST_TIMEOUT = 2000;

    private static final int WAIT_INTERVAL = 200;

    private static final String COMPONENT_IDENTIFIER_1 = "comp-id-1";

    private static final String COMPONENT_IDENTIFIER_2 = "comp-id-2";

    /**
     * Tests if permits for component execution can be acquired and released as expected.
     * 
     * @throws ExecutionException on error
     * @throws InterruptedException on error
     */
    @Test(timeout = TEST_TIMEOUT)
    public void testAcquireAndReleasePermits() throws InterruptedException, ExecutionException {

        DistributedComponentKnowledgeService componentKnowledgeServiceMock =
            createDistributedComponentKnowledgeServiceMock(createDistributedComponentKnowledgeMock(createSetOfComponentInstallations(
                new String[] { COMPONENT_IDENTIFIER_1, COMPONENT_IDENTIFIER_2 }, new int[] { 2, 1 })));

        final ComponentExecutionPermitsServiceImpl componentExecutionPermitsService = new ComponentExecutionPermitsServiceImpl();
        componentExecutionPermitsService.bindDistributedComponentKnowledgeService(componentKnowledgeServiceMock);

        final AtomicInteger order1 = new AtomicInteger(0);
        final AtomicInteger order2 = new AtomicInteger(0);

        componentExecutionPermitsService.acquire(COMPONENT_IDENTIFIER_1, UUID.randomUUID().toString()).get();
        componentExecutionPermitsService.acquire(COMPONENT_IDENTIFIER_2, UUID.randomUUID().toString()).get();
        componentExecutionPermitsService.acquire(COMPONENT_IDENTIFIER_1, UUID.randomUUID().toString()).get();

        AsyncTaskService threadPool = ConcurrencyUtils.getAsyncTaskService();
        Future<Integer> acquireTask1 = threadPool.submit(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                if (componentExecutionPermitsService.acquire(COMPONENT_IDENTIFIER_1, UUID.randomUUID().toString()).get()) {
                    synchronized (order1) {
                        return order1.incrementAndGet();
                    }
                }
                return order1.get();
            }
        });
        Future<Integer> acquireTask2 = threadPool.submit(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                if (componentExecutionPermitsService.acquire(COMPONENT_IDENTIFIER_2, UUID.randomUUID().toString()).get()) {
                    synchronized (order2) {
                        return order2.incrementAndGet();
                    }
                }
                return order2.get();
            }
        });

        Thread.sleep(WAIT_INTERVAL);
        synchronized (order2) {
            componentExecutionPermitsService.release(COMPONENT_IDENTIFIER_2);
            order2.incrementAndGet();
        }
        assertEquals(2, acquireTask2.get().intValue());
        synchronized (order1) {
            componentExecutionPermitsService.release(COMPONENT_IDENTIFIER_1);
            order1.incrementAndGet();
        }
        assertEquals(2, acquireTask1.get().intValue());

        componentExecutionPermitsService.release(COMPONENT_IDENTIFIER_1);
        componentExecutionPermitsService.acquire(COMPONENT_IDENTIFIER_1, UUID.randomUUID().toString()).get();
    }

    /**
     * Tests if permits for component execution can be acquired and released as expected.
     * 
     * @throws ExecutionException on error
     * @throws InterruptedException on error
     */
    @Test(timeout = TEST_TIMEOUT)
    public void testCancelAcquiringPermits() throws InterruptedException, ExecutionException {

        DistributedComponentKnowledgeService componentKnowledgeServiceMock =
            createDistributedComponentKnowledgeServiceMock(createDistributedComponentKnowledgeMock(createSetOfComponentInstallations(
                new String[] { COMPONENT_IDENTIFIER_1 }, new int[] { 1 })));

        final ComponentExecutionPermitsServiceImpl componentExecutionPermitsService = new ComponentExecutionPermitsServiceImpl();
        componentExecutionPermitsService.bindDistributedComponentKnowledgeService(componentKnowledgeServiceMock);

        assertTrue(componentExecutionPermitsService.acquire(COMPONENT_IDENTIFIER_1, UUID.randomUUID().toString()).get());

        Future<Boolean> acquireTask = componentExecutionPermitsService.acquire(COMPONENT_IDENTIFIER_1, UUID.randomUUID().toString());
        acquireTask.cancel(true);

        componentExecutionPermitsService.release(COMPONENT_IDENTIFIER_1);
        assertTrue(componentExecutionPermitsService.acquire(COMPONENT_IDENTIFIER_1, UUID.randomUUID().toString()).get());

    }

    /**
     * Tests if parameter for maximum parallel executions allowed is increased concurrently. That means, if permit request
     * (ComponentExecutionPermitsServiceImpl#acquire()) is blocked and the maximum parallel executions allowed are increased, the block is
     * expected to disappear immediately.
     * 
     * @throws ExecutionException on error
     * @throws InterruptedException on error
     */
    @Test(timeout = TEST_TIMEOUT)
    public void testIfMaxParallelExecutionParamIsIncreasedConcurrently() throws InterruptedException, ExecutionException {

        DistributedComponentKnowledge componentKnowledgeMock = createDistributedComponentKnowledgeMock(createSetOfComponentInstallations(
            new String[] { COMPONENT_IDENTIFIER_1 }, new int[] { 1 }));

        DistributedComponentKnowledgeService componentKnowledgeServiceMock =
            createDistributedComponentKnowledgeServiceMock(componentKnowledgeMock);

        final ComponentExecutionPermitsServiceImpl componentExecutionPermitsService = new ComponentExecutionPermitsServiceImpl();
        componentExecutionPermitsService.bindDistributedComponentKnowledgeService(componentKnowledgeServiceMock);

        final AtomicInteger order = new AtomicInteger(0);

        componentExecutionPermitsService.acquire(COMPONENT_IDENTIFIER_1, UUID.randomUUID().toString()).get();

        AsyncTaskService threadPool = ConcurrencyUtils.getAsyncTaskService();
        Future<Integer> acquireTask = threadPool.submit(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                componentExecutionPermitsService.acquire(COMPONENT_IDENTIFIER_1, UUID.randomUUID().toString()).get();
                synchronized (order) {
                    return order.incrementAndGet();
                }
            }
        });

        Thread.sleep(WAIT_INTERVAL);
        synchronized (order) {
            componentExecutionPermitsService.onDistributedComponentKnowledgeChanged(componentKnowledgeMock);
            order.incrementAndGet();
        }
        Thread.sleep(WAIT_INTERVAL);
        DistributedComponentKnowledge updatedComponentKnowledgeMock = EasyMock.createStrictMock(DistributedComponentKnowledge.class);
        final Set<ComponentInstallation> mockInstallations = createSetOfComponentInstallations(
            new String[] { COMPONENT_IDENTIFIER_1 }, new int[] { 2 });
        EasyMock.expect(updatedComponentKnowledgeMock.getAllInstallations())
            .andReturn(ComponentTestUtils.convertToListOfDistributedComponentEntries(mockInstallations)).anyTimes();
        EasyMock.replay(updatedComponentKnowledgeMock);

        synchronized (order) {
            componentExecutionPermitsService.onDistributedComponentKnowledgeChanged(updatedComponentKnowledgeMock);
            order.incrementAndGet();
        }
        assertEquals(3, acquireTask.get().intValue());
    }

    /**
     * Tests if parameter for maximum parallel executions allowed is decreased concurrently. That means, if permit request (
     * {@link ComponentExecutionPermitsService#acquire(String, String)}) is blocked and the maximum parallel executions allowed are
     * increased, the block is expected to disappear immediately.
     * 
     * @throws ExecutionException on error
     * @throws InterruptedException on error
     */
    @Test(timeout = TEST_TIMEOUT)
    public void testIfMaxParallelExecutionParamIsDecreasedConcurrently() throws InterruptedException, ExecutionException {

        DistributedComponentKnowledge componentKnowledgeMock = createDistributedComponentKnowledgeMock(createSetOfComponentInstallations(
            new String[] { COMPONENT_IDENTIFIER_1 }, new int[] { 2 }));

        DistributedComponentKnowledgeService componentKnowledgeServiceMock =
            createDistributedComponentKnowledgeServiceMock(componentKnowledgeMock);

        final ComponentExecutionPermitsServiceImpl componentExecutionPermitsService = new ComponentExecutionPermitsServiceImpl();
        componentExecutionPermitsService.bindDistributedComponentKnowledgeService(componentKnowledgeServiceMock);

        final AtomicInteger order = new AtomicInteger(0);

        componentExecutionPermitsService.acquire(COMPONENT_IDENTIFIER_1, UUID.randomUUID().toString()).get();
        componentExecutionPermitsService.acquire(COMPONENT_IDENTIFIER_1, UUID.randomUUID().toString()).get();

        AsyncTaskService threadPool = ConcurrencyUtils.getAsyncTaskService();
        Future<Integer> acquireTask = threadPool.submit(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                componentExecutionPermitsService.acquire(COMPONENT_IDENTIFIER_1, UUID.randomUUID().toString()).get();
                synchronized (order) {
                    return order.incrementAndGet();
                }
            }
        });

        Thread.sleep(WAIT_INTERVAL);
        synchronized (order) {
            componentExecutionPermitsService.onDistributedComponentKnowledgeChanged(componentKnowledgeMock);
            order.incrementAndGet();
        }
        Thread.sleep(WAIT_INTERVAL);
        DistributedComponentKnowledge updatedComponentKnowledgeMock = EasyMock.createStrictMock(DistributedComponentKnowledge.class);
        final Set<ComponentInstallation> mockInstallations = createSetOfComponentInstallations(
            new String[] { COMPONENT_IDENTIFIER_1 }, new int[] { 1 });
        EasyMock.expect(updatedComponentKnowledgeMock.getAllInstallations())
            .andReturn(ComponentTestUtils.convertToListOfDistributedComponentEntries(mockInstallations)).anyTimes();
        EasyMock.replay(updatedComponentKnowledgeMock);
        synchronized (order) {
            componentExecutionPermitsService.onDistributedComponentKnowledgeChanged(updatedComponentKnowledgeMock);
            order.incrementAndGet();
        }
        Thread.sleep(WAIT_INTERVAL);
        synchronized (order) {
            componentExecutionPermitsService.release(COMPONENT_IDENTIFIER_1);
            order.incrementAndGet();
        }
        Thread.sleep(WAIT_INTERVAL);
        synchronized (order) {
            componentExecutionPermitsService.release(COMPONENT_IDENTIFIER_1);
            order.incrementAndGet();
        }
        assertEquals(5, acquireTask.get().intValue());
    }

    /**
     * Performs more releases than acquires and checks if this doesn't affect the maximum amounts of execution permits allowed.
     * 
     * @throws ExecutionException on error
     * @throws InterruptedException on error
     */
    @Test(timeout = TEST_TIMEOUT)
    public void testIfMultipleReleasesDoesNotIncreaseTheMaximumAmountsOfPermitsAllowed() throws InterruptedException, ExecutionException {
        DistributedComponentKnowledgeService componentKnowledgeServiceMock =
            createDistributedComponentKnowledgeServiceMock(createDistributedComponentKnowledgeMock(createSetOfComponentInstallations(
                new String[] { COMPONENT_IDENTIFIER_1 }, new int[] { 1 })));

        final ComponentExecutionPermitsServiceImpl componentExecutionPermitsService = new ComponentExecutionPermitsServiceImpl();
        componentExecutionPermitsService.bindDistributedComponentKnowledgeService(componentKnowledgeServiceMock);

        final AtomicInteger order = new AtomicInteger(0);

        componentExecutionPermitsService.release(COMPONENT_IDENTIFIER_1);
        componentExecutionPermitsService.release(COMPONENT_IDENTIFIER_1);
        componentExecutionPermitsService.release(COMPONENT_IDENTIFIER_1);
        componentExecutionPermitsService.acquire(COMPONENT_IDENTIFIER_1, UUID.randomUUID().toString()).get();

        AsyncTaskService threadPool = ConcurrencyUtils.getAsyncTaskService();
        Future<Integer> acquireTask = threadPool.submit(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                componentExecutionPermitsService.acquire(COMPONENT_IDENTIFIER_1, UUID.randomUUID().toString()).get();
                synchronized (order) {
                    return order.incrementAndGet();
                }
            }
        });
        Thread.sleep(WAIT_INTERVAL);
        synchronized (order) {
            componentExecutionPermitsService.release(COMPONENT_IDENTIFIER_1);
            order.incrementAndGet();
        }
        assertEquals(2, acquireTask.get().intValue());
    }

    /**
     * Sets the maximum parallel execution allowed to 0 and awaits that no permit is granted when
     * {@link ComponentExecutionPermitsService#acquire(String, String)} is called.
     * 
     * @throws ExecutionException on error
     * @throws InterruptedException on error
     */
    @Test(timeout = TEST_TIMEOUT)
    public void testIfNoPermitIsGrantedMaximumAmountOfParallelExecutionsIsLeesOrEqualZero()
        throws InterruptedException, ExecutionException {
        final int minusFive = -5;
        DistributedComponentKnowledgeService componentKnowledgeServiceMock =
            createDistributedComponentKnowledgeServiceMock(createDistributedComponentKnowledgeMock(createSetOfComponentInstallations(
                new String[] { COMPONENT_IDENTIFIER_1, COMPONENT_IDENTIFIER_2 }, new int[] { 0, minusFive })));

        final ComponentExecutionPermitsServiceImpl componentExecutionPermitsService = new ComponentExecutionPermitsServiceImpl();
        componentExecutionPermitsService.bindDistributedComponentKnowledgeService(componentKnowledgeServiceMock);

        AsyncTaskService threadPool = ConcurrencyUtils.getAsyncTaskService();
        Future<Void> acquireTask1 = threadPool.submit(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                componentExecutionPermitsService.acquire(COMPONENT_IDENTIFIER_1, UUID.randomUUID().toString()).get();
                return null;
            }
        });
        Future<Void> acquireTask2 = threadPool.submit(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                componentExecutionPermitsService.acquire(COMPONENT_IDENTIFIER_2, UUID.randomUUID().toString()).get();
                return null;
            }
        });

        final int timeoutInMillis = 200;
        try {
            acquireTask1.get(timeoutInMillis, TimeUnit.MILLISECONDS);
            fail("Timeout expected.");
        } catch (TimeoutException e) {
            assertTrue(true);
        }
        try {
            acquireTask2.get(timeoutInMillis, TimeUnit.MILLISECONDS);
            fail("Timeout expected.");
        } catch (TimeoutException e) {
            assertTrue(true);
        }
    }

    /**
     * Performs more releases than acquires and checks if this doesn't affect the maximum amounts of execution permits allowed.
     * 
     * @throws ExecutionException on error
     * @throws InterruptedException on error
     */
    @Test(timeout = TEST_TIMEOUT)
    public void testUnlimitedParallelExecutionByDefiningNoParamForMaximumParallelExecution()
        throws InterruptedException, ExecutionException {
        DistributedComponentKnowledgeService componentKnowledgeServiceMock =
            createDistributedComponentKnowledgeServiceMock(createDistributedComponentKnowledgeMock(createSetOfComponentInstallations(
                new String[] { COMPONENT_IDENTIFIER_1 }, new int[] {})));

        final ComponentExecutionPermitsServiceImpl componentExecutionPermitsService = new ComponentExecutionPermitsServiceImpl();
        componentExecutionPermitsService.bindDistributedComponentKnowledgeService(componentKnowledgeServiceMock);

        final int amountOfAcquires = 100;
        for (int i = 0; i < amountOfAcquires; i++) {
            componentExecutionPermitsService.acquire(COMPONENT_IDENTIFIER_1, UUID.randomUUID().toString()).get();
        }
    }

    private DistributedComponentKnowledgeService createDistributedComponentKnowledgeServiceMock(
        DistributedComponentKnowledge componentKnowledge) {
        DistributedComponentKnowledgeService componentKnowledgeServiceMock = EasyMock.createNiceMock(
            DistributedComponentKnowledgeService.class);
        EasyMock.expect(componentKnowledgeServiceMock.getCurrentSnapshot()).andReturn(componentKnowledge).anyTimes();
        EasyMock.replay(componentKnowledgeServiceMock);
        return componentKnowledgeServiceMock;
    }

    private DistributedComponentKnowledge createDistributedComponentKnowledgeMock(Set<ComponentInstallation> componentInstallations) {
        DistributedComponentKnowledge componentKnowledgeMock = EasyMock.createStrictMock(DistributedComponentKnowledge.class);
        EasyMock.expect(componentKnowledgeMock.getAllInstallations())
            .andReturn(ComponentTestUtils.convertToListOfDistributedComponentEntries(componentInstallations))
            .anyTimes();
        EasyMock.replay(componentKnowledgeMock);
        return componentKnowledgeMock;
    }

    private ComponentInstallation createComponentInstallationMock(String compId, Integer maxParallelExecutions) {
        ComponentInstallation componentInstallationMock = EasyMock.createNiceMock(ComponentInstallation.class);
        ComponentInterface componentInterfaceMock = EasyMock.createNiceMock(ComponentInterface.class);
        EasyMock.expect(componentInterfaceMock.getDisplayName()).andReturn(compId).anyTimes();
        EasyMock.replay(componentInterfaceMock);
        EasyMock.expect(componentInstallationMock.getComponentInterface()).andReturn(componentInterfaceMock).anyTimes();
        EasyMock.expect(componentInstallationMock.getInstallationId()).andReturn(compId).anyTimes();
        EasyMock.expect(componentInstallationMock.getMaximumCountOfParallelInstances()).andReturn(maxParallelExecutions).anyTimes();
        EasyMock.replay(componentInstallationMock);
        return componentInstallationMock;
    }

    private Set<ComponentInstallation> createSetOfComponentInstallations(String[] compIds, int[] maxParallelExecutions) {
        Set<ComponentInstallation> componentInstallations = new HashSet<>();
        for (int i = 0; i < compIds.length; i++) {
            if (i < maxParallelExecutions.length) {
                componentInstallations.add(createComponentInstallationMock(compIds[i], maxParallelExecutions[i]));
            } else {
                componentInstallations.add(createComponentInstallationMock(compIds[i], null));
            }
        }
        return componentInstallations;
    }

}
