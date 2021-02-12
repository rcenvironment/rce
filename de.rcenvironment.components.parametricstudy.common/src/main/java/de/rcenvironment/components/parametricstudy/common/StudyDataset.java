/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.components.parametricstudy.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Class holding one set of values for a study case.
 * @author Christian Weiss
 */
public class StudyDataset implements Serializable {

    private static final long serialVersionUID = -5549958046464158432L;

    private final Map<String, Serializable> values = new HashMap<String, Serializable>();

    public StudyDataset(final Map<String, Serializable> values) {
        this.values.putAll(values);
    }

    /**
     * @param key the key of the value to get.
     * @return the value.
     */
    public Serializable getValue(final String key) {
        return values.get(key);
    }

    /**
     * @param <T> type super class.
     * @param key the key of the value to get.
     * @param clazz type of value.
     * @return the value.
     * @throws ClassCastException if cast fails.
     */
    public <T extends Serializable> T getValue(final String key, Class<T> clazz) throws ClassCastException {
        final Serializable value = values.get(key);
        
        if (value != null && !clazz.isAssignableFrom(value.getClass())) {
            throw new ClassCastException();
        }
        if (value != null) {
            return clazz.cast(value);
        } else {
            return null;
        }
    }

    public Map<String, Serializable> getValues() {
        return values;
    }

}
