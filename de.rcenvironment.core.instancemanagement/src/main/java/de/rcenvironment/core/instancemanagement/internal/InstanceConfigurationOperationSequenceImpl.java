/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.rcenvironment.core.instancemanagement.InstanceConfigurationOperationSequence;
import de.rcenvironment.core.instancemanagement.InstanceManagementConstants;

/**
 * A builder for sequences of {@link InstanceConfigurationOperationDescriptor} instances that represent the individual changes to be
 * performed on a profile's configuration. (This class was introduced to wrap existing code into a cleaner API.)
 * 
 * @author Robert Mischke
 * @author Brigitte Boden
 * @author Lukas Rosenbach
 */
class InstanceConfigurationOperationSequenceImpl implements InstanceConfigurationOperationSequence {

    private final List<InstanceConfigurationOperationDescriptor> entries = new ArrayList<>();

    private boolean listExported;

    /**
     * Retrieves the collected list of configuration steps. Once the list has been fetched, any further attempts to add configuration steps
     * will fail with an {@link IllegalStateException}.
     * 
     * @return the list of configuration steps/entries
     */
    public synchronized List<InstanceConfigurationOperationDescriptor> getConfigurationSteps() {
        listExported = true;
        return entries;
    }

    @Override
    public InstanceConfigurationOperationSequence setProfileVersion(String version) {
        if (!entries.isEmpty()) {
            throw new IllegalStateException("The 'set profile version' operation can only be added as the first configuration step");
        }
        appendStep(new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SET_RCE_VERSION, version));
        return this;
    }

    @Override
    public InstanceConfigurationOperationSequence resetConfiguration() {
        if (!entries.isEmpty()) {
            throw new IllegalStateException("The 'reset' operation can only be added as the first configuration step");
        }
        appendStep(new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_RESET));
        return this;
    }

    @Override
    public InstanceConfigurationOperationSequence wipeConfiguration() {
        if (!entries.isEmpty()) {
            throw new IllegalStateException("The 'wipe' operation can only be added as the first configuration step");
        }
        appendStep(new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_WIPE));
        return this;
    }

    @Override
    public InstanceConfigurationOperationSequence applyTemplate(String templateName) {
        if (!entries.isEmpty()) {
            throw new IllegalStateException("The 'apply template' operation can only be added as the first configuration step");
        }
        appendStep(new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_APPLY_TEMPLATE, templateName));
        return this;
    }

    @Override
    public InstanceConfigurationOperationSequence applyTemplateFile(File templateFilePath) {
        if (!entries.isEmpty()) {
            throw new IllegalStateException("The 'apply template file' operation can only be added as the first configuration step");
        }
        appendStep(new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_APPLY_TEMPLATE, templateFilePath));
        return this;
    }

    @Override
    public InstanceConfigurationOperationSequence setName(String name) {
        appendStep(new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_SET_NAME, name));
        return this;
    }

    @Override
    public InstanceConfigurationOperationSequence setComment(String comment) {
        appendStep(new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_SET_COMMENT, comment));
        return this;
    }

    @Override
    public InstanceConfigurationOperationSequence setWorkflowHostFlag(boolean value) {
        appendStep(new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_SET_WORKFLOW_HOST_OPTION, value));
        return this;
    }

    @Override
    public InstanceConfigurationOperationSequence setRelayFlag(boolean value) {
        appendStep(new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_SET_RELAY_OPTION, value));
        return this;
    }

    @Override
    public InstanceConfigurationOperationSequence setCustomNodeId(String value) {
        appendStep(new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_SET_CUSTOM_NODE_ID, value));
        return this;
    }

    @Override
    public InstanceConfigurationOperationSequence setTempDirPath(String tempPath) {
        appendStep(new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_SET_TEMPDIR_PATH, tempPath));
        return this;
    }

    @Override
    public InstanceConfigurationOperationSequence addNetworkConnection(String connectionName, String host, int port,
        final boolean autoConnect, final int autoRetryInitialDelay, final int autoRetryMaximumDelay,
        final float autoRetryDelayMultiplier) {
        final ConfigurationConnection configConnection = new ConfigurationConnection(connectionName,
            host, port, autoConnect, autoRetryInitialDelay, autoRetryMaximumDelay, autoRetryDelayMultiplier);
        appendStep(new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_ADD_CONNECTION,
            configConnection));
        return this;
    }

    @Override
    public InstanceConfigurationOperationSequence addNetworkConnectionFromStringParameters(List<String> parameters) {
        if (parameters.size() != 7) {
            throw new IllegalArgumentException("Number of parameter elements must be 7");
        }
        final String connectionName = parameters.get(0);
        final String host = parameters.get(1);
        final int port = Integer.parseInt(parameters.get(2));
        final boolean autoConnect = Boolean.parseBoolean(parameters.get(3));
        final int autoRetryInitialDelay = Integer.parseInt(parameters.get(4));
        final int autoRetryMaximumDelay = Integer.parseInt(parameters.get(5));
        final float autoRetryDelayMultiplier = Float.parseFloat(parameters.get(6));

        // delegate to main method
        return this.addNetworkConnection(connectionName, host, port, autoConnect, autoRetryInitialDelay,
            autoRetryMaximumDelay, autoRetryDelayMultiplier);
    }

    @Override
    public InstanceConfigurationOperationSequence removeConnection(String name) {
        appendStep(new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_REMOVE_CONNECTION, name));
        return this;
    }

    @Override
    public InstanceConfigurationOperationSequence addServerPort(String serverPortName, String serverPortIp, int serverPortNumber) {
        appendStep(new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_ADD_SERVER_PORT, serverPortName,
            serverPortIp, serverPortNumber));
        return this;
    }

    @Override
    public InstanceConfigurationOperationSequence setIpFilterEnabled(boolean ipFilterState) {
        appendStep(new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_SET_IP_FILTER_OPTION,
            ipFilterState));
        return this;
    }

    @Override
    public InstanceConfigurationOperationSequence enableSshServer(String ipAddress, int port) {
        appendStep(new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_CONFIGURE_SSH_SERVER, ipAddress,
            port));
        return this;
    }

    @Override
    public InstanceConfigurationOperationSequence disableSshServer() {
        appendStep(new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_DISABLE_SSH_SERVER));
        return this;
    }
    
    @Override
    public InstanceConfigurationOperationSequence addSshAccount(SSHAccountParameters parameters) {
        appendStep(new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_ADD_SSH_ACCOUNT, 
            parameters.getUserName(), 
            parameters.getUserRole(),
            parameters.isEnabled(), 
            parameters.getPassword()));
        return this;
    }

    @Override
    public InstanceConfigurationOperationSequence addSshAccountFromStringParameters(List<String> parameters) {
        String username = parameters.get(0);
        String role = parameters.get(1);
        Boolean enabled = Boolean.parseBoolean(parameters.get(2));
        String password = parameters.get(3);
        appendStep(new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_ADD_SSH_ACCOUNT, username, role,
            enabled, password));
        return this;
    }

    @Override
    public InstanceConfigurationOperationSequence removeSshAccount(String username) {
        appendStep(new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_REMOVE_SSH_ACCOUNT, username));
        return this;
    }

    @Override
    public InstanceConfigurationOperationSequence enableImSshAccessWithDefaultRole(int accessPort) {
        appendStep(new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_ENABLE_IM_SSH_ACCESS, accessPort));
        return this;
    }

    @Override
    public InstanceConfigurationOperationSequence enableImSshAccessWithRole(int accessPort, String role) {
        appendStep(
            new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_ENABLE_IM_SSH_ACCESS, accessPort, role));
        return this;
    }

    @Override
    public InstanceConfigurationOperationSequence setRequestTimeout(long timeout) {
        appendStep(new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_SET_REQUEST_TIMEOUT, timeout));
        return this;
    }

    @Override
    public InstanceConfigurationOperationSequence setForwardingTimeout(long timeout) {
        appendStep(new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_SET_FORWARDING_TIMEOUT, timeout));
        return this;
    }

    private synchronized void appendStep(InstanceConfigurationOperationDescriptor step) {
        if (listExported) {
            throw new IllegalStateException();
        }
        entries.add(step);
    }

    @Override
    public InstanceConfigurationOperationSequence addAllowedInboundIP(String ip) {
        appendStep(new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_ADD_ALLOWED_INBOUND_IP, ip));
        return this;
    }

    @Override
    public InstanceConfigurationOperationSequence removeAllowedInboundIP(String ip) {
        appendStep(new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_REMOVE_ALLOWED_INBOUND_IP, ip));
        return this;
    }

    @Override
    public InstanceConfigurationOperationSequence addSshConnection(String connectionName, String displayName, String host, int port,
        String loginName) {
        final ConfigurationSshConnection configSshConnection = new ConfigurationSshConnection(connectionName,
            displayName, host, port, loginName);
        appendStep(new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_ADD_SSH_CONNECTION,
            configSshConnection));
        return this;
    }

    @Override
    public InstanceConfigurationOperationSequence addSshConnectionFromStringParameters(List<String> parameters) {
        if (parameters.size() != 5) {
            throw new IllegalArgumentException("Number of parameter elements must be 5");
        }
        final String connectionName = parameters.get(0);
        final String displayName = parameters.get(1);
        final String host = parameters.get(2);
        final int port = Integer.parseInt(parameters.get(3));
        final String loginName = parameters.get(4);

        // delegate to main method
        return this.addSshConnection(connectionName, displayName, host, port, loginName);
    }

    @Override
    public InstanceConfigurationOperationSequence removeSshConnection(String name) {
        appendStep(new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_REMOVE_SSH_CONNECTION, name));
        return this;
    }

    @Override
    public InstanceConfigurationOperationSequence addUplinkConnectionFromStringParameters(List<String> parameters) {
        // <id> <hostname> <port> <clientid> <gateway> <connectOnStartup> <autoRetry> <user_name> password <password>
        final String connectionId = parameters.get(0);
        final String hostName = parameters.get(1);
        final int port = Integer.parseInt(parameters.get(2));
        final String clientId = parameters.get(3);
        final boolean isGateway = Boolean.parseBoolean(parameters.get(4));
        final boolean connectOnStartup = Boolean.parseBoolean(parameters.get(5));
        final boolean autoRetry = Boolean.parseBoolean(parameters.get(6));
        final String userName = parameters.get(7);
        // TODO add support for keyfiles
        String password = null;
        if (parameters.get(8).equals("password")) {
            password = parameters.get(9);
        }
        final ConfigurationUplinkConnection uplinkConnection = new ConfigurationUplinkConnection(connectionId, hostName, port, userName,
            connectionId, clientId, null, connectOnStartup, autoRetry, isGateway, password);
        appendStep(
            new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_ADD_UPLINK_CONNECTION, uplinkConnection));
        return this;
    }

    @Override
    public InstanceConfigurationOperationSequence addUplinkConnection(UplinkConnectionParameters parameters) {
        final ConfigurationUplinkConnection uplinkConnection = new ConfigurationUplinkConnection(parameters.getConnectionId(),
            parameters.getHost(), parameters.getPort(), parameters.getUserName(), parameters.getConnectionId(), parameters.getClientId(),
            null, parameters.getAutoStartFlag(), parameters.getAutoRetryFlag(), parameters.getGatewayFlag(), parameters.getPassword());
        appendStep(
            new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_ADD_UPLINK_CONNECTION, uplinkConnection));
        return this;
    }

    @Override
    public InstanceConfigurationOperationSequence removeUplinkConnection(String id) {
        appendStep(new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_REMOVE_UPLINK_CONNECTION, id));
        return this;
    }

    @Override
    public InstanceConfigurationOperationSequence publishComponent(String name) {
        appendStep(new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_PUBLISH_COMPONENT, name));
        return this;
    }

    @Override
    public InstanceConfigurationOperationSequence unpublishComponent(String name) {
        appendStep(new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_UNPUBLISH_COMPONENT, name));
        return this;
    }

    @Override
    public InstanceConfigurationOperationSequence setBackgroundMonitoring(String id, int interval) {
        appendStep(new InstanceConfigurationOperationDescriptor(InstanceManagementConstants.SUBCOMMAND_SET_BACKGROUND_MONITORING, id,
            interval));
        return this;
    }

}
