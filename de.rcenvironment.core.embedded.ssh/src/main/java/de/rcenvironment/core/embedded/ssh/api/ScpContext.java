/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.api;

import java.io.File;
import java.io.IOException;

/**
 * Represents a virtual SCP directory that is usable by a single SSH user. Operations on this SCP directory are mapped to a local directory.
 * Currently used to provide session-based, isolated upload/download directories.
 * 
 * @author Robert Mischke
 */
public interface ScpContext {

    /**
     * @return the login name of the SSH account that is allowed to use this SCP context
     */
    String getAuthorizedUsername();

    /**
     * @return the root path for SCP operations, as seen from the SCP client's side
     */
    String getVirtualScpRootPath();

    /**
     * @return the actual local directory that the root path for SCP operations is mapped to
     */
    File getLocalRootPath();

    /**
     * Dispose the temp directory for this context.
     * 
     * @throws IOException if deletion of temp directory fails.
     */
    void dispose() throws IOException;

}
