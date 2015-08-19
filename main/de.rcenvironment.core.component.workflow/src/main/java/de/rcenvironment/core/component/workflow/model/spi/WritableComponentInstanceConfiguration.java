/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.model.spi;

import java.io.Serializable;

/**
 * Provides write access to the configuration-time setup of component instances.
 * 
 * @author Robert Mischke
 * @author Doreen Seider
 */
public interface WritableComponentInstanceConfiguration {

    /**
     * Sets the id of the property map which should be the current one.
     * @param propertyMapId id of the map.
     */
    void setPropertyMapId(String propertyMapId);
    
    /**
     * Adds a new property map with given key. Entries are gathered from existing property map
     * given by its id. If no id is given the default values are set.
     * 
     * @param newPropertyMapId id of the new property map.
     * @param clonePropertyMapId id of the property map to use its values from. <code>null</code>
     *        for using default values.
     */
    void addPropertyMap(String newPropertyMapId, String clonePropertyMapId);
    
    /**
     * Removes property map given by its id.
     * @param propertyMapId id of map to remove.
     */
    void removePropertyMap(String propertyMapId);
    
    /**
     * Sets a new value for the given property.
     * 
     * @param key The key of the property.
     * @param value The new value of the property.
     * @param <T> any {@link Object} that extends {@link Serializable}.
     */
    <T extends Serializable> void setProperty(String key, T value);
    
    /**
     * Adds a new dynamic input.
     * 
     * @param name The name of the input.
     * @param type The type (class) of the input.
     * 
     * @throws IllegalArgumentException if the given name collides with an existing input.
     */
    void addInput(String name, String type) throws IllegalArgumentException;

    /**
     * Removes a dynamic input.
     * 
     * @param name The name of the input to remove.
     */
    void removeInput(String name);

    /**
     * Changes the definition of an existing dynamic input.
     * 
     * @param name The name of the input to change.
     * @param newName The new name of the input.
     * @param newType The new type of the input.
     */
    void changeInput(String name, String newName, String newType);
    
    /**
     * @param inputName the name of the affected {@link Input}.
     * @param metaDataKey the meta data key to set.
     * @param metaDataValue the meta data value to set.
     */
    void setInputMetaData(String inputName, String metaDataKey, Serializable metaDataValue);
    
    /**
     * Adds a new dynamic output.
     * 
     * @param name The name of the output.
     * @param type The type (class) of the output.
     * 
     * @throws IllegalArgumentException if the given name collides with an existing output.
     */
    void addOutput(String name, String type) throws IllegalArgumentException;

    /**
     * Removes a dynamic output.
     * 
     * @param name The name of the output to remove.
     */
    void removeOutput(String name);

    /**
     * Changes the definition of an existing dynamic output.
     * 
     * @param name The name of the output to change.
     * @param newName The new name of the output.
     * @param newType The new type of the output.
     */
    void changeOutput(String name, String newName, String newType);
    
    /**
     * @param outputName the name of the affected {@link Output}.
     * @param metaDataKey the meta data key to set.
     * @param metaDataValue the meta data value to set.
     */
    void setOutputMetaData(String outputName, String metaDataKey, Serializable metaDataValue);
}
