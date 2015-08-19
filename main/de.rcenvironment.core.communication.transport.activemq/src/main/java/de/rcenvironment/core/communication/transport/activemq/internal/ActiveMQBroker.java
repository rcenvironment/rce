/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.transport.activemq.internal;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.channel.ServerContactPoint;
import de.rcenvironment.core.communication.messaging.RawMessageChannelEndpointHandler;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.transport.jms.common.InitialInboxConsumer;
import de.rcenvironment.core.communication.transport.jms.common.JmsBroker;
import de.rcenvironment.core.communication.transport.jms.common.JmsProtocolConstants;
import de.rcenvironment.core.communication.transport.jms.common.JmsProtocolUtils;
import de.rcenvironment.core.communication.transport.jms.common.RemoteInitiatedMessageChannelFactory;
import de.rcenvironment.core.communication.transport.jms.common.RequestInboxConsumer;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.ThreadPool;

/**
 * ActiveMQ implementation of the common {@link JmsBroker} interface. It provides an embedded JMS broker for a given
 * {@link ServerContactPoint} that accepts incoming connections and creates matching remote-initiated ("passive") connections for them.
 * 
 * @author Robert Mischke
 */
public class ActiveMQBroker implements JmsBroker {

    private static final int SHUTDOWN_WAIT_AFTER_ANNOUNCE_MSEC = 1000;

    // this setting produces warnings if available disk space is less, so replace the default of 50gb - misc_ro
    // note: this value should have no effect as long as all JMS messages are non-persistent - misc_ro
    private static final long ACTIVEMQ_TEMPORARY_STORE_LIMIT = 50 * 1024 * 1024; // 50 mb (arbitrary)

    private final String brokerName;

    private final String externalUrl;

    private final String jvmLocalUrl;

    private BrokerService brokerService;

    private Connection localBrokerConnection;

    private final ServerContactPoint scp;

    private final RemoteInitiatedMessageChannelFactory remoteInitiatedConnectionFactory;

    private final ThreadPool threadPool = SharedThreadPool.getInstance();

    private final Log log = LogFactory.getLog(getClass());

    private int numRequestConsumers;

    private ActiveMQConnectionFilterPlugin connectionFilterPlugin;

    public ActiveMQBroker(ServerContactPoint scp, RemoteInitiatedMessageChannelFactory remoteInitiatedConnectionFactory) {
        this.scp = scp;
        this.remoteInitiatedConnectionFactory = remoteInitiatedConnectionFactory;
        NetworkContactPoint ncp = scp.getNetworkContactPoint();
        int port = ncp.getPort();
        String host = ncp.getHost();
        this.brokerName = "RCE_ActiveMQ_" + host + "_" + port;
        this.externalUrl = "tcp://" + host + ":" + port;
        this.jvmLocalUrl = "vm://" + brokerName;

        this.numRequestConsumers = 1;
        String property = System.getProperty("jms.numRequestConsumers");
        if (property != null) {
            try {
                numRequestConsumers = Integer.parseInt(property);
            } catch (NumberFormatException e) {
                log.warn("Ignoring invalid property value: " + property);
            }
        }
    }

    @Override
    public void start() throws Exception {
        connectionFilterPlugin = new ActiveMQConnectionFilterPlugin();
        connectionFilterPlugin.setFilter(scp.getConnectionFilter());
        brokerService = createTransientEmbeddedBroker(brokerName, connectionFilterPlugin, externalUrl, jvmLocalUrl);
        brokerService.start();

        ConnectionFactory localConnectionFactory = new ActiveMQConnectionFactory(jvmLocalUrl);
        localBrokerConnection = localConnectionFactory.createConnection();
        localBrokerConnection.setExceptionListener(new ExceptionListener() {

            @Override
            public void onException(JMSException exception) {
                handleAsyncJMSException(exception);
            }
        });
        localBrokerConnection.start();
        spawnInboxConsumers(getLocalConnection());
    }

    @Override
    public void stop() {
        try {
            Session shutdownSession = localBrokerConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            try {
                log.debug("Sending internal queue shutdown commands");
                String securityToken = "secToken"; // FIXME use proper token
                Message poisonPill = JmsProtocolUtils.createQueueShutdownMessage(shutdownSession, securityToken);
                MessageProducer producer = shutdownSession.createProducer(null);
                JmsProtocolUtils.configureMessageProducer(producer);
                producer.send(shutdownSession.createQueue(JmsProtocolConstants.QUEUE_NAME_INITIAL_BROKER_INBOX), poisonPill);
                for (int i = 0; i < numRequestConsumers; i++) {
                    producer.send(shutdownSession.createQueue(JmsProtocolConstants.QUEUE_NAME_C2B_REQUEST_INBOX), poisonPill);
                }
            } finally {
                shutdownSession.close();
            }
            Thread.sleep(SHUTDOWN_WAIT_AFTER_ANNOUNCE_MSEC);
        } catch (JMSException e) {
            log.error("Error while shutting down queue listeners", e);
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for queue shutdown", e);
        } finally {
            try {
                localBrokerConnection.close();
            } catch (JMSException e) {
                log.warn("Error closing local connection to broker " + brokerService.getBrokerName(), e);
            }
            // CHECKSTYLE:DISABLE (IllegalCatch) - ActiveMQ method declares "throws Exception"
            try {
                brokerService.stop();
                log.info("Stopped JMS broker " + brokerService.getBrokerName());
            } catch (Exception e) {
                log.warn("Error shutting down JMS broker " + brokerService.getBrokerName(), e);
            }
            // CHECKSTYLE:ENABLE (IllegalCatch)
        }
    }

    @Override
    public Connection getLocalConnection() {
        return localBrokerConnection;
    }

    private static BrokerService createTransientEmbeddedBroker(String brokerName, ActiveMQConnectionFilterPlugin filterPlugin,
        final String... urls) throws Exception {
        final BrokerService broker = new BrokerService();
        broker.setBrokerName(brokerName);
        broker.setPersistent(false);
        broker.setUseJmx(false); // default=true
        broker.getSystemUsage().getTempUsage().setLimit(ACTIVEMQ_TEMPORARY_STORE_LIMIT);
        // TODO ActiveMQ broker properties to set/evaluate:
        // - schedulePeriodForDestinationPurge
        // - inactiveTimoutBeforeGC
        // - timeBeforePurgeTempDestinations
        // ...
        broker.setPlugins(new BrokerPlugin[] { filterPlugin }); // note: must be set before connectors are added
        for (String url : urls) {
            broker.addConnector(url);
        }
        return broker;
    }

    private void spawnInboxConsumers(Connection connection) throws JMSException {
        RawMessageChannelEndpointHandler endpointHandler = scp.getEndpointHandler();
        log.debug("Spawning initial inbox consumer for SCP " + scp);
        threadPool.execute(new InitialInboxConsumer(connection, endpointHandler, scp, remoteInitiatedConnectionFactory));
        log.debug("Spawning " + numRequestConsumers + " request inbox consumer(s) for SCP " + scp);
        for (int i = 1; i <= numRequestConsumers; i++) {
            threadPool.execute(new RequestInboxConsumer(JmsProtocolConstants.QUEUE_NAME_C2B_REQUEST_INBOX, connection, endpointHandler),
                "Server request inbox consumer #" + i);
        }
    }

    private void handleAsyncJMSException(JMSException e) {
        String message = e.toString();
        if (message.contains("The destination temp-queue")) {
            // do not log full stack trace as it contains no useful information
            log.debug("Asynchronous JMS exception (usually a follow-up error of a broken connection): " + message);
        } else {
            log.warn("Asynchronous JMS exception in local broker connection", e);
        }
    }
}
