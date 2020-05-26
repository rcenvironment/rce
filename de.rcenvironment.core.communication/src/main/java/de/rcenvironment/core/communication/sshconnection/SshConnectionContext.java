/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.sshconnection;

/**
 * A class containing the parameters for an SSH connection. (Introduced to avoid methods with too many parameters.)
 *
 * @author Brigitte Boden
 */
public class SshConnectionContext {

    private String id;

    private String displayName;
    
    private String qualifier;

    private String destinationHost;

    private int port;

    private String sshAuthUser;

    private String keyfileLocation;

    private boolean usePassphrase;

    private boolean connectImmediately;

    private boolean autoRetry;
    
    private boolean isGateway;

    public SshConnectionContext(String id, String displayName, String qualifier, String destinationHost, int port, String sshAuthUser,
        String keyfileLocation, boolean usePassphrase, boolean connectImmediately, boolean autoRetry, boolean isGateway) {
        this.id = id;
        this.displayName = displayName;
        this.qualifier = qualifier;
        this.destinationHost = destinationHost;
        this.port = port;
        this.sshAuthUser = sshAuthUser;
        this.keyfileLocation = keyfileLocation;
        this.usePassphrase = usePassphrase;
        this.connectImmediately = connectImmediately;
        this.autoRetry = autoRetry;
        this.isGateway = isGateway;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDestinationHost() {
        return destinationHost;
    }

    public int getPort() {
        return port;
    }

    public String getSshAuthUser() {
        return sshAuthUser;
    }

    public boolean isConnectImmediately() {
        return connectImmediately;
    }

    public String getKeyfileLocation() {
        return keyfileLocation;
    }

    public boolean isUsePassphrase() {
        return usePassphrase;
    }

    public boolean isAutoRetry() {
        return autoRetry;
    }
    
    public String getQualifier() {
        return qualifier;
    }
    
    public boolean isGateway() {
        return isGateway;
    }
}
