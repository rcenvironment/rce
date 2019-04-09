/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.authorization.AuthorizationException;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.datamanagement.DataManagementService;
import de.rcenvironment.core.datamanagement.DataReferenceService;
import de.rcenvironment.core.datamanagement.FileDataService;
import de.rcenvironment.core.datamanagement.commons.DataReference;
import de.rcenvironment.core.datamanagement.commons.MetaDataSet;
import de.rcenvironment.core.utils.common.CrossPlatformFilenameUtils;
import de.rcenvironment.core.utils.common.FileCompressionFormat;
import de.rcenvironment.core.utils.common.FileCompressionService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Default implementation of {@link DataManagementService}.
 * 
 * @author Robert Mischke
 * @author Doreen Seider
 * @author Thorsten Sommer (integration of {@link FileCompressionService})
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
        NetworkDestination nodeId) throws IOException, AuthorizationException, InterruptedException, CommunicationException {
        if (!CrossPlatformFilenameUtils.isFilenameValid(file.getName())) {
            LOGGER.warn(StringUtils.format(STRING_FILENAME_NOT_VALID, file.getName()));
        }
        return createReferenceFromStream(new FileInputStream(file), additionalMetaData, nodeId, false);
    }

    @Override
    public String createReferenceFromLocalFile(File file, MetaDataSet additionalMetaData,
        NetworkDestination nodeId, boolean alreadyCompressed) throws IOException, AuthorizationException, InterruptedException,
        CommunicationException {
        return createReferenceFromStream(new FileInputStream(file), additionalMetaData, nodeId, alreadyCompressed);
    }

    private String createReferenceFromStream(InputStream inputStream, MetaDataSet additionalMetaData,
        NetworkDestination nodeId, boolean alreadyCompressed) throws IOException, AuthorizationException, InterruptedException,
        CommunicationException {
        if (additionalMetaData == null) {
            additionalMetaData = new MetaDataSet();
        }

        try {
            DataReference dataRef = fileDataService.newReferenceFromStream(inputStream, additionalMetaData, nodeId, alreadyCompressed);
            return dataRef.getDataReferenceKey().toString();
        } finally {
            // FIXME: replace with try-with-resources statement after release 7.0.0
            IOUtils.closeQuietly(inputStream);
        }
    }

    @Override
    public String createReferenceFromLocalDirectory(final File dir, final MetaDataSet additionalMetaData, final NetworkDestination nodeId)
        throws IOException, AuthorizationException, InterruptedException, CommunicationException {

        final File archive = TempFileServiceAccess.getInstance().createTempFileWithFixedFilename(ARCHIVE_TAR_GZ);

        try {
            if (!FileCompressionService.compressDirectoryToFile(dir, archive,
                FileCompressionFormat.TAR_GZ, true)) {

                // Case: Was not able to compress & archive the directory:
                LOGGER.error("Was not able to create a reference from a local directory due to an issue with the compression.");
                throw new IOException("Was not able to create a reference from a local directory due to an issue with the compression.");
            }

            try (FileInputStream fileInputStream = new FileInputStream(archive)) {
                return createReferenceFromStream(fileInputStream, additionalMetaData, nodeId, false);
            }
        } finally {
            TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(archive);
        }
    }

    @Override
    public String createReferenceFromString(String object, MetaDataSet additionalMetaData, // CheckStyle
        NetworkDestination nodeId) throws IOException, AuthorizationException, InterruptedException, CommunicationException {
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
        NetworkDestination nodeId) throws IOException, CommunicationException {
        copyReferenceToLocalFile(reference, targetFile, nodeId, true);
    }

    @Override
    public void copyReferenceToLocalFile(String reference, File targetFile, NetworkDestination nodeId, boolean decompress)
        throws IOException,
        CommunicationException {
        DataReference dataRef;
        if (nodeId == null) {
            // TODO can this still be reached? null nodeIds should not be in use anymore -- misc_ro
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
    @Deprecated // undefined storage locations are not used anymore; remove from code base
    public void copyReferenceToLocalFile(String reference, File targetFile, // CheckStyle
        Collection<? extends NetworkDestination> platforms) throws IOException, AuthorizationException, CommunicationException {

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
    public void copyReferenceToLocalDirectory(final String reference, final File targetDir, final NetworkDestination node)
        throws IOException,
        CommunicationException {

        final File archive = TempFileServiceAccess.getInstance().createTempFileWithFixedFilename(ARCHIVE_TAR_GZ);

        try {
            copyReferenceToLocalFile(reference, archive, node);
            if (!FileCompressionService.expandCompressedDirectoryFromFile(archive, targetDir,
                FileCompressionFormat.TAR_GZ)) {

                // Case: Expanding of archive was not possible.
                LOGGER.error("Was not able to copy reference to local directory due to an uncompression issue.");
                throw new CommunicationException("Was not able to copy reference to local directory due to an uncompression issue.");
            }
        } finally {
            TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(archive);
        }
    }

    @Override
    public String retrieveStringFromReference(String reference, NetworkDestination nodeId) throws IOException,
        AuthorizationException, CommunicationException {
        // TODO extract reference to stream resolution into separate method?
        DataReference dataRef;
        if (nodeId == null) {
            // TODO can this still be reached? null nodeIds should not be in use anymore -- misc_ro
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
