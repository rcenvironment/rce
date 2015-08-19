/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.embedded.ssh.api.ScpContext;
import de.rcenvironment.core.embedded.ssh.api.ScpContextManager;

/**
 * Default {@link ScpContextManager} implementation.
 * 
 * @author Robert Mischke
 */
public class ScpContextManagerImpl implements ScpContextManager {

    private final List<ScpContext> contexts = new ArrayList<>();

    private final Log log = LogFactory.getLog(getClass());

    @Override
    public synchronized ScpContext createScpContext(String username, String virtualRootPath) throws IOException {
        ScpContextImpl newContext = new ScpContextImpl(username, virtualRootPath);
        contexts.add(newContext);
        return newContext;
    }

    @Override
    public synchronized ScpContext getMatchingScpContext(String username, String virtualPath) {
        for (ScpContext context : contexts) {
            if (context.getAuthorizedUsername().equals(username) && virtualPath.startsWith(context.getVirtualScpRootPath())) {
                return context;
            }
        }
        return null;
    }

    @Override
    public synchronized void disposeScpContext(ScpContext scpContext) {
        boolean removed = contexts.remove(scpContext);
        if (!removed) {
            log.warn("ScpContext handed in for disposal, but not present in internal list: " + scpContext.toString());
        }
    }

}
