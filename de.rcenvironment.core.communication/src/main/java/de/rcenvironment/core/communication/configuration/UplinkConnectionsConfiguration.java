/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.naming.ConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.sshconnection.InitialUplinkConnectionConfig;
import de.rcenvironment.core.configuration.ConfigurationSegment;

/**
 * Class providing the configuration for outgoing SSH connections.
 * 
 * @author Brigitte Boden
 */
public class UplinkConnectionsConfiguration {
    
    private List<InitialUplinkConnectionConfig> providedConnectionConfigs = new ArrayList<InitialUplinkConnectionConfig>();
    
    private final Log log = LogFactory.getLog(getClass());

    /**
     * Default constructor for bean mapping and tests.
     */
    public UplinkConnectionsConfiguration() {}

    public UplinkConnectionsConfiguration(ConfigurationSegment configuration) {
        Map<String, ConfigurationSegment> connectionElements = configuration.listElements("uplinkConnections");
        if (connectionElements != null) {
            for (Entry<String, ConfigurationSegment> entry : connectionElements.entrySet()) {
                ConfigurationSegment configPart = entry.getValue();
                String id = entry.getKey();

                InitialUplinkConnectionConfig connection;
                try {
                    connection = parseConnectionEntry(configPart);
                    connection.setId(id);
                    providedConnectionConfigs.add(connection);
                } catch (ConfigurationException e) {
                    log.error("Error in connection entry " + entry.getKey(), e);
                }
            }
        }
    }


    private InitialUplinkConnectionConfig parseConnectionEntry(ConfigurationSegment connectionPart) throws ConfigurationException {
        InitialUplinkConnectionConfig connection = new InitialUplinkConnectionConfig();
        connection.setHost(connectionPart.getString("host"));
        connection.setPort(connectionPart.getLong("port").intValue());
        connection.setUser(connectionPart.getString("loginName"));
        connection.setDisplayName(connectionPart.getString("displayName"));
        connection.setQualifier(connectionPart.getString("clientID", "default"));
        connection.setKeyFileLocation(connectionPart.getString("keyfileLocation"));
        connection.setUsePassphrase(!connectionPart.getBoolean("noPassphrase", false));
        connection.setConnectOnStartup(connectionPart.getBoolean("connectOnStartup", false));
        connection.setAutoRetry(connectionPart.getBoolean("autoRetry", false));
        connection.setIsGateway(connectionPart.getBoolean("isGateway", false));
        return connection;
    }

    
    public List<InitialUplinkConnectionConfig> getProvidedConnectionConfigs() {
        return providedConnectionConfigs;
    }
    
}
