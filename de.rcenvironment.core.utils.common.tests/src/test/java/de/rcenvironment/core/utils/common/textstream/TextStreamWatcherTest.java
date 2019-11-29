/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.textstream;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;

import org.junit.Test;

import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.textstream.receivers.CapturingTextOutReceiver;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;

/**
 * {@link TextStreamWatcher} test cases; this also tests {@link CapturingTextOutReceiver} as a side effect.
 *
 * @author Robert Mischke
 */
public class TextStreamWatcherTest {

    private final AsyncTaskService asyncTaskService = ConcurrencyUtils.getAsyncTaskService();

    /**
     * Tests basic stream reading and line output.
     */
    @Test
    public void basicTest() {
        String testText = "line1\nline2\r\nline3";

        ByteArrayInputStream stream = new ByteArrayInputStream(testText.getBytes()); // charset does not matter here

        CapturingTextOutReceiver receiver = createTestOutputReceiver();
        new TextStreamWatcher(stream, asyncTaskService, receiver).start().waitForTermination();
        assertEquals("(pre)line1.line2.line3.(post)", receiver.getBufferedOutput());
    }

    /**
     * Tests reading from two streams in parallel (at least, API wise; no real concurrency test yet).
     */
    @Test
    public void multiStreamTest() {
        String testText1 = "stream1a\nstream1b\nstream1c\n";
        String testText2 = "stream2a\nstream2b\nstream2c\n";

        ByteArrayInputStream stream1 = new ByteArrayInputStream(testText1.getBytes()); // charset does not matter here
        ByteArrayInputStream stream2 = new ByteArrayInputStream(testText2.getBytes()); // charset does not matter here

        CapturingTextOutReceiver receiver1 = createTestOutputReceiver();
        CapturingTextOutReceiver receiver2 = createTestOutputReceiver();

        new TextStreamWatcher(asyncTaskService).registerStream(stream1, receiver1).registerStream(stream2, receiver2).start()
            .waitForTermination();
        assertEquals("(pre)stream1a.stream1b.stream1c.(post)", receiver1.getBufferedOutput());
        assertEquals("(pre)stream2a.stream2b.stream2c.(post)", receiver2.getBufferedOutput());
    }

    private CapturingTextOutReceiver createTestOutputReceiver() {
        return new CapturingTextOutReceiver("(pre)", ".", "(post)");
    }

}
