/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.management;

import java.io.Serializable;

import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * A service that allows the creation of communication benchmarks. It also serves as the remote
 * counterpart that responds to test requests within a benchmark (via {@link #respond()}).
 * 
 * @author Robert Mischke
 */
public interface BenchmarkService {

    /**
     * Parses a textual benchmark definition into a {@link BenchmarkSetup} object. If the input is
     * invalid, a {@link IllegalArgumentException} run-time exception is thrown.
     * 
     * TODO add format documentation -- misc_ro, 2013-01
     * 
     * @param definition the definition String
     * @return the ready-to-use benchmark setup
     */
    BenchmarkSetup parseBenchmarkDescription(String definition);

    /**
     * Performs a benchmark run.
     * 
     * @param setup the benchmark definition/setup
     * @param outputReceiver the receiver of text output and start/finish/error events.
     */
    void executeBenchmark(BenchmarkSetup setup, TextOutputReceiver outputReceiver);

    /**
     * Starts an asynchronous benchmark run. The start and finish of the benchmark run can be
     * detected via the {@link TextOutputReceiver} callback methods.
     * 
     * @param setup the benchmark definition/setup
     * @param outputReceiver the receiver of text output and start/finish/error events.
     */
    void asyncExecBenchmark(BenchmarkSetup setup, TextOutputReceiver outputReceiver);

    /**
     * Generates the response to a benchmark request.
     * 
     * @param input the benchmark request payload; may or may not contain relevant data
     * @param respSize the expected response size to generate
     * @param respDelay the expected delay (in msec) before returning the response
     * @return the generated response payload
     */
    Serializable respond(Serializable input, Integer respSize, Integer respDelay);
}
