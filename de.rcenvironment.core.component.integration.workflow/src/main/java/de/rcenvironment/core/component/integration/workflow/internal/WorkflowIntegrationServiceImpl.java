/*
 * Copyright 2020-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.integration.workflow.internal;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.component.integration.ConfigurationMap;
import de.rcenvironment.core.component.integration.IntegrationConstants;
import de.rcenvironment.core.component.integration.IntegrationContextType;
import de.rcenvironment.core.component.integration.ToolIntegrationContextRegistry;
import de.rcenvironment.core.component.integration.ToolIntegrationService;
import de.rcenvironment.core.component.integration.internal.IconHelper;
import de.rcenvironment.core.component.integration.workflow.WorkflowIntegrationConstants;
import de.rcenvironment.core.component.integration.workflow.WorkflowIntegrationService;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescriptionPersistenceHandler;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.workflow.execution.function.EndpointAdapterToJsonConverter;
import de.rcenvironment.core.workflow.execution.function.EndpointAdapters;

/**
 * @author Alexander Weinert
 * @author Kathrin Schaffert
 */
@Component
public class WorkflowIntegrationServiceImpl implements WorkflowIntegrationService {

    private static final String CONFIGURATION_FILENAME = "configuration.json";

    private static final String CONFIGURATION_TEMPLATE_FILENAME = "configuration.json";

    private final Log log = LogFactory.getLog(WorkflowIntegrationServiceImpl.class);

    private ConfigurationService configurationService;

    private WorkflowIntegrationContext integrationContext;

    private ToolIntegrationService toolIntegrationService;

    private IconHelper iconHelper;

    @Override
    public void integrateWorkflowFileAsComponent(WorkflowDescription workflowDescription, ConfigurationMap model,
        Optional<String> originalName)
        throws IOException {
        toolIntegrationService.setFileWatcherActive(false);

        String componentname = model.getToolName();

        toolIntegrationService.removeTool(componentname, integrationContext);

        // When renaming a workflow, we have to copy existing files (icon and documentation) to the new integration directory.
        copyOriginalFilesToIntegrationDirectory(model, originalName, componentname);

        writeWorkflowFileToIntegrationDirectory(workflowDescription, componentname);

        File workflowConfigurationFile = createFile(getToolIntegrationDirectoryPath(componentname));
        iconHelper.prescaleAndCopyIcon(model, workflowConfigurationFile);
        toolIntegrationService.copyToolDocumentation(model, workflowConfigurationFile);

        writeConfigurationToIntegrationDirectory(componentname, model.getRawConfigurationMap());

        toolIntegrationService.integrateTool(model.getRawConfigurationMap(), integrationContext);

        toolIntegrationService.setFileWatcherActive(true);
        toolIntegrationService.registerRecursive(componentname, integrationContext);
    }

    @Override
    public void integrateWorkflowFileAsComponent(WorkflowDescription workflowDescription, String componentname,
        EndpointAdapters endpointAdapterDefinitions) throws IOException {
        toolIntegrationService.setFileWatcherActive(false);

        writeWorkflowFileToIntegrationDirectory(workflowDescription, componentname);
        Map<String, Object> configurationMap = getConfigurationForWorkflowIntegration(componentname, endpointAdapterDefinitions);
        writeConfigurationToIntegrationDirectory(componentname, configurationMap);

        toolIntegrationService.setFileWatcherActive(true);
        toolIntegrationService.registerRecursive(componentname, integrationContext);

    }

    private void writeWorkflowFileToIntegrationDirectory(WorkflowDescription workflowDescription, String componentname) throws IOException {
        final File workflowFileTarget = createWorkflowFile(componentname);
        createParentDirectories(workflowFileTarget);
        writeWorkflowDescriptionToFile(workflowDescription, workflowFileTarget);
    }

    private void writeWorkflowDescriptionToFile(WorkflowDescription workflowDescription, final File workflowFileTarget)
        throws IOException {
        try (ByteArrayOutputStream byteOutputStream = writeDescriptionToStream(workflowDescription);
            OutputStream fileOutputStream = createFileOutputStream(workflowFileTarget)) {
            byteOutputStream.writeTo(fileOutputStream);
        }
    }

    protected ByteArrayOutputStream writeDescriptionToStream(WorkflowDescription workflowDescription) throws IOException {
        final WorkflowDescriptionPersistenceHandler persistenceHandler = new WorkflowDescriptionPersistenceHandler();
        return persistenceHandler.writeWorkflowDescriptionToStream(workflowDescription);
    }

    private void writeConfigurationToIntegrationDirectory(
        final String componentname, Map<String, Object> configurationMap)
        throws IOException {

        final OutputStream outputStream = constructOutputStreamForConfiguration(componentname);
        writeMapToJson(configurationMap, outputStream);
        toolIntegrationService.putToolNameToPath(componentname, new File(getToolIntegrationDirectoryPath(componentname)));
    }

    private OutputStream constructOutputStreamForConfiguration(String componentname) throws FileNotFoundException {
        final File configurationFileTarget = createConfigurationFile(componentname);
        return createFileOutputStream(configurationFileTarget);
    }

    private void copyOriginalFilesToIntegrationDirectory(ConfigurationMap model, Optional<String> originalName, String componentname)
        throws IOException {
        if (originalName.isPresent() && !originalName.get().equals(componentname)) {
            copyOriginalIconToIntegrationDirectory(model, originalName.get(), componentname);
            copyOriginalDocumentationToIntegrationDirectory(model, originalName.get(), componentname);
            removeOriginalDirectory(originalName.get());
            toolIntegrationService.unregisterIntegration(originalName.get(), integrationContext);
            toolIntegrationService.removeTool(originalName.get(), integrationContext);
        }
    }

    private void copyOriginalDocumentationToIntegrationDirectory(ConfigurationMap model, String originalName, String componentname) {
        if (model.containsDocFilePath()) {
            File doc = new File(model.getDocFilePath());
            if (!doc.isAbsolute()) {
                File originalDir = new File(createFile(getToolIntegrationDirectoryPath(originalName)), IntegrationConstants.DOCS_DIR_NAME);
                File newDir = new File(createFile(getToolIntegrationDirectoryPath(componentname)), IntegrationConstants.DOCS_DIR_NAME);
                File originalFile = new File(originalDir, model.getDocFilePath());
                File newFile = new File(newDir, model.getDocFilePath());
                try {
                    FileUtils.copyFile(originalFile, newFile);
                } catch (IOException e) {
                    log.warn("Could not copy documentation into integration folder.", e);
                }
            }
        }
    }

    private void copyOriginalIconToIntegrationDirectory(ConfigurationMap model, String originalName, String componentname) {
        if (model.getIconPath() != null && !model.getIconPath().isEmpty()) {
            File icon = new File(model.getIconPath());
            if (!icon.isAbsolute()) {
                File originalFile = new File(createFile(getToolIntegrationDirectoryPath(originalName)), model.getIconPath());
                File newFile = new File(createFile(getToolIntegrationDirectoryPath(componentname)), model.getIconPath());
                try {
                    FileUtils.copyFile(originalFile, newFile);
                } catch (IOException e) {
                    log.warn("Could not copy icon into integration folder.", e);
                }
            }
        }
    }

    private void removeOriginalDirectory(String originalName) throws IOException {
        File remove = createFile(getToolIntegrationDirectoryPath(originalName));
        if (remove.isDirectory()) {
            FileUtils.forceDelete(remove);
        }
    }

    private String getPathToWorkflowIntegrationDirectory() {
        return String.join(File.separator,
            configurationService.getConfigurablePath(ConfigurablePathId.DEFAULT_WRITEABLE_INTEGRATION_ROOT).getAbsolutePath(),
            integrationContext.getNameOfToolIntegrationDirectory());
    }

    private Map<String, Object> getConfigurationForWorkflowIntegration(
        final String componentname, final EndpointAdapters endpointAdapterDefinitions) throws IOException {
        final InputStream configurationTemplateStream = createConfigurationTemplateStream();

        final Map<String, Object> configurationTemplate = parseJson(configurationTemplateStream);

        return injectValuesIntoConfigurationTemplate(configurationTemplate, componentname, endpointAdapterDefinitions);
    }

    private Map<String, Object> injectValuesIntoConfigurationTemplate(final Map<String, Object> configurationTemplate,
        final String componentname, EndpointAdapters endpointAdapterDefinitions) throws IOException {

        configurationTemplate.put(IntegrationConstants.KEY_COMPONENT_NAME, componentname);

        final EndpointAdapterToJsonConverter endpointAdapterConverter = new EndpointAdapterToJsonConverter(endpointAdapterDefinitions);

        configurationTemplate.put(IntegrationConstants.KEY_ENDPOINT_INPUTS, endpointAdapterConverter.toInputDefinitions());

        configurationTemplate.put(IntegrationConstants.KEY_ENDPOINT_OUTPUTS, endpointAdapterConverter.toOutputDefinitions());

        final List<Map<String, Object>> endpointAdapterConfiguration = endpointAdapterConverter.toEndpointAdapterDefinitions();
        try {
            final String endpointAdapters = new ObjectMapper().writeValueAsString(endpointAdapterConfiguration);
            configurationTemplate.put(WorkflowIntegrationConstants.KEY_ENDPOINT_ADAPTERS, endpointAdapters);
        } catch (JsonProcessingException e) {
            throw new IOException("Could not unparse endpoint adapter definition to JSON", e);
        }

        return configurationTemplate;
    }

    protected InputStream createConfigurationTemplateStream() {
        return getClass().getClassLoader().getResourceAsStream(CONFIGURATION_TEMPLATE_FILENAME);
    }

    protected Map<String, Object> parseJson(final InputStream configurationTemplateStream) throws IOException {
        final ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        // We have to manually suppress conversion warnings here, as we would otherwise need to pass HashMap<String, Object>.class into the
        // call to readValue. Due to type erasure, however, no such object exists.
        @SuppressWarnings("unchecked") final Map<String, Object> configurationTemplate =
            mapper.readValue(configurationTemplateStream, HashMap.class);
        return configurationTemplate;
    }

    @Override
    public synchronized Set<String> getActiveIntegratedWorkflowComponentIds() {
        Set<String> activeIds = new HashSet<>();
        Map<String, Map<String, Object>> integratedConfigurations = toolIntegrationService.getIntegratedConfigurations();

        for (Entry<String, Map<String, Object>> entry : integratedConfigurations.entrySet()) {
            Map<String, Object> configMap = entry.getValue();
            if (configMap.get(IntegrationConstants.INTEGRATION_TYPE).equals(IntegrationContextType.WORKFLOW.toString())
                && (configMap.get(IntegrationConstants.IS_ACTIVE) == null
                    || (Boolean) configMap.get(IntegrationConstants.IS_ACTIVE))) {
                activeIds.add(entry.getKey());
            }
        }
        return activeIds;
    }

    @Override
    public synchronized Set<String> getInactiveIntegratedWorkflowComponentIds() {
        Set<String> inactiveIds = new HashSet<>();
        Map<String, Map<String, Object>> integratedConfigurations = toolIntegrationService.getIntegratedConfigurations();

        for (Entry<String, Map<String, Object>> entry : integratedConfigurations.entrySet()) {
            Map<String, Object> configMap = entry.getValue();
            if (configMap.get(IntegrationConstants.INTEGRATION_TYPE).equals(IntegrationContextType.WORKFLOW.toString())
                && Boolean.FALSE.equals(configMap.get(IntegrationConstants.IS_ACTIVE))) {
                inactiveIds.add(entry.getKey());
            }
        }
        return inactiveIds;
    }

    protected void writeMapToJson(final Map<String, Object> configuration, final OutputStream outputStream) throws IOException {
        final ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(outputStream, configuration);
    }

    private File createConfigurationFile(String componentname) {
        final String configurationFilePath = String.join(File.separator,
            getPathToWorkflowIntegrationDirectory(),
            integrationContext.getToolDirectoryPrefix() + componentname,
            CONFIGURATION_FILENAME);
        return createFile(configurationFilePath);
    }

    private File createWorkflowFile(String componentname) {
        final String workflowFilePath = String.join(File.separator,
            getToolIntegrationDirectoryPath(componentname),
            WorkflowIntegrationConstants.INTEGRATED_WORKFLOW_FILENAME);
        return createFile(workflowFilePath);
    }

    private String getToolIntegrationDirectoryPath(String componentname) {
        return String.join(File.separator,
            getPathToWorkflowIntegrationDirectory(),
            integrationContext.getToolDirectoryPrefix() + componentname);
    }

    protected File createFile(String path) {
        return new File(path);
    }

    protected void createParentDirectories(final File workflowFileTarget) {
        workflowFileTarget.getParentFile().mkdirs();
    }

    protected OutputStream createFileOutputStream(final File workflowFileTarget) throws FileNotFoundException {
        return new FileOutputStream(workflowFileTarget);
    }

    @Reference
    public void bindConfigurationService(final ConfigurationService service) {
        this.configurationService = service;
    }

    @Reference
    public void bindToolIntegrationService(final ToolIntegrationService service) {
        this.toolIntegrationService = service;
    }

    @Reference
    public void bindToolIntegrationServiceRegistry(final ToolIntegrationContextRegistry registry) {
        this.integrationContext = (WorkflowIntegrationContext) registry.getToolIntegrationContextByType("workflow");
    }

    @Reference
    public void bindIconHelper(final IconHelper helper) {
        this.iconHelper = helper;
    }
}
