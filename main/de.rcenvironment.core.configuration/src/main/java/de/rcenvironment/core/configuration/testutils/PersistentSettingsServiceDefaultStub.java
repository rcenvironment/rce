/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.testutils;

import java.util.List;
import java.util.Map;

import de.rcenvironment.core.configuration.PersistentSettingsService;

/**
 * Default test stub for PersistentSettingsService. Returns the Java default field values for all
 * methods with a return value.
 * 
 * @author Robert Mischke
 */
public class PersistentSettingsServiceDefaultStub implements PersistentSettingsService {

    @Override
    public void saveStringValue(String key, String value) {}

    @Override
    public String readStringValue(String key) {
        return null;
    }

    @Override
    public void delete(String key) {}

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.configuration.PersistentSettingsService#readMapWithStringList(java.lang.String)
     */
    @Override
    public Map<String, List<String>> readMapWithStringList(String type) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.configuration.PersistentSettingsService#saveMapWithStringList(java.util.Map, java.lang.String)
     */
    @Override
    public void saveMapWithStringList(Map<String, List<String>> map, String filename) {
    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.configuration.PersistentSettingsService#saveStringValue(
     *          java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void saveStringValue(String key, String value, String filename) {
        
    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.configuration.PersistentSettingsService#readStringValue(java.lang.String, java.lang.String)
     */
    @Override
    public String readStringValue(String key, String filename) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.configuration.PersistentSettingsService#delete(java.lang.String, java.lang.String)
     */
    @Override
    public void delete(String key, String filename) {
        
    }

}
