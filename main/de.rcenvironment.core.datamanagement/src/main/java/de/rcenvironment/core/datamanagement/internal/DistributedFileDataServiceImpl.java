/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.internal;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.Exchanger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.authentication.User;
import de.rcenvironment.core.authorization.AuthorizationException;
import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.datamanagement.DistributedFileDataService;
import de.rcenvironment.core.datamanagement.FileDataService;
import de.rcenvironment.core.datamanagement.commons.DataReference;
import de.rcenvironment.core.datamanagement.commons.MetaDataSet;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;

/**
 * Implementation of the {@link DistributedFileDataService}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (added buffering and new remote upload)
 */
public class DistributedFileDataServiceImpl extends DistributedDataServiceImpl implements DistributedFileDataService {

    private static final String UPLOAD_BLOCK_SIZE_PROPERTY = "communication.uploadBlockSize";

    // arbitrarily chosen; BufferedInputStream default is 8kb
    private static final int DOWNLOAD_STREAM_BUFFER_SIZE = 256 * 1024;

    private static final int DEFAULT_UPLOAD_CHUNK_SIZE = 256 * 1024;

    private static final int MINIMUM_UPLOAD_CHUNK_SIZE = 8 * 1024;

    private static final long CHUNK_UPLOAD_TIME_WARNING_THRESHOLD_MSEC = 25000;

    private static final int REMOTE_REFERENCE_POLLING_INTERVAL_MSEC = 1000;

    private static final int END_OF_STREAM_MARKER = -1;

    private FileDataService fileDataService;

    private PlatformService platformService;

    private final Log log = LogFactory.getLog(getClass());

    private final int uploadChunkSize;

    /**
     * A {@link Runnable} to handle asynchronous sending of byte buffers to a remote node.
     * 
     * @author Robert Mischke
     */
    private abstract class AsyncBufferUploader implements Runnable {

        private int bufferSize;

        private Exchanger<ChunkBuffer> exchanger = new Exchanger<ChunkBuffer>();

        public AsyncBufferUploader(int bufferSize) {
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

        public ChunkBuffer(int bufferSize) {
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
            contentSize = actualRead;
            return actualRead != END_OF_STREAM_MARKER;
        }
    }

    public DistributedFileDataServiceImpl() {
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

    protected void activate(BundleContext bundleContext) {
        context = bundleContext;
    }

    protected void bindCommunicationService(CommunicationService newCommunicationService) {
        communicationService = newCommunicationService;
    }

    protected void bindPlatformService(PlatformService newPlatformService) {
        platformService = newPlatformService;
    }

    protected void bindFileDataService(FileDataService newFileDataService) {
        fileDataService = newFileDataService;
    }

    @Override
    public InputStream getStreamFromDataReference(User user, DataReference dataReference)
        throws AuthorizationException {

        FileDataService dataService = (FileDataService) communicationService.getService(FileDataService.class,
            dataReference.getNodeIdentifier(), context);
        try {
            InputStream rawStream = dataService.getStreamFromDataReference(user, dataReference,
                !platformService.isLocalNode(dataReference.getNodeIdentifier()));
            return new BufferedInputStream(rawStream, DOWNLOAD_STREAM_BUFFER_SIZE);
        } catch (RuntimeException e) {
            log.warn("Failed to get stream from reference on platform: " + dataReference.getNodeIdentifier(), e);
            return null;
        }
    }

    @Override
    public DataReference newReferenceFromStream(final User user, InputStream inputStream, MetaDataSet metaDataSet, NodeIdentifier platform)
        throws AuthorizationException, IOException, InterruptedException {

        if (platform == null) {
            platform = platformService.getLocalNodeId();
        }

        // local case: upload to local data store and return reference
        if (platformService.isLocalNode(platform)) {
            return fileDataService.newReferenceFromStream(user, inputStream, metaDataSet);
        } else {
            return performRemoteUpload(user, inputStream, metaDataSet, platform);
        }
    }

    private DataReference performRemoteUpload(final User user, InputStream inputStream, MetaDataSet metaDataSet,
        NodeIdentifier remoteNodeId) throws InterruptedException, IOException {
        final FileDataService remoteDataService =
            (FileDataService) communicationService.getService(FileDataService.class, remoteNodeId, context);
        try {
            final String uploadId = remoteDataService.initializeUpload(user);
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
                    long remoteTotalSize = remoteDataService.appendToUpload(user, uploadId, filledBuffer.getContentSizeBuffer());
                    long duration = System.currentTimeMillis() - startTime;
                    if (duration > CHUNK_UPLOAD_TIME_WARNING_THRESHOLD_MSEC) {
                        log.warn(String.format("Uploading a data block of %d bytes took %d msec", filledBuffer.getContentSize(), duration));
                    }
                    localTotalSize += filledBuffer.getContentSize();
                    // consistency check
                    if (localTotalSize != remoteTotalSize) {
                        throw new IllegalStateException("Consistency error: Local and remote write counts are not equal!");
                    }
                }
            };
            SharedThreadPool.getInstance().execute(asyncUploader);

            long totalRead2 = 0; // other counter is held by writer thread; duplicate to avoid sync
            ChunkBuffer readBuffer = asyncUploader.getInitialEmptyBuffer();
            while (readBuffer.fillFromStream(inputStream)) {
                totalRead2 += readBuffer.getContentSize();
                readBuffer = asyncUploader.swapBuffersWhenReady(readBuffer);
            }
            asyncUploader.shutDown();
            remoteDataService.finishUpload(user, uploadId, metaDataSet);
            log.debug(String.format("Finished uploading %d bytes for upload id %s; polling for remote data reference", totalRead2,
                uploadId));
            // poll until successful or the thread is interrupted
            while (true) {
                Thread.sleep(REMOTE_REFERENCE_POLLING_INTERVAL_MSEC);
                DataReference reference = remoteDataService.pollUploadForDataReference(user, uploadId);
                if (reference != null) {
                    log.debug("Received remote data reference for upload id " + uploadId);
                    return reference;
                }
            }
        } catch (IOException e) {
            throw new IOException("Error uploading file", e);
        }
    }

}
