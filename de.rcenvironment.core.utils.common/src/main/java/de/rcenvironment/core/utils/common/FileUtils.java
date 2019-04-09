/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.input.TeeInputStream;

/**
 * A utility class for file operations.
 * 
 * @author Tobias Rodehutskors
 * @author Robert Mischke (added stream duplication)
 */
public final class FileUtils {

    private FileUtils() {

    }

    /**
     * Checks if a file is locked by trying to rename it.
     * 
     * TODO (p1) We need to check if this implementation is also a valid check on Linux. ~ rode_to
     * 
     * @param file The file to inspect.
     * @return True, if the file is locked.
     */
    public static boolean isLocked(File file) {
        return !file.renameTo(file);
    }

    /**
     * Sets up mirroring of the given {@link InputStream} to the given output file. The target file will be created if it does not exist; if
     * it already exists, it is overwritten/reset. Write operations to this output file will be buffered. The log file will be closed when
     * the given {@link InputStream} is being closed.
     * 
     * @param inputStream the {@link InputStream} to "wrap"; make sure to use the returned {@link InputStream} instead of this one in all
     *        subsequent operations!
     * @param logFile the target file to write all bytes read from the given {@link InputStream} to
     * 
     * @return the proxy {@link InputStream} to use in place of the original stream in all subsequent operations
     * 
     * @throws IOException on I/O errors while setting up the log file
     */
    public static InputStream setUpStreamDuplicationToOutputFile(InputStream inputStream, File logFile) throws IOException {
        // use default options: use buffered writing & overwrite instead of appending
        return setUpStreamDuplicationToOutputFile(inputStream, logFile, true, false);
    }

    /**
     * Sets up mirroring of the given {@link InputStream} to the given output file. The target file will be created if it does not exist.
     * Whether an existing log file is replaced or appended to is specified by a boolean parameter. The log file will be closed when the
     * given {@link InputStream} is being closed.
     * 
     * @param inputStream the {@link InputStream} to "wrap"; make sure to use the returned {@link InputStream} instead of this one in all
     *        subsequent operations!
     * @param logFile the target file to write all bytes read from the given {@link InputStream} to
     * @param useBufferedWriting whether the write operations to the log file should use buffered writing; usually, this is recommended
     * @param appendIfLogFileExists if true, append the output to the given log file if it already exists; otherwise, overwrite/reset the
     *        log file
     * 
     * @return the proxy {@link InputStream} to use in place of the original stream in all subsequent operations
     * 
     * @throws IOException on I/O errors while setting up the log file
     */
    public static InputStream setUpStreamDuplicationToOutputFile(InputStream inputStream, File logFile,
        boolean useBufferedWriting, boolean appendIfLogFileExists) throws IOException {

        OutputStream logFileStream = new FileOutputStream(logFile, appendIfLogFileExists);
        if (useBufferedWriting) {
            logFileStream = new BufferedOutputStream(logFileStream);
        }
        // true = auto close stream
        return new TeeInputStream(inputStream, logFileStream, true);
    }

}
