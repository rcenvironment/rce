/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.configuration.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.component.model.configuration.impl.ConfigurationDefinitionImpl;

/**
 * Test case for {@link ConfigurationDescription}.
 * 
 * @author Doreen Seider
 */
public class ConfigurationDescriptionTest {

    private ConfigurationDescription description;

    /**
     * Set up.
     * 
     * @throws IOException on error
     */
    @Before
    public void setUp() throws IOException {
        description = ConfigurationDescriptionStubFactory.createConfigurationDescriptionFromTestFiles();
    }

    /** Test. */
    @Test
    public void testConfigurationDescription() {

        ConfigurationDefinition configDef = description.getActiveConfigurationDefinition();
        Set<String> keys = configDef.getConfigurationKeys();

        // 4 (normal) because no activation filter matches
        assertEquals(4, keys.size());

        // 3 (normal) + 1 (ext1) + 1 (ext2) - config def keys with default value are represented
        // here
        assertEquals(5, description.getConfiguration().size());

        configDef = description.getActiveConfigurationDefinition();
        keys = configDef.getConfigurationKeys();

        assertEquals(4, keys.size());

        description.setConfigurationValue(ConfigurationDescriptionStubFactory.HOST, "localhost");

        assertEquals(6, description.getConfiguration().size());

        configDef = description.getActiveConfigurationDefinition();
        keys = configDef.getConfigurationKeys();

        assertEquals(5, keys.size());

        description.setConfigurationValue(ConfigurationDescriptionStubFactory.PORT, String.valueOf(8));

        configDef = description.getActiveConfigurationDefinition();
        keys = configDef.getConfigurationKeys();

        assertEquals(6, keys.size());

        description.setPlaceholderValue(ConfigurationDescriptionStubFactory.USER, ConfigurationDescriptionStubFactory.NAME);
        assertEquals("${user}", description.getActualConfigurationValue(ConfigurationDescriptionStubFactory.AUTH_USER));
        assertEquals(ConfigurationDescriptionStubFactory.NAME, description.getConfigurationValue(
            ConfigurationDescriptionStubFactory.AUTH_USER));

        try {
            description.setPlaceholderValue(ConfigurationDescriptionStubFactory.PHRASE, new String(
                new Base64().encode("password".getBytes("UTF-8"))));
        } catch (UnsupportedEncodingException e) {
            fail();
        }
        assertEquals("${*.phrase}", description.getActualConfigurationValue(ConfigurationDescriptionStubFactory.AUTH_PHRASE));
        assertEquals("password", description.getConfigurationValue(ConfigurationDescriptionStubFactory.AUTH_PHRASE));

        assertEquals(2, description.getPlaceholders().size());

        assertEquals(6, description.getConfiguration().size());

        assertTrue(description.isPlaceholderSet(ConfigurationDescriptionStubFactory.AUTH_USER));
        assertFalse(description.isPlaceholderSet(ConfigurationDescriptionStubFactory.HOST));

        Map<String, String> placeholders = new HashMap<String, String>();
        placeholders.put(ConfigurationDescriptionStubFactory.DIR, "/home");
        description.setPlaceholders(placeholders);

        assertEquals("/home", description.getConfigurationValue(ConfigurationDescriptionStubFactory.WORK_DIR));

        Map<String, String> configuration = new HashMap<String, String>();
        configuration.put(ConfigurationDescriptionStubFactory.AUTH_USER, ConfigurationDescriptionStubFactory.NAME);

        description.setConfiguration(configuration);
        assertEquals(1, description.getConfiguration().size());

        configDef = description.getActiveConfigurationDefinition();
        assertEquals(4, configDef.getConfigurationKeys().size());

        assertFalse(description.isPlaceholderSet(ConfigurationDescriptionStubFactory.AUTH_USER));
    }

    /** Test. */
    @Test
    public void testConfigurationDefinition() {

        ConfigurationDefinition declDescription = description.getComponentConfigurationDefinition();

        // 4 (normal) + 1 (ext1) + 1 (ext2)
        Set<String> keys = declDescription.getConfigurationKeys();

        assertEquals(6, keys.size());

        assertTrue(keys.contains(ConfigurationDescriptionStubFactory.HOST));
        assertTrue(keys.contains(ConfigurationDescriptionStubFactory.PORT));
        assertTrue(keys.contains(ConfigurationDescriptionStubFactory.AUTH_USER));
        assertTrue(keys.contains(ConfigurationDescriptionStubFactory.AUTH_PHRASE));

        assertTrue(keys.contains(ConfigurationDescriptionStubFactory.WORK_DIR));

        assertTrue(keys.contains("format"));

        assertEquals(String.valueOf(7), declDescription.getDefaultValue(ConfigurationDescriptionStubFactory.PORT));
    }

    /** Test. */
    @Test
    public void testPlaceholderMetaDataDefinition() {

        ConfigurationDefinition configDef = description.getComponentConfigurationDefinition();

        PlaceholdersMetaDataDefinition placeholderDef = configDef.getPlaceholderMetaDataDefinition();

        assertEquals("SSH username", placeholderDef.getGuiName(ConfigurationDescriptionStubFactory.USER));
        assertEquals(1, placeholderDef.getGuiPosition(ConfigurationDescriptionStubFactory.DIR));
        assertNull(placeholderDef.getDataType(ConfigurationDescriptionStubFactory.PHRASE));
        assertEquals(PlaceholdersMetaDataConstants.TYPE_DIR, placeholderDef.getDataType(ConfigurationDescriptionStubFactory.DIR));
        assertTrue(placeholderDef.decode(ConfigurationDescriptionStubFactory.PHRASE));
        assertFalse(placeholderDef.decode(ConfigurationDescriptionStubFactory.DIR));

        assertEquals(null, placeholderDef.getGuiName(ConfigurationDescriptionStubFactory.UNKNOWN));
        assertNull(placeholderDef.getDataType(ConfigurationDescriptionStubFactory.UNKNOWN));
        assertFalse(placeholderDef.decode(ConfigurationDescriptionStubFactory.UNKNOWN));

    }

    /** Test. */
    @Test
    public void testReadOnlyConfiguration() {

        String key1 = "key1";
        String value1 = "value1";
        Map<String, String> readOnlyConfig = new HashMap<String, String>();
        readOnlyConfig.put(key1, value1);

        ConfigurationDefinition configDef = description.getComponentConfigurationDefinition();
        ((ConfigurationDefinitionImpl) configDef).setRawReadOnlyConfiguration(readOnlyConfig);

        assertEquals(value1, configDef.getReadOnlyConfiguration().getValue(key1));

        ConfigurationDescription configDesc = new ConfigurationDescription(configDef, new HashSet<ConfigurationExtensionDefinition>());

        assertFalse(value1.equals(configDesc.getConfigurationValue(key1)));

        assertFalse(value1.equals(configDesc.getConfiguration().get(key1)));

        ReadOnlyConfiguration readOnlyConfiguration = configDef.getReadOnlyConfiguration();

        assertTrue(value1.equals(readOnlyConfiguration.getValue(key1)));

        assertTrue(value1.equals(readOnlyConfiguration.getConfiguration().get(key1)));

    }

}
