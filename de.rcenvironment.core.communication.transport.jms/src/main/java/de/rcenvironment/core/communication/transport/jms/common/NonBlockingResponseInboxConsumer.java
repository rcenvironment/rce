/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.communication.transport.jms.common;

import java.util.HashMap;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;

import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * A single-threaded consumer that listens for all responses sent to a shared queue.
 * 
 * @author Robert Mischke
 */
public final class NonBlockingResponseInboxConsumer extends AbstractJmsQueueConsumer implements Runnable {

    /**
     * The number of retries to make before deciding that there really is no registered local listener for a network response.
     * 
     * Note that there is no harm in retrying for quite a while - the only side effect of a high value is that messages that actually arrive
     * after the network timeout are reported/logged with an additional delay of MAX_RETRY_COUNT * WAIT_MSEC. - misc_ro
     */
    private static final int RESPONSE_LISTENER_MAX_RETRY_COUNT = 20;

    /**
     * The time to wait per retry attempt.
     */
    private static final int RESPONSE_LISTENER_RETRY_WAIT_MSEC = 500;

    /**
     * A simple callback for either a received response or a timeout event.
     * 
     * @author Robert Mischke
     */
    public interface JmsResponseCallback {

        /**
         * Called when an actual response was received.
         * 
         * @param jmsResponse the received JMS response.
         */
        void onResponseReceived(Message jmsResponse);

        /**
         * Called when the timeout was reached.
         */
        void onTimeoutReached();

        /**
         * Called when the channel was closed while waiting for a response.
         */
        void onChannelClosed();
    }

    private final AsyncTaskService threadPool = ConcurrencyUtils.getAsyncTaskService();

    private final Map<String, JmsResponseCallback> responseListenerMap = new HashMap<>();

    public NonBlockingResponseInboxConsumer(String queueName, Connection connection)
        throws JMSException {
        super(connection, queueName);
    }

    /**
     * Registers the destination to send the non-blocking response to when it arrives, along with a timeout parameter.
     * 
     * @param messageId the message correlation id
     * @param jmsResponseListener the callback listener
     * @param timeoutMsec the timeout in msec
     */
    public void registerResponseListener(final String messageId, JmsResponseCallback jmsResponseListener, final long timeoutMsec) {

        // sanity check
        if (messageId == null) {
            log.error("Internal consistency error: message id == null");
            jmsResponseListener.onTimeoutReached(); // arbitrary handling in case of this abnormal situation
            return;
        }

        // log.debug("Registering response listener for message id " + messageId);
        synchronized (responseListenerMap) {
            JmsResponseCallback replaced = responseListenerMap.put(messageId, jmsResponseListener);
            // sanity check
            if (replaced != null) {
                log.error("Internal consistency error: There was already a response listener registered for message id " + messageId);
                jmsResponseListener.onTimeoutReached(); // arbitrary handling in case of this abnormal situation
                return;
            }
        }
        threadPool.scheduleAfterDelay(new Runnable() {

            @Override
            @TaskDescription("JMS Network Transport: Check for request completion after timeout")
            public void run() {
                final JmsResponseCallback unfulfilledResponseListener;
                synchronized (responseListenerMap) {
                    unfulfilledResponseListener = responseListenerMap.remove(messageId);
                }
                if (unfulfilledResponseListener != null) {
                    log.debug("Reached timeout (" + timeoutMsec + "ms) for message id " + messageId);
                    unfulfilledResponseListener.onTimeoutReached();
                }

            }
        }, timeoutMsec);
    }

    @Override
    @TaskDescription("JMS Network Transport: Non-blocking response listener")
    public void run() {
        super.run();
        synchronized (responseListenerMap) {
            if (!responseListenerMap.isEmpty()) {
                log.debug("Response listener for queue " + queueName + " has been shut down while " + responseListenerMap.size()
                    + " request(s) were still pending; generating failure responses");
                for (final JmsResponseCallback listener : responseListenerMap.values()) {
                    threadPool.execute("JMS Network Transport: Handle pending non-blocking request after queue listener shutdown",
                        listener::onChannelClosed);


                }
                // requests are handled; do not send timeout responses, too
                responseListenerMap.clear();
            }
        }
    }

    @Override
    protected void dispatchMessage(final Message message, final Connection jmsConnection) {
        threadPool.execute("JMS Network Transport: Dispatch incoming response", () -> {

            final String messageId;
            try {
                messageId = message.getJMSCorrelationID();
                // sanity check
                if (messageId == null) {
                    log.error("Unexpected state: null JMS message correlation id");
                    return; // no graceful handling possible
                }
            } catch (JMSException e) {
                log.error("Unexpected error while handling JMS response", e);
                // TODO add an error callback for this? right now, the timeout will handle it
                return;
            }
            JmsResponseCallback responseListener;
            // As the JMS broker-generated message ids are used for correlation, the response listener cannot be registered until after
            // the JMS message has been sent. Usually, this is not a problem. If local CPU load and/or thread congestion is very high,
            // however, the response can arrive before the sender has managed to register its response listener in the synchronized map.
            // This retry loop fixes this problem by waiting briefly in case no listener is found. - misc_ro
            int retryCount = 0;
            while (true) {
                synchronized (responseListenerMap) {
                    responseListener = responseListenerMap.remove(messageId);
                }
                if (responseListener != null) {
                    if (retryCount > 0) {
                        log.debug("Successfully fetched mapping information for a network response after retrying for "
                            + retryCount * RESPONSE_LISTENER_RETRY_WAIT_MSEC
                            + " msec; there is probably high CPU load on the local instance");
                    }
                    responseListener.onResponseReceived(message);
                    break;
                }
                if (retryCount >= RESPONSE_LISTENER_MAX_RETRY_COUNT) {
                    log.debug("No response listener for message " + messageId
                        + " even after retrying - most likely, the response arrived after the timeout");
                    return;
                }
                retryCount++;
                try {
                    Thread.sleep(RESPONSE_LISTENER_RETRY_WAIT_MSEC);
                } catch (InterruptedException e) {
                    log.warn("Thread interrupted while retrying to fetch response mapping information");
                    return; // in case only this thread was interrupted, this will be handled by the standard timeout
                }
            }

        });
    }

}
