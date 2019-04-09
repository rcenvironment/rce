/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement.internal;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import de.rcenvironment.core.configuration.ConfigurationException;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.ConfigurationStoreFactory;
import de.rcenvironment.core.configuration.WritableConfigurationSegment;
import de.rcenvironment.core.configuration.internal.ConfigurationStore;
import de.rcenvironment.core.instancemanagement.InstanceManagementCommandPlugin;
import de.rcenvironment.core.instancemanagement.InstanceManagementConstants;
import de.rcenvironment.core.instancemanagement.internal.ConfigurationSegmentFactory.NetworkConnectionSegment;
import de.rcenvironment.core.instancemanagement.internal.ConfigurationSegmentFactory.SegmentBuilder;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Provides operations to configure the configuration.json via the {@link InstanceManagementCommandPlugin}.
 * 
 * @author David Scholz
 */
public class InstanceConfigurationImpl {

    private static final String ERROR_PATTERN = "Failed to %s";

    private final SegmentBuilder builder = ConfigurationSegmentFactory.getSegmentBuilder();

    private ConfigurationStore configStore;

    private ConfigurationSegment snapshot;

    public InstanceConfigurationImpl(final File config) throws InstanceConfigurationException {
        configStore = ConfigurationStoreFactory.getConfigurationStore(config);
        try {
            snapshot = configStore.getSnapshotOfRootSegment();
        } catch (IOException e) {
            throw new InstanceConfigurationException("Failed to create a snapshot of the configuration root segment.");
        }
    }

    /**
     * Sets a comment in the "general" section of the instance's configuration.
     * 
     * @param comment the comment to set.
     * @throws InstanceConfigurationException on failure.
     */
    public void setInstanceComment(String comment) throws InstanceConfigurationException {
        try {
            WritableConfigurationSegment writableSegment = snapshot.getOrCreateWritableSubSegment(builder.general().getPath());
            writableSegment.setString(builder.general().comment().getConfigurationKey(), comment);
        } catch (ConfigurationException e) {
            throw new InstanceConfigurationException(StringUtils.format(ERROR_PATTERN, "set a comment."), e);
        }
    }

    /**
     * 
     * Sets the desired instance name.
     * 
     * @param name the new name.
     * @throws InstanceConfigurationException on failure.
     */
    public void setInstanceName(String name) throws InstanceConfigurationException {
        try {
            WritableConfigurationSegment writableSegment =
                snapshot.getOrCreateWritableSubSegment(builder.general().getPath());
            writableSegment.setString(builder.general().instanceName().getConfigurationKey(), name);
        } catch (ConfigurationException e) {
            throw new InstanceConfigurationException(StringUtils.format(ERROR_PATTERN, "set an instance name."), e);
        }
    }

    /**
     * 
     * Sets the relay flag for a server instance.
     * 
     * @param isRelay <code>true</code> if the instance should be a relay, else <code>false</code>.
     * @throws InstanceConfigurationException on failure.
     */
    public void setRelayFlag(boolean isRelay) throws InstanceConfigurationException {
        try {
            WritableConfigurationSegment writableSegment = snapshot.getOrCreateWritableSubSegment(builder.general().getPath());
            writableSegment.setBoolean(builder.general().isRelay().getConfigurationKey(), isRelay);
        } catch (ConfigurationException e) {
            throw new InstanceConfigurationException(StringUtils.format(ERROR_PATTERN, "set the relay flag."), e);
        }
    }

    /**
     * 
     * Sets the workflow host flag for a server instance.
     * 
     * @param isWorkflowHost <code>true</code> if the instance should be the workflow host, else <code>false</code>.
     * @throws InstanceConfigurationException on failure.
     */
    public void setWorkflowHostFlag(boolean isWorkflowHost) throws InstanceConfigurationException {
        try {
            WritableConfigurationSegment writableSegment = snapshot.getOrCreateWritableSubSegment(builder.general().getPath());
            writableSegment.setBoolean(builder.general().isWorkflowHost().getConfigurationKey(), isWorkflowHost);
        } catch (ConfigurationException e) {
            throw new InstanceConfigurationException(StringUtils.format(ERROR_PATTERN, "set the workflow host flag."), e);
        }
    }

    /**
     * Sets a custom node id override value; typically used for automated testing.
     * 
     * @param customNodeId the node id to use
     * @throws InstanceConfigurationException on failure.
     */
    public void setCustomNodeId(String customNodeId) throws InstanceConfigurationException {
        try {
            WritableConfigurationSegment writableSegment = snapshot.getOrCreateWritableSubSegment(builder.network().getPath());
            // note: not using the unnecessarily complex builder approach for new code here
            writableSegment.setString("customNodeId", customNodeId);
        } catch (ConfigurationException e) {
            throw new InstanceConfigurationException(StringUtils.format(ERROR_PATTERN, "setting a custom node id"), e);
        }
    }

    /**
     * 
     * Sets the temp directory.
     * 
     * @param path the path.
     * @throws InstanceConfigurationException on failure.
     */
    public void setTempDirectory(String path) throws InstanceConfigurationException {
        try {
            WritableConfigurationSegment writableSegment = snapshot.getOrCreateWritableSubSegment(builder.general().getPath());
            writableSegment.setString(builder.general().tempDirectory().getConfigurationKey(), path);
        } catch (ConfigurationException e) {
            throw new InstanceConfigurationException(StringUtils.format(ERROR_PATTERN, "set the temp directory"), e);
        }
    }

    /**
     * 
     * Enables the ip filter for a server instance.
     * 
     * @param isFilterEnabled if the ip filter should be enabled.
     * @throws InstanceConfigurationException on failure.
     */
    public void setIpFilterFlag(boolean isFilterEnabled) throws InstanceConfigurationException {
        try {
            WritableConfigurationSegment writableSegment = snapshot.getOrCreateWritableSubSegment(builder.network().ipFilter().getPath());
            writableSegment.setBoolean(builder.network().ipFilter().enabled().getConfigurationKey(), isFilterEnabled);
        } catch (ConfigurationException e) {
            throw new InstanceConfigurationException(StringUtils.format(ERROR_PATTERN, "to set the ip filter flag for a server instance."),
                e);
        }
    }

    /**
     * 
     * Adds an IP-Adress to the filter.
     * 
     * @param ip the ip to add to the enabled filter.
     * @throws InstanceConfigurationException on failure.
     */
    public void addAllowedIp(String ip) throws InstanceConfigurationException {
        try {
            WritableConfigurationSegment writableSegment = snapshot.getOrCreateWritableSubSegment(builder.network().ipFilter().getPath());

            List<String> ipList = new LinkedList<>();
            for (String presentIP : writableSegment.getStringArray(builder.network().ipFilter().allowedIps().getConfigurationKey())) {
                if (ip.equals(presentIP)) {
                    continue;
                }
                ipList.add(presentIP);
            }
            ipList.add(ip);
            String[] array = new String[ipList.size()];
            array = ipList.toArray(array);
            writableSegment.setStringArray(builder.network().ipFilter().allowedIps().getConfigurationKey(), array);
        } catch (ConfigurationException e) {
            throw new InstanceConfigurationException(StringUtils.format(ERROR_PATTERN, "to add an allowed ip adress to the ip filter."), e);
        }
    }

    /**
     * 
     * Removes an IP-Adress from the filter.
     * 
     * @param ip the ip to remove from the filter.
     * @throws InstanceConfigurationException on failure.
     */
    public void removeAllowedIp(String ip) throws InstanceConfigurationException {
        try {
            WritableConfigurationSegment writableSegment = snapshot.getOrCreateWritableSubSegment(builder.network().ipFilter().getPath());
            List<String> ipList = writableSegment.getStringArray(builder.network().ipFilter().allowedIps().getConfigurationKey());
            for (String s : ipList) {
                if (s.equals(ip)) {
                    ipList.remove(ip);
                    String[] ipArray = new String[ipList.size()];
                    ipArray = ipList.toArray(ipArray);
                    writableSegment.setStringArray(builder.network().ipFilter().allowedIps().getConfigurationKey(), ipArray);
                    return;
                }
            }
            throw new InstanceConfigurationException("Couldn't remove ip: " + ip + " as it isn't present in the current configuration.");
        } catch (ConfigurationException e) {
            throw new InstanceConfigurationException(StringUtils.format(ERROR_PATTERN, "to remove an allowed ip adress to the ip filter."),
                e);
        }
    }

    /**
     * 
     * Set background system monitoring.
     * 
     * @param interval the update time.
     * @param id indicates whether to log simple or more detailed monitoring data.
     * @throws InstanceConfigurationException on failure.
     */
    public void setBackgroundMonitoring(String id, int interval) throws InstanceConfigurationException {
        try {
            WritableConfigurationSegment writableSegment = snapshot.getOrCreateWritableSubSegment(builder.backgroundMonitoring().getPath());
            writableSegment.setString(builder.backgroundMonitoring().enableIds().getConfigurationKey(), id);
            writableSegment.setInteger(builder.backgroundMonitoring().intervalSeconds().getConfigurationKey(), interval);
        } catch (ConfigurationException e) {
            throw new InstanceConfigurationException(StringUtils.format(ERROR_PATTERN, "to set background monitoring service."), e);
        }

    }

    /**
     * 
     * Adds a new connection.
     * 
     * @param connection the connection to add.
     * @throws InstanceConfigurationException on failure.
     */
    public void addConnection(ConfigurationConnection connection) throws InstanceConfigurationException {
        try {
            WritableConfigurationSegment writableSegment =
                snapshot.getOrCreateWritableSubSegment(builder.network().connections().getPath());
            ConfigurationSegment subsegment = writableSegment.getSubSegment(connection.getConnectionName());
            WritableConfigurationSegment newConnectionSegment;
            if (!subsegment.isPresentInCurrentConfiguration()) {
                newConnectionSegment = writableSegment.createElement(connection.getConnectionName());
            } else {
                newConnectionSegment = writableSegment.getOrCreateWritableSubSegment(connection.getConnectionName());
            }
            setConnectionFields(connection, newConnectionSegment);
        } catch (ConfigurationException e) {
            throw new InstanceConfigurationException(StringUtils.format(ERROR_PATTERN, "to add new connection."), e);
        }
    }

    private void setConnectionFields(ConfigurationConnection connection, WritableConfigurationSegment segment)
        throws ConfigurationException {
        final NetworkConnectionSegment connectionSegment =
            builder.network().connections().getOrCreateConnection(connection.getConnectionName());
        segment.setString(connectionSegment.host().getConfigurationKey(), connection.getHost());
        segment.setInteger(connectionSegment.port().getConfigurationKey(), connection.getPort());
        segment.setBoolean(connectionSegment.connectOnStartup().getConfigurationKey(), connection.getConnectOnStartup());
        segment.setLong(connectionSegment.autoRetryInitialDelay().getConfigurationKey(), connection.getAutoRetryInitialDelay());
        segment.setLong(connectionSegment.autoRetryMaximumDelay().getConfigurationKey(), connection.getAutoRetryMaximumDelay());
        segment.setFloat(connectionSegment.autoRetryDelayMultiplier().getConfigurationKey(), connection.getAutoRetryDelayMultiplier());
    }

    /**
     * 
     * Removes a connection.
     * 
     * @param connection the connection to remove.
     * @throws InstanceConfigurationException on failure.
     */
    public void removeConnection(String connection) throws InstanceConfigurationException {
        try {
            WritableConfigurationSegment writableSegment =
                snapshot.getOrCreateWritableSubSegment(builder.network().connections().getPath());
            ConfigurationSegment subsegment = writableSegment.getSubSegment(connection);
            if (!subsegment.isPresentInCurrentConfiguration()) {
                return;
            }
            boolean success = writableSegment.deleteElement(connection);
            if (!success) {
                throw new InstanceConfigurationException("Failed to delete connection with name: " + connection);
            }
        } catch (ConfigurationException e) {
            throw new InstanceConfigurationException(StringUtils.format(ERROR_PATTERN, "to remove connection: " + connection), e);
        }
    }

    /**
     * 
     * Sets the request timeout.
     * 
     * @param timeout the timeout.
     * @throws InstanceConfigurationException on failure.
     */
    public void setRequestTimeout(long timeout) throws InstanceConfigurationException {
        try {
            WritableConfigurationSegment writableSegment = snapshot.getOrCreateWritableSubSegment(builder.network().getPath());
            writableSegment.setLong(builder.network().requestTimeoutMsec().getConfigurationKey(), timeout);
        } catch (ConfigurationException e) {
            throw new InstanceConfigurationException(StringUtils.format(ERROR_PATTERN, "to set request timeout."));
        }
    }

    /**
     * 
     * Sets the forwarding timeout.
     * 
     * @param timeout the timeout.
     * @throws InstanceConfigurationException on failure.
     */
    public void setForwardingTimeout(long timeout) throws InstanceConfigurationException {
        try {
            WritableConfigurationSegment writableSegment = snapshot.getOrCreateWritableSubSegment(builder.network().getPath());
            writableSegment.setLong(builder.network().forwardingTimeoutMsec().getConfigurationKey(), timeout);
        } catch (ConfigurationException e) {
            throw new InstanceConfigurationException(StringUtils.format(ERROR_PATTERN, "to set forwarding timeout."));
        }
    }

    /**
     * 
     * Adds a new server port.
     * 
     * @param portName the name of the new port.
     * @param ip the ip adress.
     * @param port the port.
     * @throws InstanceConfigurationException on failure.
     */
    public void addServerPort(String portName, String ip, Integer port) throws InstanceConfigurationException {
        try {
            WritableConfigurationSegment writableSegment = snapshot.getOrCreateWritableSubSegment(builder.network().ports().getPath());
            WritableConfigurationSegment newPort = writableSegment.createElement(portName);
            newPort.setString(builder.network().ports().getOrCreateServerPort(portName).ip().getConfigurationKey(), ip);
            newPort.setInteger(builder.network().ports().getOrCreateServerPort(portName).port().getConfigurationKey(), port);
        } catch (ConfigurationException e) {
            throw new InstanceConfigurationException(StringUtils.format(ERROR_PATTERN, "to add server ports"));
        }
    }

    /**
     * 
     * Adds a ssh connection.
     * 
     * @param sshConnection the connection object.
     * @throws InstanceConfigurationException on failure.
     */
    public void addSshConnection(ConfigurationSshConnection sshConnection) throws InstanceConfigurationException {
        try {
            WritableConfigurationSegment writableSegment =
                snapshot.getOrCreateWritableSubSegment(builder.sshRemoteAccess().sshConnections().getPath());
            ConfigurationSegment subSegment = writableSegment.getSubSegment(sshConnection.getName());
            WritableConfigurationSegment newSegment;
            if (!subSegment.isPresentInCurrentConfiguration()) {
                newSegment = writableSegment.createElement(sshConnection.getName());
            } else {
                newSegment = writableSegment.getOrCreateWritableSubSegment(sshConnection.getName());
            }
            newSegment.setString(builder.sshRemoteAccess().sshConnections().getOrCreateSshConnection(sshConnection.getLoginName())
                .displayName()
                .getConfigurationKey(), sshConnection.getDisplayName());
            newSegment.setString(builder.sshRemoteAccess().sshConnections().getOrCreateSshConnection(sshConnection.getLoginName()).host()
                .getConfigurationKey(), sshConnection.getHost());
            newSegment.setInteger(builder.sshRemoteAccess().sshConnections().getOrCreateSshConnection(sshConnection.getLoginName()).port()
                .getConfigurationKey(), sshConnection.getPort());
            newSegment.setString(builder.sshRemoteAccess().sshConnections().getOrCreateSshConnection(sshConnection.getLoginName())
                .loginName()
                .getConfigurationKey(), sshConnection.getLoginName());
        } catch (ConfigurationException e) {
            throw new InstanceConfigurationException(StringUtils.format(ERROR_PATTERN, "to add new ssh connection."), e);
        }
    }

    /**
     * 
     * Removes a ssh connection.
     * 
     * @param configName the name of the ssh connection in the configuration to remove.
     * @throws InstanceConfigurationException on failure.
     */
    public void removeSshConnection(String configName) throws InstanceConfigurationException {
        try {
            WritableConfigurationSegment writableSegment =
                snapshot.getOrCreateWritableSubSegment(builder.sshRemoteAccess().sshConnections().getPath());
            ConfigurationSegment toRemove = writableSegment.getSubSegment(configName);
            if (!toRemove.isPresentInCurrentConfiguration()) {
                return;
            }
            boolean success = writableSegment.deleteElement(configName);
            if (!success) {
                throw new InstanceConfigurationException(StringUtils.format(ERROR_PATTERN, "to remove ssh connection: " + configName));
            }
        } catch (ConfigurationException e) {
            throw new InstanceConfigurationException(StringUtils.format(ERROR_PATTERN, "to remove ssh connection: " + configName));
        }
    }

    /**
     * 
     * Disables ssh server.
     * 
     * @throws InstanceConfigurationException on failure.
     */
    public void disableSshServer() throws InstanceConfigurationException {
        try {
            WritableConfigurationSegment writableSegment = snapshot.getOrCreateWritableSubSegment(builder.sshServer().getPath());
            writableSegment.setBoolean(builder.sshServer().enabled().getConfigurationKey(), false);
        } catch (ConfigurationException e) {
            throw new InstanceConfigurationException(StringUtils.format(ERROR_PATTERN, "to disable ssh server."));
        }
    }

    /**
     * 
     * Enables ssh server.
     * 
     * @throws InstanceConfigurationException on failure.
     */
    public void enableSshServer() throws InstanceConfigurationException {
        try {
            WritableConfigurationSegment writableSegment = snapshot.getOrCreateWritableSubSegment(builder.sshServer().getPath());
            writableSegment.setBoolean(builder.sshServer().enabled().getConfigurationKey(), true);
        } catch (ConfigurationException e) {
            throw new InstanceConfigurationException(StringUtils.format(ERROR_PATTERN, "to enable ssh server."));
        }
    }

    /**
     * 
     * Sets the ssh server ip adress.
     * 
     * @param ip the desired ip to set.
     * @throws InstanceConfigurationException on failure.
     */
    public void setSshServerIP(String ip) throws InstanceConfigurationException {
        try {
            WritableConfigurationSegment writableSegment = snapshot.getOrCreateWritableSubSegment(builder.sshServer().getPath());
            writableSegment.setString(builder.sshServer().ip().getConfigurationKey(), ip);
        } catch (ConfigurationException e) {
            throw new InstanceConfigurationException(StringUtils.format(ERROR_PATTERN, "to set ssh server ip adress."));
        }
    }

    /**
     * 
     * Sets the ssh server port.
     * 
     * @param port the desired port to set.
     * @throws InstanceConfigurationException on failure.
     */
    public void setSshServerPort(int port) throws InstanceConfigurationException {
        try {
            WritableConfigurationSegment writableSegment = snapshot.getOrCreateWritableSubSegment(builder.sshServer().getPath());
            writableSegment.setInteger(builder.sshServer().port().getConfigurationKey(), port);
        } catch (ConfigurationException e) {
            throw new InstanceConfigurationException(StringUtils.format(ERROR_PATTERN, "to set ssh server port."));
        }
    }

    /**
     * Enabled SSH access for the IM master.
     * 
     * @param port the desired port to set.
     * @param passphrase the passphrase for the account.
     * @throws InstanceConfigurationException on failure.
     */
    public void enableImSshAccess(int port, String passphrase) throws InstanceConfigurationException {
        enableSshServer();
        if (getSshServerIp() == null) {
            setSshServerIP(InstanceManagementConstants.LOCALHOST);
        }
        if (getSshServerPort() == null) {
            setSshServerPort(port);
        }
        try {
            // Create account; note that this makes use of the predefined role introduced in 8.0.0, so it will not work for 7.x.x
            WritableConfigurationSegment writableAccountsSegment =
                snapshot.getOrCreateWritableSubSegment(builder.sshServer().getSshAccounts().getPath());
            ConfigurationSegment accountSegment = writableAccountsSegment.getSubSegment(InstanceManagementConstants.IM_MASTER_USER_NAME);
            WritableConfigurationSegment newSegment;
            if (!accountSegment.isPresentInCurrentConfiguration()) {
                newSegment = writableAccountsSegment.createElement(InstanceManagementConstants.IM_MASTER_USER_NAME);
            } else {
                newSegment = writableAccountsSegment.getOrCreateWritableSubSegment(InstanceManagementConstants.IM_MASTER_USER_NAME);
            }
            newSegment.setString(builder.sshServer().getSshAccounts()
                .getOrCreateSshAccount(InstanceManagementConstants.IM_MASTER_USER_NAME).role().getConfigurationKey(),
                InstanceManagementConstants.IM_MASTER_ROLE);
            newSegment.setBoolean(builder.sshServer().getSshAccounts()
                .getOrCreateSshAccount(InstanceManagementConstants.IM_MASTER_USER_NAME).enabled().getConfigurationKey(),
                true);
            newSegment.setString(builder.sshServer().getSshAccounts()
                .getOrCreateSshAccount(InstanceManagementConstants.IM_MASTER_USER_NAME).passwordHash().getConfigurationKey(),
                passphrase);
        } catch (ConfigurationException e) {
            throw new InstanceConfigurationException(StringUtils.format(ERROR_PATTERN, "to add IM master account."));
        }
    }

    /**
     * 
     * Removes server port.
     * 
     * @param name port to remove.
     * @throws InstanceConfigurationException on failure.
     */
    public void removeServerPort(String name) throws InstanceConfigurationException {
        try {
            WritableConfigurationSegment writableSegment = snapshot.getOrCreateWritableSubSegment(builder.network().ports().getPath());
            boolean success = writableSegment.deleteElement(builder.network().ports().getOrCreateServerPort(name).getPath());
            if (!success) {
                throw new InstanceConfigurationException("Failed to remove server port :" + name);
            }
        } catch (ConfigurationException e) {
            throw new InstanceConfigurationException(StringUtils.format(ERROR_PATTERN, "to remove server port: " + name));
        }
    }

    /**
     * 
     * Publishes new component.
     * 
     * @param name the component name.
     * @throws InstanceConfigurationException on failure.
     */
    public void publishComponent(String name) throws InstanceConfigurationException {
        try {
            WritableConfigurationSegment writableSegment =
                snapshot.getOrCreateWritableSubSegment(builder.publishing().getPath());
            List<String> componentList = new LinkedList<>();
            for (String component : writableSegment.getStringArray(builder.publishing().components().getConfigurationKey())) {
                if (component.equals(name)) {
                    continue;
                }
                componentList.add(component);
            }
            componentList.add(name);
            String[] array = new String[componentList.size()];
            array = componentList.toArray(array);
            writableSegment.setStringArray(builder.publishing().components().getConfigurationKey(), array);
        } catch (ConfigurationException e) {
            throw new InstanceConfigurationException(StringUtils.format(ERROR_PATTERN, "to publish new component."), e);
        }
    }

    /**
     * 
     * Removes component from the publishing list.
     * 
     * @param name the component name to remove.
     * @throws InstanceConfigurationException on failure.
     */
    public void unPublishComponent(String name) throws InstanceConfigurationException {
        try {
            WritableConfigurationSegment writableSegment = snapshot.getOrCreateWritableSubSegment(builder.publishing().getPath());
            List<String> ipList = writableSegment.getStringArray(builder.publishing().components().getConfigurationKey());
            for (String s : ipList) {
                if (s.equals(name)) {
                    ipList.remove(name);
                    String[] componentArray = new String[ipList.size()];
                    componentArray = ipList.toArray(componentArray);
                    writableSegment.setStringArray(builder.publishing().components().getConfigurationKey(), componentArray);
                    return;
                }
            }
            throw new InstanceConfigurationException(
                "Couldn't unpublish component: " + name + " as it isn't present in the current configuration.");
        } catch (ConfigurationException e) {
            throw new InstanceConfigurationException(StringUtils.format(ERROR_PATTERN, "to unpublish component " + name + "."), e);
        }
    }

    /**
     * Retreives the configured server port from the configuration file.
     * 
     * @return the server port, or null, if none is configured.
     * @throws InstanceConfigurationException on failure.
     */
    public Integer getSshServerPort() throws InstanceConfigurationException {
        ConfigurationSegment segment = snapshot.getSubSegment(builder.sshServer().getPath());
        return segment.getInteger(builder.sshServer().port().getConfigurationKey());
    }

    /**
     * Retreives the configured server IP from the configuration file.
     * 
     * @return the server ip, or null, if none is configured.
     * @throws InstanceConfigurationException on failure.
     */
    public String getSshServerIp() throws InstanceConfigurationException {
        ConfigurationSegment segment = snapshot.getSubSegment(builder.sshServer().getPath());
        return segment.getString(builder.sshServer().ip().getConfigurationKey());
    }

    /**
     * 
     * Updates snapshot.
     * 
     * @throws InstanceConfigurationException on failure.
     */
    public void update() throws InstanceConfigurationException {
        try {
            configStore.update(snapshot);
        } catch (ConfigurationException | IOException e) {
            throw new InstanceConfigurationException("Failed to update configuration.");
        }
    }

}
