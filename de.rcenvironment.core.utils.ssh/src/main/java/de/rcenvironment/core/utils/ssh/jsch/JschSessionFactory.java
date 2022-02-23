/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.utils.ssh.jsch;

import java.io.File;
import java.security.SignatureException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Logger;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * A factory for configuration-based creation of JSch {@link Session}s. Supported authentication methods are password and SSH keyfile (with
 * an optional passphrase).
 * 
 * @author Robert Mischke
 * @author Brigitte Boden
 */
public final class JschSessionFactory {

    private static final int SERVER_ALIVE_INTERVAL = 5000;

    private static final String MAX_AUTH_TRIES_CONFIG_KEY = "MaxAuthTries";

    private static final String MAX_AUTH_TRIES = "1";

    private static Log log = LogFactory.getLog(JschSessionFactory.class);

    private JschSessionFactory() {
        // prevent instantiation
    }

    /**
     * Simple adapter from com.jcraft.jsch.Logger to Apache Commons Logging.
     * 
     * @author Robert Mischke
     * @author Brigitte Boden
     */
    private static final class ACLDelegate implements Logger {

        private static final String FORWARDED_LOG_LINE_PREFIX = "SSH connection: ";

        private static final String LOG_FILTER_FULL_MESSAGE_1 =
            "CheckCiphers: aes256-ctr,aes192-ctr,aes128-ctr,aes256-cbc,aes192-cbc,aes128-cbc,3des-ctr,arcfour,arcfour128,arcfour256";

        private static final String LOG_FILTER_FULL_MESSAGE_2 =
            "CheckKexes: diffie-hellman-group14-sha1,ecdh-sha2-nistp256,ecdh-sha2-nistp384,ecdh-sha2-nistp521";

        private static final String LOG_FILTER_FULL_MESSAGE_3 =
            "CheckSignatures: ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,ecdsa-sha2-nistp521";

        private static final String LOG_FILTER_FULL_MESSAGE_4 =
            "ssh_rsa_verify: signature true";

        private static final String LOG_FILTER_STARTS_WITH_1 = "kex: ";

        private static final String LOG_FILTER_STARTS_WITH_2 = "expecting ";

        private static final String LOG_FILTER_ENDS_WITH_1 = " sent";

        private static final String LOG_FILTER_ENDS_WITH_2 = " received";

        private final Log apacheCommonsLogger;

        private final int minLevel;

        private ACLDelegate(Log apacheCommonsLogger, int minLevel) {
            this.apacheCommonsLogger = apacheCommonsLogger;
            this.minLevel = minLevel;
        }

        @Override
        public void log(int level, String rawMessage) {
            if (level >= minLevel) {
                if (level == 0) {
                    apacheCommonsLogger.debug(FORWARDED_LOG_LINE_PREFIX + rawMessage + " [L0]");
                } else if (level == 1) {
                    // always log JSch "info" messages as debug, as they are quite verbose
                    // also, filter out certain messages that are usually irrelevant unless JSch DEBUG level is requested
                    if (minLevel > 0) {
                        if (rawMessage.equals(LOG_FILTER_FULL_MESSAGE_1) // clumsy, but probably more efficient than a hash-based lookup
                            || rawMessage.equals(LOG_FILTER_FULL_MESSAGE_2)
                            || rawMessage.equals(LOG_FILTER_FULL_MESSAGE_3)
                            || rawMessage.equals(LOG_FILTER_FULL_MESSAGE_4)
                            || rawMessage.startsWith(LOG_FILTER_STARTS_WITH_1)
                            || rawMessage.startsWith(LOG_FILTER_STARTS_WITH_2)
                            || rawMessage.endsWith(LOG_FILTER_ENDS_WITH_1)
                            || rawMessage.endsWith(LOG_FILTER_ENDS_WITH_2)) {
                            return; // suppress this line
                        }
                    }

                    // omit the level suffix as this is the most frequent case
                    apacheCommonsLogger.debug(FORWARDED_LOG_LINE_PREFIX + rawMessage);

                } else if (level == 2) {
                    // rewrite certain known messages
                    if (rawMessage.startsWith("Permanently added ")) {
                        // we do not consider these messages actual warnings in our log levels
                        // note: substituted and lowered to DEBUG as long as the message is actually misleading; see #0016393
                        apacheCommonsLogger.debug(
                            FORWARDED_LOG_LINE_PREFIX + rawMessage.replace("Permanently added ", "(Disabled) Would permanently add "));
                    } else if (rawMessage.startsWith("an exception ")) {
                        apacheCommonsLogger.debug(
                            FORWARDED_LOG_LINE_PREFIX + rawMessage.replace("an exception during authentication\n",
                                "Caught an exception during authentication: "));
                    } else {
                        apacheCommonsLogger.warn(FORWARDED_LOG_LINE_PREFIX + rawMessage + " [L2]");
                    }
                } else {
                    apacheCommonsLogger.error(FORWARDED_LOG_LINE_PREFIX + rawMessage + "[L" + level + "]"); // never encountered so far
                }
            }
        }

        @Override
        public boolean isEnabled(int level) {
            return level >= minLevel;
        }
    }

    /**
     * A stub class that provides pseudo user responses for password authentication.
     * 
     * @author Robert Mischke
     */
    private static final class UserInfoAdapter implements UserInfo, UIKeyboardInteractive {

        private String pw;

        UserInfoAdapter(String pw) {
            this.pw = pw;
        }

        @Override
        public String getPassphrase() {
            // not expected to be called
            log.warn("SSH called getPassphrase() unexpectedly");
            return null;
        }

        @Override
        public String getPassword() {
            // not expected to be called
            log.warn("SSH called getPassword() unexpectedly");
            return pw;
        }

        @Override
        public boolean promptPassphrase(String arg0) {
            // not expected to be called
            log.warn("SSH login sent a passphrase prompt (answered with no passphrase): " + arg0);
            return false;
        }

        @Override
        public boolean promptPassword(String arg0) {
            log.debug("SSH login sent a password prompt: " + arg0);
            return true;
        }

        @Override
        public boolean promptYesNo(String arg0) {
            // not expected to be called
            log.warn("SSH login sent a yes/no prompt (answered 'no'): " + arg0);
            return false;
        }

        @Override
        public void showMessage(String arg0) {
            log.debug("SSH login sent a message: " + arg0);
        }

        @Override
        public String[] promptKeyboardInteractive(String arg0, String arg1, String arg2, String[] arg3, boolean[] arg4) {
            if (log.isDebugEnabled()) {
                log.debug(StringUtils
                    .format("Simulating keyboard-interactive login; display parameters: %s, %s, %s, %d", arg0, arg1, arg2, arg3.length));
            }
            return new String[] { pw };
        }
    }

    /**
     * @param host the target host to connect to
     * @param port the port number
     * @param user the login username
     * @param keyfileLocation the path of the local keyfile (optional); if null or empty, password auth is used. For convenience, "~" is
     *        automatically resolved to ${user.home}. (NB: This is one of the reasons why a String is used instead of a {@link File}.)
     * @param authPhrase the authentication phrase; if a keyfile is set, this is taken as the keyfile passphase; otherwise, it is used as
     *        the login password
     * @param connectionLogger the JSch-internal logger instance to use
     * @return the initialized JSch session on success
     * @throws SshParameterException on invalid parameters
     * @throws JSchException on SSH errors
     */
    public static Session setupSession(String host, int port, String user, String keyfileLocation, String authPhrase,
        Logger connectionLogger) throws JSchException, SshParameterException {

        // TODO provide a constructor that accepts a SSHSessionConfiguration directly?

        // Retry loop in case of a SignatureException (caused by a bug that occurs randomly).
        // The actual setup is done in the setupSessionInternal method.
        int i = 5;
        do {
            try {
                return setupSessionInternal(host, port, user, keyfileLocation, authPhrase, connectionLogger);
            } catch (JSchException e) {
                Throwable cause = e.getCause();
                String message = e.getMessage();
                if ((cause != null && cause instanceof SignatureException)
                    || (message != null && message.contains("java.security.SignatureException"))) {
                    log.debug("SignatureException occured, retry connecting...");
                    i--;
                    if (i == 0) {
                        // This was the last try, rethrow exception
                        throw e;
                    }
                } else {
                    // Any other Exception should be rethrown.
                    throw e;
                }
            }
        } while (i > 0);
        return null;
    }

    private static Session setupSessionInternal(String host, int port, String user, String keyfileLocation, String authPhrase,
        Logger connectionLogger) throws JSchException, SshParameterException {
        JSch jsch = new JSch();
        Session jschSession = jsch.getSession(user, host, port);
        jschSession.setConfig("StrictHostKeyChecking", "no");

        // sanitize & trim keyfile location
        keyfileLocation = normalizeKeyfilePath(keyfileLocation);

        // validate parameters
        if (host.length() == 0) {
            throw new SshParameterException("The host name or address cannot be empty");
        }
        if (port < 0) {
            throw new SshParameterException("The port must be greater than zero");
        }
        if (user.length() == 0) {
            throw new SshParameterException("The user name cannot be empty");
        }

        if (keyfileLocation.length() == 0) {
            // use password authentication
            if (authPhrase.length() == 0) {
                throw new SshParameterException("The authentication phrase cannot be empty");
            }
            if (log.isDebugEnabled()) {
                log.debug("Setting up JSCH/SSH connection, password authentication, host='"
                    + host + "', user='" + user + "'");
            }
            UserInfo ui = new UserInfoAdapter(authPhrase);
            jschSession.setUserInfo(ui);
        } else {
            // use keyfile authentication
            keyfileLocation = resolveAndVerifyKeyfilePath(keyfileLocation);
            if (log.isDebugEnabled()) {
                log.debug("Setting up JSCH/SSH connection, keyfile authentication, host='"
                    + host + "', user='" + user + "', keyfile='" + keyfileLocation + "'");
            }
            jsch.addIdentity(keyfileLocation, authPhrase);
        }

        JSch.setLogger(connectionLogger);

        jschSession.setServerAliveInterval(SERVER_ALIVE_INTERVAL);

        // Limits the authentication retries to MAX_AUTH_TRIES
        jschSession.setConfig(MAX_AUTH_TRIES_CONFIG_KEY, MAX_AUTH_TRIES);

        jschSession.connect();
        return jschSession;
    }

    /**
     * Creates an SSH {@link Logger} from a provided Apache Commons Logging instance.
     * 
     * @param apacheCommonsLogger the ACL instance to delegate to
     * @return a new SSH logger delegate
     */
    public static Logger createDelegateLogger(final Log apacheCommonsLogger) {
        // TODO consider using the "verbose logging" flag to enable log level 0; never needed so far, though
        return new ACLDelegate(apacheCommonsLogger, Logger.INFO);
    }

    private static String normalizeKeyfilePath(String location) throws SshParameterException {
        // "null" is valid input; treat it like an empty string
        if (location == null) {
            location = "";
        }
        return location.trim();
    }

    private static String resolveAndVerifyKeyfilePath(String location) throws SshParameterException {
        // resolve "~" with home dir
        location = location.replace("~", System.getProperty("user.home"));
        // normalize path
        File sshKeyFile = new File(location);
        location = sshKeyFile.getAbsolutePath();
        // check file existence
        if (!sshKeyFile.isFile()) {
            throw new SshParameterException("SSH keyfile '" + location + "' does not exist");
        }
        return location;
    }
}
