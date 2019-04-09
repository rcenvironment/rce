/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
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

import de.rcenvironment.core.communication.sshconnection.InitialSshConnectionConfig;
import de.rcenvironment.core.configuration.ConfigurationSegment;

/**
 * Class providing the configuration for outgoing SSH connections.
 * 
 * @author Brigitte Boden
 */
public class SshConnectionsConfiguration {
    
    private List<InitialSshConnectionConfig> providedConnectionConfigs = new ArrayList<InitialSshConnectionConfig>();
    
    private final Log log = LogFactory.getLog(getClass());

    /**
     * Default constructor for bean mapping and tests.
     */
    public SshConnectionsConfiguration() {}

    public SshConnectionsConfiguration(ConfigurationSegment configuration) {
        Map<String, ConfigurationSegment> connectionElements = configuration.listElements("sshConnections");
        if (connectionElements != null) {
            for (Entry<String, ConfigurationSegment> entry : connectionElements.entrySet()) {
                ConfigurationSegment configPart = entry.getValue();
                String id = entry.getKey();
                InitialSshConnectionConfig connection;
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

    private InitialSshConnectionConfig parseConnectionEntry(ConfigurationSegment connectionPart) throws ConfigurationException {
        InitialSshConnectionConfig connection = new InitialSshConnectionConfig();
        connection.setHost(connectionPart.getString("host"));
        connection.setPort(connectionPart.getLong("port").intValue());
        connection.setUser(connectionPart.getString("loginName"));
        connection.setDisplayName(connectionPart.getString("displayName"));
        connection.setKeyFileLocation(connectionPart.getString("keyfileLocation"));
        connection.setUsePassphrase(!connectionPart.getBoolean("noPassphrase", false));
        connection.setConnectOnStartup(connectionPart.getBoolean("connectOnStartup", false));
        connection.setAutoRetry(connectionPart.getBoolean("autoRetry", false));
        return connection;
    }

    
    public List<InitialSshConnectionConfig> getProvidedConnectionConfigs() {
        return providedConnectionConfigs;
    }
    
}
