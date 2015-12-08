/*
 * Copyright (C) 2006-2015 DLR, Germany
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
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public SshConnectionSetup getConnectionSetup(String connnectionId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String retreiveSshConnectionPassword(String connectionId) {
        // TODO Auto-generated method stub
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, SshConnectionSetup> getAllActiveSshConnectionSetups() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<String> getAllActiveSshConnectionSetupIds() {
        List<String> ids = new ArrayList<String>();
        ids.add(SshRemoteAccessClientTestConstants.DUMMY_ID);
        return ids;
    }

    @Override
    public void editSshConnection(String id, String displayName, String destinationHost, int port, String sshAuthUser) {
        // TODO Auto-generated method stub

    }

    @Override
    public void editAuthPhraseForSshConnection(String id, String sshAuthPassPhrase, boolean storePassphrase, boolean connectOnStartup) {
        // TODO Auto-generated method stub

    }

    @Override
    public void disposeConnection(String connectionId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void disconnectSession(String connectionId) {
        // TODO Auto-generated method stub

    }

    @Override
    public Session connectSession(String connectionId, String passphrase) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Session connectSession(String connectionId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String addSshConnectionWithAuthPhrase(String displayName, String destinationHost, int port, String sshAuthUser,
        String sshAuthPassPhrase, boolean storePassphrase, boolean connectOnStartup) {
        // TODO Auto-generated method stub
        return null;
    }
}
