/*
 * Copyright 2019-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.network.channel.internal;

import java.io.IOException;
import java.util.List;

import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionEventHandler;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionRequest;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionRequestResponse;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionResult;
import de.rcenvironment.core.communication.uplink.client.session.api.ClientSideUplinkSession;
import de.rcenvironment.core.communication.uplink.client.session.api.ClientSideUplinkSessionEventHandler;
import de.rcenvironment.core.communication.uplink.client.session.api.ToolExecutionHandle;
import de.rcenvironment.core.communication.uplink.common.internal.MessageType;
import de.rcenvironment.core.communication.uplink.network.api.MessageBlockPriority;
import de.rcenvironment.core.communication.uplink.network.internal.MessageBlock;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.exception.ProtocolException;

/**
 * Handles the message exchange of a tool execution on the "initiating" end, i.e. the one that is forwarding the execution request from a
 * client in the local network.
 *
 * @author Robert Mischke
 */
public class ToolExecutionChannelInitiatorEndpoint extends AbstractExecutionChannelEndpoint {

    private volatile ToolExecutionHandle executionHandle; // could probably be reworked to be "final"

    private ToolExecutionEventHandler executionEventHandler; // synchronized via access methods

    private DirectoryDownloadWrapper directoryDownloadWrapper; // synchronized via access methods

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
        this.setExecutionEventHandler(eventHandler);
        executionHandle = new ToolExecutionHandle() {

            @Override
            public void requestCancel() {
                try {
                    // TODO 10.3.0+ (p3) consider moving this to a higher message priority to get ahead of bulk transfers?
                    enqueueMessageBlockForSending(messageConverter.createToolCancellationRequest(),
                        MessageBlockPriority.BLOCKABLE_CHANNEL_OPERATION, true);
                } catch (IOException e) {
                    // as there is currently no mechanism to explicitly notify the user, simply log this as a warning
                    log.error(
                        channelLogPrefix + "Failed to deliver a tool cancellation request through an Uplink connection: " + e.toString());
                }
            }
        };
        enqueueMessageBlockForSending(messageConverter.encodeToolExecutionRequest(toolExecutionRequest),
            MessageBlockPriority.BLOCKABLE_CHANNEL_OPERATION, true);
        setChannelState(ToolExecutionChannelState.EXPECTING_EXECUTION_REQUEST_RESPONSE);
    }

    @Override
    public void dispose() {
        // TODO (p1) 11.0: check: any operations to perform here?
    }

    protected boolean processMessageInternal(MessageBlock messageBlock) throws IOException {
        final MessageType messageType = messageBlock.getType();
        if (messageType == MessageType.CHANNEL_CLOSE) {
            // TODO additional teardown?
            return false;
        }

        // note: the protocol exchange is fairly linear, so a full state machine would be overkill at this point -- misc_ro
        switch (getChannelState()) {
        case EXPECTING_EXECUTION_REQUEST_RESPONSE:
            validateActualVersusExpectedMessageType(messageType, MessageType.TOOL_EXECUTION_REQUEST_RESPONSE);
            final ToolExecutionRequestResponse response = messageConverter.decodeToolExecutionRequestResponse(messageBlock);
            if (!response.isAccepted()) {
                log.debug(channelLogPrefix + "Failed to set up remote tool execution, aborting");
                // TODO other error/shutdown events
                getExecutionEventHandler().onContextClosing();
                return false;
            }

            if (VERBOSE_FILE_TRANSFER_LOGGING_ENABLED) {
                log.debug(channelLogPrefix + "Successfully set up remote tool execution, preparing to upload the input files");
            }

            // expect no return messages while uploading input files
            setChannelState(ToolExecutionChannelState.EXPECTING_NO_MESSAGES);

            getExecutionEventHandler().onInputUploadsStarting();

            // spawn a thread to upload input files without blocking the thread processing incoming messages (for all channels)
            // TODO proper cancellation is not implemented yet; will be addressed in #0017599
            ConcurrencyUtils.getAsyncTaskService().execute("Uplink: upload input files for remote tool execution", () -> {
                try {
                    uploadInputFiles();
                } catch (IOException e) {
                    log.warn(channelLogPrefix + "Error while uploading input files for remote tool execution: " + e.toString());
                    // TODO propagate this error
                    return;
                }
                getExecutionEventHandler().onInputUploadsFinished();
                // the end of input uploads implies starting the execution on the remote side, so expect related events
                // TODO 11.0.0 there could be a theoretical race condition where execution events arrive before this new state is set;
                // very unlikely, especially including network latency, but should be investigated before going 'stable'. A potential
                // fix might be setting this state before sending out the final message of the upload sequence. -- misc_ro
                setChannelState(ToolExecutionChannelState.EXPECTING_EXECUTION_EVENTS);
                getExecutionEventHandler().onExecutionStarting();
            });
            return true;
        case EXPECTING_EXECUTION_EVENTS:
            if (messageType == MessageType.TOOL_EXECUTION_EVENTS) {
                final List<ToolExecutionProviderEventTransferObject> toolExecutionEvents =
                    messageConverter.decodeToolExecutionEvents(messageBlock);
                for (ToolExecutionProviderEventTransferObject event : toolExecutionEvents) {
                    getExecutionEventHandler().processToolExecutionEvent(event.t, event.d);
                }
                return true;
            }
            validateActualVersusExpectedMessageType(messageType, MessageType.TOOL_EXECUTION_FINISHED);
            final ToolExecutionResult toolExecutionResult = messageConverter.decodeToolExecutionResult(messageBlock);
            getExecutionEventHandler().onExecutionFinished(toolExecutionResult);
            // prepare output directory download
            ensureNotDefinedYet(getDirectoryDownloadWrapper());
            setDirectoryDownloadWrapper(new DirectoryDownloadWrapper(getExecutionEventHandler().getOutputDirectoryReceiver()));
            getExecutionEventHandler().onOutputDownloadsStarting();
            setChannelState(ToolExecutionChannelState.EXPECTING_DIRECTORY_DOWNLOAD);
            return true;
        case EXPECTING_DIRECTORY_DOWNLOAD: // output file downloads
            ensure(getDirectoryDownloadWrapper() != null);
            getDirectoryDownloadWrapper().processMessageBlock(messageBlock);
            if (getDirectoryDownloadWrapper().isFinished()) {
                setChannelState(ToolExecutionChannelState.EXPECTING_NO_MESSAGES); // all finished
                // TODO do not actually finish here; expect the final result object
                getExecutionEventHandler().onOutputDownloadsFinished();
                getExecutionEventHandler().onContextClosing();
                return false;
            } else {
                return true;
            }
        default:
            throw new ProtocolException(
                channelLogPrefix + "Received an unexpected message block (type: " + messageType + ") while in state " + getChannelState());
        }

    }

    private void uploadInputFiles() throws IOException {
        new DirectoryUploadWrapper(getExecutionEventHandler().getInputDirectoryProvider()).performDirectoryUpload();
    }

    public ToolExecutionHandle getExecutionHandle() {
        return executionHandle; // volatile
    }

    // synchronized access methods to minimize synchronization scopes

    private synchronized ToolExecutionEventHandler getExecutionEventHandler() {
        return executionEventHandler;
    }

    private synchronized void setExecutionEventHandler(ToolExecutionEventHandler executionEventHandler) {
        this.executionEventHandler = executionEventHandler;
    }

    private synchronized DirectoryDownloadWrapper getDirectoryDownloadWrapper() {
        return directoryDownloadWrapper;
    }

    private synchronized void setDirectoryDownloadWrapper(DirectoryDownloadWrapper directoryDownloadWrapper) {
        this.directoryDownloadWrapper = directoryDownloadWrapper;
    }

}
