/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.toolkit.utils.common;

import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.toolkit.modules.concurrency.utils.ThreadsafeAutoCreationMap;

/**
 * A map that creates requested entries on demand if they don't exist yet. Note that this class is not thread-safe; use
 * {@link ThreadsafeAutoCreationMap} if this is required.
 * 
 * @param <K> the key type
 * @param <V> the value type
 * 
 * @author Robert Mischke
 */
public abstract class AutoCreationMap<K, V> {

    private final Map<K, V> innerMap = new HashMap<K, V>();

    /**
     * Similar to the standard {@link Map#get(Object)}, but with the addition that if no entry exists yet, one is created by the subclass
     * implementation of {@link #createNewEntry(Object)}.
     * 
     * @param key the map key
     * @return the retrieved value
     */
    public V get(K key) {
        V value = innerMap.get(key);
        if (value == null) {
            value = createNewEntry(key);
            innerMap.put(key, value);
        }
        return value;
    }

    /**
     * @see Map#remove(Object)
     * 
     * @param key the entry to delete
     */
    public void remove(String key) {
        innerMap.remove(key);
    }

    /**
     * @see Map#clear()
     */
    public void clear() {
        innerMap.clear();
    }

    /**
     * Creates a shallow copy of the internal, synchronized map. Note that the entries themselves are not synchronized or cloned in any
     * form, so they may be subject to concurrent changes from calls to the producing {@link AutoCreationMap}.
     * 
     * @return a detached map with all key-value pairs
     */
    public Map<K, V> getImmutableShallowCopy() {
        return new HashMap<K, V>(innerMap);
    }

    protected abstract V createNewEntry(K key);

}
