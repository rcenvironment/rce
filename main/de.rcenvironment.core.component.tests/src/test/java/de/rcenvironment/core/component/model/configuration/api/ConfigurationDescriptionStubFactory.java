/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.model.configuration.api;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.model.configuration.impl.ConfigurationDefinitionImpl;
import de.rcenvironment.core.component.model.configuration.impl.ConfigurationExtensionDefinitionImpl;

/**
 * Creates stub implementations of {@link ConfigurationDescription}.
 * 
 * @author Doreen Seider
 */
public final class ConfigurationDescriptionStubFactory {

    /** Constant. */
    public static final String WORK_DIR = "workDir";

    /** Constant. */
    public static final String HOST = "host";

    /** Constant. */
    public static final String NAME = "name";

    /** Constant. */
    public static final String AUTH_USER = "authUser";

    /** Constant. */
    public static final String AUTH_PHRASE = "authPhrase";

    /** Constant. */
    public static final String UNKNOWN = "unknown";

    /** Constant. */
    public static final String PHRASE = "phrase";

    /** Constant. */
    public static final String USER = "user";

    /** Constant. */
    public static final String DIR = "dir";

    /** Constant. */
    public static final String PORT = "port";
    
    /** Constant. */
    public static final String CONFIGURATION_JSON = "/configuration.json";
    
    /** Constant. */
    public static final String CONFIGURATION_EXT1_JSON = "/configuration_ext1.json";
    
    /** Constant. */
    public static final String CONFIGURATION_EXT2_JSON = "/configuration_ext2.json";
    
    private ConfigurationDescriptionStubFactory() {}
    
    /**
     * Creates {@link ConfigurationDescription} object from test files.
     * @return new {@link ConfigurationDescription} object
     * @throws IOException on error
     */
    public static ConfigurationDescription createConfigurationDescriptionFromTestFiles() throws IOException {
        return new ConfigurationDescription((ConfigurationDefinition) createConfigurationDefinitionFromTestFile(),
            new HashSet<ConfigurationExtensionDefinition>(createConfigurationExtensionDefinitionsFromTestFiles()));
    }
    
    /**
     * Creates {@link ConfigurationDefinitionImpl} object from test files including extended configuration.
     * @return new {@link ConfigurationDefinitionImpl} object
     * @throws IOException on error
     */
    public static ConfigurationDefinitionImpl createCombinedConfigurationDefinitionFromTestFiles() throws IOException {
        Set<ConfigurationDefinition> configDefs = new HashSet<ConfigurationDefinition>();
        configDefs.add(createConfigurationDefinitionFromTestFile());
        configDefs.addAll(createConfigurationExtensionDefinitionsFromTestFiles());
        
        ConfigurationDefinitionImpl combindedConfigDef = new ConfigurationDefinitionImpl();
        combindedConfigDef.setConfigurationDefinitions(configDefs);
        return combindedConfigDef;
    }
    
    /**
     * Creates {@link ConfigurationDefinitionImpl} object from test files excluding extended configuration.
     * @return new {@link ConfigurationDefinitionImpl} object
     * @throws IOException on error
     */
    public static ConfigurationDefinitionImpl createConfigurationDefinitionFromTestFile() throws IOException {
        return ComponentUtils.extractConfigurationDescription(
            ConfigurationDescriptionStubFactory.class.getResourceAsStream(CONFIGURATION_JSON),
            ConfigurationDescriptionStubFactory.class.getResourceAsStream(CONFIGURATION_JSON),
            ConfigurationDescriptionStubFactory.class.getResourceAsStream(CONFIGURATION_JSON));
    }
    
    /**
     * Creates {@link ConfigurationExtensionDefinitionImpl} set from test files.
     * @return new {@link ConfigurationExtensionDefinitionImpl} set
     * @throws IOException on error
     */
    public static Set<ConfigurationExtensionDefinitionImpl> createConfigurationExtensionDefinitionsFromTestFiles() throws IOException {
        
        ConfigurationExtensionDefinitionImpl configExtDef1 = ComponentUtils.extractConfigurationExtensionDescription(
            ConfigurationDescriptionStubFactory.class.getResourceAsStream(CONFIGURATION_EXT1_JSON),
            ConfigurationDescriptionStubFactory.class.getResourceAsStream(CONFIGURATION_EXT1_JSON),
            ConfigurationDescriptionStubFactory.class.getResourceAsStream(CONFIGURATION_EXT1_JSON));
        ConfigurationExtensionDefinitionImpl configExtDef2 = ComponentUtils.extractConfigurationExtensionDescription(
            ConfigurationDescriptionStubFactory.class.getResourceAsStream(CONFIGURATION_EXT2_JSON),
            ConfigurationDescriptionStubFactory.class.getResourceAsStream(CONFIGURATION_EXT2_JSON),
            ConfigurationDescriptionStubFactory.class.getResourceAsStream(CONFIGURATION_EXT2_JSON));
        
        Set<ConfigurationExtensionDefinitionImpl> configExtDefs = new HashSet<ConfigurationExtensionDefinitionImpl>();
        configExtDefs.add(configExtDef1);
        configExtDefs.add(configExtDef2);
        
        return configExtDefs;
    }
}
