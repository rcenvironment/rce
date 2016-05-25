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
 */
public final class ZipFolderUtil {
    
    /** Our logger instance. */
    protected static final Log LOGGER = LogFactory.getLog(ZipFolderUtil.class);
    
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
     */
    public static void zipFolderContent(final File folder, final File targetZipFile) {
        OutputStream out = null;
        InputStream is = null;
        try {
            out = new FileOutputStream(targetZipFile);
            ArchiveOutputStream os = new ArchiveStreamFactory().createArchiveOutputStream(ARCHIVERNAME, out);  
            
            for (File file: FileUtils.listFiles(folder, null, true)) {
                String zipPath = StringUtils.difference(folder.getCanonicalPath(), file.getCanonicalPath());
                zipPath = zipPath.substring(1);
                is = new FileInputStream(file);
                os.putArchiveEntry(new ZipArchiveEntry(zipPath));
                IOUtils.copy(is, os);
                os.closeArchiveEntry();
                is.close();
            }
            
            out.flush();
            os.flush();
            os.close(); 
        } catch (FileNotFoundException e) {
            LOGGER.error("Zip-file could not be found.", e);
        } catch (ArchiveException e) {
            LOGGER.error("Zip-file could not be compressed.", e);
        } catch (IOException e) {
            LOGGER.error("IO-Exception occured during extracting zip-file.", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    LOGGER.error("IO-Exception occured during closing inputstream of zipping mechanism.", e);
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    LOGGER.error("IO-Exception occured during closing outputstream of zipping mechanism.", e);
                }
            }
        }  
    }

    
    /**
     * Extract Zip file to a destination folder.
     * 
     * @param folder destination where zip file should be extracted
     * @param sourceZipFile zip file to extract
     */
    public static void extractZipToFolder(final File folder, final File sourceZipFile) {
        InputStream is = null;
        
        try {
            is = new FileInputStream(sourceZipFile);
            ArchiveInputStream in = new ArchiveStreamFactory().createArchiveInputStream(ARCHIVERNAME, is);
            
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
        } catch (FileNotFoundException e) {
            LOGGER.error("Zip-file could not be found.", e);
        } catch (ArchiveException e) {
            LOGGER.error("Zip-file could not be extracted.", e);
        } catch (IOException e) {
            LOGGER.error("IO-Exception occured during extracting zip-file", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    LOGGER.error("IO-Exception occured during closing inputstream of unzipping mechanism.", e);
                }
            }
        }
    }

}
