/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.transport.jms.common;

import de.rcenvironment.core.communication.transport.spi.HandshakeInformation;

/**
 * An extension of {@link HandshakeInformation} with JMS-specific fields.
 * 
 * @author Robert Mischke
 */
public class JMSHandshakeInformation extends HandshakeInformation {

    private String temporaryQueueInformation;

    public String getTemporaryQueueInformation() {
        return temporaryQueueInformation;
    }

    public void setTemporaryQueueInformation(String newValue) {
        this.temporaryQueueInformation = newValue;
    }
}
