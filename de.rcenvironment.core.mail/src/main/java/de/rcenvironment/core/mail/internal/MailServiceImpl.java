/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.mail.internal;

import java.util.concurrent.Future;

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;

import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.configuration.ConfigurationException;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.bootstrap.RuntimeDetection;
import de.rcenvironment.core.mail.Mail;
import de.rcenvironment.core.mail.MailDispatchResult;
import de.rcenvironment.core.mail.MailDispatchResultListener;
import de.rcenvironment.core.mail.MailService;
import de.rcenvironment.core.mail.SMTPServerConfiguration;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;
import jodd.mail.Email;
import jodd.mail.MailException;
import jodd.mail.SendMailSession;
import jodd.mail.SmtpSslServer;

/**
 * This class is an implementation of the {@link MailService}.
 * 
 * @author Tobias Rodehutskors
 */
public final class MailServiceImpl implements MailService {

    private Log log = LogFactory.getLog(MailServiceImpl.class);

    private ConfigurationService configurationService;

    private SMTPServerConfiguration mailConfiguration;

    // False, if there is no mail configuration or the configuration is invalid; True, otherwise.
    private boolean configured = false;

    private boolean trustAllCertificates = false;

    /**
     * OSGi-DS life-cycle method.
     */
    public void activate() {
        if (RuntimeDetection.isImplicitServiceActivationDenied()) {
            // do not activate this service if is was spawned as part of a default test environment
            return;
        }

        ConfigurationSegment configurationSegment =
            configurationService.getConfigurationSegment(SMTPServerConfiguration.CONFIGURATION_PATH);
        if (configurationSegment == null || !configurationSegment.isPresentInCurrentConfiguration()) {
            configured = false;
            log.debug("MailService not started as there is no mail configuration at all.");
        } else {
            mailConfiguration =
                new SMTPServerConfiguration(configurationSegment, SMTPServerConfiguration.getMailFilterInformation(configurationService));

            try {
                mailConfiguration.isValid(); // throws an exception if the configuration is invalid
                configured = true;
                log.debug("MailService is configured.");
            } catch (ConfigurationException e) {
                configured = false;
                log.error(StringUtils.format("MailService is not successfully configured since the configuration is invalid: %s ",
                    e.getMessage()));
            }
        }
    }

    // public void deactivate() {
    // TODO cancel all futures of the MailSendingHandler if the service is deactivated
    // TODO and write a test for that
    // }

    protected void bindConfigurationService(ConfigurationService newConfigurationService) {
        this.configurationService = newConfigurationService;
    }

    @Override
    public Future<?> sendMail(Mail mail, MailDispatchResultListener listener) {

        if (!configured) {
            listener.receiveResult(MailDispatchResult.FAILURE_MAIL_SERVICE_NOT_CONFIGURED, null);
            return ConcurrentUtils.constantFuture(null);
        }

        MailSendingHandler mailSendingTask = new MailSendingHandler(mail, listener);
        return ConcurrencyUtils.getAsyncTaskService().submit(mailSendingTask);
    }

    /**
     * This runnable is used to send mails asynchronously.
     */
    private class MailSendingHandler implements Runnable {

        private static final String CAN_T_SEND_COMMAND_TO_SMTP_HOST = "Can't send command to SMTP host";

        private static final String INTERRUPTED_WHILE_WAITING = "Mail delivery was interrupted while waiting for the next attempt.";

        private static final int SEC_TO_MILLIS = 1000;

        private static final String SEND_FAILED = "Sending mail failed.";

        private static final int AUTO_RETRY_INITIAL_DELAY_MILLIS = 5000;

        private static final double AUTO_RETRY_DELAY_MULTIPLIER = 1.5;

        private static final int AUTO_RETRY_MAX_DELAY_MILLIS = 300000;

        private Email mail;

        private MailDispatchResultListener listener;

        private int consecutiveConnectionFailures;

        MailSendingHandler(Mail mail, MailDispatchResultListener listener) {
            this.mail = mail.getMail();
            this.listener = listener;
            consecutiveConnectionFailures = 0;
        }

        @Override
        @TaskDescription("Sending mail")
        public void run() {
            boolean success = false;

            // flag to indicate whether we should retry the mail delivery
            boolean permFailed = false;
            String permFailedMessage = null;

            while (!success && !permFailed) {

                // try to send the mail
                try {
                    smtpServerAction(mail);
                    success = true;
                } catch (MailException e) {

                    log.error(SEND_FAILED, e);
                    success = false;
                    consecutiveConnectionFailures++;

                    permFailed = isMailExceptionCausedByPermError(e);
                    if (permFailed) {
                        permFailedMessage = e.getCause().getMessage();
                    }
                }

                // notify the listener about the result of the mail delivery attempt
                if (listener != null) {
                    if (success) {
                        listener.receiveResult(MailDispatchResult.SUCCESS, null);
                    } else {
                        if (permFailed) {
                            log.debug(StringUtils.format("Sending %s to MailDispatchResultListener.",
                                MailDispatchResult.FAILURE.toString()));
                            listener.receiveResult(MailDispatchResult.FAILURE, permFailedMessage);
                        } else {
                            listener.receiveResult(MailDispatchResult.FAILURE_RETRY, null);
                        }

                    }
                }

                // sleep some time if we should retry
                if (!success && !permFailed) {
                    try {
                        long autoRetryDelay = calculateNextAutoRetryDelay();
                        log.debug(StringUtils.format("Retrying mail delivery in %d seconds.", (autoRetryDelay / SEC_TO_MILLIS)));
                        Thread.sleep(autoRetryDelay);
                        log.debug("Thread.currentThread().isInterrupted(): " + Thread.currentThread().isInterrupted());
                    } catch (InterruptedException e) {
                        log.debug(INTERRUPTED_WHILE_WAITING);
                        listener.receiveResult(MailDispatchResult.FAILURE, INTERRUPTED_WHILE_WAITING);
                        return;
                    }
                }
            }
        }

        private long calculateNextAutoRetryDelay() {
            long targetDelay =
                Math.round(AUTO_RETRY_INITIAL_DELAY_MILLIS * Math.pow(AUTO_RETRY_DELAY_MULTIPLIER, consecutiveConnectionFailures - 1));

            // apply upper limit, if set
            targetDelay = Math.min(targetDelay, AUTO_RETRY_MAX_DELAY_MILLIS);
            return targetDelay;
        }

        /**
         * We need to decide if it worth to attempt a new delivery or if the failure indicates a permanent error like a configuration
         * mistake. For this purpose this method evaluates the cause of a MailExeption.
         * 
         * @param e MailExeption whose cause should be analyzed.
         * @return True in case of an configuration error; False, otherwise.
         */
        private boolean isMailExceptionCausedByPermError(MailException e) {

            Throwable cause = e.getCause();

            if (cause != null) {

                // if we receive a MessagingException we not not try to redeliver the mail...
                // ... except if the detail message is the specified one
                // this check is used instead of instanceof to be sure to only match MessagingExceptions and not its subclasses
                if ((cause.getClass().equals(MessagingException.class)
                    && !(cause.getMessage().equals(CAN_T_SEND_COMMAND_TO_SMTP_HOST)))

                    // if we receive an AuthenticationFailedException we do not try to redeliver the mail
                    // AuthenticationFailedException extends MessagingException
                    || cause instanceof AuthenticationFailedException) {

                    // configuration error
                    return true;
                }

            }

            // no configuration error
            return false;
        }
    }

    /**
     * @return True, if a connection to the mail server can be established.
     */
    public boolean canConnectToServer() {

        if (!configured) {
            log.error("SMTP server is not configured.");
            return false;
        }

        try {
            smtpServerAction(null);
        } catch (MailException e) {
            log.error("Unable to contact the configured SMTP server.", e);
            return false;
        }

        return true;
    }

    /**
     * Sends a mail to the configured mail server.
     * 
     * @param mail If null, this method will still connect to the server, but not send a mail.
     * @throws MailException Can have one of the following causes:
     * 
     *         <table border="1">
     *         <tbody>
     *         <tr>
     *         <th>Cause exception</th>
     *         <th>&nbsp;Nested exception</th>
     *         <th>Detail message</th>
     *         <th>Reason</th>
     *         </tr>
     *         <tr>
     *         <td>com.sun.mail.util.MailConnectException</td>
     *         <td>java.net.UnknownHostException</td>
     *         <td>TODO</td>
     *         <td>Host might be temporary offline OR host does not exist at all.</td>
     *         </tr>
     *         <tr>
     *         <td>com.sun.mail.util.MailConnectException</td>
     *         <td>java.net.ConnectException</td>
     *         <td>TODO</td>
     *         <td>High workload OR wrong port is configured</td>
     *         </tr>
     *         <tr>
     *         <td>javax.mail.AuthenticationFailedException</td>
     *         <td>&nbsp;</td>
     *         <td>TODO</td>
     *         <td>User name and/or password are not correct</td>
     *         </tr>
     *         <tr>
     *         <td>javax.mail.MessagingException</td>
     *         <td>&nbsp;</td>
     *         <td>TODO</td>
     *         <td>Connecting with explicit encryption to a port expecting implicit encryption</td>
     *         </tr>
     *         <tr>
     *         <td>javax.mail.MessagingException</td>
     *         <td>javax.net.ssl.SSLException</td>
     *         <td>TODO</td>
     *         <td>Connecting with implicit encryption to a port expecting explicit encryption</td>
     *         </tr>
     *         <tr>
     *         <td>javax.mail.MessagingException</td>
     *         <td>next: SocketException</td>
     *         <td>Can't send command to SMTP host</td>
     *         <td>The SMTP server closed the connection, e.g. because of rate limits. Retry later.</td>
     *         </tr>
     *         </tbody>
     *         </table>
     */
    private void smtpServerAction(Email mail) throws MailException {

        SendMailSession session = null;
        try {

            SmtpSslServer server = new SmtpSslServer(mailConfiguration.getHost(), mailConfiguration.getPort())
                .authenticateWith(mailConfiguration.getUsername(), mailConfiguration.getPassword());

            // if the encryption is not set to 'explicit', 'implicit' mode is automatically assumed which requires no special flags
            if (SMTPServerConfiguration.EXPLICIT_ENCRYPTION.equals(mailConfiguration.getEncryption())) {
                // if STARTTLS is not supported by the server, session opening should fail
                server.startTlsRequired(true);
                // needed in conjunction with startTlsRequired
                server.plaintextOverTLS(true);
            }

            if (trustAllCertificates) {
                server.property("mail.smtp.ssl.trust", "*");
            }

            // server.debug(true);
            session = server.createSession();
            session.open();

            if (mail != null) {
                mail.from(mailConfiguration.getSender());
                session.sendMail(mail);
            }
        } finally {
            if (session != null) {
                session.close();
            }
        }
    };

    @Override
    public boolean isConfigured() {
        return configured;
    }

    /**
     * NOT SECURE! This method is only intended to be used in unit test. Do not call this function in production code!
     * 
     * Disables SSL certificate checks for all connections.
     */
    void trustAllCertificates() {
        trustAllCertificates = true;
    }
}
