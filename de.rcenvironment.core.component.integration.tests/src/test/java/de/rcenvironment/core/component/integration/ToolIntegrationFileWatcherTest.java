/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.integration;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.component.integration.internal.FileService;
import de.rcenvironment.core.component.integration.internal.MockFileBuilder;
import de.rcenvironment.core.component.integration.internal.ToolIntegrationFileWatcher;
import de.rcenvironment.core.component.integration.internal.ToolIntegrationFileWatcher.Factory;
import de.rcenvironment.core.component.model.impl.ToolIntegrationConstants;
import de.rcenvironment.core.component.integration.internal.WatchService;

/**
 * Test that ToolIntegrationFileWatcher emits the correct callbacks when receiving events.
 * 
 * @author Alexander Weinert
 */
public class ToolIntegrationFileWatcherTest {

    private static final String PUBLISHED_CONF = "published.conf";

    private static final String CONFIGURATION_JSON = "configuration.json";

    private static final String MOCK_CONTEXT = "mockContext";

    private static final String MOCK_TOOL = "mockTool";

    private static final String INTEGRATION_DIR_NAME = "toolintegrationdir";

    private static final String PATH_TO_INTEGRATION_DIR = "/path/to/integration/dir";

    private static final String INTEGRATION_DIR_FULL_PATH = String.format("%s/%s", PATH_TO_INTEGRATION_DIR, INTEGRATION_DIR_NAME);

    private IMocksControl control;

    private ToolIntegrationContext context;

    private Factory watcherFactory;

    private WatchService watchService;

    private FileService fileService;

    private ToolIntegrationService integrationService;

    private ObjectMapper objectMapper;

    /**
     * Run before every execution of a test case.
     * @throws IOException Is never thrown, as we only call methods that might throw this exception on mock objects.
     */
    @Before
    public void setUp() throws IOException {
        control = EasyMock.createControl();

        watcherFactory = new ToolIntegrationFileWatcher.Factory();

        watchService = control.createMock(WatchService.class);
        final WatchService.Builder watchServiceBuilder = control.createMock(WatchService.Builder.class);
        EasyMock.expect(watchServiceBuilder.build()).andStubReturn(watchService);
        watcherFactory.bindWatchServiceBuilder(watchServiceBuilder);

        fileService = control.createMock(FileService.class);
        watcherFactory.bindFileService(fileService);

        integrationService = control.createMock(ToolIntegrationService.class);
        watcherFactory.setToolIntegrationService(integrationService);

        objectMapper = control.createMock(ObjectMapper.class);
        watcherFactory.setObjectMapper(objectMapper);

        context = control.createMock(ToolIntegrationContext.class);
    }

    /**
     * Test that an active {@link ToolIntegrationFileWatcher} registers all subfiles in a newly created folder and processes them.
     * 
     * @throws IOException Is never thrown, as we only call methods that might throw this exception on mock objects.
     * @throws InterruptedException Is never thrown, as we only call methods that might throw this exception on mock objects.
     */
    @Test
    public void testInterruptedException() throws IOException, InterruptedException {
        context = new MockToolIntegrationContextBuilder()
            .rootPathToToolIntegrationDirectory(PATH_TO_INTEGRATION_DIR)
            .nameOfToolIntegrationDirectory(INTEGRATION_DIR_NAME)
            .build();

        EasyMock.expect(fileService.getPath(PATH_TO_INTEGRATION_DIR, INTEGRATION_DIR_NAME)).andReturn(EasyMock.createMock(Path.class));

        final ToolIntegrationFileWatcher watcher = buildWatcher();

        expectTakeAndThrowInterruptedException();

        control.replay();
        watcher.run();
        control.verify();

    }

    private void expectTakeAndThrowInterruptedException() throws InterruptedException {
        EasyMock.expect(watchService.take()).andThrow(new InterruptedException());
    }

    /**
     * Test that an activated file watcher handles the creation of a folder correctly.
     * 
     * @throws InterruptedException Never thrown, as we only call methods that could throw this exception on mocks.
     * @throws IOException Never thrown, as we only call methods that could throw this exception on mocks.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateDirectoryActive() throws InterruptedException, IOException {

        context = new MockToolIntegrationContextBuilder()
            .rootPathToToolIntegrationDirectory(PATH_TO_INTEGRATION_DIR)
            .nameOfToolIntegrationDirectory(INTEGRATION_DIR_NAME)
            .contextType(MOCK_CONTEXT)
            .configurationFilename(CONFIGURATION_JSON)
            .build();
        final Path integrationDirPath = new MockPathBuilder().nameCount(2).build();
        EasyMock.expect(fileService.getPath(PATH_TO_INTEGRATION_DIR, INTEGRATION_DIR_NAME)).andReturn(integrationDirPath);

        final ToolIntegrationFileWatcher watcher = buildWatcher();

        final File createdFile = new MockFileBuilder().build();
        final Path createdPath = new MockPathBuilder().nameCount(3).file(createdFile).build();

        final File watchedFile = new MockFileBuilder().build();
        final Path watchedPath = new MockPathBuilder().expectResolve(createdPath, createdPath).build();

        final WatchKey watchKey = expectRegistration(watchedPath);

        control.replay();
        watcher.register(watchedPath);
        control.verify();
        control.reset();

        expectTakeAndReturn(watchKey);

        expectPollAndReturn(watchKey, buildCreateEvent(createdPath));
        final Capture<SimpleFileVisitor<Path>> visitorCapture = expectFileTreeWalk(createdPath);

        setIsDirectory(createdPath);

        final File configurationFile =
            new MockFileBuilder().exists(true).absolutePath("/home/test_user/configuration.json").parentFile(watchedFile).build();
        EasyMock.expect(fileService.createFile(createdFile, CONFIGURATION_JSON)).andReturn(configurationFile);

        final Map<String, Object> configurationMap = new HashMap<>();
        configurationMap.put(ToolIntegrationConstants.KEY_TOOL_NAME, MOCK_TOOL);
        // We should expect some subclass of Map.class here. However, defining a custom matcher for this case appears to be a lot of
        // overhead for only a small gain in testing precision.
        EasyMock.expect(objectMapper.readValue(EasyMock.eq(configurationFile), EasyMock.anyObject(Class.class)))
            .andReturn(configurationMap);

        expectToolRegistration(watchedFile, MOCK_TOOL);

        expectReset(watchKey);
        expectTakeAndThrowInterruptedException();

        control.replay();
        watcher.run();
        control.verify();
        control.reset();

        expectRegistration(createdPath);

        control.replay();
        final SimpleFileVisitor<Path> visitor = visitorCapture.getValue();
        visitor.preVisitDirectory(createdPath, EasyMock.createMock(BasicFileAttributes.class));
        control.verify();
    }

    /**
     * Test that a deactivated file watcher ignores the creation of a folder.
     * 
     * @throws InterruptedException Never thrown, as we only call methods that could throw this exception on mocks.
     * @throws IOException Never thrown, as we only call methods that could throw this exception on mocks.
     */
    @Test
    public void testCreateDirectoryInactive() throws InterruptedException, IOException {

        context = new MockToolIntegrationContextBuilder()
            .rootPathToToolIntegrationDirectory(PATH_TO_INTEGRATION_DIR)
            .nameOfToolIntegrationDirectory(INTEGRATION_DIR_NAME)
            .contextType(MOCK_CONTEXT)
            .configurationFilename(CONFIGURATION_JSON)
            .build();
        final Path integrationDirPath = new MockPathBuilder().nameCount(2).build();
        EasyMock.expect(fileService.getPath(PATH_TO_INTEGRATION_DIR, INTEGRATION_DIR_NAME)).andReturn(integrationDirPath);

        final ToolIntegrationFileWatcher watcher = buildWatcher();

        watcher.setWatcherActive(false);

        final File createdFile = new MockFileBuilder().build();
        final Path createdPath = new MockPathBuilder().nameCount(3).file(createdFile).build();

        final Path watchedPath = new MockPathBuilder().expectResolve(createdPath, createdPath).build();

        final WatchKey watchKey = expectRegistration(watchedPath);

        control.replay();
        watcher.register(watchedPath);
        control.verify();
        control.reset();

        expectTakeAndReturn(watchKey);

        expectPollAndReturn(watchKey, buildCreateEvent(createdPath));
        final Capture<SimpleFileVisitor<Path>> visitorCapture = expectFileTreeWalk(createdPath);

        expectReset(watchKey);
        expectTakeAndThrowInterruptedException();

        control.replay();
        watcher.run();
        control.verify();
        control.reset();

        expectRegistration(createdPath);

        control.replay();
        final SimpleFileVisitor<Path> visitor = visitorCapture.getValue();
        visitor.preVisitDirectory(createdPath, EasyMock.createMock(BasicFileAttributes.class));
        control.verify();

    }

    /**
     * Test that an activated file watcher handles the creation of the configuration file correctly.
     * 
     * @throws InterruptedException Never thrown, as we only call methods that could throw this exception on mocks.
     * @throws IOException Never thrown, as we only call methods that could throw this exception on mocks.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateConfigurationFile() throws InterruptedException, IOException {

        context = new MockToolIntegrationContextBuilder()
            .rootPathToToolIntegrationDirectory(PATH_TO_INTEGRATION_DIR)
            .nameOfToolIntegrationDirectory(INTEGRATION_DIR_NAME)
            .contextType(MOCK_CONTEXT)
            .configurationFilename(CONFIGURATION_JSON)
            .build();
        final Path integrationDirPath = new MockPathBuilder().nameCount(2).build();
        EasyMock.expect(fileService.getPath(PATH_TO_INTEGRATION_DIR, INTEGRATION_DIR_NAME)).andReturn(integrationDirPath);

        final ToolIntegrationFileWatcher watcher = buildWatcher();

        final File watchedFile = new MockFileBuilder().build();
        final File createdFile = new MockFileBuilder()
            .exists(true)
            .absolutePath(PATH_TO_INTEGRATION_DIR + '/' + CONFIGURATION_JSON)
            .parentFile(watchedFile)
            .build();
        final Path createdPath = new MockPathBuilder()
            .nameCount(3)
            .file(createdFile)
            .endsWith(PUBLISHED_CONF, false)
            .endsWith(CONFIGURATION_JSON, true)
            .build();

        final Path watchedPath = new MockPathBuilder().expectResolve(createdPath, createdPath).build();

        final WatchKey watchKey = expectRegistration(watchedPath);

        control.replay();
        watcher.register(watchedPath);
        control.verify();
        control.reset();

        expectTakeAndReturn(watchKey);

        expectPollAndReturn(watchKey, buildCreateEvent(createdPath));
        final Capture<SimpleFileVisitor<Path>> visitorCapture = expectFileTreeWalk(createdPath);

        setIsNotDirectory(createdPath);
        setIsRegularFile(createdPath);

        final Map<String, Object> configurationMap = new HashMap<>();
        configurationMap.put(ToolIntegrationConstants.KEY_TOOL_NAME, MOCK_TOOL);
        // We should expect some subclass of Map.class here. However, defining a custom matcher for this case appears to be a lot of
        // overhead for only a small gain in testing precision.
        EasyMock.expect(objectMapper.readValue(EasyMock.eq(createdFile), EasyMock.anyObject(Class.class)))
            .andReturn(configurationMap);

        expectToolRegistration(watchedFile, MOCK_TOOL);

        expectReset(watchKey);
        expectTakeAndThrowInterruptedException();

        control.replay();
        watcher.run();
        control.verify();
        control.reset();
        
        expectRegistration(createdPath);

        control.replay();
        final SimpleFileVisitor<Path> visitor = visitorCapture.getValue();
        visitor.preVisitDirectory(createdPath, EasyMock.createMock(BasicFileAttributes.class));
        control.verify();
    }

    /**
     * Test that an activated file watcher handles the modification of the configuration file correctly.
     * 
     * @throws InterruptedException Never thrown, as we only call methods that could throw this exception on mocks.
     * @throws IOException Never thrown, as we only call methods that could throw this exception on mocks.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testModifyConfigurationFile() throws InterruptedException, IOException {

        context = new MockToolIntegrationContextBuilder()
            .rootPathToToolIntegrationDirectory(PATH_TO_INTEGRATION_DIR)
            .nameOfToolIntegrationDirectory(INTEGRATION_DIR_NAME)
            .contextType(MOCK_CONTEXT)
            .configurationFilename(CONFIGURATION_JSON)
            .build();
        final Path integrationDirPath = new MockPathBuilder("integrationDirPath").nameCount(2).build();
        EasyMock.expect(fileService.getPath(PATH_TO_INTEGRATION_DIR, INTEGRATION_DIR_NAME)).andReturn(integrationDirPath);

        final ToolIntegrationFileWatcher watcher = buildWatcher();

        final File watchedFile = new MockFileBuilder("watchedFile")
            .name(INTEGRATION_DIR_NAME)
            .absolutePath(INTEGRATION_DIR_FULL_PATH)
            .build();
        final File modifiedFile = new MockFileBuilder("modifiedFile")
            .exists(true)
            .absolutePath(String.format("%s/%s/configuration.json", PATH_TO_INTEGRATION_DIR, INTEGRATION_DIR_NAME))
            .parentFile(watchedFile)
            .build();
        final Path modifiedPath = new MockPathBuilder("modifiedPath")
            .nameCount(3)
            .file(modifiedFile)
            .endsWith(PUBLISHED_CONF, false)
            .endsWith(CONFIGURATION_JSON, true)
            .build();

        final Path watchedPath = new MockPathBuilder("watchedPath")
            .expectResolve(modifiedPath, modifiedPath)
            .nameCount(3)
            .expectGetName(2, new MockPathBuilder("dir")
                .endsWith("docs", false)
                .build())
            .file(watchedFile)
            .build();

        final WatchKey watchKey = expectRegistration(watchedPath);

        control.replay();
        watcher.register(watchedPath);
        control.verify();
        control.reset();

        expectTakeAndReturn(watchKey);

        expectPollAndReturn(watchKey, buildModifyEvent(modifiedPath));

        EasyMock.expect(fileService.createFile(watchedFile, CONFIGURATION_JSON)).andReturn(modifiedFile);
        EasyMock.expect(integrationService.getToolNameToPath(INTEGRATION_DIR_FULL_PATH))
            .andReturn(MOCK_TOOL);
        integrationService.removeTool(MOCK_TOOL, context);
        EasyMock.expectLastCall();

        final Map<String, Object> configurationMap = new HashMap<>();
        configurationMap.put(ToolIntegrationConstants.KEY_TOOL_NAME, MOCK_TOOL);
        // We should expect some subclass of Map.class here. However, defining a custom matcher for this case appears to be a lot of
        // overhead for only a small gain in testing precision.
        EasyMock.expect(objectMapper.readValue(EasyMock.eq(modifiedFile), EasyMock.anyObject(Class.class)))
            .andReturn(configurationMap);

        expectToolRegistration(watchedFile, MOCK_TOOL);

        expectReset(watchKey);
        expectTakeAndThrowInterruptedException();

        control.replay();
        watcher.run();
        control.verify();
    }

    /**
     * Test that an activated file watcher handles the deletion of the configuration file correctly.
     * 
     * @throws InterruptedException Never thrown, as we only call methods that could throw this exception on mocks.
     * @throws IOException Never thrown, as we only call methods that could throw this exception on mocks.
     */
    @Test
    public void testDeleteConfigurationFile() throws InterruptedException, IOException {

        context = new MockToolIntegrationContextBuilder()
            .rootPathToToolIntegrationDirectory(PATH_TO_INTEGRATION_DIR)
            .nameOfToolIntegrationDirectory(INTEGRATION_DIR_NAME)
            .contextType(MOCK_CONTEXT)
            .configurationFilename(CONFIGURATION_JSON)
            .build();

        final File deletedFile = new MockFileBuilder("watchedFile")
            .name(INTEGRATION_DIR_NAME)
            .absolutePath(INTEGRATION_DIR_FULL_PATH)
            .build();
        final Path deletedPath = new MockPathBuilder("deletedPath")
            .nameCount(3)
            .file(deletedFile)
            .endsWith(PUBLISHED_CONF, false)
            .endsWith(CONFIGURATION_JSON, false)
            .build();

        final Path integrationDirPath =
            new MockPathBuilder("integrationDirPath").nameCount(2).expectResolve(deletedPath, deletedPath).startsWith(deletedPath, false)
                .build();
        EasyMock.expect(fileService.getPath(PATH_TO_INTEGRATION_DIR, INTEGRATION_DIR_NAME)).andReturn(integrationDirPath);

        final ToolIntegrationFileWatcher watcher = buildWatcher();

        final WatchKey watchKey = expectRegistration(integrationDirPath);

        control.replay();
        watcher.register(integrationDirPath);
        control.verify();
        control.reset();

        expectTakeAndReturn(watchKey);

        expectPollAndReturn(watchKey, buildDeleteEvent(deletedPath));

        EasyMock.expect(integrationService.getToolNameToPath(INTEGRATION_DIR_FULL_PATH))
            .andReturn(MOCK_TOOL);
        integrationService.removeTool(MOCK_TOOL, context);
        EasyMock.expectLastCall();

        expectReset(watchKey);
        expectTakeAndThrowInterruptedException();

        control.replay();
        watcher.run();
        control.verify();
    }

    private void setIsRegularFile(Path path) {
        EasyMock.expect(fileService.isRegularFile(path)).andReturn(true);
    }

    private void expectReset(final WatchKey watchKey) {
        EasyMock.expect(watchKey.reset()).andReturn(true);
    }

    private void expectToolRegistration(final File watchedFile, final String toolName) {
        integrationService.integrateTool(EasyMock.anyObject(), EasyMock.eq(context));
        EasyMock.expectLastCall();
        integrationService.putToolNameToPath(toolName, watchedFile);
        EasyMock.expectLastCall();
    }

    private void setIsDirectory(final Path path) {
        EasyMock.expect(fileService.isDirectory(path)).andReturn(true);
    }

    private void setIsNotDirectory(final Path path) {
        EasyMock.expect(fileService.isDirectory(path)).andReturn(false);
    }

    private Capture<SimpleFileVisitor<Path>> expectFileTreeWalk(final Path createdPath) throws IOException {
        final Capture<SimpleFileVisitor<Path>> capture = Capture.newInstance();
        fileService.walkFileTree(EasyMock.eq(createdPath), EasyMock.capture(capture));
        EasyMock.expectLastCall();
        return capture;
    }

    private void expectTakeAndReturn(final WatchKey watchKey) throws InterruptedException {
        EasyMock.expect(watchService.take()).andReturn(watchKey);
    }

    private WatchKey expectRegistration(final Path watchedPath) throws IOException {
        final WatchKey watchKey = control.createMock(WatchKey.class);
        EasyMock
            .expect(
                watchService.watch(watchedPath, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY))
            .andReturn(watchKey);
        return watchKey;
    }

    private ToolIntegrationFileWatcher buildWatcher() throws IOException {
        control.replay();
        final ToolIntegrationFileWatcher watcher = watcherFactory.create(context);
        control.verify();
        control.reset();
        return watcher;
    }

    private WatchEvent<Path> buildCreateEvent(final Path createdPath) {
        @SuppressWarnings("unchecked") final WatchEvent<Path> event = EasyMock.createMock(WatchEvent.class);

        EasyMock.expect(event.kind()).andStubReturn(ENTRY_CREATE);
        EasyMock.expect(event.context()).andStubReturn(createdPath);

        EasyMock.replay(event);

        return event;
    }

    private WatchEvent<Path> buildModifyEvent(Path modifiedPath) {
        @SuppressWarnings("unchecked") final WatchEvent<Path> event = EasyMock.createMock(WatchEvent.class);

        EasyMock.expect(event.kind()).andStubReturn(ENTRY_MODIFY);
        EasyMock.expect(event.context()).andStubReturn(modifiedPath);

        EasyMock.replay(event);

        return event;
    }

    private WatchEvent<?> buildDeleteEvent(Path deletedPath) {
        @SuppressWarnings("unchecked") final WatchEvent<Path> event = EasyMock.createMock(WatchEvent.class);

        EasyMock.expect(event.kind()).andStubReturn(ENTRY_DELETE);
        EasyMock.expect(event.context()).andStubReturn(deletedPath);

        EasyMock.replay(event);

        return event;
    }

    private void expectPollAndReturn(WatchKey key, WatchEvent<?>... events) {
        final List<WatchEvent<?>> eventList = Arrays.asList(events);
        EasyMock.expect(key.pollEvents()).andReturn(eventList);
    }

}
