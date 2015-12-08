/*
 * Copyright (C) 2006-2015 DLR, Germany
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
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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
                compressDirectoryToZipfile(rootDir, sourceDir + "/" + file.getName() + File.separator, out);
            } else {
                ZipEntry entry = new ZipEntry(sourceDir.replace(rootDir, "") + file.getName());
                entry.setTime(0); // Set creation time to 0 to get the same hash value for same
                                  // content.
                out.putNextEntry(entry);
                FileInputStream in = new FileInputStream(sourceDir + "/" + file.getName());
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

}
