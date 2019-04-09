/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.mail.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;

import org.junit.Rule;
import org.junit.rules.ExpectedException;

import com.icegreen.greenmail.util.GreenMailUtil;

import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.testutils.ConfigurationSegmentUtils;
import de.rcenvironment.core.configuration.testutils.TestConfigurationProvider;
import de.rcenvironment.core.mail.InvalidMailException;
import de.rcenvironment.core.mail.Mail;
import de.rcenvironment.core.mail.SMTPServerConfiguration;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;


/**
 * Some common utility methods for mail service testing.
 *
 * @author Tobias Rodehutskors
 */
public class AbstractMailServiceImplTest {

    private static final String MAIL_RECIPIENT = "tobias.rodehutskors@dlr.de";

    private static final String MAIL_SUBJECT = "subject";

    private static final String MAIL_TEXT = "text";

    private static final String MAIL_HTML = "<html><head><title>Title</title></head><body>Body</body></html>";

    /**
     * ExpectedException.
     */
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    /**
     * The class under test.
     */
    protected MailServiceImpl mailServiceImpl = null;

    protected boolean setupMailService(String configurationLocation) throws IOException {

        mailServiceImpl = new MailServiceImpl();

        // load a configuration segment from the resources ...
        TempFileServiceAccess.setupUnitTestEnvironment();
        ConfigurationSegment configurationSegment =
            ConfigurationSegmentUtils.readTestConfigurationFromStream(getClass().getResourceAsStream(configurationLocation));

        // ... and make it available to the service
        TestConfigurationProvider configurationProvider = new TestConfigurationProvider();
        configurationProvider.setConfigurationSegment(SMTPServerConfiguration.CONFIGURATION_PATH, configurationSegment);
        mailServiceImpl.bindConfigurationService(configurationProvider);
        mailServiceImpl.activate();

        return mailServiceImpl.isConfigured();
    }
    
    protected Mail createDefaultMail() throws InvalidMailException {
        return Mail.createMail(new String[] { MAIL_RECIPIENT }, MAIL_SUBJECT, MAIL_TEXT, MAIL_HTML);
    }
    
    protected void validateDefaultMail(Message[] receivedMessages) throws MessagingException, IOException {
        assertEquals(1, receivedMessages.length);
        assertEquals(MAIL_SUBJECT, receivedMessages[0].getSubject());
        Multipart mmp = (Multipart) receivedMessages[0].getContent();
        String content = GreenMailUtil.getBody(mmp.getBodyPart(0)).trim();
        assertTrue(content.contains(MAIL_TEXT));
        assertTrue(content.contains(MAIL_HTML));
    }
}
