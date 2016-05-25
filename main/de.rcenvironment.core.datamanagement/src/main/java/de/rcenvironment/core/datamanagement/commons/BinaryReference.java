/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.commons;

import java.io.Serializable;

import de.rcenvironment.core.datamodel.api.CompressionFormat;

/**
 * Identifier for binary references.
 * 
 * @author Jan Flink
 */
public class BinaryReference implements Serializable {

    private static final long serialVersionUID = -90630053035161867L;

    private final String binaryReferenceKey;

    private final CompressionFormat compression;

    private final String revision;

    public BinaryReference(String binaryReferenceKey, CompressionFormat compression, String revision) {
        this.binaryReferenceKey = binaryReferenceKey;
        this.compression = compression;
        this.revision = revision;
    }

    public String getBinaryReferenceKey() {
        return binaryReferenceKey;
    }

    public CompressionFormat getCompression() {
        return compression;
    }

    public String getRevision() {
        return revision;
    }

}
