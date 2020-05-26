/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.network.channel.internal;

import java.io.IOException;

import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionProvider;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionRequest;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionRequestResponse;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionResult;
import de.rcenvironment.core.communication.uplink.client.session.api.ClientSideUplinkSessionEventHandler;
import de.rcenvironment.core.communication.uplink.common.internal.MessageType;
import de.rcenvironment.core.communication.uplink.network.internal.MessageBlock;
import de.rcenvironment.core.communication.uplink.session.api.UplinkSession;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;
import de.rcenvironment.core.utils.common.exception.ProtocolException;

/**
 * Handles the message exchange of a tool execution on the "providing" end, i.e. the one where the tool should be executed.
 *
 * @author Robert Mischke
 */
public class ToolExecutionChannelProviderEndpoint extends AbstractExecutionChannelEndpoint {

    private final ClientSideUplinkSessionEventHandler sessionEventHandler;

    private final String destinationId;

    private ToolExecutionProvider toolExecutionProvider;

    private DirectoryDownloadWrapper directoryDownloadWrapper;

    public ToolExecutionChannelProviderEndpoint(UplinkSession session, long channelId,
        ClientSideUplinkSessionEventHandler sessionEventHandler, String destinationId) {
        super(session, channelId);
        this.sessionEventHandler = sessionEventHandler;
        this.destinationId = destinationId;
        this.channelState = ToolExecutionChannelState.EXPECTING_EXECUTION_REQUEST;
    }

    @Override
    public synchronized void dispose() {
        if (toolExecutionProvider != null) {
            toolExecutionProvider.onContextClosing();
        }
    }

    @Override
    protected synchronized boolean processMessageInternal(MessageBlock messageBlock) throws IOException {
        final MessageType messageType = messageBlock.getType();

        // special case: tool cancellation requests are currently accepted at any time during the tool's life cycle
        if (messageType == MessageType.TOOL_CANCELLATION_REQUEST) {
            ensure(toolExecutionProvider != null);
            toolExecutionProvider.requestCancel();
            return true; // continue listening for other messages
        }

        switch (channelState) {
        case EXPECTING_EXECUTION_REQUEST:
            validateActualVersusExpectedMessageType(messageType, MessageType.TOOL_EXECUTION_REQUEST);
            ToolExecutionRequestResponse response = processToolExecutionRequest(messageConverter.decodeToolExecutionRequest(messageBlock));
            enqueueMessageBlockForSending(messageConverter.encodeToolExecutionRequestResponse(response));
            if (response.isAccepted()) {
                // TODO otherwise, close the channel, and/or send the final result or an error message
                ensureNotDefinedYet(directoryDownloadWrapper);
                directoryDownloadWrapper = new DirectoryDownloadWrapper(toolExecutionProvider.getInputDirectoryReceiver());
                channelState = ToolExecutionChannelState.EXPECTING_DIRECTORY_DOWNLOAD;
            }
            return true;
        case EXPECTING_DIRECTORY_DOWNLOAD: // input file downloads
            ensure(directoryDownloadWrapper != null);
            directoryDownloadWrapper.processMessageBlock(messageBlock);
            if (directoryDownloadWrapper.isFinished()) {
                // input files received, start the actual execution
                channelState = ToolExecutionChannelState.EXPECTING_NO_MESSAGES; // tool execution is "send only" mode
                // spawn execution thread
                ConcurrencyUtils.getAsyncTaskService().execute("Uplink: tool execution and output file sending", () -> {
                    try {
                        runToolExecution();
                    } catch (IOException | OperationFailureException e) {
                        log.error("Error during tool execution", e);
                        // TODO propagate this error
                        return;
                    }
                    try {
                        uploadOutputFiles();
                    } catch (IOException e) {
                        // TODO propagate this error
                        log.error("Error uploading execution output files", e);
                        return;
                    }
                });
            }
            return true;
        default:
            return refuseUnexpectedMessageType(messageBlock);
        }
    }

    private ToolExecutionRequestResponse processToolExecutionRequest(ToolExecutionRequest toolExecutionRequest) throws ProtocolException {
        ensureNotDefinedYet(toolExecutionProvider);
        // TODO clarify; at this time, a ToolExecutionProvider is *always* created; should tool existence and access permission
        // be checked here already?
        toolExecutionProvider = sessionEventHandler.setUpToolExecutionProvider(toolExecutionRequest);
        return new ToolExecutionRequestResponse(true); // always send "success" result for now
    }

    private void runToolExecution() throws IOException, OperationFailureException {
        ToolExecutionProviderEventCollectorImpl eventCollector =
            new ToolExecutionProviderEventCollectorImpl((batch) -> {
                try {
                    enqueueMessageBlockForSending(messageConverter.encodeToolExecutionEvents(batch));
                } catch (IOException e) {
                    // TODO mark failure and do not attempt to send any more?
                    log.error("Error while trying to forward one or more tool execution events: " + e.toString());
                }
            }, ConcurrencyUtils.getFactory()) {
            };
        ToolExecutionResult executionResult = toolExecutionProvider.execute(eventCollector);
        // do not accept further events and wait for queued events to be sent
        try {
            eventCollector.shutdownAndAwaitCompletion();
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for event queue to complete: " + e.toString());
            Thread.currentThread().interrupt();
            return;
        }
        enqueueMessageBlockForSending(messageConverter.encodeToolExecutionResult(executionResult));
    }

    private void uploadOutputFiles() throws IOException {
        new DirectoryUploadWrapper(toolExecutionProvider.getOutputDirectoryProvider()).performDirectoryUpload();
    }
}
