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
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.component.integration.ToolIntegrationContextRegistry;
import de.rcenvironment.core.component.integration.ToolIntegrationService;
import de.rcenvironment.core.component.integration.workflow.WorkflowIntegrationService;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescriptionPersistenceHandler;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.workflow.execution.function.EndpointAdapters;

/**
 * @author Alexander Weinert
 */
@Component
public class WorkflowIntegrationServiceImpl implements WorkflowIntegrationService {

    private static final String CONFIGURATION_FILENAME = "configuration.json";

    private static final String CONFIGURATION_TEMPLATE_FILENAME = "configuration.json";

    private static final String INTEGRATED_WORKFLOW_FILENAME = "workflow.wf";

    private ConfigurationService configurationService;

    private WorkflowIntegrationContext integrationContext;

    private ToolIntegrationService toolIntegrationService;

    @Override
    public void integrateWorkflowFileAsComponent(WorkflowDescription workflowDescription, String componentname,
        EndpointAdapters endpointAdapterDefinitions) throws IOException {

        toolIntegrationService.setFileWatcherActive(false);

        writeWorkflowFileToIntegrationDirectory(workflowDescription, componentname);
        constructAndWriteConfigurationToIntegrationDirectory(componentname, endpointAdapterDefinitions);

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

    private void constructAndWriteConfigurationToIntegrationDirectory(
        final String componentname, final EndpointAdapters endpointAdapterDefinitions) throws IOException {
        final Map<String, Object> configuration = getConfigurationForWorkflowIntegration(componentname, endpointAdapterDefinitions);
        final OutputStream outputStream = constructOutputStreamForConfiguration(componentname);
        writeMapToJson(configuration, outputStream);
    }

    private OutputStream constructOutputStreamForConfiguration(String componentname) throws FileNotFoundException {
        final File configurationFileTarget = createConfigurationFile(componentname);
        return createFileOutputStream(configurationFileTarget);
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

        configurationTemplate.put("toolName", componentname);
        
        final EndpointAdapterToJsonConverter endpointAdapterConverter = new EndpointAdapterToJsonConverter(endpointAdapterDefinitions);

        configurationTemplate.put("inputs", endpointAdapterConverter.toInputDefinitions());
        configurationTemplate.put("outputs", endpointAdapterConverter.toOutputDefinitions());

        final List<Map<String, Object>> endpointAdapterConfiguration = endpointAdapterConverter.toEndpointAdapterDefinitions();
        try {
            final String endpointAdapters = new ObjectMapper().writeValueAsString(endpointAdapterConfiguration);
            configurationTemplate.put("endpointAdapters", endpointAdapters);
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
        @SuppressWarnings("unchecked")
        final Map<String, Object> configurationTemplate = mapper.readValue(configurationTemplateStream, HashMap.class);
        return configurationTemplate;
    }

    protected void writeMapToJson(final Map<String, Object> configuration, final OutputStream outputStream) throws IOException {
        final ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        mapper.writeValue(outputStream, configuration);
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
            INTEGRATED_WORKFLOW_FILENAME);
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

}
