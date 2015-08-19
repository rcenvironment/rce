/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.integration;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;

import de.rcenvironment.core.utils.common.concurrent.TaskDescription;

/**
 * Handles the file watcher for the tool integration.
 * 
 * @author Sascha Zur
 */
public class IntegrationWatcher implements Runnable {

    private static final String INTEGRATION_WATCHER = "IntegrationWatcher: ";

    private static final int WAIT_TIME_AFTER_NEW_DIRECTORY_IS_CREATED = 500;

    private static final Log LOGGER = LogFactory.getLog(IntegrationWatcher.class);

    private static boolean watcherActive = true;

    private final WatchService watcher;

    private final ToolIntegrationContext context;

    private final ToolIntegrationService integrationService;

    private final ObjectMapper mapper = new ObjectMapper();

    public IntegrationWatcher(WatchService watcher, File folder, ToolIntegrationContext context,
        ToolIntegrationService integrationService) {
        this.watcher = watcher;
        this.context = context;
        this.integrationService = integrationService;
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
                LOGGER.error(INTEGRATION_WATCHER, e);
                return;
            }
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                if (isWatcherActive()) {
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    } else if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        try {
                            Thread.sleep(WAIT_TIME_AFTER_NEW_DIRECTORY_IS_CREATED);
                        } catch (InterruptedException e1) {
                            LOGGER.error(INTEGRATION_WATCHER + e1);
                        }
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path filename = ev.context();
                        File newTool =
                            new File(context.getRootPathToToolIntegrationDirectory() + File.separator
                                + context.getNameOfToolIntegrationDirectory(), filename.toString());
                        if (newTool.isDirectory()) {
                            Path toolPath =
                                FileSystems.getDefault().getPath(newTool.getAbsolutePath());
                            try {
                                toolPath.register(watcher, new Kind[] { StandardWatchEventKinds.ENTRY_CREATE,
                                    StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY });
                            } catch (IOException e) {
                                LOGGER.error(INTEGRATION_WATCHER + e);
                            }
                            File newConfiguration = new File(newTool, context.getConfigurationFilename());
                            if (newConfiguration.exists() && newConfiguration.isFile()) {

                                integrateFile(newConfiguration);
                            }
                        } else if (newTool.isFile()
                            && !newTool.getAbsolutePath().endsWith(ToolIntegrationConstants.PUBLISHED_COMPONENTS_FILENAME)) {
                            integrateFile(newTool);
                        }
                    } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path dir = (Path) key.watchable();
                        Path fullPath = dir.resolve(ev.context());
                        File toolFile = new File(fullPath.toString());
                        removeTool(toolFile);
                    } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path dir = (Path) key.watchable();
                        Path fullPath = dir.resolve(ev.context());
                        File modifiedFileOrFolder = new File(fullPath.toString());
                        if (modifiedFileOrFolder.isFile() && modifiedFileOrFolder.getName().equals(context.getConfigurationFilename())) {
                            removeTool(modifiedFileOrFolder);
                            integrateFile(modifiedFileOrFolder);
                        }
                        if (modifiedFileOrFolder.isFile()
                            && modifiedFileOrFolder.getName().equals(ToolIntegrationConstants.PUBLISHED_COMPONENTS_FILENAME)) {
                            if (modifiedFileOrFolder.exists() && watcherActive) {
                                integrationService.updatePublishedComponents(context);
                            }
                        }
                    }
                }
            }
            key.reset();
        }
    }

    private void removeTool(File removeConfig) {
        if (removeConfig.getName().endsWith(".json")) {
            removeConfig = removeConfig.getParentFile();
        }
        String toolName = integrationService.getToolNameToPath(removeConfig.getAbsolutePath());
        if (toolName != null) {
            integrationService.removeTool(toolName,
                context);
        }
    }

    private void integrateFile(File newConfiguration) {
        try {
            if (newConfiguration.getAbsolutePath().endsWith(".json")) {
                @SuppressWarnings("unchecked") Map<String, Object> configuration =
                    mapper.readValue(newConfiguration, new HashMap<String, Object>().getClass());
                integrationService.integrateTool(configuration, context);
                integrationService.putToolNameToPath((String) configuration.get(ToolIntegrationConstants.KEY_TOOL_NAME),
                    newConfiguration.getParentFile());
            }
        } catch (IOException e) {
            LOGGER.error(e);
        }
    }

    public static boolean isWatcherActive() {
        return watcherActive;
    }

    public static void setWatcherActive(boolean watcherActive) {
        IntegrationWatcher.watcherActive = watcherActive;
    }
}
