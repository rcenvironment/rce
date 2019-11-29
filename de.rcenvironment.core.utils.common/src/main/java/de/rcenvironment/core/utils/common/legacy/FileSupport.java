/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.legacy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


/**
 * File support methods.
 * 
 * @author Doreen Seider
 * @deprecated user apache.commons.io or other common libraries
 */
@Deprecated
public abstract class FileSupport {

    private static final int EOF_INDICATOR = -1;
    private static final int BUFFER_SIZE = 8192;

    /**
     * Private constructor of this utility class.
     */
    private FileSupport() {}
    
    /**
     * Deletes a directory recursively.
     * 
     * @param file the directory that should be deleted.
     * @return True on success.
     */
    public static boolean deleteFile(File file) {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                boolean success = deleteFile(f);
                if (!success) {
                    return false;
                }
            }
        }
        return file.delete();
    }

    /**
     * Zip's a file or directory.
     * 
     * @param file the plain file or directory
     * @return a zip'ed stream
     * @throws IOException in case of an IOException
     * @throws InterruptedException upon interruption
     */
    public static byte[] zip(final File file) throws IOException, InterruptedException {
        final ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        final ZipOutputStream zipOut = new ZipOutputStream(byteOut);
        zip(file, file, zipOut);
        zipOut.close();
        final byte[] result = byteOut.toByteArray();
        return result;
    }

    /**
     * Zip's a file or directory.
     * 
     * @param file the input plain file or directory
     * @param zip the output file
     * @throws IOException in case of an IOException
     * @throws InterruptedException upon interruption
     */
    public static void zipToFile(final File file, final File zip) throws IOException, InterruptedException {
        if (!zip.exists()) {
            zip.createNewFile();
        }
        final FileOutputStream fileOut = new FileOutputStream(zip);
        final ZipOutputStream zipOut = new ZipOutputStream(fileOut);
        zip(file, file, zipOut);
        zipOut.close();
    }

    /**
     * Zip's a file or directory.
     * 
     * @param file the plain file or directory
     * @param base the base file or directory
     * @param out the {@link ZipOutputStream} to write to
     * @throws IOException in case of an IOException
     * @throws InterruptedException upon interruption
     */
    public static void zip(final File file, final File base, final ZipOutputStream out) throws IOException, InterruptedException {
        if (!file.exists() || !file.canRead()) {
            throw new RuntimeException("File does not exist or can not be read.");
        }
        if (Thread.interrupted()) {
            Thread.currentThread().interrupt();
            throw new InterruptedException();
        }
        if (file.isDirectory()) {
            final File[] subFiles = file.listFiles();
            for (final File subFile : subFiles) {
                zip(subFile, base, out);
            }
        } else {
            final FileInputStream in = new FileInputStream(file);
            final ZipEntry entry = new ZipEntry(file.getPath().substring(
                base.getPath().length() + 1));
            out.putNextEntry(entry);
            final byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != EOF_INDICATOR) {
                out.write(buffer, 0, read);
            }
            in.close();
        }
    }

    /**
     * Unzip's content to a directory.
     * 
     * @param zip the zip'ed content
     * @param target the target directory
     * @throws IOException in case of an IOException
     */
    public static void unzip(final byte[] zip, final File target) throws IOException {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(zip);
        unzip(inputStream, target);
    }

    /**
     * Unzip's content from an {@link InputStream} to a directory.
     * 
     * @param inputStream the {@link InputStream} providing the zip'ed content
     * @param target the target directory
     * @throws IOException in case of an IOException
     */
    public static void unzip(final InputStream inputStream, final File target) throws IOException {
        final ZipInputStream input = new ZipInputStream(inputStream);
        ZipEntry entry;
        while ((entry = input.getNextEntry()) != null) {
            int count;
            byte[] data = new byte[BUFFER_SIZE];
            // write the files to the disk
            final File file = new File(target, entry.getName());
            if (entry.isDirectory()) {
                file.mkdirs();
            } else {
                final File parent = file.getParentFile();
                parent.mkdirs();
                FileOutputStream out = new FileOutputStream(file);
                while ((count = input.read(data, 0, BUFFER_SIZE)) != EOF_INDICATOR) {
                    out.write(data, 0, count);
                }
                out.close();
            }
        }
        input.close();
    }

}
