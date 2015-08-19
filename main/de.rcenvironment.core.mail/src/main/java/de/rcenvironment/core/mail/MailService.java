/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.mail;

import java.util.Map;

/**
 * Service used for sending e-mails. The smtp server configuration is done via the external bundle's
 * configuration.
 * 
 * @author Tobias Menden
 */
public interface MailService {

    /**
     * Creates an new {@link Mail} ready for sending.
     * 
     * @param to - set the recipients of the mail, define names of mailing lists and mail addresses
     *        separated by a ";"
     * @param subject of the mail
     * @param text of the mail
     * @param replyTo address in the mail header
     * @return mail object for sendMail method
     */
    Mail createMail(String to, String subject, String text, String replyTo);

    /**
     * Sends the given {@link Mail}. For sending bundle's configuration defining the smpt server
     * settings is used.
     * 
     * @param mail object to send.
     */
    void sendMail(Mail mail);

    /**
     * Returns all mailing lists configured in this bundle's configuration.
     * 
     * @return a {@link Map} with list names (key) and e-mail addresses (value).
     */
    Map<String, String> getMailingLists();

}
