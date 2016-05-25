/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.integration.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.integration.ToolIntegrationContext;
import de.rcenvironment.core.component.integration.ToolIntegrationService;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;

/**
 * Manager for the filewatcher of all types of integrations.
 * 
 * @author Sascha Zur
 */
public class ToolIntegrationFileWatcherManager {

    private static final Log LOGGER = LogFactory.getLog(ToolIntegrationFileWatcherManager.class);

    private static final String COULD_NOT_CREATE_A_WATCH_SERVICE_FOR_THE_FILE = "Could not create a WatchService for the file: ";

    private ToolIntegrationService integrationService;

    private Map<ToolIntegrationContext, ToolIntegrationFileWatcher> watchers;

    public ToolIntegrationFileWatcherManager(ToolIntegrationService integrationService) {
        this.integrationService = integrationService;
        this.watchers = new HashMap<>();
    }

    /**
     * Create watcher for tools folder (e.g. at startup).
     * 
     * @param context to add new watcher to
     */
    public void createWatcherForToolRootDirectory(ToolIntegrationContext context) {
        File integrationRootFolder =
            new File(context.getRootPathToToolIntegrationDirectory(), context.getNameOfToolIntegrationDirectory());
        if (!integrationRootFolder.exists()) {
            integrationRootFolder.mkdirs();
        }
        try {
            Path toolIntegrationPath =
                FileSystems.getDefault().getPath(integrationRootFolder.getAbsolutePath());

            ToolIntegrationFileWatcher integrationWatcher = new ToolIntegrationFileWatcher(context,
                integrationService);
            SharedThreadPool.getInstance().execute(integrationWatcher, "FileWatcher " + context.getContextId());
            integrationWatcher.registerRecursive(toolIntegrationPath);
            watchers.put(context, integrationWatcher);
            LOGGER.debug("Created new watcher for context " + context.getContextType());
        } catch (IOException e) {
            LOGGER.warn(COULD_NOT_CREATE_A_WATCH_SERVICE_FOR_THE_FILE + integrationRootFolder.getAbsolutePath(), e);
        }
    }

    /**
     * Unregister watcher for the given context.
     * 
     * @param context to unregister
     */
    public void unregisterRootDirectory(ToolIntegrationContext context) {
        File integrationRootFolder =
            new File(context.getRootPathToToolIntegrationDirectory(), context.getNameOfToolIntegrationDirectory());
        ToolIntegrationFileWatcher integrationWatcher = watchers.get(context);

        if (integrationWatcher != null) {
            try {
                integrationWatcher.unregisterRecursive(FileSystems.getDefault().getPath(integrationRootFolder.getAbsolutePath()));
            } catch (IOException e) {
                LOGGER.warn("Could not unregister integration root folder: " + integrationRootFolder.getAbsolutePath(), e);
            }
            integrationWatcher.stop();
            watchers.remove(integrationWatcher);
        }
        LOGGER.debug("Unregistered watcher for context " + context.getContextId());
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
            File path = new File(new File(integrationContext.getRootPathToToolIntegrationDirectory(),
                integrationContext.getNameOfToolIntegrationDirectory()), previousToolName);
            try {
                watchers.get(integrationContext).unregisterRecursive(FileSystems.getDefault().getPath(path.getAbsolutePath()));
            } catch (IOException e) {
                LOGGER.debug("Could not unregister tool directory of tool " + previousToolName, e);
            }
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
            File path = new File(new File(integrationContext.getRootPathToToolIntegrationDirectory(),
                integrationContext.getNameOfToolIntegrationDirectory()), toolName);
            try {
                watchers.get(integrationContext).registerRecursive(FileSystems.getDefault().getPath(path.getAbsolutePath()));
            } catch (IOException e) {
                LOGGER.debug("Could not register tool directory of tool " + toolName, e);
            }
        }
    }

}
