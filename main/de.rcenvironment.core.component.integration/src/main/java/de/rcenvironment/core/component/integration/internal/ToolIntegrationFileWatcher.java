/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.integration.internal;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;

import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.component.integration.ToolIntegrationContext;
import de.rcenvironment.core.component.integration.ToolIntegrationService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;

/**
 * Implementation for a file watcher in tool integration.
 * 
 * @author Sascha Zur
 */
public class ToolIntegrationFileWatcher implements Runnable {

    private static final Log LOGGER = LogFactory.getLog(ToolIntegrationFileWatcher.class);

    private WatchService watcher;

    private ToolIntegrationContext context;

    private ToolIntegrationService integrationService;

    private Map<WatchKey, Path> registeredKeys;

    private AtomicBoolean isActive = new AtomicBoolean(true);

    private Map<Path, Long> lastModified;

    private Path rootContextPath;

    private ObjectMapper mapper = new ObjectMapper();

    public ToolIntegrationFileWatcher(ToolIntegrationContext context, ToolIntegrationService integrationService) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.context = context;
        this.integrationService = integrationService;
        this.registeredKeys = new HashMap<WatchKey, Path>();
        this.lastModified = new HashMap<>();
        this.rootContextPath =
            FileSystems.getDefault().getPath(context.getRootPathToToolIntegrationDirectory(), context.getNameOfToolIntegrationDirectory());
    }

    /**
     * Register a path and if it is a directory, do it recursively.
     * 
     * @param path to register
     * @throws IOException if registration fails.
     */
    public void registerRecursive(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                throws IOException {
                register(dir);
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
            WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
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

    @SuppressWarnings("unchecked")
    @Override
    @TaskDescription("Filewatcher for integration files")
    public void run() {
        boolean valid = true;
        while (valid) {
            WatchKey key = null;
            try {
                if (watcher != null) {
                    key = watcher.take();
                } else {
                    valid = false;
                }
            } catch (InterruptedException e) {
                LOGGER.error("Got interrupted waiting for watch keys", e);
                return;
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
                } else {
                    LOGGER.debug(String.format("Got event %s in context %s for file: %s", kind.name(), context.getContextType(),
                        child.toString()));
                    if (kind == ENTRY_CREATE) {
                        handleCreate(child, directory);
                    } else if (kind == ENTRY_DELETE) {
                        handleDelete(child, directory);
                    } else if (kind == ENTRY_MODIFY) {
                        handleModify(child, directory);
                    }
                }
            }
            key.reset();
        }
    }

    private void handleCreate(Path child, Path directory) {
        try {
            registerRecursive(child);
        } catch (IOException x) {
            LOGGER.debug("Could not register new path: " + child.toString());
        }
        if (isActive.get()) {
            if (Files.isDirectory(child)) {
                if (child.getNameCount() == rootContextPath.getNameCount() + 1) {
                    File configurationFile = new File(child.toFile(), context.getConfigurationFilename());
                    integrateFile(configurationFile);
                }
            } else if (Files.isRegularFile(child)) {
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
            File configurationFile = new File(directory.toFile(), context.getConfigurationFilename());
            removeAndReintegrate(directory.toFile(), configurationFile);
        }
        if (directory.getName(directory.getNameCount() - 1).endsWith(ToolIntegrationConstants.DOCS_DIR_NAME)) {
            File configurationFile = new File(directory.getParent().toFile(), context.getConfigurationFilename());
            removeAndReintegrate(directory.getParent().toFile(), configurationFile);
        }
    }

    private void removeAndReintegrate(File toRemove, File toIntegrate) {
        LOGGER.debug("Reload tool configuration for " + toRemove.getName());
        removeTool(toRemove);
        integrateFile(toIntegrate);
    }

    private void removeTool(File toolDir) {
        String toolName = integrationService.getToolNameToPath(toolDir.getAbsolutePath());
        if (toolName != null) {
            integrationService.removeTool(toolName, context);
        }
    }

    private void integrateFile(File newConfiguration) {
        try {
            if (newConfiguration.exists() && newConfiguration.getAbsolutePath().endsWith(".json")) {
                @SuppressWarnings("unchecked") Map<String, Object> configuration =
                    mapper.readValue(newConfiguration, new HashMap<String, Object>().getClass());
                integrationService.integrateTool(configuration, context);
                integrationService.putToolNameToPath((String) configuration.get(ToolIntegrationConstants.KEY_TOOL_NAME),
                    newConfiguration.getParentFile());
            }
        } catch (IOException e) {
            LOGGER.error("Could not read tool configuration", e);
        }
    }
}
