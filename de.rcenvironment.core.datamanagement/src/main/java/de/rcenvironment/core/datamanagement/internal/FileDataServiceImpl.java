/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.internal;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.Exchanger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.authorization.AuthorizationException;
import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.datamanagement.FileDataService;
import de.rcenvironment.core.datamanagement.RemotableFileDataService;
import de.rcenvironment.core.datamanagement.backend.DataBackend;
import de.rcenvironment.core.datamanagement.commons.BinaryReference;
import de.rcenvironment.core.datamanagement.commons.DataReference;
import de.rcenvironment.core.datamanagement.commons.MetaDataSet;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Implementation of the {@link FileDataService}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (added buffering and new remote upload)
 * @author Brigitte Boden (added method for small uploads)
 */
public class FileDataServiceImpl implements FileDataService {

    private static final String UPLOAD_BLOCK_SIZE_PROPERTY = "communication.uploadBlockSize";

    // arbitrarily chosen; BufferedInputStream default is 8kb
    private static final int DOWNLOAD_STREAM_BUFFER_SIZE = 256 * 1024;

    private static final int DEFAULT_UPLOAD_CHUNK_SIZE = 256 * 1024;

    private static final int MINIMUM_UPLOAD_CHUNK_SIZE = 8 * 1024;

    private static final long CHUNK_UPLOAD_TIME_WARNING_THRESHOLD_MSEC = 25000;

    private static final int REMOTE_REFERENCE_POLLING_INTERVAL_MSEC = 1000;

    private static final int END_OF_STREAM_MARKER = -1;

    private static final String DRCE_DEACTIVATE_SINGLE_STEP_UPDATE = "rce.upload.deactivateSingleStepUpdate";

    private PlatformService platformService;

    private final Log log = LogFactory.getLog(getClass());

    private final int uploadChunkSize;

    private CommunicationService communicationService;

    /**
     * A {@link Runnable} to handle asynchronous sending of byte buffers to a remote node.
     * 
     * @author Robert Mischke
     */
    private abstract class AsyncBufferUploader implements Runnable {

        private int bufferSize;

        private Exchanger<ChunkBuffer> exchanger = new Exchanger<ChunkBuffer>();

        AsyncBufferUploader(int bufferSize) {
            this.bufferSize = bufferSize;
        }

        @Override
        @TaskDescription("Async upload")
        public void run() {
            // create second buffer
            ChunkBuffer readyToReturnBuffer = new ChunkBuffer(bufferSize);
            try {
                while (true) {
                    ChunkBuffer filledBuffer = exchanger.exchange(readyToReturnBuffer);
                    if (filledBuffer == null) {
                        // shutdown signal ("poison pill")
                        log.debug("Async uploader shutting down");
                        break;
                    }
                    try {
                        sendSingleBuffer(filledBuffer);
                    } catch (IOException e) {
                        log.debug("I/O exception during async upload", e);
                        // register the exception in the current buffer; on the next
                        // "exchange()" call, the exception will be thrown at the caller
                        filledBuffer.setExceptionOnWrite(e);
                        exchanger.exchange(filledBuffer);
                        break;
                    }
                    readyToReturnBuffer = filledBuffer;
                }
            } catch (InterruptedException e) {
                log.warn("Async upload thread interrupted");
            }
        }

        protected abstract void sendSingleBuffer(ChunkBuffer filledBuffer) throws IOException;

        ChunkBuffer getInitialEmptyBuffer() {
            ChunkBuffer firstBuffer = new ChunkBuffer(bufferSize);
            return firstBuffer;
        }

        ChunkBuffer swapBuffersWhenReady(ChunkBuffer filledBuffer) throws InterruptedException, IOException {
            ChunkBuffer returnedBuffer = exchanger.exchange(filledBuffer);
            IOException exceptionOnWrite = returnedBuffer.getExceptionOnWrite();
            if (exceptionOnWrite != null) {
                // re-throw async exceptions for caller
                throw exceptionOnWrite;
            }
            return returnedBuffer;
        }

        void shutDown() throws InterruptedException, IOException {
            // note: this also ensures that the last (and final) buffer was written - misc_ro
            ChunkBuffer returnedBuffer = exchanger.exchange(null);
            // check final buffer for async exception
            IOException exceptionOnWrite = returnedBuffer.getExceptionOnWrite();
            if (exceptionOnWrite != null) {
                // re-throw async exceptions for caller
                throw exceptionOnWrite;
            }
        }
    }

    /**
     * Simple holder for a fixed-size buffer and actual content length information.
     * 
     * @author Robert Mischke
     */
    private final class ChunkBuffer {

        private byte[] buffer;

        private int contentSize;

        private IOException exceptionOnWrite;

        ChunkBuffer(int bufferSize) {
            buffer = new byte[bufferSize];
            contentSize = 0;
        }

        public void setExceptionOnWrite(IOException e) {
            exceptionOnWrite = e;
        }

        public IOException getExceptionOnWrite() {
            return exceptionOnWrite;
        }

        public int getContentSize() {
            return contentSize;
        }

        public byte[] getContentSizeBuffer() {
            // shorten array to actual content if necessary.
            // note that the main buffer is kept at maximum chunk size to avoid performance
            // degradation if source streams produce reads of different sizes - misc_ro
            if (contentSize == buffer.length) {
                return buffer;
            } else {
                return Arrays.copyOf(buffer, contentSize);
            }
        }

        public boolean fillFromStream(InputStream inputStream) throws IOException {
            int actualRead = inputStream.read(buffer);
            // sanity check
            if (actualRead == 0) {
                throw new IllegalStateException("Read zero bytes");
            }
            if (actualRead >= 0) {
                contentSize = actualRead;
            } else {
                // ContentSize cannot be negative.
                contentSize = 0;
            }
            while (actualRead < buffer.length && actualRead != END_OF_STREAM_MARKER) {
                // Theoretically, it is possible that the read method didn't fill the buffer although the end of the stream is not reached.
                // In this case, read until the buffer is full or the end is reached.
                actualRead = inputStream.read(buffer, contentSize, buffer.length - contentSize);
                // sanity check
                if (actualRead == 0) {
                    throw new IllegalStateException("Read zero bytes");
                }
                if (actualRead > 0) {
                    contentSize += actualRead;
                }
            }

            return actualRead != END_OF_STREAM_MARKER;
        }
    }

    public FileDataServiceImpl() {
        int tempHolder = DEFAULT_UPLOAD_CHUNK_SIZE;
        String chunkSizeArg = System.getProperty(UPLOAD_BLOCK_SIZE_PROPERTY);
        if (chunkSizeArg != null) {
            try {
                int parsedValue = Integer.parseInt(chunkSizeArg);
                if (parsedValue >= MINIMUM_UPLOAD_CHUNK_SIZE) {
                    // apply
                    tempHolder = parsedValue;
                } else {
                    log.error("Invalid upload block size specified: minimum value is " + MINIMUM_UPLOAD_CHUNK_SIZE);
                }
            } catch (NumberFormatException e) {
                log.error("Failed to parse " + UPLOAD_BLOCK_SIZE_PROPERTY + " setting; using default", e);
            }

        }
        uploadChunkSize = tempHolder;
        log.debug("Using remote upload block size " + uploadChunkSize);
    }

    protected void activate(BundleContext bundleContext) {}

    protected void bindCommunicationService(CommunicationService newCommunicationService) {
        communicationService = newCommunicationService;
    }

    protected void bindPlatformService(PlatformService newPlatformService) {
        platformService = newPlatformService;
    }

    @Override
    public InputStream getStreamFromDataReference(DataReference dataReference)
        throws AuthorizationException, CommunicationException {
        return getStreamFromDataReference(dataReference, true);
    }

    @Override
    public InputStream getStreamFromDataReference(DataReference dataReference, boolean decompress) throws AuthorizationException,
        CommunicationException {
        try {
            InputStream rawStream = getRemoteFileDataService(dataReference.getStorageNodeId()).getStreamFromDataReference(dataReference,
                !platformService.matchesLocalInstance(dataReference.getStorageNodeId()), decompress);
            return new BufferedInputStream(rawStream, DOWNLOAD_STREAM_BUFFER_SIZE);
        } catch (RemoteOperationException e) {
            throw new CommunicationException(StringUtils.format("Failed to get stream from data reference from remote node %s: ",
                dataReference.getStorageNodeId()) + e.getMessage());
        }
    }

    @Override
    public DataReference newReferenceFromStream(InputStream inputStream, MetaDataSet metaDataSet, NetworkDestination platform)
        throws AuthorizationException, IOException, InterruptedException, CommunicationException {

        return newReferenceFromStream(inputStream, metaDataSet, platform, false);
    }

    @Override
    public DataReference newReferenceFromStream(InputStream inputStream, MetaDataSet metaDataSet, NetworkDestination platform,
        boolean alreadyCompressed)
        throws AuthorizationException, IOException, InterruptedException, CommunicationException {

        if (platform == null) {
            platform = platformService.getLocalInstanceNodeSessionId();
        }

        // local case: upload to local data store and return reference
        if (platform instanceof ResolvableNodeId && platformService.matchesLocalInstance((ResolvableNodeId) platform)) {
            try {
                return getRemoteFileDataService(platform).newReferenceFromStream(inputStream, metaDataSet, alreadyCompressed);
            } catch (RemoteOperationException e) {
                throw new CommunicationException(StringUtils.format(
                    "Failed to create new data reference from stream from remote node @%s: ", platform)
                    + e.getMessage());
            }
        } else {
            return performRemoteUpload(inputStream, metaDataSet, platform, alreadyCompressed);
        }
    }

    private DataReference performRemoteUpload(InputStream inputStream, MetaDataSet metaDataSet,
        final NetworkDestination remoteNodeId, boolean alreadyCompressed) throws InterruptedException, IOException, CommunicationException {
        final RemotableFileDataService remoteDataService =
            (RemotableFileDataService) communicationService.getRemotableService(RemotableFileDataService.class, remoteNodeId);
        try {
            ChunkBuffer readBuffer = new ChunkBuffer(uploadChunkSize);
            readBuffer.fillFromStream(inputStream);

            if (!System.getProperties().containsKey(DRCE_DEACTIVATE_SINGLE_STEP_UPDATE) && readBuffer.getContentSize() < uploadChunkSize) {
                log.debug("Data to upload is smaller than chunk size: performing upload in single step.");
                DataReference reference =
                    remoteDataService.uploadInSingleStep(readBuffer.getContentSizeBuffer(), metaDataSet, alreadyCompressed);
                return reference;
            } else {

                final String uploadId = remoteDataService.initializeUpload();
                // sanity check
                if (uploadId == null) {
                    throw new NullPointerException("Received null upload id");
                }
                log.debug("Received remote upload id " + uploadId);

                // create and start async uploader
                AsyncBufferUploader asyncUploader = new AsyncBufferUploader(uploadChunkSize) {

                    private long localTotalSize = 0;

                    @Override
                    protected void sendSingleBuffer(ChunkBuffer filledBuffer) throws IOException {
                        // log.debug("uploading chunk of size " + filledBuffer.getContentSize());
                        long startTime = System.currentTimeMillis();
                        long remoteTotalSize;
                        try {
                            remoteTotalSize = remoteDataService.appendToUpload(uploadId, filledBuffer.getContentSizeBuffer());
                        } catch (RemoteOperationException e) {
                            throw new RuntimeException(StringUtils.format(
                                "Failed to create new data reference from stream from remote node @%s: ", remoteNodeId)
                                + e.getMessage());
                        }
                        long duration = System.currentTimeMillis() - startTime;
                        if (duration > CHUNK_UPLOAD_TIME_WARNING_THRESHOLD_MSEC) {
                            log.warn(StringUtils.format("Uploading a data block of %d bytes took %d msec", filledBuffer.getContentSize(),
                                duration));
                        }
                        localTotalSize += filledBuffer.getContentSize();
                        // consistency check
                        if (localTotalSize != remoteTotalSize) {
                            throw new IllegalStateException("Consistency error: Local and remote write counts are not equal!");
                        }
                    }
                };
                ConcurrencyUtils.getAsyncTaskService().execute("Async upload", asyncUploader);

                long totalRead2 = 0; // other counter is held by writer thread; duplicate to avoid sync
                do {
                    totalRead2 += readBuffer.getContentSize();
                    readBuffer = asyncUploader.swapBuffersWhenReady(readBuffer);
                } while (readBuffer.fillFromStream(inputStream));
                if (readBuffer.getContentSize() > 0) {
                    // Because the fillFromStream method tries reading from the stream again if not the full buffer size was read,
                    // it is possible that in the last iteration fillFromStream returns false, but the last content is still in the buffer.
                    totalRead2 += readBuffer.getContentSize();
                    readBuffer = asyncUploader.swapBuffersWhenReady(readBuffer);
                }
                asyncUploader.shutDown();
                remoteDataService.finishUpload(uploadId, metaDataSet, alreadyCompressed);
                log.debug(StringUtils.format("Finished uploading %d bytes for upload id %s; polling for remote data reference", totalRead2,
                    uploadId));

                // poll until successful or the thread is interrupted
                while (true) {
                    Thread.sleep(REMOTE_REFERENCE_POLLING_INTERVAL_MSEC);
                    DataReference reference = remoteDataService.pollUploadForDataReference(uploadId);
                    if (reference != null) {
                        log.debug("Received remote data reference for upload id " + uploadId);
                        return reference;
                    }
                }
            }

        } catch (IOException e) {
            throw new IOException("Error uploading file", e);
        } catch (RemoteOperationException e) {
            throw new CommunicationException(StringUtils.format("Failed to perform remote upload to node @%s: ", remoteNodeId)
                + e.getMessage());
        }
    }

    @Override
    public void deleteReference(DataReference dataReference) throws CommunicationException {

        try {
            for (BinaryReference br : dataReference.getBinaryReferences()) {
                getRemoteFileDataService(dataReference.getStorageNodeId()).deleteReference(br.getBinaryReferenceKey());
            }
        } catch (RemoteOperationException e) {
            throw new CommunicationException(StringUtils.format("Failed to delete data reference on remote node %s: ",
                dataReference.getStorageNodeId()) + e.getMessage());
        }
    }

    private RemotableFileDataService getRemoteFileDataService(NetworkDestination nodeId) throws RemoteOperationException {
        return (RemotableFileDataService) communicationService.getRemotableService(RemotableFileDataService.class, nodeId);
    }

    @Override
    public void deleteReference(String binaryReferenceKey) throws RemoteOperationException {
        DataBackend dataService =
            BackendSupport.getDataBackend();
        URI location =
            dataService.suggestLocation(
                UUID.fromString(binaryReferenceKey));
        dataService.delete(location);
    }

}
