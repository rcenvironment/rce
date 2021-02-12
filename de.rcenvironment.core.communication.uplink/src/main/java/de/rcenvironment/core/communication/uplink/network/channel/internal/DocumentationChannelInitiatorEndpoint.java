/*
 * Copyright 2019-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.network.channel.internal;

import java.io.IOException;
import java.io.PipedInputStream;
import java.util.Optional;
import java.util.function.Consumer;

import de.rcenvironment.core.communication.uplink.client.session.api.ClientSideUplinkSession;
import de.rcenvironment.core.communication.uplink.common.internal.DataStreamDownloadWrapper;
import de.rcenvironment.core.communication.uplink.common.internal.MessageType;
import de.rcenvironment.core.communication.uplink.entities.ToolDocumentationResponse;
import de.rcenvironment.core.communication.uplink.network.internal.MessageBlock;
import de.rcenvironment.core.utils.common.SizeValidatedDataSource;
import de.rcenvironment.core.utils.common.exception.ProtocolException;

/**
 * The provider side end of a documentation request channel.
 *
 * @author Robert Mischke
 */
public class DocumentationChannelInitiatorEndpoint extends AbstractChannelEndpoint {

    private Consumer<Optional<SizeValidatedDataSource>> documentationReceiver;

    private DataStreamDownloadWrapper downloadWrapper;

    public DocumentationChannelInitiatorEndpoint(ClientSideUplinkSession session, long channelId,
        Consumer<Optional<SizeValidatedDataSource>> documentationReceiver) {
        super(session, session.getLocalSessionId(), channelId);
        this.documentationReceiver = documentationReceiver;
    }

    @Override
    protected boolean processMessageInternal(MessageBlock messageBlock) throws IOException {

        MessageType messageType = messageBlock.getType();
        switch (messageType) {
        case TOOL_DOCUMENTATION_RESPONSE:
            if (downloadWrapper != null) {
                throw new ProtocolException("Received more than one documentation response header");
            }
            final ToolDocumentationResponse response = messageConverter.decodeDocumentationResponse(messageBlock);
            if (response.isAvailable()) {
                downloadWrapper = new DataStreamDownloadWrapper<SizeValidatedDataSource>() {

                    @Override
                    public SizeValidatedDataSource createReturnObject(long size, PipedInputStream inputStream) {
                        return new SizeValidatedDataSource(size, inputStream);
                    }
                };
                final SizeValidatedDataSource downloadStream =
                    downloadWrapper.initialize(response.getSize(), MessageType.TOOL_DOCUMENTATION_CONTENT);
                documentationReceiver.accept(Optional.of(downloadStream));
            } else {
                documentationReceiver.accept(Optional.empty());
            }
            return true;
        case TOOL_DOCUMENTATION_CONTENT:
            if (downloadWrapper == null) {
                throw new ProtocolException("Received documentation data without preceding header");
            }
            if (!downloadWrapper.processMessageBlock(messageBlock)) {
                log.debug("Completed documentation download, closing channel");
                return false;
            }
            return true; // continue
        default:
            throw new ProtocolException("Received unexpected message type " + messageType);
        }
    }

    @Override
    public void dispose() {
        // TODO (p1) 11.0: check: any operations to perform here?
    }

}
