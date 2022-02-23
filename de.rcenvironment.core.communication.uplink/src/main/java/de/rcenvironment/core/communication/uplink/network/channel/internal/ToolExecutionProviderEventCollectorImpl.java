/*
 * Copyright 2019-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.network.channel.internal;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionProviderEventCollector;
import de.rcenvironment.toolkit.modules.concurrency.api.BatchAggregator;
import de.rcenvironment.toolkit.modules.concurrency.api.ConcurrencyUtilsFactory;

/**
 * {@link ToolExecutionProviderEventCollector} implementation.
 *
 * @author Robert Mischke
 */
public class ToolExecutionProviderEventCollectorImpl implements ToolExecutionProviderEventCollector {

    private static final int MAX_BATCH_SIZE = 50;

    private static final long MAX_BATCH_LATENCY = 500;

    // only needed to prevent *really* excessive numbers of queued events, so the exact number is not too relevant;
    // note that this does not take data length into account, so limiting heap usage would need another counter
    private static final int MAX_QUEUED_EVENTS = 1000;

    private static final int MAX_TIME_TO_WAIT_FOR_QUEUE_COMPLETION_MSEC = 5000;

    private final BatchAggregator<ToolExecutionProviderEventTransferObject> batchAggregator;

    private final Semaphore maxQueuedEventsSemaphore = new Semaphore(MAX_QUEUED_EVENTS);

    private boolean shutDown;

    private final Object shutDownLock = new Object();

    private Consumer<List<ToolExecutionProviderEventTransferObject>> batchConsumer;

    public ToolExecutionProviderEventCollectorImpl(Consumer<List<ToolExecutionProviderEventTransferObject>> batchConsumer,
        ConcurrencyUtilsFactory concurrencyUtilsFactory) {
        this.batchConsumer = batchConsumer;
        this.batchAggregator = concurrencyUtilsFactory.createBatchAggregator(MAX_BATCH_SIZE, MAX_BATCH_LATENCY,
            this::enqueueBatchOfTransferObjectsForSending);
    }

    @Override
    public void submitEvent(String type, String data) {
        synchronized (shutDownLock) {
            if (shutDown) {
                LogFactory.getLog(getClass()).warn(
                    "Dropping execution event as the event forwarder has already been shut down: Type='" + type + "', data='" + data + "'");
                return;
            }
        }
        try {
            maxQueuedEventsSemaphore.acquire();
        } catch (InterruptedException e) {
            LogFactory.getLog(getClass()).warn(
                "Dropping execution event due to interruption while waiting for space in the outgoing queue: Type='" + type + "', data='"
                    + data + "'");
            Thread.currentThread().interrupt();
        }
        batchAggregator.enqueue(new ToolExecutionProviderEventTransferObject(type, data));
    }

    /**
     * Called at the end of tool execution, this blocks any further events from being enqueued (which can only happen if event handling is
     * asynchronous), and waits until all queued event batches have been forwarded to the event consumer.
     * 
     * @throws InterruptedException on interruption while waiting to complete
     */
    public void shutdownAndAwaitCompletion() throws InterruptedException {
        synchronized (shutDownLock) {
            shutDown = true;
        }
        // waits until the queue is empty
        if (!maxQueuedEventsSemaphore.tryAcquire(MAX_QUEUED_EVENTS, MAX_TIME_TO_WAIT_FOR_QUEUE_COMPLETION_MSEC, TimeUnit.MILLISECONDS)) {
            // log this, but otherwise ignore it
            LogFactory.getLog(getClass()).warn(
                "Waited for the outgoing event queue to complete for the maximum wait time of " + MAX_TIME_TO_WAIT_FOR_QUEUE_COMPLETION_MSEC
                    + " msec");
        }
    }

    private void enqueueBatchOfTransferObjectsForSending(List<ToolExecutionProviderEventTransferObject> batch) {
        batchConsumer.accept(batch);
        maxQueuedEventsSemaphore.release(batch.size());
    }

}
