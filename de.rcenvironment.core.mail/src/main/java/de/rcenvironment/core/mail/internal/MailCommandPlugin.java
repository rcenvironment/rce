/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.mail.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import jodd.mail.EmailAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandDescription;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.mail.InvalidMailException;
import de.rcenvironment.core.mail.Mail;
import de.rcenvironment.core.mail.MailDispatchResult;
import de.rcenvironment.core.mail.MailDispatchResultListener;
import de.rcenvironment.core.mail.MailService;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * A {@link CommandPlugin} providing a mail command to send mails from the command console.
 * 
 * @author Tobias Rodehutskors
 */
public class MailCommandPlugin implements CommandPlugin {

    private static final String PERM_FAILURE =
        "Unable to delivered the mail to the mail server. Most likely the SMTP server configuration is wrong: %s";

    private static final String CMD_MAIL = "mail";

    private Log log = LogFactory.getLog(MailCommandPlugin.class);

    private MailService mailService;

    protected void bindMailService(MailService service) {
        this.mailService = service;
    }

    @Override
    public void execute(final CommandContext context) throws CommandException {

        context.consumeExpectedToken(CMD_MAIL);

        if (!mailService.isConfigured()) {
            context.println("The SMTP mail server is not configured or invalid configured.");
            // ignore additional tokens
            context.consumeRemainingTokens();
            return;
        }

        String recipient = context.consumeNextToken();
        if (recipient == null || !(new EmailAddress(recipient).isValid())) {
            throw CommandException.syntaxError("You need to specify a valid mail address as the recipient.", context);
        }

        String subject = context.consumeNextToken();
        if (subject == null) {
            throw CommandException.syntaxError("You need to specify a subject.", context);
        }

        String body = context.consumeNextToken();
        if (body == null) {
            throw CommandException.syntaxError("You need the message body.", context);
        }

        // ignore additional tokens
        context.consumeRemainingTokens();

        Mail validatedMail;
        try {
            validatedMail = Mail.createMail(new String[] { recipient }, subject, body, null);
        } catch (InvalidMailException e) {
            // unlikely that this is going to happen, since we already have validated the input
            throw CommandException.executionError(e.getMessage(), context);
        }

        Future<?> sendMailFuture = mailService.sendMail(validatedMail, new MailDispatchResultListener() {

            @Override
            public void receiveResult(MailDispatchResult result, String message) {
                switch (result) {
                case SUCCESS:
                    context.println("Successfully delivered the mail to the mail server.");
                    break;
                case FAILURE:
                    context.println(StringUtils.format(PERM_FAILURE, message));
                    break;
                case FAILURE_RETRY:
                    context.println("Unable to delivered the mail to the mail server. Retrying...");
                    break;
                default:
                    log.warn("Received unexpected result from the mail service.");
                    break;
                }
            }
        });

        try {
            sendMailFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error(e);
            throw CommandException.executionError(e.getMessage(), context);
        }

    }

    @Override
    public Collection<CommandDescription> getCommandDescriptions() {
        final Collection<CommandDescription> contributions = new ArrayList<CommandDescription>();
        contributions.add(new CommandDescription(CMD_MAIL, "<recipient> \"<subject>\" \"<body>\"", false, "Sends an email.",
            "<recipient> - The recipient to whom the mail should be addressed.",
            "\"<subject>\" - The subject of the mail.",
            "\"<body>\" - The mail body."));
        return contributions;
    }

}
