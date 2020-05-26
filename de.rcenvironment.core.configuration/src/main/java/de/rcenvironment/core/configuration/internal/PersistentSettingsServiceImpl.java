/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.internal;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.configuration.PersistentSettingsService;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * Implementation of simple key-value store to persist settings of an RCE platform.
 * 
 * @author Sascha Zur
 * @author Robert Mischke
 */
public class PersistentSettingsServiceImpl implements PersistentSettingsService {

    private static final String STORAGE_FILENAME = "settings.json";

    private static final String ERROR_MSG_SAVE = "Could not save persistent settings: ";

    private static final String ERROR_MSG_LOAD = "Could not find persistent settings file. It will be created: ";

    private static final Log LOGGER = LogFactory.getLog(PersistentSettingsServiceImpl.class);

    private ConfigurationService configurationService;

    private File storageDirectory;

    private Map<String, String> store;

    private Set<String> alreadyBackupedFiles = new HashSet<>();

    @Override
    public synchronized void saveStringValue(String key, String value) {
        saveStringValue(key, value, STORAGE_FILENAME);
    }

    private void saveStore(String filename) {
        saveAnyMapInGivenStore(store, filename);
    }

    @Override
    public synchronized String readStringValue(String key) {
        return readStringValue(key, STORAGE_FILENAME);
    }

    private Map<String, String> readStore(String filename) {
        Map<String, String> result = new HashMap<String, String>();
        File storeFile = new File(storageDirectory, filename);
        if (!storeFile.exists()) {
            try {
                storeFile.createNewFile();
            } catch (IOException e) {
                LOGGER.warn("Could not create new persistent settings file: ", e);
            }
        }
        JsonFactory f = new JsonFactory();
        JsonParser jp;
        try {
            jp = f.createParser(new File(storageDirectory, filename));
            jp.nextToken();
            while (jp.hasCurrentToken() && jp.nextToken() != JsonToken.END_OBJECT) {
                String fieldname = jp.getCurrentName();
                if (jp.nextToken() == JsonToken.END_OBJECT) {
                    break;
                } else {
                    result.put(fieldname, jp.getText());
                }
            }
            jp.close();
        } catch (IOException e) {
            result = null;
            // TODO review this error case
            LOGGER.warn(ERROR_MSG_LOAD + "(This is normal if RCE is starting for the first time) :", e);
        }

        return result;
    }

    @Override
    public synchronized void saveStringValue(String key, String value, String filename) {
        if (store != null) {
            store.put(key, value);

        } else {
            store = readStore(filename);
            if (store == null) {
                store = new HashMap<String, String>();
            }
            store.put(key, value);
        }
        saveStore(filename);

    }

    @Override
    public synchronized String readStringValue(String key, String filename) {
        store = readStore(filename);
        if (store != null && store.containsKey(key)) {
            return store.get(key);
        }
        return null;
    }

    @Override
    public synchronized void delete(String key) {
        delete(key, STORAGE_FILENAME);
    }

    @Override
    public synchronized void delete(String key, String filename) {
        store = readStore(filename);
        if (store != null) {
            store.remove(key);
        }
        saveStore(filename);

    }

    protected void activate(BundleContext context) {
        if (storageDirectory == null) {
            // TODO use File object instead of String where possible
            storageDirectory = configurationService.getConfigurablePath(ConfigurablePathId.PROFILE_INTERNAL_DATA);
            if (!storageDirectory.exists()) {
                throw new RuntimeException("Unexpected state: Persistent settings path should exist at this point: "
                    + storageDirectory);
            }
        }
        alreadyBackupedFiles = new HashSet<>();
    }

    @Override
    public synchronized Map<String, List<String>> readMapWithStringList(String filename) {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        File f = null;
        f = new File(storageDirectory, filename);
        if (!f.exists()) {
            f.getParentFile().mkdirs();
        }
        Map<String, List<String>> result = new HashMap<String, List<String>>();
        if (f.exists()) {
            try {
                result = mapper.readValue(f, new TypeReference<Map<String, List<String>>>() {
                });
            } catch (IOException e) {
                LOGGER.error(ERROR_MSG_SAVE, e);
            }
        }
        return result;
    }

    @Override
    public synchronized void saveMapWithStringList(Map<String, List<String>> map, String filename) {
        saveAnyMapInGivenStore(map, filename);
    }

    protected void bindConfigurationService(ConfigurationService newConfigurationService) {
        configurationService = newConfigurationService;
    }

    // for unit testing
    protected void setStorageDirectory(String path) {
        this.storageDirectory = new File(path);
    }

    private void saveAnyMapInGivenStore(@SuppressWarnings("rawtypes") Map content, String filenameOfStore) {
        File f = new File(storageDirectory, filenameOfStore);
        if (!f.exists()) {
            f.getParentFile().mkdirs();
        }
        if (f.exists()) {
            try {
                File backupFile = new File(storageDirectory, filenameOfStore + ".bak");
                if (!alreadyBackupedFiles.contains(f.getAbsolutePath())) {
                    FileUtils.copyFile(f, backupFile);
                    alreadyBackupedFiles.add(f.getAbsolutePath());
                }
            } catch (IOException e) {
                LOGGER.warn("PersistentSettingsService: Could not rename storage file to backup file", e);
            }
        }

        // write JSON to a file
        try {
            JsonGenerator jsonGenerator = new JsonFactory().createGenerator(f, JsonEncoding.UTF8);
            JsonUtils.getDefaultObjectMapper().writeValue(jsonGenerator, content);
        } catch (IOException e) {
            // TODO >6.0.0: this should probably do more than just log a message
            LOGGER.error(ERROR_MSG_SAVE, e);
        }
    }
}
