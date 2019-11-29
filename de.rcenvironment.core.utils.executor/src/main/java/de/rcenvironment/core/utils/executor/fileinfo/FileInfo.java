/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.utils.executor.fileinfo;

import java.io.Serializable;
import java.util.Date;

/**
 * Encapsulates information about a file.
 * @author Christian Weiss
 */
public class FileInfo implements Serializable {

    private static final long serialVersionUID = 6891766153373941656L;

    private static final char SEPARATOR = '/';

    private final boolean isFile;

    private final String relativePath;

    private final Date modificationDate;

    private final long size;

    public FileInfo(final boolean isFile, final String relativePath, final Date modificationDate, final long size) {
        this.isFile = isFile;
        this.relativePath = relativePath;
        this.modificationDate = modificationDate;
        this.size = size;
    }

    public boolean isFile() {
        return isFile;
    }

    public boolean isDirectory() {
        return !isFile();
    }

    public String getAbsolutePath() {
        return relativePath;
    }

    public Date getModificationDate() {
        return modificationDate;
    }

    public long getSize() {
        return size;
    }

    public String getName() {
        return relativePath.substring(relativePath.lastIndexOf(SEPARATOR) + 1);
    }

}
