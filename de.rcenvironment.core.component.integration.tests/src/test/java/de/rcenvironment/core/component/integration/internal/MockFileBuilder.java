/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.integration.internal;

import java.io.File;

import org.easymock.EasyMock;

/**
 * Utility class for building a mock of a File-object for testing.
 * 
 * @author Alexander Weinert
 */
public class MockFileBuilder {

    private final String mockIdentifier;

    private String name = null;

    private Boolean exists = null;

    private Boolean isFile = null;

    private Boolean isAbsolute = null;

    private Long lastModified = null;

    private String absolutePath = null;

    private File parentFile = null;

    public MockFileBuilder() {
        mockIdentifier = null;
    }

    public MockFileBuilder(String mockIdentifierParam) {
        mockIdentifier = mockIdentifierParam;
    }

    /**
     * @param nameParam The desired return value of a call to file.getName()
     * @return This builder-object for daisy-chaining.
     */
    public MockFileBuilder name(String nameParam) {
        this.name = nameParam;
        return this;
    }

    /**
     * @param existsParam The desired return value of a call to file.exists()
     * @return This builder-object for daisy-chaining.
     */
    public MockFileBuilder exists(boolean existsParam) {
        this.exists = existsParam;
        return this;
    }

    /**
     * @param isFileParam The desired return value of a call to file.exists()
     * @return This builder-object for daisy-chaining.
     */
    public MockFileBuilder isFile(boolean isFileParam) {
        this.isFile = isFileParam;
        return this;
    }

    /**
     * @param isAbsoluteParam The desired return value of a call to file.isAbsolute()
     * @return This builder-object for daisy-chaining.
     */
    public MockFileBuilder isAbsolute(boolean isAbsoluteParam) {
        this.isAbsolute = isAbsoluteParam;
        return this;
    }

    /**
     * @param lastModifiedParam The desired return value of a call to file.getLastModified()
     * @return This builder-object for daisy-chaining.
     */
    public MockFileBuilder lastModified(long lastModifiedParam) {
        this.lastModified = lastModifiedParam;
        return this;
    }

    /**
     * @param absolutePathParam The desired return value of a call to file.getAbsolutePath()
     * @return This builder-object for daisy-chaining.
     */
    public MockFileBuilder absolutePath(String absolutePathParam) {
        absolutePath = absolutePathParam;
        return this;
    }

    /**
     * @param parentFileParam The desired return value of a call to file.getParentFile()
     * @return This builder-object for daisy-chaining.
     */
    public MockFileBuilder parentFile(File parentFileParam) {
        parentFile = parentFileParam;
        return this;
    }

    /**
     * @return A mock of {@link File} that behaves as configured by previous calls to this object.
     */
    public File build() {
        final File file;
        if (mockIdentifier != null) {
            file = EasyMock.createMock(mockIdentifier, File.class);
        } else {
            file = EasyMock.createMock(File.class);
        }

        if (name != null) {
            EasyMock.expect(file.getName()).andStubReturn(name);
        }

        if (exists != null) {
            EasyMock.expect(file.exists()).andStubReturn(exists);
        }

        if (isFile != null) {
            EasyMock.expect(file.isFile()).andStubReturn(isFile);
        }

        if (isAbsolute != null) {
            EasyMock.expect(file.isAbsolute()).andStubReturn(isAbsolute);
        }

        if (lastModified != null) {
            EasyMock.expect(file.lastModified()).andStubReturn(lastModified);
        }

        if (absolutePath != null) {
            EasyMock.expect(file.getAbsolutePath()).andStubReturn(absolutePath);
        }

        if (parentFile != null) {
            EasyMock.expect(file.getParentFile()).andStubReturn(parentFile);
        }

        EasyMock.replay(file);

        return file;
    }
}
