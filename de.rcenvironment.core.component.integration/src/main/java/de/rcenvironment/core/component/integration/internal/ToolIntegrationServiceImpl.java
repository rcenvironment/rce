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
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentGroupPathRules;
import de.rcenvironment.core.component.api.ComponentIdRules;
import de.rcenvironment.core.component.integration.ConfigurationMap;
import de.rcenvironment.core.component.integration.ToolIntegrationContext;
import de.rcenvironment.core.component.integration.ToolIntegrationContextRegistry;
import de.rcenvironment.core.component.integration.ToolIntegrationService;
import de.rcenvironment.core.component.integration.internal.ToolIntegrationFileWatcherManager.Builder;
import de.rcenvironment.core.component.management.api.LocalComponentRegistrationService;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInstallationBuilder;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.api.ComponentInterfaceBuilder;
import de.rcenvironment.core.component.model.api.ComponentRevision;
import de.rcenvironment.core.component.model.api.ComponentRevisionBuilder;
import de.rcenvironment.core.component.model.configuration.api.ComponentConfigurationModelFactory;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDefinition;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationExtensionDefinition;
import de.rcenvironment.core.component.model.endpoint.api.ComponentEndpointModelFactory;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputDatumHandling;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputExecutionContraint;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinitionBuilder;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinitionConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinitionsProvider;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataConstants.Visibility;
import de.rcenvironment.core.component.model.impl.ToolIntegrationConstants;
import de.rcenvironment.core.configuration.CommandLineArguments;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.CrossPlatformFilenameUtils;
import de.rcenvironment.core.utils.common.FileCompressionFormat;
import de.rcenvironment.core.utils.common.FileCompressionService;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.ServiceUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;

/**
 * Implementation of {@link ToolIntegrationService}.
 * 
 * @author Sascha Zur
 * @author Robert Mischke (disabled legacy component publishing; cleaned up start process and threading)
 * @author Thorsten Sommer (integration of {@link FileCompressionService})
 */
@Component(immediate = true)
public class ToolIntegrationServiceImpl implements ToolIntegrationService {

    private static final String WARNING_INVALID_TOOL = "Tool Integration: Tool %s has been disabled. Reason: Invalid %s. %s.";

    private static final String IDENTIFIER = "identifier";

    private static final String META_DATA = "metaData";

    private static final String NAME = "name";

    private static final String DATA_TYPES = "dataTypes";

    private static final String DEFAULT_DATA_TYPE = "defaultDataType";

    private static final String INPUT_HANDLINGS = "inputHandlingOptions";

    private static final String DEFAULT_INPUT_HANDLING = "defaultInputHandling";

    private static final String INPUT_EXECUTION_CONSTRAINTS = "inputExecutionConstraintOptions";

    private static final String DEFAULT_INPUT_EXECUTION_CONSTRAINT = "defaultInputExecutionConstraint";

    private static final String STRING_0 = "0";

    private static final String DEFAULT_VALUE = "defaultValue";

    private static final String POSSIBLE_VALUES = "possibleValues";

    /** The icon sizes used in RCE. */
    enum IconSize {

        /** Icon size 16 * 16. */
        ICONSIZE16(16, "icon16.png"),
        /** Icon size 24 * 24. */
        ICONSIZE24(24, "icon24.png"),
        /** Icon size 32 * 32. */
        ICONSIZE32(32, "icon32.png");

        /** The size of the icon. */
        private final int size;

        /**
         * The relative path to a pre-scaled version of the icon. The suffix of this path is used to determine the encoding of the
         * pre-scaled version of the icon.
         */
        private final String path;

        IconSize(int size, String path) {
            this.size = size;
            this.path = path;
        }

        public int getSize() {
            return size;
        }

        public String getPath() {
            return path;
        }
    }

    private static final String COULD_NOT_READ_TOOL_CONFIGURATION = "Could not read tool configuration: ";

    private static final String ERROR_WRITING_TOOL_INTEGRATION_CONFIG_FILE = "Error writing tool integration config file: ";

    private static final BigInteger DOCU_DIRECTORY_MAXIMUM_SIZE = new BigInteger("52428800"); // max 50 MB

    private final Map<String, String> toolNameToPath = Collections.synchronizedMap(new HashMap<>());

    private final Map<String, Map<String, Object>> integratedConfiguration = Collections
        .synchronizedMap(new HashMap<>());

    @Deprecated
    private final Set<String> publishedComponents = Collections.synchronizedSet(new HashSet<>());

    private final Semaphore sequentialToolInitializationSemaphore = new Semaphore(1);

    private final ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();

    // We explicitly do not mark the watchManager as a Reference, as it requires a
    // ToolIntegrationService to be constructed. Hence, OSGI
    // would be unable to instantiate the class during construction of
    // ToolIntegrationService. Hence, we obtain an instance of this class
    // only after all dependencies have been injected, i.e., during the
    // activation-method.
    private ToolIntegrationFileWatcherManager watchManager;

    private ToolIntegrationContextRegistry toolIntegrationContextRegistry;

    private LocalComponentRegistrationService localComponentRegistry;

    private LogicalNodeId localLogicalNodeId;

    private IconHelper iconHelper;

    private final AsyncTaskService asyncTaskService = ConcurrencyUtils.getAsyncTaskService();

    private final CountDownLatch initializationComplete = new CountDownLatch(1);

    private final Log log = LogFactory.getLog(ToolIntegrationServiceImpl.class);

    private Builder fileWatcherManagerBuilder;

    @Override
    public void integrateTool(Map<String, Object> configurationMap, ToolIntegrationContext context) {
        integrateTool(configurationMap, context, true);
    }

    @Override
    public void integrateTool(Map<String, Object> rawConfigurationMap, ToolIntegrationContext context,
        boolean savePublished) {
        final ConfigurationMap configurationMap = context.parseConfigurationMap(rawConfigurationMap).get();
        final String toolComponentID = context.getPrefixForComponentId() + configurationMap.getToolName();
        final String toolClassName = context.getImplementingComponentClassName();

        final File toolDirFile = createToolDirFile(configurationMap, context);
        final byte[] icon16 = iconHelper.getIcon(IconSize.ICONSIZE16, configurationMap, toolDirFile);
        final byte[] icon32 = iconHelper.getIcon(IconSize.ICONSIZE32, configurationMap, toolDirFile);
        final String docuHash = createDocumentationHash(configurationMap, context);
        final String toolName = configurationMap.getToolName();
        final String version = configurationMap.getToolVersion();
        String groupPath = configurationMap.getGroupPath();

        if (!areConfigurationIdsValid(toolName, version, groupPath)) {
            removeTool(toolComponentID, context);
            return;
        }

        EndpointDefinitionsProvider inputProvider;
        EndpointDefinitionsProvider outputProvider;

        ConfigurationDefinition configuration;
        try {
            Set<EndpointDefinition> inputs = createInputs(configurationMap);
            inputProvider = ComponentEndpointModelFactory.createEndpointDefinitionsProvider(inputs);

            Set<EndpointDefinition> outputs = createOutputs(configurationMap);
            outputProvider = ComponentEndpointModelFactory.createEndpointDefinitionsProvider(outputs);

            configuration = configurationMap.generateConfiguration(this);
        } catch (IllegalArgumentException e) {
            log.warn(StringUtils.format("Could not read endpoints from %s: ", toolComponentID), e);
            inputProvider = ComponentEndpointModelFactory
                .createEndpointDefinitionsProvider(new HashSet<EndpointDefinition>());
            outputProvider = ComponentEndpointModelFactory
                .createEndpointDefinitionsProvider(new HashSet<EndpointDefinition>());
            configuration = ComponentConfigurationModelFactory.createEmptyConfigurationDefinition();
        }

        if (groupPath == null || groupPath.isEmpty()) {
            groupPath = context.getDefaultComponentGroupId();
        }

        List<String> supportedIds = new LinkedList<>();
        supportedIds.add(toolComponentID);
        supportedIds.add(StringUtils.format("%s_%s", ToolIntegrationConstants.COMPONENT_IDS[1], configurationMap.getToolName()));
        ComponentInterface componentInterface = new ComponentInterfaceBuilder().setIdentifier(toolComponentID)
            .setIdentifiers(supportedIds)
            .setDisplayName(configurationMap.getToolName())
            .setGroupName(groupPath).setIcon16(icon16).setIcon32(icon32).setDocumentationHash(docuHash)
            .setVersion(version).setInputDefinitionsProvider(inputProvider)
            .setOutputDefinitionsProvider(outputProvider).setConfigurationDefinition(configuration)
            .setConfigurationExtensionDefinitions(new HashSet<ConfigurationExtensionDefinition>())
            .setColor(ComponentConstants.COMPONENT_COLOR_STANDARD)
            .setShape(ComponentConstants.COMPONENT_SHAPE_STANDARD)
            .setSize(ComponentConstants.COMPONENT_SIZE_STANDARD).build();

        String limitExecutionCount = configurationMap.getExecutionCountLimit();
        String maxParallelCountString = configurationMap.getMaxParallelCount();

        Integer maxParallelCount = null;
        if (limitExecutionCount != null && Boolean.parseBoolean(limitExecutionCount) && maxParallelCountString != null
            && !maxParallelCountString.equals("")) {
            maxParallelCount = Integer.parseInt(maxParallelCountString);
            if (maxParallelCount < 1) {
                log.error(StringUtils.format(
                    "A maximum count of parallel executions of %d is invalid, it must be >= 1; a maximum count of 1 is used instead",
                    maxParallelCount));
                maxParallelCount = 1;
            }
        }

        final ComponentRevision componentRevision = new ComponentRevisionBuilder()
            .setComponentInterface(componentInterface)
            .setClassName(toolClassName)
            .build();

        final ComponentInstallation componentInstallation = new ComponentInstallationBuilder()
            .setComponentRevision(componentRevision)
            .setNodeId(localLogicalNodeId)
            .setInstallationId(componentInterface.getIdentifierAndVersion())
            .setMaximumCountOfParallelInstances(maxParallelCount)
            .build();

        if (configurationMap.isActive().orElse(true)) {
            // not setting any publication permissions here; this will be handled by the
            // registration service
            localComponentRegistry.registerOrUpdateLocalComponentInstallation(componentInstallation);
        }

        synchronized (integratedConfiguration) {
            integratedConfiguration.put(toolComponentID, configurationMap.getShallowClone());
        }

        log.debug("ToolIntegration: Registered new Component " + toolComponentID);
    }

    private boolean areConfigurationIdsValid(String toolName, String version, String groupPath) {
        boolean valid = true;
        Optional<String> toolNameValidation = ComponentIdRules.validateComponentIdRules(toolName);
        if (toolNameValidation.isPresent()) {
            log.warn(StringUtils.format(WARNING_INVALID_TOOL, toolName, "tool name", toolNameValidation.get()));
            valid = false;
        }
        if (version != null) {
            Optional<String> versionValidation = ComponentIdRules.validateComponentVersionRules(version);
            if (versionValidation.isPresent()) {
                log.warn(StringUtils.format(WARNING_INVALID_TOOL, toolName, "version", versionValidation.get()));
                valid = false;
            }
        }
        if (groupPath != null && !groupPath.isEmpty()) {
            Optional<String> groupValidation = ComponentGroupPathRules.validateComponentGroupPathRules(groupPath);
            if (groupValidation.isPresent()) {
                log.warn(StringUtils.format(WARNING_INVALID_TOOL, toolName, "group name", groupValidation.get()));
                valid = false;
            }
        }
        return valid;
    }

    private String createDocumentationHash(final ConfigurationMap configurationMap,
        final ToolIntegrationContext context) {
        final File toolDir = createToolDirFile(configurationMap, context);
        final File docDir = new File(toolDir, ToolIntegrationConstants.DOCS_DIR_NAME);
        if (!docDir.exists()) {
            docDir.mkdirs();
        }
        if (docDir.listFiles() != null && docDir.listFiles().length > 0) {
            if (docDir.exists() && validateDocumentationDirectory(docDir)) {
                final byte[] zippedByteArray = FileCompressionService.compressDirectoryToByteArray(docDir,
                    FileCompressionFormat.ZIP, false);

                if (zippedByteArray == null) {
                    log.error("Was not able to create hash for documentation due to an issue with the compression.");
                    return "";
                }

                return DigestUtils.md5Hex(zippedByteArray);
            }
        }
        return "";
    }

    private boolean validateDocumentationDirectory(File docDir) {
        boolean valid = true;
        BigInteger directorySize = FileUtils.sizeOfDirectoryAsBigInteger(docDir);
        if (DOCU_DIRECTORY_MAXIMUM_SIZE.compareTo(directorySize) < 0) {
            log.error(StringUtils.format("Size of documentation directory %s too big (max. 50 Mb).",
                docDir.getAbsolutePath()));
            valid = false;
        }
        for (File f : docDir.listFiles()) {
            if (f.isDirectory()) {
                log.error(StringUtils.format("Directories not allowed in documentation directory %s.",
                    docDir.getAbsolutePath()));
                valid = false;
            } else if (!ArrayUtils.contains(ToolIntegrationConstants.VALID_DOCUMENTATION_EXTENSIONS,
                FilenameUtils.getExtension(f.getName()))) {

                // ignore .nfs files since they are an delete artifact
                if (CrossPlatformFilenameUtils.isNFSFile(f.getName())) {
                    continue;
                }

                log.error(StringUtils.format(
                    "Invalid filetype of %s in documentation directory %s. (Valid filetypes: %s)", f.getName(),
                    docDir.getAbsolutePath(),
                    Arrays.toString(ToolIntegrationConstants.VALID_DOCUMENTATION_EXTENSIONS).replaceAll("\\[", "")
                        .replaceAll("\\]", "")));
                valid = false;
            }
        }
        return valid;
    }

    private File createToolDirFile(ConfigurationMap configurationMap, ToolIntegrationContext context) {
        return new File(
            new File(context.getRootPathToToolIntegrationDirectory(), context.getNameOfToolIntegrationDirectory()),
            configurationMap.getToolName());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void removeTool(String componentID, ToolIntegrationContext context) {
        String toolComponentID = componentID;
        if (!componentID.startsWith(context.getPrefixForComponentId())) {
            toolComponentID = context.getPrefixForComponentId() + componentID;
        }

        if (integratedConfiguration.containsKey(toolComponentID)) {
            String toolIDAndVersion = toolComponentID + ComponentConstants.ID_SEPARATOR
                + ((List<Map<String, String>>) integratedConfiguration.get(toolComponentID)
                    .get(ToolIntegrationConstants.KEY_LAUNCH_SETTINGS)).get(0)
                        .get(ToolIntegrationConstants.KEY_VERSION);
            synchronized (integratedConfiguration) {
                integratedConfiguration.remove(toolComponentID);
            }
            localComponentRegistry.unregisterLocalComponentInstallation(toolIDAndVersion);
        }
        log.debug("ToolIntegration: Removed Component " + toolComponentID);
    }

    @SuppressWarnings("unchecked")
    private Set<EndpointDefinition> createOutputs(ConfigurationMap configurationMap) {
        List<Map<String, String>> definedOutputs = configurationMap.getStaticOutputs();
        Set<EndpointDefinition> outputs = new HashSet<>();
        if (definedOutputs != null) {
            for (Map<String, String> output : definedOutputs) {
                Map<String, Object> description = new HashMap<>();
                description.put(NAME, output.get(ToolIntegrationConstants.KEY_ENDPOINT_NAME));
                description.put(DEFAULT_DATA_TYPE, output.get(ToolIntegrationConstants.KEY_ENDPOINT_DATA_TYPE));
                List<String> dataTypes = new LinkedList<>();
                dataTypes.add(output.get(ToolIntegrationConstants.KEY_ENDPOINT_DATA_TYPE));
                description.put(DATA_TYPES, dataTypes);
                outputs.add(ComponentEndpointModelFactory.createEndpointDefinition(description, EndpointType.OUTPUT));
            }
        }

        List<Map<String, Object>> dynamicOutputs = configurationMap.getDynamicOutputs();

        if (dynamicOutputs != null) {
            for (Map<String, Object> output : dynamicOutputs) {
                Map<String, Object> description = new HashMap<>();
                description.put(IDENTIFIER, output.get(ToolIntegrationConstants.KEY_ENDPOINT_IDENTIFIER));
                description.put(DEFAULT_DATA_TYPE, output.get(ToolIntegrationConstants.KEY_ENDPOINT_DEFAULT_TYPE));
                List<String> dataTypes = new LinkedList<>();
                dataTypes.addAll((List<String>) output.get(ToolIntegrationConstants.KEY_ENDPOINT_DATA_TYPES));
                description.put(DATA_TYPES, dataTypes);

                Map<String, Object> metadata = (Map<String, Object>) output
                    .get(ToolIntegrationConstants.KEY_ENDPOINT_METADATA);
                description.put(META_DATA, metadata);
                outputs.add(ComponentEndpointModelFactory.createEndpointDefinition(description, EndpointType.OUTPUT));
            }
        }
        return outputs;
    }

    @SuppressWarnings("unchecked")
    private Set<EndpointDefinition> createInputs(ConfigurationMap configurationMap) {
        Set<EndpointDefinition> inputs = new HashSet<>();
        for (Map<String, String> input : configurationMap.getStaticInputs()) {
            final EndpointDefinitionBuilder descriptionBuilder = EndpointDefinition.inputBuilder()
                .name(input.get(ToolIntegrationConstants.KEY_ENDPOINT_NAME))
                .defaultDatatype(DataType.valueOf(input.get(ToolIntegrationConstants.KEY_ENDPOINT_DATA_TYPE)))
                .allowedDatatype(DataType.valueOf(input.get(ToolIntegrationConstants.KEY_ENDPOINT_DATA_TYPE)));

            // migration code: usage (required, initial, optional) -> constant, single
            String[] inputHandlings;
            if (input.containsKey(ToolIntegrationConstants.KEY_INPUT_HANDLING)) {
                inputHandlings = StringUtils
                    .splitAndUnescape(input.get(ToolIntegrationConstants.KEY_INPUT_HANDLING));
                if (input.containsKey(ToolIntegrationConstants.KEY_DEFAULT_INPUT_HANDLING)) {
                    descriptionBuilder
                        .defaultInputHandling(InputDatumHandling.valueOf(input.get(ToolIntegrationConstants.KEY_DEFAULT_INPUT_HANDLING)));
                } else {
                    descriptionBuilder.defaultInputHandling(InputDatumHandling.valueOf(inputHandlings[0]));
                }
            } else {
                inputHandlings = new String[] { EndpointDefinition.InputDatumHandling.Single.name() };
                if (input.get(ToolIntegrationConstants.KEY_ENDPOINT_USAGE).equals("initial")) {
                    inputHandlings = new String[] { EndpointDefinition.InputDatumHandling.Constant.name() };
                }
                descriptionBuilder.defaultInputHandling(InputDatumHandling.valueOf(inputHandlings[0]));
            }

            descriptionBuilder.inputHandlings(Arrays.asList(inputHandlings).stream()
                .map(InputDatumHandling::valueOf)
                .collect(Collectors.toList()));

            // migration code: usage (required, initial, optional) -> required, not required
            String[] inputExecutionConstraints;
            if (input.containsKey(ToolIntegrationConstants.KEY_INPUT_EXECUTION_CONSTRAINT)) {
                inputExecutionConstraints = StringUtils
                    .splitAndUnescape(input.get(ToolIntegrationConstants.KEY_INPUT_EXECUTION_CONSTRAINT));
                if (input.containsKey(ToolIntegrationConstants.KEY_DEFAULT_INPUT_EXECUTION_CONSTRAINT)) {
                    descriptionBuilder.defaultInputExecutionConstraint(InputExecutionContraint.valueOf(
                        input.get(ToolIntegrationConstants.KEY_DEFAULT_INPUT_EXECUTION_CONSTRAINT)));
                } else {
                    descriptionBuilder.defaultInputExecutionConstraint(InputExecutionContraint.valueOf(
                        inputExecutionConstraints[0]));
                }
            } else {
                inputExecutionConstraints = new String[] {
                    EndpointDefinition.InputExecutionContraint.Required.name() };
                if (input.get(ToolIntegrationConstants.KEY_ENDPOINT_USAGE).equals("optional")) {
                    inputExecutionConstraints = new String[] {
                        EndpointDefinition.InputExecutionContraint.NotRequired.name() };
                }
                descriptionBuilder.defaultInputExecutionConstraint(InputExecutionContraint.valueOf(
                    inputExecutionConstraints[0]));
            }
            descriptionBuilder.inputExecutionConstraints(
                Arrays.asList(inputExecutionConstraints).stream().map(InputExecutionContraint::valueOf).collect(Collectors.toList()));

            Map<String, Map<String, Object>> metadata = new HashMap<>();
            if ((input.get(ToolIntegrationConstants.KEY_ENDPOINT_DATA_TYPE).equals(DataType.FileReference.name())
                || input.get(ToolIntegrationConstants.KEY_ENDPOINT_DATA_TYPE)
                    .equals(DataType.DirectoryReference.name()))
                && input.get(ToolIntegrationConstants.KEY_ENDPOINT_FILENAME) != null) {
                Map<String, Object> metadataFilename = new HashMap<>();
                metadataFilename.put(EndpointDefinitionConstants.KEY_GUI_NAME, "Filename");
                metadataFilename.put(EndpointDefinitionConstants.KEY_GUI_POSITION, STRING_0);
                metadataFilename.put(EndpointDefinitionConstants.KEY_GUIGROUP, "File");
                List<String> possibleValuesListFilename = new LinkedList<>();
                possibleValuesListFilename.add(input.get(ToolIntegrationConstants.KEY_ENDPOINT_FILENAME));
                metadataFilename.put(POSSIBLE_VALUES, possibleValuesListFilename);
                metadataFilename.put(DEFAULT_VALUE, input.get(ToolIntegrationConstants.KEY_ENDPOINT_FILENAME));
                metadataFilename.put(EndpointDefinitionConstants.KEY_VISIBILITY,
                    Visibility.developerConfigurable.toString());
                metadata.put(ToolIntegrationConstants.KEY_ENDPOINT_FILENAME, metadataFilename);
            }

            descriptionBuilder.metadata(metadata);
            inputs.add(descriptionBuilder.build());
        }

        if (configurationMap.containsDynamicInputs()) {
            List<Map<String, Object>> dynamicInputs = configurationMap.getDynamicInputs();
            for (Map<String, Object> input : dynamicInputs) {
                Map<String, Object> description = new HashMap<>();
                description.put(IDENTIFIER, input.get(ToolIntegrationConstants.KEY_ENDPOINT_IDENTIFIER));
                description.put(DEFAULT_DATA_TYPE, input.get(ToolIntegrationConstants.KEY_ENDPOINT_DEFAULT_TYPE));
                List<String> dataTypes = new LinkedList<>();
                dataTypes.addAll((List<String>) input.get(ToolIntegrationConstants.KEY_ENDPOINT_DATA_TYPES));
                description.put(DATA_TYPES, dataTypes);

                Map<String, Object> metadata = (Map<String, Object>) input
                    .get(ToolIntegrationConstants.KEY_ENDPOINT_METADATA);
                description.put(META_DATA, metadata);

                // migration code: usage (required, initial, optional) -> consuming vs.
                // immutable
                // and required vs. required if connected
                if (metadata.containsKey("usage")) {
                    description.put(DEFAULT_INPUT_HANDLING, EndpointDefinition.InputDatumHandling.Single.name());
                    List<String> inputHandlingOptions = new LinkedList<>();
                    inputHandlingOptions.add(EndpointDefinition.InputDatumHandling.Single.name());
                    inputHandlingOptions.add(EndpointDefinition.InputDatumHandling.Constant.name());
                    description.put(INPUT_HANDLINGS, inputHandlingOptions);
                    description.put(DEFAULT_INPUT_EXECUTION_CONSTRAINT,
                        EndpointDefinition.InputExecutionContraint.Required.name());
                    List<String> inputinputExecutionConstraintOptions = new LinkedList<>();
                    inputinputExecutionConstraintOptions
                        .add(EndpointDefinition.InputExecutionContraint.Required.name());
                    inputinputExecutionConstraintOptions
                        .add(EndpointDefinition.InputExecutionContraint.RequiredIfConnected.name());
                    description.put(INPUT_EXECUTION_CONSTRAINTS, inputinputExecutionConstraintOptions);
                    metadata.remove("usage");
                } else {
                    if (input.containsKey(ToolIntegrationConstants.KEY_INPUT_HANDLING_OPTIONS)) {
                        description.put(DEFAULT_INPUT_HANDLING,
                            input.get(ToolIntegrationConstants.KEY_DEFAULT_INPUT_HANDLING));
                        List<String> inputHandlingOptions = new LinkedList<>();
                        inputHandlingOptions
                            .addAll((List<String>) input.get(ToolIntegrationConstants.KEY_INPUT_HANDLING_OPTIONS));
                        description.put(INPUT_HANDLINGS, inputHandlingOptions);
                    }

                    if (input.containsKey(ToolIntegrationConstants.KEY_INPUT_EXECUTION_CONSTRAINT_OPTIONS)) {
                        description.put(DEFAULT_INPUT_EXECUTION_CONSTRAINT,
                            input.get(ToolIntegrationConstants.KEY_DEFAULT_INPUT_EXECUTION_CONSTRAINT));
                        List<String> inputinputExecutionConstraintOptions = new LinkedList<>();
                        inputinputExecutionConstraintOptions.addAll((List<String>) input
                            .get(ToolIntegrationConstants.KEY_INPUT_EXECUTION_CONSTRAINT_OPTIONS));
                        description.put(INPUT_EXECUTION_CONSTRAINTS, inputinputExecutionConstraintOptions);
                    }
                }
                inputs.add(ComponentEndpointModelFactory.createEndpointDefinition(description, EndpointType.INPUT));
            }
        }
        return inputs;
    }

    /**
     * Reads all previously added {@link ComponentInterface}s.
     * 
     * @param information about the tools to be integrated.
     */
    private void initializeExistingToolsInContext(ToolIntegrationContext context) {
        String configFolder = context.getRootPathToToolIntegrationDirectory();
        File toolIntegrationFile = new File(configFolder, context.getNameOfToolIntegrationDirectory());
        readPublishedComponents(context);
        if (toolIntegrationFile.exists() && toolIntegrationFile.isDirectory()
            && toolIntegrationFile.listFiles().length > 0) {
            log.debug("Initializing tool integration root directory " + toolIntegrationFile.getAbsolutePath());
            for (File toolFolder : toolIntegrationFile.listFiles()) {
                if (toolFolder.isDirectory() && !toolFolder.getName().equals("null")) { // to review: why is this "null"
                                                                                        // check needed?
                    log.debug("Initializing tool directory " + toolFolder.getAbsolutePath());
                    try {
                        readToolDirectory(toolFolder, context);
                    } catch (RuntimeException e) {
                        final String errorMessage =
                            StringUtils.format("Exception caught during integration of tool directory %s", toolFolder.getAbsolutePath());
                        log.error(errorMessage, e);
                    }
                }
            }
        }
    }

    /**
     * Reads the given configuration file and integrates it as a tool.
     * 
     * @param toolFolder the configuration folder
     * @param context used for integration
     */
    @SuppressWarnings("unchecked")
    private void readToolDirectory(File toolFolder, ToolIntegrationContext context) {
        File configFile = new File(toolFolder, context.getConfigurationFilename());
        if (configFile.exists() && configFile.isFile()) {
            try {
                Map<String, Object> configurationMap = mapper.readValue(configFile,
                    new HashMap<String, Object>().getClass());
                if (!integratedConfiguration.containsKey(context.getPrefixForComponentId()
                    + configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME))) {
                    toolNameToPath.put((String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME),
                        toolFolder.getAbsolutePath());

                    if (!isToolIntegrated(configurationMap, context)) {
                        integrateTool(configurationMap, context);
                    }

                } else {
                    log.warn("Tool with foldername already exists:  " + toolFolder.getName());
                }
            } catch (IOException e) {
                log.error(COULD_NOT_READ_TOOL_CONFIGURATION, e);
            }
        }
    }

    @Override
    public void writeToolIntegrationFileToSpecifiedFolder(String folder, Map<String, Object> rawConfigurationMap,
        ToolIntegrationContext information) throws IOException {
        final ConfigurationMap configurationMap = information.parseConfigurationMap(rawConfigurationMap).get();

        if (!configurationMap.hasIntegrationVersion()) {
            configurationMap.setIntegrationVersion(ToolIntegrationConstants.CURRENT_TOOLINTEGRATION_VERSION);
        }

        configurationMap.applyMigration(this::migrateDeprecatedInstanceLimitKey);

        File toolConfigFile = new File(folder, information.getNameOfToolIntegrationDirectory() + File.separator
            + information.getToolDirectoryPrefix() + configurationMap.getToolName());
        toolConfigFile.mkdirs();

        iconHelper.prescaleAndCopyIcon(configurationMap, toolConfigFile);

        handleDoc(configurationMap, toolConfigFile);
        // deprecated, remove when done; see Mantis #16044
        // configurationMap.remove(ToolIntegrationConstants.TEMP_KEY_PUBLISH_COMPONENT);
        Map<String, Object> sortedMap = configurationMap.getShallowClone();
        mapper.writerWithDefaultPrettyPrinter()
            .writeValue(new File(toolConfigFile, information.getConfigurationFilename()), sortedMap);
        toolNameToPath.put(configurationMap.getToolName(), toolConfigFile.getAbsolutePath());
    }

    private void migrateDeprecatedInstanceLimitKey(Map<String, Object> backingMap) {
        // In older versions of RCE, there was a typo in the key used for setting the maximal number of instances to run in parallel. This
        // typo has been fixed in newer versions, but we still have to expect configuration maps that use the old key. Hence, we silently
        // migrate such configuration maps to the new key here
        @SuppressWarnings("unchecked") Map<String, String> launchSettings = ((List<Map<String, String>>) backingMap
            .get(ToolIntegrationConstants.KEY_LAUNCH_SETTINGS)).get(0);
        String value = launchSettings.remove(ToolIntegrationConstants.KEY_LIMIT_INSTANCES_OLD);
        if (!launchSettings.containsKey(ToolIntegrationConstants.KEY_LIMIT_INSTANCES) && value != null) {
            launchSettings.put(ToolIntegrationConstants.KEY_LIMIT_INSTANCES, value);
        }
    }

    private void handleDoc(ConfigurationMap configurationMap, File toolConfigFile) {
        if ((!configurationMap.containsDocFilePath()) || configurationMap.getDocFilePath().isEmpty()) {
            return;
        }

        final File docfile = new File(configurationMap.getDocFilePath());
        File docDir = new File(toolConfigFile, ToolIntegrationConstants.DOCS_DIR_NAME);

        if (docfile.isAbsolute() && docfile.exists() && docfile.isFile()) {
            if (docDir.exists() && docDir.listFiles().length > 0) {
                for (File f : docDir.listFiles()) {
                    try {
                        FileUtils.forceDelete(f);
                    } catch (IOException e) {
                        log.error("Could not delete old documentation file: " + f.getAbsolutePath() + ": "
                            + e.getMessage());
                    }
                }
            }
            File destination = new File(docDir, docfile.getName());
            if (!destination.getAbsolutePath().equals(docfile.getAbsolutePath())) {
                try {
                    FileUtils.copyFile(docfile, destination);
                    configurationMap.setDocFilePath(docfile.getName());
                } catch (IOException e) {
                    log.error("Could not copy documentation to tool directory: ", e);
                }
            }
        }
    }

    @Override
    public void writeToolIntegrationFile(Map<String, Object> configurationMap, ToolIntegrationContext information)
        throws IOException {
        String configFolder = information.getRootPathToToolIntegrationDirectory();
        writeToolIntegrationFileToSpecifiedFolder(configFolder, configurationMap, information);
    }

    private void readPublishedComponents(ToolIntegrationContext context) {
        File toolsfolder = new File(context.getRootPathToToolIntegrationDirectory(),
            context.getNameOfToolIntegrationDirectory());
        if (toolsfolder.exists()) {
            try {
                File publishedComponentsFile = new File(toolsfolder,
                    ToolIntegrationConstants.PUBLISHED_COMPONENTS_FILENAME);
                // deprecated mechanism - the file is only checked to log backwards
                // compatibility warnings; see Mantis #16044
                if (publishedComponentsFile.isFile()) {
                    Set<String> newPublishedComponents = new HashSet<>(FileUtils.readLines(publishedComponentsFile));
                    for (String newComp : newPublishedComponents) {
                        String comp = newComp.trim();
                        if (comp.isEmpty()) {
                            continue;
                        }
                        // log a deprecation warning for existing entries; see Mantis #16044
                        log.warn("Read the deprecated component publication entry \"" + comp
                            + "\" from configuration file " + publishedComponentsFile.getAbsolutePath()
                            + ", but it will not be applied; use the component authorization system to publish integrated tools. "
                            + "Delete this file to get rid of this warning.");
                    }
                }
            } catch (IOException e) {
                log.error(ERROR_WRITING_TOOL_INTEGRATION_CONFIG_FILE, e);
            }
        }
    }

    @Override
    public synchronized Map<String, Object> getToolConfiguration(String toolId) {
        return integratedConfiguration.get(toolId);
    }

    @Override
    public synchronized Set<String> getIntegratedComponentIds() {
        return integratedConfiguration.keySet();
    }

    @Override
    public synchronized Set<String> getActiveComponentIds() {
        Set<String> activeIds = new HashSet<>();
        for (String key : integratedConfiguration.keySet()) {
            if (integratedConfiguration.get(key).get(ToolIntegrationConstants.IS_ACTIVE) == null
                || (Boolean) integratedConfiguration.get(key).get(ToolIntegrationConstants.IS_ACTIVE)) {
                activeIds.add(key);
            }
        }
        return activeIds;
    }

    /**
     * This method will be called when the bundle is started.
     * 
     * @param context of the bundle
     */
    @Activate
    protected void activate(BundleContext bundleContext) {
        watchManager = fileWatcherManagerBuilder.build(this);

        asyncTaskService.execute("Initialize all provided ToolIntegrationContexts", () -> {

            ToolIntegrationContext context;
            while (bundleContext.getBundle().getState() == Bundle.ACTIVE
                && (context = toolIntegrationContextRegistry.fetchNextUninitializedToolIntegrationContext()) != null) {
                if (!CommandLineArguments.isDoNotStartComponentsRequested()) {
                    log.debug("Registering " + ToolIntegrationContext.class.getSimpleName() + " " + context.getContextId());
                    integrateToolIntegrationContext(context);
                }
            }
            log.debug("Finished processing tool integration contexts from " + ToolIntegrationContext.class.getSimpleName() + " queue");
            initializationComplete.countDown();

            // note: this is only a band-aid fix until the structural startup/shutdown issues are addressed;
            // this fix still leaves a minor risk of a race condition, although MUCH smaller than without it -- misc_ro
            if (bundleContext.getBundle().getState() == Bundle.ACTIVE) {
                localComponentRegistry.reportToolIntegrationRegistrationComplete();
            } else {
                log.debug("Skipping 'tool integration complete' trigger as this bundle seems to be shutting down");
            }
        });
    }

    /**
     * Deactivate the service (e.g. unregister watcher).
     */
    @Deactivate
    protected void deactivateIntegrationService() {
        try {
            if (!initializationComplete.await(10, TimeUnit.SECONDS)) {
                log.warn("Exceeded timeout while waiting for initialization to finish");
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for initialization to finish", e);
        }
        watchManager.shutdown();
    }

    @Reference
    protected void bindToolIntegrationContextRegistry(ToolIntegrationContextRegistry newInstance) {
        this.toolIntegrationContextRegistry = newInstance;
    }

    @Reference
    protected void bindFileWatcherManagerBuilder(Builder newInstance) {
        this.fileWatcherManagerBuilder = newInstance;
    }

    @Reference(unbind = "unbindComponentRegistry")
    protected void bindComponentRegistry(LocalComponentRegistrationService newRegistry) {
        localComponentRegistry = newRegistry;
    }

    protected void unbindComponentRegistry(LocalComponentRegistrationService newRegistry) {
        localComponentRegistry = ServiceUtils.createFailingServiceProxy(LocalComponentRegistrationService.class);
    }

    @Reference
    protected void bindPlatformService(PlatformService newService) {
        // fetch and store the node id; decoupling this from the service also guards it
        // against access from asynchronous task runs after this service was shut down
        localLogicalNodeId = newService.getLocalDefaultLogicalNodeId();
    }

    @Override
    public String getPathOfComponentID(String id, ToolIntegrationContext context) {
        if (id.startsWith(context.getPrefixForComponentId())) {
            return toolNameToPath.get(id.substring(context.getPrefixForComponentId().length()));
        }
        return toolNameToPath.get(id);
    }

    @Override
    public boolean isToolIntegrated(Map<String, Object> configurationMap, ToolIntegrationContext integrationContext) {
        String toolComponentID = integrationContext.getPrefixForComponentId()
            + (String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME);
        return integratedConfiguration.keySet().contains(toolComponentID);
    }

    @Override
    public String getToolNameToPath(String path) {
        for (Entry<String, String> e : toolNameToPath.entrySet()) {
            if (e.getValue().trim().equals(path.trim())) {
                return e.getKey();
            }
        }
        return null;
    }

    @Override
    public void putToolNameToPath(String toolName, File parentFile) {
        toolNameToPath.put(toolName, parentFile.getAbsolutePath());
    }

    @Override
    public void updatePublishedComponents(ToolIntegrationContext context) {
        // note: this code is still left in place to prevent side effects, but no
        // publication settings are loaded anymore;
        // see see Mantis #16044
        Set<String> oldPublishedComponents = new HashSet<>();
        synchronized (publishedComponents) {
            oldPublishedComponents.addAll(publishedComponents);
            Set<String> toRemove = new HashSet<>();
            for (String path : publishedComponents) {
                if (path.startsWith(context.getRootPathToToolIntegrationDirectory() + File.separator
                    + context.getNameOfToolIntegrationDirectory())) {
                    toRemove.add(path);
                }
            }
            publishedComponents.removeAll(toRemove);
            readPublishedComponents(context);
            Set<String> addPublished = new HashSet<>();
            for (String newComp : publishedComponents) {
                if (!oldPublishedComponents.contains(newComp)
                    && newComp.startsWith(context.getRootPathToToolIntegrationDirectory() + File.separator
                        + context.getNameOfToolIntegrationDirectory())) {
                    addPublished.add(newComp);
                }
            }
            Set<String> removePublished = new HashSet<>();
            for (String oldComp : oldPublishedComponents) {
                if (!publishedComponents.contains(oldComp)
                    && oldComp.startsWith(context.getRootPathToToolIntegrationDirectory() + File.separator
                        + context.getNameOfToolIntegrationDirectory())) {
                    removePublished.add(oldComp);
                }
            }

            for (String path : addPublished) {
                String toolComponentID = context.getPrefixForComponentId() + getToolNameToPath(path);
                Map<String, Object> configuration = integratedConfiguration.get(toolComponentID);
                removeTool(toolComponentID, context);
                if (configuration != null) {
                    integrateTool(configuration, context, false);
                }
            }
            for (String path : removePublished) {
                String toolComponentID = context.getPrefixForComponentId() + getToolNameToPath(path);
                Map<String, Object> configuration = integratedConfiguration.get(toolComponentID);
                removeTool(toolComponentID, context);
                if (configuration != null) {
                    integrateTool(configuration, context, false);
                }
            }
        }
    }

    @Override
    public byte[] getToolDocumentation(final String identifier) throws RemoteOperationException {
        final ToolIntegrationContext context = getContextForIdentifier(identifier);
        if (context == null) {
            return null;
        }

        String name = identifier.substring(context.getPrefixForComponentId().length());
        name = name.substring(0, name.indexOf(ComponentConstants.ID_SEPARATOR));

        final File sourceDirectory = new File(toolNameToPath.get(name), ToolIntegrationConstants.DOCS_DIR_NAME);

        final byte[] resultData = FileCompressionService.compressDirectoryToByteArray(sourceDirectory,
            FileCompressionFormat.ZIP, false);

        if (resultData == null) {
            log.error("Was not able to create an archive for documentation due to a compression issue.");
            return null;
        }

        return resultData;
    }

    /**
     * @param identifier The identifier of some component, including component id and version.
     * @return The {@link ToolIntegrationContext} that corresponds to the given component ID or null if none exists.
     */
    private ToolIntegrationContext getContextForIdentifier(String identifier) {
        final ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        final ToolIntegrationContextRegistry contextRegistry = serviceRegistryAccess.getService(ToolIntegrationContextRegistry.class);

        final Optional<ToolIntegrationContext> result = contextRegistry.getAllIntegrationContexts().stream()
            .filter(context -> identifier.startsWith(context.getPrefixForComponentId()))
            .findAny();

        return result.orElse(null);
    }

    @Override
    public void setFileWatcherActive(boolean value) {
        watchManager.setAllWatcherActivity(value);
    }

    @Override
    public void unregisterIntegration(String previousToolName, ToolIntegrationContext integrationContext) {
        watchManager.unregister(previousToolName, integrationContext);
    }

    @Override
    public void registerRecursive(String toolName, ToolIntegrationContext integrationContext) {
        watchManager.registerRecursive(toolName, integrationContext);

    }

    private void integrateToolIntegrationContext(ToolIntegrationContext context) {
        try {
            sequentialToolInitializationSemaphore.acquire();
            try {
                log.debug("Initializing existing tools for context " + context.getContextType());
                initializeExistingToolsInContext(context);

                log.debug("Creating tool watcher for context " + context.getContextType());
                watchManager.createWatcherForToolRootDirectory(context);
                updatePublishedComponents(context);

                log.debug("Finished initialization of context " + context.getContextType());
            } finally {
                sequentialToolInitializationSemaphore.release();
            }
        } catch (InterruptedException e) {
            log.debug("Interrupted during tool integration");
            Thread.currentThread().interrupt();
            return;
        }
    }

    @Reference
    private void bindIconHelper(IconHelper newInstance) {
        this.iconHelper = newInstance;
    }

    private void unbindIconHelper(IconHelper oldInstance) {
        this.iconHelper = null;
    }

}
