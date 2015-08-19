/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.mail.internal;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jodd.mail.Email;
import junit.framework.TestCase;
import de.rcenvironment.core.mail.MailMockFactory;
import de.rcenvironment.core.mail.MailTestConstants;

/**
 * Test case for the class {@link MailHandler}.
 * 
 * @author Tobias Menden
 */
public class MailHandlerTest extends TestCase {

    /**
     * The class under test.
     */
    private MailHandler mailSendThread = null;

    private MailServiceImpl mailServiceImpl;

    private ExecutorService executor;

    @Override
    public void setUp() throws Exception {
        mailServiceImpl = new MailServiceImpl();
        mailServiceImpl.bindConfigurationService(MailMockFactory.getInstance().getConfigurationServiceMock(
            false, MailTestConstants.USER_PASS));
        mailServiceImpl.activate(MailMockFactory.getInstance().getBundleContextMock());
        executor = Executors.newFixedThreadPool(10);
    }

    /**
     * Test method for {@link de.rcenvironment.core.mail.internal.MailHandler#MailSendThread}.
     */
    public void testMailSendThread() {
        new Email();
        Email email = Email.create()
            .from(MailTestConstants.FROM)
            .sentOn(new Date())
            .replyTo(MailTestConstants.REPLY_TO)
            .subject(MailTestConstants.SUBJECT)
            .addText(MailTestConstants.TEXT);
        for (String addRecepient : MailTestConstants.TO) {
            email.to(addRecepient);
        }
        mailSendThread = new MailHandler(email);
    }

    /**
     * Test method for {@link de.rcenvironment.core.mail.internal.MailHandler#run}.
     */
    public void testRun() {
        // run without ssl
        testMailSendThread();
        executor.execute(mailSendThread);
        // run with ssl
        mailServiceImpl.bindConfigurationService(MailMockFactory.getInstance().getConfigurationServiceMock(
            true, MailTestConstants.USER_PASS));
        mailServiceImpl.activate(MailMockFactory.getInstance().getBundleContextMock());
        testMailSendThread();
        executor.execute(mailSendThread);
    }
}
