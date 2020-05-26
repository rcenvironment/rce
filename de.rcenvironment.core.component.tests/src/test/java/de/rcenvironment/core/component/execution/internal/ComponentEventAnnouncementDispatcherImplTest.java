/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.component.execution.api.ComponentEventAnnouncement;
import de.rcenvironment.core.component.execution.api.ComponentEventAnnouncement.WorkflowEventType;
import de.rcenvironment.core.mail.Mail;
import de.rcenvironment.core.mail.MailDispatchResult;
import de.rcenvironment.core.mail.MailDispatchResultListener;
import de.rcenvironment.core.mail.MailService;
import de.rcenvironment.toolkit.utils.text.TextLinesReceiver;

/**
 * Test cases for {@link ComponentEventAnnouncementDispatcherImpl}.
 * 
 * @author Doreen Seider
 */
public class ComponentEventAnnouncementDispatcherImplTest {

    /**
     * Tests if dispatching event via mail fails gracefully in case no recipients are passed.
     */
    @Test
    public void testDispatchingEventViaEmailNoRecipients() {

        // no recipients

        TextLinesReceiver textLinesReceiverMock = EasyMock.createStrictMock(TextLinesReceiver.class);
        Capture<String> lineCapture = Capture.newInstance();
        textLinesReceiverMock.addLine(EasyMock.capture(lineCapture));
        EasyMock.expectLastCall();
        EasyMock.replay(textLinesReceiverMock);

        ComponentEventAnnouncementDispatcherImpl eventDispatcher = new ComponentEventAnnouncementDispatcherImpl();
        assertFalse(
            eventDispatcher.dispatchWorkflowEventAnnouncementViaMail(new String[0], createEventAnnouncement(), textLinesReceiverMock));

        EasyMock.verify(textLinesReceiverMock);
        assertTrue(lineCapture.hasCaptured());

    }

    /**
     * Tests if dispatching mails succeeded on proper parameter passed and if information are forwarded to actual mail service properly.
     * 
     * @throws ExecutionException on unexpected error
     * @throws InterruptedException on unexpected error
     */
    @Test
    public void testDispatchingEventViaEmailForSuccess() throws InterruptedException, ExecutionException {
        TextLinesReceiver textLinesReceiverMock = EasyMock.createStrictMock(TextLinesReceiver.class);
        EasyMock.replay(textLinesReceiverMock);
        testDispatchingEventViaEmail(true, textLinesReceiverMock);
    }

    /**
     * Tests if dispatching mails indicates a failure if mail service stated that delivering failed.
     * 
     * @throws ExecutionException on unexpected error
     * @throws InterruptedException on unexpected error
     */
    @Test
    public void testDispatchingEventViaEmailForFailure() throws InterruptedException, ExecutionException {
        TextLinesReceiver textLinesReceiverMock = EasyMock.createStrictMock(TextLinesReceiver.class);
        textLinesReceiverMock.addLine(EasyMock.anyObject(String.class));
        EasyMock.expectLastCall();
        EasyMock.replay(textLinesReceiverMock);
        testDispatchingEventViaEmail(false, textLinesReceiverMock);
    }

    private void testDispatchingEventViaEmail(final boolean forSuccess, TextLinesReceiver textLinesReceiverMock)
        throws InterruptedException, ExecutionException {

        final Future<?> futureMock = EasyMock.createStrictMock(Future.class);
        EasyMock.expect(futureMock.get()).andReturn(null);
        EasyMock.replay(futureMock);

        MailService mailServiceStub = new MailService() {

            @Override
            public Future<?> sendMail(Mail mail, MailDispatchResultListener listener) {
                if (forSuccess) {
                    listener.receiveResult(MailDispatchResult.SUCCESS, null);
                } else {
                    listener.receiveResult(MailDispatchResult.FAILURE, null);
                }
                return futureMock;
            }

            @Override
            public boolean isConfigured() {
                return true;
            }
        };

        ComponentEventAnnouncementDispatcherImpl eventDispatcher = new ComponentEventAnnouncementDispatcherImpl();
        eventDispatcher.bindMailService(mailServiceStub);

        boolean result =
            eventDispatcher.dispatchWorkflowEventAnnouncementViaMail(new String[] { "john.doe@mail.de" }, createEventAnnouncement(),
                textLinesReceiverMock);
        if (forSuccess) {
            assertTrue(result);
        } else {
            assertFalse(result);
        }

        EasyMock.verify(futureMock);
        EasyMock.verify(textLinesReceiverMock);

    }

    private ComponentEventAnnouncement createEventAnnouncement() {
        ComponentEventAnnouncement eventAnnouncementMock = EasyMock.createStrictMock(ComponentEventAnnouncement.class);
        EasyMock.expect(eventAnnouncementMock.hasSubject()).andStubReturn(true);
        EasyMock.expect(eventAnnouncementMock.getSubject()).andStubReturn("some subject");
        EasyMock.expect(eventAnnouncementMock.getBody()).andStubReturn("some body");
        EasyMock.expect(eventAnnouncementMock.getWorkflowEventType()).andStubReturn(WorkflowEventType.REQUEST_FOR_OUTPUT_APPROVAL);
        EasyMock.replay(eventAnnouncementMock);
        return eventAnnouncementMock;
    }

}
