/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.integration.internal;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.component.integration.ToolIntegrationContext;
import de.rcenvironment.core.component.integration.ToolIntegrationService;
import de.rcenvironment.core.component.model.impl.ToolIntegrationConstants;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Implementation for a file watcher in tool integration.
 * 
 * @author Sascha Zur
 */
public class ToolIntegrationFileWatcher implements Runnable {

    protected static final Log LOGGER = LogFactory.getLog(ToolIntegrationFileWatcher.class);

    private static final int MAX_RETRIES_REGISTER_ON_CREATE = 5;

    private static final int MAX_RETRIES_INTEGRATE_NEW_FILE = 5;

    private static final int SLEEPING_TIME = 50;

    private WatchService watcher;

    private FileService fileService;

    private ToolIntegrationContext context;

    private ToolIntegrationService integrationService;

    private Map<WatchKey, Path> registeredKeys = new HashMap<>();

    private AtomicBoolean isActive = new AtomicBoolean(true);

    private Map<Path, Long> lastModified = new HashMap<>();

    private Path rootContextPath;

    private final ObjectMapper mapper;

    // Used to notify callers of #stop that the thread has finished.
    private CountDownLatch stoppingLatch = new CountDownLatch(1);

    /**
     *
     * @author Alexander Weinert
     */
    @Component(service = Factory.class)
    public static class Factory {

        private WatchService.Builder watchServiceBuilder;

        private FileService fileService;

        private ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();

        private ToolIntegrationService toolIntegrationService;

        /**
         * @param context The context of the tool whose configuration folder shall be watched by the constructed ToolIntegrationFileWatcher.
         * @return A file watcher that watches the configuration folder of the given tool integration.
         * @throws IOException If no {@link WatchService} can be constructed for the integration folder of the given tool.
         */
        public ToolIntegrationFileWatcher create(ToolIntegrationContext context)
            throws IOException {
            final Path rootContextPath = fileService.getPath(context.getRootPathToToolIntegrationDirectory(),
                context.getNameOfToolIntegrationDirectory());
            final ToolIntegrationFileWatcher fileWatcher =
                new ToolIntegrationFileWatcher(context, toolIntegrationService, rootContextPath, fileService, mapper,
                    watchServiceBuilder.build());
            return fileWatcher;
        }

        /**
         * We explicitly implement this method instead of only relying on OSGI annotations for creating this method in order to make it
         * accessible for testing. During normal operation, this method should not be called, but instead OSGI should be configured to
         * inject the correct implementation into the Factory.
         * 
         * @param newInstance The WatchService to be supplied to newly created {@link ToolIntegrationFileWatcher}s
         */
        @Reference
        public void bindWatchServiceBuilder(WatchService.Builder newInstance) {
            watchServiceBuilder = newInstance;
        }

        /**
         * We explicitly implement this method instead of only relying on OSGI annotations for creating this method in order to make it
         * accessible for testing. During normal operation, this method should not be called, but instead OSGI should be configured to
         * inject the correct implementation into the Factory.
         * 
         * @param newInstance The FileService to be supplied to newly created {@link ToolIntegrationFileWatcher}s
         */
        @Reference
        public void bindFileService(FileService newInstance) {
            fileService = newInstance;
        }

        public void setToolIntegrationService(final ToolIntegrationService newInstance) {
            toolIntegrationService = newInstance;
        }

        public void setObjectMapper(final ObjectMapper newInstance) {
            mapper = newInstance;
        }
    }

    protected ToolIntegrationFileWatcher(ToolIntegrationContext context, ToolIntegrationService integrationService,
        final Path rootContextPathParam, FileService fileServiceParam, ObjectMapper mapperParam, WatchService watchService) {
        this.context = context;
        this.integrationService = integrationService;
        this.rootContextPath = rootContextPathParam;
        this.fileService = fileServiceParam;
        this.mapper = mapperParam;
        this.watcher = watchService;
    }

    /**
     * Register a path and if it is a directory, do it recursively.
     * 
     * @param path to register
     * @throws IOException if registration fails.
     */
    public void registerRecursive(Path path) throws IOException {
        fileService.walkFileTree(path, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                try {
                    register(dir);
                } catch (IOException e) {
                    LOGGER.debug(StringUtils.format("Problem registering directory %s to watch service: %s", dir, e.getMessage()));
                }
                return FileVisitResult.CONTINUE;
            }
        });

    }

    /**
     * Register given path.
     * 
     * @param dir to register
     * @throws IOException if it fails.
     */
    public void register(Path dir) throws IOException {
        if (!registeredKeys.containsValue(dir)) {
            WatchKey key = watcher.watch(dir, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            LOGGER.debug(String.format("Watching %s", dir));
            registeredKeys.put(key, dir);
        }
    }

    /**
     * Remove a registered path and all sub pathes.
     * 
     * @param path to unregister.
     * @throws IOException if s.th. goes wrong
     */
    public void unregisterRecursive(Path path) throws IOException {
        List<WatchKey> remove = new LinkedList<>();
        for (WatchKey key : registeredKeys.keySet()) {
            if (registeredKeys.get(key).startsWith(path)) {
                remove.add(key);
                key.cancel();
            }
        }
        for (WatchKey key : remove) {
            registeredKeys.remove(key);
        }
    }

    /**
     * Set watcher active/inactive.
     * 
     * @param value true, if it should be active, else false.
     */
    public void setWatcherActive(boolean value) {
        isActive.set(value);
    }

    /**
     * Stops the watcher and ends the thread. May only be called after #run has been called
     */
    public void stop() {
        try {
            watcher.close();
            stoppingLatch.await(5, TimeUnit.SECONDS);
            // We have to reinitialize the countdown latch in case the FileWatcher is restarted
            stoppingLatch = new CountDownLatch(1);
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Error stopping watcher thread:", e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    @TaskDescription("Filewatcher for integration files")
    public void run() {
        boolean running = true;
        while (running) {
            WatchKey key = null;
            try {
                if (watcher != null) {
                    key = watcher.take();
                } else {
                    running = false;
                }
            } catch (InterruptedException e) {
                LOGGER.debug("Got interrupted waiting for watch keys. " + e.getMessage());
                return;
            } catch (ClosedWatchServiceException e) {
                running = false;
                LOGGER.debug("Shut down watcher for context " + context.getContextType());
                continue;
            }

            if (key == null) {
                LOGGER.debug("Got null WatchKey for FileWatcher of type " + context.getContextType());
                continue;
            }
            Path directory = registeredKeys.get(key);
            if (directory == null) {
                LOGGER.debug(StringUtils.format("Got unregistered WatchKey for FileWatcher of type %s", context.getContextType()));
                continue;
            }
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path name = ev.context();
                Path child = directory.resolve(name);
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }
                LOGGER.debug(StringUtils.format("Got event %s in context %s for file: %s", kind.name(), context.getContextType(),
                    child.toString()));
                if (kind == ENTRY_CREATE) {
                    handleCreate(child, directory);
                } else if (kind == ENTRY_DELETE) {
                    handleDelete(child, directory);
                } else if (kind == ENTRY_MODIFY) {
                    handleModify(child, directory);
                }
            }
            key.reset();
        }
        stoppingLatch.countDown();
    }

    private void handleCreate(Path child, Path directory) {
        boolean registered = false;
        int attempt = 0;
        while (!registered && attempt < MAX_RETRIES_REGISTER_ON_CREATE) {
            try {
                registerRecursive(child);
                registered = true;
            } catch (IOException x) {
                registered = false;
                LOGGER.error(StringUtils.format(
                    "Could not register new path (Tried %s of %s times): %s; Cause: %s", ++attempt, MAX_RETRIES_REGISTER_ON_CREATE,
                    child.toString(), x));
                try {
                    Thread.sleep(SLEEPING_TIME);
                } catch (InterruptedException e1) {
                    LOGGER.debug("Integration watcher sleep interrupted.", e1);
                }
            }
        }
        if (attempt == MAX_RETRIES_REGISTER_ON_CREATE) {
            LOGGER.error(
                StringUtils.format("Could not register new path after %s tries: %s", MAX_RETRIES_REGISTER_ON_CREATE, child.toString()));
        }
        if (isActive.get()) {
            if (fileService.isDirectory(child)) {
                if (child.getNameCount() == rootContextPath.getNameCount() + 1) {
                    File configurationFile = fileService.createFile(child.toFile(), context.getConfigurationFilename());
                    integrateFile(configurationFile);
                }
            } else if (fileService.isRegularFile(child)) {
                if (child.endsWith(ToolIntegrationConstants.PUBLISHED_COMPONENTS_FILENAME)) {
                    integrationService.updatePublishedComponents(context);
                } else if (child.endsWith(context.getConfigurationFilename())) {
                    integrateFile(child.toFile());
                } else if ((child.getName(child.getNameCount() - 2).endsWith(ToolIntegrationConstants.DOCS_DIR_NAME))) {
                    File configurationFile = new File(child.getParent().getParent().toFile(), context.getConfigurationFilename());
                    removeAndReintegrate(child.getParent().getParent().toFile(), configurationFile);
                }
            }
        } else {
            LOGGER.debug("Did not handle create because watcher is inactive.");
        }

    }

    private void handleDelete(Path child, Path directory) {
        try {
            unregisterRecursive(child);
        } catch (IOException x) {
            LOGGER.debug("Could not unregister path: " + child.toString(), x);
        }
        if (isActive.get()) {
            if (rootContextPath.equals(directory)) {
                if (child.endsWith(ToolIntegrationConstants.PUBLISHED_COMPONENTS_FILENAME)) {
                    integrationService.updatePublishedComponents(context);
                } else if (child.getNameCount() == rootContextPath.getNameCount() + 1) {
                    removeTool(child.toFile());
                }
            } else if (child.getName(child.getNameCount() - 1).endsWith(ToolIntegrationConstants.DOCS_DIR_NAME)) {
                File configurationFile = new File(child.getParent().toFile(), context.getConfigurationFilename());
                removeAndReintegrate(child.getParent().toFile(), configurationFile);
            } else if ((child.getName(child.getNameCount() - 2).endsWith(ToolIntegrationConstants.DOCS_DIR_NAME))) {
                File configurationFile = new File(child.getParent().getParent().toFile(), context.getConfigurationFilename());
                removeAndReintegrate(child.getParent().getParent().toFile(), configurationFile);
            }

        } else {
            LOGGER.debug("Did not handle delete because watcher is inactive.");
        }

    }

    private void handleModify(Path child, Path directory) {
        if (isActive.get()) {
            final int waitingTime = 200;
            long currentTime = System.currentTimeMillis();
            if (lastModified.get(child) == null) {
                modify(child, directory);
            } else {
                // Since the file watcher throws two modify events for changed content and changed
                // timestop, one must be ignored
                if (currentTime - lastModified.get(child).longValue() > waitingTime) {
                    modify(child, directory);
                } else {
                    LOGGER.debug("Skipped modify event because of too frequent calls for " + child.getFileName());
                }
            }
            lastModified.put(child, currentTime);
        }
    }

    private void modify(Path child, Path directory) {
        try {
            final int sleepTime = 150;
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            LOGGER.debug("Sleeping for modify event interrupted");
        }
        if (child.endsWith(ToolIntegrationConstants.PUBLISHED_COMPONENTS_FILENAME)) {
            integrationService.updatePublishedComponents(context);
        }
        if (directory.getNameCount() == rootContextPath.getNameCount() + 1) {
            File configurationFile = fileService.createFile(directory.toFile(), context.getConfigurationFilename());
            removeAndReintegrate(directory.toFile(), configurationFile);
        }
        if (directory.getName(directory.getNameCount() - 1).endsWith(ToolIntegrationConstants.DOCS_DIR_NAME)) {
            File configurationFile = fileService.createFile(directory.getParent().toFile(), context.getConfigurationFilename());
            removeAndReintegrate(directory.getParent().toFile(), configurationFile);
        }
    }

    private void removeAndReintegrate(File toRemove, File toIntegrate) {
        LOGGER.debug("Reload tool configuration for " + toRemove.getName());
        removeTool(toRemove);
        integrateFile(toIntegrate);
    }

    private void removeTool(File toolDir) {
        LOGGER.debug(StringUtils.format("Unregistering integrated tool %s", toolDir.getAbsolutePath()));
        String toolName = integrationService.getToolNameToPath(toolDir.getAbsolutePath());
        if (toolName != null) {
            integrationService.removeTool(toolName, context);
        }
    }

    private void integrateFile(File newConfiguration) {
        LOGGER.debug(StringUtils.format("Trying to integrate tool from configuration file %s", newConfiguration.getAbsolutePath()));
        boolean read = false;
        int attempt = 0;
        while (!read && attempt < MAX_RETRIES_INTEGRATE_NEW_FILE) {
            try {
                if (newConfiguration.exists() && newConfiguration.getAbsolutePath().endsWith(".json")) {
                    @SuppressWarnings("unchecked") Map<String, Object> configuration =
                        mapper.readValue(newConfiguration, new HashMap<String, Object>().getClass());
                    integrationService.integrateTool(configuration, context);
                    integrationService.putToolNameToPath((String) configuration.get(ToolIntegrationConstants.KEY_TOOL_NAME),
                        newConfiguration.getParentFile());
                    read = true;
                } else {
                    LOGGER.debug(StringUtils.format("Configuration file does not exist or is no json file: %s",
                        newConfiguration.getAbsolutePath()));
                    read = true; // cancel while
                }
            } catch (IOException e) {
                read = false;
                LOGGER.debug(
                    StringUtils.format("Could not read tool configuration (Tried %s of %s times)", ++attempt,
                        MAX_RETRIES_INTEGRATE_NEW_FILE),
                    e);
                try {
                    Thread.sleep(SLEEPING_TIME);
                } catch (InterruptedException e1) {
                    LOGGER.error("Integration watcher sleep interrupted.");
                }
            }
        }
        if (attempt == MAX_RETRIES_INTEGRATE_NEW_FILE) {
            LOGGER.error(StringUtils.format("Could not read tool configuration after %s times. Path: %s", MAX_RETRIES_INTEGRATE_NEW_FILE,
                newConfiguration.getAbsolutePath()));
        }
    }

    public Map<WatchKey, Path> getRegisteredPaths() {
        return registeredKeys;
    }
}
