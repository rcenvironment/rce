/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import de.rcenvironment.core.configuration.ConfigurationSegment;

/**
 * Default {@link WritableConfigurationSegment} implementation.
 * 
 * @author Robert Mischke
 */
class WritableConfigurationSegmentImpl implements WritableConfigurationSegment {

    private WritableConfigurationSegmentImpl rootSegment;

    // note: may be null in case of a non-existing segment;
    private JsonNode segmentRootNode;

    @SuppressWarnings("unused")
    private final Log log = LogFactory.getLog(getClass());

    public WritableConfigurationSegmentImpl(JsonNode treeRoot) {
        this.segmentRootNode = treeRoot;
        this.rootSegment = this;
    }

    public WritableConfigurationSegmentImpl(JsonNode treeRoot, WritableConfigurationSegmentImpl rootSegment) {
        this.segmentRootNode = treeRoot;
        this.rootSegment = rootSegment;
    }

    @Override
    public ConfigurationSegment getSubSegment(String relativePath) {
        JsonNode treeLocation = navigatePath(relativePath);
        return new WritableConfigurationSegmentImpl(treeLocation, rootSegment);
    }

    @Override
    public boolean isPresentInCurrentConfiguration() {
        return segmentRootNode != null;
    }

    @Override
    public String getString(String relativePath) {
        return getString(relativePath, null);
    }

    @Override
    public String getString(String relativePath, String defaultValue) {
        JsonNode treeLocation = navigatePath(relativePath);
        if (treeLocation != null) {
            return useDefaultValueIfNull(treeLocation.getTextValue(), defaultValue);
        } else {
            return defaultValue;
        }
    }

    @Override
    public Long getLong(String relativePath) {
        return getLong(relativePath, null);
    }

    @Override
    public Long getLong(String relativePath, Long defaultValue) {
        JsonNode treeLocation = navigatePath(relativePath);
        if (treeLocation != null) {
            return useDefaultValueIfNull(treeLocation.getLongValue(), defaultValue);
        } else {
            return defaultValue;
        }
    }

    @Override
    public Integer getInteger(String relativePath) {
        return getInteger(relativePath, null);
    }

    @Override
    public Integer getInteger(String relativePath, Integer defaultValue) {
        JsonNode treeLocation = navigatePath(relativePath);
        if (treeLocation != null) {
            return useDefaultValueIfNull(treeLocation.getIntValue(), defaultValue);
        } else {
            return defaultValue;
        }
    }

    @Override
    public Double getDouble(String relativePath) {
        return getDouble(relativePath, null);
    }

    @Override
    public Double getDouble(String relativePath, Double defaultValue) {
        JsonNode treeLocation = navigatePath(relativePath);
        if (treeLocation != null) {
            return useDefaultValueIfNull(treeLocation.getDoubleValue(), defaultValue);
        } else {
            return defaultValue;
        }
    }

    @Override
    public Boolean getBoolean(String relativePath) {
        return getBoolean(relativePath, null);
    }

    @Override
    public Boolean getBoolean(String relativePath, Boolean defaultValue) {
        JsonNode treeLocation = navigatePath(relativePath);
        if (treeLocation != null) {
            return useDefaultValueIfNull(treeLocation.getBooleanValue(), defaultValue);
        } else {
            return defaultValue;
        }
    }

    @Override
    public Map<String, ConfigurationSegment> listElements(String relativePath) {
        Map<String, ConfigurationSegment> resultMap = new HashMap<>();
        JsonNode treeLocation = navigatePath(relativePath);
        if (treeLocation == null) {
            return new HashMap<>(); // see JavaDoc
        }
        Iterator<Entry<String, JsonNode>> iterator = treeLocation.getFields();
        while (iterator.hasNext()) {
            Entry<String, JsonNode> entry = iterator.next();
            resultMap.put(entry.getKey(), new WritableConfigurationSegmentImpl(entry.getValue(), rootSegment));
        }
        return resultMap;
    }

    @Override
    public <T> T mapToObject(Class<T> clazz) throws IOException {
        try {
            if (segmentRootNode == null) {
                return clazz.newInstance();
            }
            return new ObjectMapper().treeToValue(segmentRootNode, clazz);
        } catch (RuntimeException | InstantiationException | IllegalAccessException e) {
            throw new IOException("Error parsing configuration", e);
        }
    }

    protected WritableConfigurationSegmentImpl getRootSegment() {
        return rootSegment;
    }

    @Override
    public String toString() {
        if (segmentRootNode != null) {
            return segmentRootNode.toString();
        } else {
            return "<null root node>";
        }
    }

    protected JsonNode getSegmentRootNode() {
        return segmentRootNode;
    }

    private JsonNode navigatePath(String relativePath) {
        if (segmentRootNode == null) {
            return null;
        }
        JsonNode treeLocation = segmentRootNode;
        for (String pathSegment : relativePath.split("/")) {
            @SuppressWarnings("unused") JsonNode oldTreeLocation = treeLocation; // only for debug output
            treeLocation = treeLocation.get(pathSegment);
            // log.debug(String.format("Traversing JSON tree by path segment '%s': %s -> %s", pathSegment, oldTreeLocation, treeLocation));
            if (treeLocation == null) {
                return null;
            }
        }
        return treeLocation;
    }

    private <T> T useDefaultValueIfNull(T value, T defaultValue) {
        if (value != null) {
            return value;
        } else {
            return defaultValue;
        }
    }
}
