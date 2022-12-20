/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.mail.internal;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.AbstractCommandParameter;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandModifierInfo;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.command.spi.MainCommandDescription;
import de.rcenvironment.core.command.spi.ParsedCommandModifiers;
import de.rcenvironment.core.command.spi.ParsedStringParameter;
import de.rcenvironment.core.command.spi.StringParameter;
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
    
    private static final StringParameter RECIPIENT_PARAMETER = new StringParameter(null, "recipient", "recipient of the e-mail");

    private static final StringParameter SUBJECT_PARAMETER = new StringParameter(null, "subject", "subject of the e-mail");

    private static final StringParameter BODY_PARAMETER = new StringParameter(null, "body", "body of the e-mail");
    
    private Log log = LogFactory.getLog(MailCommandPlugin.class);

    private MailService mailService;

    protected void bindMailService(MailService service) {
        this.mailService = service;
    }

    @Override
    public MainCommandDescription[] getCommands() {
        final MainCommandDescription commands = new MainCommandDescription(CMD_MAIL, "send an email",
            "send an email", this::performSendMail,
            new CommandModifierInfo(
                new AbstractCommandParameter[] {
                    RECIPIENT_PARAMETER,
                    SUBJECT_PARAMETER,
                    BODY_PARAMETER
                }
            )
        );
        return new MainCommandDescription[] { commands };
    }
    
    private void performSendMail(CommandContext context) throws CommandException {
        
        if (!mailService.isConfigured()) {
            context.println("The SMTP mail server is not configured or invalid configured.");
            return;
        }
        
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        String recipient = ((ParsedStringParameter) modifiers.getPositionalCommandParameter(0)).getResult();
        String subject = ((ParsedStringParameter) modifiers.getPositionalCommandParameter(1)).getResult();
        String body = ((ParsedStringParameter) modifiers.getPositionalCommandParameter(2)).getResult();
        
        
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

}
