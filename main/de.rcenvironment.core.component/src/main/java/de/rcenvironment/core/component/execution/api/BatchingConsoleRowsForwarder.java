/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import java.util.List;

import de.rcenvironment.core.utils.common.concurrent.BatchAggregator;

/**
 * Collects single {@link ConsoleRow}s and forwards them as batches to the provided {@link BatchedConsoleRowsProcessor}.
 * 
 * @author Robert Mischke
 */
public class BatchingConsoleRowsForwarder implements SingleConsoleRowsProcessor {

    // the maximum number of ConsoleRows to aggregate to a single batch
    // NOTE: arbitrary value; adjust when useful/necessary
    private static final int MAX_BATCH_SIZE = 500;

    // the maximum time a ConsoleRow may be delayed by batch aggregation
    // NOTE: arbitrary value; adjust when useful/necessary
    private static final long MAX_BATCH_LATENCY_MSEC = 200;

    private final BatchAggregator<ConsoleRow> batchAggregator;

    public BatchingConsoleRowsForwarder(final BatchedConsoleRowsProcessor consoleRowsProcessor) {
        BatchAggregator.BatchProcessor<ConsoleRow> batchProcessor = new BatchAggregator.BatchProcessor<ConsoleRow>() {

            @Override
            public void processBatch(List<ConsoleRow> batch) {
                ConsoleRow[] batchArray = batch.toArray(new ConsoleRow[batch.size()]);
                consoleRowsProcessor.processConsoleRows(batchArray);
            }

        };
        batchAggregator = new BatchAggregator<ConsoleRow>(MAX_BATCH_SIZE, MAX_BATCH_LATENCY_MSEC, batchProcessor);
    }

    @Override
    public void onConsoleRow(ConsoleRow consoleRow) {
        batchAggregator.enqueue(consoleRow);
    }

}
