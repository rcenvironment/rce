/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.mail.internal;

import de.rcenvironment.core.mail.Mail;
import de.rcenvironment.core.mail.MailTestConstants;
import junit.framework.TestCase;

/**
 * Test case for the class {@link Mail}.
 * 
 * @author Tobias Menden
 */
public class MailTest extends TestCase {

    /**
     * The class under test.
     */
    private Mail myMail = null;

    @Override
    public void setUp() throws Exception {
        myMail = new Mail(MailTestConstants.TO,
            MailTestConstants.SUBJECT,
            MailTestConstants.TEXT,
            MailTestConstants.REPLY_TO
        );
    }

    /**
     * Test method for {@link de.rcenvironment.core.mail.internal.MailConfiguration} Getter Methods.
     */
    public void testGettterForSuccess() {
        assertTrue(myMail.getReplyTo().equals(MailTestConstants.REPLY_TO));
        assertTrue(myMail.getSubject().equals(MailTestConstants.SUBJECT));
        assertTrue(myMail.getText().equals(MailTestConstants.TEXT));
        assertEquals(MailTestConstants.TO, myMail.getRecipients());
    }

}
