/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration;

import java.util.List;
import java.util.Map;

/**
 * A simple key-value store to persist settings of an RCE platform.
 *
 * @author Robert Mischke
 * @author Sascha Zur
 */
public interface PersistentSettingsService {

    /**
     * Stores a key-value pair. If the key already exists, the old value is overwritten.
     * This method uses the standard RCE persistent settings file.
     * 
     * @param key a non-empty, non-null string that serves as an identifier; TODO define maximum
     *        length and allowed charset
     * @param value the content for the given key in string form; if necessary, this data type may
     *        be relaxed to "Serializable" in the future, although storing structured data as JSON
     *        or XML is preferred
     */
    void saveStringValue(String key, String value);

    /**
     * Retrieves the value for a given key. If the key does not exist, null is returned.
     * This method uses the standard RCE persistent settings file.
     * 
     * @param key a non-empty, non-null string that serves as an identifier; TODO define maximum
     *        length and allowed charset
     * @return the data that was stored for this key, or null if the key does not exist
     */
    String readStringValue(String key);
    /**
     * Stores a key-value pair. If the key already exists, the old value is overwritten.
     * This method uses the given persistent settings file.     * 
     * @param key a non-empty, non-null string that serves as an identifier; TODO define maximum
     *        length and allowed charset
     * @param value the content for the given key in string form; if necessary, this data type may
     *        be relaxed to "Serializable" in the future, although storing structured data as JSON
     *        or XML is preferred
     * @param filename of the file the value will be stored in
     */
    void saveStringValue(String key, String value, String filename);

    /**
     * Retrieves the value for a given key. If the key does not exist, null is returned.
     * This method uses the given persistent settings file.
     * 
     * @param key a non-empty, non-null string that serves as an identifier; TODO define maximum
     *        length and allowed charset
     * @param filename of the file the value will be read from
     * @return the data that was stored for this key, or null if the key does not exist
     */
    String readStringValue(String key, String filename);
   
    /**
     * Deletes a key and its associated data.
     * This method uses the standard RCE persistent settings file.
     * @param key the key to delete; passing null is not allowed
     */
    void delete(String key);
    
    /**
     * Deletes a key and its associated data.
     * This method uses the given persistent settings file.
     * 
     * @param key the key to delete; passing null is not allowed
     * @param filename the data to delete is stored in; passing null is not allowed
     */
    void delete(String key, String filename);
    
    /**
     * Loads a map containing a List of Strings from the given file. It has to be in JSON format.
     * 
     * @param filename : The file to load the Map from.
     * @return The Map with a List of Strings
     */
    Map<String, List<String>> readMapWithStringList(String filename);
    
    /**
     * The given map to the given filename in JSON format.
     * 
     * @param map : to save
     * @param filename : where to save
     */
    void saveMapWithStringList(Map<String, List<String>> map, String filename);
}
