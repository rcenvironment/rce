/*
 * Copyright 2020-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.integration.workflow;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.authorization.AuthorizationException;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.integration.workflow.internal.WorkflowIntegrationContext;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputDatumHandling;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.component.workflow.execution.api.PersistentWorkflowDescriptionLoaderService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileException;
import de.rcenvironment.core.component.workflow.execution.spi.WorkflowDescriptionLoaderCallback;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.datamanagement.DataManagementService;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.workflow.execution.function.EndpointAdapter;
import de.rcenvironment.core.workflow.execution.function.EndpointAdapter.Builder;
import de.rcenvironment.core.workflow.execution.function.EndpointAdapters;
import de.rcenvironment.core.workflow.execution.function.WorkflowFunction;
import de.rcenvironment.core.workflow.execution.function.WorkflowFunctionException;
import de.rcenvironment.core.workflow.execution.function.WorkflowFunctionInputs;
import de.rcenvironment.core.workflow.execution.function.WorkflowFunctionResult;
import de.rcenvironment.core.workflow.execution.function.WorkflowFunctionService;

public class WorkflowIntegratorComponent extends DefaultComponent {

    public static class EndpointAdapterFactory {
    
        private static final String TYPE_KEY = "type";
    
        private static final String IDENTIFIER_KEY = "identifier";
    
        private static final String INTERNAL_NAME_KEY = "internalName";
    
        private static final String EXTERNAL_NAME_KEY = "externalName";
    
        private final WorkflowDescription description;
    
        private Map<String, String> configurationMap;
    
        private Collection<String> errorMessages = new LinkedList<>();
    
        public EndpointAdapterFactory(final WorkflowDescription descriptionParam) {
            this.description = descriptionParam;
        }
    
        public EndpointAdapter buildFromMap(Map<String, String> map) throws ComponentException {
            final EndpointAdapter.Builder builder;
    
            this.configurationMap = map;
    
            if ("INPUT".equals(this.configurationMap.get(TYPE_KEY))) {
                builder = EndpointAdapter.inputAdapterBuilder();
            } else {
                builder = EndpointAdapter.outputAdapterBuilder();
            }
            // final EndpointAdapterDefinition product = new EndpointAdapterDefinition();
    
            initializeEndpointNames(builder);
            initializeAdaptedDataType(builder);
            initializeInputHandling(builder);
            initializeInputExecutionConstraint(builder);
    
            if (!this.configurationMap.containsKey(TYPE_KEY)) {
                errorMessages.add("Configuration does not key 'type', which must be set to ether 'INPUT' or 'OUTPUT'");
            } else if (!("INPUT".equals(this.configurationMap.get(TYPE_KEY)) || "OUTPUT".equals(this.configurationMap.get(TYPE_KEY)))) {
                final String errorMessage = StringUtils
                    .format("Endpoint adapter defined as unknown type %s. Supported types: 'INPUT', 'OUTPUT'",
                        this.configurationMap.get(TYPE_KEY));
                errorMessages.add(errorMessage);
            }
    
            throwExceptionIfErrorOccurred();
    
            if ("INPUT".equals(this.configurationMap.get(TYPE_KEY))) {
                return builder.build();
            } else {
                return builder.build();
            }
        }
    
        private void initializeInputExecutionConstraint(Builder builder) {
            if (!this.configurationMap.containsKey(IDENTIFIER_KEY)) {
                // If the workflow node to adapt is not present in the workflow, we have already logged an error earlier. Thus, a simple
                // early exit suffices here.
                return;
            }
    
            final boolean isInputAdapter = "INPUT".equals(this.configurationMap.get(TYPE_KEY));
            
            if (!isInputAdapter) {
                return;
            }
            
            final Optional<EndpointDescription> adaptedInput = getAdaptedEndpoint();
            if (!adaptedInput.isPresent()) {
                // If the adapted input does not exist, we have already loged an error earlier during parsing. Thus, an early exit suffices
                return;
            }
            
            builder.inputExecutionConstraint(
                EndpointDefinition.InputExecutionContraint.valueOf(this.configurationMap.get("inputExecutionConstraint")));
        }

        private void initializeInputHandling(Builder builder) {
            if (!this.configurationMap.containsKey(IDENTIFIER_KEY)) {
                // If the workflow node to adapt is not present in the workflow, we have already logged an error earlier. Thus, a simple
                // early exit suffices here.
                return;
            }
    
            final boolean isInputAdapter = "INPUT".equals(this.configurationMap.get(TYPE_KEY));
            
            if (!isInputAdapter) {
                return;
            }
            
            final Optional<EndpointDescription> adaptedInput = getAdaptedEndpoint();
            if (!adaptedInput.isPresent()) {
                // If the adapted input does not exist, we have already loged an error earlier during parsing. Thus, an early exit suffices
                return;
            }
            
            builder.inputHandling(InputDatumHandling.valueOf(this.configurationMap.get("inputHandling")));
        }

        private void throwExceptionIfErrorOccurred() throws ComponentException {
            if (errorMessages.isEmpty()) {
                return;
            }
    
            final String unparsedConfigurationMap = this.configurationMap.entrySet().stream()
                .map(entry -> StringUtils.format("%s=%s", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(", ", "{", "}"));
            final String joinedErrorMessages = this.errorMessages.stream()
                .collect(Collectors.joining(". "));
    
            throw new ComponentException(StringUtils.format("Invalid endpoint adapter definition: %s. Specific errors: %s.",
                unparsedConfigurationMap, joinedErrorMessages));
    
        }
    
        private void initializeEndpointNames(final EndpointAdapter.Builder builder) throws ComponentException {
            if (!this.configurationMap.containsKey(IDENTIFIER_KEY)) {
                errorMessages.add("Configuration does not contain required parameter 'identifier'");
            } else {
                builder.workflowNodeIdentifier(this.configurationMap.get(IDENTIFIER_KEY));
            }
    
            if (!this.configurationMap.containsKey(INTERNAL_NAME_KEY)) {
                errorMessages.add("Configuration does not contain required parameter 'internalName'");
            } else {
                builder.internalEndpointName(this.configurationMap.get(INTERNAL_NAME_KEY));
            }
    
            if (!this.configurationMap.containsKey(EXTERNAL_NAME_KEY)) {
                errorMessages.add("Configuration does not contain required parameter 'externalName'");
            } else {
                builder.externalEndpointName(this.configurationMap.get(EXTERNAL_NAME_KEY));
            }
        }
    
        private void initializeAdaptedDataType(final EndpointAdapter.Builder builder) {
            if (!this.configurationMap.containsKey(IDENTIFIER_KEY)) {
                // If the workflow node to adapt is not present in the workflow, we have already logged an error earlier. Thus, a simple
                // early exit suffices here.
                return;
            }
    
            final Optional<EndpointDescription> internalEndpointDescription = getAdaptedEndpoint();
    
            if (!internalEndpointDescription.isPresent()) {
                errorMessages.add(StringUtils.format(
                    "Endpoint with name '%s' at workflow node with ID '%s' is configured to pass values from or to the external workflow, "
                        + "but that endpoint does not exist on that node",
                    this.configurationMap.get(INTERNAL_NAME_KEY), this.configurationMap.get(IDENTIFIER_KEY)));
                return;
            }
    
            builder.dataType(internalEndpointDescription.get().getDataType());
        }

        private Optional<EndpointDescription> getAdaptedEndpoint() {
            final boolean isInputAdapter = "INPUT".equals(this.configurationMap.get(TYPE_KEY));

            final String workflowNodeIdentifier = this.configurationMap.get(IDENTIFIER_KEY);
            final Optional<WorkflowNode> adaptedWorkflowNode = description.getWorkflowNodes().stream()
                .filter(node -> workflowNodeIdentifier.equals(node.getIdentifierAsObject().toString()))
                .findAny();
    
            if (!adaptedWorkflowNode.isPresent()) {
                errorMessages.add(StringUtils.format(
                    "Workflow node with identifier '%s' is configured to pass values from or to the external workflow, "
                        + "but that node does not exist in the workflow",
                    workflowNodeIdentifier));
                return Optional.empty();
            }
    
            final EndpointDescriptionsManager endpointDescriptions;
            if (isInputAdapter) {
                endpointDescriptions = adaptedWorkflowNode.get().getInputDescriptionsManager();
            } else {
                endpointDescriptions = adaptedWorkflowNode.get().getOutputDescriptionsManager();
            }
    
            final Stream<EndpointDescription> allEndpointDescriptions = Stream.concat(
                endpointDescriptions.getDynamicEndpointDescriptions().stream(),
                endpointDescriptions.getStaticEndpointDescriptions().stream());
            final Optional<EndpointDescription> internalEndpointDescription = allEndpointDescriptions
                .filter(endpointDescription -> endpointDescription.getName().equals(this.configurationMap.get(INTERNAL_NAME_KEY)))
                .findAny();
            return internalEndpointDescription;
        }
    }

    private ComponentContext context;

    private PersistentWorkflowDescriptionLoaderService workflowLoaderService;

    private WorkflowFunctionService workflowFunctionService;

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.context = componentContext;

        this.workflowLoaderService = context.getService(PersistentWorkflowDescriptionLoaderService.class);
        this.workflowFunctionService = context.getService(WorkflowFunctionService.class);
    }

    @Override
    public boolean treatStartAsComponentRun() {
        /*
         * Treating a start as a run of the component amounts to executing the component once at the start of the workflow without any
         * inputs. We only want to do this if this component will never receive inputs. Otherwise, we wait for all required inputs to be
         * present before executing the component. This behavior is consistent with the behavior of the @{link ScriptComponent}.
         */
        return this.context.getInputs().isEmpty();
    }

    @Override
    public void start() throws ComponentException {
        if (treatStartAsComponentRun()) {
            processInputs();
        }
    }

    @Override
    public void processInputs() throws ComponentException {
        super.processInputs();

        final File workflowFile = getWorkflowFileAndAssertExistence();
        final WorkflowDescription workflowDescription = loadWorkflowDescriptionFromFile(workflowFile);

        final String endpointAdapterDefinitionsString = context.getConfigurationValue("endpointAdapters");

        List<Map<String, String>> parsedEndpointAdapterConfiguration;
        try {
            parsedEndpointAdapterConfiguration =
                JsonUtils.getDefaultObjectMapper().readValue(endpointAdapterDefinitionsString, LinkedList.class);
        } catch (IOException e) {
            throw new ComponentException("Error when deserializing endpointAdapterDefinitions", e);
        }

        final EndpointAdapters.Builder endpointAdapterDefinitionsBuilder = new EndpointAdapters.Builder();
        final List<ComponentException> thrownExceptions = new LinkedList<>();
        final EndpointAdapterFactory factory = new EndpointAdapterFactory(workflowDescription);

        for (Map<String, String> parsedEndpointAdapterConfigurations : parsedEndpointAdapterConfiguration) {
            try {
                endpointAdapterDefinitionsBuilder.addEndpointAdapter(factory.buildFromMap(parsedEndpointAdapterConfigurations));
            } catch (ComponentException e) {
                thrownExceptions.add(e);
            }

            if (!thrownExceptions.isEmpty()) {
                final String joinedExceptionMessages =
                    thrownExceptions.stream().map(exception -> exception.toString()).collect(Collectors.joining("; "));
                final String errorMessage = "Errors in the configuration of endpoint adapters: " + joinedExceptionMessages;
                throw new ComponentException(errorMessage);
            }
        }

        final WorkflowFunction workflowFunction = this.workflowFunctionService.createBuilder()
            .setComponentContext(context)
            .withWorkflowDescription(workflowDescription)
            .withEndpointAdapters(endpointAdapterDefinitionsBuilder.build())
            .withInternalName(context.getComponentName())
            .withExternalName(context.getInstanceName())
            .withCallingWorkflowName(context.getWorkflowInstanceName())
            .build();

        WorkflowFunctionInputs workflowInputs = createWorkflowInputsFromComponentContext();

        context.announceExternalProgramStart();
        WorkflowFunctionResult workflowResult;
        try {
            workflowResult = workflowFunction.execute(workflowInputs);
        } catch (WorkflowFunctionException e) {
            throw new ComponentException("Could not execute underlying workflow", e);
        }
        context.announceExternalProgramTermination();

        if (workflowResult.isFailure()) {
            throw new ComponentException(StringUtils.format("Execution of workflow %s failed", workflowFile.getAbsolutePath()));
        }

        for (Entry<String, TypedDatum> result : workflowResult.toMap().entrySet()) {
            context.writeOutput(result.getKey(), result.getValue());
        }
    }
    
    private WorkflowFunctionInputs createWorkflowInputsFromComponentContext() throws ComponentException {
        final Map<String, TypedDatum> inputs = context.getInputsWithDatum().stream()
            .collect(Collectors.toMap(name -> name, context::readInput));
        
        final DataManagementService dms = context.getService(DataManagementService.class);
        final NetworkDestination outerWorkflowStorage = context.getStorageNetworkDestination();
        final TempFileService tempFileService = TempFileServiceAccess.getInstance();
        final TypedDatumService tds = context.getService(TypedDatumService.class);
        
        final Map<String, TypedDatum> replacedInputs = new HashMap<>();
        
        for (Entry<String, TypedDatum> nonreplacedInput : inputs.entrySet()) {
            if (nonreplacedInput.getValue() instanceof FileReferenceTD) {
                try {
                    final File tempFile = createTempFile();
                    final FileReferenceTD nonreplacedDatum = (FileReferenceTD) nonreplacedInput.getValue();
                    dms.copyReferenceToLocalFile(nonreplacedDatum.getFileReference(), tempFile, outerWorkflowStorage);
                    
                    replacedInputs.put(
                        nonreplacedInput.getKey(),
                        tds.getFactory().createShortText(
                            Paths.get(
                                tempFile.getAbsolutePath()
                                ).toString()
                            )
                    );
                } catch (AuthorizationException | IOException | CommunicationException e) {
                    throw new ComponentException("Error when copying outer inputs to inner ones", e);
                }
            } else if (nonreplacedInput.getValue() instanceof DirectoryReferenceTD) {
                try {
                    final File tempDir = createTempDir();
                    final DirectoryReferenceTD nonreplacedDatum = (DirectoryReferenceTD) nonreplacedInput.getValue();
        
                    dms.copyReferenceToLocalDirectory(nonreplacedDatum.getDirectoryReference(), tempDir, outerWorkflowStorage);
        
                    replacedInputs.put(
                        nonreplacedInput.getKey(),
                        tds.getFactory().createShortText(
                            Paths.get(
                                tempDir.getAbsolutePath(),
                                nonreplacedDatum.getDirectoryName()).toString()
                            )
                    );
                    
                    LogFactory.getLog(WorkflowFunctionInputs.class).info(StringUtils.format("Copied directory to '%s'", tempDir));
                } catch (AuthorizationException | IOException | CommunicationException e) {
                    throw new ComponentException("Error when copying outer inputs to inner ones", e);
                }
            } else {
                replacedInputs.put(nonreplacedInput.getKey(), nonreplacedInput.getValue());
            }
        }
        
        return WorkflowFunctionInputs.createFromMap(replacedInputs);
    }

    protected File createTempDir() throws IOException {
        return TempFileServiceAccess.getInstance().createManagedTempDir();
    }

    protected File createTempFile() throws IOException {
        return TempFileServiceAccess.getInstance().createTempFileFromPattern("*");
    }

    public File getWorkflowFileAndAssertExistence() throws ComponentException {
        final WorkflowIntegrationContext workflowIntegrationContext = new WorkflowIntegrationContext();

        final String workflowFilePath = String.join(File.separator,
            context.getService(ConfigurationService.class)
                .getConfigurablePath(ConfigurablePathId.DEFAULT_WRITEABLE_INTEGRATION_ROOT).getAbsolutePath(),
            workflowIntegrationContext.getNameOfToolIntegrationDirectory(),
            workflowIntegrationContext.getToolDirectoryPrefix() + context.getComponentName(),
            "workflow.wf");
        final File workflowFile = createWorkflowFile(workflowFilePath);

        if (!fileExists(workflowFile)) {
            throw new ComponentException(StringUtils.format(
                "Path %s is expected to point to a workflow file. Instead, that path does not exist.", workflowFile.getAbsolutePath()));
        }

        if (!isFile(workflowFile)) {
            throw new ComponentException(StringUtils.format(
                "Path %s is expected to point to a workflow file, but points at something that is not a file. "
                    + "Hence, that workflow cannot be executed",
                workflowFile.getAbsolutePath()));
        }

        return workflowFile;
    }

    protected boolean isFile(final File workflowFile) {
        return workflowFile.isFile();
    }

    protected boolean fileExists(final File workflowFile) {
        return workflowFile.exists();
    }

    protected File createWorkflowFile(String workflowFilePath) {
        return new File(workflowFilePath);
    }

    private WorkflowDescription loadWorkflowDescriptionFromFile(final File workflowFile) throws ComponentException {
        final WorkflowDescriptionLoaderCallback loaderCallback = new WorkflowDescriptionLoaderCallback() {

            @Override
            public void onWorkflowFileParsingPartlyFailed(String backupFilename) {
                return;
            }

            @Override
            public void onSilentWorkflowFileUpdated(String message) {
                return;
            }

            @Override
            public void onNonSilentWorkflowFileUpdated(String message, String backupFilename) {
                return;

            }

            @Override
            public boolean arePartlyParsedWorkflowConsiderValid() {
                return false;
            }
        };
        try {
            return workflowLoaderService.loadWorkflowDescriptionFromFileConsideringUpdates(workflowFile, loaderCallback);
        } catch (WorkflowFileException e) {
            throw new ComponentException(
                StringUtils.format("Unexpected exception thrown during parsing of file %s", workflowFile.getAbsolutePath()), e);
        }
    }
}
