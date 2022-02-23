/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.workflow.execution.function.internal;

import java.io.File;
import java.io.IOException;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import de.rcenvironment.core.utils.common.TempFileService;

class FileUtils {
    private BiFunction<File, String, File> createFile;

    private Supplier<ObjectWriter> createObjectWriter;

    private Supplier<ObjectMapper> createObjectMapper;
    
    private TempFileService tempFileService;
    
    public File createFile(File parent, String name) {
        return createFile.apply(parent, name);
    }

    public ObjectWriter getObjectWriter() {
        return createObjectWriter.get();
    }
    
    public ObjectMapper getObjectMapper() {
        return createObjectMapper.get();
    }
    
    /**
     * Creates a temporary directory of the given name. This directory is automatically removed upon program termination, so the caller does
     * not need to implement cleanup of the returned file.
     */
    public File createTempDir(final String name) throws IOException {
        return this.tempFileService.createManagedTempDir(name);
    }
    
    public void setCreateFile(final BiFunction<File, String, File> createFile) {
        this.createFile = createFile;
    }

    
    public void setCreateObjectWriter(final Supplier<ObjectWriter> createObjectWriter) {
        this.createObjectWriter = createObjectWriter;
    }

    
    public void setCreateObjectMapper(final Supplier<ObjectMapper> createObjectMapper) {
        this.createObjectMapper = createObjectMapper;
    }
    
    public void setTempFileService(final TempFileService service) {
        this.tempFileService = service;
    }
}
