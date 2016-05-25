/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.datamodel.types.internal;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;

/**
 * Implementation of {@link DirectoryReferenceTD}.
 * 
 * @author Doreen Seider
 */
public class DirectoryReferenceTDImpl extends AbstractTypedDatum implements DirectoryReferenceTD {

    private final String directoryReference;
    
    private final String directoryName;
    
    private long directorySize;
    
    public DirectoryReferenceTDImpl(String directoryReference, String directoryname) {
        super(DataType.DirectoryReference);
        this.directoryReference = directoryReference;
        this.directoryName = directoryname;
    }

    @Override
    public String getDirectoryReference() {
        return directoryReference;
    }

    @Override
    public String getDirectoryName() {
        return directoryName;
    }

    @Override
    public long getDirectorySizeInBytes() {
        return directorySize;
    }

    @Override
    public void setDirectorySize(long directorySize) {
        this.directorySize = directorySize;
    }

    @Override
    public String toString() {
        return getDirectoryName();
    }

    
}
