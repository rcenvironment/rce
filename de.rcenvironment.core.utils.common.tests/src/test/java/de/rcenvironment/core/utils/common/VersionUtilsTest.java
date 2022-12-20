/*
 * Copyright 2022-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.utils.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.experimental.runners.Enclosed;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

/**
 * {@link VersionUtils} unit tests.
 * 
 * @author Alexander Weinert
 * @author Dominik Schneider (JavaDoc)
 *
 */
@RunWith(Enclosed.class)
public class VersionUtilsTest {

    private final Capture<String> capturedString = Capture.newInstance(CaptureType.ALL);

    private class MockBundleAccessor extends BundleAccessor {

        private final Bundle ownBundle;

        private final Bundle[] bundlesInContext;

        private final Capture<String> debugMessages = Capture.newInstance(CaptureType.ALL);

        private final Capture<String> errorMessages = Capture.newInstance(CaptureType.ALL);

        MockBundleAccessor() {
            super(null);
            this.ownBundle = null;
            this.bundlesInContext = null;
        }

        MockBundleAccessor(final Bundle ownBundle, final Bundle... bundlesInContext) {
            super(null);
            this.ownBundle = ownBundle;
            this.bundlesInContext = bundlesInContext;
        }

        @Override
        public Bundle getOwnBundleOrLogDebug() {
            return ownBundle;
        }

        @Override
        public Bundle[] getAllBundlesOrLogError() {
            if (this.ownBundle == null) {
                return new Bundle[] {};
            }

            return bundlesInContext;
        }

        @Override
        protected void logDebug(String message) {
            this.debugMessages.setValue(message);
        }

        @Override
        protected void logError(String message) {
            this.errorMessages.setValue(message);
        }
    }

    /**
     * Part of {@link VersionUtils} unit tests. Tests build ID to string conversion.
     * 
     * @author Alexander Weinert
     * @author Dominik Schneider (JavaDoc)
     *
     */
    @RunWith(Parameterized.class)
    public static final class VersionUtilsGetBuildIdAsStringTest {

        private final Version version;

        private final String expected;

        public VersionUtilsGetBuildIdAsStringTest(Version version, String expected) {
            this.expected = expected;
            this.version = version;
        }

        @Parameterized.Parameters
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][] {
                { new Version("0.3.0.202206024412_SNAPSHOT"), "202206024412_SNAPSHOT" },
                { new Version("0.3.0.202206024412_RC"), "202206024412_RC" },
                { new Version("0.3.0.202206024412qualifier"), null },
                { new Version("0.3.0.202206024412foo"), "202206024412foo" },
            });
        }

        @Test
        public void getVersionAsStringTest() {
            assertEquals(this.expected, VersionUtils.getBuildIdAsString(this.version));
        }
    }

    /**
     * Part of {@link VersionUtils} unit tests. Tests version to string conversion.
     * 
     * @author Alexander Weinert
     * @author Dominik Schneider (JavaDoc)
     *
     */
    @RunWith(Parameterized.class)
    public static final class VersionUtilsGetVersionAsStringTest {

        private final Version version;

        private final String expected;

        public VersionUtilsGetVersionAsStringTest(Version version, String expected) {
            this.expected = expected;
            this.version = version;
        }

        @Parameterized.Parameters
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][] {
                { new Version("10.3.1"), "10.3.1" },
                { new Version("0.3.0"), "0.3.0" },
                { new Version("0.3.0.202206024412_SNAPSHOT"), "0.3.0_Snapshot" },
                { new Version("0.3.0.202206024412_RC"), "0.3.0_Release_Candidate" },
                { new Version("0.3.0.202206024412qualifier"), "0.3.0_Development" },
                { new Version("0.3.0.202206024412foo"), "0.3.0" },
            });
        }

        @Test
        public void getVersionAsStringTest() {
            assertEquals(this.expected, VersionUtils.getVersionAsString(this.version));
        }
    }

    @Test
    public void givenNullOwnBundleVersionOfPlatformBundlesIsNull() {
        assertNull(VersionUtils.getPlatformVersion(new MockBundleAccessor(), this::captureString));
        assertFalse(this.capturedString.hasCaptured());
    }

    @Test
    public void givenEmptyContextVersionOfPlatformBundlesIsNull() {
        assertNull(VersionUtils.getPlatformVersion(new MockBundleAccessor(createOwnBundle()), this::captureString));
        assertFalse(this.capturedString.hasCaptured());
    }

    @Test
    public void givenSingletonContextVersionOfPlatformBundlesIsThatBundle() {
        final Version version = new Version("10.3.1");
        final Bundle platformBundle = createBundle("de.rcenvironment.platform.foo", version);
        assertSame(version, VersionUtils.getPlatformVersion(new MockBundleAccessor(platformBundle, platformBundle), this::captureString));
        assertFalse(this.capturedString.hasCaptured());
    }

    @Test
    public void givenTwoElementContextVersionOfPlatformBundlesIsEitherBundle() {
        final Version versionFoo = new Version("10.3.1");
        final Version versionBar = new Version("10.3.2");
        final Version nonPlatformVersion = new Version("10.3.0");
        final Bundle platformBundleFoo = createBundle("de.rcenvironment.platform.foo", versionFoo);
        final Bundle platformBundleBar = createBundle("de.rcenvironment.platform.bar", versionBar);
        final Bundle platformBundleFoobar = createBundle("de.rcenvironment.platform.foobar", versionBar);
        final Bundle nonPlatformBundle = createBundle("de.rcenvironment.notaplatform", nonPlatformVersion);

        final Version actualVersion = VersionUtils.getPlatformVersion(
            new MockBundleAccessor(nonPlatformBundle, platformBundleFoo, platformBundleBar, platformBundleFoobar), this::captureString);

        assertTrue(Arrays.asList(versionFoo, versionBar).contains(actualVersion));
        assertTrue(this.capturedString.hasCaptured());
        assertEquals(1, this.capturedString.getValues().size());
        final String errorMessage = this.capturedString.getValues().get(0);
        assertTrue(errorMessage.contains("[10.3.1, 10.3.2]") || errorMessage.contains("[10.3.2, 10.3.1]"));
    }

    @Test
    public void givenNullContextVersionOfPlatformBundlesIsNull() {
        assertNull(VersionUtils.getPlatformVersion(new MockBundleAccessor(), this::captureString));
    }

    @Test
    public void givenNullOwnBundleVersionOfProductIsNull() {
        assertNull(VersionUtils.getProductVersion(new MockBundleAccessor()));
    }

    @Test
    public void givenEmptyContextVersionOfProductIsNull() {
        assertNull(VersionUtils.getProductVersion(new MockBundleAccessor(createOwnBundle())));
    }

    @Test
    public void givenSingletonContextVersionOfProductIsThatBundle() {
        final Version version = new Version("10.3.1");
        final Bundle versionInfoBundle = createBundle("de.rcenvironment.core.gui.branding.default.versioninfo", version);
        assertSame(version, VersionUtils.getProductVersion(new MockBundleAccessor(versionInfoBundle, versionInfoBundle)));
    }

    @Test
    public void givenNullContextVersionOfProductIsNull() {
        assertNull(VersionUtils.getProductVersion(new MockBundleAccessor()));
    }

    @Test
    public void givenLargeContextVersionOfProductIsThatBundle() {
        final Version versionA = new Version("10.3.1");
        final Version versionB = new Version("10.3.0");
        final Version versionC = new Version("10.4.1");
        final Bundle otherBundleA = createBundle("de.rcenvironment.core.components", versionA);
        final Bundle otherBundleB = createBundle("de.rcenvironment.core.gui.branding.default", versionB);
        final Bundle versionInfoBundle = createBundle("de.rcenvironment.core.gui.branding.default.versioninfo", versionC);
        assertSame(versionC, VersionUtils.getProductVersion(new MockBundleAccessor(otherBundleA, otherBundleB, versionInfoBundle)));
    }

    private static Bundle createOwnBundle(Bundle... bundlesInContext) {
        final Bundle bundle = EasyMock.createMock(Bundle.class);
        final BundleContext bundleContext = EasyMock.createMock(BundleContext.class);

        EasyMock.expect(bundleContext.getBundles()).andStubReturn(bundlesInContext);
        EasyMock.expect(bundle.getBundleContext()).andStubReturn(bundleContext);

        EasyMock.replay(bundle, bundleContext);

        return bundle;
    }

    private static Bundle createBundle(final String symbolicName, final Version version) {
        final Bundle bundle = EasyMock.createMock(Bundle.class);
        EasyMock.expect(bundle.getSymbolicName()).andStubReturn(symbolicName);
        EasyMock.expect(bundle.getVersion()).andStubReturn(version);
        EasyMock.replay(bundle);
        return bundle;
    }

    private void captureString(String param) {
        this.capturedString.setValue(param);
    }
}
