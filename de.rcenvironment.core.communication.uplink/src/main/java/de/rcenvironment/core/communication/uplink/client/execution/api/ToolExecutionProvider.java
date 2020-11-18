/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.execution.api;

import de.rcenvironment.core.utils.common.exception.OperationFailureException;

/**
 * The client-side interface that is called when a remote tool execution request is performed. The decision whether the tool is valid for
 * execution, and whether the caller is authorized to run it has already been performed at this point.
 * <p>
 * For each incoming {@link ToolExecutionRequest}, a single stateful {@link ToolExecutionProvider} implementation object should be created.
 *
 * @author Robert Mischke
 */
public interface ToolExecutionProvider {

    /**
     * @return the destination to which input files from the caller's side should be provided to
     */
    DirectoryDownloadReceiver getInputDirectoryReceiver();

    /**
     * Called after all input files have been provided. At this point, the actual tool execution should be performed, and this method should
     * block until it completes. Note that asynchronous calls to report output events (e.g. StdOut) can still be made, and calls to
     * {@link #requestCancel()} should also be processed concurrently for cancellation to work.
     * 
     * @param eventCollector the {@link ToolExecutionProviderEventCollector} to send event data (e.g. stdout/stderr output) to
     * @return the {@link ToolExecutionRequest} object transporting the execution's final result parameters
     * @throws OperationFailureException if the tool execution failed with an abnormal error; note that the actual tool execution's result,
     *         including cancellation etc., should be encoded in its output, e.g. a standardized result file
     */
    ToolExecutionResult execute(ToolExecutionProviderEventCollector eventCollector) throws OperationFailureException;

    /**
     * Signals that a currently running {@link #execute()} method should be cancelled if possible.
     */
    void requestCancel();

    /**
     * @return the source from which the output files generated by the tool's execution should be fetched
     */
    DirectoryUploadProvider getOutputDirectoryProvider();

    /**
     * Called as the last method after everything related to this tool execution has been finished. Intended for local cleanup.
     */
    void onContextClosing();

}