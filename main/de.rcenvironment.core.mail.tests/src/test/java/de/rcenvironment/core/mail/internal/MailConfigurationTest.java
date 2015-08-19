/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.mail.internal;

import junit.framework.TestCase;
import de.rcenvironment.core.mail.MailTestConstants;

/**
 * Test case for the class {@link MailConfiguration}.
 * 
 * @author Tobias Menden
 */
public class MailConfigurationTest extends TestCase {

    private static final int DEFAULT_SSL_PORT = 465;

    /**
     * The class under test.
     */
    private MailConfiguration myMailSettings = null;

    @Override
    public void setUp() throws Exception {
        myMailSettings = new MailConfiguration();
    }

    /**
     * Test method for {@link de.rcenvironment.core.mail.internal.MailConfiguration} Default Values.
     */
    public void testDefaultValues() {
        assertTrue(myMailSettings.getMaillingLists().isEmpty());
        assertEquals("smtp.dlr.de", myMailSettings.getSmtpServer());
        assertNull(myMailSettings.getUserPass());
        assertFalse(myMailSettings.getUseSSL());
        assertEquals(DEFAULT_SSL_PORT, myMailSettings.getSslPort());
    }

    /**
     * Test method for {@link de.rcenvironment.core.mail.internal.MailConfiguration} Getter Methods.
     */
    public void testGettterForSuccess() {
        myMailSettings.setMaillingLists(MailTestConstants.MAIL_ADDRESS_MAP);
        myMailSettings.setSmtpServer(MailTestConstants.SMTP_SERVER);
        myMailSettings.setUserPass(MailTestConstants.USER_PASS);
        myMailSettings.setUseSSL(MailTestConstants.USE_SSL);
        myMailSettings.setSslPort(MailTestConstants.SSL_PORT);

        assertTrue(myMailSettings.getMaillingLists().equals(MailTestConstants.MAIL_ADDRESS_MAP));
        assertEquals(MailTestConstants.SMTP_SERVER, myMailSettings.getSmtpServer());
        assertEquals(MailTestConstants.USER_PASS, myMailSettings.getUserPass());
        assertTrue(myMailSettings.getUseSSL());
        assertEquals(MailTestConstants.SSL_PORT, myMailSettings.getSslPort());
    }

}
