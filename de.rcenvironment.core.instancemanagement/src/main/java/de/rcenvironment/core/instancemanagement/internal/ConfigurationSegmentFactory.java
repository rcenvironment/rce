/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * Provide configuration segments of the configuration.json file for easier configuration handling.
 *
 * @author David Scholz
 */
public final class ConfigurationSegmentFactory {

    private static final String PORT = "port";

    private static final String HOST = "host";

    private static final String IP = "ip";

    private static final String ENABLED = "enabled";

    private ConfigurationSegmentFactory() {

    }

    public static SegmentBuilder getSegmentBuilder() {
        return new ConfigurationSegmentFactory().new SegmentBuilder();
    }

    /**
     * 
     * Represents a config attribute.
     *
     * @author David Scholz
     */
    public interface ConfigurationKey {

        /**
         * 
         * Get a string representation of a key mapped to a value in the configuration.json.
         * 
         * @return the key a configuration.json mapping.
         */
        String getConfigurationKey();
    }

    /**
     * 
     * Represents a config segment.
     *
     * @author David Scholz
     */
    public interface Segment {

        /**
         * 
         * Get a string representation of a name of segment in the configuration.json. A {@link Segment} can have other {@link Segment}s or
         * {@link ConfigurationKey}s.
         * 
         * @return name of a segment.
         */
        String getPath();
    }

    /**
     * 
     * Container for concrete {@link Segment}s to avoid object recreation (immutable).
     *
     * @author David Scholz
     */
    private final class SegmentContainer {

        private final GeneralSegment generalSegment;

        private final BackgroundMonitoringSegment backgroundMonitoringSegment;

        private final NetworkSegment network;

        private final SshRemoteAccessSegment remoteAccess;

        private final UplinkSegment uplink;

        private final PublishingSegment publishing;

        private final AuthorizationSegment authorization;

        private final ComponentSettingsSegment componentSettings;

        private final SshServerSegment sshServer;

        SegmentContainer() {
            generalSegment = new GeneralSegment();
            backgroundMonitoringSegment = new BackgroundMonitoringSegment();
            network = new NetworkSegment();
            publishing = new PublishingSegment();
            authorization = new AuthorizationSegment();
            remoteAccess = new SshRemoteAccessSegment();
            uplink = new UplinkSegment();
            componentSettings = new ComponentSettingsSegment();
            sshServer = new SshServerSegment();
        }

        public GeneralSegment getGeneralSegment() {
            return generalSegment;
        }

        public BackgroundMonitoringSegment getBackgroundMonitoringSegment() {
            return backgroundMonitoringSegment;
        }

        public NetworkSegment getNetworkSegment() {
            return network;
        }

        public PublishingSegment getPublishingSegment() {
            return publishing;
        }

        public AuthorizationSegment getAuthorizationSegment() {
            return authorization;
        }

        public SshRemoteAccessSegment getRemoteAccess() {
            return remoteAccess;
        }
        
        public UplinkSegment getUplink() {
            return uplink;
        }

        public ComponentSettingsSegment getComponentSettings() {
            return componentSettings;
        }

        public SshServerSegment getSshServer() {
            return sshServer;
        }
    }

    /**
     * According to the builder-pattern and a fluent interface mechanism (builds {@link ConfigurationStringSegment}). Getting the correct
     * implementation of {@link Segment} is done by the visitor pattern.
     * 
     * TODO in my opinion, this approach is overly complicated; it seems that much of this could be replaced with simple constants. this
     * should be reviewed before extending it for new use cases -- misc_ro, Aug 2017
     *
     * @author David Scholz
     */
    public class SegmentBuilder {

        private final SegmentContainer container = new SegmentContainer();

        public SegmentBuilder() {

        }

        /**
         * 
         * Get general segment.
         * 
         * @return general segment.
         */
        public GeneralSegment general() {
            return container.getGeneralSegment();
        }

        /**
         * 
         * Get background monitoring segment.
         * 
         * @return background monitoring segment.
         */
        public BackgroundMonitoringSegment backgroundMonitoring() {
            return container.getBackgroundMonitoringSegment();
        }

        /**
         * 
         * Get network monitoring segment.
         * 
         * @return network monitoring segment.
         */
        public NetworkSegment network() {
            return container.getNetworkSegment();
        }

        /**
         * 
         * Get ssh remote access segment.
         * 
         * @return ssh remote access segment.
         */
        public SshRemoteAccessSegment sshRemoteAccess() {
            return container.getRemoteAccess();
        }
        
        /**
         * @return the uplink segment.
         */
        public UplinkSegment uplink() {
            return container.getUplink();
        }

        /**
         * 
         * Get publishing segment.
         * 
         * @return publishing segment.
         */
        public PublishingSegment publishing() {
            return container.getPublishingSegment();
        }

        /**
         * 
         * Get authorization segment.
         * 
         * @return authorization segment.
         */
        public AuthorizationSegment authorization() {
            return container.getAuthorizationSegment();
        }

        /**
         * 
         * Get component settings segment.
         * 
         * @return component settings segment.
         */
        public ComponentSettingsSegment componentSettings() {
            return container.getComponentSettings();
        }

        /**
         * 
         * Get ssh server segment.
         * 
         * @return ssh server segment.
         */
        public SshServerSegment sshServer() {
            return container.getSshServer();
        }
    }

    /**
     * 
     * Represents the general config {@link Segment}.
     *
     * @author David Scholz
     */
    public class GeneralSegment implements Segment {

        /**
         * 
         * Get comment {@link ConfigurationKey}.
         * 
         * @return comment key.
         */
        public ConfigurationKey comment() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return "comment";
                }

            };
        }

        /**
         * 
         * Get instance name {@link ConfigurationKey}.
         * 
         * @return instance key.
         */
        public ConfigurationKey instanceName() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return "instanceName";
                }
            };
        }

        /**
         * 
         * Get workflow host flag {@link ConfigurationKey}.
         * 
         * @return workflow host flag key.
         */
        public ConfigurationKey isWorkflowHost() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return "isWorkflowHost";
                }
            };
        }

        /**
         * 
         * Get relay flag {@link ConfigurationKey}.
         * 
         * @return relay flag key.
         */
        public ConfigurationKey isRelay() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return "isRelay";
                }
            };
        }

        /**
         * 
         * Get temp directory {@link ConfigurationKey}.
         * 
         * @return temp directory key.
         */
        public ConfigurationKey tempDirectory() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return "tempDirectory";
                }
            };
        }

        /**
         * 
         * Get deprecated input tab flag {@link ConfigurationKey}.
         * 
         * @return deprecated input tab flag key.
         */
        public ConfigurationKey enableDeprecatedInputTab() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return "enableDeprecatedInputTab";
                }
            };
        }

        @Override
        public String getPath() {
            return "general/";
        }
    }

    /**
     * 
     * Represents the background monitoring config {@link Segment}.
     *
     * @author David Scholz
     */
    public class BackgroundMonitoringSegment implements Segment {

        @Override
        public String getPath() {
            return "backgroundMonitoring/";
        }

        /**
         * 
         * Get enabled ids {@link ConfigurationKey}.
         * 
         * @return enabled ids key.
         */
        public ConfigurationKey enableIds() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return "enableIds";
                }
            };
        }

        /**
         * 
         * Get interval seconds {@link ConfigurationKey}.
         * 
         * @return interval seconds key.
         */
        public ConfigurationKey intervalSeconds() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return "intervalSeconds";
                }
            };
        }
    }

    /**
     * 
     * Represents the network config {@link Segment}.
     *
     * @author David Scholz
     */
    public class NetworkSegment implements Segment {

        private final NetworkConnectionListSegment connections = new NetworkConnectionListSegment();

        private final ServerPortListSegment ports = new ServerPortListSegment();

        private final IpFilterSegment ipFilter = new IpFilterSegment();

        @Override
        public String getPath() {
            return "network/";
        }

        /**
         * 
         * Get connection segments.
         * 
         * @return connection segments.
         */
        public NetworkConnectionListSegment connections() {
            return connections;
        }

        /**
         * 
         * Get server port segments.
         * 
         * @return server port segments.
         */
        public ServerPortListSegment ports() {
            return ports;
        }

        /**
         * 
         * Get ip filter segment.
         * 
         * @return ip filter segment.
         */
        public IpFilterSegment ipFilter() {
            return ipFilter;
        }

        /**
         * 
         * Get request timeout {@link ConfigurationKey}.
         * 
         * @return request timeout key.
         */
        public ConfigurationKey requestTimeoutMsec() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return "requestTimeoutMsec";
                }
            };
        }

        /**
         * 
         * Get forwarding timeout {@link ConfigurationKey}.
         * 
         * @return forwarding timeout key.
         */
        public ConfigurationKey forwardingTimeoutMsec() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return "forwardingTimeoutMsec";
                }
            };
        }
    }

    /**
     * 
     * Basis class for {@link NetworkConnectionSegment} and {@link SshConnectionSegment}.
     *
     * @author David Scholz
     */
    private abstract class ConnectionSegment implements Segment {

        private final String name;

        ConnectionSegment(String name) {
            this.name = name;
        }

        @Override
        public String getPath() {
            return "network/connections/" + name;
        }

        abstract ConfigurationKey host();

        abstract ConfigurationKey port();
    }

    /**
     * 
     * Represents the connections config {@link Segment}.
     *
     * @author David Scholz
     */
    public class NetworkConnectionListSegment implements Segment {

        private Map<String, NetworkConnectionSegment> connectionNameToConnectionSegmentMap = new ConcurrentHashMap<>();

        @Override
        public String getPath() {
            return "network/connections";
        }

        /**
         * 
         * Gets existing or create a new {@link NetworkConnectionSegment}.
         * 
         * @param name the connection name.
         * @return the desired {@link NetworkConnectionSegment}.
         */
        public NetworkConnectionSegment getOrCreateConnection(String name) {
            synchronized (connectionNameToConnectionSegmentMap) {
                if (connectionNameToConnectionSegmentMap.containsKey(name)) {
                    return connectionNameToConnectionSegmentMap.get(name);
                } else {
                    NetworkConnectionSegment segment = new NetworkConnectionSegment(name);
                    connectionNameToConnectionSegmentMap.put(name, segment);
                    return segment;
                }
            }
        }

        /**
         * 
         * Checks wether desired connection is already present in the current configuration.
         * 
         * @param name the connection name.
         * @return <code>true</code> if connection is already present in the config file, <code>false</code> else.
         */
        public boolean isConnectionPresent(String name) {
            synchronized (connectionNameToConnectionSegmentMap) {
                return connectionNameToConnectionSegmentMap.containsKey(name);
            }
        }
    }

    /**
     * 
     * Represents a new connection {@link Segment} in the config file.
     *
     * @author David Scholz
     */
    public class NetworkConnectionSegment extends ConnectionSegment {

        public NetworkConnectionSegment(String name) {
            super(name);
        }

        @Override
        public ConfigurationKey host() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return HOST;
                }
            };
        }

        @Override
        public ConfigurationKey port() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return PORT;
                }
            };
        }

        /**
         * 
         * Get connectOnStartup {@link ConfigurationKey}.
         * 
         * @return connectonOnStartup key.
         */
        public ConfigurationKey connectOnStartup() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return "connectOnStartup";
                }
            };
        }

        /**
         * 
         * Get autoRetryInitialDelay {@link ConfigurationKey}.
         * 
         * @return autoRetryInitialDelay key.
         */
        public ConfigurationKey autoRetryInitialDelay() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return "autoRetryInitialDelay";
                }
            };
        }

        /**
         * 
         * Get autoRetryDelayMutliplier {@link ConfigurationKey}.
         * 
         * @return autoRetryDelayMutliplier key.
         */
        public ConfigurationKey autoRetryDelayMultiplier() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return "autoRetryDelayMultiplier";
                }
            };
        }

        /**
         * 
         * Get the autoRetryMaximumDelay {@link ConfigurationKey}.
         * 
         * @return autoRetryMaximumDelay key.
         */
        public ConfigurationKey autoRetryMaximumDelay() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return "autoRetryMaximumDelay";
                }
            };
        }
    }

    /**
     * 
     * Represents the defined ports (ip + port).
     *
     * @author David Scholz
     */
    public class ServerPortListSegment implements Segment {

        private Map<String, ServerPortSegment> portSegmentNameToServerPortSegmentMap = new HashMap<>();

        public ServerPortListSegment() {

        }

        @Override
        public String getPath() {
            return "network/serverPorts";
        }

        /**
         * 
         * Gets existing or create a new {@link ServerPortSegment}.
         * 
         * @param name the port name.
         * @return the desired {@link ServerPortSegment}.
         */
        public ServerPortSegment getOrCreateServerPort(String name) {
            synchronized (portSegmentNameToServerPortSegmentMap) {
                if (portSegmentNameToServerPortSegmentMap.containsKey(name)) {
                    return portSegmentNameToServerPortSegmentMap.get(name);
                } else {
                    ServerPortSegment segment = new ServerPortSegment(name);
                    portSegmentNameToServerPortSegmentMap.put(name, segment);
                    return segment;
                }
            }
        }
    }

    /**
     * 
     * Represents one port in the {@link ServerPortListSegment}.
     *
     * @author David Scholz
     */
    public class ServerPortSegment implements Segment {

        private final String name;

        public ServerPortSegment(String name) {
            this.name = name;
        }

        @Override
        public String getPath() {
            return "network/serverPorts/" + name;
        }

        /**
         * 
         * Get port {@link ConfigurationKey}.
         * 
         * @return port key.
         */
        public ConfigurationKey port() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return PORT;
                }
            };
        }

        /**
         * 
         * Get ip {@link ConfigurationKey}.
         * 
         * @return ip key.
         */
        public ConfigurationKey ip() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return IP;
                }
            };
        }
    }

    /**
     * 
     * Represents the ip filter segment.
     *
     * @author David Scholz
     */
    public class IpFilterSegment implements Segment {

        @Override
        public String getPath() {
            return "network/ipFilter";
        }

        /**
         * 
         * Get enabled {@link ConfigurationKey}.
         * 
         * @return enabled key.
         */
        public ConfigurationKey enabled() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return ENABLED;
                }
            };
        }

        /**
         * 
         * Get allowedIps {@link ConfigurationKey}.
         * 
         * @return allowedIps key.
         */
        public ConfigurationKey allowedIps() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return "allowedIPs";
                }
            };
        }
    }

    /**
     * 
     * SshRemoteAccess field in the config.
     *
     * @author David Scholz
     */
    public class SshRemoteAccessSegment implements Segment {

        private final SshConnectionListSegment sshConnections = new SshConnectionListSegment();

        @Override
        public String getPath() {
            return "sshRemoteAccess/";
        }

        /**
         * 
         * Get exisiting {@link SshConnectionSegment}s.
         * 
         * @return all existing {@link SshConnectionSegment}s.
         */
        public SshConnectionListSegment sshConnections() {
            return sshConnections;
        }

    }

    /**
     * 
     * SshRemoteAccess field in the config.
     *
     * @author Brigitte Boden
     */
    public class UplinkSegment implements Segment {

        private final UplinkConnectionListSegment uplinkConnections = new UplinkConnectionListSegment();

        @Override
        public String getPath() {
            return "uplink/";
        }

        /**
         * 
         * Get exisiting {@link UplinkConnectionSegment}s.
         * 
         * @return all existing {@link UplinkConnectionSegment}s.
         */
        public UplinkConnectionListSegment uplinkConnections() {
            return uplinkConnections;
        }

    }

    /**
     * 
     * Ssh connection list in the config.
     *
     * @author David Scholz
     */
    public class SshConnectionListSegment implements Segment {

        private Map<String, SshConnectionSegment> sshConnectionNameToSegmentMap = new HashMap<>();

        @Override
        public String getPath() {
            return "sshRemoteAccess/sshConnections";
        }

        /**
         * 
         * Gets existing or create a new {@link SshConnectionSegment}.
         * 
         * @param connectionName the connection name.
         * @return the desired {@link SshConnectionSegment}.
         */
        public SshConnectionSegment getOrCreateSshConnection(String connectionName) {
            synchronized (sshConnectionNameToSegmentMap) {
                if (sshConnectionNameToSegmentMap.containsKey(connectionName)) {
                    return sshConnectionNameToSegmentMap.get(connectionName);
                } else {
                    SshConnectionSegment segment = new SshConnectionSegment(connectionName);
                    sshConnectionNameToSegmentMap.put(connectionName, segment);
                    return segment;
                }
            }
        }

    }

    /**
     * 
     * Uplink connection list in the config.
     *
     * @author Brigitte Boden
     */
    public class UplinkConnectionListSegment implements Segment {

        private Map<String, UplinkConnectionSegment> uplinkConnectionNameToSegmentMap = new HashMap<>();

        @Override
        public String getPath() {
            return "uplink/uplinkConnections";
        }

        /**
         * 
         * Gets existing or create a new {@link UplinkConnectionSegment}.
         * 
         * @param connectionId the connection name.
         * @return the desired {@link SshConnectionSegment}.
         */
        public UplinkConnectionSegment getOrCreateUplinkConnection(String connectionId) {
            synchronized (uplinkConnectionNameToSegmentMap) {
                if (uplinkConnectionNameToSegmentMap.containsKey(connectionId)) {
                    return uplinkConnectionNameToSegmentMap.get(connectionId);
                } else {
                    UplinkConnectionSegment segment = new UplinkConnectionSegment(connectionId);
                    uplinkConnectionNameToSegmentMap.put(connectionId, segment);
                    return segment;
                }
            }
        }

    }

    /**
     * 
     * Represents a ssh connection in the {@link SshConnectionListSegment}.
     *
     * @author David Scholz
     */
    public class SshConnectionSegment implements Segment {

        private final String name;

        public SshConnectionSegment(String name) {
            this.name = name;
        }

        @Override
        public String getPath() {
            return "sshRemoteAccess/sshConnections/" + name;
        }

        /**
         * 
         * Get the display name {@link ConfigurationKey}.
         * 
         * @return display name key.
         */
        public ConfigurationKey displayName() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return "displayName";
                }
            };
        }

        /**
         * 
         * Get the host {@link ConfigurationKey}.
         * 
         * @return host key.
         */
        public ConfigurationKey host() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return HOST;
                }
            };
        }

        /**
         * 
         * Get the port {@link ConfigurationKey}.
         * 
         * @return port key.
         */
        public ConfigurationKey port() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return PORT;
                }
            };
        }

        /**
         * 
         * Get the login name {@link ConfigurationKey}.
         * 
         * @return login name key.
         */
        public ConfigurationKey loginName() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return "loginName";
                }
            };
        }
    }

    /**
     * 
     * Represents an uplink connection in the {@link UplinkConnectionListSegment}.
     *
     * @author Brigitte Boden
     */
    public class UplinkConnectionSegment implements Segment {

        private final String id;

        public UplinkConnectionSegment(String id) {
            this.id = id;
        }

        @Override
        public String getPath() {
            return "uplink/uplinkConnections/" + id;
        }

        /**
         * 
         * Get the display name {@link ConfigurationKey}.
         * 
         * @return display name key.
         */
        public ConfigurationKey displayName() {
            return () -> "displayName";
        }

        
        /**
         * 
         * Get the host {@link ConfigurationKey}.
         * 
         * @return host key.
         */
        public ConfigurationKey host() {
            return () -> HOST;
        }

        /**
         * 
         * Get the port {@link ConfigurationKey}.
         * 
         * @return port key.
         */
        public ConfigurationKey port() {
            return () -> PORT;
        }

        /**
         * 
         * Get the login name {@link ConfigurationKey}.
         * 
         * @return login name key.
         */
        public ConfigurationKey loginName() {
            return () -> "loginName";
        }

        /**
         * 
         * Get the client ID {@link ConfigurationKey}.
         * 
         * @return client id key.
         */
        public ConfigurationKey clientID() {
            return () -> "clientID";
        }

        /**
         * 
         * Get the connectOnStartup key {@link ConfigurationKey}.
         * 
         * @return connect on startup key.
         */
        public ConfigurationKey connectOnStartup() {
            return () -> "connectOnStartup";
        }

        /**
         * 
         * Get the isRetry key {@link ConfigurationKey}.
         * 
         * @return retry key.
         */
        public ConfigurationKey autoRetry() {
            return () -> "autoRetry";
        }

        /**
         * 
         * Get the isGateway key {@link ConfigurationKey}.
         * 
         * @return isGateway key.
         */
        public ConfigurationKey isGateway() {
            return () -> "isGateway";
        }
    }

    /**
     * 
     * The published components of the instance.
     *
     * @author David Scholz
     */
    public class PublishingSegment implements Segment {

        @Override
        public String getPath() {
            return "publishing/";
        }

        /**
         * 
         * Get the components {@link ConfigurationKey}.
         * 
         * @return components key.
         */
        public ConfigurationKey components() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return "components";
                }
            };
        }
    }

    /**
     * 
     * The published components of the instance in profile version >= 2.
     *
     * @author Alexander Weinert
     */
    public class AuthorizationSegment implements Segment {

        @Override
        public String getPath() {
            return "authorization/";
        }
    }

    /**
     * 
     * The component settings.
     *
     * @author David Scholz
     */
    public class ComponentSettingsSegment implements Segment {

        private final ClusterComponentSettingsSegment clusterComponent = new ClusterComponentSettingsSegment();

        @Override
        public String getPath() {
            return "componentSettings/";
        }

        public ClusterComponentSettingsSegment getClusterComponentSettings() {
            return clusterComponent;
        }

    }

    /**
     * 
     * Settings of the cluster component.
     *
     * @author David Scholz
     */
    public class ClusterComponentSettingsSegment implements Segment {

        @Override
        public String getPath() {
            return "componentSettings/de.rcenvironment.cluster";
        }

        /**
         * 
         * Get the maxChannels {@link ConfigurationKey}.
         * 
         * @return maxChannels key.
         */
        public ConfigurationKey maxChannels() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return "maxChannels";
                }
            };
        }
    }

    /**
     * 
     * The ssh server segment.
     *
     * @author David Scholz
     */
    public class SshServerSegment implements Segment {

        private final SshAccountListSegment accounts = new SshAccountListSegment();

        private final SshAccountRoleListSegment roles = new SshAccountRoleListSegment();

        @Override
        public String getPath() {
            return "sshServer/";
        }

        public SshAccountListSegment getSshAccounts() {
            return accounts;
        }

        public SshAccountRoleListSegment getSshAccountRoles() {
            return roles;
        }

        /**
         * 
         * Get the enabled {@link ConfigurationKey}.
         * 
         * @return enabled key.
         */
        public ConfigurationKey enabled() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return ENABLED;
                }
            };
        }

        /**
         * 
         * Get the ip {@link ConfigurationKey}.
         * 
         * @return ip key.
         */
        public ConfigurationKey ip() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return IP;
                }
            };
        }

        /**
         * 
         * Get the port {@link ConfigurationKey}.
         * 
         * @return port key.
         */
        public ConfigurationKey port() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return PORT;
                }
            };
        }
    }

    /**
     * 
     * All ssh accounts in {@link SshServerSegment}.
     *
     * @author David Scholz
     */
    public class SshAccountListSegment implements Segment {

        private final Map<String, SshAccountSegment> sshAccountNameToSshAccountSegmentMap = new HashMap<>();

        @Override
        public String getPath() {
            return "sshServer/accounts";
        }

        /**
         * 
         * Gets existing or create a new {@link SshAccountSegment}.
         * 
         * @param name the account name.
         * @return the desired {@link SshAccountSegment}.
         */
        public SshAccountSegment getOrCreateSshAccount(String name) {
            synchronized (sshAccountNameToSshAccountSegmentMap) {
                if (sshAccountNameToSshAccountSegmentMap.containsKey(name)) {
                    return sshAccountNameToSshAccountSegmentMap.get(name);
                } else {
                    SshAccountSegment segment = new SshAccountSegment(name);
                    sshAccountNameToSshAccountSegmentMap.put(name, segment);
                    return segment;
                }
            }
        }
    }

    /**
     * 
     * Represents a ssh account.
     *
     * @author David Scholz
     */
    public class SshAccountSegment implements Segment {

        private final String name;

        public SshAccountSegment(String name) {
            this.name = name;
        }

        @Override
        public String getPath() {
            return "sshServer/accounts/" + name;
        }

        /**
         * 
         * Get the role {@link ConfigurationKey}.
         * 
         * @return role key.
         */
        public ConfigurationKey role() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return "role";
                }
            };
        }

        /**
         * 
         * Get the password hash {@link ConfigurationKey}.
         * 
         * @return password hash key.
         */
        public ConfigurationKey passwordHash() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return "passwordHash";
                }
            };
        }

        /**
         * 
         * Get the enabled {@link ConfigurationKey}.
         * 
         * @return enabled key.
         */
        public ConfigurationKey enabled() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return "enabled";
                }
            };
        }
    }

    /**
     * 
     * All ssh roles.
     *
     * @author David Scholz
     */
    public class SshAccountRoleListSegment implements Segment {

        private final Map<String, SshAccountRoleSegment> sshRoleNameToSegmentMap = new HashMap<>();

        @Override
        public String getPath() {
            return "sshServer/roles";
        }

        /**
         * 
         * Gets existing or create a new {@link SshAccountRoleSegment}.
         * 
         * @param name the account name.
         * @return the desired {@link SshAccountRoleSegment}.
         */
        public SshAccountRoleSegment getOrCreateSshRole(String name) {
            synchronized (sshRoleNameToSegmentMap) {
                if (sshRoleNameToSegmentMap.containsKey(name)) {
                    return sshRoleNameToSegmentMap.get(name);
                } else {
                    SshAccountRoleSegment segment = new SshAccountRoleSegment(name);
                    sshRoleNameToSegmentMap.put(name, segment);
                    return segment;
                }
            }
        }
    }

    /**
     * 
     * A ssh role.
     *
     * @author David Scholz
     */
    public class SshAccountRoleSegment implements Segment {

        private final String name;

        public SshAccountRoleSegment(String name) {
            this.name = name;
        }

        @Override
        public String getPath() {
            return "sshServer/roles/" + name;
        }

        public ConfigurationKey getAllowedCommandPatterns() {
            return new ConfigurationKey() {

                @Override
                public String getConfigurationKey() {
                    return "allowedCommandPatterns";
                }
            };
        }
    }

}
