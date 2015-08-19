/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * A service interface for creating temporary directories and files in a centrally defined location, with additional convenience methods.
 * Implementations of this service should always be used instead of manually creating temp files and folders to allow for automatic cleanup
 * of file resources, even on unclean shutdown of an instance.
 * <p>
 * The main design goals were:
 * <ul>
 * <li>Provide central cleanup of temp files, to avoid temp files getting left behind after crashes or components/tools that do not
 * implement proper cleanup</li>
 * <li>Prevent collisions between multiple RCE instances on acquisition or cleanup</li>
 * <li>Prevent accidential deletes outside of the created temp folders, as far as possible</li>
 * <li>Heap usage and the number of file locks should not increase with the number of acquired temp files and directories, to prevent
 * resource drain in long-running instances</li>
 * <li>(to be continued: specific-filename temp files etc.)</li>
 * </ul>
 * <br/>
 * Basic approach:
 * <ul>
 * <li>TODO</li>
 * </ul>
 * 
 * @author Robert Mischke
 */
public interface TempFileService {

    /**
     * Creates a new "managed" temporary directory with an information text. See {@link #createManagedTempDir(String)} for details.
     * 
     * @return a new, empty directory that is guaranteed to exist
     * 
     * @throws IOException if creating the new directory fails
     */
    File createManagedTempDir() throws IOException;

    /**
     * Creates a new "managed" temporary directory with an optional information text that is added to the directories filename. The latter
     * is only for identifying temporary directories during debugging; it has no effect at runtime (except for the fact that illegal
     * characters will break creation of the directory).
     * 
     * @param infoText the optional information text; ignored if null or empty
     * @return a new, empty directory that is guaranteed to exist
     * 
     * @throws IOException if creating the new directory fails
     */
    File createManagedTempDir(String infoText) throws IOException;

    /**
     * Creates a new temporary, managed file from the given filename pattern.
     * 
     * @param filenamePattern the pattern for the name of the temporary file; must contain the character "*", which is replaced by a
     *        generated string; relative paths are not permitted at this time
     * @return a new {@link File} pointing to a newly created, empty file
     * @throws IOException if creating the file or a containing directory fails
     */
    File createTempFileFromPattern(String filenamePattern) throws IOException;

    /**
     * Creates a new file in a managed temporary directory with the given filename, ie the last segment of the generated path will be the
     * passed string.
     * 
     * Note that when the same filename is repeatedly requested, a new directory is created for each request. If the filename is not
     * actually relevant, using {@link #createTempFileFromPattern(String)} requires less I/O operations, and should therefore be slightly
     * faster.
     * 
     * @param filename the filename for the new temporary file; relative paths are not permitted
     * @return a new {@link File} pointing to a newly created, empty file
     * @throws IOException if creating the file or a containing directory fails
     */
    File createTempFileWithFixedFilename(String filename) throws IOException;

    /**
     * Creates a new temporary file with a randomly-generated filename, and copies the content of the given {@link InputStream} into it. The
     * stream is closed after copying its contents.
     * 
     * @param is the {@link InputStream} to read from
     * @return the {@link File} object representing the genrated file
     * @throws IOException if temp file creation or stream reading failed
     */
    File writeInputStreamToTempFile(InputStream is) throws IOException;

    // TODO add more convenience methods?

    /**
     * An optional method that can be used to release disk space used by temporary files as soon as they are no longer needed, instead of
     * leaving them for automatic cleanup. Whenever files of a significant size are written and the end-of-use time can be determined,
     * calling this method is strongly recommended.
     * 
     * The main benefit over simply using commons-io FileUtils#deleteDirectory(File) is that this method is safer: It can check if the
     * provided {@link File} is indeed inside the managed temporary directory, therefore avoiding accidential mixups leading to wrong data
     * being deleted.
     * 
     * Even after a successful call, no guarantee is made if the given file or directory was immediately removed. (For example, deleting
     * these files may have been delegated to a background task.)
     * 
     * @param tempFileOrDir a {@link File} pointing to the directory or file to be deleted
     * 
     * @throws IOException on consistency errors, or when the delete operation failed
     */
    void disposeManagedTempDirOrFile(File tempFileOrDir) throws IOException;

}
