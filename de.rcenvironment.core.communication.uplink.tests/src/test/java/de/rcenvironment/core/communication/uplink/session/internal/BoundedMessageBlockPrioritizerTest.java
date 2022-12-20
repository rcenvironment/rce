/*
 * Copyright 2021-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.session.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.communication.uplink.common.internal.MessageType;
import de.rcenvironment.core.communication.uplink.network.api.MessageBlockPriority;
import de.rcenvironment.core.communication.uplink.network.internal.MessageBlock;
import de.rcenvironment.core.communication.uplink.network.internal.MessageBlockWithMetadata;
import de.rcenvironment.core.communication.uplink.network.internal.UplinkProtocolConfiguration;
import de.rcenvironment.core.communication.uplink.network.internal.UplinkProtocolConfiguration.Builder;
import de.rcenvironment.core.communication.uplink.network.internal.UplinkProtocolConstants;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;
import de.rcenvironment.core.utils.common.exception.ProtocolException;

/**
 * Unit tests for {@link BoundedMessageBlockPrioritizer}.
 *
 * @author Robert Mischke
 */
public class BoundedMessageBlockPrioritizerTest {

    private static final String DEFAULT_LOG_PREFIX = "[Test] ";

    @Before
    public void before() {
        // configure the test queue sizes as global default before creating test instances

        Builder builder = UplinkProtocolConfiguration.newBuilder();

        builder.setMaxBufferedMessagesForPriority(MessageBlockPriority.DEFAULT, 2);
        // note: the priority level is just descriptive, there is no difference for this queue regarding blocking
        builder.setMaxBufferedMessagesForPriority(MessageBlockPriority.LOW_NON_BLOCKABLE, 1);

        UplinkProtocolConfiguration.override(builder); // affects all Uplink objects created afterwards
    }

    @Test
    public void submitOrFail() throws ProtocolException, OperationFailureException, InterruptedException {

        BoundedMessageBlockPrioritizer prioritizer = new BoundedMessageBlockPrioritizer();
        MessageBlock mockMessageBlock1 = new MessageBlock(MessageType.TEST);

        // add to queue DEFAULT twice -> should succeed
        submitOrFail(prioritizer, mockMessageBlock1, MessageBlockPriority.DEFAULT);
        submitOrFail(prioritizer, mockMessageBlock1, MessageBlockPriority.DEFAULT);
        // add to queue LOW_NON_BLOCKABLE -> should succeed
        submitOrFail(prioritizer, mockMessageBlock1, MessageBlockPriority.LOW_NON_BLOCKABLE);
        // add to queue DEFAULT again, exceeding the limit -> should throw an OperationFailureException
        try {
            submitOrFail(prioritizer, mockMessageBlock1, MessageBlockPriority.DEFAULT);
            fail("(1a) submit() did not fail as expected");
        } catch (OperationFailureException e) {
            // expected flow
        }
        // add to queue LOW_NON_BLOCKABLE again, exceeding the limit -> should throw an OperationFailureException
        try {
            submitOrFail(prioritizer, mockMessageBlock1, MessageBlockPriority.LOW_NON_BLOCKABLE);
            fail("(1b) submit() did not fail as expected");
        } catch (OperationFailureException e) {
            // expected flow
        }

        // drain the next element; should come from queue DEFAULT (not tested)
        prioritizer.takeNext();

        // add to queue LOW_NON_BLOCKABLE again, exceeding the limit -> should still throw an OperationFailureException
        try {
            submitOrFail(prioritizer, mockMessageBlock1, MessageBlockPriority.LOW_NON_BLOCKABLE);
            fail("(1c) submit() did not fail as expected");
        } catch (OperationFailureException e) {
            // expected flow
        }

        // add to queue DEFAULT once -> should succeed
        submitOrFail(prioritizer, mockMessageBlock1, MessageBlockPriority.DEFAULT);
        // add to queue DEFAULT again, exceeding the limit -> should throw an OperationFailureException
        try {
            submitOrFail(prioritizer, mockMessageBlock1, MessageBlockPriority.DEFAULT);
            fail("(2) submit() did not fail as expected");
        } catch (OperationFailureException e) {
            // expected flow
        }

        // drain the three expected elements
        assertTrue(prioritizer.takeNext().isPresent());
        assertTrue(prioritizer.takeNext().isPresent());
        assertTrue(prioritizer.takeNext().isPresent());

        // consistency test: the next takeNext() should return nothing, i.e. Optional.empty()
        assertFalse("(3) takeNext() did not fail as expected", prioritizer.takeNext().isPresent());
    }

    private void submitOrFail(BoundedMessageBlockPrioritizer prioritizer, MessageBlock messageBlock, MessageBlockPriority priority)
        throws OperationFailureException, InterruptedException, ProtocolException {
        prioritizer.submitOrFail(new MessageBlockWithMetadata(messageBlock, UplinkProtocolConstants.UNDEFINED_CHANNEL_ID, priority),
            DEFAULT_LOG_PREFIX);
    }

}
