/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.concurrent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A thread-safe map that creates requested entries on demand if they don't exist yet.
 * 
 * A typical use case are counter maps, where for each id, an atomic integer or long holder is
 * created when the key is used for the first time. Subsequent calls to {@link #get(Object)} return
 * this holder, which can then be incremented or queried for the counter value.
 * 
 * @param <K> the key type
 * @param <V> the value type
 * 
 * @author Robert Mischke
 */
public abstract class ThreadsafeAutoCreationMap<K, V> {

    // TODO reduce lock contention?
    private final Map<K, V> innerMap = Collections.synchronizedMap(new HashMap<K, V>());

    /**
     * Similar to the standard {@link Map#get(Object)}, but with the addition that if no entry
     * exists yet, one is created by the subclass implementation of {@link #createNewEntry(Object)}.
     * 
     * @param key the map key
     * @return the retrieved value
     */
    public V get(K key) {
        V value = innerMap.get(key);
        if (value == null) {
            // NOTE: while this looks similar to the double-checked locking anti-pattern,
            // it should be safe as statisticsMap is a synchronizedMap; the synchronized block only
            // serves to prevent race conditions <b>between</b> the already-synchronized calls
            synchronized (innerMap) {
                value = innerMap.get(key);
                if (value == null) {
                    value = createNewEntry(key);
                    innerMap.put(key, value);
                }
            }
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
        // note: explicit synchronization might be redundant here
        synchronized (innerMap) {
            innerMap.clear();
        }
    }

    /**
     * Creates a shallow copy of the internal, synchronized map. Note that the entries themselves
     * are not synchronized or cloned in any form, so they may be subject to concurrent changes from
     * calls to the producing {@link ThreadsafeAutoCreationMap}.
     * 
     * @return a detached map with all key-value pairs
     */
    public Map<K, V> getShallowCopy() {
        synchronized (innerMap) {
            return new HashMap<K, V>(innerMap);
        }
    }

    protected abstract V createNewEntry(K key);

}
