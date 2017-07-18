/*
 * Copyright (C) 2006-2016 DLR, Germany
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

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.fileaccess.api.RemoteInputStream;

/**
 * Container class for {@link InputStream}s returned from or passed into a data management service.
 * 
 * @author Doreen Seider
 */
public class DistributableInputStream extends InputStream implements Serializable {

    private static final long serialVersionUID = 3334370872679358011L;

    private transient InputStream inputStream;

    private URI uriToInputStream;

    private RemoteInputStream remoteInputStream;

    public DistributableInputStream(DataReference dataRef, InputStream inputStream) {
        this.inputStream = inputStream;
        try {
            LogicalNodeId nodeId = dataRef.getStorageNodeId();
            // TODO review/encapsulate; but RCEFileURIUtils is currently an internal class, so it's not a trivial refactoring
            this.uriToInputStream =
                new URI("rce://" + nodeId.getInstanceNodeIdString() + "/" + dataRef.getDataReferenceKey());
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
                remoteInputStream = new RemoteInputStream(uriToInputStream);
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
                remoteInputStream = new RemoteInputStream(uriToInputStream);
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
                remoteInputStream = new RemoteInputStream(uriToInputStream);
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
                remoteInputStream = new RemoteInputStream(uriToInputStream);
            }
            remoteInputStream.close();
        }
    }

    public InputStream getLocalInputStream() {
        return inputStream;
    }
}
