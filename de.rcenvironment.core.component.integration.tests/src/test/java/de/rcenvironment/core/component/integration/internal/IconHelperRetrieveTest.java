/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.integration.internal;

import static org.junit.Assert.assertSame;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.component.integration.ConfigurationMap;
import de.rcenvironment.core.component.integration.internal.ToolIntegrationServiceImpl.IconSize;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Tests for IconHelper.prescaleAndCopy.
 * 
 * @author Alexander Weinert
 */
public class IconHelperRetrieveTest {

    private static final String ICON_PNG = "icon.png";

    private static final long MODIFICATION_DATE_2 = 42L;

    private static final long MODIFICATION_DATE = 4815162342L;

    private static final String DEADBEEF = "DEADBEEF";

    private static final String BADDCAFE = "BADDCAFE";

    private IMocksControl control;

    private IconHelper iconHelper;

    private FileAccessService fileAccessService;

    private HashingService hashingService;

    private ImageService imageService;

    /**
     * Matcher that matches the captured value of the given Capture.
     * 
     * @author Alexander Weinert
     * @param <T> The type of the captured value
     */
    private static final class CaptureMatcher<T> implements IArgumentMatcher {

        private final Capture<T> capture;

        private CaptureMatcher(Capture<T> capture) {
            this.capture = capture;
        }

        @Override
        public boolean matches(Object other) {
            if (!capture.hasCaptured()) {
                return false;
            } else if (capture.getValue() == null) {
                return other == null;
            } else {
                return capture.getValue().equals(other);
            }
        }

        @Override
        public void appendTo(StringBuffer buffer) {
            buffer.append("eqCaptured(");
            buffer.append(capture);
            buffer.append(")");
        }
    }

    private static <T> T eqCapture(Capture<T> capture) {
        EasyMock.reportMatcher(new CaptureMatcher<T>(capture));
        return null;
    }

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
     * If no path is given for the icon, iconHelper.getIcon(IconSize.ICONSIZE16) shall return the icon stored at
     * /resources/icons/tool16.png.
     * 
     * @throws IOException Is never thrown, as we only call methods that may throw this exception on mocked objects.
     */
    @Test
    public void testRetrieveNullIcon16() throws IOException {

        // We construct the configuration map without using the ConfigurationMapBuilder here, since we want to expect a call to
        // configurationMap.get(KEY_TOOL_ICON_PATH), but we want this call to return null, which is not possible using the builder.
        final ConfigurationMap configurationMap = EasyMock.createMock(ConfigurationMap.class);
        EasyMock.expect(configurationMap.getIconPath()).andStubReturn(null);
        EasyMock.replay(configurationMap);

        final File toolConfigFile = control.createMock(File.class);

        final byte[] expectedResult = { (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE };

        EasyMock.expect(fileAccessService.toByteArray(EasyMock.anyObject(InputStream.class))).andReturn(expectedResult);
        control.replay();

        final byte[] actualResult = iconHelper.getIcon(IconSize.ICONSIZE16, configurationMap, toolConfigFile);
        assertSame(expectedResult, actualResult);

        control.verify();
    }

    /**
     * If no path is given for the icon, iconHelper.getIcon(IconSize.ICONSIZE32) shall return the icon stored at
     * /resources/icons/tool32.png.
     * 
     * @throws IOException Is never thrown, as we only call methods that may throw this exception on mocked objects.
     */
    @Test
    public void testRetrieveNullIcon32() throws IOException {

        // We construct the configuration map without using the ConfigurationMapBuilder here, since we want to expect a call to
        // configurationMap.get(KEY_TOOL_ICON_PATH), but we want this call to return null, which is not possible using the builder.
        final ConfigurationMap configurationMap = EasyMock.createMock(ConfigurationMap.class);
        EasyMock.expect(configurationMap.getIconPath()).andStubReturn(null);
        EasyMock.replay(configurationMap);

        final File toolConfigFile = control.createMock(File.class);

        final byte[] expectedResult = { (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE };

        EasyMock.expect(fileAccessService.toByteArray(EasyMock.anyObject(InputStream.class))).andReturn(expectedResult);
        control.replay();

        final byte[] actualResult = iconHelper.getIcon(IconSize.ICONSIZE32, configurationMap, toolConfigFile);

        assertSame(expectedResult, actualResult);
        control.verify();
    }

    /**
     * If the actual icon has changed since the pre-scaled versions have been created, then icon retrieval shall fall back to live-scaling.
     * 
     * @throws IOException Is never thrown, as we only call methods that may throw this exception on mocked objects.
     */
    @Test
    public void testRetrievePrescaledIconOutdated() throws IOException {
        final String iconName = ICON_PNG;
        final String iconPath = iconName;

        final ConfigurationMap configurationMap = new ConfigurationMapBuilder()
            .toolIconPath(iconName)
            .modificationDate(MODIFICATION_DATE)
            .build();

        final File toolConfigFile = control.createMock(File.class);
        final File toolIconFile = new MockFileBuilder()
            .lastModified(MODIFICATION_DATE_2)
            .exists(true)
            .isFile(true)
            .build();

        expectToolIconFileRetrieval(iconPath, toolConfigFile, toolIconFile);

        expectToolIconFileRetrieval(iconPath, toolConfigFile, toolIconFile);

        final BufferedImage toolIconUnscaled = EasyMock.createMock(BufferedImage.class);
        final BufferedImage toolIconScaled = EasyMock.createMock(BufferedImage.class);
        EasyMock.expect(imageService.readImage(toolIconFile)).andReturn(toolIconUnscaled);
        EasyMock.expect(imageService.resize(toolIconUnscaled, IconSize.ICONSIZE16.getSize())).andReturn(toolIconScaled);
        // The file used for writing the scaled tool icon is obtained from the TempFileService, which is not injected - hence, we have to
        // expect any File here.
        final Capture<File> tempFileCapture = Capture.newInstance();
        imageService.write(EasyMock.eq(toolIconScaled), EasyMock.eq("PNG"), EasyMock.capture(tempFileCapture));

        final byte[] expectedResult = {};
        EasyMock.expect(fileAccessService.readToByteArray(eqCapture(tempFileCapture))).andReturn(expectedResult);

        control.replay();

        final byte[] actualResult = iconHelper.getIcon(IconSize.ICONSIZE16, configurationMap, toolConfigFile);

        assertSame(expectedResult, actualResult);

        control.verify();
    }

    /**
     * If the actual icon has not changed since the pre-scaled versions have been created, but if it has a different hash that the
     * pre-scaled icon, then icon retrieval shall fall back to live-scaling.
     * 
     * @throws IOException Is never thrown, as we only call methods that may throw this exception on mocked objects.
     */
    @Test
    public void testRetrievePrescaledIconWrongHash() throws IOException {
        final String iconName = ICON_PNG;
        final String iconPath = iconName;

        final ConfigurationMap configurationMap = new ConfigurationMapBuilder()
            .toolIconPath(iconName)
            .modificationDate(MODIFICATION_DATE)
            .hash(DEADBEEF)
            .build();

        final File toolConfigFile = control.createMock(File.class);
        final File toolIconFile = new MockFileBuilder()
            .lastModified(MODIFICATION_DATE)
            .exists(true)
            .isFile(true)
            .build();

        // First, IconHelper shall check and build the file for the actual icon when checking its modification date
        expectToolIconFileRetrieval(iconPath, toolConfigFile, toolIconFile);

        expectToolIconFileHashing(toolIconFile, BADDCAFE);

        expectToolIconFileRetrieval(iconPath, toolConfigFile, toolIconFile);

        final BufferedImage toolIconUnscaled = EasyMock.createMock(BufferedImage.class);
        final BufferedImage toolIconScaled = EasyMock.createMock(BufferedImage.class);
        EasyMock.expect(imageService.readImage(toolIconFile)).andReturn(toolIconUnscaled);
        EasyMock.expect(imageService.resize(toolIconUnscaled, IconSize.ICONSIZE16.getSize())).andReturn(toolIconScaled);
        // The file used for writing the scaled tool icon is obtained from the TempFileService, which is not injected - hence, we have to
        // expect any File here.
        final Capture<File> tempFileCapture = Capture.newInstance();
        imageService.write(EasyMock.eq(toolIconScaled), EasyMock.eq("PNG"), EasyMock.capture(tempFileCapture));

        final byte[] expectedResult = {};
        EasyMock.expect(fileAccessService.readToByteArray(eqCapture(tempFileCapture))).andReturn(expectedResult);

        control.replay();

        final byte[] actualResult = iconHelper.getIcon(IconSize.ICONSIZE16, configurationMap, toolConfigFile);

        assertSame(expectedResult, actualResult);

        control.verify();
    }

    /**
     * If the actual icon has not changed since the pre-scaled versions have been created and if it has the same hash value as during
     * pre-scaling, then icon retrieval shall return the pre-scaled icon.
     * 
     * @throws IOException Is never thrown, as we only call methods that may throw this exception on mocked objects.
     */
    @Test
    public void testRetrievePrescaledIcon() throws IOException {
        final String iconName = ICON_PNG;
        final String iconPath = iconName;

        final ConfigurationMap configurationMap = new ConfigurationMapBuilder()
            .toolIconPath(iconName)
            .modificationDate(MODIFICATION_DATE)
            .hash(DEADBEEF)
            .build();

        final File toolConfigFile = control.createMock(File.class);
        final File toolIconFile = new MockFileBuilder()
            .lastModified(MODIFICATION_DATE)
            .exists(true)
            .isFile(true)
            .build();

        // In order to check modification date and hash value, the IconHelper first has to retrieve the actual icon file and compute its
        // md5-hash.
        expectToolIconFileRetrieval(iconPath, toolConfigFile, toolIconFile);
        expectToolIconFileHashing(toolIconFile, DEADBEEF);

        // Since both the modification date and the hash match the stored values, the IconHelper has to retrieve the stored pre-scaled
        // version.
        final File prescaledFile = new MockFileBuilder().exists(true).build();
        final byte[] expectedResult = {};
        EasyMock.expect(fileAccessService.createFile(toolConfigFile, IconSize.ICONSIZE16.getPath())).andReturn(prescaledFile);
        EasyMock.expect(fileAccessService.readToByteArray(prescaledFile)).andReturn(expectedResult);

        control.replay();

        final byte[] actualResult = iconHelper.getIcon(IconSize.ICONSIZE16, configurationMap, toolConfigFile);

        assertSame(expectedResult, actualResult);

        control.verify();
    }

    /**
     * If the actual icon has changed since the pre-scaled versions have been created, and if reading the custom icon fails with an
     * IOException, then IconHelper shall fall back to retrieving the default icon.
     * 
     * @throws IOException Is never thrown, as we only call methods that may throw this exception on mocked objects.
     */
    @Test
    public void testRetrievePrescaledIconOutdatedNoCustomIcon() throws IOException {
        final String iconName = ICON_PNG;
        final String iconPath = iconName;

        final ConfigurationMap configurationMap = new ConfigurationMapBuilder()
            .toolIconPath(iconName)
            .modificationDate(MODIFICATION_DATE)
            .hash(DEADBEEF)
            .build();

        final File toolConfigFile = control.createMock(File.class);
        final File toolIconFile = new MockFileBuilder()
            .lastModified(MODIFICATION_DATE_2)
            .exists(true)
            .isFile(true)
            .build();

        // First, IconHelper shall check and build the file for the actual icon when checking its modification date
        expectToolIconFileRetrieval(iconPath, toolConfigFile, toolIconFile);

        // Subsequently, it shall build the file again for retrieving the actual icon and trying to rescale it.
        expectToolIconFileRetrieval(iconPath, toolConfigFile, toolIconFile);

        EasyMock.expect(imageService.readImage(toolIconFile)).andThrow(new IOException());

        // Since retrieving the actual file has failed, IconHelper shall fall back to retrieving the default icon for the given size.
        final byte[] expectedResult = { (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE };

        EasyMock.expect(fileAccessService.toByteArray(EasyMock.anyObject(InputStream.class))).andReturn(expectedResult);
        control.replay();

        final byte[] actualResult = iconHelper.getIcon(IconSize.ICONSIZE32, configurationMap, toolConfigFile);

        assertSame(expectedResult, actualResult);
        control.verify();
    }

    private void expectToolIconFileRetrieval(final String iconPath, final File toolConfigFile, final File toolIconFile) {
        EasyMock.expect(fileAccessService.createFile(iconPath)).andReturn(new MockFileBuilder().isAbsolute(false).build());
        EasyMock.expect(fileAccessService.createFile(toolConfigFile, iconPath)).andReturn(toolIconFile);
    }

    private void expectToolIconFileHashing(final File toolIconFile, final String hash) throws IOException {
        final byte[] toolIconFileContent = {};
        EasyMock.expect(fileAccessService.readToByteArray(toolIconFile)).andReturn(toolIconFileContent);
        EasyMock.expect(hashingService.md5Hex(toolIconFileContent)).andReturn(hash);
    }
}
