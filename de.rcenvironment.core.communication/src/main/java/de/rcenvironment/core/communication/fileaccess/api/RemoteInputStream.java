/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.fileaccess.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.fileaccess.internal.RemoteFileConnectionSupport;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.Assertions;

/**
 * This class provides access to remote file by using a specified {@link RemoteFileConnection}
 * implementation got from the supportive class {@link RemoteFileConnectionSupport}.
 * 
 * This class is not meant to be thread safe!
 * 
 * @author Heinrich Wendel
 * @author Doreen Seider
 */
public class RemoteInputStream extends InputStream implements Serializable {

    private static final long serialVersionUID = 8592687526532347895L;

    private static final String ERROR_PARAMETERS_NULL = "The parameter \"%s\" must not be null.";

    private static final String REMOTE_STREAMS_SHOULD_NOT_CALL_THIS_METHOD = "Remote streams should not call this method";

    private final RemoteFileConnection remoteFileConnection;

    /**
     * Creates a new {@link RemoteInputStream} of a remote file.
     * 
     * @param uri URI pointing to remote file. (rce://host:platformNo/dataReferenceUUID/revision or
     *        file://host:platformNo/absolutePathToFile)
     * @throws IOException if the file could not be accessed remotely.
     */
    public RemoteInputStream(URI uri) throws IOException {

        Assertions.isDefined(uri, StringUtils.format(ERROR_PARAMETERS_NULL, "uri"));
        Assertions.isDefined(uri, StringUtils.format(ERROR_PARAMETERS_NULL, "certificate"));

        try {
            remoteFileConnection = RemoteFileConnectionSupport.getRemoteFileConnection(uri);
        } catch (CommunicationException e) {
            throw new IOException(e);
        }

    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return remoteFileConnection.read(b, off, len);
    }

    @Override
    public int read(byte[] b) throws IOException {
        // delegate
        return read(b, 0, b.length);
    }

    @Override
    public int read() throws IOException {
        throw new UnsupportedOperationException(REMOTE_STREAMS_SHOULD_NOT_CALL_THIS_METHOD);
    }

    @Override
    public long skip(long n) throws IOException {
        return remoteFileConnection.skip(n);
    }

    @Override
    public void close() throws IOException {
        remoteFileConnection.close();
    }

}
