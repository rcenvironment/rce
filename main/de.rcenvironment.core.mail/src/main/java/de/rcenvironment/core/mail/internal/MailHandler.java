/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.mail.internal;

import jodd.exception.UncheckedException;
import jodd.mail.Email;
import jodd.mail.MailException;
import jodd.mail.SendMailSession;
import jodd.mail.SimpleAuthenticator;
import jodd.mail.SmtpServer;
import jodd.mail.SmtpSslServer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Class handling sending of e-mails. To allow asynchronous execution, this class implements the
 * {@link Runnable} interface so a created object can be executed using a new thread.
 * 
 * @author Tobias Menden
 */
public class MailHandler implements Runnable {
    
    private static final String EMPTY = "";

    private static final Log LOGGER = LogFactory.getLog(MailHandler.class);

    private static final String SEND_FAILED = "Sending mail failed.";
    
    private static final String USERPASS_CONF =
        "Wrong username, password, e-mail address. Please check the configuration of the mail bundle.";

    private static final String COLON = ":";

    private static MailConfiguration mailConfiguration;

    private SmtpServer smtpServer;

    private Email mail;

    public MailHandler(Email mail) {
        this.mail = mail;
        mailConfiguration = MailServiceImpl.getMailConfiguration();
        String[] user = mailConfiguration.getUserPass().split(COLON);
        if (user.length != 3) {
            throw new IllegalArgumentException(USERPASS_CONF);
        } else if (user[0].equals(EMPTY) || user[1].equals(EMPTY) || user[2].equals(EMPTY)) {
            throw new IllegalArgumentException(USERPASS_CONF);
        }
    }

    @Override
    public void run() {
        SendMailSession session = null;
        try {
            session = openSession();
            session.sendMail(mail);
        } catch (MailException e) {
            LOGGER.error(SEND_FAILED, e);
        } catch (UncheckedException e) {
            LOGGER.error(SEND_FAILED, e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    private SendMailSession openSession() throws MailException, UncheckedException {
        String[] user = mailConfiguration.getUserPass().split(COLON);
        if (!mailConfiguration.getUseSSL()) {
            smtpServer = new SmtpServer(mailConfiguration.getSmtpServer(),
                new SimpleAuthenticator(user[1], user[2]));
        } else {
            smtpServer = new SmtpSslServer(mailConfiguration.getSmtpServer(),
                mailConfiguration.getSslPort(),
                new SimpleAuthenticator(user[1], user[2]));
        }
        SendMailSession session = smtpServer.createSession();
        session.open();
        return session;
    }
}
