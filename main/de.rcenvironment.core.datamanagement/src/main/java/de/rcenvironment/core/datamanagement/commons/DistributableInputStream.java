/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.commons;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

import de.rcenvironment.core.authentication.User;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.fileaccess.api.RemoteInputStream;

/**
 * Container class for {@link InputStream}s returned from or passed into a data management service.
 * 
 * @author Doreen Seider
 */
public class DistributableInputStream extends InputStream implements Serializable {

    private static final long serialVersionUID = 3334370872679358011L;

    private transient InputStream inputStream;

    private User cert;

    private URI uriToInputStream;

    private RemoteInputStream remoteInputStream;

    public DistributableInputStream(User user, DataReference dataRef, InputStream inputStream) {
        this.cert = user;
        this.inputStream = inputStream;
        try {
            NodeIdentifier nodeId = dataRef.getNodeIdentifier();
            this.uriToInputStream = new URI("rce://" + nodeId.getIdString() + "/"
                + dataRef.getDataReferenceKey());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (inputStream != null) {
            return inputStream.read(b, off, len);
        } else {
            if (remoteInputStream == null) {
                remoteInputStream = new RemoteInputStream(cert, uriToInputStream);
            }
            return remoteInputStream.read(b, off, len);
        }
    }

    @Override
    public int read() throws IOException {
        if (inputStream != null) {
            return inputStream.read();
        } else {
            if (remoteInputStream == null) {
                remoteInputStream = new RemoteInputStream(cert, uriToInputStream);
            }
            return remoteInputStream.read();
        }
    }

    @Override
    public long skip(long n) throws IOException {
        if (inputStream != null) {
            return inputStream.skip(n);
        } else {
            if (remoteInputStream == null) {
                remoteInputStream = new RemoteInputStream(cert, uriToInputStream);
            }
            return remoteInputStream.skip(n);
        }
    }

    @Override
    public void close() throws IOException {
        if (inputStream != null) {
            inputStream.close();
        } else {
            // TODO review: what is this for? - misc_ro
            if (remoteInputStream == null) {
                remoteInputStream = new RemoteInputStream(cert, uriToInputStream);
            }
            remoteInputStream.close();
        }
    }

    public InputStream getLocalInputStream() {
        return inputStream;
    }
}
