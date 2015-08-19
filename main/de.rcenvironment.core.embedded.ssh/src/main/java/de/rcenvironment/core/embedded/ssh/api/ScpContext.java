/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.api;

import java.io.File;

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

}
