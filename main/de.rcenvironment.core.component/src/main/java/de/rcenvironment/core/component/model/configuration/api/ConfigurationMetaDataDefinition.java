/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.configuration.api;


/**
 * Describes meta data information for configuration options.
 * 
 * @author Doreen Seider
 */
public interface ConfigurationMetaDataDefinition {
    
    /**
     * @param key configuration key
     * @return its GUI name
     */
    String getGuiName(String key);
    
    /**
     * @param key configuration key
     * @return its GUI group name
     */
    String getGuiGroupName(String key);

    /**
     * @param key configuration key
     * @return its GUI position or -1 if no position is defined
     */
    int getGuiPosition(String key);
    
    /**
     * @param key configuration key
     * @param metaDataKey meta data key
     * @return meta data value or <code>null</code> if no value is defined for given key
     */
    String getMetaDataValue(String key, String metaDataKey);

}
