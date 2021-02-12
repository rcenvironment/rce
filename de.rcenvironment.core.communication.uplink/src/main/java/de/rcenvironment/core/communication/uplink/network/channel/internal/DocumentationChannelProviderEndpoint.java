/*
 * Copyright 2019-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.network.channel.internal;

import java.io.IOException;
import java.util.Optional;

import de.rcenvironment.core.communication.uplink.client.session.api.ClientSideUplinkSession;
import de.rcenvironment.core.communication.uplink.client.session.api.ClientSideUplinkSessionEventHandler;
import de.rcenvironment.core.communication.uplink.common.internal.DataStreamUploadWrapper;
import de.rcenvironment.core.communication.uplink.common.internal.MessageType;
import de.rcenvironment.core.communication.uplink.entities.ToolDocumentationRequest;
import de.rcenvironment.core.communication.uplink.entities.ToolDocumentationResponse;
import de.rcenvironment.core.communication.uplink.network.internal.MessageBlock;
import de.rcenvironment.core.utils.common.SizeValidatedDataSource;
import de.rcenvironment.core.utils.common.exception.ProtocolException;

/**
 * The provider side end of a documentation request channel.
 *
 * @author Robert Mischke
 */
public class DocumentationChannelProviderEndpoint extends AbstractChannelEndpoint {

    private final ClientSideUplinkSessionEventHandler sessionEventHandler;

    private final String destinationId;

    public DocumentationChannelProviderEndpoint(ClientSideUplinkSession session, long channelId,
        ClientSideUplinkSessionEventHandler sessionEventHandler, String destinationId) {
        super(session, session.getLocalSessionId(), channelId);
        this.sessionEventHandler = sessionEventHandler;
        this.destinationId = destinationId;
    }

    @Override
    protected boolean processMessageInternal(MessageBlock messageBlock) throws IOException {

        MessageType messageType = messageBlock.getType();

        switch (messageType) {
        case TOOL_DOCUMENTATION_REQUEST:
            final ToolDocumentationRequest request = messageConverter.decodeDocumentationRequest(messageBlock);
            final Optional<SizeValidatedDataSource> optionalData =
                sessionEventHandler.provideToolDocumentationData(destinationId, request.getReferenceId());
            if (optionalData.isPresent()) {
                SizeValidatedDataSource data = optionalData.get();
                log.debug("Documentation data for reference id " + request.getReferenceId() + " is available, size " + data.getSize());
                // TODO introduce a better maximum documentation size; this limit equals ~2 GB
                if (data.getSize() > Integer.MAX_VALUE) {
                    throw new IllegalArgumentException();
                }
                sendResponseHeader(true, data.getSize());
                sendDocumentationData(channelId, data);
            } else {
                log.debug(
                    "No documentation data for reference id " + request.getReferenceId() + " found, creating empty data response");
                sendResponseHeader(false, 0);
            }
            return true;
        default:
            throw new ProtocolException("Received unexpected message type " + messageType);
        }
    }

    @Override
    public void dispose() {
        // TODO (p1) 11.0: check: any operations to perform here?
    }

    private void sendResponseHeader(boolean available, long size) throws IOException {
        enqueueMessageBlockForSending(messageConverter.encodeDocumentationResponse(new ToolDocumentationResponse(available, size)));
    }

    private void sendDocumentationData(final long channelId, SizeValidatedDataSource data) throws IOException {
        try {
            final DataStreamUploadWrapper uploadWrapper =
                new DataStreamUploadWrapper(asyncMessageBlockSender);
            uploadWrapper.uploadFromDataSource(channelId, MessageType.TOOL_DOCUMENTATION_CONTENT, data);
        } catch (IOException e) {
            log.error("Error while converting documentation data into a network message", e);
            sendResponseHeader(false, 0);
        }
    }

}
