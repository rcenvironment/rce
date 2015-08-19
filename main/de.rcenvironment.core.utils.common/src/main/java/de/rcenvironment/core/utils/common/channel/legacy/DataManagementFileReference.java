/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.common.channel.legacy;

import java.io.Serializable;


/**
 * A uuid for data management file references to be transferred between components.
 *
 * @author Arne Bachmann. Markus Kunde
 */
@Deprecated
public final class DataManagementFileReference implements Serializable {
    
    private static final long serialVersionUID = 6389026110038812502L;
    
    private final String reference;

    private final String name;

    /**
     * Deprecated - use constructor {@link #DataManagementFileReference(String, String)} instead to
     * have reasonable string representation via {@link #toString()}.
     * 
     * @param reference the reference
     */
    @Deprecated
    public DataManagementFileReference(final String reference) {
        this(reference, reference);
    }

    public DataManagementFileReference(final String reference, final String name) {
        this.reference = reference;
        this.name = name;
    }

    @Override
    public String toString() {        
        /*
         * At this point reference should be returned because in datamanagement (and other points as well)
         * mostly "object" or "serializable" is used and obj.toString() is the common method for getting 
         * the dataholding information.
         * If DataManagementFileReference is directly used as an object, try to avoid to use this method.
         */
        return reference;
    }
    
    /**
     * Returns name of file reference (file name).
     * 
     * @return name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns reference (UUID) of file reference.
     * 
     * @return reference
     */
    public String getReference() {
        return reference;
    }
    
    @Override
    public int hashCode() {
        return reference.hashCode();
    }
    
    @Override
    public boolean equals(final Object other) {
        if (other instanceof DataManagementFileReference) {
            return reference.equals(other.toString());
        }
        throw new IllegalArgumentException("equals() must be called with an argument of type "
            + DataManagementFileReference.class.getName());
    }

}
