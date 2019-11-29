/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.ssh.jsch;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;

/**
 * Provides test behavior for incoming commands.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (fixed issue after upgrading to SSHD 2.x)
 */
public class DummyCommand implements Command {

    /** Test constant. */
    public static final String EMPTY_STRING = "";

    protected ExitCallback exitCallback;

    private String stdout;

    private String stderr;

    private int exitValue;

    private OutputStream stdoutStream;

    private OutputStream stderrStream;

    public DummyCommand() {
        this(null, null, 0);
    }

    public DummyCommand(String stdout, String stderr) {
        this(stdout, stderr, 0);
    }

    public DummyCommand(String stdout, String stderr, int exitValue) {
        this.stdout = stdout;
        this.stderr = stderr;
        this.exitValue = exitValue;
    }

    @Override
    public void setInputStream(InputStream in) {}

    @Override
    public void setOutputStream(OutputStream out) {
        this.stdoutStream = out;
    }

    @Override
    public void setErrorStream(OutputStream err) {
        this.stderrStream = err;
    }

    @Override
    public void setExitCallback(ExitCallback callback) {
        this.exitCallback = callback;
    }

    @Override
    public void start(ChannelSession channelSession, Environment env) throws IOException {
        if (stdout != null) {
            stdoutStream.write(stdout.getBytes());
        } else {
            stdoutStream.write(EMPTY_STRING.getBytes());
        }
        if (stderr != null) {
            stderrStream.write(stderr.getBytes());
        } else {
            stderrStream.write(EMPTY_STRING.getBytes());
        }
        stdoutStream.flush();
        stderrStream.flush();
        exitCallback.onExit(exitValue);
    }

    @Override
    public void destroy(ChannelSession channelSession) throws IOException {
        // not necessary for the test to work, but as it is done similarly
        // in live code, do it here too to catch related errors -- misc_ro
        if (stdoutStream != null) {
            stdoutStream.close();
        }
        if (stderrStream != null) {
            stderrStream.close();
        }
    }

}
