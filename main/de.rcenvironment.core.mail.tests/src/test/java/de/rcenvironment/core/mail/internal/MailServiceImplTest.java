/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.mail.internal;

import java.util.List;

import junit.framework.TestCase;
import de.rcenvironment.core.mail.Mail;
import de.rcenvironment.core.mail.MailMockFactory;
import de.rcenvironment.core.mail.MailTestConstants;

/**
 * Test case for the class {@link MailServiceImpl}.
 * 
 * @author Tobias Menden
 */
public class MailServiceImplTest extends TestCase {

    /**
     * The class under test.
     */
    private MailServiceImpl mailServiceImpl = null;

    @Override
    public void setUp() throws Exception {
        mailServiceImpl = new MailServiceImpl();
        mailServiceImpl.bindConfigurationService(MailMockFactory.getInstance().getConfigurationServiceMock(
            false, MailTestConstants.USER_PASS));
        mailServiceImpl.activate(MailMockFactory.getInstance().getBundleContextMock());
    }

    @Override
    public void tearDown() {
        mailServiceImpl.deactivate(null);
    }

    /**
     * Test method for {@link de.rcenvironment.core.mail.internal.MailServiceImpl#getMailingMap}.
     */
    public void testGetMailingListsForSanity() {
        assertEquals(MailTestConstants.MAIL_ADDRESS_MAP, mailServiceImpl.getMailingLists());
    }

    /**
     * Test method for {@link de.rcenvironment.core.mail.internal.MailServiceImpl#createMail}.
     */
    public void testCreateMailForFailure() {
        try {
            mailServiceImpl.createMail(null,
                MailTestConstants.SUBJECT,
                MailTestConstants.TEXT,
                MailTestConstants.REPLY_TO);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * Test method for {@link de.rcenvironment.core.mail.internal.MailServiceImpl#createMail}.
     */
    public void testCreateMailForSanity() {
        // Mail with multiple recepients
        Mail mail = mailServiceImpl.createMail(MailTestConstants.RECEPIENTS_LIST2,
            MailTestConstants.SUBJECT,
            MailTestConstants.TEXT,
            MailTestConstants.REPLY_TO);
        List<String> mailRecepients = mail.getRecipients();
        if (mailRecepients.contains("listRecepient1@smtp.de")) {
            mailRecepients.remove("listRecepient1@smtp.de");
        }
        if (mailRecepients.contains("listRecepient3@smtp.de")) {
            mailRecepients.remove("listRecepient3@smtp.de");
        }
        if (mailRecepients.contains(MailTestConstants.TEST_ADDRESS)) {
            mailRecepients.remove(MailTestConstants.TEST_ADDRESS);
        }
        if (mailRecepients.contains("another@address.de")) {
            mailRecepients.remove("another@address.de");
        }
        assertTrue(mailRecepients.isEmpty());
        assertEquals(MailTestConstants.REPLY_TO, mail.getReplyTo());
        assertEquals(MailTestConstants.SUBJECT, mail.getSubject());
        assertEquals(MailTestConstants.TEXT, mail.getText());
        // Mail with single recepients
        mail = mailServiceImpl.createMail(MailTestConstants.TEST_ADDRESS,
            MailTestConstants.SUBJECT,
            MailTestConstants.TEXT,
            MailTestConstants.REPLY_TO);
        mailRecepients = mail.getRecipients();
        if (mailRecepients.contains(MailTestConstants.TEST_ADDRESS)) {
            mailRecepients.remove(MailTestConstants.TEST_ADDRESS);
        }
        assertTrue(mailRecepients.isEmpty());
    }

    /**
     * Test method for {@link de.rcenvironment.core.mail.internal.MailServiceImpl#getMailingMap}.
     */
    public void testSendMailForFailure() {
        // Send a null object
        try {
            mailServiceImpl.sendMail(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        Mail mail = mailServiceImpl.createMail(MailTestConstants.RECEPIENTS_LIST2,
            MailTestConstants.SUBJECT,
            MailTestConstants.TEXT,
            MailTestConstants.REPLY_TO);
        // Send without SSL
        mailServiceImpl.sendMail(mail);
        // Send with broken userPass1
        mailServiceImpl.bindConfigurationService(MailMockFactory.getInstance().getConfigurationServiceMock(
            true, ":user"));
        mailServiceImpl.activate(MailMockFactory.getInstance().getBundleContextMock());
        try {
            mailServiceImpl.sendMail(mail);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        // Send with broken userPass2
        mailServiceImpl.bindConfigurationService(MailMockFactory.getInstance().getConfigurationServiceMock(
            true, ":user:pass"));
        mailServiceImpl.activate(MailMockFactory.getInstance().getBundleContextMock());
        try {
            mailServiceImpl.sendMail(mail);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        // Send with SSL
        mailServiceImpl.bindConfigurationService(MailMockFactory.getInstance().getConfigurationServiceMock(
            true, MailTestConstants.USER_PASS));
        mailServiceImpl.activate(MailMockFactory.getInstance().getBundleContextMock());
        mailServiceImpl.sendMail(mail);
    }

}
