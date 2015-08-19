/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.utils.executor.fileinfo;

import java.io.IOException;
import java.util.Collection;

/**
 * Provides information about files and directories in a file system.
 * @author Christian Weiss
 */
public interface FileInfoService {

    /**
     * Lists all file of a given directory.
     * @param path absolute path to directory or regular expression
     * @return file information
     * @throws IOException on I/O errors
     */
    Collection<FileInfo> listFiles(String path) throws IOException;

    /**
     * Lists all file of a given directory.
     * @param pyth absolute path to directory or regular expression
     * @param recursively <code>true</code> with sub-directories, otherwise <code>false</code>
     * @return file information
     * @throws IOException on I/O errors
     */
    Collection<FileInfo> listFiles(String pyth, boolean recursively) throws IOException;

    /**
     * Lists all files and directories of a given directory.
     * 
     * @param path absolute path to directory or regular expression
     * @return file and directory information
     * @throws IOException on I/O errors
     */
    Collection<FileInfo> listContent(String path) throws IOException;

    /**
     * Lists all files and directories of a given directory.
     * @param path absolute path to directory or regular expression
     * @param recursively <code>true</code> with sub-directories, otherwise <code>false</code>
     * @return file and directory information
     * @throws IOException on I/O errors
     */
    Collection<FileInfo> listContent(String path, boolean recursively) throws IOException;

    /**
     * Checks if given path points to a directory.
     * @param path absolute path to check
     * @return <code>true</code> if directory, otherwise <code>false</code>
     * @throws IOException on I/O errors
     */
    boolean isDirectory(String path) throws IOException;

    /**
     * Retrieves the size of a given file.
     * @param path absolute path to file
     * @return size of file
     * @throws IOException on I/O errors
     */
    Long size(String path) throws IOException;

}
