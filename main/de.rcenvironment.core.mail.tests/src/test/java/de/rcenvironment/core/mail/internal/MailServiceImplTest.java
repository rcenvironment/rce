/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.mail.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.mail.MessagingException;

import org.easymock.EasyMock;
import org.junit.Ignore;
import org.junit.Test;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

import de.rcenvironment.core.mail.InvalidMailException;
import de.rcenvironment.core.mail.MailDispatchResult;
import de.rcenvironment.core.mail.MailDispatchResultListener;

/**
 * Test case for the class {@link MailServiceImpl}.
 * 
 * @author Tobias Rodehutskors
 */
public class MailServiceImplTest extends AbstractMailServiceImplTest {

    private static final String GREENMAIL_IMPLICIT_ENCRYPTION_JSON = "/greenmail_implicit_encryption.json";
    
    private static final String NOT_IMPLEMENTED_YET = "Not implemented yet";

    private static final int TWO_SECS_IN_MILLIS = 2000;

    /**
     * Tests if the mail dispatch fails with an incomplete configuration, which only specifies the host and port but nothing else.
     * 
     * @throws InterruptedException unexpected
     * @throws ExecutionException unexpected
     * @throws IOException unexpected
     * @throws InvalidMailException unexpected
     */
    @Test
    public void testSendMailWithInvalidConfig() throws InterruptedException, ExecutionException, IOException, InvalidMailException {
        boolean configured = setupMailService("/invalidConfig.json");
        assertFalse(configured);

        Future<?> sendMail = mailServiceImpl.sendMail(createDefaultMail(), new MailDispatchResultListener() {

            @Override
            public void receiveResult(MailDispatchResult result, String message) {
                assertEquals(MailDispatchResult.FAILURE_MAIL_SERVICE_NOT_CONFIGURED, result);
                assertEquals(null, message);
            }
        });
        sendMail.get();
    }

    /**
     * This test will attempt to establish a connection with explicit security using the STARTTLS command. However, the server does not
     * support STARTTLS. Therefore, we expect a permanent failure.
     * 
     * @throws IOException unexpected
     * @throws InterruptedException unexpected
     * @throws ExecutionException unexpected
     * @throws InvalidMailException unexpected
     */
    @Test
    public void testConnectWithExplicitSecurityButServerDoesNotSupportStartTLS()
        throws IOException, InterruptedException, ExecutionException, InvalidMailException {

        GreenMail greenmail = new GreenMail(ServerSetupTest.SMTP); // port 3025
        greenmail.start();

        boolean configured = setupMailService("/greenmail_explicit_encryption.json");
        assertTrue(configured);

        final Future<?> sendMail = mailServiceImpl.sendMail(createDefaultMail(), new MailDispatchResultListener() {

            @Override
            public void receiveResult(MailDispatchResult result, String message) {
                assertEquals(MailDispatchResult.FAILURE, result);
                assertTrue(message.contains("STARTTLS is required but host does not support STARTTLS"));
            }
        });
        sendMail.get();

        greenmail.stop();
    }

    /**
     * This test will attempt to establish a connection with implicit security to a SMTP port. Therefore, we expect a permanent failure.
     * 
     * @throws IOException unexpected
     * @throws InterruptedException unexpected
     * @throws ExecutionException unexpected
     * @throws InvalidMailException unexpected
     */
    @Test
    public void testConnectWithImplicitSecurityButServerExpectsExplicitSecurity() throws IOException, InvalidMailException,
        InterruptedException, ExecutionException {

        GreenMail greenmail = new GreenMail(ServerSetupTest.SMTP); // port 3025
        greenmail.start();

        boolean configured = setupMailService("/greenmail_implicit_encryption_port3025.json");
        assertTrue(configured);

        final Future<?> sendMail = mailServiceImpl.sendMail(createDefaultMail(), new MailDispatchResultListener() {

            @Override
            public void receiveResult(MailDispatchResult result, String message) {
                assertEquals(MailDispatchResult.FAILURE, result);
                assertTrue(message.contains("Could not connect to SMTP host"));
            }
        });
        sendMail.get();

        greenmail.stop();
    }

    /**
     * Attempts to connect to the Greenmail dummy server over an implicitly secured connection.
     * 
     * @throws IOException unexpected
     * @throws InterruptedException unexpected
     * @throws ExecutionException unexpected
     * @throws InvalidMailException unexpected
     * @throws MessagingException unexpected
     */
    @Test
    public void testConnectWithImplicitSecurity()
        throws IOException, InterruptedException, ExecutionException, InvalidMailException, MessagingException {

        // opens an SMTPS server on port 3465 which awaits SSL/TLS connections
        GreenMail greenmail = new GreenMail(ServerSetupTest.SMTPS);
        greenmail.getManagers().getUserManager().setAuthRequired(true);
        greenmail.start();

        boolean configured = setupMailService(GREENMAIL_IMPLICIT_ENCRYPTION_JSON);
        assertTrue(configured);

        mailServiceImpl.trustAllCertificates();
        assertTrue(mailServiceImpl.canConnectToServer());

        greenmail.stop();
    }

    /**
     * Attempts to connect to the Greenmail dummy server with an invalid certificate over an implicitly secured connection.
     * 
     * @throws IOException unexpected
     * @throws InterruptedException unexpected
     * @throws ExecutionException unexpected
     * @throws InvalidMailException unexpected
     * @throws MessagingException unexpected
     */
    @Test
    public void testConnectWithImplicitSecurityAndInvalidCertificate()
        throws IOException, InterruptedException, ExecutionException, InvalidMailException, MessagingException {

        // opens an SMTPS server on port 3465 which awaits SSL/TLS connections
        GreenMail greenmail = new GreenMail(ServerSetupTest.SMTPS);
        greenmail.getManagers().getUserManager().setAuthRequired(true);
        greenmail.start();

        boolean configured = setupMailService(GREENMAIL_IMPLICIT_ENCRYPTION_JSON);
        assertTrue(configured);

        assertFalse(mailServiceImpl.canConnectToServer());

        greenmail.stop();
    }

    /**
     * Attempts to send a mail to the Greenmail dummy server over an implicitly secured connection.
     * 
     * @throws IOException unexpected
     * @throws InvalidMailException unexpected
     * @throws ExecutionException unexpected
     * @throws InterruptedException unexpected
     * @throws MessagingException unexpected
     */
    @Test
    public void testMailDispatchWithImplicitSecurity() throws IOException, InvalidMailException, InterruptedException, ExecutionException,
        MessagingException {

        // opens an SMTPS server on port 3465 which awaits SSL/TLS connections
        GreenMail greenmail = new GreenMail(ServerSetupTest.SMTPS);
        greenmail.getManagers().getUserManager().setAuthRequired(true);
        greenmail.start();

        boolean configured = setupMailService(GREENMAIL_IMPLICIT_ENCRYPTION_JSON);
        // Since Greenmail uses self-signed certificates, we need to disable certificate validation.
        mailServiceImpl.trustAllCertificates();
        assertTrue(configured);

        Future<?> sendMail = mailServiceImpl.sendMail(createDefaultMail(), new MailDispatchResultListener() {

            @Override
            public void receiveResult(MailDispatchResult result, String message) {
                assertEquals(MailDispatchResult.SUCCESS, result);
                assertEquals(null, message);
            }
        });
        // wait till the mail is send
        sendMail.get();

        validateDefaultMail(greenmail.getReceivedMessages());

        greenmail.stop();
    }

    /**
     * Tests if the mail delivery is retried after an initial connection failure.
     * 
     * @throws IOException unexpected
     * @throws InvalidMailException unexpected
     * @throws InterruptedException unexpected
     * @throws ExecutionException unexpected
     * @throws MessagingException unexpected
     */
    @Test
    public void testMailDispatchIsReattempted() throws IOException, InvalidMailException, InterruptedException, ExecutionException,
        MessagingException {

        // opens an SMTPS server on port 3465 which awaits SSL/TLS connections
        final GreenMail greenmail = new GreenMail(ServerSetupTest.SMTPS);
        greenmail.getManagers().getUserManager().setAuthRequired(true);
        // do not start the server here

        boolean configured = setupMailService(GREENMAIL_IMPLICIT_ENCRYPTION_JSON);
        // Since Greenmail uses self-signed certificates, we need to disable certificate validation.
        mailServiceImpl.trustAllCertificates();
        assertTrue(configured);

        Future<?> sendMail = mailServiceImpl.sendMail(createDefaultMail(), new MailDispatchResultListener() {

            private boolean firstRun = true;

            @Override
            public void receiveResult(MailDispatchResult result, String message) {
                // since the mail server is not running, we expect a failure
                if (firstRun) {
                    assertEquals(MailDispatchResult.FAILURE_RETRY, result);
                    assertEquals(null, message);
                    // activate the server
                    greenmail.start();
                    firstRun = false;
                } else {
                    // we expect a retry and a successful mail delivery after the start of the server
                    assertEquals(MailDispatchResult.SUCCESS, result);
                    assertEquals(null, message);
                }

            }
        });
        // wait till the mail is send
        sendMail.get();

        validateDefaultMail(greenmail.getReceivedMessages());

        greenmail.stop();
    }

    /**
     * Tests if the mail delivery can be canceled while a retry is attempted.
     * 
     * @throws IOException unexpected
     * @throws InvalidMailException unexpected
     * @throws InterruptedException unexpected
     * @throws ExecutionException unexpected
     */
    @Test
    public void testCancelFutureWhileMailDispatchIsReattempted() throws IOException, InvalidMailException, InterruptedException,
        ExecutionException {
        // Do not start a Greenmail server, since we want to check the behavior during a retry

        boolean configured = setupMailService(GREENMAIL_IMPLICIT_ENCRYPTION_JSON);
        assertTrue(configured);

        MailDispatchResultListener listenerMock = EasyMock.createStrictMock(MailDispatchResultListener.class);
        listenerMock.receiveResult(MailDispatchResult.FAILURE_RETRY, null);
        EasyMock.expectLastCall().once();
        listenerMock.receiveResult(EasyMock.eq(MailDispatchResult.FAILURE), EasyMock.contains("interrupted"));
        EasyMock.expectLastCall().once();
        EasyMock.replay(listenerMock);

        Future<?> sendMail = mailServiceImpl.sendMail(createDefaultMail(), listenerMock);

        Thread.sleep(TWO_SECS_IN_MILLIS);

        boolean canceled = sendMail.cancel(true);
        assertTrue(canceled);

        expectedException.expect(CancellationException.class);
        sendMail.get();
    }
    
    /**
     * This test will attempt to establish a connection with explicit security using the STARTTLS command.
     * 
     * TODO Greenmail currently does not support STARTTLS at all. Therefore, this test should be created as soon as Greenmail supports
     * STARTTLS.
     */
    @Ignore
    public void testConnectWithExplicitSecurity() {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
    }

    /**
     * This test will attempt to establish a connection and authenticate with invalid credentials.
     * 
     * TODO Greenmail 1.5.1 does not verify users, but will accept any password. However, future version probably will support this feature:
     * https://github.com/greenmail-mail-test/greenmail/issues/127 . It would be nice to have an integration test that checks how our code
     * behaves if the wrong user credentials are used when connecting to the server.
     */
    @Ignore
    public void testConnectWithInvalidCredentials() {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
    }
    
    /**
     * Tests the mail service deactivation during waiting for reattempt of mail delivery.
     * 
     * TODO
     */
    @Ignore
    public void testDeactivateMailService() {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
    }
}
