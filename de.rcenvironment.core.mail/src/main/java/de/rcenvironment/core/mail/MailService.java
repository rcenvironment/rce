/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.mail;

import java.util.concurrent.Future;

/**
 * Service used for sending e-mails. The SMTP server configuration is done via the external bundle's configuration.
 * 
 * @author Tobias Rodehutskors
 */
public interface MailService {

    /**
     * Asynchronously sends the given {@link Mail} through the configured mail server.
     * 
     * @param mail object to send.
     * @param listener A {@link MailDispatchResultListener} which informs the caller about the state of the mail dispatch.
     * @return Returns a future as a handle of the asynchronous task. The future returns either after a successful mail dispatch or if there
     *         is no more hope for a successful delivery. If this future is canceled, the {@link MailDispatchResultListener} will receive a
     *         {@link MailDispatchResult.FAILURE} result.
     */
    Future<?> sendMail(Mail mail, MailDispatchResultListener listener);

    /**
     * Synchronous call to check if the MailService is properly configured.
     * 
     * @return False, if there is no mail configuration or the configuration is invalid; True, otherwise.
     */
    boolean isConfigured();
}
