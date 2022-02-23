/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.Charsets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;

import de.rcenvironment.core.communication.uplink.relay.api.ServerSideUplinkEndpointService;
import de.rcenvironment.core.communication.uplink.relay.api.ServerSideUplinkSession;
import de.rcenvironment.core.communication.uplink.relay.api.ServerSideUplinkSessionService;
import de.rcenvironment.core.embedded.ssh.api.SshAccount;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StreamConnectionEndpoint;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Class for handling the execution of the uplink pseudo-command from the perspective of the embedded SSH server. The only relevant part of
 * this "execution" is grabbing the input and output streams and creating a server-side uplink session by passing these streams to the
 * {@link ServerSideUplinkEndpointService}.
 * 
 * @author Robert Mischke
 */
public class SshUplinkCommandHandler implements Command {

    private InputStream inputStream;

    private ExitCallback callback;

    private OutputStream outputStream;

    private OutputStream errorStream;

    private ServerSideUplinkSessionService serverSideUplinkSessionService;

    private SshAuthenticationManager authenticationManager;

    private boolean terminationSignalSent; // a flag to prevent redundant callback.onExit() calls; synchronized on "this"

    private final Log log = LogFactory.getLog(getClass());

    private final class ConnectionEndpointAdapter implements StreamConnectionEndpoint {

        private final ChannelSession channelSession;

        private boolean closed;

        private ConnectionEndpointAdapter(ChannelSession channelSession) {
            this.channelSession = channelSession;
        }

        @Override
        public OutputStream getOutputStream() {
            return SshUplinkCommandHandler.this.outputStream;
        }

        @Override
        public InputStream getInputStream() {
            return SshUplinkCommandHandler.this.inputStream;
        }

        @Override
        public synchronized void close() {
            if (closed) {
                log.debug("Ignoring a repeated call to close the Uplink pseudo-command handler of SSH session "
                    + System.identityHashCode(channelSession.getSession()));
                return;
            }

            closed = true; // set this early to prevent infinite loops even if any step below fails

            // terminate the Uplink pseudo-command to make the SSHD server close the underlying SSH/TCP connection
            SshUplinkCommandHandler.this.destroy(channelSession);

            // close the command's input stream (the output stream from this perspective) first
            // TODO 10.3.0+ review if this is still needed, or if the pseudo-command exit trigger above is sufficient now
            try {
                if (SshUplinkCommandHandler.this.outputStream != null) {
                    SshUplinkCommandHandler.this.outputStream.close();
                } else {
                    log.warn("Unexpected null outputStream");
                }
            } catch (IOException e) {
                log.debug("Non-critical exception closing the connection output stream before shutdown: " + e);
            }
        }
    }

    public SshUplinkCommandHandler(ServerSideUplinkSessionService serverSideUplinkSessionService,
        SshAuthenticationManager authenticationManager) {
        this.serverSideUplinkSessionService = serverSideUplinkSessionService;
        this.authenticationManager = authenticationManager;
    }

    @Override
    public void start(ChannelSession channelSession, Environment env) throws IOException {
        final String loginAccountName = env.getEnv().get(Environment.ENV_USER);
        final String sessionContextInfoString = StringUtils.format("ssh session %d", System.identityHashCode(channelSession.getSession()));
        final ServerSideUplinkSession session =
            serverSideUplinkSessionService.createServerSideSession(new ConnectionEndpointAdapter(channelSession), loginAccountName,
                sessionContextInfoString);

        SshAccount userAccount = authenticationManager.getAccountByLoginName(loginAccountName, false); // false = do not allow disabled
        if (userAccount == null) {
            writeToStream(errorStream, "Invalid/unknown login name: " + loginAccountName);
            log.warn("Blocked unrecognized SSH account " + loginAccountName);
            sendTerminationSignal(1);
            return;
        }

        if (authenticationManager.isAllowedToUseUplink(loginAccountName)) {
            ConcurrencyUtils.getAsyncTaskService().execute("SSH Uplink server: run session", () -> {
                final boolean terminatedNormally = session.runSession();
                if (terminatedNormally) {
                    sendTerminationSignal(0);
                } else {
                    sendTerminationSignal(1);
                }
            });
        } else {
            log.warn("Blocked uplink access for account " + loginAccountName);
            sendTerminationSignal(1);
        }

    }

    @Override
    public void destroy(ChannelSession channelSession) {
        // TODO 10.3.0+ review: code 0 is also sent if the pseudo-command is terminated due to an error, e.g. a failed heartbeat check
        sendTerminationSignal(0); // note: exit code 0 is only sent if no signal was sent before
    }

    @Override
    public void setInputStream(InputStream in) {
        this.inputStream = in;
    }

    @Override
    public void setOutputStream(OutputStream out) {
        this.outputStream = out;
    }

    @Override
    public void setErrorStream(OutputStream err) {
        this.errorStream = err;
    }

    @Override
    public void setExitCallback(ExitCallback callbackParam) {
        this.callback = callbackParam;
    }

    private void writeToStream(OutputStream stream, String message) throws IOException {
        stream.write(message.getBytes(Charsets.UTF_8));
        stream.flush();
    }

    private void sendTerminationSignal(int exitCode) {
        synchronized (this) {
            if (!terminationSignalSent) {
                terminationSignalSent = true;
                log.debug("Sending SSH command termination signal, exit code " + exitCode);
                callback.onExit(exitCode);
            }
        }
    }
}
