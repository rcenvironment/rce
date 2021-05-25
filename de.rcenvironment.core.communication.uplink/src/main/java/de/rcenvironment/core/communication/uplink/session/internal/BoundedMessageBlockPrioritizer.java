/*
 * Copyright 2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.session.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.uplink.network.api.MessageBlockPriority;
import de.rcenvironment.core.communication.uplink.network.internal.MessageBlock;
import de.rcenvironment.core.communication.uplink.network.internal.MessageBlockWithMetadata;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Utility class that encapsulates {@link MessageBlock} prioritisation as well as buffer limiting. The latter sets an upper limit of
 * messages queued for each {@link MessageBlockPriority} level, and blocks on attempts to add additional messages. Reaching the limit for
 * one priority level does not block adding messages for other priority levels.
 * 
 * Messages without an explicit prioritisation in relation to each other (i.e. currently, having the same {@link MessageBlockPriority}) are
 * returned FIFO.
 *
 * @author Robert Mischke
 */
public class BoundedMessageBlockPrioritizer {

    private static final int NUM_PRIORITY_LEVELS = MessageBlockPriority.values().length;

    private final List<LinkedBlockingQueue<MessageBlockWithMetadata>> queuesByPriority =
        new ArrayList<LinkedBlockingQueue<MessageBlockWithMetadata>>(NUM_PRIORITY_LEVELS);

    private final Log log = LogFactory.getLog(getClass());

    public BoundedMessageBlockPrioritizer(int maxMessagesPerPriority) {
        // create an array of ordered blocking queues, one for each priority;
        // keeping them in an ArrayList provides efficient access while being type-safe despite generics
        for (int i = 0; i < NUM_PRIORITY_LEVELS; i++) {
            queuesByPriority.add(new LinkedBlockingQueue<MessageBlockWithMetadata>(maxMessagesPerPriority));
        }
    }

    /**
     * Adds a message with a given priority. This method is thread-safe.
     * <p>
     * If the specified message limit for the given priority has not been reached yet, this method returns immediately. Once this limit is
     * reached, however, this method blocks <em>for the given priority</em>; all calling code must be able to handle this. Most notably, the
     * calling code MUST NOT risk causing a deadlock or other issues if this queue blocks.
     * 
     * @param messageBlock the message block to add to the queue
     * @param logPrefix a prefix to prepend to any log messages
     * @throws InterruptedException if interrupted while waiting for per-priority limit space becoming available
     */
    public void submit(MessageBlockWithMetadata messageBlock, String logPrefix) throws InterruptedException {
        // preparations
        Objects.requireNonNull(messageBlock); // guard against invalid null elements
        messageBlock.setLocalQueueStartTime(System.currentTimeMillis());

        // select queue by priority
        MessageBlockPriority priority = messageBlock.getPriority();
        LinkedBlockingQueue<MessageBlockWithMetadata> queue = queuesByPriority.get(priority.getIndex());

        synchronized (queue) { // block concurrent additions to the same queue from interfering
            // append the message at the "tail" of the selected queue, probing first to be able to log a message if it is full
            if (!queue.offer(messageBlock)) {
                MessageBlockWithMetadata headElement = queue.peek();
                // virtually always true; guards against the exotic case of the complete queue being drained between these calls
                if (headElement != null) {
                    long waitTimeOfHeadElement = System.currentTimeMillis() - headElement.getLocalQueueStartTime();
                    log.debug(StringUtils.format("%sStalling a message of type %s as there are already "
                        + "%d messages queued for priority %s; longest queue time: %d msec",
                        logPrefix, messageBlock.getType(), queue.size(), priority.name(), waitTimeOfHeadElement));
                }
                queue.put(messageBlock); // will usually block, unless space became available by a concurrent thread consuming an element
            }
        }
    }

    /**
     * Removes and returns the highest-priority message. This method is thread-safe and always returns immediately.
     * 
     * @return the highest-priority message
     * @throws NoSuchElementException on an empty queue
     */
    public MessageBlockWithMetadata takeNext() throws NoSuchElementException {
        // iterate through queues by descending priority, and return the head of the first non-empty queue
        for (int i = 0; i < NUM_PRIORITY_LEVELS; i++) {
            MessageBlockWithMetadata pollResult = queuesByPriority.get(i).poll(); // removes from "head" if queue is not empty
            if (pollResult != null) {
                return pollResult;
            }
        }
        throw new NoSuchElementException("Expected to find a queued " + MessageBlock.class.getSimpleName());
    }
}
