/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.integration;

import java.io.File;
import java.nio.file.Path;

import org.easymock.EasyMock;

/**
 * Helper class that simplifies constructing a mocked path that does not represent an actual path on the file system.
 * 
 * @author Alexander Weinert
 */
public class MockPathBuilder {

    private final Path path;

    public MockPathBuilder() {
        path = EasyMock.createMock(Path.class);
    }

    public MockPathBuilder(String name) {
        path = EasyMock.createMock(name, Path.class);
    }

    /**
     * Let the constructed Path answer calls to path.resolve(pathToResolve) with result. In contrast to the EasyMock-language, verification
     * does not fail if the resolve-method is not called.
     * 
     * @param pathToResolve The path which shall be resolved against the constructed path.
     * @param result The result to be returned by the resolution.
     * @return This builder-object for daisy-chaining.
     */
    public MockPathBuilder expectResolve(Path pathToResolve, Path result) {
        EasyMock.expect(path.resolve(pathToResolve)).andStubReturn(result);
        return this;
    }

    /**
     * @param nameCount The name count of the constructed path.
     * @return This builder-object for daisy-chaining.
     */
    public MockPathBuilder nameCount(int nameCount) {
        EasyMock.expect(path.getNameCount()).andStubReturn(nameCount);
        return this;
    }

    /**
     * @param file The file represented by the constructed path.
     * @return This builder-object for daisy-chaining.
     */
    public MockPathBuilder file(File file) {
        EasyMock.expect(path.toFile()).andStubReturn(file);
        return this;
    }

    /**
     * @param retVal The desired result of the call path.startsWith(path).
     * @return This builder-object for daisy-chaining.
     */
    public MockPathBuilder startsWithSelf(final boolean retVal) {
        EasyMock.expect(path.startsWith(path)).andStubReturn(retVal);
        return this;
    }

    /**
     * @param suffix The potential suffix of the constructed path.
     * @param retVal The desired return value of the call path.endsWith(suffix).
     * @return This builder-object for daisy-chaining.
     */
    public MockPathBuilder endsWith(String suffix, boolean retVal) {
        EasyMock.expect(path.endsWith(suffix)).andStubReturn(retVal);
        return this;
    }

    /**
     * @param index The index of the name to be retrieved.
     * @param retVal The desired return value of the call path.getName(index).
     * @return This builder-object for daisy-chaining.
     */
    public MockPathBuilder expectGetName(int index, Path retVal) {
        EasyMock.expect(path.getName(index)).andReturn(retVal);
        return this;
    }

    /**
     * @param prefix The potential prefix of the constructed path.
     * @param retVal The desired return value of the call path.startsWith(prefix).
     * @return This builder-object for daisy-chaining.
     */
    public MockPathBuilder startsWith(Path prefix, boolean retVal) {
        EasyMock.expect(path.startsWith(prefix)).andReturn(retVal);
        return this;
    }

    /**
     * @return The constructed mocked path.
     */
    public Path build() {
        EasyMock.replay(path);
        return path;
    }

}
