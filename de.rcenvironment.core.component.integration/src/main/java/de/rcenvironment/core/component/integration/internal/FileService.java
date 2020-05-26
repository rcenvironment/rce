/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.integration.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;

import org.osgi.service.component.annotations.Component;

/**
 * A service for ToolIntegration that encapsulates all tasks related to file handling.
 * 
 * @author Alexander Weinert
 */
@Component(service = FileService.class)
public class FileService {

    /**
     * Encapsulates new File(parent, child).
     * 
     * @param parent The path to the parent folder in which the file shall be created.
     * @param child The path of the file to be created relative to parent.
     * @return A {@link File} denoting the file at ``parent/child''.
     */
    public File createFile(String parent, String child) {
        return new File(parent, child);
    }

    /**
     * @param absolutePath A string representation of the path to be created.
     * @return A path object denoting the path to the given absolutePath.
     */
    public Path getPath(String absolutePath) {
        return FileSystems.getDefault().getPath(absolutePath);
    }

    /**
     * Wraps Files.walkFileTree(path, fileVisitor).
     * 
     * @param path The root path of the file tree to be traversed.
     * @param fileVisitor The visitor used for callbacks at every file node.
     * @throws IOException If the underlying call to Files.walkFileTree throws an IOException.
     */
    public void walkFileTree(Path path, FileVisitor<? super Path> fileVisitor) throws IOException {
        Files.walkFileTree(path, fileVisitor);
    }

    /**
     * Wraps Files.isDirectory(path).
     * 
     * @param path The path which is to be checked for being a directory.
     * @return True if the given path is a directory, false otherwise.
     */
    public boolean isDirectory(Path path) {
        return Files.isDirectory(path);
    }

    /**
     * Wraps Files.isRegularFile(path).
     * 
     * @param path The path which is to be checked for being a regular file.
     * @return True if the given path is a regular file, i.e., not a directory, false otherwise.
     */
    public boolean isRegularFile(Path path) {
        return Files.isRegularFile(path);
    }

    /**
     * Wraps new File(file, child).
     * 
     * @param parent The parent file of the newly created file.
     * @param child The path of the file to be created relative to parent
     * @return A File representing 'parent/child'
     */
    public File createFile(File parent, String child) {
        return new File(parent, child);
    }

    /**
     * Wraps FileSystems.getDefault().getPath(parent, child).
     * 
     * @param parent Some path to a parent folder.
     * @param child Some path relative to parent.
     * @return A Path representing 'parent/child'
     */
    public Path getPath(String parent, String child) {
        return FileSystems.getDefault().getPath(parent, child);
    }

}
