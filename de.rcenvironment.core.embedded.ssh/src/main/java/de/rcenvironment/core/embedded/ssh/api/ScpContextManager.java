/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.api;

import java.io.IOException;

/**
 * Service interface for the creation, fetching, and disposal of {@link ScpContext} instances. The service is stateful; it keeps track of
 * all active {@link ScpContext}s.
 * 
 * @author Robert Mischke
 */
public interface ScpContextManager {

    /**
     * Creates a new {@link ScpContext} for the given SSH username and with the given SCP root path. A temporary directory is implicitly
     * created and stored in the {@link ScpContext}.
     * 
     * @param username the name of the SSH user that is authorized to access the given SCP path
     * @param virtualRootPath the root SCP path that all SCP operations must take place in
     * @return the new {@link ScpContext} object
     * @throws IOException when creating the temporary directory fails (unlikely, but possible)
     */
    ScpContext createScpContext(String username, String virtualRootPath) throws IOException;

    /**
     * Fetches a matching {@link ScpContext} for a SSH username and the requested SCP path.
     * 
     * TODO (p3) expand explanation
     * 
     * @param username the requesting user's SSH login name
     * @param virtualPath the requested SCP path
     * @return a matching {@link ScpContext}, if one exists, null otherwise
     */
    ScpContext getMatchingScpContext(String username, String virtualPath);

    /**
     * Deactivates and removes a {@link ScpContext} instance previously created by calling {@link #createScpContext(String, String)}.
     * 
     * @param scpContext the {@link ScpContext} instance
     * @throws IOException when releasing/deleting the temporary directory fails (unlikely, but possible)
     */
    void disposeScpContext(ScpContext scpContext) throws IOException;

}
