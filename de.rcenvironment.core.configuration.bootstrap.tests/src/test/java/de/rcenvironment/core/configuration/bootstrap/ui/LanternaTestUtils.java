/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap.ui;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hamcrest.Matcher;

import com.googlecode.lanterna.input.Key;
import com.googlecode.lanterna.terminal.Terminal;

import de.rcenvironment.core.configuration.bootstrap.ui.TerminalStub.FlushListener;

/**
 * A utility class to write tests for Lanterna UIs. Since interaction with a Lanterna terminal is quite involved, developers may use
 *
 * @author Tobias Brieden
 * @author Alexander Weinert (renaming)
 */
public class LanternaTestUtils {

    // TODO is should be possible to check the state of the application, besides the displayed GUI stuff
    // this could be achieved by not automatically executing the next stage but instead waiting for an external call to continue the
    // execution

    // TODO it should be easier to select items from a dialog and more flexible than inputting all necessary keys by hand

    // TODO test fail if the line wrapping changes, make this more flexible

    // 100, 30 seems to be the default size
    private static final int TERMINAL_HEIGHT = 30;

    private static final int TERMINAL_WIDTH = 100;

    // a queue containing all matchers that need to be satisfied in the current stage
    private final Queue<Matcher<char[][]>> currentMatchers = new LinkedList<Matcher<char[][]>>();

    private final Queue<TestStage> testStages = new LinkedList<TestStage>();

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private final TerminalStub terminal;

    private Runnable uiRunnable;

    private ScheduledFuture<?> timerFuture;

    private ScheduledThreadPoolExecutor timerExecutor = new ScheduledThreadPoolExecutor(1);

    private Log log = LogFactory.getLog(LanternaTestUtils.class);

    public LanternaTestUtils() {

        terminal = new TerminalStub(TERMINAL_WIDTH, TERMINAL_HEIGHT);
        terminal.setFlushListener(new FlushListener() {

            @Override
            public void notifyListener() {
                log.debug("notifyListener");
                log.debug("Matchers: " + currentMatchers.size());

                // if the terminal content changed ...
                char[][] lastContent = terminal.getPreviousContent().getLast();

                // boolean removedAtLeastOneMatcher = false;

                while (!currentMatchers.isEmpty()) {
                    // ... we check if the next matchers in the queue are satisfied ...
                    if (currentMatchers.peek().matches(lastContent)) {
                        log.debug("Matcher matches!");
                        currentMatchers.poll();
                        // removedAtLeastOneMatcher = true;
                    } else {
                        log.debug("Matcher doesn't match!");
                        // ... if this is not the case, we wait for the next content update
                        break;
                    }
                }

                // TODO why do we need to make sure that at least one matcher was removed?
                // This prevents the further execution if a sage has no assertions/matchers
                if (/* removedAtLeastOneMatcher && */ currentMatchers.isEmpty()) {
                    log.debug("Cancelling the timer!: " + timerFuture);
                    // cancel the timer if all matchers are satisfied
                    timerFuture.cancel(true);
                    // timerFuture = null;
                }
            }
        });
    }

    public Terminal getTestTerminal() {
        return terminal;
    }

    public void setUiRunnable(Runnable uiRunnable) {
        this.uiRunnable = uiRunnable;
    }

    /**
     * A TestStage represents one phase of a test. First the input keys are presented to the UI. Each time the UI is flushed the assertions
     * are checked. If the assertions are not satisfied within the given timeout, the test fails.
     * 
     * @author Tobias Brieden
     */
    class TestStage {

        private Queue<Key> input;

        private Queue<Matcher<char[][]>> assertions;

        private long timeoutInSeconds;
    }

    void addTestStage(Queue<Matcher<char[][]>> assertions, long timeoutInSeconds) {

        this.addTestStage(new LinkedList<Key>(), assertions, timeoutInSeconds);
    }

    void addTestStage(Key input, long timeoutInSeconds) {

        this.addTestStage(input, new LinkedList<Matcher<char[][]>>(), timeoutInSeconds);
    }

    void addTestStage(Key input, Queue<Matcher<char[][]>> assertions, long timeoutInSeconds) {

        Queue<Key> queuedInput = new LinkedList<Key>();
        queuedInput.add(input);
        this.addTestStage(queuedInput, assertions, timeoutInSeconds);
    }

    void addTestStage(Queue<Key> input, Queue<Matcher<char[][]>> assertions, long timeoutInSeconds) {

        TestStage stage = new TestStage();
        stage.input = input;
        stage.assertions = assertions;
        stage.timeoutInSeconds = timeoutInSeconds;

        testStages.add(stage);
    }

    private void activateNextStage() {
        TestStage stage = testStages.poll();

        log.debug("add stage");

        // make sure that all matchers of previous stages have been satisfied
        assertTrue(currentMatchers.isEmpty());

        // add the matchers of the current stage
        currentMatchers.addAll(stage.assertions);

        timerFuture = timerExecutor.schedule(new Runnable() {

            @Override
            public void run() {
                // if this code is reached, the matchers were not all satisfied in the given time and the test should fail
                assertTrue(currentMatchers.isEmpty());
            }
        }, stage.timeoutInSeconds, TimeUnit.SECONDS);
        log.debug("new timerFuture: " + timerFuture);

        // feed the input to the terminal
        terminal.appendKeyToQueue(stage.input);
        log.debug("stage added");

        // wait for the timeout
        try {
            timerFuture.get();
        } catch (InterruptedException e) {
            // TODO in which cases does this happen?
        } catch (ExecutionException e) {
            // if the timer execution is interrupted, this is most likely due to the fact that the assertion within the timer failed
            if (e.getCause() instanceof AssertionError) {
                throw (AssertionError) e.getCause();
            }
        } catch (CancellationException e) {
            // the timer was cancelled since all matchers are satisfied

            // ... and activate the next test stage
            if (!testStages.isEmpty()) {
                activateNextStage();
            }
        }
    }

    /**
     * Executes all test stages added to this object. Does not impose a global timeout, but only the timeouts given in the individual
     * stages.
     */
    void executeTests() {

        log.debug("executeTests");

        // start the GUI under test
        executor.execute(uiRunnable);
        // do not execute anything else
        executor.shutdown();

        // read the first test stage
        if (!testStages.isEmpty()) {
            activateNextStage();
        }

        log.debug("done");
    }

    /**
     * Executes all test stages added to this object and imposes a global timeout, i.e., a timeout in which all added stages must finish.
     * This is mainly used for ensuring that asynchronous code executed during testing finished gracefully. Fails the enclosing unit test if
     * the timeout is violated.
     * 
     * @param timeout The number of timeunits to elapse before the timeout hits.
     * @param timeunit The unit in which the timout is given.
     * @throws AssertionError If the terminal is not closed within the given timeout.
     */
    void executeTestsWithTimeout(long timeout, TimeUnit timeunit) {
        executeTests();

        log.debug("before gracefullness check");
        // check if the UI executed gracefully
        try {
            assertTrue("The UI did not exit as expected.", executor.awaitTermination(5, TimeUnit.SECONDS));
            assertTrue(currentMatchers.isEmpty());
        } catch (InterruptedException e) {
            fail();
        }

    }
}
