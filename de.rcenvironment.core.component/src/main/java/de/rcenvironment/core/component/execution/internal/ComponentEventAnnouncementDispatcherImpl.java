/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.execution.api.ComponentEventAnnouncement;
import de.rcenvironment.core.component.execution.api.ComponentEventAnnouncementDispatcher;
import de.rcenvironment.core.mail.InvalidMailException;
import de.rcenvironment.core.mail.Mail;
import de.rcenvironment.core.mail.MailDispatchResult;
import de.rcenvironment.core.mail.MailDispatchResultListener;
import de.rcenvironment.core.mail.MailService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.toolkit.utils.text.TextLinesReceiver;

/**
 * Default implementation of {@link ComponentEventAnnouncementDispatcher}.
 * 
 * @author Doreen Seider
 */
public class ComponentEventAnnouncementDispatcherImpl implements ComponentEventAnnouncementDispatcher {

    private static final String SUBJECT_PATTERN = "[RCE] %s";

    private MailService mailService;

    @Override
    public boolean dispatchWorkflowEventAnnouncementViaMail(String[] recipients, final ComponentEventAnnouncement compEventAnnouncement,
        final TextLinesReceiver errorTextReceiver) {
        String subject;
        if (compEventAnnouncement.hasSubject()) {
            subject = StringUtils.format(SUBJECT_PATTERN, compEventAnnouncement.getSubject());
        } else {
            subject = StringUtils.format(SUBJECT_PATTERN, "no subject");
        }

        Mail mail;
        try {
            mail = Mail.createMail(recipients, subject, compEventAnnouncement.getBody(), null);
        } catch (InvalidMailException e) {
            errorTextReceiver.addLine("Failed to send mail: " + e.getMessage());
            return false;
        }

        final AtomicBoolean success = new AtomicBoolean(false);

        Future<?> mailSendFuture = mailService.sendMail(mail, new MailDispatchResultListener() {

            @Override
            public void receiveResult(MailDispatchResult result, String message) {
                if (message == null) {
                    message = ""; // to keep the code simple, should be improved to get "clearer" log messages
                }
                switch (result) {
                case SUCCESS:
                    success.set(true);
                    break;
                case FAILURE:
                    errorTextReceiver.addLine(
                        StringUtils.format("Failed to deliver email to mail server: '%s'; %s",
                            compEventAnnouncement.getWorkflowEventType().getDisplayName(), message));
                    break;
                case FAILURE_RETRY:
                    errorTextReceiver.addLine(StringUtils.format("Failed to deliver email to mail server: '%s'; %s; retrying...",
                        compEventAnnouncement.getWorkflowEventType().getDisplayName(), message));
                    break;
                case FAILURE_MAIL_SERVICE_NOT_CONFIGURED:
                    errorTextReceiver
                        .addLine(StringUtils.format("Failed to deliver email to mail server: '%s'; cause: mail server is not "
                            + "configured; %s", compEventAnnouncement.getWorkflowEventType().getDisplayName(), message));
                    break;
                default:
                    LogFactory.getLog(getClass()).error("Received unexpected result from the mail service.");
                    break;
                }

            }
        });

        try {
            mailSendFuture.get();
        } catch (InterruptedException e) {
            // make sure the mail sending task is canceled, if this waiting thread gets interrupted 
            mailSendFuture.cancel(true);
            
            LogFactory.getLog(getClass())
                .warn(StringUtils.format("Interrupted while waiting for mail to be delivered that announces the component event '%s'",
                    compEventAnnouncement.getWorkflowEventType()));
        } catch (ExecutionException e) {
            LogFactory.getLog(getClass())
                .error(StringUtils.format("Error when delivering mail to mail server that announces the component event '%s'",
                    compEventAnnouncement.getWorkflowEventType()), e);
        }
        return success.get();
    }

    protected void bindMailService(MailService service) {
        this.mailService = service;
    }

}
