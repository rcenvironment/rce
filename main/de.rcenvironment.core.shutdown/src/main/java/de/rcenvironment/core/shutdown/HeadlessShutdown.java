/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.shutdown;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.configuration.bootstrap.BootstrapConfiguration;
import de.rcenvironment.core.start.common.Instance;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;
import de.rcenvironment.core.utils.incubator.IdGenerator;

/**
 * This class either (a) opens a tcp port and listens for orders to shutdown itself or (b) it tries to connect to another running instance
 * of rce and send the order to shut down before it shuts down itself. The argument --shutdown triggers (b). To prevent unintended
 * shutdowns, the port is chosen randomly and a secret token, stored in the configurations folder is checked.
 * 
 * @author Oliver Seebach
 * @author Robert Mischke
 */
public class HeadlessShutdown {

    private static final String TOKEN_FILENAME = "shutdown.dat";

    private static final String HOST = "localhost";

    private static final int REGULAR_SHUTDOWN_WAIT_TIME_MSEC = 20000; // in ms

    private static final int BUFFERSIZE = 200;

    private BootstrapConfiguration bootstrapSettings;

    private final Log logger = LogFactory.getLog(getClass());

    /**
     * Triggers either the initiation of the shutdown mechanism either is client or server.
     * 
     * @param bootstrapConfiguration the global BootstrapConfiguration instance
     */
    public void executeByLaunchConfiguration(BootstrapConfiguration bootstrapConfiguration) {
        this.bootstrapSettings = bootstrapConfiguration;

        final File shutdownDataDir = bootstrapSettings.getShutdownDataDirectory();
        if (!shutdownDataDir.exists()) {
            shutdownDataDir.mkdirs();
        }

        if (bootstrapSettings.isShutdownRequested()) {
            try {
                writeToLog("Running this instance as a shutdown signal sender");
                sendShutdownTokenInternal(shutdownDataDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                tryToRemoveProfileStubDir();
                System.exit(0);
            }
        } else {
            try {
                initReceiver(shutdownDataDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Sends a shutdown signal to a different (external) instance.
     * 
     * @param externalProfileDir the profile directory of the external instance
     * @throws IOException if sending the shutdown signal fails
     */
    public void shutdownExternalInstance(File externalProfileDir) throws IOException {
        sendShutdownTokenInternal(new File(externalProfileDir, BootstrapConfiguration.PROFILE_SHUTDOWN_DATA_SUBDIR));
    }

    private void writeToLog(String text) {
        logger.debug(text);
    }

    // RECEIVER PART
    private void initReceiver(File shutdownDataDir) throws IOException {

        // Automatically create socket on free port
        final ServerSocket serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();

        // generate (pseudo-)random secret token
        final String secretString = IdGenerator.randomUUIDWithoutDashes();

        String secret = StringUtils.escapeAndConcat(String.valueOf(port), secretString);
        File secretFile = new File(shutdownDataDir, TOKEN_FILENAME);
        FileUtils.writeStringToFile(secretFile, secret);

        secretFile.deleteOnExit();

        writeToLog("Stored shutdown information at location " + secretFile.getAbsolutePath());

        SharedThreadPool.getInstance().execute(new Runnable() {

            @Override
            @TaskDescription("Service/daemon shutdown listener")
            public void run() {
                writeToLog("Listening for shutdown signals");
                Socket client = null;
                String message = null;
                try {
                    client = waitForConnection(serverSocket);
                    message = readMessage(client);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                if (message != null) {
                    writeToLog("Message \"" + message + "\" received");
                    if (message.contains("shutdown") && message.contains(secretString)) {
                        writeToLog("Received shutdown signal, shutting down");
                        IOUtils.closeQuietly(serverSocket);
                        Instance.shutdown();
                        try {
                            Thread.sleep(REGULAR_SHUTDOWN_WAIT_TIME_MSEC);
                        } catch (InterruptedException e) {
                            logger.error(e);
                            writeToLog("Regular shutdown time expired, shutting down hard using System.exit()");
                            System.exit(0);
                        }
                    }
                }
            }

        });

    }

    private Socket waitForConnection(ServerSocket serverSocket) throws IOException {
        writeToLog("Waiting for connection at port " + serverSocket.getLocalPort());
        Socket socket = serverSocket.accept(); // blocking wait
        writeToLog("Accepted connection at port " + socket.getLocalPort());
        return socket;
    }

    private String readMessage(Socket socket) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        char[] buffer = new char[BUFFERSIZE];
        int length = bufferedReader.read(buffer, 0, BUFFERSIZE);
        String message = new String(buffer, 0, length);
        return message;
    }

    // SENDER PART
    private void sendShutdownTokenInternal(File shutdownDataDir) throws IOException, UnknownHostException {
        File secretFile = new File(shutdownDataDir, TOKEN_FILENAME);
        String content;
        try {
            content = FileUtils.readFileToString(secretFile);
        } catch (IOException e) {
            logger.error("Failed to load shutdown configuration file: " + e.getMessage());
            throw e;
        }

        // writeToLog("Loaded shutdown configuration " + content + " from file " + secretFile.getAbsolutePath());

        int port = Integer.parseInt(StringUtils.splitAndUnescape(content)[0]);
        String secret = StringUtils.splitAndUnescape(content)[1];

        Socket socket = new Socket(HOST, port);
        String message = "shutdown " + secret;

        writeToLog("Sending \"" + message + "\" to " + HOST + ":" + port);

        writeMessageToConnection(socket, message);
    }

    private void tryToRemoveProfileStubDir() {
        // this will only delete the directory if it is empty, so there is no harm in trying
        if (!bootstrapSettings.getProfileDirectory().delete()) {
            logger.warn("Failed to remove temporary profile directory " + bootstrapSettings.getProfileDirectory()
                + " although it should not contain any files");
        }

    }

    private void writeMessageToConnection(Socket socket, String message) throws IOException {
        PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
        printWriter.print(message);
        printWriter.flush();
    }

}
