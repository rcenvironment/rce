/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap.ui;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Matcher;
import org.junit.Test;

import com.googlecode.lanterna.input.Key;
import com.googlecode.lanterna.input.Key.Kind;

/**
 * Tests for {@link ErrorTextUI}.
 *
 * @author Tobias Brieden
 */
public class ErrorUITest {

    private static final String ARBITRARY_ERROR_TEXT = "This is some kind of error text.";
    
    /**
     * Tests if the given error message is displayed.
     */
    @Test
    public void testErrorTextUIWithLanternaTest() {
        final LanternaTestUtils lanternaTest = new LanternaTestUtils();
        Runnable uiRunnable = new Runnable() {

            @Override
            public void run() {
                new ErrorTextUI(ARBITRARY_ERROR_TEXT, lanternaTest.getTestTerminal()).run();
            }
        };
        lanternaTest.setUiRunnable(uiRunnable);
        
        Queue<Matcher<char[][]>> assertions = new LinkedList<Matcher<char[][]>>();
        assertions.add(TerminalStub.containsString(ARBITRARY_ERROR_TEXT));
        lanternaTest.addTestStage(assertions, 1);
        
        // TODO there is no assertion in this case, but the timer still waits for a flush, which causes it to wait for a second
        lanternaTest.addTestStage(new Key(Kind.Enter), 1);
        
        lanternaTest.executeTestsWithTimeout(5, TimeUnit.SECONDS);
    }
}
