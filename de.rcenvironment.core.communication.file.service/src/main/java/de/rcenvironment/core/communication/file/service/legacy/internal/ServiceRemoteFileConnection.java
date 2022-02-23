/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.file.service.legacy.internal;

import java.io.IOException;
import java.net.URI;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.file.service.legacy.api.RemotableFileStreamAccessService;
import de.rcenvironment.core.communication.fileaccess.api.RemoteFileConnection;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.incubator.Assertions;

/**
 * This class provides access remote files via the communication bundle service call concept.
 * 
 * @author Heinrich Wendel
 * @author Doreen Seider
 * @author Robert Mischke (adapted for 7.0.0)
 */
@Deprecated
public class ServiceRemoteFileConnection implements RemoteFileConnection {

    private static final int MINUS_ONE = -1;

    private static final long serialVersionUID = -3315352695999821776L;

    private static final String ERROR_PARAMETERS_NULL = "The parameter \"%s\" must not be null.";

    /**
     * The {@link RemotableFileStreamAccessService} of the remote instance where the file is located.
     */
    private final RemotableFileStreamAccessService fileService;

    /**
     * The remote UUID of the {@link InputStream}.
     */
    private final String remoteInputStreamUUID;

    /**
     * Creates a new {@link ServiceRemoteFileConnection} of a remote file and initialize it.
     * @param uri URI pointing to remote file. (rce://node-id/dataReferenceUUID/revision)
     * @param user The user's certificate.
     * 
     * @throws IOException if the file could not be accessed remotely.
     */
    public ServiceRemoteFileConnection(URI uri, CommunicationService communicationService)
        throws IOException {

        try {
            fileService = communicationService.getRemotableService(RemotableFileStreamAccessService.class,
                RCEFileURIUtils.getNodeIdentifier(uri));

            remoteInputStreamUUID = fileService.open(RCEFileURIUtils.getType(uri), RCEFileURIUtils.getPath(uri));

        } catch (RemoteOperationException e) {
            throw new IOException(e.toString());
        } catch (CommunicationException e) {
            throw new IOException(e);
        }

    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {

        Assertions.isDefined(b, StringUtils.format(ERROR_PARAMETERS_NULL, "b"));

        Byte[] objectB = new Byte[b.length];
        for (int i = 0; i < b.length; i++) {
            objectB[i] = new Byte(b[i]);
        }
        int read = 0;
        try {
            byte[] buffer = (byte[]) fileService.read(remoteInputStreamUUID, new Integer(len));

            if (buffer.length > 0) {
                System.arraycopy(buffer, 0, b, off, buffer.length);
                read = buffer.length;
            } else {
                read = MINUS_ONE;
            }
        } catch (RemoteOperationException e) {
            throw new IOException(e.toString());
        }

        return read;
    }

    @Override
    public int read() throws IOException {
        try {
            return fileService.read(remoteInputStreamUUID);
        } catch (RemoteOperationException e) {
            throw new IOException(e.toString());
        }
    }

    @Override
    public long skip(long n) throws IOException {
        try {
            return fileService.skip(remoteInputStreamUUID, n);
        } catch (RemoteOperationException e) {
            throw new IOException(e.toString());
        }
    }

    @Override
    public void close() throws IOException {
        try {
            fileService.close(remoteInputStreamUUID);
        } catch (RemoteOperationException e) {
            throw new IOException(e.toString());
        }
    }

}
