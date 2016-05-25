/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.fileaccess.spi;

import java.io.IOException;
import java.net.URI;

import de.rcenvironment.core.communication.fileaccess.api.RemoteFileConnection;

/**
 * 
 * Factory creating {@link RemoteFileConnection} implementations. This interface has to be
 * implemented and registers as an OSGi service by bundles providing a file transfer protocol and
 * thus a {@link RemoteFileConnection} implementation.
 * 
 * @author Doreen Seider
 */
public interface RemoteFileConnectionFactory {

    /**
     * Key for a service property.
     */
    String PROTOCOL = "protocol";

    /**
     * 
     * Creates {@link RemoteFileConnection} implementations.
     * 
     * @param uri The URI pointing to the remote file to access.
     * @return the {@link RemoteFileConnection} object.
     * @throws IOException if the file could not be accessed remotely.
     */
    RemoteFileConnection createRemoteFileConnection(URI uri) throws IOException;
}
