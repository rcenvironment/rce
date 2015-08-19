/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import de.rcenvironment.core.authentication.User;
import de.rcenvironment.core.authorization.AuthorizationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.datamanagement.DataManagementService;
import de.rcenvironment.core.datamanagement.DistributedDataReferenceService;
import de.rcenvironment.core.datamanagement.DistributedFileDataService;
import de.rcenvironment.core.datamanagement.commons.DataReference;
import de.rcenvironment.core.datamanagement.commons.MetaDataSet;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Default implementation of {@link DataManagementService}.
 * 
 * @author Robert Mischke
 */
public class DataManagementServiceImpl implements DataManagementService {

    private static final String ARCHIVE_TAR_GZ = "archive.tar.gz";

    private static final int BUFFER = 1024;

    private static final String REFERENCE_NOT_FOUND_MESSAGE = "No such data entry (id='%s').";

    private DistributedFileDataService fileDataService;

    private DistributedDataReferenceService dataReferenceService;

    @Override
    public String createReferenceFromLocalFile(User user, File file, MetaDataSet additionalMetaData,
        NodeIdentifier nodeId) throws IOException, AuthorizationException, InterruptedException {
        return createReferenceFromStream(user, new FileInputStream(file), additionalMetaData, nodeId);
    }

    private String createReferenceFromStream(User user, InputStream inputStream, MetaDataSet additionalMetaData,
        NodeIdentifier nodeId) throws IOException, AuthorizationException, InterruptedException {
        if (additionalMetaData == null) {
            additionalMetaData = new MetaDataSet();
        }

        try {
            DataReference dataRef = fileDataService.newReferenceFromStream(user, inputStream, additionalMetaData, nodeId);
            return dataRef.getDataReferenceKey().toString();
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    @Override
    public String createReferenceFromLocalDirectory(User user, File dir, MetaDataSet additionalMetaData, NodeIdentifier nodeId)
        throws IOException, AuthorizationException, InterruptedException {

        File archive = TempFileServiceAccess.getInstance().createTempFileWithFixedFilename(ARCHIVE_TAR_GZ);
        createTarGz(dir, archive);

        String reference = createReferenceFromStream(user, new FileInputStream(archive), additionalMetaData, nodeId);
        TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(archive);
        return reference;
    }

    private void createTarGz(File dir, File archive) throws IOException {

        FileOutputStream fileOutStream = new FileOutputStream(archive);
        BufferedOutputStream bufferedOutStream = new BufferedOutputStream(fileOutStream);
        GzipCompressorOutputStream gzipOutStream;
        try {
            gzipOutStream = new GzipCompressorOutputStream(bufferedOutStream);
        } catch (IOException e) {
            bufferedOutStream.close();
            fileOutStream.close();
            throw e;
        }
        TarArchiveOutputStream tarOutStream = new TarArchiveOutputStream(gzipOutStream);
        try {
            addFileToTarGz(tarOutStream, dir.getAbsolutePath(), "");
        } finally {
            tarOutStream.finish();
            tarOutStream.close();
            gzipOutStream.close();
            bufferedOutStream.close();
            fileOutStream.close();
        }
    }

    private void addFileToTarGz(TarArchiveOutputStream tOutStream, String path, String base) throws IOException {
        File file = new File(path);
        String entryName = base + file.getName();
        TarArchiveEntry tarEntry = new TarArchiveEntry(file, entryName);
        tOutStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
        tOutStream.putArchiveEntry(tarEntry);

        if (file.isFile()) {
            InputStream inputStream = new FileInputStream(file);
            IOUtils.copy(inputStream, tOutStream);
            inputStream.close();
            tOutStream.closeArchiveEntry();
        } else {
            tOutStream.closeArchiveEntry();
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    addFileToTarGz(tOutStream, child.getAbsolutePath(), entryName + "/");
                }
            }
        }
    }

    @Override
    public String createReferenceFromString(User user, String object, MetaDataSet additionalMetaData, // CheckStyle
        NodeIdentifier nodeId) throws IOException, AuthorizationException, InterruptedException {
        if (additionalMetaData == null) {
            additionalMetaData = new MetaDataSet();
        }

        InputStream inputStream = IOUtils.toInputStream(object);
        try {
            DataReference dataRef = fileDataService. // break line here for Checkstyle
                newReferenceFromStream(user, inputStream, additionalMetaData, nodeId);
            return dataRef.getDataReferenceKey().toString();
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    @Override
    public void copyReferenceToLocalFile(User user, String reference, File targetFile, // CheckStyle
        NodeIdentifier nodeId) throws IOException, AuthorizationException {

        DataReference dataRef;
        if (nodeId == null) {
            dataRef = dataReferenceService.getReference(reference);
        } else {
            dataRef = dataReferenceService.getReference(reference, nodeId);
        }
        if (dataRef == null) {
            throw new FileNotFoundException(StringUtils.format(REFERENCE_NOT_FOUND_MESSAGE, reference));
        }
        InputStream dataMgmtStream = fileDataService.getStreamFromDataReference(user, dataRef);
        try {
            FileUtils.copyInputStreamToFile(dataMgmtStream, targetFile);
        } finally {
            IOUtils.closeQuietly(dataMgmtStream);
        }
    }

    @Override
    public void copyReferenceToLocalFile(User user, String reference, File targetFile, // CheckStyle
        Collection<NodeIdentifier> platforms) throws IOException, AuthorizationException {

        DataReference dataRef;
        if ((platforms == null) || (platforms.size() == 0)) {
            dataRef = dataReferenceService.getReference(reference);
        } else {
            dataRef = dataReferenceService.getReference(reference, platforms);
        }
        if (dataRef == null) {
            throw new FileNotFoundException(StringUtils.format(REFERENCE_NOT_FOUND_MESSAGE, reference));
        }
        InputStream dataMgmtStream = fileDataService.getStreamFromDataReference(user, dataRef);
        try {
            FileUtils.copyInputStreamToFile(dataMgmtStream, targetFile);
        } finally {
            IOUtils.closeQuietly(dataMgmtStream);
        }
    }

    @Override
    public void copyReferenceToLocalDirectory(User user, String reference, File targetDir, NodeIdentifier node) throws IOException {

        File archive = TempFileServiceAccess.getInstance().createTempFileWithFixedFilename(ARCHIVE_TAR_GZ);
        copyReferenceToLocalFile(user, reference, archive, node);
        createDirectoryFromTarGz(archive, targetDir);
        TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(archive);
    }

    private void createDirectoryFromTarGz(File archive, File targetDir) throws FileNotFoundException, IOException {

        targetDir.mkdirs();

        FileInputStream fileInStream = new FileInputStream(archive);
        BufferedInputStream bufferedInStream = new BufferedInputStream(fileInStream);
        GzipCompressorInputStream gzipInStream;
        try {
            gzipInStream = new GzipCompressorInputStream(bufferedInStream);
        } catch (IOException e) {
            bufferedInStream.close();
            fileInStream.close();
            throw e;
        }
        TarArchiveInputStream tarInStream = new TarArchiveInputStream(gzipInStream);
        try {
            createFileOrDirForTarEntry(tarInStream, targetDir);
        } finally {
            tarInStream.close();
            gzipInStream.close();
            bufferedInStream.close();
            fileInStream.close();
        }
    }

    private void createFileOrDirForTarEntry(TarArchiveInputStream tarInStream, File targetDir) throws IOException {

        TarArchiveEntry tarEntry = tarInStream.getNextTarEntry();
        if (tarEntry == null) {
            return;
        }
        File destPath = new File(targetDir, tarEntry.getName());
        if (tarEntry.isDirectory()) {
            destPath.mkdirs();
        } else {
            destPath.createNewFile();
            byte[] btoRead = new byte[BUFFER];
            BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(destPath));
            int len = 0;
            final int minusOne = -1;
            while ((len = tarInStream.read(btoRead)) != minusOne) {
                bout.write(btoRead, 0, len);
            }
            bout.close();
        }
        createFileOrDirForTarEntry(tarInStream, targetDir);
    }

    @Override
    public String retrieveStringFromReference(User user, String reference, NodeIdentifier nodeId) throws IOException,
        AuthorizationException {
        // TODO extract reference to stream resolution into separate method?
        DataReference dataRef;
        if (nodeId == null) {
            dataRef = dataReferenceService.getReference(reference);
        } else {
            dataRef = dataReferenceService.getReference(reference, nodeId);
        }
        if (dataRef == null) {
            throw new FileNotFoundException(StringUtils.format(REFERENCE_NOT_FOUND_MESSAGE, reference));
        }
        InputStream dataMgmtStream = fileDataService.getStreamFromDataReference(user, dataRef);
        try {
            return IOUtils.toString(dataMgmtStream);
        } finally {
            IOUtils.closeQuietly(dataMgmtStream);
        }
    }

    /**
     * OSGi-DS setter.
     * 
     * @param fileDataService The fileDataService to set.
     */
    protected void bindFileDataService(DistributedFileDataService newValue) {
        this.fileDataService = newValue;
    }

    /**
     * OSGi-DS setter.
     * 
     * @param dataReferenceService The dataReferenceService to set.
     */
    protected void bindDataReferenceService(DistributedDataReferenceService newValue) {
        this.dataReferenceService = newValue;
    }

}
