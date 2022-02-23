/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.internal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.authorization.AuthorizationException;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.datamanagement.RemotableFileDataService;
import de.rcenvironment.core.datamanagement.backend.DataBackend;
import de.rcenvironment.core.datamanagement.backend.MetaDataBackendService;
import de.rcenvironment.core.datamanagement.commons.BinaryReference;
import de.rcenvironment.core.datamanagement.commons.DataReference;
import de.rcenvironment.core.datamanagement.commons.DistributableInputStream;
import de.rcenvironment.core.datamanagement.commons.MetaData;
import de.rcenvironment.core.datamanagement.commons.MetaDataKeys;
import de.rcenvironment.core.datamanagement.commons.MetaDataSet;
import de.rcenvironment.core.datamodel.api.CompressionFormat;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;
import de.rcenvironment.toolkit.utils.common.IdGenerator;

/**
 * Implementation of the {@link RemotableFileDataService}.
 * 
 * @author Juergen Klein
 * @author Brigitte Boden
 */
public class RemotableFileDataServiceImpl implements RemotableFileDataService {

    protected static final String PASSED_USER_IS_NOT_VALID = "Passed user representation is not valid.";

    private static final Log LOGGER = LogFactory.getLog(RemotableFileDataServiceImpl.class);

    private static final int UPLOAD_TEMP_FILE_STREAM_BUFFER_SIZE = 64 * 1024; // arbitrary

    private static final int UPLOAD_SESSION_ID_LENGTH = 32;

    protected PlatformService platformService;

    protected BundleContext context;

    private Map<String, UploadHolder> uploads; // synchronized map

    /**
     * Local data related to a single upload.
     * 
     * @author Robert Mischke
     */
    private static class UploadHolder {

        private File tempFile;

        private OutputStream outputStream;

        private long totalBytesWritten = 0;

        private DataReference dataReference;

        private IOException asyncException;

        UploadHolder() throws IOException {
            this.tempFile = TempFileServiceAccess.getInstance().createTempFileFromPattern("upload.*.tmp");
            this.outputStream = new BufferedOutputStream(new FileOutputStream(tempFile), UPLOAD_TEMP_FILE_STREAM_BUFFER_SIZE);
        }

        /**
         * @return the total number of bytes written so far
         */
        public long appendData(byte[] data) throws IOException {
            outputStream.write(data);
            totalBytesWritten += data.length;
            return totalBytesWritten;
        }

        public File finishAndGetFile() throws IOException {
            outputStream.close();
            outputStream = null;
            return tempFile;
        }

        public synchronized void setDataReference(DataReference dataReference) throws IOException {
            this.dataReference = dataReference;
            // dispose temp file
            TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(tempFile);
            tempFile = null;
        }

        public synchronized DataReference getDataReference() {
            return dataReference;
        }

        public synchronized void setAsyncException(IOException asyncException) {
            this.asyncException = asyncException;
        }

        public synchronized IOException getAsyncException() {
            return asyncException;
        }
    }

    public RemotableFileDataServiceImpl() {
        uploads = Collections.synchronizedMap(new HashMap<String, UploadHolder>());
    }

    protected void activate(BundleContext bundleContext) {
        context = bundleContext;
    }

    protected void bindPlatformService(PlatformService newPlatformService) {
        platformService = newPlatformService;
    }

    // override without change because the @AllowRemoteAccess check does not detect superclass annotations
    @Override
    @AllowRemoteAccess
    public void deleteReference(String binaryReferenceKey) throws AuthorizationException {

        DataBackend dataService =
            BackendSupport.getDataBackend();
        URI location =
            dataService.suggestLocation(
                UUID.fromString(binaryReferenceKey));
        dataService.delete(location);
    }

    @Override
    @AllowRemoteAccess
    public InputStream getStreamFromDataReference(DataReference dataReference, Boolean calledFromRemote)
        throws AuthorizationException {
        return getStreamFromDataReference(dataReference, calledFromRemote, true);
    }

    @Override
    @AllowRemoteAccess
    public InputStream getStreamFromDataReference(DataReference dataReference, Boolean calledFromRemote, Boolean decompress) {
        DataBackend dataBackend = BackendSupport.getDataBackend();
        String gzipReferenceKey = null;
        for (BinaryReference br : dataReference.getBinaryReferences()) {
            if (br.getCompression().equals(CompressionFormat.GZIP)) {
                gzipReferenceKey = br.getBinaryReferenceKey();
            }
        }
        URI location =
            BackendSupport.getDataBackend().suggestLocation(
                UUID.fromString(gzipReferenceKey));
        DistributableInputStream stream = new DistributableInputStream(dataReference,
            (InputStream) dataBackend.get(location, decompress));
        // close local input stream before sending to another node. it is a transient field and will be "lost" without be closed before
        if (calledFromRemote) {
            try {
                stream.getLocalInputStream().close();
            } catch (IOException e) {
                LogFactory.getLog(RemotableFileDataServiceImpl.class)
                    .error("Failed to close local, transient input stream before sent to another node", e);
            }
        }
        return stream;
    }

    @Override
    @AllowRemoteAccess
    // FIXME review: why can this method never throw an IOException? - misc_ro
    public DataReference newReferenceFromStream(InputStream inputStream, MetaDataSet metaDataSet) {

        return newReferenceFromStream(inputStream, metaDataSet, false);
    }

    @Override
    @AllowRemoteAccess
    // FIXME review: why can this method never throw an IOException? - misc_ro
    public DataReference newReferenceFromStream(InputStream inputStream, MetaDataSet metaDataSet, Boolean alreadyCompressed) {

        UUID uuid = UUID.randomUUID();

        // store input stream
        DataBackend dataBackend = BackendSupport.getDataBackend();
        URI location = dataBackend.suggestLocation(uuid);
        dataBackend.put(location, inputStream, alreadyCompressed);

        // add a data reference with only one binary reference with the current only standard format and a default revision number.
        // TODO replace on new blob store implementation
        BinaryReference binaryReference = new BinaryReference(uuid.toString(), CompressionFormat.GZIP, "1");
        Set<BinaryReference> binaryReferences = new HashSet<BinaryReference>();
        binaryReferences.add(binaryReference);
        // create a new data reference
        DataReference dataReference = new DataReference(uuid.toString(),
            platformService.getLocalDefaultLogicalNodeId(), binaryReferences);

        // get the meta data backend and add the newly created data reference
        MetaDataBackendService metaDataBackend = BackendSupport.getMetaDataBackend();
        if (metaDataSet.getValue(new MetaData(MetaDataKeys.COMPONENT_RUN_ID, true, true)) != null) {
            metaDataBackend.addDataReferenceToComponentRun(
                Long.valueOf(metaDataSet.getValue(new MetaData(MetaDataKeys.COMPONENT_RUN_ID, true, true))),
                dataReference);
        } else if (metaDataSet.getValue(new MetaData(MetaDataKeys.WORKFLOW_RUN_ID, true, true)) != null) {
            metaDataBackend.addDataReferenceToWorkflowRun(
                Long.valueOf(metaDataSet.getValue(new MetaData(MetaDataKeys.WORKFLOW_RUN_ID, true, true))),
                dataReference);
        } else if (metaDataSet.getValue(new MetaData(MetaDataKeys.COMPONENT_INSTANCE_ID, true, true)) != null) {
            metaDataBackend.addDataReferenceToComponentInstance(
                Long.valueOf(metaDataSet.getValue(new MetaData(MetaDataKeys.COMPONENT_INSTANCE_ID, true, true))),
                dataReference);
        } else {
            LOGGER
                .warn("Data reference could not be added because not component run id, workflow run id or component instance id was given");
        }
        return dataReference;
    }

    @Override
    @AllowRemoteAccess
    public String initializeUpload() throws IOException {
        // note: not using a secure id here as RPC session authentication should not rely on such an id's secrecy anyway
        String id = IdGenerator.fastRandomHexString(UPLOAD_SESSION_ID_LENGTH);
        UploadHolder upload = new UploadHolder();
        uploads.put(id, upload);
        return id;
    }

    @Override
    @AllowRemoteAccess
    public long appendToUpload(String id, byte[] data) throws IOException {
        final UploadHolder upload = safeGetUploadById(id);

        // TODO keep track of "last updated" time; implement cleanup of abandoned uploads
        return upload.appendData(data);
    }

    @Override
    @AllowRemoteAccess
    public void finishUpload(String id, final MetaDataSet metaDataSet) throws IOException {
        finishUpload(id, metaDataSet, false);
    }

    @Override
    @AllowRemoteAccess
    public void finishUpload(String id, final MetaDataSet metaDataSet, final Boolean alreadyCompressed)
        throws IOException {
        final UploadHolder upload = safeGetUploadById(id);

        final File tempFile = upload.finishAndGetFile();
        if (tempFile == null) {
            throw new IOException("Internal error: upload stream was valid, but no file set");
        }

        // asynchronously transfer the upload file to data management
        ConcurrencyUtils.getAsyncTaskService().execute("Generate data reference from upload", () -> {

            // TODO depending on file store, this could be optimized by moving (reusing) the
            // upload file; alternatively, data stores could directly support incremental
            // uploading - misc_ro
            try {
                InputStream fis = new BufferedInputStream(new FileInputStream(tempFile), UPLOAD_TEMP_FILE_STREAM_BUFFER_SIZE);
                try {
                    DataReference reference;
                    reference = newReferenceFromStream(fis, metaDataSet, alreadyCompressed);
                    // on success, attach the reference to the upload data
                    upload.setDataReference(reference);
                } finally {
                    IOUtils.closeQuietly(fis);
                }
            } catch (IOException e) {
                // on any error, attach the exception to the upload data
                upload.setAsyncException(e);
            } catch (RuntimeException e2) {
                upload.setAsyncException(new IOException(e2));
            }

        });
    }

    @Override
    @AllowRemoteAccess
    public DataReference pollUploadForDataReference(String id) throws IOException {
        final UploadHolder upload = safeGetUploadById(id);
        DataReference reference = upload.getDataReference();
        if (reference != null) {
            // delete upload data once the reference has been fetched
            uploads.remove(id);
            return reference;
        }
        // if there was an asynchronous exception, re-throw it now
        if (upload.getAsyncException() != null) {
            uploads.remove(id);
            throw upload.getAsyncException();
        }
        return null;
    }

    private UploadHolder safeGetUploadById(String id) throws IOException {
        final UploadHolder upload = uploads.get(id);
        if (upload == null) {
            throw new IOException("Invalid upload id");
        }
        return upload;
    }

    @Override
    @AllowRemoteAccess
    public DataReference uploadInSingleStep(byte[] data, MetaDataSet metaDataSet) throws IOException, RemoteOperationException {
        return uploadInSingleStep(data, metaDataSet, false);
    }

    @Override
    @AllowRemoteAccess
    public DataReference uploadInSingleStep(byte[] data, MetaDataSet metaDataSet, Boolean alreadyCompressed) throws IOException,
        RemoteOperationException {
        UploadHolder upload = new UploadHolder();
        upload.appendData(data);

        final File tempFile = upload.finishAndGetFile();
        if (tempFile == null) {
            throw new IOException("Internal error: upload stream was valid, but no file set");
        }

        // synchronously transfer the upload file to data management
        InputStream fis = new BufferedInputStream(new FileInputStream(tempFile), UPLOAD_TEMP_FILE_STREAM_BUFFER_SIZE);
        DataReference reference;
        reference = newReferenceFromStream(fis, metaDataSet, alreadyCompressed);
        // on success, attach the reference to the upload data
        upload.setDataReference(reference);
        IOUtils.closeQuietly(fis);

        return upload.getDataReference();
    }

}
