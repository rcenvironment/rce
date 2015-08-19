/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.configuration.api;


/**
 * Describes meta data of configuration placeholder.
 * 
 * @author Doreen Seider
 */
public interface PlaceholdersMetaDataDefinition {
    
    /**
     * @param key of placeholder
     * @return its gui name
     */
    String getGuiName(String key);

    /**
     * @param key of placeholder
     * @return its gui position or -1 if no position is defined
     */
    int getGuiPosition(String key);

    /**
     * @param key meta data key
     * @return data type of given meta datum
     */
    String getDataType(String key);

    /**
     * @param key of placeholder
     * @return <code>true</code> if it must be encrypted, otherwise <code>false</code>
     */
    boolean decode(String key);

}
