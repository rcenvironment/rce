/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.testutils;

import java.io.File;
import java.util.Map;

import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.ConfigurationService;

/**
 * Common test/mock implementations of {@link ConfigurationService}. These can be used directly, or can as superclasses for custom mock
 * classes.
 * 
 * Custom mock implementations of {@link ConfigurationService} should use these as superclasses whenever possible to avoid code duplication,
 * and to shield the mock classes from irrelevant API changes.
 * 
 * @author Robert Mischke
 */
public abstract class MockConfigurationService implements ConfigurationService {

    /**
     * A mock implementation of {@link CommunicationService} that throws an exception on every method call. Subclasses for tests should
     * override the methods they expect to be called.
     * 
     * @author Robert Mischke
     */
    @Deprecated
    // TODO use EasyMock instances instead
    public static class ThrowExceptionByDefault implements ConfigurationService {

        private static final String MOCK_INSTANCE_INVOCATION_MESSAGE = "Mock instance invoked";

        @Override
        public void addSubstitutionProperties(String namespace, Map<String, String> properties) {
            throw new UnsupportedOperationException(MOCK_INSTANCE_INVOCATION_MESSAGE);
        }

        @Override
        public <T> T getConfiguration(String identifier, Class<T> clazz) {
            throw new UnsupportedOperationException(MOCK_INSTANCE_INVOCATION_MESSAGE);
        }

        @Override
        public ConfigurationSegment getConfigurationSegment(String relativePath) {
            throw new UnsupportedOperationException(MOCK_INSTANCE_INVOCATION_MESSAGE);
        }

        @Override
        public void reloadConfiguration() {
            throw new UnsupportedOperationException(MOCK_INSTANCE_INVOCATION_MESSAGE);
        }

        @Override
        public String resolveBundleConfigurationPath(String identifier, String path) {
            throw new UnsupportedOperationException(MOCK_INSTANCE_INVOCATION_MESSAGE);
        }

        @Override
        public File[] getConfigurablePathList(ConfigurablePathListId pathListId) {
            throw new UnsupportedOperationException(MOCK_INSTANCE_INVOCATION_MESSAGE);
        }

        @Override
        public String getInstanceName() {
            throw new UnsupportedOperationException(MOCK_INSTANCE_INVOCATION_MESSAGE);
        }

        @Override
        public boolean getIsWorkflowHost() {
            throw new UnsupportedOperationException(MOCK_INSTANCE_INVOCATION_MESSAGE);
        }

        @Override
        public boolean getIsRelay() {
            throw new UnsupportedOperationException(MOCK_INSTANCE_INVOCATION_MESSAGE);
        }

        @Override
        public File getProfileDirectory() {
            throw new UnsupportedOperationException(MOCK_INSTANCE_INVOCATION_MESSAGE);
        }

        @Override
        public File getProfileConfigurationFile() {
            throw new UnsupportedOperationException(MOCK_INSTANCE_INVOCATION_MESSAGE);
        }

        @Override
        public File getConfigurablePath(ConfigurablePathId pathId) {
            throw new UnsupportedOperationException(MOCK_INSTANCE_INVOCATION_MESSAGE);
        }

        @Override
        public File initializeSubDirInConfigurablePath(ConfigurablePathId pathId, String relativePath) {
            throw new UnsupportedOperationException(MOCK_INSTANCE_INVOCATION_MESSAGE);
        }

        @Override
        public File getOriginalProfileDirectory() {
            throw new UnsupportedOperationException(MOCK_INSTANCE_INVOCATION_MESSAGE);
        }

        @Override
        public File getParentTempDirectoryRoot() {
            throw new UnsupportedOperationException(MOCK_INSTANCE_INVOCATION_MESSAGE);
        }

        @Override
        public boolean isUsingIntendedProfileDirectory() {
            throw new UnsupportedOperationException(MOCK_INSTANCE_INVOCATION_MESSAGE);
        }

        @Override
        public boolean isUsingDefaultConfigurationValues() {
            throw new UnsupportedOperationException(MOCK_INSTANCE_INVOCATION_MESSAGE);
        }

        @Override
        public File getInstallationDir() {
            throw new UnsupportedOperationException(MOCK_INSTANCE_INVOCATION_MESSAGE);
        }
    }
}
