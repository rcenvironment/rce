/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.network.channel.internal;

import java.io.IOException;
import java.io.PipedInputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Semaphore;

import de.rcenvironment.core.communication.uplink.client.execution.api.DirectoryDownloadReceiver;
import de.rcenvironment.core.communication.uplink.client.execution.api.DirectoryUploadContext;
import de.rcenvironment.core.communication.uplink.client.execution.api.DirectoryUploadProvider;
import de.rcenvironment.core.communication.uplink.client.execution.api.FileDataSource;
import de.rcenvironment.core.communication.uplink.common.internal.DataStreamDownloadWrapper;
import de.rcenvironment.core.communication.uplink.common.internal.DataStreamUploadWrapper;
import de.rcenvironment.core.communication.uplink.common.internal.MessageType;
import de.rcenvironment.core.communication.uplink.entities.FileHeader;
import de.rcenvironment.core.communication.uplink.entities.FileTransferSectionInfo;
import de.rcenvironment.core.communication.uplink.network.internal.MessageBlock;
import de.rcenvironment.core.communication.uplink.session.api.UplinkSession;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.ProtocolException;

/**
 * Common base class of {@link ToolExecutionChannelInitiatorEndpoint} and {@link ToolExecutionChannelProviderEndpoint}.
 *
 * @author Robert Mischke
 */
public abstract class AbstractExecutionChannelEndpoint extends AbstractChannelEndpoint {

    // synchronized on "this"
    protected ToolExecutionChannelState channelState = ToolExecutionChannelState.EXPECTING_NO_MESSAGES;

    /**
     * Encapsulates handling the message sequence at the sending end of a directory transfer. Sends messages starting with
     * {@link MessageType#FILE_TRANSFER_SECTION_START} and ending with {@link MessageType#FILE_TRANSFER_SECTION_END}, unless there is a
     * critical error, in which case ... TODO specify abort behavior.
     *
     * @author Robert Mischke
     */
    protected final class DirectoryUploadWrapper {

        private DirectoryUploadProvider localProvider;

        public DirectoryUploadWrapper(DirectoryUploadProvider localProvider) {
            this.localProvider = localProvider;
        }

        public void performDirectoryUpload() throws IOException {
            final List<String> directoryListing = localProvider.provideDirectoryListing();
            enqueueMessageBlockForSending(messageConverter.encodeFileTransferSectionStart(new FileTransferSectionInfo(directoryListing)));
            localProvider.provideFiles(new DirectoryUploadContext() {

                @Override
                public void provideFile(FileDataSource dataSource) throws IOException {
                    FileHeader fileHeader = new FileHeader(dataSource.getSize(), dataSource.getRelativePath());
                    log.debug(
                        StringUtils.format("Starting upload of file '%s', size: %d bytes", fileHeader.getPath(), fileHeader.getSize()));
                    enqueueMessageBlockForSending(messageConverter.encodeFileHeader(fileHeader));
                    new DataStreamUploadWrapper(asyncMessageBlockSender).uploadFromDataSource(channelId, MessageType.FILE_CONTENT,
                        dataSource);
                }
            });
            enqueueMessageBlockForSending(new MessageBlock(MessageType.FILE_TRANSFER_SECTION_END));
        }
    }

    /**
     * Encapsulates handling the message sequence at the receiving end of a directory transfer. Expects the first message to be
     * {@link MessageType#FILE_TRANSFER_SECTION_START} for consistency checking, and consumes up to (including)
     * {@link MessageType#FILE_TRANSFER_SECTION_END} before setting {@link #isFinished()} to true.
     *
     * @author Robert Mischke
     */
    protected final class DirectoryDownloadWrapper {

        private DirectoryDownloadReceiver localReceiver;

        private boolean initialized;

        private DataStreamDownloadWrapper<FileDataSource> currentDownloadWrapper;

        private boolean receivedEndOfTransferMessage;

        private Semaphore receiveFileMethodsLock = new Semaphore(1);

        public DirectoryDownloadWrapper(DirectoryDownloadReceiver localReceiver) {
            this.localReceiver = localReceiver;
        }

        public void processMessageBlock(MessageBlock messageBlock) throws IOException {
            // note: unlike the main endpoints, this switches on message type, not internal state as this simplifies handling
            switch (messageBlock.getType()) {
            case FILE_TRANSFER_SECTION_START:
                ensure(!initialized);
                final FileTransferSectionInfo fileTransferSectionInfo = messageConverter.decodeFileTransferSectionStart(messageBlock);
                final Optional<List<String>> optionalListOfDirectories = fileTransferSectionInfo.getDirectoriesAsOptional();
                if (optionalListOfDirectories.isPresent()) {
                    localReceiver.receiveDirectoryListing(optionalListOfDirectories.get());
                }
                initialized = true;
                return;
            case FILE_HEADER:
                ensure(initialized);
                ensure(currentDownloadWrapper == null);
                final FileHeader fileHeader = messageConverter.decodeFileHeader(messageBlock);
                try {
                    receiveFileMethodsLock.acquire();
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                    log.warn("Interrupted while preparing download of " + fileHeader.getPath());
                }
                log.debug(StringUtils.format("Starting download of file '%s', size: %d bytes", fileHeader.getPath(), fileHeader.getSize()));
                currentDownloadWrapper = new DataStreamDownloadWrapper<FileDataSource>() {

                    @Override
                    public FileDataSource createReturnObject(long sizeParam, PipedInputStream inputStream) {
                        return new FileDataSource(fileHeader.getPath(), sizeParam, inputStream);
                    }
                };
                final FileDataSource fileDataSource = currentDownloadWrapper.initialize(fileHeader.getSize(), MessageType.FILE_CONTENT);
                if (fileHeader.getSize() == 0) {
                    currentDownloadWrapper = null; // ensure consistency in the empty file case where no data block will be sent
                }

                // the receiveFile() method is blocking, so run it in a separate thread
                ConcurrencyUtils.getAsyncTaskService().execute("Uplink: receive a file download", () -> {
                    try {
                        localReceiver.receiveFile(fileDataSource);
                    } catch (IOException e) {
                        // TODO propagate this error
                        log.error("Error while downloading file " + fileHeader.getPath(), e);
                    } finally {
                        receiveFileMethodsLock.release();
                        log.debug("Finished download of " + fileHeader.getPath());
                    }
                });
                return;
            case FILE_CONTENT:
                ensure(currentDownloadWrapper != null);
                final boolean fileComplete = currentDownloadWrapper.processMessageBlock(messageBlock);
                if (fileComplete) {
                    currentDownloadWrapper = null; // for consistency checking
                }
                return;
            case FILE_TRANSFER_SECTION_END:
                ensure(initialized);
                ensure(currentDownloadWrapper == null);
                receivedEndOfTransferMessage = true;
                return;
            default:
                throw new ProtocolException("Unexpected message type during directory download: " + messageBlock.getType());
            }
        }

        public boolean isFinished() throws IOException {
            if (receivedEndOfTransferMessage) {
                try {
                    // make sure that all pending receiveFile() methods have completed before returning "true"
                    receiveFileMethodsLock.acquire();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for all receiveFile() methods to complete");
                }
                return true;
            } else {
                return false;
            }
        }
    }

    public AbstractExecutionChannelEndpoint(UplinkSession session, long channelId) {
        super(session, session.getLocalSessionId(), channelId);
    }

    protected void validateExpectedChannelState(ToolExecutionChannelState expectedState, MessageBlock message) throws ProtocolException {
        if (channelState != expectedState) {
            throw new ProtocolException(
                "Received a message of type " + message.getType() + " in channel state " + channelState.name() + " when it should be "
                    + expectedState.name());
        }
    }

    protected void validateActualVersusExpectedMessageType(MessageType actual, MessageType expected) throws ProtocolException {
        if (actual != expected) {
            throw new ProtocolException("Expected a message of type " + expected + ", but received " + actual);
        }
    }

    protected void ensure(boolean condition) throws ProtocolException {
        if (!condition) {
            throw new ProtocolException("An internal check or condition was not in the required state; "
                + "this indicates a protocol violation or an internal error");
        }
    }

    protected void ensureNotDefinedYet(Object object) throws ProtocolException {
        if (object != null) {
            throw new ProtocolException("An internal field was about to be initialized, "
                + "but was already set; this indicates a protocol violation or an internal error");
        }
    }
}
