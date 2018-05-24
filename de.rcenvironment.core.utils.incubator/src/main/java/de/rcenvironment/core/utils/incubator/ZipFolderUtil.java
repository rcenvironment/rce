/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class to handle ZIP file archives.
 * 
 * @author Markus Kunde, Markus Litz
 * @author Robert Mischke (improved error handling)
 */
public final class ZipFolderUtil {

    /** Our logger instance. */
    protected static final Log LOG = LogFactory.getLog(ZipFolderUtil.class);

    /** archiver name. */
    private static final String ARCHIVERNAME = ArchiveStreamFactory.ZIP;

    /**
     * Default constructor.
     */
    private ZipFolderUtil() {
        // empty constructor
    }

    /**
     * Compress content of a folder (without root-folder).
     * 
     * @param folder parent to compress
     * @param targetZipFile target zip-file
     * @throws IOException on failure
     */
    public static void zipFolderContent(final File folder, final File targetZipFile) throws IOException {
        ArchiveOutputStream out = null;
        InputStream in = null;
        try {
            out = new ArchiveStreamFactory().createArchiveOutputStream(ARCHIVERNAME, new FileOutputStream(targetZipFile));

            for (File file : FileUtils.listFiles(folder, null, true)) {
                String zipPath = StringUtils.difference(folder.getCanonicalPath(), file.getCanonicalPath());
                zipPath = zipPath.substring(1);
                in = new FileInputStream(file);
                out.putArchiveEntry(new ZipArchiveEntry(zipPath));
                IOUtils.copy(in, out);
                out.closeArchiveEntry();
                in.close();
                in = null;
            }

            out.flush();
            out.close();
            out = null;
        } catch (FileNotFoundException e) {
            throw new IOException("File not found during zipping process", e);
        } catch (ArchiveException e) {
            throw new IOException("Zip file could not be compressed", e); // when does this actually happen?
        } catch (IOException e) {
            throw new IOException("Error while creating zip file", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // should only happen as part of an already thrown exception, so only log it
                    LOG.warn("Error while closing leftover input stream of zipping mechanism", e);
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // as above
                    LOG.warn("Error while closing output stream of zipping mechanism", e);
                }
            }
        }
    }

    /**
     * Extract Zip file to a destination folder.
     * 
     * @param folder destination where zip file should be extracted
     * @param sourceZipFile zip file to extract
     * @throws IOException on failure
     */
    public static void extractZipToFolder(final File folder, final File sourceZipFile) throws IOException {
        ArchiveInputStream in = null;

        try {
            in = new ArchiveStreamFactory().createArchiveInputStream(ARCHIVERNAME, new FileInputStream(sourceZipFile));

            ZipArchiveEntry entry;

            while ((entry = (ZipArchiveEntry) in.getNextEntry()) != null) {
                String parentOfFile;
                if (entry.isDirectory()) {
                    parentOfFile = entry.getName();
                } else {
                    parentOfFile = new File(entry.getName()).getParent();
                }
                if (parentOfFile != null && !parentOfFile.isEmpty()) {
                    File directory = new File(folder, parentOfFile);
                    if (!directory.exists()) {
                        FileUtils.forceMkdir(directory);
                    }
                }
                if (!entry.isDirectory()) {
                    OutputStream out = new FileOutputStream(new File(folder, entry.getName()));
                    IOUtils.copy(in, out);
                    out.close();
                }
            }
            in.close();
            in = null;
        } catch (FileNotFoundException e) {
            throw new IOException("Zip file " + sourceZipFile + " not found", e);
        } catch (ArchiveException e) {
            throw new IOException("Failed to extract zip file " + sourceZipFile, e);
        } catch (IOException e) {
            throw new IOException("Error while extracting zip file " + sourceZipFile, e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LOG.warn("Error while closing leftover input stream of unzip process", e);
                }
            }
        }
    }

}
