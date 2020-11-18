/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.testing;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.utils.common.StreamConnectionEndpoint;

/**
 * Opens a local server socket on a random port, opens a TCP connection to it, and provides access to the four stream endpoints. Intended
 * for using an actual TCP connection as part of network integration or unit tests where {@link PipedInputStream} and
 * {@link PipedOutputStream} are not sufficient, for example for testing proper EOF/closing/Exception behavior.
 *
 * @author Robert Mischke
 */
public class LocalTCPTestConnection implements Closeable {

    private static final int LOCAL_CONNECTION_INIT_TIMEOUT = 500;

    private final ServerSocket serverSocket;

    private final Socket clientSocket;

    private InputStream clientSideInputStream;

    private OutputStream clientSideOutputStream;

    private InputStream serverSideInputStream;

    private OutputStream serverSideOutputStream;

    public LocalTCPTestConnection() throws IOException {
        final AtomicReference<String> serverSideInitError = new AtomicReference<>();
        final CountDownLatch serverSideInitializedLatch = new CountDownLatch(1);

        serverSocket = new ServerSocket(0);

        // using a bare Thread to avoid a dependency on AsyncTaskService
        new Thread(() -> {
            try {
                Socket socket = serverSocket.accept();
                serverSideInputStream = socket.getInputStream();
                serverSideOutputStream = socket.getOutputStream();
            } catch (IOException e) {
                serverSideInitError.set(e.toString());
            }
            serverSideInitializedLatch.countDown();
        }).start();

        clientSocket = new Socket("127.0.0.1", serverSocket.getLocalPort());
        clientSideInputStream = clientSocket.getInputStream();
        clientSideOutputStream = clientSocket.getOutputStream();

        try {
            serverSideInitializedLatch.await(LOCAL_CONNECTION_INIT_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new IOException(e); // for API simplicity
        }
        if (serverSideInitError.get() != null) {
            throw new IOException("Server-side init error: " + serverSideInitError.get());
        }
    }

    @Override
    public void close() throws IOException {
        try {
            clientSocket.close();
        } finally {
            serverSocket.close();
        }
    }

    public InputStream getClientSideInputStream() {
        return clientSideInputStream;
    }

    public OutputStream getClientSideOutputStream() {
        return clientSideOutputStream;
    }

    public InputStream getServerSideInputStream() {
        return serverSideInputStream;
    }

    public OutputStream getServerSideOutputStream() {
        return serverSideOutputStream;
    }

    public StreamConnectionEndpoint getServerSideEndpoint() {
        return new StreamConnectionEndpoint() {

            @Override
            public OutputStream getOutputStream() {
                return serverSideOutputStream;
            }

            @Override
            public InputStream getInputStream() {
                return serverSideInputStream;
            }

            @Override
            public void close() {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    LogFactory.getLog(getClass()).debug("Error while closing the server-side end of a test connection: " + e.toString());
                }
            }
        };

    }

    public StreamConnectionEndpoint getClientSideEndpoint() {
        return new StreamConnectionEndpoint() {

            @Override
            public OutputStream getOutputStream() {
                return clientSideOutputStream;
            }

            @Override
            public InputStream getInputStream() {
                return clientSideInputStream;
            }

            @Override
            public void close() {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    LogFactory.getLog(getClass()).debug("Error while closing the client-side end of a test connection: " + e.toString());
                }
            }
        };

    }

}
