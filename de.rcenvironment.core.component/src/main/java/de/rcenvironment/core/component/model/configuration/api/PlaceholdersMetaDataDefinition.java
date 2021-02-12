/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.model.configuration.api;


/**
 * Describes meta data of configuration placeholder.
 * 
 * @author Doreen Seider
 * 
 * Note: Used to show placeholders with proper display names in the workflow execution wizard. --seid_do
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
