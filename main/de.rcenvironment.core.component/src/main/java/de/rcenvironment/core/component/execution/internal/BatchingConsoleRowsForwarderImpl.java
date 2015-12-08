/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.api.BatchedConsoleRowsProcessor;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.SingleConsoleRowsProcessor;
import de.rcenvironment.core.utils.common.concurrent.BatchAggregator;

/**
 * Collects single {@link ConsoleRow}s and forwards them as batches to the provided
 * {@link BatchedConsoleRowsProcessor}.
 * 
 * @author Robert Mischke
 */
public class BatchingConsoleRowsForwarderImpl implements SingleConsoleRowsProcessor {

    // the maximum number of ConsoleRows to aggregate to a single batch
    // NOTE: arbitrary value; adjust when useful/necessary
    private static final int MAX_BATCH_SIZE = 500;

    // the maximum time a ConsoleRow may be delayed by batch aggregation
    // NOTE: arbitrary value; adjust when useful/necessary
    private static final long MAX_BATCH_LATENCY_MSEC = 200;

    private final BatchAggregator<ConsoleRow> batchAggregator;

    private final Log log = LogFactory.getLog(getClass());

    public BatchingConsoleRowsForwarderImpl(final BatchedConsoleRowsProcessor consoleRowsReceiver) {
        BatchAggregator.BatchProcessor<ConsoleRow> batchProcessor = new BatchAggregator.BatchProcessor<ConsoleRow>() {

            @Override
            public void processBatch(List<ConsoleRow> batch) {
                ConsoleRow[] batchArray = batch.toArray(new ConsoleRow[batch.size()]);
                // TODO can be disabled if too verbose
                // log.debug("Sending batch of " + batchArray.length + " console rows");
                try {
                    consoleRowsReceiver.processConsoleRows(batchArray);
                } catch (UndeclaredThrowableException e) {
                    log.error("Could not send console rows to caller.", e);
                }
            }

        };
        batchAggregator = new BatchAggregator<ConsoleRow>(MAX_BATCH_SIZE, MAX_BATCH_LATENCY_MSEC, batchProcessor);
    }

    @Override
    public void onConsoleRow(ConsoleRow consoleRow) {
        batchAggregator.enqueue(consoleRow);
    }

}
