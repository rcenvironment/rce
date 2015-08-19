/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.authentication.User;
import de.rcenvironment.core.authorization.AuthorizationException;
import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.datamanagement.FileDataService;
import de.rcenvironment.core.datamanagement.backend.DataBackend;
import de.rcenvironment.core.datamanagement.backend.MetaDataBackendService;
import de.rcenvironment.core.datamanagement.commons.BinaryReference;
import de.rcenvironment.core.datamanagement.commons.DataReference;
import de.rcenvironment.core.datamanagement.commons.DistributableInputStream;
import de.rcenvironment.core.datamanagement.commons.MetaData;
import de.rcenvironment.core.datamanagement.commons.MetaDataKeys;
import de.rcenvironment.core.datamanagement.commons.MetaDataSet;
import de.rcenvironment.core.datamodel.api.CompressionFormat;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;
import de.rcenvironment.core.utils.incubator.Assertions;
import de.rcenvironment.core.utils.incubator.IdGenerator;

/**
 * Implementation of the {@link FileDataService}.
 * 
 * @author Juergen Klein
 */
public class FileDataServiceImpl extends DataServiceImpl implements FileDataService {

    private static final int UPLOAD_TEMP_FILE_STREAM_BUFFER_SIZE = 64 * 1024; // arbitrary

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

        public UploadHolder() throws IOException {
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

    public FileDataServiceImpl() {
        uploads = Collections.synchronizedMap(new HashMap<String, UploadHolder>());
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

    // override without change because the @AllowRemoteAccess check does not detect superclass annotations
    @Override
    @AllowRemoteAccess
    public void deleteReference(String binaryReferenceKey) throws AuthorizationException {
        super.deleteReference(binaryReferenceKey);
    }

    @Override
    @AllowRemoteAccess
    public InputStream getStreamFromDataReference(User user, DataReference dataReference, Boolean calledFromRemote)
        throws AuthorizationException {
        Assertions.isTrue(user.isValid(), PASSED_USER_IS_NOT_VALID);

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
        DistributableInputStream stream = new DistributableInputStream(user, dataReference,
            (InputStream) dataBackend.get(location));
        // close local input stream before sending to another node. it is a transient field and will be "lost" without be closed before
        if (calledFromRemote) {
            try {
                stream.getLocalInputStream().close();
            } catch (IOException e) {
                LogFactory.getLog(FileDataServiceImpl.class)
                    .error("Failed to close local, transient input stream before sent to another node", e);
            }
        }
        return stream;
    }

    @Override
    @AllowRemoteAccess
    // FIXME review: why can this method never throw an IOException? - misc_ro
    public DataReference newReferenceFromStream(User user, InputStream inputStream, MetaDataSet metaDataSet)
        throws AuthorizationException {
        Assertions.isTrue(user.isValid(), PASSED_USER_IS_NOT_VALID);

        UUID uuid = UUID.randomUUID();

        // store input stream
        DataBackend dataBackend = BackendSupport.getDataBackend();
        URI location = dataBackend.suggestLocation(uuid);
        dataBackend.put(location, inputStream);

        // add a data reference with only one binary reference with the current only standard format and a default revision number.
        // TODO replace on new blob store implementation
        BinaryReference binaryReference = new BinaryReference(uuid.toString(), CompressionFormat.GZIP, "1");
        Set<BinaryReference> binaryReferences = new HashSet<BinaryReference>();
        binaryReferences.add(binaryReference);
        // create a new data reference
        DataReference dataReference = new DataReference(uuid.toString(),
            platformService.getLocalNodeId(), binaryReferences);

        // get the meta data backend and add the newly created data reference
        MetaDataBackendService metaDataBackend = BackendSupport.getMetaDataBackend();
        metaDataBackend.addDataReferenceToComponentRun(
            Long.valueOf(metaDataSet.getValue(new MetaData(MetaDataKeys.COMPONENT_RUN_ID, true, true))),
            dataReference);
        return dataReference;
    }

    @Override
    @AllowRemoteAccess
    public String initializeUpload(User user) throws IOException {
        String id = IdGenerator.randomUUID();
        UploadHolder upload = new UploadHolder();
        uploads.put(id, upload);
        return id;
    }

    @Override
    @AllowRemoteAccess
    public long appendToUpload(User user, String id, byte[] data) throws IOException {
        final UploadHolder upload = safeGetUploadById(id);

        // TODO keep track of "last updated" time; implement cleanup of abandoned uploads
        return upload.appendData(data);
    }

    @Override
    @AllowRemoteAccess
    public void finishUpload(final User user, String id, final MetaDataSet metaDataSet) throws IOException {
        final UploadHolder upload = safeGetUploadById(id);

        final File tempFile = upload.finishAndGetFile();
        if (tempFile == null) {
            throw new IOException("Internal error: upload stream was valid, but no file set");
        }

        // asynchronously transfer the upload file to data management
        SharedThreadPool.getInstance().execute(new Runnable() {

            @Override
            @TaskDescription("Generate data reference from upload")
            public void run() {
                // TODO depending on file store, this could be optimized by moving (reusing) the
                // upload file; alternatively, data stores could directly support incremental
                // uploading - misc_ro
                try {
                    InputStream fis = new BufferedInputStream(new FileInputStream(tempFile), UPLOAD_TEMP_FILE_STREAM_BUFFER_SIZE);
                    try {
                        DataReference reference;
                        try {
                            reference = newReferenceFromStream(user, fis, metaDataSet);
                            // on success, attach the reference to the upload data
                            upload.setDataReference(reference);
                        } catch (RuntimeException e) {
                            // wrap any RTEs
                            throw new IOException(e);
                        }
                    } finally {
                        IOUtils.closeQuietly(fis);
                    }
                } catch (IOException e) {
                    // on any error, attach the exception to the upload data
                    upload.setAsyncException(e);
                }
            }
        });
    }

    @Override
    @AllowRemoteAccess
    public DataReference pollUploadForDataReference(User user, String id) throws IOException {
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

}
