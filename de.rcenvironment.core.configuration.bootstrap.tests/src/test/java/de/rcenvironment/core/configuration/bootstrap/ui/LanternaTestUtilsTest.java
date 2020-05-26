/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap.ui;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.googlecode.lanterna.input.Key;

/**
 * Tests for {@link LanternaTestUtils}.
 *
 * @author Tobias Brieden
 */
public class LanternaTestUtilsTest {

    private static final String ARBITRARY_STRING = "hallo";
    /**
     * ExpectedException.
     */
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    /**
     * Test if a LanternaTest fails if an assertion is not met.
     */
    @Test
    public void testAssertionFailsAsExpected() {

        final LanternaTestUtils lanternaTest = new LanternaTestUtils();
        Runnable uiRunnable = new Runnable() {

            @Override
            public void run() {
                new ErrorTextUI(ARBITRARY_STRING, lanternaTest.getTestTerminal()).run();
            }
        };
        lanternaTest.setUiRunnable(uiRunnable);

        Queue<Matcher<char[][]>> assertions = new LinkedList<Matcher<char[][]>>();
        // this assertion should not be satisfied
        assertions.add(TerminalStub.containsString("fdsafdsafds"));
        lanternaTest.addTestStage(new LinkedList<Key>(), assertions, 1); // timeout of one second

        AssertionError expectedError = null;
        try {
            lanternaTest.executeTestsWithTimeout(2, TimeUnit.SECONDS);
            fail("Expecting AssertionError.");
        } catch (AssertionError e) {
            // we expect the LanternaTest to throw an AssertionError ...
            expectedError = e;
        }
        assertNotNull("Expected an assertion error due to terminal execution encountering a timeout", expectedError);
    }
    
    /**
     * Test if a LanternaTest fails if it does not shutdown gracefully.
     */
    @Test
    public void testGracefulShutdownAssertionFailsAsExpected() {

        final LanternaTestUtils lanternaTest = new LanternaTestUtils();
        Runnable uiRunnable = new Runnable() {

            @Override
            public void run() {
                new ErrorTextUI(ARBITRARY_STRING, lanternaTest.getTestTerminal()).run();
            }
        };
        lanternaTest.setUiRunnable(uiRunnable);

        Queue<Matcher<char[][]>> assertions = new LinkedList<Matcher<char[][]>>();
        // this assertion will be satisfied
        assertions.add(TerminalStub.containsString(ARBITRARY_STRING));
        lanternaTest.addTestStage(new LinkedList<Key>(), assertions, 1); // timeout of one second

        // but the ErrorTextUI dialog will not be closed at the end.
        
        AssertionError expectedError = null;
        try {
            // Therefore this test should fail, since we test explicitly for a graceful shutdown 
            lanternaTest.executeTestsWithTimeout(2, TimeUnit.SECONDS);
            fail("Expecting AssertionError.");
        } catch (AssertionError e) {
            // we expect the LanternaTest to throw an AssertionError ...
            expectedError = e;
        }
        
        assertNotNull("Expected an assertion error due to terminal execution encountering a timeout", expectedError);
    }
    
    /**
     * Tests if the executeTests method can be called without any given stages.
     */
    @Test
    public void testExecuteTestWithoutAddedStages() {

        final LanternaTestUtils lanternaTest = new LanternaTestUtils();
        Runnable uiRunnable = new Runnable() {

            @Override
            public void run() {
                new ErrorTextUI(ARBITRARY_STRING, lanternaTest.getTestTerminal()).run();
            }
        };
        lanternaTest.setUiRunnable(uiRunnable);
        lanternaTest.executeTests();
    }
}
