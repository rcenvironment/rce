/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.file.service.legacy.internal;

import java.net.URI;
import java.text.MessageFormat;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.communication.fileaccess.api.RemoteFileConnection.FileType;
import de.rcenvironment.core.utils.incubator.Assertions;

/**
 * Supportive class for URIs used to identify files. (rce://host:instance/dataReference or
 * file://host:instance/pathToFile)
 * 
 * @author Doreen Seider
 */
public final class RCEFileURIUtils {

    private static final String PARAMETER_URI = "uri";

    private static final String RCE = "rce";

    private static final String ERROR_URI_IS_INVALID = "The URI is invalid: ";

    private static final String ERROR_PARAMETERS_NULL = "The parameter \"{0}\" must not be null.";

    private RCEFileURIUtils() {

    }

    /**
     * Extracts the scheme of the given {@link URI}.
     * 
     * @param uri The {@link URI} extract from.
     * @return the scheme of the {@link URI}.
     * @throws CommunicationException if the {@link URI} does not contain a scheme.
     */
    public static FileType getType(URI uri) throws CommunicationException {

        Assertions.isDefined(uri, MessageFormat.format(ERROR_PARAMETERS_NULL, PARAMETER_URI));

        if (!uri.isAbsolute()) {
            throw new CommunicationException(ERROR_URI_IS_INVALID + "scheme is missing.");
        }
        
        if (uri.getScheme().equals(RCE)) {
            return FileType.RCE_DM;
        } else {
            throw new CommunicationException("Scheme unknown: " + uri.getScheme());
        }
    }

    /**
     * Extracts the host of the given {@link URI}.
     * 
     * @param uri The {@link URI} extract from.
     * @return the host of the {@link URI}.
     * @throws CommunicationException if the {@link URI} does not contain a host.
     */
    public static NodeIdentifier getNodeIdentifier(URI uri) throws CommunicationException {

        Assertions.isDefined(uri, MessageFormat.format(ERROR_PARAMETERS_NULL, PARAMETER_URI));

        validateURI(uri);

        return NodeIdentifierFactory.fromNodeId(uri.getHost());
    }

    /**
     * Extracts the path of the given URI.
     * 
     * @param uri The URI to check.
     * @return the path of the URI.
     * @throws CommunicationException if the URI does not contain a path.
     */
    public static String getPath(URI uri) throws CommunicationException {

        Assertions.isDefined(uri, MessageFormat.format(ERROR_PARAMETERS_NULL, PARAMETER_URI));

        validateURI(uri);

        return uri.getPath().replaceFirst("/", "");
    }

    /**
     * Validates a given URI.
     * 
     * @param uri The URI to checks.
     * @throws CommunicationException if the URI does not contain a path or the path does not
     *         contain instance and path to the file.
     */
    private static void validateURI(URI uri) throws CommunicationException {

        Assertions.isDefined(uri, MessageFormat.format(ERROR_PARAMETERS_NULL, PARAMETER_URI));

        String host = uri.getHost();
        if (host == null || host.trim().isEmpty()) {
            throw new CommunicationException(ERROR_URI_IS_INVALID + "host is missing.");
        }

        String path = uri.getPath();

        if (path == null || path.trim().isEmpty()) {
            throw new CommunicationException(ERROR_URI_IS_INVALID + "path is missing.");
        }
    }
}
