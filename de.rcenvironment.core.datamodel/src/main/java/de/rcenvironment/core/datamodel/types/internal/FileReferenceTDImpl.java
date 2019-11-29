/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.datamodel.types.internal;

import java.util.Date;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;

/**
 * Implementation of {@link FileReferenceTD}.
 * 
 * @author Doreen Seider
 */
public class FileReferenceTDImpl extends AbstractTypedDatum implements FileReferenceTD {

    private final String fileReference;
    
    private final String fileName;
    
    private long fileSize;
    
    private Date lastModified;
    
    public FileReferenceTDImpl(String fileReference, String filename) {
        super(DataType.FileReference);
        this.fileReference = fileReference;
        this.fileName = filename;
    }

    @Override
    public String getFileReference() {
        return fileReference;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public long getFileSizeInBytes() {
        return fileSize;
    }

    @Override
    public Date getLastModified() {
        return lastModified;
    }

    @Override
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    @Override
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public String toString() {
        return getFileName();
    }

    
}
