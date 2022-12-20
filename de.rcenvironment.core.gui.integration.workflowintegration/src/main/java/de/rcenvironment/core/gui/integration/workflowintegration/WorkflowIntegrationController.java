/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.integration.workflowintegration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.component.integration.IntegrationConstants;
import de.rcenvironment.core.component.integration.IntegrationContext;
import de.rcenvironment.core.component.integration.IntegrationContextType;
import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.component.integration.workflow.WorkflowConfigurationMap;
import de.rcenvironment.core.component.integration.workflow.WorkflowIntegrationConstants;
import de.rcenvironment.core.component.integration.workflow.WorkflowIntegrationService;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputDatumHandling;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputExecutionContraint;
import de.rcenvironment.core.component.workflow.execution.api.PersistentWorkflowDescriptionLoaderService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileException;
import de.rcenvironment.core.component.workflow.execution.spi.WorkflowDescriptionLoaderCallback;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.gui.integration.common.CommonIntegrationController;
import de.rcenvironment.core.gui.integration.workflowintegration.editor.WorkflowIntegrationEditorConstants;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;
import de.rcenvironment.core.workflow.execution.function.EndpointAdapter;
import de.rcenvironment.core.workflow.execution.function.EndpointAdapterToJsonConverter;
import de.rcenvironment.core.workflow.execution.function.EndpointAdapters;
import de.rcenvironment.core.workflow.execution.function.EndpointAdapters.Builder;

/**
 * Extension of {@link CommonIntegrationController} as controller class for workflow configurations.
 * 
 * @author Kathrin Schaffert
 * @author Jan Flink
 */
public class WorkflowIntegrationController extends CommonIntegrationController {

    public enum ConfigurationContext {
        /**
         * Configuration context for launch settings.
         */
        LAUNCH_SETTINGS,

        /**
         * Configuration context for common settings.
         */
        COMMON_SETTINGS
    }

    private static final Log LOG = LogFactory.getLog(WorkflowIntegrationController.class);

    private WorkflowIntegrationService integrationService;

    private boolean editMode = false;

    private WorkflowDescription workflowDescription;

    private Optional<String> originalName = Optional.empty();

    public WorkflowIntegrationController(String componentName) throws FileNotFoundException {
        super();
        this.originalName = Optional.of(componentName);
        integrationService = ServiceRegistry.createAccessFor(this).getService(WorkflowIntegrationService.class);
        setValue(IntegrationConstants.INTEGRATION_TYPE, IntegrationContextType.WORKFLOW.toString(),
            ConfigurationContext.COMMON_SETTINGS);
        loadWorkflowIntegration(componentName);
    }

    public WorkflowIntegrationController() {
        super();
        integrationService = ServiceRegistry.createAccessFor(this).getService(WorkflowIntegrationService.class);
        setValue(IntegrationConstants.INTEGRATION_TYPE, IntegrationContextType.WORKFLOW.toString(),
            ConfigurationContext.COMMON_SETTINGS);
        setDefaultLaunchSettings();
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    @Override
    public void instantiateModel() {
        setConfigurationModel(new WorkflowConfigurationMap());
    }

    public WorkflowConfigurationMap getWorkflowConfigurationModel() {
        return (WorkflowConfigurationMap) configurationModel;
    }

    private void setDefaultLaunchSettings() {
        Map<String, String> launchSettings = new HashMap<>();
        launchSettings.put(IntegrationConstants.KEY_HOST, "RCE");
        launchSettings.put(ToolIntegrationConstants.KEY_ROOT_WORKING_DIRECTORY, "");
        launchSettings.put(ToolIntegrationConstants.KEY_TOOL_DIRECTORY, ".");
        launchSettings.put(IntegrationConstants.KEY_VERSION, "0.0");
        launchSettings.put(IntegrationConstants.KEY_LIMIT_INSTANCES, "true");
        launchSettings.put(IntegrationConstants.KEY_LIMIT_INSTANCES_COUNT, "10");
        setLaunchSettings(launchSettings);
    }

    public void setValue(String key, Object value, ConfigurationContext context) {
        if (context.equals(ConfigurationContext.COMMON_SETTINGS)) {
            configurationModel.getRawConfigurationMap().put(key, value);
        } else if (context.equals(ConfigurationContext.LAUNCH_SETTINGS)) {
            Map<String, String> launchSettings = configurationModel.getFirstLaunchSettings();
            launchSettings.put(key, (String) value);
            setLaunchSettings(launchSettings);
        }
    }

    private void setLaunchSettings(Map<String, String> launchSettings) {
        List<Map<String, String>> list = new LinkedList<>();
        list.add(launchSettings);
        configurationModel.getRawConfigurationMap().put(IntegrationConstants.KEY_LAUNCH_SETTINGS, list);
    }

    @SuppressWarnings("unchecked")
    private void loadWorkflowIntegration(String componentName) throws FileNotFoundException {

        IntegrationContext context = integrationContextRegistry.getToolIntegrationContextByType(IntegrationContextType.WORKFLOW.toString());

        if (context == null) {
            LOG.error(StringUtils.format(
                "The Workflow Integration Editor cannot be displayed. No Integration Context of the expected type "
                    + "\"%s\" of the Component \"%s\" exists.",
                IntegrationContextType.WORKFLOW.toString(), componentName));
            return;
        }

        String rootPath = context.getRootPathToToolIntegrationDirectory();
        String integrationDir = context.getNameOfToolIntegrationDirectory();

        Path configFilePath = Paths.get(rootPath, integrationDir, componentName, context.getConfigurationFilename());

        File configFile = configFilePath.toFile();
        if (!configFile.exists()) {
            throw new FileNotFoundException(configFilePath.toString());
        }

        try {
            this.configurationModel.setRawConfigurationMap(JsonUtils.getDefaultObjectMapper().readValue(configFile, HashMap.class));
        } catch (IOException e) {
            throw new RuntimeException(
                StringUtils.format("Error reading the configuration file of the integration \"%s\"", componentName),
                e);
        }
        Path workflowFilePath =
            Paths.get(rootPath, integrationDir, componentName, WorkflowIntegrationConstants.INTEGRATED_WORKFLOW_FILENAME);
        setWorkflowDescriptionFromFile(workflowFilePath.toFile());

    }

    private WorkflowDescription loadWorkflowDescriptionFromFile(File workflowFile) throws FileNotFoundException {
        ServiceRegistryAccess serviceRegistry = ServiceRegistry.createAccessFor(this);
        PersistentWorkflowDescriptionLoaderService loaderService =
            serviceRegistry.getService(PersistentWorkflowDescriptionLoaderService.class);
        final WorkflowDescriptionLoaderCallback loaderCallback = new WorkflowDescriptionLoaderCallback() {

            @Override
            public boolean arePartlyParsedWorkflowConsiderValid() {
                return false;
            }

            @Override
            public void onNonSilentWorkflowFileUpdated(String message, String backupFilename) {
                // Intentionally left empty
            }

            @Override
            public void onSilentWorkflowFileUpdated(String message) {
                // Intentionally left empty
            }

            @Override
            public void onWorkflowFileParsingPartlyFailed(String backupFilename) {
                // Intentionally left empty
            }
        };
        if (!workflowFile.exists()) {
            throw new FileNotFoundException(workflowFile.toString());
        }
        try {
            return loaderService.loadWorkflowDescriptionFromFile(workflowFile, loaderCallback);
        } catch (WorkflowFileException e) {
            LOG.error("Error reading the workflow file.", e);
        }
        return null;
    }

    public void createEndpointAdapters(List<EndpointAdapter> endpointAdapters) {
        Builder adapters = new EndpointAdapters.Builder();
        for (EndpointAdapter adapter : endpointAdapters) {
            adapters.addEndpointAdapter(adapter);
        }

        final EndpointAdapterToJsonConverter endpointAdapterConverter = new EndpointAdapterToJsonConverter(adapters.build());

        configurationModel.getRawConfigurationMap().put(IntegrationConstants.KEY_ENDPOINT_INPUTS,
            endpointAdapterConverter.toInputDefinitions());

        configurationModel.getRawConfigurationMap().put(IntegrationConstants.KEY_ENDPOINT_OUTPUTS,
            endpointAdapterConverter.toOutputDefinitions());

        final List<Map<String, Object>> endpointAdapterConfiguration = endpointAdapterConverter.toEndpointAdapterDefinitions();
        try {
            final String endpointAdaptersConfigurationString = new ObjectMapper().writeValueAsString(endpointAdapterConfiguration);
            configurationModel.getRawConfigurationMap().put(WorkflowIntegrationConstants.KEY_ENDPOINT_ADAPTERS,
                endpointAdaptersConfigurationString);
        } catch (JsonProcessingException e) {
            LOG.error("Error creating endpoint adapters for workflow integration.", e);
        }
    }

    public List<EndpointAdapter> getPersistedEndpointAdapters() {
        try {
            final List<Map<String, String>> endpointAdapterDefinitions = getWorkflowConfigurationModel().getEndpointAdapters();
            return endpointAdapterDefinitions.stream().map(
                entry -> {
                    if (entry.get(WorkflowIntegrationConstants.KEY_ENDPOINT_ADAPTER_TYPE)
                        .equals(WorkflowIntegrationConstants.VALUE_ENDPOINT_ADAPTER_INPUT)) {
                        return EndpointAdapter.inputAdapterBuilder()
                            .internalEndpointName(entry.get(WorkflowIntegrationConstants.KEY_ENDPOINT_ADAPTER_INTERNAL_NAME))
                            .externalEndpointName(entry.get(WorkflowIntegrationConstants.KEY_ENDPOINT_ADAPTER_EXTERNAL_NAME))
                            .workflowNodeIdentifier(entry.get(WorkflowIntegrationConstants.KEY_ENDPOINT_ADAPTER_NODE_IDENTIFIER))
                            .inputHandling(
                                InputDatumHandling.valueOf(entry.get(WorkflowIntegrationConstants.KEY_ENDPOINT_ADAPTER_INPUT_HANDLING)))
                            .inputExecutionConstraint(InputExecutionContraint
                                .valueOf(entry.get(WorkflowIntegrationConstants.KEY_ENDPOINT_ADAPTER_INPUT_EXECUTION_CONSTRAINT)))
//                    .dataType(DataType.byDisplayName(entry.get(WorkflowIntegrationConstants.KEY_ENDPOINT_ADAPTER_DATA_TYPE_KEY)))
                            .build();
                    }
                    return EndpointAdapter.outputAdapterBuilder()
                        .internalEndpointName(entry.get(WorkflowIntegrationConstants.KEY_ENDPOINT_ADAPTER_INTERNAL_NAME))
                        .externalEndpointName(entry.get(WorkflowIntegrationConstants.KEY_ENDPOINT_ADAPTER_EXTERNAL_NAME))
                        .workflowNodeIdentifier(entry.get(WorkflowIntegrationConstants.KEY_ENDPOINT_ADAPTER_NODE_IDENTIFIER))
//                .dataType(DataType.byDisplayName(entry.get(WorkflowIntegrationConstants.KEY_ENDPOINT_ADAPTER_DATA_TYPE_KEY)))
                        .build();
                }).collect(Collectors.toList());

        } catch (JsonProcessingException e) {
            // TODO: handle catch
            LOG.error(e.getStackTrace());
        }
        return null;
    }

    public boolean integrateWorkflow() {
        try {
            integrationService.integrateWorkflowFileAsComponent(getWorkflowDescription(),
                getConfigurationModel(), originalName);
        } catch (IOException e) {
            LOG.error("Error integrating component.", e);
            return false;
        }
        return true;
    }

    public WorkflowDescription getWorkflowDescription() {
        return this.workflowDescription;
    }

    public void setWorkflowDescription(WorkflowDescription workflowDescription) {
        this.workflowDescription = workflowDescription;
    }

    public void setWorkflowDescriptionFromFile(File workflowFile) throws FileNotFoundException {
        WorkflowDescription workflowDec = loadWorkflowDescriptionFromFile(workflowFile);
        setWorkflowDescription(workflowDec);
        if (getWorkflowDescription().getName() == null) {
            getWorkflowDescription().setName(workflowFile.getName());
        }
    }

    public String getEditorTitle() {
        String workflowName = getWorkflowDescription().getName();
        String componentName = getConfigurationModel().getToolName();
        StringBuilder builder = new StringBuilder(WorkflowIntegrationEditorConstants.EDITOR_TITLE);
        if (componentName != null && !componentName.isEmpty()) {
            builder.append(StringUtils.format(WorkflowIntegrationEditorConstants.EDITOR_COMPONENT_NAME_TEMPLATE, componentName));
        } else if (workflowName != null && !workflowName.isEmpty()) {
            builder.append(StringUtils.format(WorkflowIntegrationEditorConstants.EDITOR_WORKFLOW_NAME_TEMPLATE, workflowName));
        }
        return builder.toString();
    }

    public Optional<String> getOriginalName() {
        return originalName;
    }

}
