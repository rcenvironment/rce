/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration;

/**
 * Sub-interface of {@link ConfigurationSegment} that adds write/update methods.
 * 
 * @author Robert Mischke
 * @author David Scholz
 */
public interface WritableConfigurationSegment extends ConfigurationSegment {

    /**
     * Adds or replaces a string field.
     * 
     * @param key the field name
     * @param value the new value
     * @throws ConfigurationException if editing the configuration failed
     */
    void setString(String key, String value) throws ConfigurationException;

    /**
     * Adds or replaces a boolean field.
     * 
     * @param key the field name
     * @param value the new value
     * @throws ConfigurationException if editing the configuration failed
     */
    void setBoolean(String key, boolean value) throws ConfigurationException;

    /**
     * 
     * Adds or replaces an integer field.
     * 
     * @param key the field name.
     * @param value the new value.
     * @throws ConfigurationException if editing the configuration failed.
     */
    void setInteger(String key, Integer value) throws ConfigurationException;
    
    /**
     * 
     * Adds or replaces a long field.
     * 
     * @param key the field name.
     * @param value the new value.
     * @throws ConfigurationException if editing the configuration failed.
     */
    void setLong(String key, Long value) throws ConfigurationException;

    // void setFloat(String relativePath, Double value);

    /**
     * Adds or replaces a string array.
     * 
     * @param key the field name
     * @param value the new value
     * @throws ConfigurationException if editing the configuration failed
     */
    void setStringArray(String key, String[] value) throws ConfigurationException;

    /**
     * Creates a new element, which can then be edited.
     * 
     * @param id the new element's id
     * @return a {@link WritableConfigurationSegment} representing the new element
     * @throws ConfigurationException if such an element already exists, or if editing the configuration failed
     */
    WritableConfigurationSegment createElement(String id) throws ConfigurationException;

    /**
     * Deletes the element for the given id. If no such element exists, "false" is returned, otherwise "true".
     * 
     * @param id the new element's id
     * @return whether an element actually existed for the given id
     * @throws ConfigurationException if editing the configuration failed
     */
    boolean deleteElement(String id) throws ConfigurationException;

}
