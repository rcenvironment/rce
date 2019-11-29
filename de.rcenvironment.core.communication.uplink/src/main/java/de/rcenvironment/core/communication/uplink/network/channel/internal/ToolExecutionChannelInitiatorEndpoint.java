/*
 * Copyright 2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.network.channel.internal;

import java.io.IOException;
import java.util.List;

import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionEventHandler;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionProvider;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionRequest;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionRequestResponse;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionResult;
import de.rcenvironment.core.communication.uplink.client.session.api.ClientSideUplinkSession;
import de.rcenvironment.core.communication.uplink.client.session.api.ClientSideUplinkSessionEventHandler;
import de.rcenvironment.core.communication.uplink.client.session.api.ToolExecutionHandle;
import de.rcenvironment.core.communication.uplink.common.internal.MessageType;
import de.rcenvironment.core.communication.uplink.network.internal.MessageBlock;
import de.rcenvironment.core.utils.common.exception.ProtocolException;

/**
 * Handles the message exchange of a tool execution on the "initiating" end, i.e. the one that is forwarding the execution request from a
 * client in the local network.
 *
 * @author Robert Mischke
 */
public class ToolExecutionChannelInitiatorEndpoint extends AbstractExecutionChannelEndpoint {

    private ToolExecutionProvider executionProvider;

    private ToolExecutionHandle executionHandle;

    private ToolExecutionEventHandler executionEventHandler;

    private DirectoryDownloadWrapper directoryDownloadWrapper;

    public ToolExecutionChannelInitiatorEndpoint(ClientSideUplinkSession session, long channelId,
        ClientSideUplinkSessionEventHandler sessionEventHandler) {
        super(session, channelId);
    }

    /**
     * Starts the execution sequence.
     * 
     * @param toolExecutionRequest the {@link ToolExecutionRequest} to transmit
     * @param eventHandler the handler for execution-related callbacks
     * @throws IOException on I/O or protocol errors
     */
    public void initiateToolExecution(ToolExecutionRequest toolExecutionRequest, ToolExecutionEventHandler eventHandler)
        throws IOException {
        this.executionEventHandler = eventHandler;
        executionHandle = new ToolExecutionHandle() {

            @Override
            public void requestCancel() {
                try {
                    enqueueMessageBlockForSending(messageConverter.createToolCancellationRequest());
                } catch (IOException e) {
                    // as there is currently no mechanism to explicitly notify the user, simply log this as a warning
                    log.error("Failed to deliver a tool cancellation request through an Uplink connection: " + e.toString());
                }
            }
        };
        enqueueMessageBlockForSending(messageConverter.encodeToolExecutionRequest(toolExecutionRequest));
        channelState = ToolExecutionChannelState.EXPECTING_EXECUTION_REQUEST_RESPONSE;
    }

    @Override
    public void dispose() {
        // TODO (p1) 11.0: check: any operations to perform here?
    }

    protected synchronized boolean processMessageInternal(MessageBlock messageBlock) throws IOException {
        final MessageType messageType = messageBlock.getType();
        if (messageType == MessageType.CHANNEL_CLOSE) {
            // TODO additional teardown?
            return false;
        }

        // note: the protocol exchange is fairly linear, so a full state machine would be overkill at this point -- misc_ro
        switch (channelState) {
        case EXPECTING_EXECUTION_REQUEST_RESPONSE:
            validateActualVersusExpectedMessageType(messageType, MessageType.TOOL_EXECUTION_REQUEST_RESPONSE);
            final ToolExecutionRequestResponse response = messageConverter.decodeToolExecutionRequestResponse(messageBlock);
            if (!response.isAccepted()) {
                log.debug("Failed to set up remote tool execution, aborting");
                // TODO other error/shutdown events
                executionEventHandler.onContextClosing();
                return false;
            }

            log.debug("Successfully set up remote tool execution, preparing to upload the input files");
            channelState = ToolExecutionChannelState.EXPECTING_NO_MESSAGES;
            // input upload sequence
            executionEventHandler.onInputUploadsStarting();
            uploadInputFiles();
            executionEventHandler.onInputUploadsFinished();
            // the end of input uploads implies starting the execution on the remote side, so expect related events
            executionEventHandler.onExecutionStarting();
            channelState = ToolExecutionChannelState.EXPECTING_EXECUTION_EVENTS;
            return true;
        case EXPECTING_EXECUTION_EVENTS:
            if (messageType == MessageType.TOOL_EXECUTION_EVENTS) {
                final List<ToolExecutionProviderEventTransferObject> toolExecutionEvents =
                    messageConverter.decodeToolExecutionEvents(messageBlock);
                for (ToolExecutionProviderEventTransferObject event : toolExecutionEvents) {
                    executionEventHandler.processToolExecutionEvent(event.t, event.d);
                }
                return true;
            }
            validateActualVersusExpectedMessageType(messageType, MessageType.TOOL_EXECUTION_FINISHED);
            final ToolExecutionResult toolExecutionResult = messageConverter.decodeToolExecutionResult(messageBlock);
            executionEventHandler.onExecutionFinished(toolExecutionResult);
            // prepare output directory download
            ensureNotDefinedYet(directoryDownloadWrapper);
            directoryDownloadWrapper = new DirectoryDownloadWrapper(executionEventHandler.getOutputDirectoryReceiver());
            executionEventHandler.onOutputDownloadsStarting();
            channelState = ToolExecutionChannelState.EXPECTING_DIRECTORY_DOWNLOAD;
            return true;
        case EXPECTING_DIRECTORY_DOWNLOAD: // output file downloads
            ensure(directoryDownloadWrapper != null);
            directoryDownloadWrapper.processMessageBlock(messageBlock);
            if (directoryDownloadWrapper.isFinished()) {
                channelState = ToolExecutionChannelState.EXPECTING_NO_MESSAGES; // all finished
                // TODO do not actually finish here; expect the final result object
                executionEventHandler.onOutputDownloadsFinished();
                executionEventHandler.onContextClosing();
                return false;
            } else {
                return true;
            }
        default:
            throw new ProtocolException("Received an unexpected message block (type: " + messageType + ") while in state " + channelState);
        }

    }

    private void uploadInputFiles() throws IOException {
        new DirectoryUploadWrapper(executionEventHandler.getInputDirectoryProvider()).performDirectoryUpload();
    }

    public ToolExecutionHandle getExecutionHandle() {
        return executionHandle;
    }

}
