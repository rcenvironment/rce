/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.utils.executor.fileinfo.internal;

import java.io.IOException;
import java.util.Collection;

import de.rcenvironment.core.utils.executor.CommandLineExecutor;
import de.rcenvironment.core.utils.executor.fileinfo.FileInfo;
import de.rcenvironment.core.utils.executor.fileinfo.FileInfoService;

/**
 * Abstract implementation of {@link FileInfoService} with common functionality.
 * @author Christian Weiss
 */
public abstract class AbstractFileService implements FileInfoService {

    private final CommandLineExecutor commandLineExecutor;

    protected AbstractFileService(final CommandLineExecutor commandLineExecutor) {
        this.commandLineExecutor = commandLineExecutor;
    }

    protected CommandLineExecutor getCommandLineExecutor() {
        return commandLineExecutor;
    }

    @Override
    public Collection<FileInfo> listFiles(final String directory) throws IOException {
        return listFiles(directory, false);
    }

    @Override
    public abstract Collection<FileInfo> listFiles(String directory, boolean recursively) throws IOException;

    @Override
    public Collection<FileInfo> listContent(String directory) throws IOException {
        return listContent(directory, false);
    }

    @Override
    public abstract Collection<FileInfo> listContent(String directory, boolean recursively) throws IOException;

}
