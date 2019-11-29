/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.component.integration.ToolIntegrationContext;
import de.rcenvironment.core.component.integration.ToolIntegrationContextRegistry;
import de.rcenvironment.core.component.integration.ToolIntegrationService;
import de.rcenvironment.core.component.model.impl.ToolIntegrationConstants;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Opens the dialog to remove registered integrated component.
 * 
 * @author Sascha Zur
 */
public class ShowIntegrationRemoveHandler extends AbstractHandler {

    private static final Log LOGGER = LogFactory.getLog(ShowIntegrationRemoveHandler.class);

    @Override
    public Object execute(final ExecutionEvent event) throws ExecutionException {
        ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        ToolIntegrationService integrationService = serviceRegistryAccess.getService(ToolIntegrationService.class);
        ToolIntegrationContextRegistry registry = serviceRegistryAccess.getService(ToolIntegrationContextRegistry.class);
        RemoveToolIntegrationDialog dialog =
            new RemoveToolIntegrationDialog(null, integrationService.getActiveComponentIds(), registry.getAllIntegrationContexts());

        if (dialog.open() == 0) {
            integrationService.setFileWatcherActive(false);
            for (String selectedTool : dialog.getSelectedTools()) {
                Map<String, Object> configuration = integrationService.getToolConfiguration(selectedTool);
                ToolIntegrationContext context = null;
                if (configuration != null) {
                    Object integrationTypeProperty = configuration.get(ToolIntegrationConstants.INTEGRATION_TYPE);
                    if (integrationTypeProperty == null) {
                        context = registry.getToolIntegrationContextById(ToolIntegrationConstants.COMMON_TOOL_INTEGRATION_CONTEXT_UID);
                    } else {
                        context = registry.getToolIntegrationContextByType(integrationTypeProperty.toString());
                    }
                    if (context != null) {
                        String toolname = selectedTool.substring(context.getPrefixForComponentId().length());
                        integrationService.unregisterIntegration(toolname, context);
                        integrationService.removeTool(selectedTool, context);
                        File remove =
                            new File(integrationService.getPathOfComponentID(selectedTool, context));
                        removeOrDeactive(integrationService, dialog, context, remove);

                    } else {
                        LOGGER.error("ToolintegrationContext is null.");
                    }
                }
            }
        }
        integrationService.setFileWatcherActive(true);
        return null;
    }

    private void removeOrDeactive(ToolIntegrationService integrationService, RemoveToolIntegrationDialog dialog,
        ToolIntegrationContext context, File remove) {
        if (!dialog.getKeepOnDisk()) {
            if (remove.exists() && remove.canWrite()) {
                try {
                    FileUtils.forceDelete(remove);
                } catch (IOException e) {
                    LOGGER.error("Could not delete tool directory: ", e);
                }
            }
        } else if (remove.exists()) {
            ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
            try {
                @SuppressWarnings("unchecked") Map<String, Object> configurationMap =
                    mapper.readValue(new File(remove, context.getConfigurationFilename()),
                        new HashMap<String, Object>().getClass());
                configurationMap.put(ToolIntegrationConstants.IS_ACTIVE, false);
                integrationService.writeToolIntegrationFile(configurationMap, context);
            } catch (IOException e) {
                LOGGER.error("Failed to set tool inactive ( " + remove + "): ", e);
            }
        }
    }
}
