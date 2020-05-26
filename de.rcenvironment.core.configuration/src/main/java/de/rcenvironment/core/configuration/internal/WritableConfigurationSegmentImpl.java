/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.rcenvironment.core.configuration.ConfigurationException;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.WritableConfigurationSegment;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * Default {@link WritableConfigurationSegment} implementation.
 * 
 * @author Robert Mischke
 * @author David Scholz
 */
class WritableConfigurationSegmentImpl implements WritableConfigurationSegment {

    private WritableConfigurationSegmentImpl rootSegment;

    // note: may be null in case of a non-existing segment;
    private JsonNode segmentRootNode;

    @SuppressWarnings("unused")
    private final Log log = LogFactory.getLog(getClass());

    WritableConfigurationSegmentImpl(JsonNode treeRoot) {
        this.segmentRootNode = treeRoot;
        this.rootSegment = this;
    }

    WritableConfigurationSegmentImpl(JsonNode treeRoot, WritableConfigurationSegmentImpl rootSegment) {
        this.segmentRootNode = treeRoot;
        this.rootSegment = rootSegment;
    }

    @Override
    public ConfigurationSegment getSubSegment(String relativePath) {
        JsonNode treeLocation = navigatePath(relativePath);
        return new WritableConfigurationSegmentImpl(treeLocation, rootSegment);
    }

    @Override
    public WritableConfigurationSegment getOrCreateWritableSubSegment(String relativePath) throws ConfigurationException {
        JsonNode treeLocation = createPath(relativePath); // create missing elements
        return new WritableConfigurationSegmentImpl(treeLocation, rootSegment);
    }

    @Override
    public WritableConfigurationSegment createElement(String id) throws ConfigurationException {
        // at the moment, this is the same, so just delegate
        // FIXME 7.0.0: check for an existing element for this id, and fail if there is one
        return getOrCreateWritableSubSegment(id);
    }

    @Override
    public boolean deleteElement(String id) throws ConfigurationException {
        if (!(segmentRootNode instanceof ObjectNode)) {
            throw new ConfigurationException("Consistency error: segment node for deleteElement() is not an object node, but "
                + segmentRootNode.getClass());
        }
        JsonNode removed = ((ObjectNode) segmentRootNode).remove(id);
        return (removed != null);
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
            return useDefaultValueIfNull(treeLocation.textValue(), defaultValue);
        } else {
            return defaultValue;
        }
    }

    @Override
    public void setString(String name, String value) throws ConfigurationException {
        validateNodeExistsAndIsAnObjectNode();
        ((ObjectNode) segmentRootNode).put(name, value);
    }

    @Override
    public Long getLong(String relativePath) {
        return getLong(relativePath, null);
    }

    @Override
    public Long getLong(String relativePath, Long defaultValue) {
        JsonNode treeLocation = navigatePath(relativePath);
        if (treeLocation != null) {
            if (!treeLocation.isIntegralNumber()) {
                // TODO (p2) improve: print configuration tree location to make this more user-friendly
                log.error(
                    "Expected an integer configuration value, but found \"" + treeLocation.asText()
                        + "\"; treating it as the default value \"" + defaultValue + "\"");
                return defaultValue;
            }
            return treeLocation.longValue();
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
            // TODO (p1) >8.0.0 check how this reacts to integer over-/underflows
            if (!treeLocation.isIntegralNumber()) {
                // TODO (p2) improve: print configuration tree location to make this more user-friendly
                log.error(
                    "Expected an integer configuration value, but found \"" + treeLocation.asText()
                        + "\"; treating it as the default value \"" + defaultValue + "\"");
                return defaultValue;
            }
            return treeLocation.intValue();
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
            return useDefaultValueIfNull(treeLocation.doubleValue(), defaultValue);
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
            return useDefaultValueIfNull(treeLocation.booleanValue(), defaultValue);
        } else {
            return defaultValue;
        }
    }

    @Override
    public void setBoolean(String key, boolean value) throws ConfigurationException {
        validateNodeExistsAndIsAnObjectNode();
        ((ObjectNode) segmentRootNode).put(key, value);
    }

    @Override
    public void setStringArray(String key, String[] value) throws ConfigurationException {
        validateNodeExistsAndIsAnObjectNode();
        final ArrayNode newArrayNode = JsonNodeFactory.instance.arrayNode();
        for (String element : value) {
            newArrayNode.add(element);
        }
        ((ObjectNode) segmentRootNode).put(key, newArrayNode);
    }

    @Override
    public void setInteger(String key, Integer value) throws ConfigurationException {
        validateNodeExistsAndIsAnObjectNode();
        ((ObjectNode) segmentRootNode).put(key, value);
    }
    
    @Override
    public void setFloat(String key, Float value) throws ConfigurationException {
        validateNodeExistsAndIsAnObjectNode();
        ((ObjectNode) segmentRootNode).put(key, value);
    }

    @Override
    public void setLong(String key, Long value) throws ConfigurationException {
        validateNodeExistsAndIsAnObjectNode();
        ((ObjectNode) segmentRootNode).put(key, value);
    }

    @Override
    public List<String> getStringArray(String relativePath) throws ConfigurationException {
        validateNodeExistsAndIsAnObjectNode();
        List<String> list = new ArrayList<String>();
        for (JsonNode node : segmentRootNode.path(relativePath)) {
            list.add(node.asText());
        }

        return list;
    }

    @Override
    public Map<String, ConfigurationSegment> listElements(String relativePath) {
        Map<String, ConfigurationSegment> resultMap = new HashMap<>();
        JsonNode treeLocation = navigatePath(relativePath);
        if (treeLocation == null) {
            return new HashMap<>(); // see JavaDoc
        }
        Iterator<Entry<String, JsonNode>> iterator = treeLocation.fields();
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
            return JsonUtils.getDefaultObjectMapper().treeToValue(segmentRootNode, clazz);
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
            // log.debug(StringUtils.format("Traversing JSON tree by path segment '%s': %s -> %s", pathSegment, oldTreeLocation,
            // treeLocation));
            if (treeLocation == null) {
                return null;
            }
        }
        return treeLocation;
    }

    private JsonNode createPath(String relativePath) throws ConfigurationException {
        if (segmentRootNode == null) {
            throw new ConfigurationException("Tried to create a new configuration segment from a non-existing one");
        }
        JsonNode treeLocation = segmentRootNode;
        for (String pathSegment : relativePath.split("/")) {
            final JsonNode oldTreeLocation = treeLocation;
            treeLocation = oldTreeLocation.get(pathSegment);
            // log.debug(StringUtils.format("Traversing JSON tree by path segment '%s': %s -> %s", pathSegment, oldTreeLocation,
            // treeLocation));
            if (treeLocation == null) {
                final ObjectNode newObjectNode = JsonNodeFactory.instance.objectNode();
                ((ObjectNode) oldTreeLocation).put(pathSegment, newObjectNode);
                treeLocation = newObjectNode;
            }
        }
        return treeLocation;
    }

    private void validateNodeExistsAndIsAnObjectNode() throws ConfigurationException {
        if (!isPresentInCurrentConfiguration()) {
            throw new ConfigurationException("The parent segment must exist before new fields can be added");
        }
        if (!segmentRootNode.isObject()) {
            throw new ConfigurationException("The parent segment does not point to a valid configuration (JSON) node");
        }
    }

    private <T> T useDefaultValueIfNull(T value, T defaultValue) {
        if (value != null) {
            return value;
        } else {
            return defaultValue;
        }
    }

}
