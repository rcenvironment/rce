/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.file.service.legacy.internal;

import java.io.IOException;
import java.net.URI;

import org.osgi.framework.BundleContext;

import de.rcenvironment.core.authentication.User;
import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.fileaccess.api.RemoteFileConnection;
import de.rcenvironment.core.communication.fileaccess.spi.RemoteFileConnectionFactory;

/**
 * Implementation of the {@link RemoteFileConnectionFactory} creating
 * {@link ServiceRemoteFileConnection} objects.
 * 
 * @author Doreen Seider
 */
@Deprecated
public class ServiceRemoteFileConnectionFactory implements RemoteFileConnectionFactory {

    private BundleContext context;

    private CommunicationService communicationService;
    
    protected void activate(BundleContext bundleContext) {
        context = bundleContext;
    }

    protected void bindCommunicationService(CommunicationService newCommunicationService) {
        communicationService = newCommunicationService;
    }
    
    @Override
    public RemoteFileConnection createRemoteFileConnection(User cert, URI uri) throws IOException {
        return new ServiceRemoteFileConnection(cert, uri, communicationService, context);
    }

}
