/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.backend.data.efs.internal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.datamanagement.backend.DataBackend;

/**
 * IEFS implementation of {@link DataBackend}.
 * 
 * @author Sandra Schroedter
 * @author Juergen Klein
 * @author Sascha Zur
 * @author Robert Mischke (added stream buffering)
 */
public class EFSDataBackend implements DataBackend {

    /** Supported scheme. */
    public static final String SCHEMA_EFS = "efs";

    private static final String SCHEME_URI_COMPLETION = "://";

    private static final String SLASH = "/";

    private static final String ZIP_FILE_SUFFIX = ".gz";

    private static final String STORAGE_SUBDIRECTORY = "data";

    private static final String FAILED_TO_WRITE_FILE_FOR_URI = "Failed to write file for URI ";

    private static final int STREAM_BUFFER_SIZE = 256 * 1024; // arbitrary

    private static Pattern parentPattern;

    private static Pattern uriPattern;

    private static boolean useGZipCompression = false;

    private EFSDataBackendConfiguration configuration;

    private ConfigurationService configurationService;

    private EncapsulatedEFSService encapsulatedEFSService;

    private final Log log = LogFactory.getLog(getClass());

    static {

        /**
         * String defining the pattern for the parent directory.
         */
        final String patternStrParent = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
        /**
         * String defining the pattern for an URI.
         */
        final String patternStrURI = SLASH + patternStrParent;
        /**
         * Compiled pattern for the parent directory.
         */
        parentPattern = Pattern.compile(patternStrParent);
        /**
         * Compiled pattern for an URI.
         */
        uriPattern = Pattern.compile(patternStrURI);

    }

    protected void activate(BundleContext context) {
        // note: disabled old configuration loading for 6.0.0 as the only parameter is not being used anymore anyway
        // configuration = configurationService.getConfiguration(context.getBundle().getSymbolicName(), EFSDataBackendConfiguration.class);
        // TODO use default configuration until reworked or removed
        configuration = new EFSDataBackendConfiguration();
        useGZipCompression = true; // TODO always on since 6.0.0; remove parameter?
        if (configuration.getEfsStorage().equals("")) {
            File efsSubDir = configurationService.initializeSubDirInConfigurablePath(ConfigurablePathId.PROFILE_DATA_MANAGEMENT,
                STORAGE_SUBDIRECTORY);
            String efsPath = efsSubDir.getAbsolutePath();
            configuration.setEfsStorage(efsPath);
            log.debug("Initializing EFS storage in " + efsPath);
        } else {
            // note: the "else" path was not doing anything before; at least log if this happens
            log.warn("Unexpected state: EFS storage path already defined");
        }
    }

    protected void bindConfigurationService(ConfigurationService newConfigurationService) {
        configurationService = newConfigurationService;
    }

    protected void bindEncapsulatedEFSService(EncapsulatedEFSService newEncapsulatedEFService) {
        encapsulatedEFSService = newEncapsulatedEFService;
    }

    @Override
    public boolean delete(URI uri) {

        boolean deleted = false;

        if (!isURIValid(uri)) {
            throw new IllegalArgumentException("Given URI representing a file to delete is not valid: " + uri);
        }

        try {
            File fileToDelete = new File(getFileStorageRoot().getAbsolutePath() + new File(uri.getRawPath()).getPath());
            if (!fileToDelete.exists()) {
                fileToDelete = new File(getFileStorageRoot().getAbsolutePath() + new File(uri.getRawPath()).getPath() + ZIP_FILE_SUFFIX);
            }
            if (!fileToDelete.exists()) {
                log.debug("Given URI representing a file to delete could not be resolved to an existing path in the file store: "
                    + fileToDelete.getAbsolutePath());
            } else {
                IFileStore fileStore = encapsulatedEFSService.getStore(fileToDelete.toURI());
                fileStore.delete(EFS.NONE, null);
                deleted = true;

                // remove file store parent if there are no more revisions stored
                IFileStore parent = fileStore.getParent();
                if (isParentValid(parent.getName())) {
                    if (parent.childNames(EFS.NONE, null).length == 0) {
                        parent.delete(EFS.NONE, null);
                    }
                }
            }
        } catch (CoreException e) {
            // CoreException not wrapped in RuntimeException as it contains a
            // non-serializable object: org.eclipse.core.runtime.MultiStatus and
            // as this method is accessible from remote
            throw new RuntimeException("File with given URI could not be deleted: " + uri
                + " (Message: " + e.getMessage() + ")", e.getCause());
        }

        return deleted;
    }

    @Override
    public InputStream get(URI uri) {
        return get(uri, true);
    }

    @Override
    public InputStream get(URI uri, boolean decompress) {
        IFileStore fileStore = null;

        if (!isURIValid(uri)) {
            throw new IllegalArgumentException("Given URI representing a file to get is not valid: " + uri);
        }

        try {
            boolean isZipped = false;
            File fileToGet = new File(getFileStorageRoot().getAbsolutePath() + new File(uri.getRawPath()).getPath());
            if (!fileToGet.exists()) {
                fileToGet = new File(getFileStorageRoot().getAbsolutePath() + new File(uri.getRawPath()).getPath() + ZIP_FILE_SUFFIX);
                isZipped = true;
            }
            fileStore = encapsulatedEFSService.getStore(fileToGet.toURI());

            InputStream storageInputStream;
            // get buffered storage file stream
            if (isZipped && decompress) {
                storageInputStream =
                    new BufferedInputStream(new GzipCompressorInputStream(fileStore.openInputStream(EFS.NONE, null)), STREAM_BUFFER_SIZE);
            } else {
                storageInputStream = new BufferedInputStream(fileStore.openInputStream(EFS.NONE, null), STREAM_BUFFER_SIZE);
            }
            return storageInputStream;
        } catch (CoreException | IOException e) {
            throw new RuntimeException("File with given URI could not be found: " + uri, e);
        } 
    }

    @Override
    public URI suggestLocation(UUID guid) {

        URI newUri;
        try {
            newUri = new URI(SCHEMA_EFS + SCHEME_URI_COMPLETION + SLASH + guid.toString());
        } catch (URISyntaxException e) {
            // should never get here
            throw new IllegalArgumentException("Creating URI failed.", e);
        }
        return newUri;
    }

    @Override
    public long put(URI uri, Object object) {
        return put(uri, object, false);
    }

    @Override
    // TODO messy exception handling; improve
    public long put(URI uri, Object object, boolean alreadyCompressed) {

        if (!isURIValid(uri)) {
            throw new IllegalArgumentException("Given URI representing the location to put a file to is not valid: " + uri);
        }

        long writtenBytes = 0;

        if (object instanceof InputStream) {
            InputStream inputStream = (InputStream) object;

            OutputStream storageOutputStream = null;
            IFileStore fileStore = null;
            try {
                File fileToSave;
                IFileStore parent = null;
                if (useGZipCompression && !alreadyCompressed) {
                    fileToSave = new File(getFileStorageRoot().getAbsolutePath() + new File(uri.getRawPath()).getPath() + ZIP_FILE_SUFFIX);
                } else {
                    fileToSave = new File(getFileStorageRoot().getAbsolutePath() + new File(uri.getRawPath()).getPath());

                }
                fileStore = encapsulatedEFSService.getStore(fileToSave.toURI());
                parent = fileStore.getParent();
                if (parent != null && isParentValid(parent.getName())) {
                    parent.mkdir(0, null);
                }


                // get buffered storage file stream for writing
                if (useGZipCompression && !alreadyCompressed) {
                    storageOutputStream =
                        new BufferedOutputStream(new GzipCompressorOutputStream(fileStore.openOutputStream(EFS.NONE, null)),
                            STREAM_BUFFER_SIZE);
                } else {
                    storageOutputStream = new BufferedOutputStream(fileStore.openOutputStream(EFS.NONE, null), STREAM_BUFFER_SIZE);
                }
            } catch (CoreException e) {
                // TODO review: RTEs should only be thrown when unavoidable; change method API to
                // declare explicit exceptions - misc_ro
                throw new RuntimeException(FAILED_TO_WRITE_FILE_FOR_URI + uri, e);
            } catch (IOException e) {
                // TODO review: RTEs should only be thrown when unavoidable; change method API to
                // declare explicit exceptions - misc_ro
                throw new RuntimeException(FAILED_TO_WRITE_FILE_FOR_URI + uri, e);
            }

            try {
                final int minusOne = -1;
                final int bufferSize = 256 * 1024;
                byte[] buffer = new byte[bufferSize];
                int n = 0;
                while (minusOne != (n = inputStream.read(buffer))) {
                    storageOutputStream.write(buffer, 0, n);
                    writtenBytes += n;
                }

            } catch (IOException e) {
                try {
                    fileStore.delete(EFS.NONE, null);
                    IFileStore parent = fileStore.getParent();
                    if (isParentValid(parent.getName())) {
                        // delete directory if it is empty
                        if (parent.childNames(EFS.NONE, null).length == 0) {
                            parent.delete(EFS.NONE, null);
                        }
                    }
                } catch (CoreException e2) {
                    log.error("File with given URI for which writing failed could not be deleted: " + uri);
                }
                throw new RuntimeException(FAILED_TO_WRITE_FILE_FOR_URI + uri, e);
            } finally {
                try {
                    storageOutputStream.close();
                } catch (IOException e2) {
                    log.error("EFS output stream for given URI could not be closed: " + uri, e2);
                }
                try {
                    inputStream.close();
                } catch (IOException e2) {
                    log.error("Input stream for given URI could not be closed: " + uri, e2);
                }
            }
        } else {
            throw new IllegalArgumentException("Given object to put is not an instance of InputStream: " + object);
        }
        return writtenBytes;
    }

    /**
     * Checks if the given name resembles a valid URI for the parent directory of a persisted file.
     * 
     * @param name Name to be checked.
     * @return <code>true</code> if name resembles a valid parent directory, <code>false</code> otherwise.
     */
    private boolean isParentValid(String name) {
        // must be like f8f3fe28-7970-4f5d-a7ea-8f611f6aa83c
        String stringToTest = name;
        Matcher matcher = parentPattern.matcher(stringToTest);
        boolean isValue = matcher.matches();
        return isValue;
    }

    /**
     * Checks if the given URI resembles a valid URI for the persisted file.
     * 
     * @param uri URI to be checked.
     * @return <code>true</code> if URI is valid, <code>false</code> otherwise.
     */
    private boolean isURIValid(URI uri) {
        // must be like /f8f3fe28-7970-4f5d-a7ea-8f611f6aa83c/file-1
        String stringToTest = uri.getPath();
        Matcher matcher = uriPattern.matcher(stringToTest);
        boolean isValue = matcher.matches();
        return isValue;
    }

    private File getFileStorageRoot() {
        return new File(configuration.getEfsStorage()).getAbsoluteFile();
    }
}
