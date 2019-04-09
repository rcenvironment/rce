/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.io.File;

import de.rcenvironment.core.embedded.ssh.api.TemporarySshAccount;

/**
 * Represents a temporary login account, authenticated with a public key or password. Currently used for restricted SCP uploads/downloads
 * only.
 * 
 * @author Sebastian Holtappels
 * @author Robert Mischke
 */
public class TemporarySshAccountImpl extends SshAccountImpl implements TemporarySshAccount {

    private String virtualScpRootPath = null;

    private File localScpRootPath = null;

    public TemporarySshAccountImpl() {}

    @Override
    public String getVirtualScpRootPath() {
        return virtualScpRootPath;
    }

    public void setVirtualScpRootPath(String path) {
        this.virtualScpRootPath = path;
    }

    @Override
    public File getLocalScpRootPath() {
        return localScpRootPath;
    }

    public void setLocalScpRootPath(File localScpRootPath) {
        this.localScpRootPath = localScpRootPath;
    }

}
