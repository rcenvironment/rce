/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.testutils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;

import de.rcenvironment.core.authorization.AuthorizationException;
import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandDescription;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.datamanagement.DataManagementService;
import de.rcenvironment.core.datamanagement.backend.MetaDataBackendService;
import de.rcenvironment.core.datamanagement.commons.ComponentInstance;
import de.rcenvironment.core.datamanagement.commons.EndpointInstance;
import de.rcenvironment.core.datamanagement.commons.MetaData;
import de.rcenvironment.core.datamanagement.commons.MetaDataKeys;
import de.rcenvironment.core.datamanagement.commons.MetaDataSet;
import de.rcenvironment.core.datamanagement.commons.PropertiesKeys;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.datamodel.api.FinalComponentRunState;
import de.rcenvironment.core.datamodel.api.FinalComponentState;
import de.rcenvironment.core.datamodel.api.FinalWorkflowState;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.toolkit.modules.concurrency.api.CallablesGroup;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * A {@link CommandPlugin} providing commands for testing the data management.
 *
 * @author Brigitte Boden
 * @author Jascha Riedel (#13978)
 */
public class DMCommandPlugin implements CommandPlugin {

    private static final int NUMBER_100 = 100;

    private static final int WORKFLOWS_TO_CREATE_IN_PARALLEL = 20;

    private static final int DEFAULT_NUMBER_OF_INPUTS = 5;

    private static final int DEFAULT_NUMBER_OF_ITERATIONS = 10;

    private static final int DEFAULT_NUMBER_OF_COMPONENTS = 10;

    private static final int DEFAULT_NUMBER_OF_WORKFLOWS = 10;

    private static final String DEFAULT_WORKFLOWNAME_PREFIX = "dummy_workflow";

    /**
     * The values for number of iterations, number of components and number of inputs are varied for each workflow. This constant defines
     * how much deviation from the default value is allowed (i.e. 20%)
     */
    private static final double DEFAULT_ALLOWED_DEVIATION_IN_PERCENT = 20;

    private MetaDataBackendService metaDataService;

    private PlatformService platformService;

    private TypedDatumService typedDatumService;

    private DataManagementService dataManagementService;

    private TypedDatumFactory typedDatumFactory;

    private TypedDatumSerializer typedDatumSerializer;

    /**
     * Option for file creation.
     *
     */
    private enum FileCreationOption {
        NONE, SMALL, BIG
    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.command.spi.SingleCommandHandler#execute(de.rcenvironment.core.command.spi.CommandContext)
     */
    @Override
    public void execute(CommandContext context) throws CommandException {
        context.consumeExpectedToken("dm");
        String subCmd = context.consumeNextToken();
        if (subCmd == null) {
            throw CommandException.unknownCommand(context);
        } else {
            if ("create-test-data".equals(subCmd)) {
                // "dm create-test-data"
                performDmCreateTestData(context);
            } else {
                throw CommandException.unknownCommand(context);
            }
        }
    }

    /**
     * Fill datamanagement with workflow runs.
     * 
     * @param context
     */
    private void performDmCreateTestData(final CommandContext context) throws CommandException {

        final int userDefinednumberOfWorkflows;
        final int userDefinednumberOfIterations;
        final double userDefinedAllowedDeviation;
        final String userDefinedWorkflowNamePrefix;

        // Read optional parameters
        String workflowPrefix = readOptionalStringParameter(context, "--prefix");
        if (workflowPrefix != null) {
            userDefinedWorkflowNamePrefix = workflowPrefix;
        } else {
            userDefinedWorkflowNamePrefix = DEFAULT_WORKFLOWNAME_PREFIX;
        }
        Integer w = readOptionalIntParameter(context, "--workflows");
        if (w != null) {
            userDefinednumberOfWorkflows = w;
        } else {
            userDefinednumberOfWorkflows = DEFAULT_NUMBER_OF_WORKFLOWS;
        }
        Integer iter = readOptionalIntParameter(context, "--iterations");
        if (iter != null) {
            userDefinednumberOfIterations = iter;
        } else {
            userDefinednumberOfIterations = DEFAULT_NUMBER_OF_ITERATIONS;
        }
        Double deviationInPercent = readOptionalDoubleParameter(context, "--allowedDeviation");
        if (deviationInPercent != null) {
            userDefinedAllowedDeviation = deviationInPercent / NUMBER_100;
        } else {
            userDefinedAllowedDeviation = DEFAULT_ALLOWED_DEVIATION_IN_PERCENT / NUMBER_100;
        }

        if (userDefinednumberOfWorkflows < 0 || userDefinednumberOfIterations < 0 || userDefinedAllowedDeviation < 0) {
            throw CommandException.executionError("Negative values are not supported.", context);
        }
        if (userDefinedAllowedDeviation > 1) {
            throw CommandException.executionError("Deviation of more than 100% is not supported.", context);
        }

        List<String> fileTokens = context.consumeRemainingTokens();

        FileCreationOption fileCreationOption = FileCreationOption.NONE;
        if (fileTokens.contains("--smallfiles")) {
            fileCreationOption = FileCreationOption.SMALL;
            if (fileTokens.contains("--bigfiles")) {
                context.println("Both --smallfiles and --bigfiles were set, --smallfiles will be used.");
            }
        } else if (fileTokens.contains("--bigfiles")) {
            fileCreationOption = FileCreationOption.BIG;
        }

        // Read files from resources folder

        TempFileService tempFileService = TempFileServiceAccess.getInstance();
        File wfFile;
        File logFile;
        File dummyFile;
        try (InputStream wfFileStream = getClass().getResourceAsStream("/DummyWorkflowFile.txt");
            InputStream logFileStream = getClass().getResourceAsStream("/DummyLogFile.txt");) {
            wfFile = tempFileService.createTempFileWithFixedFilename("DummyWorkflowFile.txt");
            FileUtils.copyInputStreamToFile(wfFileStream, wfFile);
            logFile = tempFileService.createTempFileWithFixedFilename("DummyLogFile.txt");
            FileUtils.copyInputStreamToFile(logFileStream, logFile);
        } catch (IOException e1) {
            throw CommandException.executionError("Could not read dummy files from resources folder.", context);
        }

        if (fileCreationOption.equals(FileCreationOption.SMALL)) {
            try (InputStream dummyFileStream = getClass().getResourceAsStream("/SmallDummyFile.txt");) {
                dummyFile = tempFileService.createTempFileWithFixedFilename("SmallDummyFile.txt");
                FileUtils.copyInputStreamToFile(dummyFileStream, dummyFile);
            } catch (IOException e) {
                throw CommandException.executionError("Could not read dummy file from resources folder.", context);
            }
        } else if (fileCreationOption.equals(FileCreationOption.BIG)) {
            try (InputStream dummyFileStream = getClass().getResourceAsStream("/BigDummyFile.xml");) {
                dummyFile = tempFileService.createTempFileWithFixedFilename("BigDummyFile.xml");
                FileUtils.copyInputStreamToFile(dummyFileStream, dummyFile);
            } catch (IOException e) {
                throw CommandException.executionError("Could not read dummy file from resources folder.", context);
            }
        } else {
            dummyFile = null;
        }

        final String localNodeId = platformService.getLocalInstanceNodeSessionId().getInstanceNodeSessionIdString();
        typedDatumFactory = typedDatumService.getFactory();
        typedDatumSerializer = typedDatumService.getSerializer();

        context.println("Creating workflows...");

        int workflowIndex = 0;
        while (workflowIndex < userDefinednumberOfWorkflows) {

            // Create a CallablesGroup with WORKFLOWS_TO_CREATE_IN_PARALLEL threads
            CallablesGroup<CommandException> callablesGroup =
                ConcurrencyUtils.getFactory().createCallablesGroup(CommandException.class);

            for (int i = 0; i < WORKFLOWS_TO_CREATE_IN_PARALLEL; i++) {
                workflowIndex++;

                // Create random parameters for this workflow
                int numberOfComponents =
                    randomizeValue(DEFAULT_NUMBER_OF_COMPONENTS, userDefinedAllowedDeviation);
                int numberOfInputs =
                    randomizeValue(DEFAULT_NUMBER_OF_INPUTS, userDefinedAllowedDeviation);
                int numberOfIterations = randomizeValue(userDefinednumberOfIterations, userDefinedAllowedDeviation);
                CreateWorkflowCallableContext workflowCreationContext =
                    new CreateWorkflowCallableContext(wfFile, userDefinedWorkflowNamePrefix, localNodeId, context, dummyFile,
                        logFile, numberOfComponents,
                        numberOfInputs, workflowIndex, numberOfIterations, fileCreationOption);

                callablesGroup.add(new CreateWorkflowCallable(workflowCreationContext));

                // Stop if required number of workflows is reached.
                if (workflowIndex >= userDefinednumberOfWorkflows) {
                    break;
                }
            }

            List<CommandException> exceptions = callablesGroup.executeParallel(null);
            for (CommandException e : exceptions) {
                if (e != null) {
                    throw e;
                }
            }
            // context.println("Created " + workflowIndex + " out of " + userDefinednumberOfWorkflows + " workflows.");
        }

        context.println("Finished creating test data: created " + userDefinednumberOfWorkflows + " workflows.");
    }

    private int randomizeValue(int defaultValue, double allowedDeviation) {
        return (int) (Math.round((Math.random() * (2 * allowedDeviation) + (1 - allowedDeviation)) * defaultValue));
    }

    private Integer readOptionalIntParameter(CommandContext context, String parameter) throws CommandException {
        if (context.consumeNextTokenIfEquals(parameter)) {
            String number = context.consumeNextToken();
            if (number == null) {
                throw CommandException.syntaxError("Missing number", context);
            }
            try {
                return Integer.parseInt(number);
            } catch (NumberFormatException e) {
                throw CommandException.executionError(e.getMessage(), context);
            }
        }
        return null;
    }

    private String readOptionalStringParameter(CommandContext context, String parameter) throws CommandException {
        if (context.consumeNextTokenIfEquals(parameter)) {
            String prefix = context.consumeNextToken();
            if (prefix == null) {
                throw CommandException.syntaxError("Missing Workflow Name Prefix", context);
            }
            return prefix;
        }
        return null;
    }

    private Double readOptionalDoubleParameter(CommandContext context, String parameter) throws CommandException {
        if (context.consumeNextTokenIfEquals(parameter)) {
            String number = context.consumeNextToken();
            if (number == null) {
                throw CommandException.syntaxError("Missing number", context);
            }
            try {
                return Double.parseDouble(number);
            } catch (NumberFormatException e) {
                throw CommandException.executionError(e.getMessage(), context);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.command.spi.CommandPlugin#getCommandDescriptions()
     */
    @Override
    public Collection<CommandDescription> getCommandDescriptions() {
        final Collection<CommandDescription> contributions = new ArrayList<CommandDescription>();
        contributions
            .add(new CommandDescription(
                "dm create-test-data [--workflows <number of workflows>] [--iterations <number of iterations per workflow>]\n\t"
                    + " [--allowedDeviation <allowed deviation of values in %>]"
                    + " [--smallfiles] [--bigfiles]", "", true, "creates test data in the database. ", "If --smallfiles or --bigfiles is "
                    + "set, files are generated as inputs/outputs of the components.", " Values for number of iterations, number of "
                    + "components, number of inputs/outputs are randomized, deviating at most <allowedDeviation> % from default values "
                    + "(default 20%)"));
        return contributions;
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    public void bindMetaDataService(MetaDataBackendService newInstance) {
        this.metaDataService = newInstance;
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    public void bindTypedDatumService(TypedDatumService newInstance) {
        this.typedDatumService = newInstance;
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    public void bindPlatformService(PlatformService newInstance) {
        this.platformService = newInstance;
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    public void bindDataManagementService(DataManagementService newInstance) {
        this.dataManagementService = newInstance;
    }

    /**
     * OSGi-DS lifecycle method.
     */
    public void activate() {}

    /**
     * A container class for the parameters and files for the creation of a workflow.
     *
     * @author Brigitte Boden
     */
    private final class CreateWorkflowCallableContext {

        private File wfFile;

        private String prefixName;

        private String localNodeId;

        private CommandContext context;

        private File dummyFile;

        private File logFile;

        private int numberOfComponents;

        private int numberOfInputs;

        private int workflowNumber;

        private int numberOfIterations;

        private FileCreationOption fileCreationOption;

        private CreateWorkflowCallableContext(File wfFile, String prefixName, String localNodeId, CommandContext context, File dummyFile,
            File logFile, int numberOfComponents, int numberOfInputs,
            int workflowNumber, int numberOfIterations, FileCreationOption fileCreationOption) {
            this.wfFile = wfFile;
            this.prefixName = prefixName;
            this.localNodeId = localNodeId;
            this.context = context;
            this.dummyFile = dummyFile;
            this.logFile = logFile;
            this.numberOfComponents = numberOfComponents;
            this.numberOfInputs = numberOfInputs;
            this.workflowNumber = workflowNumber;
            this.numberOfIterations = numberOfIterations;
            this.fileCreationOption = fileCreationOption;
        }
    }

    /**
     * A "callable" class for creatinng a single dummy workflow in the data management.
     *
     * @author Brigitte Boden
     */
    private final class CreateWorkflowCallable implements Callable<CommandException> {

        private CreateWorkflowCallableContext workflowCreationContext;

        private CreateWorkflowCallable(CreateWorkflowCallableContext workflowCreationContext) {
            this.workflowCreationContext = workflowCreationContext;
        }

        @Override
        @TaskDescription(value = "Create a single workflow entry.")
        public CommandException call() {
            Map<String, Long> compInstDmIds;
            try {
                final String workflowTitle = workflowCreationContext.prefixName + "_" + workflowCreationContext.workflowNumber;
                Long id =
                    metaDataService.addWorkflowRun(workflowTitle,
                        workflowCreationContext.localNodeId, workflowCreationContext.localNodeId,
                        System.currentTimeMillis());
                MetaDataSet mds = new MetaDataSet();
                MetaData mdWorkflowRunId = new MetaData(MetaDataKeys.WORKFLOW_RUN_ID, true, true);
                mds.setValue(mdWorkflowRunId, id.toString());
                try {
                    final String wfFileReference =
                        dataManagementService.createReferenceFromLocalFile(workflowCreationContext.wfFile, mds,
                            platformService.getLocalInstanceNodeSessionId());
                    TypedDatum fileRefTD = typedDatumFactory.createFileReference(wfFileReference,
                        workflowCreationContext.wfFile.getName());
                    metaDataService.addWorkflowFileToWorkflowRun(id, typedDatumSerializer.serialize(fileRefTD));
                } catch (AuthorizationException | IOException | InterruptedException | CommunicationException e) {
                    return CommandException.executionError("Failed to upload wf file." + e, workflowCreationContext.context);
                }
                Set<ComponentInstance> componentInstances = new HashSet<>();
                for (int j = 1; j <= workflowCreationContext.numberOfComponents; j++) {
                    componentInstances.add(new ComponentInstance(UUID.randomUUID().toString(),
                        "de.rcenvironment.script/3.4", "Script " + j, null));
                }
                compInstDmIds = metaDataService.addComponentInstances(id, componentInstances);
                Set<EndpointInstance> endpointInstancesIn = new HashSet<>();
                Set<EndpointInstance> endpointInstancesOut = new HashSet<>();
                for (int z = 1; z <= workflowCreationContext.numberOfInputs; z++) {
                    Map<String, String> metaData = new HashMap<String, String>();
                    metaData.put(MetaDataKeys.DATA_TYPE, DataType.Float.getShortName());
                    endpointInstancesIn.add(new EndpointInstance("input_" + z, EndpointType.INPUT, metaData));
                    endpointInstancesOut.add(new EndpointInstance("output_" + z, EndpointType.OUTPUT, metaData));
                }
                for (Long componentInstanceId : compInstDmIds.values()) {
                    Map<String, Long> endpointIdsIn =
                        metaDataService.addEndpointInstances(componentInstanceId, endpointInstancesIn);
                    Map<String, Long> endpointIdsOut =
                        metaDataService.addEndpointInstances(componentInstanceId, endpointInstancesOut);
                    long typedDatumId = 0;
                    for (int j = 1; j <= workflowCreationContext.numberOfIterations; j++) {
                        Long componentRunId =
                            metaDataService.addComponentRun(componentInstanceId, workflowCreationContext.localNodeId, j,
                                System.currentTimeMillis());
                        for (Long endpointInstanceId : endpointIdsOut.values()) {
                            TypedDatum datum;
                            String dummyFileReference;
                            if ((workflowCreationContext.fileCreationOption.equals(FileCreationOption.SMALL)
                                || workflowCreationContext.fileCreationOption.equals(FileCreationOption.BIG))
                                && workflowCreationContext.dummyFile != null) {
                                try {
                                    dummyFileReference =
                                        dataManagementService.createReferenceFromLocalFile(workflowCreationContext.dummyFile, mds,
                                            platformService.getLocalInstanceNodeSessionId());
                                } catch (AuthorizationException | IOException | InterruptedException | CommunicationException e) {
                                    return CommandException.executionError("Could not create reference for dummy file.",
                                        workflowCreationContext.context);
                                }
                                datum =
                                    typedDatumFactory.createFileReference(dummyFileReference, workflowCreationContext.dummyFile.getName());
                            } else {
                                datum = typedDatumFactory.createFloat(7.0);
                            }

                            String datumSerialized = typedDatumSerializer.serialize(datum);
                            typedDatumId = metaDataService.addOutputDatum(componentRunId, endpointInstanceId, datumSerialized, j);
                        }
                        for (Long endpointInstanceId : endpointIdsIn.values()) {
                            metaDataService.addInputDatum(componentRunId, typedDatumId, endpointInstanceId, j);
                        }
                        metaDataService
                            .setComponentRunFinished(componentRunId, System.currentTimeMillis(), FinalComponentRunState.FINISHED);

                        Map<String, String> properties = new HashMap<String, String>();
                        String logFileReference;
                        try {
                            logFileReference =
                                dataManagementService.createReferenceFromLocalFile(workflowCreationContext.logFile, mds,
                                    platformService.getLocalInstanceNodeSessionId());
                        } catch (AuthorizationException | IOException | InterruptedException | CommunicationException e) {
                            return CommandException.executionError("Could not store dummy component log file.",
                                workflowCreationContext.context);
                        }
                        TypedDatum logRefTD = typedDatumFactory.createFileReference(logFileReference,
                            workflowCreationContext.logFile.getName());
                        properties.put(PropertiesKeys.COMPONENT_LOG_FILE, typedDatumService.getSerializer().serialize(logRefTD));
                        metaDataService.addComponentRunProperties(componentRunId, properties);
                    }

                    metaDataService.setComponentInstanceFinalState(componentInstanceId, FinalComponentState.FINISHED);
                }

                metaDataService.setWorkflowRunFinished(id, System.currentTimeMillis(), FinalWorkflowState.FINISHED);
                String type = "float";
                if (workflowCreationContext.fileCreationOption.equals(FileCreationOption.BIG)) {
                    type = "big files";
                } else if (workflowCreationContext.fileCreationOption.equals(FileCreationOption.SMALL)) {
                    type = "small files";
                }
                workflowCreationContext.context.println(StringUtils.format(
                    "Created workflow %s: %d components, %d iterations, %d inputs/outputs (%s) per component.", workflowTitle,
                    workflowCreationContext.numberOfComponents, workflowCreationContext.numberOfIterations,
                    workflowCreationContext.numberOfInputs, type));
            } catch (RemoteOperationException e) {
                return CommandException.executionError("Could not store workflow runs.", workflowCreationContext.context);
            }
            return null;
        }
    }
}
