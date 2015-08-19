/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.transport.jms.common;

import de.rcenvironment.core.communication.transport.spi.HandshakeInformation;

/**
 * An extension of {@link HandshakeInformation} with JMS-specific fields.
 * 
 * @author Robert Mischke
 */
public class JMSHandshakeInformation extends HandshakeInformation {

    private String remoteInitiatedRequestInboxQueueName;

    public String getRemoteInitiatedRequestInboxQueueName() {
        return remoteInitiatedRequestInboxQueueName;
    }

    public void setRemoteInitiatedRequestInboxQueueName(String remoteInitiatedRequestInboxQueueName) {
        this.remoteInitiatedRequestInboxQueueName = remoteInitiatedRequestInboxQueueName;
    }
}
