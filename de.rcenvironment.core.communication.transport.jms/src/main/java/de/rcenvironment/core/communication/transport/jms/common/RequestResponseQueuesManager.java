/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.transport.jms.common;

import java.net.ProtocolException;

import javax.jms.JMSException;
import javax.jms.Session;

import de.rcenvironment.core.communication.transport.spi.MessageChannel;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Manages the (temporary) queues created as part of a JMS {@link MessageChannel}.
 * 
 * @author Robert Mischke
 */
public class RequestResponseQueuesManager {

    private String c2bRequestQueue;

    private String c2bResponseQueue;

    private String b2cRequestQueue;

    private String b2cResponseQueue;

    /**
     * Creates two temporary queues for requests and responses consumed by the client.
     * 
     * @param initialSession the client's initial JMS session
     * @return the information string to send to the server
     * @throws JMSException on errors creating the queues
     */
    public String initClientSide(Session initialSession) throws JMSException {
        c2bResponseQueue = initialSession.createTemporaryQueue().getQueueName();
        b2cRequestQueue = initialSession.createTemporaryQueue().getQueueName();
        return StringUtils.escapeAndConcat(c2bResponseQueue, b2cRequestQueue);
    }

    /**
     * Creates two temporary queues for requests and responses consumed by the server.
     * 
     * @param serverSession the server's JMS session
     * @param clientInfo the information string sent by the client
     * @return the information string to send back to the client
     * @throws JMSException on errors creating the queues
     * @throws ProtocolException on unexpected client data
     */
    public String initServerSide(Session serverSession, String clientInfo) throws JMSException, ProtocolException {
        if (clientInfo == null) {
            throw new ProtocolException("The client side did not send the expected temporary queue information");
        }
        String[] clientTempQueues = StringUtils.splitAndUnescape(clientInfo);
        c2bResponseQueue = clientTempQueues[0];
        b2cRequestQueue = clientTempQueues[1];
        c2bRequestQueue = JmsProtocolConstants.QUEUE_NAME_C2B_REQUEST_INBOX; // serverSession.createTemporaryQueue().getQueueName();
        b2cResponseQueue = serverSession.createTemporaryQueue().getQueueName();
        return StringUtils.escapeAndConcat(c2bRequestQueue, b2cResponseQueue);
    }

    /**
     * Registers the server's temporary queues on the client side.
     * 
     * @param serverInfo the information string sent by the server
     * @throws ProtocolException on unexpected client data
     */
    public void finishClientSide(String serverInfo) throws ProtocolException {
        if (serverInfo == null) {
            throw new ProtocolException("The server side did not send the expected temporary queue information");
        }
        String[] serverTempQueues = StringUtils.splitAndUnescape(serverInfo);
        c2bRequestQueue = serverTempQueues[0];
        b2cResponseQueue = serverTempQueues[1];
    }

    public String getC2BRequestQueue() {
        return c2bRequestQueue;
    }

    public String getC2BResponseQueue() {
        return c2bResponseQueue;
    }

    public String getB2CRequestQueue() {
        return b2cRequestQueue;
    }

    public String getB2CResponseQueue() {
        return b2cResponseQueue;
    }

}
