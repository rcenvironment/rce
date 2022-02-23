/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
import de.rcenvironment.core.configuration.bootstrap.profile.Profile;
import de.rcenvironment.core.start.common.Instance;
import de.rcenvironment.core.toolkitbridge.api.ToolkitBridge;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.AuditLog;
import de.rcenvironment.core.utils.common.AuditLogIds;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.toolkit.utils.common.IdGenerator;

/**
 * This class either (a) opens a tcp port and listens for orders to shutdown itself or (b) it tries to connect to another running instance
 * of rce and send the order to shut down before it shuts down itself. The argument --shutdown triggers (b). To prevent unintended
 * shutdowns, the port is chosen randomly and a secret token, stored in the configurations folder is checked.
 * 
 * @author Oliver Seebach
 * @author Robert Mischke
 */
public class HeadlessShutdown {

    private static final int SHUTDOWN_TOKEN_LENGTH = 32;

    private static final String TOKEN_FILENAME = "shutdown.dat";

    private static final String HOST = "localhost";

    private static final int REGULAR_SHUTDOWN_WAIT_TIME_MSEC = 60000; // may include waiting for GUI startup to complete first

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

        if (bootstrapSettings.isShutdownRequested()) {
            try {
                writeToLog("Running this instance as a shutdown signal sender");
                sendShutdownTokenInternal(bootstrapSettings.getTargetShutdownDataDirectory());
            } catch (IOException e) {
                logger.error("Failed to shutdown external instance: " + e.getMessage());
                throw new RuntimeException(e);
            } finally {
                tryToRemoveInternalProfileDir();
                System.exit(0);
            }
        } else {
            try {
                initReceiver(bootstrapSettings.getOwnShutdownDataDirectory());
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
        sendShutdownTokenInternal(new File(externalProfileDir, Profile.PROFILE_SHUTDOWN_DATA_SUBDIR));
    }

    private void writeToLog(String text) {
        logger.debug(text);
    }

    // RECEIVER PART
    private void initReceiver(File shutdownDataDir) throws IOException {

        if (!shutdownDataDir.exists()) {
            shutdownDataDir.mkdirs();
        }

        // Automatically create socket on free port
        final ServerSocket serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();

        // generate (pseudo-)random secret token
        final String secretString = IdGenerator.secureRandomHexString(SHUTDOWN_TOKEN_LENGTH);

        String secret = StringUtils.escapeAndConcat(String.valueOf(port), secretString);
        File secretFile = new File(shutdownDataDir, TOKEN_FILENAME);
        FileUtils.writeStringToFile(secretFile, secret);

        secretFile.deleteOnExit();

        writeToLog("Stored shutdown information at location " + secretFile.getAbsolutePath());

        // necessary as both bundles are on OSGi start level 3, and the wrapped method uses ConcurrencyUtils.getAsyncTaskService()
        ToolkitBridge.afterToolkitAvailable(new Runnable() {

            @Override
            public void run() {
                startShutdownListener(serverSocket, secretString);
            }
        });

    }

    private void startShutdownListener(final ServerSocket serverSocket, final String secretString) {
        ConcurrencyUtils.getAsyncTaskService().execute("Service/daemon shutdown listener", () -> {

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
                    AuditLog.append(AuditLogIds.APPLICATION_SHUTDOWN_REQUESTED, "method", "CLI/network signal");
                    logger.info("Received shutdown signal, shutting down");
                    IOUtils.closeQuietly(serverSocket);
                    Instance.shutdown(); // non-blocking
                    try {
                        Thread.sleep(REGULAR_SHUTDOWN_WAIT_TIME_MSEC);
                        writeToLog("Regular shutdown time expired, shutting down hard using System.exit()");
                        System.exit(0);
                    } catch (InterruptedException e) {
                        writeToLog("Received expected interrupt before the shutdown timeout expired");
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
        if (!secretFile.exists()) {
            logger.warn("The shutdown configuration file does not exit. Most likely, the target instance is not running.");
            return;
        }

        String content;
        try {
            content = FileUtils.readFileToString(secretFile);
        } catch (IOException e) {
            logger.warn("Failed to load shutdown configuration file: " + e.getMessage());
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

    private void tryToRemoveInternalProfileDir() {
        // this will only delete the directory if it is empty, so there is no harm in trying
        if (!bootstrapSettings.deleteInternalDataDirectoryIfEmpty()) {
            logger.warn("Failed to remove temporary profile directory " + bootstrapSettings.getInternalDataDirectory()
                + " although it should not contain any files");
        }

    }

    private void writeMessageToConnection(Socket socket, String message) throws IOException {
        PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
        printWriter.print(message);
        printWriter.flush();
    }

}
