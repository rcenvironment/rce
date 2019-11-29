/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.mail.internal;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.rcenvironment.core.mail.InvalidMailException;
import de.rcenvironment.core.mail.Mail;
import static org.hamcrest.CoreMatchers.containsString;

/**
 * Tests for {@link Mail}.
 *
 * @author Tobias Rodehutskors
 */
public class MailTest {

    private static final String INVALID_MAIL_ADDRESS = "hallo?";

    private static final String VALID_MAIL_ADDRESS = "tobias.rodehutskors@dlr.de";

    private static final String HTML_TEXT = "HTML Text";

    private static final String TEXT = "Text";

    private static final String SUBJECT = "Subject";

    /**
     * ExpectedException.
     */
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    /**
     *  Tests, if an email with an valid input can be created successfully.
     * 
     * @throws InvalidMailException unexpected
     */
    @Test
    public void testValidMail() throws InvalidMailException {
        Mail.createMail(new String[] { VALID_MAIL_ADDRESS }, SUBJECT, TEXT, HTML_TEXT);
    }

    /**
     * 
     * Tests, if an email without a recipient is properly rejected.
     * 
     * @throws InvalidMailException expected
     */
    @Test
    public void testMailWithoutRecipients() throws InvalidMailException {
        expectedException.expect(InvalidMailException.class);
        Mail.createMail(new String[] {}, SUBJECT, TEXT, HTML_TEXT);
    }

    /**
     * Tests, if an email with an invalid recipient is properly rejected.
     * 
     * @throws InvalidMailException expected
     */
    @Test
    public void testMailWithInvalidRecipients() throws InvalidMailException {
        expectedException.expect(InvalidMailException.class);
        expectedException.expectMessage(containsString(INVALID_MAIL_ADDRESS));
        Mail.createMail(new String[] { INVALID_MAIL_ADDRESS }, SUBJECT, TEXT, HTML_TEXT);
    }
}
