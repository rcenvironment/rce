/*
 * Copyright (C) 2006-2016 DLR, Germany
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.authorization.AuthorizationException;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.datamanagement.DataManagementService;
import de.rcenvironment.core.datamanagement.DataReferenceService;
import de.rcenvironment.core.datamanagement.FileDataService;
import de.rcenvironment.core.datamanagement.commons.DataReference;
import de.rcenvironment.core.datamanagement.commons.MetaDataSet;
import de.rcenvironment.core.utils.common.CrossPlatformFilenameUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Default implementation of {@link DataManagementService}.
 * 
 * @author Robert Mischke
 * @author Doreen Seider
 */
public class DataManagementServiceImpl implements DataManagementService {

    private static final String STRING_FILENAME_NOT_VALID =
        "Filename/path '%S' contains characters that are not valid for all operating systems; "
            + "it might lead to problems when accessing the file on another operating system";

    private static final String ARCHIVE_TAR_GZ = "archive.tar.gz";

    private static final int BUFFER = 1024;

    private static final String REFERENCE_NOT_FOUND_MESSAGE = "No such data entry (id='%s').";

    private static final String TAR_GZ_PATH_SEPARATOR = "/";

    private static final Log LOGGER = LogFactory.getLog(DataManagementServiceImpl.class);

    private FileDataService fileDataService;

    private DataReferenceService dataReferenceService;

    @Override
    public String createReferenceFromLocalFile(File file, MetaDataSet additionalMetaData,
        ResolvableNodeId nodeId) throws IOException, AuthorizationException, InterruptedException, CommunicationException {
        if (!CrossPlatformFilenameUtils.isFilenameValid(file.getName())) {
            LOGGER.warn(StringUtils.format(STRING_FILENAME_NOT_VALID, file.getName()));
        }
        return createReferenceFromStream(new FileInputStream(file), additionalMetaData, nodeId);
    }

    private String createReferenceFromStream(InputStream inputStream, MetaDataSet additionalMetaData,
        ResolvableNodeId nodeId) throws IOException, AuthorizationException, InterruptedException, CommunicationException {
        if (additionalMetaData == null) {
            additionalMetaData = new MetaDataSet();
        }

        try {
            DataReference dataRef = fileDataService.newReferenceFromStream(inputStream, additionalMetaData, nodeId);
            return dataRef.getDataReferenceKey().toString();
        } finally {
            // FIXME: replace with try-with-resources statement after release 7.0.0
            IOUtils.closeQuietly(inputStream);
        }
    }

    @Override
    public String createReferenceFromLocalDirectory(File dir, MetaDataSet additionalMetaData, ResolvableNodeId nodeId)
        throws IOException, AuthorizationException, InterruptedException, CommunicationException {

        File archive = TempFileServiceAccess.getInstance().createTempFileWithFixedFilename(ARCHIVE_TAR_GZ);
        createTarGz(dir, archive);

        try (FileInputStream fileInputStream = new FileInputStream(archive)) {
            String reference = createReferenceFromStream(fileInputStream, additionalMetaData, nodeId);
            TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(archive);
            return reference;
        }
    }

    // set visibility from private to protected for test purposes
    protected void createTarGz(File dir, File archive) throws IOException {

        try (FileOutputStream fileOutStream = new FileOutputStream(archive);
            BufferedOutputStream bufferedOutStream = new BufferedOutputStream(fileOutStream);
            GzipCompressorOutputStream gzipOutStream = new GzipCompressorOutputStream(bufferedOutStream);
            TarArchiveOutputStream tarOutStream = new TarArchiveOutputStream(gzipOutStream)) {
            addFileToTarGz(tarOutStream, dir.getAbsolutePath(), "");
        }
    }

    private void addFileToTarGz(TarArchiveOutputStream tOutStream, String path, String base) throws IOException {
        File file = new File(path);
        if (!CrossPlatformFilenameUtils.isPathValid(file.getAbsolutePath())) {
            LOGGER.warn(StringUtils.format(STRING_FILENAME_NOT_VALID, file.getAbsolutePath()));
        }
        String entryName = base + file.getName();
        TarArchiveEntry tarEntry = new TarArchiveEntry(file, entryName);
        tOutStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
        tOutStream.putArchiveEntry(tarEntry);

        if (file.isFile()) {
            try (InputStream inputStream = new FileInputStream(file)) {
                IOUtils.copy(inputStream, tOutStream);
            }
            tOutStream.closeArchiveEntry();
        } else {
            tOutStream.closeArchiveEntry();
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!CrossPlatformFilenameUtils.isFilenameValid(child.getName())) {
                        LOGGER.warn(StringUtils.format(STRING_FILENAME_NOT_VALID, child.getName()));
                    }
                    addFileToTarGz(tOutStream, child.getAbsolutePath(), entryName + TAR_GZ_PATH_SEPARATOR);
                }
            }
        }
    }

    @Override
    public String createReferenceFromString(String object, MetaDataSet additionalMetaData, // CheckStyle
        ResolvableNodeId nodeId) throws IOException, AuthorizationException, InterruptedException, CommunicationException {
        if (additionalMetaData == null) {
            additionalMetaData = new MetaDataSet();
        }

        InputStream inputStream = IOUtils.toInputStream(object);
        try {
            DataReference dataRef = fileDataService.newReferenceFromStream(inputStream, additionalMetaData, nodeId);
            return dataRef.getDataReferenceKey().toString();
        } finally {
            // FIXME: replace with try-with-resources statement after release 7.0.0
            IOUtils.closeQuietly(inputStream);
        }
    }

    @Override
    public void copyReferenceToLocalFile(String reference, File targetFile, // CheckStyle
        ResolvableNodeId nodeId) throws IOException, CommunicationException {

        DataReference dataRef;
        if (nodeId == null) {
            dataRef = dataReferenceService.getReference(reference);
        } else {
            dataRef = dataReferenceService.getReference(reference, nodeId);
        }
        if (dataRef == null) {
            throw new FileNotFoundException(StringUtils.format(REFERENCE_NOT_FOUND_MESSAGE, reference));
        }
        InputStream dataMgmtStream = fileDataService.getStreamFromDataReference(dataRef);
        try {
            FileUtils.copyInputStreamToFile(dataMgmtStream, targetFile);
        } finally {
            // FIXME: replace with try-with-resources statement after release 7.0.0
            IOUtils.closeQuietly(dataMgmtStream);
        }
    }

    @Override
    public void copyReferenceToLocalFile(String reference, File targetFile, // CheckStyle
        Collection<? extends ResolvableNodeId> platforms) throws IOException, AuthorizationException, CommunicationException {

        DataReference dataRef;
        if ((platforms == null) || (platforms.size() == 0)) {
            dataRef = dataReferenceService.getReference(reference);
        } else {
            dataRef = dataReferenceService.getReference(reference, platforms);
        }
        if (dataRef == null) {
            throw new FileNotFoundException(StringUtils.format(REFERENCE_NOT_FOUND_MESSAGE, reference));
        }
        InputStream dataMgmtStream = fileDataService.getStreamFromDataReference(dataRef);
        try {
            FileUtils.copyInputStreamToFile(dataMgmtStream, targetFile);
        } finally {
            // FIXME: replace with try-with-resources statement after release 7.0.0
            IOUtils.closeQuietly(dataMgmtStream);
        }
    }

    @Override
    public void copyReferenceToLocalDirectory(String reference, File targetDir, ResolvableNodeId node) throws IOException,
        CommunicationException {
        File archive = TempFileServiceAccess.getInstance().createTempFileWithFixedFilename(ARCHIVE_TAR_GZ);
        copyReferenceToLocalFile(reference, archive, node);
        createDirectoryFromTarGz(archive, targetDir);
        TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(archive);
    }

    // set visibility to from private to protected for test purposes
    protected void createDirectoryFromTarGz(File archive, File targetDir) throws FileNotFoundException, IOException {

        targetDir.mkdirs();

        try (FileInputStream fileInStream = new FileInputStream(archive);
            BufferedInputStream bufferedInStream = new BufferedInputStream(fileInStream);
            GzipCompressorInputStream gzipInStream = new GzipCompressorInputStream(bufferedInStream);
            TarArchiveInputStream tarInStream = new TarArchiveInputStream(gzipInStream)) {
            createFileOrDirForTarEntry(tarInStream, targetDir);
        }
    }

    private void createFileOrDirForTarEntry(TarArchiveInputStream tarInStream, File targetDir) throws IOException {

        TarArchiveEntry tarEntry;
        while ((tarEntry = tarInStream.getNextTarEntry()) != null) {
            File destPath = new File(targetDir, tarEntry.getName());

            if (tarEntry.isDirectory()) {
                destPath.mkdirs();
            } else {
                destPath.createNewFile();
                byte[] btoRead = new byte[BUFFER];
                try (BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(destPath))) {
                    int len = 0;
                    final int minusOne = -1;
                    while ((len = tarInStream.read(btoRead)) != minusOne) {
                        bout.write(btoRead, 0, len);
                    }
                }
            }
        }
    }

    @Override
    public String retrieveStringFromReference(String reference, ResolvableNodeId nodeId) throws IOException,
        AuthorizationException, CommunicationException {
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
        InputStream dataMgmtStream = fileDataService.getStreamFromDataReference(dataRef);
        try {
            return IOUtils.toString(dataMgmtStream);
        } finally {
            // FIXME: replace with try-with-resources statement after release 7.0.0
            IOUtils.closeQuietly(dataMgmtStream);
        }
    }

    /**
     * OSGi-DS setter.
     * 
     * @param fileDataService The fileDataService to set.
     */
    protected void bindFileDataService(FileDataService newValue) {
        this.fileDataService = newValue;
    }

    /**
     * OSGi-DS setter.
     * 
     * @param dataReferenceService The dataReferenceService to set.
     */
    protected void bindDataReferenceService(DataReferenceService newValue) {
        this.dataReferenceService = newValue;
    }

}
