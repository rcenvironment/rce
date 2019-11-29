/*
 * Copyright (C) 2018 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.integration.internal;

import java.util.Map;

import org.easymock.EasyMock;

import de.rcenvironment.core.component.model.impl.ToolIntegrationConstants;

/**
 * Utility class for constructing a configuration map for testing. For now, only certain values can be set, other setter functions may be
 * added as required.
 *
 * @author Alexander Weinert
 */
class ConfigurationMapBuilder {

    private String toolIconPath = null;

    private Boolean uploadIcon = null;

    private String existingHash = null;

    private String expectedHash = null;

    private Long expectedModificationDate = null;

    private String updatedToolIconPath = null;

    private boolean expectUploadRemoval = false;

    private Long modificationDate = null;

    /**
     * @param toolIconPathParam The value to be returned by configurationMap.get(KEY_TOOL_ICON_PATH).
     * @return This builder for daisy-chaining.
     */
    public ConfigurationMapBuilder toolIconPath(String toolIconPathParam) {
        this.toolIconPath = toolIconPathParam;
        return this;
    }

    /**
     * @param toolIconPath The value to be returned by configurationMap.get(KEY_UPLOAD_ICON).
     * @return This builder for daisy-chaining.
     */
    public ConfigurationMapBuilder uploadIcon(Boolean uploadIconParam) {
        this.uploadIcon = uploadIconParam;
        return this;
    }

    /**
     * @param hashParam The hashcode to be stored in the configuration map.
     * @return This builder for daisy-chaining.
     */
    public ConfigurationMapBuilder hash(String hashParam) {
        this.existingHash = hashParam;
        return this;
    }

    /**
     * @param expectedHashParam The hash expected to be set for the key KEY_ICON_HASH.
     * @return This builder for daisy-chaining.
     */
    public ConfigurationMapBuilder expectHash(String expectedHashParam) {
        this.expectedHash = expectedHashParam;
        return this;
    }

    /**
     * @param expectedModificationDateParam The modification date expected to be set for the key KEY_ICON_MODIFICATION_DATE.
     * @return This builder for daisy-chaining.
     */
    public ConfigurationMapBuilder expectModificationDate(Long expectedModificationDateParam) {
        this.expectedModificationDate = expectedModificationDateParam;
        return this;
    }

    /**
     * @param toolIconPathParam The tool icon path expected to be set for the key KEY_TOOL_ICON_PATH.
     * @return This builder for daisy-chaining.
     */
    public ConfigurationMapBuilder expectToolIconPathUpdate(String toolIconPathParam) {
        this.updatedToolIconPath = toolIconPathParam;
        return this;
    }

    /**
     * Causes the resulting mock to allow a call to .remove(KEY_UPLOAD_ICON).
     * 
     * @return This builder for daisy-chaining.
     */
    public ConfigurationMapBuilder expectUploadRemoval() {
        this.expectUploadRemoval = true;
        return this;
    }

    public ConfigurationMapBuilder modificationDate(long modificationDateParam) {
        this.modificationDate = modificationDateParam;
        return this;
    }

    /**
     * @return A mock of map with the given configuration.
     */
    public Map<String, Object> build() {
        @SuppressWarnings("unchecked") final Map<String, Object> configurationMap = EasyMock.createMock(Map.class);

        if (toolIconPath != null) {
            EasyMock.expect(configurationMap.get(ToolIntegrationConstants.KEY_TOOL_ICON_PATH)).andStubReturn(toolIconPath);
        }

        if (uploadIcon != null) {
            EasyMock.expect(configurationMap.get(ToolIntegrationConstants.KEY_UPLOAD_ICON)).andStubReturn(uploadIcon);
        }

        if (existingHash != null) {
            EasyMock.expect(configurationMap.get(ToolIntegrationConstants.KEY_ICON_HASH)).andStubReturn(existingHash);
        }

        if (expectedHash != null) {
            EasyMock.expect(configurationMap.put(ToolIntegrationConstants.KEY_ICON_HASH, expectedHash)).andStubReturn(existingHash);
        }

        if (expectedModificationDate != null) {
            EasyMock.expect(configurationMap.put(ToolIntegrationConstants.KEY_ICON_MODIFICATION_DATE, expectedModificationDate))
                .andStubReturn(null);
        }

        if (updatedToolIconPath != null) {
            EasyMock.expect(configurationMap.put(ToolIntegrationConstants.KEY_TOOL_ICON_PATH, updatedToolIconPath))
                .andStubReturn(toolIconPath);
        }

        if (expectUploadRemoval) {
            EasyMock.expect(configurationMap.remove(ToolIntegrationConstants.KEY_UPLOAD_ICON)).andStubReturn(uploadIcon);
        }

        if (modificationDate != null) {
            EasyMock.expect(configurationMap.get(ToolIntegrationConstants.KEY_ICON_MODIFICATION_DATE)).andStubReturn(modificationDate);
        }

        EasyMock.replay(configurationMap);

        return configurationMap;

    }

}
