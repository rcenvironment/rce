/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.io.File;
import java.io.IOException;

import de.rcenvironment.core.embedded.ssh.api.ScpContext;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Default {@link ScpContext} implementation.
 * 
 * @author Robert Mischke
 */
public class ScpContextImpl implements ScpContext {

    private String authorizedUsername;

    private String virtualScpRootPath;

    private File localRootPath;

    public ScpContextImpl(String username, String virtualRootPath) throws IOException {
        this.authorizedUsername = username;
        this.virtualScpRootPath = virtualRootPath;
        this.localRootPath = TempFileServiceAccess.getInstance().createManagedTempDir("scp");
    }

    @Override
    public String getAuthorizedUsername() {
        return authorizedUsername;
    }

    public void setAuthorizedUsername(String accountName) {
        this.authorizedUsername = accountName;
    }

    @Override
    public String getVirtualScpRootPath() {
        return virtualScpRootPath;
    }

    public void setVirtualScpRootPath(String virtualScpRootPath) {
        this.virtualScpRootPath = virtualScpRootPath;
    }

    @Override
    public File getLocalRootPath() {
        return localRootPath;
    }

    public void setLocalRootPath(File localScpRootPath) {
        this.localRootPath = localScpRootPath;
    }

    /**
     * Deletes the local temporary root directory.
     * 
     * @throws IOException if deletion fails
     */
    @Override
    public void dispose() throws IOException {
        TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(localRootPath);
    }

    @Override
    public String toString() {
        return StringUtils.format("User='%s', SCP root path='%s', local root dir='%s'", authorizedUsername, virtualScpRootPath,
            localRootPath);
    }
}
