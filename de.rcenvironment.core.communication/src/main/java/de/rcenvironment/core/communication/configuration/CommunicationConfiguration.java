/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.naming.ConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Class providing the configuration of the communication bundle. Additionally it defines the default configuration.
 * 
 * @author Frank Kautz
 * @author Doreen Seider
 * @author Tobias Menden
 * @author Robert Mischke
 */
public class CommunicationConfiguration {

    /**
     * The interval between connection health/liveliness checks.
     */
    public static final int CONNECTION_HEALTH_CHECK_INTERVAL_MSEC = 20 * 1000;

    /**
     * Defines the maximum random delay ("jitter") that is waited before each individual connection health check. This randomness serves to
     * avoid all connections being checked at once, and always in the same order. The interval for the random delay is [0;
     * CONNECTION_HEALTH_CHECK_MAX_JITTER_MSEC]. The maximum value should be smaller than (CONNECTION_HEALTH_CHECK_INTERVAL_MSEC -
     * CONNECTION_HEALTH_CHECK_TIMEOUT_MSEC) to avoid overlapping checks for the same connection.
     * 
     */
    public static final int CONNECTION_HEALTH_CHECK_MAX_JITTER_MSEC = 7 * 1000;

    /**
     * The maximum response time for an individual connection health check.
     */
    public static final int CONNECTION_HEALTH_CHECK_TIMEOUT_MSEC = 10 * 1000;

    /**
     * The number of consecutive health check failures before a connection is considered "broken".
     */
    public static final int CONNECTION_HEALTH_CHECK_FAILURE_LIMIT = 3;

    /**
     * Default request/response timeout on the sender side.
     */
    public static final int DEFAULT_REQUEST_TIMEOUT_MSEC = 40000;

    /**
     * Default timeout for waiting for the response while forwarding.
     */
    public static final int DEFAULT_FORWARDING_TIMEOUT_MSEC = 35000;

    private static final int MAX_VALID_PORT = 65535;

    private static final String COMMA = ",";

    private List<String> providedContactPoints = new ArrayList<String>();

    private List<String> remoteContactPoints = new ArrayList<String>();

    private int requestTimeoutMsec = DEFAULT_REQUEST_TIMEOUT_MSEC;

    private int forwardingTimeoutMsec = DEFAULT_FORWARDING_TIMEOUT_MSEC;

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Default constructor for bean mapping and tests.
     */
    public CommunicationConfiguration() {}

    public CommunicationConfiguration(ConfigurationSegment configuration) {
        requestTimeoutMsec = configuration.getLong("requestTimeoutMsec", (long) DEFAULT_REQUEST_TIMEOUT_MSEC).intValue();
        forwardingTimeoutMsec = configuration.getLong("forwardingTimeoutMsec", (long) DEFAULT_FORWARDING_TIMEOUT_MSEC).intValue();
        Map<String, ConfigurationSegment> connectionElements = configuration.listElements("connections");
        if (connectionElements != null) {
            for (Entry<String, ConfigurationSegment> entry : connectionElements.entrySet()) {
                ConfigurationSegment configPart = entry.getValue();
                String connection;
                try {
                    connection = parseConnectionEntry(configPart);
                    remoteContactPoints.add(connection);
                } catch (ConfigurationException e) {
                    // TODO >6.0.0: change to throw exception to outside?
                    log.error("Error in network connection entry \"" + entry.getKey() + "\": " + e.getMessage());
                }
            }
        }
        Map<String, ConfigurationSegment> serverPortElements = configuration.listElements("serverPorts");
        if (serverPortElements != null) {
            for (Entry<String, ConfigurationSegment> entry : serverPortElements.entrySet()) {
                ConfigurationSegment configPart = entry.getValue();
                String serverPort;
                try {
                    serverPort = parseServerPortEntry(configPart);
                    providedContactPoints.add(serverPort);
                } catch (ConfigurationException e) {
                    // TODO >6.0.0: change to throw exception to outside?
                    log.error("Error in server port entry " + entry.getKey() + ": " + e.getMessage());
                }
            }
        }
    }

    private String parseConnectionEntry(ConfigurationSegment connectionPart) throws ConfigurationException {
        // TODO mapping to old string approach to reduce code changes; improve later
        String host = connectionPart.getString("host");
        if (host == null) {
            throw new ConfigurationException("Missing required parameter \"host\"");
        }
        final Long portString = connectionPart.getLong("port");
        if (portString == null) {
            throw new ConfigurationException("Missing required parameter \"port\"");
        }
        int port = portString.intValue();
        StringBuilder options = new StringBuilder();
        Long autoRetryInitialDelay = connectionPart.getLong("autoRetryInitialDelay");
        if (autoRetryInitialDelay != null) {
            options.append("autoRetryInitialDelay=");
            options.append(autoRetryInitialDelay);
            options.append(COMMA);
        }
        Long autoRetryMaximumDelay = connectionPart.getLong("autoRetryMaximumDelay");
        if (autoRetryMaximumDelay != null) {
            options.append("autoRetryMaximumDelay=");
            options.append(autoRetryMaximumDelay);
            options.append(COMMA);
        }
        Double autoRetryDelayMultiplier = connectionPart.getDouble("autoRetryDelayMultiplier");
        if (autoRetryDelayMultiplier != null) {
            options.append("autoRetryDelayMultiplier=");
            options.append(autoRetryDelayMultiplier);
            options.append(COMMA);
        }
        boolean connectOnStartup = connectionPart.getBoolean("connectOnStartup", true);
        options.append("connectOnStartup=");
        options.append(Boolean.toString(connectOnStartup));
        options.append(COMMA);

        if (options.length() != 0) {
            options.setLength(options.length() - 1);
        }
        String connection = StringUtils.format("activemq-tcp:%s:%d(%s)", host, port, options.toString());
        return connection;
    }

    private String parseServerPortEntry(ConfigurationSegment connectionPart) throws ConfigurationException {
        // TODO mapping to old string approach to reduce code changes; improve later
        String ip = connectionPart.getString("ip");
        if (ip == null || ip.trim().isEmpty()) {
            throw new ConfigurationException("Missing or invalid \"ip\" parameter");
        }
        Long port = connectionPart.getLong("port");
        if (port == null || port < 1 || port > MAX_VALID_PORT) {
            throw new ConfigurationException("Missing or invalid \"port\" parameter");
        }
        return StringUtils.format("activemq-tcp:%s:%d", ip, port.intValue());
    }

    public List<String> getProvidedContactPoints() {
        return providedContactPoints;
    }

    public void setProvidedContactPoints(List<String> providedContactPoints) {
        this.providedContactPoints = providedContactPoints;
    }

    public List<String> getRemoteContactPoints() {
        return remoteContactPoints;
    }

    public void setRemoteContactPoints(List<String> remoteContactPoints) {
        this.remoteContactPoints = remoteContactPoints;
    }

    public int getRequestTimeoutMsec() {
        return requestTimeoutMsec;
    }

    public void setRequestTimeoutMsec(int requestTimeoutMsec) {
        this.requestTimeoutMsec = requestTimeoutMsec;
    }

    public int getForwardingTimeoutMsec() {
        return forwardingTimeoutMsec;
    }

    public void setForwardingTimeoutMsec(int forwardingTimeoutMsec) {
        this.forwardingTimeoutMsec = forwardingTimeoutMsec;
    }

}
