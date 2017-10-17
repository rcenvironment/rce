/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * Helper class for compressing / unfolding folders.
 * 
 * @author Sascha Zur
 */
public final class CompressingHelper {

    private CompressingHelper() {}

    /**
     * Create a zipped byte array from the given folder.
     * 
     * @param folderToCompress folder to compress.
     * @return byte array of zipped folder or null, if the given directory does not exist.
     * @throws IOException if compressing did not work
     * @throws FileNotFoundException if the temp dir could not be created
     */
    public static byte[] createZippedByteArrayFromFolder(File folderToCompress) throws IOException, FileNotFoundException {
        if (folderToCompress.exists()) {
            File tempZipFile = TempFileServiceAccess.getInstance().createTempFileFromPattern("zippedFolder*.tar.gz");
            ZipOutputStream zipFile = new ZipOutputStream(new FileOutputStream(tempZipFile));
            compressDirectoryToZipfile(folderToCompress.getAbsolutePath(), folderToCompress.getAbsolutePath(), zipFile);
            IOUtils.closeQuietly(zipFile);
            tempZipFile.setLastModified(0);
            byte[] zippedByteArray = FileUtils.readFileToByteArray(tempZipFile);
            TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(tempZipFile);
            return zippedByteArray;
        }
        return null;
    }

    private static void compressDirectoryToZipfile(String rootDir, String sourceDir, ZipOutputStream out)
        throws IOException, FileNotFoundException {
        File[] list = new File(sourceDir).listFiles();
        Arrays.sort(list);
        for (File file : list) {
            if (file.isDirectory()) {
                compressDirectoryToZipfile(rootDir, new File(sourceDir, file.getName()).getAbsolutePath(), out);
            } else {
                if (CrossPlatformFilenameUtils.isNFSFile(file.getName())) {
                    continue;
                }

                ZipEntry entry = new ZipEntry(sourceDir.replace(rootDir, "") + file.getName());
                entry.setTime(0); // Set creation time to 0 to get the same hash value for same
                                  // content.
                out.putNextEntry(entry);
                FileInputStream in = new FileInputStream(new File(sourceDir, file.getName()));
                IOUtils.copy(in, out);
                IOUtils.closeQuietly(in);
            }
        }
    }

    /**
     * Decompresses a given byte array that is a compressed folder.
     * 
     * @param folderAsCompressedArray to decompress
     * @param unzippedLocation where the decompressed folder should be
     * @throws IOException e
     * @throws FileNotFoundException e
     */
    public static void decompressFolderByteArray(byte[] folderAsCompressedArray, File unzippedLocation)
        throws IOException, FileNotFoundException {
        ZipInputStream zipFile = new ZipInputStream(new ByteArrayInputStream(folderAsCompressedArray));
        ZipEntry ze = null;
        final int minusOne = -1;
        while ((ze = zipFile.getNextEntry()) != null) {
            FileOutputStream fout = new FileOutputStream(new File(unzippedLocation, ze.getName()).getAbsolutePath());
            for (int c = zipFile.read(); c != minusOne; c = zipFile.read()) {
                fout.write(c);
            }
            zipFile.closeEntry();
            fout.close();
        }
        zipFile.close();
    }

    /**
     * Unzip a zip input stream to the destination folder.
     * 
     * @param is {@link InputStream} from a zip file
     * @param destination direcotry to unzip to
     * @throws FileNotFoundException wrong is.
     * @throws IOException unzipping failed
     * @throws ArchiveException unzipping failed
     */
    public static void unzip(InputStream is, File destination) throws FileNotFoundException, IOException, ArchiveException {
        try (ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream("zip", is)) {
            ZipEntry entry = null;
            while ((entry = (ZipArchiveEntry) ais.getNextEntry()) != null) {
                if (entry.getName().endsWith("/")) {
                    File dir = new File(destination, entry.getName());
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    continue;
                }

                File outFile = new File(destination, entry.getName());
                if (outFile.isDirectory()) {
                    continue;
                }
                if (outFile.exists()) {
                    continue;
                }
                FileOutputStream out = new FileOutputStream(outFile);
                final int byteBuffer = 1024;
                byte[] buffer = new byte[byteBuffer];
                int length = 0;
                while ((length = ais.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                    out.flush();
                }
                out.close();
            }
        }
    }

}
