/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.ssh.jsch.internal;

import de.rcenvironment.core.utils.ssh.jsch.SshSessionConfiguration;


/**
 * Implementation of {@link SshSessionConfiguration}.
 * 
 * @author Doreen Seider
 */
public class SshSessionConfigurationImpl implements SshSessionConfiguration {

    private final String destinationHost;

    private final int port;

    private final String sshAuthUser;

    private final String sshAuthPassPhrase;

    private final String sshKeyFileLocation;

    public SshSessionConfigurationImpl(String destinationHost, int port, String sshAuthUser,
        String sshAuthPassPhrase, String sshKeyFileLocation) {
        this.destinationHost = destinationHost;
        this.port = port;
        this.sshAuthUser = sshAuthUser;
        this.sshAuthPassPhrase = sshAuthPassPhrase;
        this.sshKeyFileLocation = sshKeyFileLocation;
    }

    @Override
    public String getDestinationHost() {
        return destinationHost;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getSshAuthUser() {
        return sshAuthUser;
    }

    @Override
    public String getSshAuthPhrase() {
        return sshAuthPassPhrase;
    }

    @Override
    public String getSshKeyFileLocation() {
        return sshKeyFileLocation;
    }

}
