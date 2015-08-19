/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.transport.jms.common;

import de.rcenvironment.core.communication.model.MessageChannel;

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
     * @param destinationQueueName the name of the JMS queue to send outgoing requests to
     */
    void setOutgoingRequestQueueName(String destinationQueueName);

    /**
     * @param token the (usually random and secret) token sent with the special messages that shut
     *        down a JMS queue; prevents unauthorized queue shutdown
     */
    void setShutdownSecurityToken(String token);

}
