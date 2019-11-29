/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.integration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.component.integration.internal.FileService;
import de.rcenvironment.core.component.integration.internal.RunnerService;
import de.rcenvironment.core.component.integration.internal.ToolIntegrationFileWatcher;
import de.rcenvironment.core.component.integration.internal.ToolIntegrationFileWatcherManager;
import de.rcenvironment.core.component.integration.internal.ToolIntegrationFileWatcherManager.Builder;

/**
 * Test the {@link ToolIntegrationFileWatcherManager}.
 * 
 * @author Alexander Weinert
 */
public class ToolIntegrationFileWatcherManagerTest {

    /**
     * Tests that createWatcherForRootDirectory works as expected.
     * 
     * @throws IOException Is not thrown, since we only call methods that may throw this exception on mocked objects.
     */
    @Test
    public void testCreateWatcher() throws IOException {
        final Builder managerBuilder = new ToolIntegrationFileWatcherManager.Builder();

        final FileService fileService = EasyMock.createMock(FileService.class);

        final ToolIntegrationFileWatcher.Factory fileWatcherFactory = EasyMock.createMock(ToolIntegrationFileWatcher.Factory.class);
        final ToolIntegrationService integrationService = EasyMock.createMock(ToolIntegrationService.class);

        final RunnerService runnerService = EasyMock.createMock(RunnerService.class);

        managerBuilder.bindFileService(fileService);
        managerBuilder.bindRunnerService(runnerService);
        managerBuilder.bindFileWatcherFactory(fileWatcherFactory);
        
        fileWatcherFactory.setToolIntegrationService(integrationService);
        EasyMock.expectLastCall();
        EasyMock.replay(fileWatcherFactory);

        final ToolIntegrationFileWatcherManager manager = managerBuilder.build(integrationService);

        EasyMock.reset(fileWatcherFactory);

        final String rootpath = "rootpath";
        final String toolIntegrationDir1 = "toolIntegrationDir";
        final String absolutePath1 = String.format("/%s/%s", rootpath, toolIntegrationDir1);
        final Path integrationRootFolderPath1 = EasyMock.createMock(Path.class);

        EasyMock.expect(fileService.getPath(absolutePath1)).andStubReturn(integrationRootFolderPath1);

        final ToolIntegrationContext context = EasyMock.createMock("context", ToolIntegrationContext.class);
        EasyMock.expect(context.getRootPathToToolIntegrationDirectory()).andStubReturn(rootpath);
        EasyMock.expect(context.getNameOfToolIntegrationDirectory()).andStubReturn(toolIntegrationDir1);
        EasyMock.expect(context.getContextId()).andStubReturn("context");
        EasyMock.expect(context.getContextType()).andStubReturn("test context");
        EasyMock.replay(context);

        final File integrationRootFolderFile = EasyMock.createMock(File.class);
        EasyMock.expect(fileService.createFile(rootpath, toolIntegrationDir1)).andReturn(integrationRootFolderFile);
        EasyMock.expect(integrationRootFolderFile.exists()).andStubReturn(Boolean.TRUE);
        EasyMock.expect(integrationRootFolderFile.getAbsolutePath()).andStubReturn(absolutePath1);
        EasyMock.replay(fileService, integrationRootFolderFile);

        final ToolIntegrationFileWatcher watcher = EasyMock.createMock("watcher", ToolIntegrationFileWatcher.class);
        EasyMock.expect(fileWatcherFactory.create(context)).andStubReturn(watcher);
        EasyMock.replay(fileWatcherFactory);

        watcher.registerRecursive(integrationRootFolderPath1);
        EasyMock.expectLastCall();
        EasyMock.replay(watcher);

        runnerService.execute(watcher, "FileWatcher context");
        EasyMock.expectLastCall();
        EasyMock.replay(runnerService);

        manager.createWatcherForToolRootDirectory(context);
        
        EasyMock.verify(watcher, fileWatcherFactory, integrationRootFolderFile, context, fileService, runnerService);
    }

}
