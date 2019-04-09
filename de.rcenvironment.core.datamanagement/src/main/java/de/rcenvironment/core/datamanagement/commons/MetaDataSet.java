/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.commons;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Container class for {@link MetaData} associated with a {@link Revision} of a {@link DataReference}.
 * 
 * @author Dirk Rossow
 */
public class MetaDataSet implements Cloneable, Serializable {

    private static final long serialVersionUID = 8868578666276625820L;

    private Map<MetaData, String> values = null;

    public MetaDataSet() {
        values = new HashMap<MetaData, String>();
    }

    /**
     * @param metaData
     *            {@link MetaData} for which the value should be retrieved.
     * @return the value of given {@link MetaData} or <code>null</code> if it does not exist.
     */
    public String getValue(MetaData metaData) {
        return values.get(metaData);
    }

    /**
     * Sets a value of a MetaData in this set and returns if exists the old value.
     * 
     * @param metaData
     *            {@link MetaData} to set.
     * @param newValue
     *            New value to set.
     */
    public void setValue(MetaData metaData, String newValue) {
        values.put(metaData, newValue);
    }

    /**
     * Removes a {@link MetaData} value.
     * 
     * @param metaData
     *            {@link MetaData} for which the value should be removed.
     */
    public void remove(MetaData metaData) {
        values.remove(metaData);
    }

    /**
     * @return an Iterator which iterates over all {@link MetaData} in this set.
     */
    public Iterator<MetaData> iterator() {
        return values.keySet().iterator();
    }

    /**
     * @return <code>true</code> if {@link MetaDataSet} contains no {@link MetaData} values, <code>false</code>
     *         otherwise.
     */
    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public MetaDataSet clone() {
        MetaDataSet metaDataSet = new MetaDataSet();
        metaDataSet.values.putAll(values);
        return metaDataSet;
    }
    
    @Override
    public int hashCode() {
        return values.hashCode();
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
        final MetaDataSet other = (MetaDataSet) obj;
        if (values == null) {
            if (other.values != null) {
                return false;
            }
        } else if (!values.equals(other.values)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        String result = "";
        for (MetaData metaData : values.keySet()) {
            result += metaData.getKey() + "=" + getValue(metaData);
            result += "  ";
        }
        return result;
    }

}
