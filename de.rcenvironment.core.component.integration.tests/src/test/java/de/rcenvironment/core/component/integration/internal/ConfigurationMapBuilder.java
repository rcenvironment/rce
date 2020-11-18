/*
 * Copyright (C) 2018 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.integration.internal;

import org.easymock.EasyMock;

import de.rcenvironment.core.component.integration.ConfigurationMap;

/**
 * Utility class for constructing a configuration map for testing. For now, only certain values can be set, other setter functions may be
 * added as required.
 *
 * @author Alexander Weinert
 */
class ConfigurationMapBuilder {
    
    private final ConfigurationMap mockedConfigurationMap = EasyMock.createMock(ConfigurationMap.class);

    /**
     * @param toolIconPathParam The value to be returned by configurationMap.get(KEY_TOOL_ICON_PATH).
     * @return This builder for daisy-chaining.
     */
    public ConfigurationMapBuilder toolIconPath(String toolIconPathParam) {
        EasyMock.expect(mockedConfigurationMap.getIconPath()).andStubReturn(toolIconPathParam);
        return this;
    }

    /**
     * @param toolIconPath The value to be returned by configurationMap.get(KEY_UPLOAD_ICON).
     * @return This builder for daisy-chaining.
     */
    public ConfigurationMapBuilder uploadIcon(Boolean uploadIconParam) {
        EasyMock.expect(mockedConfigurationMap.shouldUploadIcon()).andStubReturn(uploadIconParam);
        return this;
    }

    /**
     * @param hashParam The hashcode to be stored in the configuration map.
     * @return This builder for daisy-chaining.
     */
    public ConfigurationMapBuilder hash(String hashParam) {
        EasyMock.expect(this.mockedConfigurationMap.getIconHash()).andStubReturn(hashParam);
        return this;
    }

    /**
     * @param expectedHashParam The hash expected to be set for the key KEY_ICON_HASH.
     * @return This builder for daisy-chaining.
     */
    public ConfigurationMapBuilder expectHash(String expectedHashParam) {
        this.mockedConfigurationMap.setIconHash(expectedHashParam);
        EasyMock.expectLastCall();
        return this;
    }

    /**
     * @param expectedModificationDateParam The modification date expected to be set for the key KEY_ICON_MODIFICATION_DATE.
     * @return This builder for daisy-chaining.
     */
    public ConfigurationMapBuilder expectModificationDate(Long expectedModificationDateParam) {
        this.mockedConfigurationMap.setIconModificationDate(expectedModificationDateParam);
        EasyMock.expectLastCall();
        return this;
    }

    /**
     * @param toolIconPathParam The tool icon path expected to be set for the key KEY_TOOL_ICON_PATH.
     * @return This builder for daisy-chaining.
     */
    public ConfigurationMapBuilder expectToolIconPathUpdate(String toolIconPathParam) {
        this.mockedConfigurationMap.setIconPath(toolIconPathParam);
        EasyMock.expectLastCall();
        return this;
    }

    /**
     * Causes the resulting mock to allow a call to .remove(KEY_UPLOAD_ICON).
     * 
     * @return This builder for daisy-chaining.
     */
    public ConfigurationMapBuilder expectUploadRemoval() {
        this.mockedConfigurationMap.doNotUploadIcon();
        EasyMock.expectLastCall();
        return this;
    }

    public ConfigurationMapBuilder modificationDate(long modificationDateParam) {
        EasyMock.expect(this.mockedConfigurationMap.getIconModificationDate()).andStubReturn(modificationDateParam);
        return this;
    }

    /**
     * @return A mock of map with the given configuration.
     */
    public ConfigurationMap build() {

        EasyMock.replay(this.mockedConfigurationMap);

        return this.mockedConfigurationMap;

    }

}
