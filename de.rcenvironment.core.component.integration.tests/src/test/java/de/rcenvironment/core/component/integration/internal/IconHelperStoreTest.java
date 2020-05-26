/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.integration.internal;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.component.model.impl.ToolIntegrationConstants;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Tests for IconHelper.prescaleAndCopy.
 * 
 * @author Alexander Weinert
 */
public class IconHelperStoreTest {

    private static final String C_DRIVE = "C:/";

    private static final String ICON_PNG = "icon.png";

    private static final String DEADBEEF = "DEADBEEF";

    private IMocksControl control;

    private IconHelper iconHelper;

    private FileAccessService fileAccessService;

    private HashingService hashingService;

    private ImageService imageService;

    /**
     * Runs before every unit test.
     */
    @Before
    public void setup() {
        TempFileServiceAccess.setupUnitTestEnvironment();

        control = EasyMock.createControl();

        fileAccessService = control.createMock(FileAccessService.class);
        hashingService = control.createMock(HashingService.class);
        imageService = control.createMock(ImageService.class);

        iconHelper = new IconHelper();

        iconHelper.bindFileAccessService(fileAccessService);
        iconHelper.bindHashingService(hashingService);
        iconHelper.bindImageService(imageService);
    }

    /**
     * If no path is given for the icon, iconHelper.prescaleAndCopy shall return early.
     */
    @Test
    public void testStoreNullPath() {

        // We construct the configuration map without using the ConfigurationMapBuilder here, since we want to expect a call to
        // configurationMap.get(KEY_TOOL_ICON_PATH), but we want this call to return null, which is not possible using the builder.
        @SuppressWarnings("unchecked") final Map<String, Object> configurationMap = EasyMock.createMock(Map.class);
        EasyMock.expect(configurationMap.get(ToolIntegrationConstants.KEY_TOOL_ICON_PATH)).andReturn(null);
        EasyMock.replay(configurationMap);

        final File toolConfigFile = control.createMock(File.class);
        control.replay();

        iconHelper.prescaleAndCopyIcon(configurationMap, toolConfigFile);

        EasyMock.verify(fileAccessService, hashingService, imageService, configurationMap, toolConfigFile);
    }

    /**
     * If an empty path is given for the icon, iconHelper.prescaleAndCopy shall return early.
     */
    @Test
    public void testStoreEmptyPath() {
        final Map<String, Object> configurationMap = new ConfigurationMapBuilder()
            .toolIconPath("")
            .build();

        final File toolConfigFile = control.createMock(File.class);
        control.replay();

        iconHelper.prescaleAndCopyIcon(configurationMap, toolConfigFile);

        EasyMock.verify(fileAccessService, hashingService, imageService, configurationMap, toolConfigFile);
    }

    /**
     * Tests that pre-scaling works when given a relative path and not asking that the icon be copied to the tool integration folder.
     * 
     * @throws IOException Does not actually occur, since we are only calling the methods that may throw exceptions on mocks.
     */
    @Test
    public void testStoreRelativePathWithoutCopy() throws IOException {
        final String toolName = ICON_PNG;
        final String toolPath = toolName;

        final File toolConfigFile = new MockFileBuilder().build();
        final long lastModified = 4815162342L;
        final File toolIconFile = new MockFileBuilder()
            .exists(true)
            .isFile(true)
            .isAbsolute(false)
            .lastModified(lastModified)
            .build();

        final String md5Hash = DEADBEEF;
        final Map<String, Object> configurationMap = new ConfigurationMapBuilder()
            .toolIconPath(toolPath)
            .uploadIcon(Boolean.FALSE)
            .expectHash(md5Hash)
            .expectModificationDate(lastModified)
            .build();

        final File toolPathFile = control.createMock(File.class);
        EasyMock.expect(fileAccessService.createFile(toolPath)).andReturn(toolPathFile);
        EasyMock.expect(toolPathFile.isAbsolute()).andReturn(false);
        EasyMock.expect(fileAccessService.createFile(toolConfigFile, toolPath)).andReturn(toolIconFile);

        final BufferedImage iconImage = control.createMock(BufferedImage.class);
        final byte[] iconContent = {};

        EasyMock.expect(imageService.readImage(toolIconFile)).andReturn(iconImage);
        EasyMock.expect(fileAccessService.readToByteArray(toolIconFile)).andReturn(iconContent);
        EasyMock.expect(hashingService.md5Hex(iconContent)).andReturn(md5Hash);
        
        expectScaleAndWrite(toolConfigFile, iconImage);
        expectScaleAndWrite(toolConfigFile, iconImage);
        expectScaleAndWrite(toolConfigFile, iconImage);

        control.replay();

        iconHelper.prescaleAndCopyIcon(configurationMap, toolConfigFile);

        EasyMock.verify(toolConfigFile, configurationMap, toolIconFile, toolPathFile, iconImage,
            fileAccessService,
            hashingService, imageService);

    }

    /**
     * Tests that pre-scaling works when given a relative path and asking that the icon be copied to the tool integration folder.
     * 
     * @throws IOException Does not actually occur, since we are only calling the methods that may throw exceptions on mocks.
     */
    @Test
    public void testStoreRelativePathWithCopy() throws IOException {
        final String toolName = ICON_PNG;
        final String toolPath = toolName;

        final File toolConfigFile = new MockFileBuilder().build();
        final long lastModified = 4815162342L;
        final File toolIconFile = new MockFileBuilder()
            .name(toolName)
            .exists(true)
            .isFile(true)
            .isAbsolute(false)
            .lastModified(lastModified)
            .build();

        final String md5Hash = DEADBEEF;
        final Map<String, Object> configurationMap = new ConfigurationMapBuilder()
            .toolIconPath(toolPath)
            .uploadIcon(Boolean.TRUE)
            .expectHash(md5Hash)
            .expectModificationDate(lastModified)
            .expectToolIconPathUpdate(toolPath)
            .expectUploadRemoval()
            .build();

        final File toolPathFile = control.createMock(File.class);
        EasyMock.expect(fileAccessService.createFile(toolPath)).andReturn(toolPathFile);
        EasyMock.expect(toolPathFile.isAbsolute()).andReturn(false);
        EasyMock.expect(fileAccessService.createFile(toolConfigFile, toolPath)).andReturn(toolIconFile);

        final BufferedImage iconImage = control.createMock(BufferedImage.class);
        final byte[] iconContent = {};

        EasyMock.expect(imageService.readImage(toolIconFile)).andReturn(iconImage);
        EasyMock.expect(fileAccessService.readToByteArray(toolIconFile)).andReturn(iconContent);
        EasyMock.expect(hashingService.md5Hex(iconContent)).andReturn(md5Hash);
        
        // final File copyDestinationFile = new MockFileBuilder().name(toolPath).build();
        // EasyMock.expect(fileAccessService.createFile(toolConfigFile, toolPath)).andReturn(copyDestinationFile);
        // fileAccessService.copyFile(toolIconFile, copyDestinationFile);
        // EasyMock.expectLastCall();

        expectScaleAndWrite(toolConfigFile, iconImage);
        expectScaleAndWrite(toolConfigFile, iconImage);
        expectScaleAndWrite(toolConfigFile, iconImage);

        control.replay();

        iconHelper.prescaleAndCopyIcon(configurationMap, toolConfigFile);

        EasyMock.verify(toolConfigFile, configurationMap, toolIconFile, toolPathFile, iconImage,
            fileAccessService,
            hashingService, imageService);
    }

    /**
     * Tests that pre-scaling works when given a relative path and not asking that the icon be copied to the tool integration folder.
     * 
     * @throws IOException Does not actually occur, since we are only calling the methods that may throw exceptions on mocks.
     */
    @Test
    public void testStoreAbsolutePathWithoutCopy() throws IOException {
        final String toolName = ICON_PNG;
        final String toolPath = C_DRIVE + toolName;

        final File toolConfigFile = new MockFileBuilder().build();
        final long lastModified = 4815162342L;
        final File toolIconFile = new MockFileBuilder()
            .exists(true)
            .isFile(true)
            .isAbsolute(false)
            .lastModified(lastModified)
            .build();

        final String md5Hash = DEADBEEF;
        final Map<String, Object> configurationMap = new ConfigurationMapBuilder()
            .toolIconPath(toolPath)
            .uploadIcon(Boolean.FALSE)
            .expectHash(md5Hash)
            .expectModificationDate(lastModified)
            .build();

        final File toolPathFile = control.createMock(File.class);
        EasyMock.expect(fileAccessService.createFile(toolPath)).andReturn(toolPathFile);
        EasyMock.expect(toolPathFile.isAbsolute()).andReturn(false);
        EasyMock.expect(fileAccessService.createFile(toolConfigFile, toolPath)).andReturn(toolIconFile);

        final BufferedImage iconImage = control.createMock(BufferedImage.class);
        final byte[] iconContent = {};

        EasyMock.expect(imageService.readImage(toolIconFile)).andReturn(iconImage);
        EasyMock.expect(fileAccessService.readToByteArray(toolIconFile)).andReturn(iconContent);
        EasyMock.expect(hashingService.md5Hex(iconContent)).andReturn(md5Hash);

        expectScaleAndWrite(toolConfigFile, iconImage);
        expectScaleAndWrite(toolConfigFile, iconImage);
        expectScaleAndWrite(toolConfigFile, iconImage);

        control.replay();

        iconHelper.prescaleAndCopyIcon(configurationMap, toolConfigFile);

        EasyMock.verify(toolConfigFile, configurationMap, toolIconFile, toolPathFile, iconImage,
            fileAccessService,
            hashingService, imageService);

    }

    /**
     * Tests that pre-scaling works when given a relative path and asking that the icon be copied to the tool integration folder.
     * 
     * @throws IOException Does not actually occur, since we are only calling the methods that may throw exceptions on mocks.
     */
    @Test
    public void testStoreAbsolutePathWithCopy() throws IOException {
        final String toolName = ICON_PNG;
        final String toolPath = C_DRIVE + toolName;

        final File toolConfigFile = new MockFileBuilder().build();
        final long lastModified = 4815162342L;
        final File toolIconFile = new MockFileBuilder()
            .name(toolPath)
            .exists(true)
            .isFile(true)
            .isAbsolute(true)
            .lastModified(lastModified)
            .build();

        final String md5Hash = DEADBEEF;
        final Map<String, Object> configurationMap = new ConfigurationMapBuilder()
            .toolIconPath(toolPath)
            .uploadIcon(Boolean.TRUE)
            .expectHash(md5Hash)
            .expectModificationDate(lastModified)
            .expectToolIconPathUpdate(toolPath)
            .expectUploadRemoval()
            .build();

        final File toolPathFile = control.createMock(File.class);
        EasyMock.expect(fileAccessService.createFile(toolPath)).andReturn(toolPathFile);
        EasyMock.expect(toolPathFile.isAbsolute()).andReturn(false);
        EasyMock.expect(fileAccessService.createFile(toolConfigFile, toolPath)).andReturn(toolIconFile);

        final BufferedImage iconImage = control.createMock(BufferedImage.class);
        final byte[] iconContent = {};

        EasyMock.expect(imageService.readImage(toolIconFile)).andReturn(iconImage);
        EasyMock.expect(fileAccessService.readToByteArray(toolIconFile)).andReturn(iconContent);
        EasyMock.expect(hashingService.md5Hex(iconContent)).andReturn(md5Hash);

        final File copyDestinationFile = new MockFileBuilder().name(toolPath).build();
        EasyMock.expect(fileAccessService.createFile(toolConfigFile, toolPath)).andReturn(copyDestinationFile);
        fileAccessService.copyFile(toolIconFile, copyDestinationFile);
        EasyMock.expectLastCall();

        expectScaleAndWrite(toolConfigFile, iconImage);
        expectScaleAndWrite(toolConfigFile, iconImage);
        expectScaleAndWrite(toolConfigFile, iconImage);

        control.replay();

        iconHelper.prescaleAndCopyIcon(configurationMap, toolConfigFile);

        EasyMock.verify(toolConfigFile, configurationMap, toolIconFile, toolPathFile, iconImage,
            fileAccessService,
            hashingService, imageService);
    }

    private void expectScaleAndWrite(final File toolConfigFile, final BufferedImage iconImage) throws IOException {

        final File scaledIconFile = control.createMock(File.class);
        final String iconFormat = "png";
        final BufferedImage scaledImage = control.createMock(BufferedImage.class);

        EasyMock.expect(imageService.resize(EasyMock.eq(iconImage), EasyMock.anyInt())).andReturn(scaledImage);
        EasyMock.expect(fileAccessService.createFile(EasyMock.eq(toolConfigFile), EasyMock.anyObject(String.class)))
            .andReturn(scaledIconFile);
        imageService.write(scaledImage, iconFormat, scaledIconFile);
        EasyMock.expectLastCall();
    }

}
