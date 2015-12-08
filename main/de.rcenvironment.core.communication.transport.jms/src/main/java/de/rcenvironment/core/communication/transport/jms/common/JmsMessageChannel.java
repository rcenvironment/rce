/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.transport.jms.common;

import javax.jms.JMSException;

import de.rcenvironment.core.communication.transport.spi.MessageChannel;

/**
 * Sub-interface of {@link MessageChannel} that adds JMS-specific methods.
 * 
 * @author Robert Mischke
 */
public interface JmsMessageChannel extends MessageChannel {

    /**
     * @return the name of the JMS queue to send outgoing requests to
     */
    String getOutgoingRequestQueueName();

    /**
     * @param token the (usually random and secret) token sent with the special messages that shut down a JMS queue; prevents unauthorized
     *        queue shutdown
     */
    void setShutdownSecurityToken(String token);

    /**
     * Configures and starts the worker tasks used for non-blocking request handling.
     * 
     * @param outgoingRequestQueueName the name of the JMS queue to send outgoing requests to
     * @param incomingResponseQueueName the name of the JMS queue to listen on for incoming requests
     * @throws JMSException on setup failure
     */
    void setupNonBlockingRequestSending(String outgoingRequestQueueName, String incomingResponseQueueName) throws JMSException;

}
