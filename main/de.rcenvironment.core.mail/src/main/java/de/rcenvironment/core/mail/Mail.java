/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.mail;

import jodd.mail.Email;
import jodd.mail.EmailAddress;
import jodd.mail.MailAddress;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * This class is a utility to make sure that only valid mails can be created.
 *
 * @author Tobias Rodehutskors
 */
public final class Mail {

    private Email email;

    /**
     * Private constructor to avoid creation of unvalidated instances.
     */
    private Mail() {

    }

    /**
     * Creates a new Mail that is valid. If the mail cannot be created, since the given parameters are not valid, a
     * {@link InvalidMailException} will be thrown.
     * 
     * @param recipients An array of recipient email addresses. All these recipients will be added to the TO field of the created mail. At
     *        least one recipient has to be configured.
     * @param subject The required subject of this mail.
     * @param text The text of the mail. You need to specify at least one of text and htmlText.
     * @param htmlText The HTML text of the mail. You need to specify at least one of text and htmlText.
     * @throws InvalidMailException On validation errors during the construction of this mail.
     * @return The validated mail.
     */
    public static Mail createMail(String[] recipients, String subject, String text, String htmlText)
        throws InvalidMailException {

        if (recipients == null || recipients.length < 1) {
            throw new InvalidMailException("You need to configure at least one valid recipient.");
        }

        Email tmpEmail = new Email();

        // verify that all recipients are valid mail addresses
        for (String recipient : recipients) {
            EmailAddress recipientEmailAddress = new EmailAddress(recipient);
            if (!recipientEmailAddress.isValid()) {
                throw new InvalidMailException(StringUtils.format("The email address %s is not valid.", recipient));
            }

            tmpEmail.addTo(new MailAddress(recipientEmailAddress));
        }

        if (subject == null) {
            throw new InvalidMailException("You need to specify a subject.");
        }
        tmpEmail.subject(subject);

        if (text == null && htmlText == null) {
            throw new InvalidMailException("You need to specify either a text or a HTML text.");
        }

        if (text != null) {
            tmpEmail.addText(text);
        }

        if (htmlText != null) {
            tmpEmail.addHtml(htmlText);
        }

        Mail mail = new Mail();
        mail.email = tmpEmail;
        return mail;
    }

    public Email getMail() {
        return this.email;
    }
}
