/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * This class consists of static methods to provide compression (e.g. zip, tar) to or from the file system. By using this centralized
 * implementation, it is facilitated to identify and fix corresponding security vulnerabilities. All static methods of this class are
 * thread-safe.
 *
 * @author Sascha Zur (previous CompressingHelper class)
 * @author Markus Kunde (previous ZipFolderUtil class)
 * @author Markus Litz (previous ZipFolderUtil class)
 * @author Robert Mischke (previous ZipFolderUtil class)
 * @author Thorsten Sommer (refactored aforementioned CompressingHelper & ZipFolderUtil into this class)
 */
public final class FileCompressionService {

    private static final Log LOG = LogFactory.getLog(FileCompressionService.class);

    private static final String COMPRESS_IO_ISSUE = "Compress: Was not able to compress due to an IO issue: '%s'";

    private static final String EXPAND_THE_SOURCE_WAS_EMPTY = "Expand: The source was null.";

    private static final String EXPAND_IO_ISSUE = "Expand: Was not able to expand due to an IO issue: '%s'";

    // Define file name filters. Ignore these files while compress a directory i.e.
    // a filter returns true to ignore a file.
    private static final List<? extends FilenameFilter> FILTERS = Arrays.asList(new FilenameFilter() {

        @Override
        public boolean filter(File file) {
            return CrossPlatformFilenameUtils.isNFSFile(file.getName());
        }
    });

    /**
     * 
     * This interface allows the definition of lambdas to filter files based on their name. A filter must yield true in order to ignore a
     * file.
     *
     * @author Thorsten Sommer
     */
    private interface FilenameFilter {

        boolean filter(File file);
    }

    /**
     * 
     * The constructor of this class is private, since the class should only contain static methods.
     *
     */
    private FileCompressionService() {}

    /**
     * 
     * This method compresses the specified directory {@code sourceDirectory} and writes the compressed file to the specified location
     * {@code destinationFile}. This method is thread-safe.
     * 
     * @param sourceDirectory - the source directory to compress. Must be an existing directory.
     * @param destinationFile - the target file to which the compressed data is written. Must be a file. If not existing, the file gets
     *        created.
     * @param format - the compression format to use, e.g. zip or tar.
     * @param integrateSourceDirectory - true to use the source directory's name as root element of the archive.
     * @return true if the compression operation was successful.
     */
    public static Boolean compressDirectoryToFile(final File sourceDirectory, final File destinationFile,
        final FileCompressionFormat format, final Boolean integrateSourceDirectory) {

        if (destinationFile == null) {
            LOG.error("Compress: The destination file was null.");
            return false;
        }

        if (format == null) {
            LOG.error("Compress: The desired compression format was null.");
            return false;
        }

        if (!destinationFile.getParentFile().exists()) {

            // Create the destination's directories:
            if (!destinationFile.getParentFile().mkdirs()) {
                LOG.error(String.format("Compress: Was not able to create the desired destination directories '%s'.",
                    destinationFile.getParentFile()));
                return false;
            }
        }

        if (destinationFile.isDirectory()) {
            LOG.error("Compress: The desired destination is a directory instead of a file.");
            return false;
        }

        if (sourceDirectory == null) {
            LOG.error("Compress: The source directory was null.");
            return false;
        }

        //
        // Please be careful... there are three known cases cf.
        // https://stackoverflow.com/q/30520179/2258393
        //
        final Path sourcePath = sourceDirectory.toPath();
        final Boolean assumeSourceDirectoryExist;

        if (Files.exists(sourcePath)) {
            // Case 1: Java is sure that the file exist
            assumeSourceDirectoryExist = true;
        } else if (Files.notExists(sourcePath)) {
            // Case 2: Java is sure that the file does not exist
            assumeSourceDirectoryExist = false;
        } else {
            // Case 3: Java does not know or is not sure, cf. StackOverflow
            assumeSourceDirectoryExist = false;
        }

        if (!assumeSourceDirectoryExist) {
            LOG.error("Compress: It seems that the source directory does not exist.");
            return false;
        }

        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destinationFile))) {
            return FileCompressionService.compressDirectoryToOutputStream(sourceDirectory, bos, format,
                integrateSourceDirectory);
        } catch (FileNotFoundException e) {
            LOG.error(String.format("Compress: Was not able to compress due to a file not found issue: '%s'",
                e.getMessage()));
            return false;
        } catch (IOException e) {
            LOG.error(String.format(COMPRESS_IO_ISSUE, e.getMessage()));
            return false;
        }
    }

    /**
     * 
     * This method compresses the specified directory {@code sourceDirectory} and writes the compressed file to the specified stream
     * {@code OutputStream}. This method is thread-safe.
     * 
     * @param sourceDirectory - the source directory to compress. Must be an existing directory.
     * @param destination - the output stream where to write the compressed data.
     * @param format - the compression format to use, e.g. zip or tar.
     * @param integrateSourceDirectory - true to use the source directory's name as root element of the archive.
     * @return true if the compression operation was successful.
     */
    public static Boolean compressDirectoryToOutputStream(final File sourceDirectory, final OutputStream destination,
        final FileCompressionFormat format, final Boolean integrateSourceDirectory) {

        if (sourceDirectory == null) {
            LOG.error("Compress: The source directory was null.");
            return false;
        }

        if (destination == null) {
            LOG.error("Compress: The destination output stream was null.");
            return false;
        }

        if (format == null) {
            LOG.error("Compress: The desired compression format was null.");
            return false;
        }

        //
        // Please be careful... there are three known cases cf.
        // https://stackoverflow.com/q/30520179/2258393
        //
        final Path sourcePath = sourceDirectory.toPath();
        final Boolean assumeSourceDirectoryExist;

        if (Files.exists(sourcePath)) {
            // Case 1: Java is sure that the file exist
            assumeSourceDirectoryExist = true;
        } else if (Files.notExists(sourcePath)) {
            // Case 2: Java is sure that the file does not exist
            assumeSourceDirectoryExist = false;
        } else {
            // Case 3: Java does not know or is not sure, cf. StackOverflow
            assumeSourceDirectoryExist = false;
        }

        if (!assumeSourceDirectoryExist) {
            LOG.error("Compress: It seems that the source directory does not exist.");
            return false;
        }

        if (!sourceDirectory.isDirectory()) {
            LOG.error("Compress: The source is a file instead of a directory.");
            return false;
        }

        final OutputStream outputStream;
        if (format.applyGzipToArchiveStream()) {
            try {
                outputStream = new GZIPOutputStream(destination);
            } catch (IOException e) {
                LOG.error("Error creating GZip stream", e);
                return false;
            }
        } else {
            outputStream = destination;
        }

        try (ArchiveOutputStream aos = new ArchiveStreamFactory()
            .createArchiveOutputStream(format.getArchiveStreamType(), outputStream)) {
            return FileCompressionService.compress(aos, sourceDirectory, integrateSourceDirectory);
        } catch (IOException e) {
            LOG.error(String.format(COMPRESS_IO_ISSUE, e.getMessage()));
            return false;
        } catch (ArchiveException e) {
            LOG.error(
                String.format("Compress: Was not able to compress due to an archive issue: '%s'", e.getMessage()));
            return false;
        }
    }

    /**
     * 
     * This method compresses the specified directory {@code sourceDirectory}. The result gets returned as {@link ByteArrayOutputStream}.
     * This method is thread-safe. The caller is responsible to close the returned stream. <b>Please note</b> that this method allocates as
     * many memory as needed to hold the entire compressed directory. Consider to use another method to use a implementation based on
     * streaming.
     * 
     * @param sourceDirectory - the source directory to compress. Must be an existing directory.
     * @param format - the compression format to use, e.g. zip or tar.
     * @param integrateSourceDirectory - true to use the source directory's name as root element of the archive.
     * @return the {@link ByteArrayOutputStream} which contains the compressed data. The caller is responsible to close this stream. The
     *         optional object is empty in case of any error.
     */
    public static ByteArrayOutputStream compressDirectoryToByteStream(final File sourceDirectory,
        final FileCompressionFormat format, final Boolean integrateSourceDirectory) {

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (BufferedOutputStream bos = new BufferedOutputStream(baos)) {
            final Boolean result = FileCompressionService.compressDirectoryToOutputStream(sourceDirectory, bos, format,
                integrateSourceDirectory);
            if (result) {
                return baos;
            } else {
                return null;
            }

        } catch (IOException e) {
            LOG.error(String.format(COMPRESS_IO_ISSUE, e.getMessage()));
            return null;
        }
    }

    /**
     * 
     * This method compresses the specified directory {@code sourceDirectory}. The result gets returned as byte array. This method is
     * thread-safe. <b>Please note</b> that this method allocates as many memory as needed to hold the entire compressed directory. Consider
     * to use another method to use a implementation based on streaming.
     * 
     * @param sourceDirectory - the source directory to compress. Must be an existing directory.
     * @param format - the compression format to use, e.g. zip or tar.
     * @param integrateSourceDirectory - true to use the source directory's name as root element of the archive.
     * @return the byte array which contains the compressed data. The optional object is empty in case of any error.
     */
    public static byte[] compressDirectoryToByteArray(final File sourceDirectory, final FileCompressionFormat format,
        final Boolean integrateSourceDirectory) {

        final ByteArrayOutputStream optionalStream = FileCompressionService
            .compressDirectoryToByteStream(sourceDirectory, format, integrateSourceDirectory);
        if (optionalStream == null) {
            return null;
        }

        try (ByteArrayOutputStream baos = optionalStream) {
            return baos.toByteArray();
        } catch (IOException e) {
            LOG.error(String.format(COMPRESS_IO_ISSUE, e.getMessage()));
            return null;
        }
    }

    /**
     * 
     * This method expands a compressed archive {@code sourceCompressed} into a directory {@code destinationDirectory}. This method is
     * thread-safe.
     * 
     * @param sourceCompressed - the input file which contains the compressed data.
     * @param destinationDirectory - the final destination where the compressed files should be expanded.
     * @param format - the desired compression format.
     * @return true if the expanding was successful.
     */
    public static Boolean expandCompressedDirectoryFromFile(final File sourceCompressed,
        final File destinationDirectory, final FileCompressionFormat format) {

        if (sourceCompressed == null) {
            LOG.error(EXPAND_THE_SOURCE_WAS_EMPTY);
            return false;
        }

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sourceCompressed))) {
            return FileCompressionService.expandCompressedDirectoryFromInputStream(bis, destinationDirectory, format);
        } catch (FileNotFoundException e) {
            LOG.error(String.format("Expand: Was not able to expand due to a file not found issue: '%s'",
                e.getMessage()));
            return false;
        } catch (IOException e) {
            LOG.error(String.format(EXPAND_IO_ISSUE, e.getMessage()));
            return false;
        }
    }

    /**
     * 
     * This method expands a compressed archive {@code sourceCompressed} into a directory {@code destinationDirectory}. This method is
     * thread-safe.
     * 
     * @param sourceCompressed - the input stream which contains the compressed data.
     * @param destinationDirectory - the final destination where the compressed files should be expanded.
     * @param format - the desired compression format.
     * @return true if the expanding was successful.
     */
    public static Boolean expandCompressedDirectoryFromInputStream(final InputStream sourceCompressed,
        final File destinationDirectory, final FileCompressionFormat format) {

        if (sourceCompressed == null) {
            LOG.error(EXPAND_THE_SOURCE_WAS_EMPTY);
            return false;
        }

        if (destinationDirectory == null) {
            LOG.error("Expand: The destination directory i.e. destinationDirectory was null.");
            return false;
        }

        if (format == null) {
            LOG.error("Expand: The desired compression format was null.");
            return false;
        }

        if (!destinationDirectory.exists()) {
            if (!destinationDirectory.mkdirs()) {
                LOG.error(String.format("Expand: Was not able to create the desired destination directory '%s'.",
                    destinationDirectory.getPath()));
                return false;
            }
        }

        if (!destinationDirectory.isDirectory()) {
            LOG.error("Expand: The destination is a file instead of a directory.");
            return false;
        }

        final InputStream inputStream;
        if (format.applyGzipToArchiveStream()) {
            try {
                inputStream = new GZIPInputStream(sourceCompressed);
            } catch (IOException e) {
                LOG.error("Error creating GZip stream", e);
                return false;
            }
        } else {
            inputStream = sourceCompressed;
        }

        try (ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream(format.getArchiveStreamType(),
            inputStream)) {
            return FileCompressionService.uncompress(ais, destinationDirectory);
        } catch (IOException e) {
            LOG.error(String.format(EXPAND_IO_ISSUE, e.getMessage()));
            return false;
        } catch (ArchiveException e) {
            LOG.error(String.format("Expand: Was not able to expand due to an archive issue: '%s'", e.getMessage()));
            return false;
        }
    }

    /**
     * 
     * This method expands a compressed archive {@code sourceCompressed} into a directory {@code destinationDirectory}. This method is
     * thread-safe.
     * 
     * @param sourceCompressed - the byte input stream which contains the compressed data.
     * @param destinationDirectory - the final destination where the compressed files should be expanded.
     * @param format - the desired compression format.
     * @return true if the expanding was successful.
     */
    public static Boolean expandCompressedDirectoryFromByteStream(final ByteArrayInputStream sourceCompressed,
        final File destinationDirectory, final FileCompressionFormat format) {

        if (sourceCompressed == null) {
            LOG.error(EXPAND_THE_SOURCE_WAS_EMPTY);
            return false;
        }

        if (sourceCompressed.available() == 0) {
            LOG.warn("Expand: The given input stream was empty.");
            return false;
        }

        try (BufferedInputStream bis = new BufferedInputStream(sourceCompressed)) {
            return FileCompressionService.expandCompressedDirectoryFromInputStream(bis, destinationDirectory, format);
        } catch (IOException e) {
            LOG.error(String.format(EXPAND_IO_ISSUE, e.getMessage()));
            return false;
        }
    }

    /**
     * 
     * This method expands a compressed archive {@code sourceCompressed} into a directory {@code destinationDirectory}. This method is
     * thread-safe.
     * 
     * @param sourceCompressed - the byte array which contains the compressed data.
     * @param destinationDirectory - the final destination where the compressed files should be expanded.
     * @param format - the desired compression format.
     * @return true if the expanding was successful.
     */
    public static Boolean expandCompressedDirectoryFromByteArray(final byte[] sourceCompressed,
        final File destinationDirectory, final FileCompressionFormat format) {

        if (sourceCompressed == null) {
            LOG.error(EXPAND_THE_SOURCE_WAS_EMPTY);
            return false;
        }

        if (sourceCompressed.length == 0) {
            LOG.warn("Expand: The given array was empty.");
            return false;
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(sourceCompressed)) {
            return FileCompressionService.expandCompressedDirectoryFromInputStream(bais, destinationDirectory, format);
        } catch (IOException e) {
            LOG.error(String.format(EXPAND_IO_ISSUE, e.getMessage()));
            return false;
        }
    }

    /**
     * 
     * This private method compresses a given directory recursively.
     * 
     * @param outputStream - the desired output stream
     * @param sourceDirectory - the source directory
     * @return true if the entire source directory was successfully compressed
     */
    private static Boolean compress(final ArchiveOutputStream outputStream, final File sourceDirectory,
        final Boolean integrateSourceDirectory) {

        if (outputStream == null) {
            LOG.error("Was not able to compress due to missing output stream.");
            return false;
        }

        if (sourceDirectory == null) {
            LOG.error("Was not able to compress due to missing source directory.");
            return false;
        }

        final ArchiveOutputStream os = outputStream;
        final File source = sourceDirectory;
        final String sourceName;

        if (os instanceof TarArchiveOutputStream) {
            ((TarArchiveOutputStream) os).setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
        }
        
        if (integrateSourceDirectory) {
            sourceName = String.format("%s%s", source.getName(), File.separator);
        } else {
            sourceName = "";
        }

        // For each file at the source directory (recursively):
        fileLoop: for (final File file : FileUtils.listFilesAndDirs(source, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)) {
            try {
                // Find the first file name filter who does not match:
                for (FilenameFilter f : FILTERS) {
                    if (f.filter(file)) {
                        // filter matched -> ignore this file
                        continue fileLoop;
                    }
                }

                // Construct the relative path for the file's representation inside the archive.
                // Note I: substring(1) removes the leading / or \
                // Note II: The name of the source directory e.g. sourceName might be included!
                String zipPath;
                if (file.equals(source)) {
                    if (!integrateSourceDirectory) {
                        continue fileLoop;
                    }
                    zipPath = sourceName;
                } else {
                    zipPath = String.format("%s%s", sourceName,
                        StringUtils.difference(source.getCanonicalPath(), file.getCanonicalPath()).substring(1));
                }

                os.putArchiveEntry(os.createArchiveEntry(file, zipPath));

                if (file.isFile()) {
                    // Archive and compress the file:
                    try (InputStream is = new FileInputStream(file)) {
                        IOUtils.copy(is, os);
                    }
                }

                os.closeArchiveEntry();

            } catch (IOException e) {
                LOG.error(String.format("Was not able to compress the file '%s'.", file.getPath()));
                return false;
            }
        }

        return true;
    }

    /**
     * 
     * This private method uncompresses an archive to the file systems.
     * 
     * @param inputStream - the input stream from where the files are expanded
     * @param destinationDirectory - the destination directory
     * @return true if the entire archive was successfully uncompressed
     */
    private static Boolean uncompress(final ArchiveInputStream inputStream, final File destinationDirectory) {

        if (inputStream == null) {
            LOG.error("Was not able to uncompress due to missing input stream.");
            return false;
        }

        if (destinationDirectory == null) {
            LOG.error("Was not able to uncompres due to missing destination directory.");
            return false;
        }

        // Store the destination path for continuously checking:
        final String destinationPath;
        try {
            destinationPath = destinationDirectory.getCanonicalPath();
        } catch (IOException e) {
            LOG.error(String.format("Was not able to construct the destination path for uncompres an archive: %s",
                e.getMessage()));
            return false;
        }

        // Loop over all known file entries in the archive:
        ArchiveEntry entry = null;
        final ArchiveInputStream is = inputStream;
        do {
            try {
                entry = is.getNextEntry();
                if (entry == null) {
                    break;
                }

                // Determine the parent of this entry:
                final String parentOfFile;
                if (entry.isDirectory()) {
                    parentOfFile = entry.getName();
                } else {
                    parentOfFile = new File(entry.getName()).getParent();
                }

                // Ensure that the parent exist:
                if (parentOfFile != null && !parentOfFile.isEmpty()) {
                    final File directory = new File(destinationDirectory, parentOfFile);
                    if (!directory.exists()) {
                        FileUtils.forceMkdir(directory);
                    }
                }

                // In case that entry is a file: Extract it.
                if (!entry.isDirectory()) {

                    // Create the destination file's object:
                    final File destinationFile = new File(destinationDirectory, entry.getName());

                    // Check against "zip slip" attacks, cf.
                    // https://github.com/snyk/zip-slip-vulnerability:
                    if (!destinationFile.getCanonicalPath().startsWith(destinationPath)) {
                        LOG.warn(
                            String.format(
                                "Detected a zip slip attack while uncompress a file. The affected file is '%s'."
                                    + "The attack was prevented.",
                                destinationDirectory.getCanonicalPath()));
                        return false;
                    }

                    try (OutputStream os = new FileOutputStream(destinationFile)) {
                        IOUtils.copy(is, os);
                    }
                }
            } catch (IOException ioe) {
                LOG.error(String.format("Was not able to uncompress the archive: '%s'", ioe.getMessage()));
                return false;
            }
        } while (entry != null);
        return true;
    }
}
