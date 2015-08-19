/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.scripting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.script.ScriptException;

import org.junit.Test;
import org.python.jsr223.PyScriptEngine;
import org.python.jsr223.PyScriptEngineFactory;

import de.rcenvironment.core.scripting.python.PythonOutputWriter;

/**
 * Tests the execution behavior of the Python script engine. Especially, during parallel script execution.
 * 
 * @author Doreen Seider
 */
public class PythonScriptEngineTest {

    static final String SCRIPT = "import sys\nfor i in range(0, 5000):\n print '%s%s'\nfor i in range(0, 10000):\n "
        + "sys.stderr.write('%s%s\\n')\n sys.stderr.flush()\nfor i in range(0, 5000):\n print '%s%s'";

    private static final String OUTPUT_PREFIX_STDOUT = "stdout ";

    private static final String OUTPUT_PREFIX_STDERR = "stderr ";

    private static final int TEST_TIMEOUT = 65000;

    private final AtomicInteger stdoutCloseCount = new AtomicInteger(0);

    private final AtomicInteger stderrCloseCount = new AtomicInteger(0);

    private final AtomicInteger wrongOutputCount = new AtomicInteger(0);

    /**
     * Tests correct output handling of multiple scripts are executed in parallel threads but synchronized.
     * 
     * @throws InterruptedException on error
     */
    @Test(timeout = TEST_TIMEOUT)
    public void testFetchingStdoutErrDuringSynchronizedParallelExecution() throws InterruptedException {
        testFetchingStdoutErrDuringParallelExecution(true);
        assertEquals(0, wrongOutputCount.get());
    }

    private void testFetchingStdoutErrDuringParallelExecution(final boolean isSynchronized) throws InterruptedException {

        final int waitInterval = 60;

        final int scriptEvalCount = 1000;

        final CountDownLatch iterationFinishedLatch = new CountDownLatch(scriptEvalCount);

        ExecutorService threadPool = Executors.newFixedThreadPool(scriptEvalCount);

        for (int i = 0; i < scriptEvalCount; i++) {

            final String suffix = String.valueOf(i);
            threadPool.submit(new Runnable() {

                @Override
                public void run() {
                    if (isSynchronized) {
                        synchronized (PythonScriptEngineTest.this) {
                            executePythonScript(suffix, iterationFinishedLatch);
                        }
                    } else {
                        executePythonScript(suffix, iterationFinishedLatch);
                    }
                }
            });
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

            engine.eval(String.format(SCRIPT, OUTPUT_PREFIX_STDOUT, suffix, OUTPUT_PREFIX_STDERR, suffix,
                OUTPUT_PREFIX_STDOUT, suffix));
            engine.eval(String.format(
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

        public PythonOutputWriterStub(Object lock, String outPrefix, String outSuffix) {
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
