/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.integration.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.osgi.service.component.annotations.Component;

/**
 * Wrapper for methods used by {@link IconHelper} concerning {@link File} reading and copying. We wrap these methods in order to be able to
 * test icon helper.
 * 
 * @author Alexander Weinert
 */
@Component(service = FileAccessService.class)
public class FileAccessService {

    /**
     * Wraps Files.readAllBytes.
     * 
     * @param file The file to be read
     * @return A byte array containing the contents of the given file
     * @throws IOException If an error occurs during reading the given file.
     */
    public byte[] readToByteArray(File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }

    /**
     * Wraps File-constructor.
     * 
     * @param path The path at which a file is to be created, may be relative or absolute.
     * @return A file representing the given path.
     */
    public File createFile(String path) {
        return new File(path);
    }

    /**
     * Wraps File-constructor.
     * 
     * @param parent The parent folder of the file to be created.
     * @param child The name of the actual file, must be relative to parent.
     * @return A file describing the file at parent/child
     */
    public File createFile(File parent, String child) {
        return new File(parent, child);
    }

    /**
     * Wraps FileUtils.copyFile.
     * 
     * @param source The file that shall be copied.
     * @param destination The file to which source shall be copied.
     * @throws IOException If an error occurs during the copying.
     */
    public void copyFile(File source, File destination) throws IOException {
        FileUtils.copyFile(source, destination);
    }

    /**
     * Wraps IOUtils.toByteArray.
     * 
     * @param inputStream Some stream of values.
     * @return A byte array containing all values of the input stream
     * @throws IOException If an error occurs during reading the values of the inputStream
     */
    public byte[] toByteArray(InputStream inputStream) throws IOException {
        return IOUtils.toByteArray(inputStream);
    }

}
