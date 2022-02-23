/*
 * Copyright 2021-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.session.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.uplink.network.api.MessageBlockPriority;
import de.rcenvironment.core.communication.uplink.network.internal.MessageBlock;
import de.rcenvironment.core.communication.uplink.network.internal.MessageBlockWithMetadata;
import de.rcenvironment.core.communication.uplink.network.internal.UplinkProtocolConfiguration;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;
import de.rcenvironment.core.utils.incubator.DebugSettings;

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

    // note: "flow control" is not quite accurate (yet?), but avoids switching to a different id later
    private static final boolean VERBOSE_LOGGING_ENABLED = DebugSettings.getVerboseLoggingEnabled("uplink.flowcontrol");

    private final List<LinkedBlockingQueue<MessageBlockWithMetadata>> queuesByPriority =
        new ArrayList<LinkedBlockingQueue<MessageBlockWithMetadata>>(NUM_PRIORITY_LEVELS);

    private final Log log = LogFactory.getLog(getClass());

    public BoundedMessageBlockPrioritizer() {

        Map<MessageBlockPriority, Integer> maxMessagesPerPriority =
            UplinkProtocolConfiguration.getCurrent().getMaxBufferedOutgoingMessagesPerSessionAndPriority();

        // create an array of ordered blocking queues, one for each priority;
        // keeping them in an ArrayList provides efficient access while being type-safe despite generics
        for (MessageBlockPriority priority : MessageBlockPriority.values()) {
            // fail on undefined buffer size for any priority level
            try {
                Integer maxMessages = Objects.requireNonNull(maxMessagesPerPriority.get(priority));
                queuesByPriority.add(new LinkedBlockingQueue<MessageBlockWithMetadata>(maxMessages));
            } catch (NullPointerException | IllegalArgumentException e) {
                log.error("Invalid limit value for priority " + priority.name() + ": " + maxMessagesPerPriority.get(priority));
                throw e; // after logging the source, fail anyway
            }
        }
    }

    /**
     * Adds a message with a given priority. This method is thread-safe.
     * <p>
     * If the specified message limit for the given priority has not been reached yet, this method returns "semi-immediately", in the sense
     * that it may still temporarily block if concurrent threads are submitting messages of the same priority. Once the message limit is
     * reached, however, this method blocks <em>for the given priority</em>; all calling code must be able to handle this. Most notably, the
     * calling code MUST NOT risk causing a deadlock or other issues if this queue blocks.
     * 
     * @param messageBlock the message block to add to the queue
     * @param logPrefix a prefix to prepend to any log messages
     * @throws InterruptedException if interrupted while waiting for per-priority limit space becoming available
     */
    public void submitOrBlock(MessageBlockWithMetadata messageBlock, String logPrefix) throws InterruptedException {
        // preparations
        Objects.requireNonNull(messageBlock); // guard against invalid null elements
        messageBlock.setLocalQueueStartTime(System.currentTimeMillis());

        // select queue by priority
        MessageBlockPriority priority = messageBlock.getPriority();
        LinkedBlockingQueue<MessageBlockWithMetadata> queue = queuesByPriority.get(priority.getIndex());

        // synchronize to block concurrent additions to the same queue from interfering
        // IMPORTANT: removal operations MUST NOT synchronize on this as well, as this would result in a deadlock when the queue is full
        synchronized (queue) {
            // append the message at the "tail" of the selected queue, probing first to be able to log a message if it is full
            if (!queue.offer(messageBlock)) {
                MessageBlockWithMetadata headElement = queue.peek();
                // virtually always true; guards against the exotic case of the complete queue being drained between these calls
                if (headElement != null) {
                    long waitTimeOfHeadElement = System.currentTimeMillis() - headElement.getLocalQueueStartTime();
                    if (VERBOSE_LOGGING_ENABLED) {
                        log.debug(StringUtils.format("%sStalling a message of type %s as there are already "
                            + "%d messages queued for priority %s; longest queue time: %d msec",
                            logPrefix, messageBlock.getType(), queue.size(), priority.name(), waitTimeOfHeadElement));
                    }
                }
                queue.put(messageBlock); // will usually block, unless space became available by a concurrent thread consuming an element
            }
        }
    }

    /**
     * Adds a message with a given priority. This method is thread-safe. This method always returns "semi-immediately", in the sense that it
     * never blocks due to a full queue, but temporarily blocks if concurrent threads are submitting messages of the same priority.
     * <p>
     * If the specified message limit for the given priority has been reached yet, this method fails with an exception. This is designed as
     * a mechanism for queues where blocking on a full queue is not acceptable. If this occurs, the caller must decide how to deal with the
     * situation explicitly.
     * 
     * @param messageBlock the message block to add to the queue
     * @param logPrefix a prefix to prepend to any log messages
     * @throws OperationFailureException if the message could not be added due to the queue's limit being reached
     * @throws InterruptedException if interrupted while waiting for per-priority limit space becoming available
     */
    public void submitOrFail(MessageBlockWithMetadata messageBlock, String logPrefix)
        throws OperationFailureException, InterruptedException {
        // preparations
        Objects.requireNonNull(messageBlock); // guard against invalid null elements
        messageBlock.setLocalQueueStartTime(System.currentTimeMillis());

        // select queue by priority
        MessageBlockPriority priority = messageBlock.getPriority();
        LinkedBlockingQueue<MessageBlockWithMetadata> queue = queuesByPriority.get(priority.getIndex());

        // synchronize to block concurrent additions to the same queue from interfering
        // IMPORTANT: removal operations MUST NOT synchronize on this as well, as this would result in a deadlock when the queue is full
        synchronized (queue) {
            // append the message at the "tail" of the selected queue, probing first to be able to log a message if it is full
            if (!queue.offer(messageBlock)) {
                MessageBlockWithMetadata headElement = queue.peek();
                // virtually always true; guards against the exotic case of the complete queue being drained between these calls
                if (headElement != null) {
                    long waitTimeOfHeadElement = System.currentTimeMillis() - headElement.getLocalQueueStartTime();
                    throw new OperationFailureException(StringUtils.format(
                        "%sFailed to submit a message of type %s for sending as there are already "
                            + "%d messages queued for priority %s; longest queue time: %d msec",
                        logPrefix, messageBlock.getType(), queue.size(), priority.name(), waitTimeOfHeadElement));
                }
            }
        }
    }

    /**
     * Removes and returns the highest-priority message. This method is thread-safe and always returns immediately.
     * 
     * @return the highest-priority message, or {@link Optional#empty()} if all queues are empty
     */
    public Optional<MessageBlockWithMetadata> takeNext() throws NoSuchElementException {
        // iterate through queues by descending priority, and return the head of the first non-empty queue
        // TODO optimize this by keeping a pointer to the highest priority with a non-empty queue
        for (int i = 0; i < NUM_PRIORITY_LEVELS; i++) {
            MessageBlockWithMetadata pollResult = queuesByPriority.get(i).poll(); // removes from "head" if queue is not empty
            if (pollResult != null) {
                return Optional.of(pollResult);
            }
        }
        return Optional.empty();
    }

}
