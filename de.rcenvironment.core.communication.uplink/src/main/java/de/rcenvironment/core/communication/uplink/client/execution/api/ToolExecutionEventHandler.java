/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.execution.api;

import java.io.InputStream;

/**
 * A callback interface providing life-cycle events and output data of a tool execution to the initiating client-side code.
 *
 * @author Robert Mischke
 */
// TODO rename for clarity
public interface ToolExecutionEventHandler {

    /**
     * Called before the {@link ToolExecutionClientSideSetup#}'s {@link DirectoryUploadProvider} is invoked to provide all input files for
     * the execution.
     */
    void onInputUploadsStarting();

    /**
     * Returns a {@link DirectoryUploadProvider} to receive the input directories and files for the tool execution.
     * 
     * @return the {@link DirectoryUploadProvider} instance to provide input directories and files to
     */
    DirectoryUploadProvider getInputDirectoryProvider();

    /**
     * Called after the {@link ToolExecutionClientSideSetup#}'s {@link DirectoryUploadProvider} was called to provide all input files for
     * the execution. Can be used to clean up temporary resources used to provide the input data streams.
     */
    void onInputUploadsFinished();

    /**
     * Called before {@link ToolExecutionProvider#execute()} is invoked to perform the tool's execution.
     */
    void onExecutionStarting();

    /**
     * Provides event data submitted to {@link ToolExecutionProviderEventCollector#submitEvent()} on the executing side.
     * 
     * @param type the event type identifier
     * @param data the event data
     */
    void processToolExecutionEvent(String type, String data);

    /**
     * Called after {@link ToolExecutionProvider#execute()} has terminated.
     * 
     * @param toolExecutionResult the result object as returned by
     *        {@link ToolExecutionProvider#execute(ToolExecutionProviderEventCollector)}
     */
    void onExecutionFinished(ToolExecutionResult toolExecutionResult);

    /**
     * Called before any output files are provided via {@link #receiveOutputFile(String, int, InputStream)}. This is only called if
     * execution was successful. If there are no output files, this method is still called if execution was successful.
     */
    void onOutputDownloadsStarting();

    /**
     * Returns a {@link DirectoryDownloadReceiver} to receive the output files directory generated as the result of the tool execution.
     * 
     * @return the {@link DirectoryDownloadReceiver} instance to send directory and file events to
     */
    DirectoryDownloadReceiver getOutputDirectoryReceiver();

    /**
     * Called after all output files were provided via {@link #receiveOutputFile(String, int, InputStream)}. Called if and only if
     * {@link #onOutputDownloadsStarting()} was called before.
     */
    void onOutputDownloadsFinished();

    /**
     * Called if a non-recoverable error occurred while setting up or managing the tool's execution. This is not invoked if the tool itself
     * fails or returns an error code.
     * 
     * @param message the error message
     */
    void onError(String message);

    /**
     * Called after all operations related to this execution have finished, regardless of whether the execution was successful. It is also
     * called after {@link #onError()} was invoked. Can be used for final cleanup or other operations.
     */
    void onContextClosing();

}
