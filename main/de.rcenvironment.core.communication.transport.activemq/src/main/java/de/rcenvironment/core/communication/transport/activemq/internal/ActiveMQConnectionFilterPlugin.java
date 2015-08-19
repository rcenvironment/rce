/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.transport.activemq.internal;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jms.Connection;

import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerPluginSupport;
import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.ConnectionInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.configuration.ConnectionFilter;

/**
 * A custom ActiveMQ {@link BrokerPlugin} to allow or deny incoming JMS {@link Connection}s.
 * 
 * @author Robert Mischke
 */
public class ActiveMQConnectionFilterPlugin extends BrokerPluginSupport {

    private static final Pattern VM_CONNECTION_PATTERN = Pattern.compile("vm://.+#\\d+");

    private static final Pattern TCP_CONNECTION_PATTERN = Pattern.compile("tcp://(\\d{1,3}.\\d{1,3}.\\d{1,3}.\\d{1,3}):\\d{1,5}");

    private final Log log = LogFactory.getLog(getClass());

    private volatile ConnectionFilter filter = null; // will cause an exception if queried before setting

    @Override
    public void addConnection(ConnectionContext context, ConnectionInfo info) throws Exception {
        // note that the "IP" in the method name is misleading; this actually returns the connection URL - misc_ro
        String clientUrl = info.getClientIp();
        if (VM_CONNECTION_PATTERN.matcher(clientUrl).matches()) {
            // allow all in-JVM connections
            log.debug("Accepting in-JVM JMS broker connection " + clientUrl);
        } else {
            // attempt to match IPv4 connection URL
            Matcher m = TCP_CONNECTION_PATTERN.matcher(clientUrl);
            if (!m.matches()) {
                throw new CommunicationException("Connection refused: Malformed client URL (not a numeric IPv4 address): " + clientUrl);
            }
            String ipPart = m.group(1);
            // if matched, check the IP against the provided filter
            try {
                boolean accepted = filter.isIpAllowedToConnect(ipPart);
                if (accepted) {
                    log.debug("Accepting TCP JMS connection from " + ipPart);
                } else {
                    log.error("Refusing TCP JMS connection from " + ipPart);
                    throw new CommunicationException("Connection from " + ipPart + " refused by IP filter");
                }
            } catch (RuntimeException e) {
                throw new IllegalStateException("Error while checking filter for incoming IP " + ipPart, e);
            }
        }
        super.addConnection(context, info);
    }

    // overridden to suppress warnings caused by superclass - misc_ro
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Set getDestinations(ActiveMQDestination destination) {
        return super.getDestinations(destination);
    }

    public void setFilter(ConnectionFilter filter) {
        this.filter = filter;
    }
}
