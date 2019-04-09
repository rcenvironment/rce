/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.api;

import java.io.File;

/**
 * Represents a temporary login account, authenticated with a public key or password. Currently used to provide a separate context for SCP
 * uploads/downloads.
 * 
 * @deprecated superseded by {@link ScpContext} since RCE 5.0.0 - misc_ro
 * 
 * @author Sebastian Holtappels
 * @author Robert Mischke
 */
@Deprecated
public interface TemporarySshAccount extends SshAccount {

    /**
     * @return the root path for SCP operations, as seen from the SCP client's side
     */
    String getVirtualScpRootPath();

    /**
     * @return the actual local directory that the root path for SCP operations is mapped to
     */
    File getLocalScpRootPath();
}
