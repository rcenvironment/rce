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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.authorization.AuthorizationException;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.integration.workflow.internal.WorkflowIntegrationContext;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.component.workflow.execution.api.PersistentWorkflowDescriptionLoaderService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileException;
import de.rcenvironment.core.component.workflow.execution.spi.WorkflowDescriptionLoaderCallback;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
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
import de.rcenvironment.core.workflow.execution.function.EndpointAdapters;
import de.rcenvironment.core.workflow.execution.function.WorkflowFunction;
import de.rcenvironment.core.workflow.execution.function.WorkflowFunctionException;
import de.rcenvironment.core.workflow.execution.function.WorkflowFunctionInputs;
import de.rcenvironment.core.workflow.execution.function.WorkflowFunctionResult;
import de.rcenvironment.core.workflow.execution.function.WorkflowFunctionService;

/**
 * TODO: Enter comment!
 *
 * @author Alexander Weinert
 */
public class WorkflowIntegratorComponent extends DefaultComponent {

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

    @SuppressWarnings("unchecked")
    @Override
    public void processInputs() throws ComponentException {
        super.processInputs();

        final File workflowFile = getWorkflowFileAndAssertExistence();
        final WorkflowDescription workflowDescription = loadWorkflowDescriptionFromFile(workflowFile);

        final String endpointAdapterDefinitionsString = context.getConfigurationValue(WorkflowIntegrationConstants.KEY_ENDPOINT_ADAPTERS);

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
