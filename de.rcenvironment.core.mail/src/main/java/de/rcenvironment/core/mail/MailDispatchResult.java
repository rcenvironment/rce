/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.mail;

/**
 * Enum to indicate the state of an attempted mail dispatch.
 *
 * @author Tobias Rodehutskors
 */
public enum MailDispatchResult {

    /**
     * The mail was successfully delivered to the mail server. This does not necessarily mean that the mail reached the desired recipient,
     * since the mail is delivered to the mail sender even if the recipient's mail address does not exist.
     */
    SUCCESS,

    /**
     * The mail was not sent. Most likely the configuration of the SMTP mail server is not correct. No Retry.
     */
    FAILURE,

    /**
     * The mail was not sent but another attempt will be done.
     */
    FAILURE_RETRY,

    /**
     * The MailService is not configured. Mail was not send. No retry.
     */
    FAILURE_MAIL_SERVICE_NOT_CONFIGURED;
}
