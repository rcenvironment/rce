/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.fileaccess.internal;

import java.io.IOException;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.fileaccess.api.RemoteFileConnection;
import de.rcenvironment.core.communication.fileaccess.spi.RemoteFileConnectionFactory;

/**
 * Supportive class to get {@link RemoteFileConnection} objects. The class looks for the right one
 * by means of the contact information.
 * 
 * @author Doreen Seider
 */
public final class RemoteFileConnectionSupport {

    private static final String ERROR_SERVICE_NOT_REGISTERED = "A remote input stream factory service is not registered.";

    private static final Log LOGGER = LogFactory.getLog(RemoteFileConnectionSupport.class);

    private static BundleContext bundleContext;

    /** Only called by OSGi. */
    @Deprecated
    public RemoteFileConnectionSupport() {}

    /**
     * Activation method called by OSGi.
     * 
     * @param context The injected {@link BundleContext}.
     **/
    public void activate(BundleContext context) {
        bundleContext = context;
    }

    /**
     * Returns a new {@link RemoteFileConnection} instance.
     * 
     * @param uri The URI for which a {@link RemoteFileConnection} instance should be created.
     * @return A new {@link RemoteFileConnection} object.
     * @throws CommunicationException Thrown if the {@link RemoteFileConnection} object could not be
     *         created.
     * @throws IOException if the file does not exist remotely.
     */
    public static RemoteFileConnection getRemoteFileConnection(URI uri) throws CommunicationException, IOException {

        // try to get the remote input stream access object by getting and calling the remote input
        // stream access factory
        // service
        ServiceReference<?>[] factoryReferences = null;
        try {
            factoryReferences = bundleContext.getAllServiceReferences(RemoteFileConnectionFactory.class.getName(), null);
        } catch (InvalidSyntaxException e) {
            // Will not happen
            LOGGER.error("Failed to get a remote input stream factory service: Invalid protocol filter syntax.");
        }

        RemoteFileConnectionFactory remoteFileConnectionFactory;
        if (factoryReferences != null && factoryReferences.length > 0) {
            remoteFileConnectionFactory = (RemoteFileConnectionFactory) bundleContext.getService(factoryReferences[0]);
            if (remoteFileConnectionFactory == null) {
                throw new CommunicationException(ERROR_SERVICE_NOT_REGISTERED);
            }
        } else {
            throw new CommunicationException(ERROR_SERVICE_NOT_REGISTERED);
        }

        return remoteFileConnectionFactory.createRemoteFileConnection(uri);
    }
}
