/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.sshremoteaccess.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.jcraft.jsch.Session;

import de.rcenvironment.core.communication.sshconnection.SshConnectionContext;
import de.rcenvironment.core.communication.sshconnection.SshConnectionService;
import de.rcenvironment.core.communication.sshconnection.api.SshConnectionSetup;

/**
 * SSH connection service mock.
 *
 * @author Brigitte Boden
 */
final class MockSshConnectionService implements SshConnectionService {

    private Session session;

    MockSshConnectionService(Session session) {
        this.session = session;
    }

    @Override
    public boolean isConnected(String connectionId) {
        return false;
    }

    @Override
    public SshConnectionSetup getConnectionSetup(String connnectionId) {
        return null;
    }

    @Override
    public String retreiveSshConnectionPassword(String connectionId) {
        return null;
    }

    @Override
    public Session getAvtiveSshSession(String connnectionId) {
        if (connnectionId.equals(SshRemoteAccessClientTestConstants.DUMMY_ID)) {
            return session;
        }
        return null;
    }

    @Override
    public Collection<SshConnectionSetup> getAllSshConnectionSetups() {
        return null;
    }

    @Override
    public Map<String, SshConnectionSetup> getAllActiveSshConnectionSetups() {
        return null;
    }

    @Override
    public Collection<String> getAllActiveSshConnectionSetupIds() {
        List<String> ids = new ArrayList<String>();
        ids.add(SshRemoteAccessClientTestConstants.DUMMY_ID);
        return ids;
    }

    @Override
    public void editSshConnection(SshConnectionContext parameterObject) {

    }

    @Override
    public void setAuthPhraseForSshConnection(String id, String sshAuthPassPhrase, boolean storePassphrase) {

    }

    @Override
    public void disposeConnection(String connectionId) {}

    @Override
    public void disconnectSession(String connectionId) {}

    @Override
    public Session connectSession(String connectionId, String passphrase) {
        return null;
    }

    @Override
    public Session connectSession(String connectionId) {
        return null;
    }

    @Override
    public String addSshConnection(String displayName, String destinationHost, int port, String sshAuthUser, String keyfileLocation,
        boolean usePassphrase, boolean connectImmediately) {
        return null;
    }
}
