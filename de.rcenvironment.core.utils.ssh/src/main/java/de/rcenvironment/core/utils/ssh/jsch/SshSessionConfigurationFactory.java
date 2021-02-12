/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.ssh.jsch;

import de.rcenvironment.core.utils.ssh.jsch.internal.SshSessionConfigurationImpl;

/**
 * Creates {@link SshSessionConfiguration}s.
 * @author Doreen Seider
 */
public final class SshSessionConfigurationFactory {

    private SshSessionConfigurationFactory() {}
    
    /**
     * Creates {@link SshSessionConfiguration} with auth phrase.
     * @param destinationHost host to connect to
     * @param port port of host to use
     * @param sshAuthUser auth name of user
     * @param sshAuthPassPhrase auth phrase
     * @return {@link SshSessionConfiguration}
     */
    public static SshSessionConfiguration createSshSessionConfigurationWithAuthPhrase(String destinationHost, int port, String sshAuthUser,
        String sshAuthPassPhrase) {
        return new SshSessionConfigurationImpl(destinationHost, port, sshAuthUser, sshAuthPassPhrase, null);
    }
    
    /**
     * Creates {@link SshSessionConfiguration} with key file location.
     * @param destinationHost host to connect to
     * @param port port of host to use
     * @param sshAuthUser auth name of user
     * @param sshKeyFileLocation location of key file to use
     * @return {@link SshSessionConfiguration}
     */
    public static SshSessionConfiguration createSshSessionConfigurationWithKeyFileLocation(String destinationHost, int port,
        String sshAuthUser, String sshKeyFileLocation) {
        return new SshSessionConfigurationImpl(destinationHost, port, sshAuthUser, null, sshKeyFileLocation);
    }
}
