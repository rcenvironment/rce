/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.mail.internal;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import jodd.mail.Email;

import org.osgi.framework.BundleContext;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.mail.Mail;
import de.rcenvironment.core.mail.MailService;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.incubator.Assertions;

/**
 * This class is an implementation of the {@link MailService}.
 * 
 * @author Tobias Menden
 */
public final class MailServiceImpl implements MailService {

    private static final String ASSERTION_DEFINED = "The parameter must not be null: ";

    private static final String COLON = ":";

    private static final String COMMA = ";";

    private static ConfigurationService configurationService;

    private static MailConfiguration mailConfiguration;

    protected void activate(BundleContext bundleContext) {
        mailConfiguration = configurationService.getConfiguration(bundleContext.getBundle().getSymbolicName(), MailConfiguration.class);
    }

    protected void deactivate(BundleContext bundleContext) {}

    protected void bindConfigurationService(ConfigurationService newConfigurationService) {
        configurationService = newConfigurationService;
    }

    public static MailConfiguration getMailConfiguration() {
        return mailConfiguration;
    }

    @Override
    public Map<String, String> getMailingLists() {
        return mailConfiguration.getMaillingLists();
    }

    @Override
    public Mail createMail(String to, String subject, String text, String replyTo) throws IllegalArgumentException {
        Assertions.isDefined(to, ASSERTION_DEFINED + "to");
        Assertions.isDefined(subject, ASSERTION_DEFINED + "subject");
        Assertions.isDefined(text, ASSERTION_DEFINED + "text");
        Assertions.isDefined(replyTo, ASSERTION_DEFINED + "replyTo");
        return new Mail(convertRecipients(to), subject, text, replyTo);
    }

    @Override
    public void sendMail(Mail mail) {
        Assertions.isDefined(mail, ASSERTION_DEFINED + "mail");
        String userPass = mailConfiguration.getUserPass();
        if (userPass == null || userPass.isEmpty() || userPass.split(COLON).length != 3) {
            throw new IllegalArgumentException(
                "A user name and password must be configured in order to send e-mails. Please check the congifuration of the mail bundle.");
        }
        for (String to : mail.getRecipients()) {
            Email email = Email.create()
                .to(to)
                .from(mailConfiguration.getUserPass().split(COLON)[0])
                .sentOn(new Date())
                .replyTo(mail.getReplyTo())
                .subject(mail.getSubject())
                .addText(mail.getText());
            MailHandler handler = new MailHandler(email);
            SharedThreadPool.getInstance().execute(handler);
        }
    }

    /**
     * Create a list of final mail recipients out of given list names and addresses.
     * 
     * @param to whom to send, separated by ";"
     * @return list with addresses
     */
    private List<String> convertRecipients(String to) {
        List<String> recipients = new Vector<String>();
        String[] givenRecipients = to.split(COMMA);
        for (int i = 0; i < givenRecipients.length; i++) {
            String recipient = givenRecipients[i];
            Map<String, String> mailAddressMap = mailConfiguration.getMaillingLists();
            if (mailAddressMap.containsKey(recipient)) {
                recipients.addAll(convertRecipients(mailAddressMap.get(recipient)));
            } else {
                if (recipient.contains("@")) {
                    recipients.add(recipient.trim());
                }
            }
        }
        return recipients;
    }

}
