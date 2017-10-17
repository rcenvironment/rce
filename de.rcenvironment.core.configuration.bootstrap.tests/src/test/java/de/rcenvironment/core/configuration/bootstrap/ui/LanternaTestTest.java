/*
 * Copyright (C) 2006-2017 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.LinkedList;
import java.util.Queue;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.googlecode.lanterna.input.Key;

/**
 * Tests for {@link LanternaTest}.
 *
 * @author Tobias Brieden
 */
public class LanternaTestTest {

    private static final String ARBITRARY_STRING = "hallo";
    private static final double TIMEOUT_MARGIN = 0.5;
    private static final double MILLIS_IN_SECONDS = 1000.0;
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

        final LanternaTest lanternaTest = new LanternaTest();
        Runnable uiRunnable = new Runnable() {

            @Override
            public void run() {
                new ErrorTextUI(ARBITRARY_STRING, lanternaTest.getTestTerminal()).run();
            }
        };
        lanternaTest.setUiRunnable(uiRunnable);

        Queue<Matcher<char[][]>> assertions = new LinkedList<Matcher<char[][]>>();
        // this assertion should not be satisfied
        assertions.add(TestTerminal.containsString("fdsafdsafds"));
        lanternaTest.addTestStage(new LinkedList<Key>(), assertions, 1); // timeout of one second

        long start = System.currentTimeMillis();

        try {
            lanternaTest.executeTests(true);
            fail("Expecting AssertionError.");
        } catch (AssertionError e) {
            // we expect the LanternaTest to throw an AssertionError ...

            long end = System.currentTimeMillis();
            double executionTimeInSeconds = (end - start)/MILLIS_IN_SECONDS;
            // ... after the specified timeout of one second
            assertEquals(1.0, executionTimeInSeconds, TIMEOUT_MARGIN);
        }
    }
    
    /**
     * Test if a LanternaTest fails if it does not shutdown gracefully.
     */
    @Test
    public void testGracefullShutdownAssertionFailsAsExpected() {

        final LanternaTest lanternaTest = new LanternaTest();
        Runnable uiRunnable = new Runnable() {

            @Override
            public void run() {
                new ErrorTextUI(ARBITRARY_STRING, lanternaTest.getTestTerminal()).run();
            }
        };
        lanternaTest.setUiRunnable(uiRunnable);

        Queue<Matcher<char[][]>> assertions = new LinkedList<Matcher<char[][]>>();
        // this assertion will be satisfied
        assertions.add(TestTerminal.containsString(ARBITRARY_STRING));
        lanternaTest.addTestStage(new LinkedList<Key>(), assertions, 1); // timeout of one second

        // but the ErrorTextUI dialog will not be closed at the end.
        
        long start = System.currentTimeMillis();

        try {
            // Therefore this test should fail, since we test explicitly for a graceful shutdown 
            lanternaTest.executeTests(true);
            fail("Expecting AssertionError.");
        } catch (AssertionError e) {
            // we expect the LanternaTest to throw an AssertionError ...

            long end = System.currentTimeMillis();
            double executionTimeInSeconds = (end - start)/MILLIS_IN_SECONDS;
            // ... after the specified timeout of one second
            assertEquals(5.0, executionTimeInSeconds, TIMEOUT_MARGIN);
        }
    }
    
    /**
     * Tests if the executeTests method can be called without any given stages.
     */
    @Test
    public void testExecuteTestWithoutAddedStages() {

        final LanternaTest lanternaTest = new LanternaTest();
        Runnable uiRunnable = new Runnable() {

            @Override
            public void run() {
                new ErrorTextUI(ARBITRARY_STRING, lanternaTest.getTestTerminal()).run();
            }
        };
        lanternaTest.setUiRunnable(uiRunnable);
        lanternaTest.executeTests(false);
    }
}
