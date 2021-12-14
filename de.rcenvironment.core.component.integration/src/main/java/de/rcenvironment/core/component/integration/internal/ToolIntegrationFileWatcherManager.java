/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.integration.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.component.integration.ToolIntegrationContext;
import de.rcenvironment.core.component.integration.ToolIntegrationService;

/**
 * Manager for the filewatcher of all types of integrations.
 * 
 * @author Sascha Zur
 * @author Robert Mischke
 */
public class ToolIntegrationFileWatcherManager {

    private static final String FOUND_NO_WATCHER_FOR_CONTEXT = "Found no watcher for context ";

    private static final String COULD_NOT_CREATE_A_WATCH_SERVICE_FOR_THE_FILE = "Could not create a WatchService for the file: ";

    private final Map<ToolIntegrationContext, ToolIntegrationFileWatcher> watchers = Collections.synchronizedMap(new HashMap<>());

    private ToolIntegrationFileWatcher.Factory fileWatcherFactory;

    private RunnerService runnerService;

    private FileService fileService;

    private final Log log = LogFactory.getLog(ToolIntegrationFileWatcherManager.class);

    /**
     * We implement a builder for this class in order to allow for supplying a reference to a ToolIntegrationService upon construction of a
     * ToolIntegrationFileWatcherManager.
     * 
     * @author Alexander Weinert
     */
    @Component(service = ToolIntegrationFileWatcherManager.Builder.class)
    public static class Builder {

        private ToolIntegrationFileWatcher.Factory fileWatcherFactory;

        private RunnerService runnerService;

        private FileService fileService;

        /**
         * 
         * @param integrationService The {@link ToolIntegrationService} to be used by the manager when constructing new
         *        {@link ToolIntegrationFileWatcher}s.
         * @return An instance of {@link ToolIntegrationFileWatcherManager}.
         */
        public ToolIntegrationFileWatcherManager build(ToolIntegrationService integrationService) {
            final ToolIntegrationFileWatcherManager returnValue = new ToolIntegrationFileWatcherManager();

            fileWatcherFactory.setToolIntegrationService(integrationService);

            returnValue.setFileWatcherFactory(fileWatcherFactory);
            returnValue.setRunnerService(runnerService);
            returnValue.setFileService(fileService);

            return returnValue;
        }

        /**
         * We explicitly implement this method in order to make it accessible for testing. During normal operation, this method should not
         * be called.
         * 
         * @param newInstance The {@link ToolIntegrationFileWatcherFactory} to be bound to this factory. Must not be null.
         */
        @Reference
        public void bindFileWatcherFactory(ToolIntegrationFileWatcher.Factory newInstance) {
            this.fileWatcherFactory = newInstance;
        }

        /**
         * We explicitly implement this method in order to make it accessible for testing. During normal operation, this method should not
         * be called.
         * 
         * @param newInstance The {@link RunnerService} to be bound to this factory. Must not be null.
         */
        @Reference
        public void bindRunnerService(RunnerService newInstance) {
            this.runnerService = newInstance;
        }

        /**
         * We explicitly implement this method in order to make it accessible for testing. During normal operation, this method should not
         * be called.
         * 
         * @param newInstance The {@link FileService} to be bound to this factory. Must not be null.
         */
        @Reference
        public void bindFileService(FileService newInstance) {
            this.fileService = newInstance;
        }

    }

    // We hide the default constructor in order to enforce creation via the factory
    protected ToolIntegrationFileWatcherManager() {}

    /**
     * Create watcher for tools folder (e.g. at startup).
     * 
     * @param context to add new watcher to
     */
    public void createWatcherForToolRootDirectory(ToolIntegrationContext context) {
        File integrationRootFolder = constructToolFolderFile(context);
        if (!integrationRootFolder.exists()) {
            integrationRootFolder.mkdirs();
        }
        try {
            ToolIntegrationFileWatcher integrationWatcher = fileWatcherFactory.create(context);
            runnerService.execute("Filewatcher for integration files", integrationWatcher, "FileWatcher " + context.getContextId());

            final Path toolIntegrationPath = constructPath(integrationRootFolder);
            integrationWatcher.registerRecursive(toolIntegrationPath);
            watchers.put(context, integrationWatcher);
            log.debug("Created new watcher for context " + context.getContextType());
        } catch (IOException e) {
            log.warn(COULD_NOT_CREATE_A_WATCH_SERVICE_FOR_THE_FILE + integrationRootFolder.getAbsolutePath(), e);
        }
    }

    private File constructToolFolderFile(ToolIntegrationContext context) {
        return fileService.createFile(context.getRootPathToToolIntegrationDirectory(), context.getNameOfToolIntegrationDirectory());
    }

    private Path constructPath(File file) {
        return fileService.getPath(file.getAbsolutePath());
    }

    /**
     * Unregister watcher for the given context.
     * 
     * @param context to unregister
     */
    public void unregisterRootDirectory(ToolIntegrationContext context) {
        File integrationRootFolder = constructToolFolderFile(context);
        ToolIntegrationFileWatcher integrationWatcher = watchers.get(context);

        if (integrationWatcher != null) {
            try {
                integrationWatcher.unregisterRecursive(constructPath(integrationRootFolder));
            } catch (IOException e) {
                log.warn("Could not unregister integration root folder: " + integrationRootFolder.getAbsolutePath(), e);
            }
            integrationWatcher.stop();
            watchers.remove(context);
            log.debug("Unregistered watcher for context " + context.getContextId());
        } else {
            log.warn(FOUND_NO_WATCHER_FOR_CONTEXT + context.getContextId());
        }
    }

    /**
     * Set watcher active or inactive.
     * 
     * @param active value
     */
    public void setAllWatcherActivity(boolean active) {
        for (ToolIntegrationFileWatcher current : watchers.values()) {
            current.setWatcherActive(active);
        }
    }

    /**
     * Unregister a tool directory completely.
     * 
     * @param previousToolName to unregister
     * @param integrationContext of the tool.
     */
    public void unregister(String previousToolName, ToolIntegrationContext integrationContext) {
        if (watchers.get(integrationContext) != null) {
            File path = new File(constructToolFolderFile(integrationContext), previousToolName);
            try {
                watchers.get(integrationContext).unregisterRecursive(constructPath(path));
            } catch (IOException e) {
                log.debug("Could not unregister tool directory of tool " + previousToolName, e);
            }
        } else {
            log.warn(FOUND_NO_WATCHER_FOR_CONTEXT + integrationContext.getContextId());
        }
    }

    /**
     * Register a tool directory completely.
     * 
     * @param toolName to unregister
     * @param integrationContext of the tool.
     */
    public void registerRecursive(String toolName, ToolIntegrationContext integrationContext) {
        if (watchers.get(integrationContext) != null) {
            File path = new File(constructToolFolderFile(integrationContext), toolName);
            try {
                watchers.get(integrationContext).registerRecursive(constructPath(path));
            } catch (IOException e) {
                log.debug("Could not register tool directory of tool " + toolName, e);
            }
        } else {
            log.warn(FOUND_NO_WATCHER_FOR_CONTEXT + integrationContext.getContextId());
        }
    }

    /**
     * Shut down all tool integration watcher active.
     */
    public void shutdown() {
        log.debug("Shutting down file watchers");
        List<ToolIntegrationContext> contexts = new ArrayList<>(watchers.keySet()); // avoid concurrent modification
        for (ToolIntegrationContext context : contexts) {
            unregisterRootDirectory(context);
        }
        // consistency check
        if (watchers.size() != 0) {
            log.error("Unexpected state: Remaining registered watchers after deregistration");
        }
    }

    private void setFileWatcherFactory(ToolIntegrationFileWatcher.Factory fileWatcherFactory) {
        this.fileWatcherFactory = fileWatcherFactory;
    }

    private void setRunnerService(RunnerService runnerService) {
        this.runnerService = runnerService;
    }

    private void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

}
