/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.transport.jms.activemq.internal;

import java.util.concurrent.atomic.AtomicInteger;

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
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.transport.jms.common.InitialInboxConsumer;
import de.rcenvironment.core.communication.transport.jms.common.JmsBroker;
import de.rcenvironment.core.communication.transport.jms.common.JmsProtocolConstants;
import de.rcenvironment.core.communication.transport.jms.common.JmsProtocolUtils;
import de.rcenvironment.core.communication.transport.jms.common.RemoteInitiatedMessageChannelFactory;
import de.rcenvironment.core.communication.transport.jms.common.RequestInboxConsumer;
import de.rcenvironment.core.communication.transport.spi.MessageChannelEndpointHandler;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;

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

    private static final AtomicInteger sharedInboxConsumerIdGenerator = new AtomicInteger();

    private final String brokerName;

    private final String externalUrl;

    private final String jvmLocalUrl;

    private BrokerService brokerService;

    private Connection localBrokerConnection;

    private final ServerContactPoint scp;

    private final RemoteInitiatedMessageChannelFactory remoteInitiatedConnectionFactory;

    private int numRequestConsumers;

    private ActiveMQConnectionFilterPlugin connectionFilterPlugin;

    private final AsyncTaskService threadPool = ConcurrencyUtils.getAsyncTaskService();

    private final Log log = LogFactory.getLog(getClass());

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
        log.info(StringUtils.format("Listening for standard connections on %s:%d", scp.getNetworkContactPoint().getHost(),
            scp.getNetworkContactPoint().getPort()));

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
                MessageProducer producer = shutdownSession.createProducer(null); // disposed in finally block (as part of the session)
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
                log.info("Shutting down server port " + scp.getNetworkContactPoint().getPort());
                brokerService.stop();
                log.debug("Stopped JMS broker " + brokerService.getBrokerName());
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
        log.debug("Spawning initial inbox consumer for " + scp.toString());
        threadPool.execute(new InitialInboxConsumer(connection, scp, remoteInitiatedConnectionFactory));

        log.debug("Spawning " + numRequestConsumers + " request inbox consumer(s) for " + scp);
        final MessageChannelEndpointHandler endpointHandler = scp.getEndpointHandler();
        for (int i = 1; i <= numRequestConsumers; i++) {
            threadPool.execute(
                new RequestInboxConsumer(JmsProtocolConstants.QUEUE_NAME_C2B_REQUEST_INBOX, connection, endpointHandler),
                StringUtils.format("Shared C2B Request Inbox Consumer #%d (worker #%d for %s')",
                    sharedInboxConsumerIdGenerator.incrementAndGet(), i, scp.toString()));
        }
    }

    private void handleAsyncJMSException(JMSException e) {
        final String exceptionString = e.toString();
        final boolean isKnownHarmlessMessage = exceptionString.contains("The destination temp-queue")
            || exceptionString.contains("Cannot remove session that had not been registered");
        if (isKnownHarmlessMessage) {
            // do not log full stack trace as it contains no useful information
            log.debug("Asynchronous JMS exception (usually a follow-up error of a broken connection): " + exceptionString);
        } else {
            log.warn("Asynchronous JMS exception in local broker connection", e);
        }
    }
}
