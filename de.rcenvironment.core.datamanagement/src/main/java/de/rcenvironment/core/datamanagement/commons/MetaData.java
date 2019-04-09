/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.commons;

import java.io.Serializable;

import de.rcenvironment.core.utils.incubator.Assertions;

/**
 * Identifier for meta data.
 * 
 * @author Dirk Rossow
 */
public final class MetaData implements Cloneable, Serializable {

    /** Maximal length of keys. */
    public static final int MAX_KEY_LENGTH = 255;

    /** Maximal length of values. */
    public static final int MAX_VALUE_LENGTH = 32000;

    /** External keys may not start with this prefix. */
    // TODO delegated to MetaDataKeys for now; rewrite all references instead
    public static final String PROTECTEDKEYPREFIX = MetaDataKeys.Managed.PROTECTED_KEY_PREFIX;

    /** User who has written the data. Read-only data. */
    public static final MetaData AUTHOR = new MetaData(MetaDataKeys.Managed.AUTHOR, false, true);

    /** Date the data was written. Format: "yyyy-MM-dd HH:mm:ss". Read-only data. */
    public static final MetaData DATE = new MetaData(MetaDataKeys.Managed.DATE, false, true);

    /** Size of data. Read-only data. */
    public static final MetaData SIZE = new MetaData(MetaDataKeys.Managed.SIZE, false, true);

    /**
     * User who has initial created the {@link DataReference}. Read-only data. This is a revision independent meta data.
     */
    public static final MetaData CREATOR = new MetaData(MetaDataKeys.Managed.CREATOR, true, true);

    private static final long serialVersionUID = -8984237820030181855L;

    private final String key;

    /** Used for data management internal meta data. */
    private final boolean isReadOnly;

    /** Constructor for read-only meta data. */
    public MetaData(String key, boolean isRevisionIndependent) {
        this(key, isRevisionIndependent, false);
    }

    public MetaData(String key, boolean isRevisionIndependent, boolean isReadOnly) {
        Assertions.isTrue(key.length() <= MAX_KEY_LENGTH, "key > " + MAX_KEY_LENGTH);

        this.key = key;
        this.isReadOnly = isReadOnly;
    }

    /**
     * @return <code>true</code> if this {@link MetaData} is read only.
     */
    public boolean isReadOnly() {
        return isReadOnly;
    }

    /**
     * Factory method for creating new MetaData instances.
     * 
     * @param key Key to use.
     * @param isRevisionIndependent True creates a revision independent MetaData.
     * @return created MetaData.
     */
    @Deprecated
    public static MetaData create(String key, boolean isRevisionIndependent) {
        Assertions.isDefined(key, "Key must not be null.");
        Assertions.isFalse(key.isEmpty(), "Key must not be empty.");
        if (key.startsWith(PROTECTEDKEYPREFIX)) {
            return new MetaData(key, isRevisionIndependent, true);
        } else {
            return new MetaData(key, isRevisionIndependent, false);
        }
    }

    /**
     * @return key representing this {@link MetaData}.
     */
    public String getKey() {
        return key;
    }

    @Override
    public int hashCode() {
        final int c1231 = 1231;
        final int c1237 = 1237;
        final int prime = 31;
        int result = 1;
        if (isReadOnly) {
            result = prime * result + c1231;
        } else {
            result = prime * result + c1237;
        }
        if (key == null) {
            result = prime * result;
        } else {
            result = prime * result + key.hashCode();
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MetaData other = (MetaData) obj;
        if (isReadOnly != other.isReadOnly) {
            return false;
        }
        if (key == null) {
            if (other.key != null) {
                return false;
            }
        } else if (!key.equals(other.key)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return getKey();
    }

}
