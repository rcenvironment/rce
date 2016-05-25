/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.transport.jms.common;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Base class for single-threaded consumers of a JMS {@link Queue}.
 * 
 * @author Robert Mischke
 */
public abstract class AbstractJmsQueueConsumer implements Runnable {

    protected final Log log = LogFactory.getLog(getClass());

    protected final Connection jmsConnection;

    protected final String queueName;

    private Session session;

    public AbstractJmsQueueConsumer(Connection connection, String queueName) throws JMSException {
        this.jmsConnection = connection;
        this.queueName = queueName;
    }

    @Override
    public void run() {
        try {
            // synchronize for session visibility
            synchronized (this) {
                if (session != null) {
                    // not meant to be run twice
                    throw new IllegalStateException("Session not null");
                }
                session = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            }
            Queue queue = session.createQueue(queueName);
            MessageConsumer consumer = session.createConsumer(queue);
            log.debug("Running listener for queue " + queue.getQueueName() + " in thread " + Thread.currentThread().getName());
            try {
                while (true) {
                    Message message;
                    try {
                        message = consumer.receive();
                        if (message == null) {
                            log.debug("Clean shutdown of queue listener for " + queue.getQueueName() + ": received NULL");
                            break;
                        }
                    } catch (JMSException e) {
                        log.warn("Exception while listening on queue " + queue.getQueueName() + " (unclean shutdown?): " + e.toString());
                        break;
                    }
                    try {
                        if (checkForShutdown(message)) {
                            log.debug("Clean shutdown of queue listener for " + queue.getQueueName() + ": received shutdown message");
                            break;
                        }
                        dispatchMessage(message, jmsConnection);
                    } catch (JMSException e) {
                        log.warn("Error while processing received message; continuing to listen", e);
                    }
                }
            } finally {
                if (session != null) {
                    try {
                        session.close();
                    } catch (JMSException e1) {
                        // TODO how to prevent these? (they should be harmless, though)
                        log.debug("JMS exception while closing inbox consumer session on " + queueName + ": " + e1.toString());
                    }
                }
            }
        } catch (JMSException e) {
            log.warn("Unhandled exception in inbox consumer thread, terminating", e);
        }
    }

    /**
     * Provides a direct way to shut down this listener (instead of posting a shutdown message to the queue). Can be called from any thread,
     * and does not wait for the shutdown to complete.
     * 
     * @throws JMSException on internal JMS errors
     */
    public void triggerShutDown() throws JMSException {
        // synchronize for session visibility
        synchronized (this) {
            if (session == null) {
                log.warn("Queue consumer received shutdown command, but had no session yet");
                return;
            }
            session.close();
        }
    }

    private boolean checkForShutdown(Message message) throws JMSException {
        String messageType = message.getStringProperty(JmsProtocolConstants.MESSAGE_FIELD_MESSAGE_TYPE);
        if (JmsProtocolConstants.MESSAGE_TYPE_QUEUE_SHUTDOWN.equals(messageType)) {
            String textContent = ((TextMessage) message).getText();
            log.debug("Received shutdown command, token=" + textContent);
            // FIXME check token
            return true;
        }
        return false;
    }

    protected abstract void dispatchMessage(Message message, Connection connection);
}
