/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.transport.jms.activemq.impl;

import de.rcenvironment.core.communication.channel.MessageChannelIdFactory;
import de.rcenvironment.core.communication.transport.jms.activemq.internal.ActiveMQJmsFactory;
import de.rcenvironment.core.communication.transport.jms.common.AbstractJmsTransportProvider;
import de.rcenvironment.core.communication.transport.spi.DefaultMessageChannelIdFactoryImpl;

/**
 * ActiveMQ variant of the generic JMS transport provider.
 * 
 * @author Robert Mischke
 */
public class ActiveMQTransportProvider extends AbstractJmsTransportProvider {

    /**
     * The transport id of this provider.
     */
    public static final String TRANSPORT_ID = "activemq-tcp";

    public ActiveMQTransportProvider() {
        this(new DefaultMessageChannelIdFactoryImpl());
    }

    // explicit constructor for unit tests
    public ActiveMQTransportProvider(MessageChannelIdFactory connectionIdFactory) {
        super(connectionIdFactory, new ActiveMQJmsFactory());
    }

    @Override
    public String getTransportId() {
        return TRANSPORT_ID;
    }

    // OSGi-DS component lifecycle method
    protected void activate() {
        log.debug("Activating ActiveMQ transport");
    }

    // OSGi-DS component lifecycle method
    protected void deactivate() {
        log.debug("Deactivating ActiveMQ transport");
    }

}
