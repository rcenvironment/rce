/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.scripting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.script.ScriptException;

import org.junit.Test;
import org.python.jsr223.PyScriptEngine;
import org.python.jsr223.PyScriptEngineFactory;

import de.rcenvironment.core.scripting.python.PythonOutputWriter;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.testing.CommonTestOptions;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;

/**
 * Tests the execution behavior of the Python script engine. Especially, during parallel script execution.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (added test size switching)
 */
public class PythonScriptEngineTest {

    private static final int ITERATION_COUNT_STANDARD_TESTING = 20; // arbitrary; adjust as needed

    private static final int TIMEOUT_SEC_STANDARD_TESTING = 10;

    private static final int ITERATION_COUNT_EXTENDED_TESTING = 800;

    private static final int TIMEOUT_SEC_EXTENDED_TESTING = 90;

    private static final int FALLBACK_TEST_TIMEOUT = 120000;

    private static final String SCRIPT = "import sys\nfor i in range(0, 5000):\n print '%s%s'\nfor i in range(0, 10000):\n "
        + "sys.stderr.write('%s%s\\n')\n sys.stderr.flush()\nfor i in range(0, 5000):\n print '%s%s'";

    private static final String OUTPUT_PREFIX_STDOUT = "stdout ";

    private static final String OUTPUT_PREFIX_STDERR = "stderr ";

    private final AtomicInteger stdoutCloseCount = new AtomicInteger(0);

    private final AtomicInteger stderrCloseCount = new AtomicInteger(0);

    private final AtomicInteger wrongOutputCount = new AtomicInteger(0);

//    /**
//     * Sets JVM properties required for Jython script execution.
//     */
//    @BeforeClass
//    public static void setUp() {
//        ScriptingUtils.setJVMPropertiesForJython270Support();
//    }
    
    /**
     * Tests correct output handling of multiple scripts are executed in parallel threads but synchronized.
     * 
     * @throws InterruptedException on error
     */
    @Test(timeout = FALLBACK_TEST_TIMEOUT)
    public void testFetchingStdoutErrDuringSynchronizedParallelExecution() throws InterruptedException {
        testFetchingStdoutErrDuringParallelExecution(true);
        assertEquals(0, wrongOutputCount.get());
    }

    private void testFetchingStdoutErrDuringParallelExecution(final boolean isSynchronized) throws InterruptedException {

        final int waitInterval =
            CommonTestOptions.selectStandardOrExtendedValue(TIMEOUT_SEC_STANDARD_TESTING, TIMEOUT_SEC_EXTENDED_TESTING);
        final int scriptEvalCount =
            CommonTestOptions.selectStandardOrExtendedValue(ITERATION_COUNT_STANDARD_TESTING, ITERATION_COUNT_EXTENDED_TESTING);

        final CountDownLatch iterationFinishedLatch = new CountDownLatch(scriptEvalCount);

        final AsyncTaskService threadPool = ConcurrencyUtils.getAsyncTaskService();

        for (int i = 0; i < scriptEvalCount; i++) {

            final String suffix = String.valueOf(i);
            threadPool.execute("Execute script", () -> {


                    if (isSynchronized) {
                        synchronized (PythonScriptEngineTest.this) {
                            executePythonScript(suffix, iterationFinishedLatch);
                        }
                    } else {
                        executePythonScript(suffix, iterationFinishedLatch);
                    }
                }
            );
        }

        assertTrue(iterationFinishedLatch.await(waitInterval, TimeUnit.SECONDS));
        assertEquals(scriptEvalCount, stdoutCloseCount.get());
        assertEquals(scriptEvalCount, stderrCloseCount.get());
    }

    private void executePythonScript(String suffix, CountDownLatch iterationFinishedLatch) {
        PyScriptEngineFactory factory = new PyScriptEngineFactory();
        PyScriptEngine engine = (PyScriptEngine) factory.getScriptEngine();
        try {
            Writer outWriter = new PythonOutputWriterStub(new Object(), OUTPUT_PREFIX_STDOUT, suffix);
            engine.getContext().setWriter(outWriter);
            Writer errWriter = new PythonOutputWriterStub(new Object(), OUTPUT_PREFIX_STDERR, suffix);
            engine.getContext().setErrorWriter(errWriter);

            engine.eval(StringUtils.format(SCRIPT, OUTPUT_PREFIX_STDOUT, suffix, OUTPUT_PREFIX_STDERR, suffix,
                OUTPUT_PREFIX_STDOUT, suffix));
            engine.eval(StringUtils.format(
                "sys.stdout.write('%s')\nsys.stderr.write('%s')\nsys.stdout.flush()\nsys.stderr.flush()",
                PythonOutputWriter.CONSOLE_END, PythonOutputWriter.CONSOLE_END));

            outWriter.close();
            errWriter.close();

            iterationFinishedLatch.countDown();

        } catch (ScriptException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Stub implementation of {@link PythonOutputWriter} used by the Jython script engine for stdout and stderr.
     * 
     * @author Doreen Seider
     */
    class PythonOutputWriterStub extends PythonOutputWriter {

        private final String outPrefix;

        private final String outSuffix;

        PythonOutputWriterStub(Object lock, String outPrefix, String outSuffix) {
            super(lock, null);
            this.outPrefix = outPrefix;
            this.outSuffix = outSuffix;
        }

        @Override
        public void close() throws IOException {
            super.close();
            if (outPrefix.equals(OUTPUT_PREFIX_STDOUT)) {
                stdoutCloseCount.incrementAndGet();
            } else {
                stderrCloseCount.incrementAndGet();
            }
        }

        @Override
        protected void onNewLineToForward(String line) {
            // line is null if end of output is reached; not relevant for this test case
            if (line != null && !line.equals(outPrefix + outSuffix + "\n")) {
                wrongOutputCount.incrementAndGet();
            }

        }
    }
}
