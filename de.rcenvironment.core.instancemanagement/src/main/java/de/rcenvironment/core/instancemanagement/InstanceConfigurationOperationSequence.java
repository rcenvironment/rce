/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement;

import java.io.File;
import java.util.List;

import de.rcenvironment.core.instancemanagement.internal.SSHAccountParameters;
import de.rcenvironment.core.instancemanagement.internal.UplinkConnectionParameters;

/**
 * Provides a builder-style interface to construct sequences of steps to modify an instance's configuration.
 *
 * @author Robert Mischke
 * @author Brigitte Boden
 * @author Lukas Rosenbach
 */
public interface InstanceConfigurationOperationSequence {
    
    /**
     * Adds an operation to set the profile version of an instance. Only reasonable as the first change operation.
     * 
     * @param version the profile version
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence setProfileVersion(String version);

    /**
     * Adds an operation to reset the configuration to the empty default state. Only reasonable as the first change operation.
     * 
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence resetConfiguration();
    
    /**
     * Adds an operation to wipe the configuration to the empty default state. Only reasonable as the first change operation.
     * 
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence wipeConfiguration();

    /**
     * Adds an operation to reset the configuration to the given template. Only reasonable as the first change operation.
     * 
     * @param templateName the name/id of the template to apply
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence applyTemplate(String templateName);

    /**
     * Adds an operation to reset the configuration to the given template. Only reasonable as the first change operation.
     * 
     * @param templateFilePath the path of the configuration file to apply; if necessary, this could be changed to point to a directory in
     *        the future
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence applyTemplateFile(File templateFilePath);

    /**
     * Adds an operation to set the instance's name to the given value.
     * 
     * @param name the new value
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence setName(String name);

    /**
     * Adds an operation to set the configuration file's comment field to the given value.
     * 
     * @param comment the new value
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence setComment(String comment);

    /**
     * Adds an operation to set the "workflow host" flag to the given value.
     * 
     * @param value the new value
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence setWorkflowHostFlag(boolean value);

    /**
     * Adds an operation to set the "relay" flag to the given value.
     * 
     * @param value the new value
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence setRelayFlag(boolean value);

    /**
     * Adds an operation to add a custon network node id valud, which overrides any automatically generated and persisted node id. This
     * feature is usually only used for automated testing.
     * 
     * @param customNodeId the custom id value to set
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence setCustomNodeId(String customNodeId);

    /**
     * Adds an operation to configure the root temporary directory.
     * 
     * @param tempPath the root temporary directory path to set
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence setTempDirPath(String tempPath);

    /**
     * Adds an operation to add a network connection.
     * 
     * @param connectionName the connections's display name
     * @param host the host to connect to
     * @param port the port to connect to
     * @param autoConnect whether auto-connect on startup should be enabled
     * @param autoRetryInitialDelay the initial auto-retry delay, in seconds
     * @param autoRetryMaximumDelay the maximum auto-retry delay, in seconds
     * @param autoRetryDelayMultiplier the multiplier for the auto-retry delay after each failure
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence addNetworkConnection(String connectionName, String host, int port, boolean autoConnect,
        int autoRetryInitialDelay, int autoRetryMaximumDelay, float autoRetryDelayMultiplier);

    /**
     * Adds an operation to add a network connection. The individual parameters are parsed from a list of 7 string parameters.
     * 
     * @param parameters the list of string parameter tokens; see
     *        {@link #addNetworkConnection(String, String, int, boolean, long, long, float)} for their meaning and expected order
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence addNetworkConnectionFromStringParameters(List<String> parameters);

    /**
     * Adds an operation to remove a connection.
     * 
     * @param name the id of the connection
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence removeConnection(String name);

    /**
     * Adds an operation to add a server port for default connections.
     * 
     * @param serverPortName the id to assign to this sever port
     * @param serverPortIp the IP address to bind this port to
     * @param serverPortNumber the port number to bind this port to
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence addServerPort(String serverPortName, String serverPortIp, int serverPortNumber);

    /**
     * Adds an operation to toggle the IP filter (for standard connections) on or off.
     * 
     * @param ipFilterState the requested state (true = on)
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence setIpFilterEnabled(boolean ipFilterState);

    /**
     * Adds an operation to enable the embedded SSH server with the given IP and port.
     * 
     * @param ipAddress the IP address to bind the SSH server to
     * @param port the port to bind the SSH server to
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence enableSshServer(String ipAddress, int port);

    /**
     * Adds an operation to disable the embedded SSH server.
     * 
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence disableSshServer();
    
    /**
     * Adds an operation to add an SSH account to an embedded SSH server. The individual parameters are passed by a parameter object
     * 
     * @param parameters parameter object containing parameters
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence addSshAccount(SSHAccountParameters parameters);

    /**
     * Adds an operation to add an SSH account to an embedded SSH server.
     * 
     * @param parameters The list of parameters (username, role, enabled<true|false>, password)
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence addSshAccountFromStringParameters(List<String> parameters);
    
    /**
     * Adds an operation to remove an SSH account from an embedded SSH server.
     * 
     * @param username The name of the account to be removed.
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence removeSshAccount(String username);
    
    /**
     * Adds an operation to enable the embedded SSH server, and configure a reserved type of SSH account used to execute commands from the
     * IM master instance.
     * 
     * @param accessPort the port to bind the SSH server to
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence enableImSshAccess(int accessPort);

    /**
     * Adds an operation to set the request timeout.
     * 
     * @param timeout the value for the timeout.
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence setRequestTimeout(long timeout);

    /**
     * Adds an operation to set the forwarding timeout.
     * 
     * @param timeout the value for the timeout.
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence setForwardingTimeout(long timeout);

    /**
     * Adds an operation to add an allowed inbound IP.
     * 
     * @param ip the IP.
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence addAllowedInboundIP(String ip);

    /**
     * Adds an operation to remove an allowed inbound IP.
     * 
     * @param ip the IP.
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence removeAllowedInboundIP(String ip);

    /**
     * Adds an operation to add a SSH connection.
     * 
     * @param connectionName the connections's id
     * @param displayName the connections's display name
     * @param host the host to connect to
     * @param port the port to connect to
     * @param loginName the login name
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence addSshConnection(String connectionName, String displayName, String host, int port,
        String loginName);

    /**
     * Adds an operation to add a SSH connection. The individual parameters are parsed from a list of 5 string parameters.
     * 
     * @param parameters the list of string parameter tokens; see {@link #addSshConnection(String, String, String, int, String)} for their
     *        meaning and expected order
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence addSshConnectionFromStringParameters(List<String> parameters);

    /**
     * Adds an operation to remove a SSH connection.
     * 
     * @param name the id of the SSH connection
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence removeSshConnection(String name);
    
    /**
     * Adds an operation to add an uplink connection. The individual parameters are passed by a parameter object.
     * 
     * @param parameters parameter object containing parameters
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence addUplinkConnection(UplinkConnectionParameters parameters);
    
    /**
     * Adds an operation to add an uplink connection. The individual parameters are parsed from a list of string parameters.
     * 
     * @param parameters the list of string parameter tokens; in the format:
     *  <id> <hostname> <port> <clientid> <gateway> <connectOnStartup> <autoRetry> <user_name> password <password>
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence addUplinkConnectionFromStringParameters(List<String> parameters);

    /**
     * Adds an operation to remove an uplink connection.
     * 
     * @param id the id of the uplink connection
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence removeUplinkConnection(String id);

    
    /**
     * Adds an operation to publish a component.
     * 
     * @param name the name of the component
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence publishComponent(String name);

    /**
     * Adds an operation to unpublish a component.
     * 
     * @param name the name of the component
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence unpublishComponent(String name);

    /**
     * Adds an operation to set the background monitoring.
     * 
     * @param id indicates whether to log simple or more detailed monitoring data.
     * @param interval the update time.
     * @return the {@link InstanceConfigurationOperationSequence} instance itself (for command chaining)
     */
    InstanceConfigurationOperationSequence setBackgroundMonitoring(String id, int interval);

}
