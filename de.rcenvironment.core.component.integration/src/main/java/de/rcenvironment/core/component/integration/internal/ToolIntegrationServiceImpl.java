/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.integration.internal;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.TreeMap;
import java.util.concurrent.Semaphore;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageInputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentIdRules;
import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.component.integration.ToolIntegrationContext;
import de.rcenvironment.core.component.integration.ToolIntegrationContextRegistry;
import de.rcenvironment.core.component.integration.ToolIntegrationService;
import de.rcenvironment.core.component.integration.internal.ToolIntegrationFileWatcherManager.Builder;
import de.rcenvironment.core.component.management.api.LocalComponentRegistrationService;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInstallationBuilder;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.api.ComponentInterfaceBuilder;
import de.rcenvironment.core.component.model.api.ComponentRevisionBuilder;
import de.rcenvironment.core.component.model.configuration.api.ComponentConfigurationModelFactory;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDefinition;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDefinitionConstants;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationExtensionDefinition;
import de.rcenvironment.core.component.model.endpoint.api.ComponentEndpointModelFactory;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinitionConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinitionsProvider;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataConstants.Visibility;
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
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.incubator.ImageResize;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

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

    private static final int ICONSIZE32 = 32;

    private static final int ICONSIZE16 = 16;

    private static final String COULD_NOT_READ_TOOL_CONFIGURATION = "Could not read tool configuration: ";

    private static final String ERROR_WRITING_TOOL_INTEGRATION_CONFIG_FILE = "Error writing tool integration config file: ";

    private static final BigInteger DOCU_DIRECTORY_MAXIMUM_SIZE = new BigInteger("52428800"); // max 50 MB

    private final Map<String, String> toolNameToPath = Collections.synchronizedMap(new HashMap<>());

    private final Map<String, Map<String, Object>> integratedConfiguration = Collections.synchronizedMap(new HashMap<>());

    @Deprecated
    private final Set<String> publishedComponents = Collections.synchronizedSet(new HashSet<>());

    private final Semaphore sequentialToolInitializationSemaphore = new Semaphore(1);

    private final ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();

    // We explicitly do not mark the watchManager as a Reference, as it requires a ToolIntegrationService to be constructed. Hence, OSGI
    // would be unable to instantiate the class during construction of ToolIntegrationService. Hence, we obtain an instance of this class
    // only after all dependencies have been injected, i.e., during the activation-method.
    private ToolIntegrationFileWatcherManager watchManager;

    private ToolIntegrationContextRegistry toolIntegrationContextRegistry;

    private LocalComponentRegistrationService localComponentRegistry;

    private LogicalNodeId localLogicalNodeId;

    private AuthorizationService authorizationService;

    private final AsyncTaskService asyncTaskService = ConcurrencyUtils.getAsyncTaskService();

    private final Log log = LogFactory.getLog(ToolIntegrationServiceImpl.class);

    private Builder fileWatcherManagerBuilder;

    @Override
    public void integrateTool(Map<String, Object> configurationMap, ToolIntegrationContext context) {
        integrateTool(configurationMap, context, true);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void integrateTool(Map<String, Object> configurationMap, ToolIntegrationContext context, boolean savePublished) {
        byte[] icon16 = readIcons(ICONSIZE16, configurationMap, context);
        byte[] icon32 = readIcons(ICONSIZE32, configurationMap, context);
        String docuHash = createDocumentationHash(configurationMap, context);
        String toolName = (String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME);
        String toolComponentID = context.getPrefixForComponentId()
            + toolName;
        String toolClassName = context.getImplementingComponentClassName();
        String version = ((Map<String, String>) ((List<Object>) configurationMap.get(ToolIntegrationConstants.KEY_LAUNCH_SETTINGS)).get(0))
            .get(ToolIntegrationConstants.KEY_VERSION);
        String groupName = (String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_GROUPNAME);

        if (!areConfigurationIdsValide(toolName, version, groupName)) {
            removeTool(toolComponentID, context);
            return;
        }

        EndpointDefinitionsProvider inputProvider;
        EndpointDefinitionsProvider outputProvider;
        // tool publication mixed into tool integration is deprecated; see Mantis #16044
        // boolean isPublished = false;
        // readPublishedComponents(context);
        // if ((publishedComponents.contains(configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME)) || publishedComponents
        // .contains(context.getRootPathToToolIntegrationDirectory() + File.separator
        // + context.getNameOfToolIntegrationDirectory()
        // + File.separator + (String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME)))) {
        // isPublished = true;
        // }
        ConfigurationDefinition configuration;
        try {
            Set<EndpointDefinition> inputs = createInputs(configurationMap);
            inputProvider = ComponentEndpointModelFactory.createEndpointDefinitionsProvider(inputs);

            Set<EndpointDefinition> outputs = createOutputs(configurationMap);
            outputProvider = ComponentEndpointModelFactory.createEndpointDefinitionsProvider(outputs);

            configuration = generateConfiguration(configurationMap);
        } catch (IllegalArgumentException e) {
            log.warn("Could not read endpoints from " + toolComponentID + ": ", e);
            inputProvider = ComponentEndpointModelFactory.createEndpointDefinitionsProvider(new HashSet<EndpointDefinition>());
            outputProvider = ComponentEndpointModelFactory.createEndpointDefinitionsProvider(new HashSet<EndpointDefinition>());
            configuration =
                ComponentConfigurationModelFactory.createConfigurationDefinition(new LinkedList<>(), new LinkedList<>(),
                    new LinkedList<>(), new HashMap<String, String>());
        }
        if (groupName == null || groupName.isEmpty()) {
            groupName = context.getComponentGroupId();
        }
        List<String> supportedIds = new LinkedList<>();
        supportedIds.add(toolComponentID);
        supportedIds.add(ToolIntegrationConstants.COMPONENT_IDS[1] + "_"
            + (String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME));
        ComponentInterface componentInterface =
            new ComponentInterfaceBuilder()
                .setIdentifier(toolComponentID)
                .setIdentifiers(supportedIds)
                .setDisplayName((String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME))
                .setGroupName(groupName)
                .setIcon16(icon16)
                .setIcon32(icon32)
                .setDocumentationHash(docuHash)
                .setVersion(version)
                .setInputDefinitionsProvider(inputProvider).setOutputDefinitionsProvider(outputProvider)
                .setConfigurationDefinition(configuration)
                .setConfigurationExtensionDefinitions(new HashSet<ConfigurationExtensionDefinition>())
                .setColor(ComponentConstants.COMPONENT_COLOR_STANDARD)
                .setShape(ComponentConstants.COMPONENT_SHAPE_STANDARD)
                .setSize(ComponentConstants.COMPONENT_SIZE_STANDARD)
                .build();

        String limitExecutionCount = "";
        if ((((List<Map<String, String>>) configurationMap.get(ToolIntegrationConstants.KEY_LAUNCH_SETTINGS)).get(0))
            .get(ToolIntegrationConstants.KEY_LIMIT_INSTANCES) != null) {
            limitExecutionCount = (((List<Map<String, String>>) configurationMap.get(ToolIntegrationConstants.KEY_LAUNCH_SETTINGS)).get(0))
                .get(ToolIntegrationConstants.KEY_LIMIT_INSTANCES);
        } else {
            limitExecutionCount = (((List<Map<String, String>>) configurationMap.get(ToolIntegrationConstants.KEY_LAUNCH_SETTINGS)).get(0))
                .get(ToolIntegrationConstants.KEY_LIMIT_INSTANCES_OLD);
        }
        String maxParallelCountString =
            (((List<Map<String, String>>) configurationMap.get(ToolIntegrationConstants.KEY_LAUNCH_SETTINGS)).get(0))
                .get(ToolIntegrationConstants.KEY_LIMIT_INSTANCES_COUNT);

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
        ComponentInstallation ci =
            new ComponentInstallationBuilder()
                .setComponentRevision(
                    new ComponentRevisionBuilder()
                        .setComponentInterface(componentInterface)
                        .setClassName(toolClassName).build())
                .setNodeId(localLogicalNodeId)
                // For testing purposes:
                // .setNodeId(platformService.createTransientLocalLogicalNodeId())
                .setInstallationId(componentInterface.getIdentifierAndVersion())
                .setMaximumCountOfParallelInstances(maxParallelCount)
                .build();
        if (configurationMap.get(ToolIntegrationConstants.IS_ACTIVE) == null
            || (Boolean) configurationMap.get(ToolIntegrationConstants.IS_ACTIVE)) {
            // not setting any publication permissions here; this will be handled by the registration service
            localComponentRegistry.registerOrUpdateLocalComponentInstallation(ci);
        }

        synchronized (integratedConfiguration) {
            integratedConfiguration.put(toolComponentID, configurationMap);
        }
        log.debug("ToolIntegration: Registered new Component " + toolComponentID);
    }

    private boolean areConfigurationIdsValide(String toolName, String version, String groupName) {
        boolean valid = true;
        Optional<String> toolNameValidation = ComponentIdRules.validateComponentIdRules(toolName);
        if (toolNameValidation.isPresent()) {
            log.warn(StringUtils.format(WARNING_INVALID_TOOL, toolName, "tool name",
                toolNameValidation.get()));
            valid = false;
        }
        if (version != null) {
            Optional<String> versionValidation = ComponentIdRules.validateComponentVersionRules(version);
            if (versionValidation.isPresent()) {
                log.warn(StringUtils.format(WARNING_INVALID_TOOL, toolName, "version",
                    versionValidation.get()));
                valid = false;
            }
        }
        if (groupName != null && !groupName.isEmpty()) {
            Optional<String> groupValidation = ComponentIdRules.validateComponentGroupNameRules(groupName);
            if (groupValidation.isPresent()) {
                log.warn(StringUtils.format(WARNING_INVALID_TOOL, toolName, "group name",
                    groupValidation.get()));
                valid = false;
            }
        }
        return valid;
    }

    private String createDocumentationHash(final Map<String, Object> configurationMap, final ToolIntegrationContext context) {
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
            log.error(StringUtils.format("Size of documentation directory %s too big (max. 50 Mb).", docDir.getAbsolutePath()));
            valid = false;
        }
        for (File f : docDir.listFiles()) {
            if (f.isDirectory()) {
                log.error(StringUtils.format("Directories not allowed in documentation directory %s.", docDir.getAbsolutePath()));
                valid = false;
            } else if (!ArrayUtils.contains(
                ToolIntegrationConstants.VALID_DOCUMENTATION_EXTENSIONS, FilenameUtils.getExtension(f.getName()))) {

                // ignore .nfs files since they are an delete artifact
                if (CrossPlatformFilenameUtils.isNFSFile(f.getName())) {
                    continue;
                }

                log.error(
                    StringUtils.format("Invalid filetype of %s in documentation directory %s. (Valid filetypes: %s)", f.getName(),
                        docDir.getAbsolutePath(),
                        Arrays.toString(ToolIntegrationConstants.VALID_DOCUMENTATION_EXTENSIONS).replaceAll("\\[", "").replaceAll("\\]",
                            "")));
                valid = false;
            }
        }
        return valid;
    }

    private File createToolDirFile(Map<String, Object> configurationMap, ToolIntegrationContext context) {
        return new File(new File(context.getRootPathToToolIntegrationDirectory(), context.getNameOfToolIntegrationDirectory()),
            (String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME));
    }

    private byte[] readIcons(int size, Map<String, Object> configurationMap, ToolIntegrationContext context) {
        byte[] iconArray = null;
        iconArray = readDefaultToolIcon(size);
        String iconPath = (String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_ICON_PATH);

        if (iconPath != null && !iconPath.isEmpty()) {
            if (!new File(iconPath).isAbsolute()) {
                iconPath = context.getRootPathToToolIntegrationDirectory() + File.separator + context.getNameOfToolIntegrationDirectory()
                    + File.separator + context.getToolDirectoryPrefix() + configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME)
                    + File.separator + iconPath;
            }
            File icon = new File(iconPath);
            if (icon.exists() && icon.isFile()) {
                try {
                    final File iconsource = icon;
                    final File icontarget = TempFileServiceAccess.getInstance().createTempFileFromPattern("icon_" + size + "*.png");
                    Image image = ImageIO.read(icon);
                    if (image == null) {
                        iconArray = readDefaultToolIcon(size);
                    } else {
                        if (iconsource.exists()) {
                            try (FileImageInputStream imageInputStream = new FileImageInputStream(iconsource)) {
                                BufferedImage bi = ImageResize.resize(ImageIO.read(imageInputStream), size);
                                if (bi != null && icontarget != null) {
                                    ImageIO.write(bi, "PNG", icontarget);
                                    iconArray = FileUtils.readFileToByteArray(icontarget);
                                    TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(icontarget);
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    log.debug("Could not load icon, use default icon");
                }
            }
        }
        return iconArray;
    }

    private byte[] readDefaultToolIcon(int iconSize) {
        try (InputStream inputStream = ToolIntegrationServiceImpl.class.getResourceAsStream("/resources/icons/tool" + iconSize + ".png")) {
            return IOUtils.toByteArray(inputStream);
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
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
                + ((List<Map<String, String>>) integratedConfiguration
                    .get(toolComponentID).get(ToolIntegrationConstants.KEY_LAUNCH_SETTINGS)).get(0)
                        .get(ToolIntegrationConstants.KEY_VERSION);
            synchronized (integratedConfiguration) {
                integratedConfiguration.remove(toolComponentID);
            }
            localComponentRegistry.unregisterLocalComponentInstallation(toolIDAndVersion);
        }
        log.debug("ToolIntegration: Removed Component " + toolComponentID);
    }

    private ConfigurationDefinition generateConfiguration(Map<String, Object> configurationMap) {
        List<Object> configuration = new LinkedList<>();
        List<Object> configurationMetadata = new LinkedList<>();
        readConfigurationWithMetaDataToLists(configurationMap, configuration, configurationMetadata);
        Map<String, String> readOnlyConfiguration = createReadOnlyConfiguration(configurationMap);
        return ComponentConfigurationModelFactory.createConfigurationDefinition(configuration, new LinkedList<>(),
            configurationMetadata, readOnlyConfiguration);
    }

    @SuppressWarnings("unchecked")
    private void readConfigurationWithMetaDataToLists(Map<String, Object> configurationMap, List<Object> configuration,
        List<Object> configurationMetadata) {
        Map<String, Object> properties = (Map<String, Object>) configurationMap.get(ToolIntegrationConstants.KEY_PROPERTIES);
        if (properties != null) {
            for (String groupKey : properties.keySet()) {
                Map<String, Object> group = (Map<String, Object>) properties.get(groupKey);
                String configFileName = null;
                if (group.get(ToolIntegrationConstants.KEY_PROPERTY_CREATE_CONFIG_FILE) != null
                    && (Boolean) group.get(ToolIntegrationConstants.KEY_PROPERTY_CREATE_CONFIG_FILE)) {
                    configFileName = (String) group.get(ToolIntegrationConstants.KEY_PROPERTY_CONFIG_FILENAME);
                }
                for (String propertyOrConfigfile : group.keySet()) {
                    int i = 0;
                    if (!(group.get(propertyOrConfigfile) instanceof String || group.get(propertyOrConfigfile) instanceof Boolean)) {
                        Map<String, String> property = (Map<String, String>) group.get(propertyOrConfigfile);
                        Map<String, String> config = new HashMap<>();
                        config.put(ConfigurationDefinitionConstants.KEY_CONFIGURATION_KEY,
                            property.get(ToolIntegrationConstants.KEY_PROPERTY_KEY));
                        config.put(ComponentConstants.KEY_DEFAULT_VALUE, property.get(ToolIntegrationConstants.KEY_PROPERTY_DEFAULT_VALUE));
                        configuration.add(config);
                        Map<String, String> configMetadata = new HashMap<>();
                        configMetadata.put(ConfigurationDefinitionConstants.KEY_METADATA_GUI_NAME,
                            property.get(ToolIntegrationConstants.KEY_PROPERTY_DISPLAYNAME));
                        if (configFileName != null) {
                            configMetadata.put(ToolIntegrationConstants.KEY_PROPERTY_CONFIG_FILENAME, configFileName);
                        }
                        configMetadata.put(ConfigurationDefinitionConstants.KEY_METADATA_GUI_GROUP_NAME, groupKey);
                        configMetadata.put(ConfigurationDefinitionConstants.KEY_METADATA_GUI_POSITION, "" + i++);
                        configMetadata.put(ConfigurationDefinitionConstants.KEY_METADATA_CONFIG_KEY,
                            property.get(ToolIntegrationConstants.KEY_PROPERTY_KEY));
                        configurationMetadata.add(configMetadata);
                    }
                }
            }
        }
        Map<String, String> historyConfig = new HashMap<>();
        historyConfig.put(ConfigurationDefinitionConstants.KEY_CONFIGURATION_KEY, ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM);
        historyConfig.put(ComponentConstants.KEY_DEFAULT_VALUE, "" + false);
        configuration.add(historyConfig);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> createReadOnlyConfiguration(Map<String, Object> configurationMap) {
        Map<String, String> configuration = new HashMap<>();
        for (String key : configurationMap.keySet()) {
            if (configurationMap.get(key) instanceof String) {
                configuration.put(key, (String) configurationMap.get(key));
            }
            if (configurationMap.get(key) instanceof Boolean) {
                configuration.put(key, ((Boolean) configurationMap.get(key)).toString());
            }
        }
        configuration.put(ToolIntegrationConstants.KEY_ROOT_WORKING_DIRECTORY,
            ((List<Map<String, String>>) configurationMap.get(ToolIntegrationConstants.KEY_LAUNCH_SETTINGS)).get(0).get(
                ToolIntegrationConstants.KEY_ROOT_WORKING_DIRECTORY));

        configuration.put(ToolIntegrationConstants.KEY_TOOL_DIRECTORY,
            ((List<Map<String, String>>) configurationMap.get(ToolIntegrationConstants.KEY_LAUNCH_SETTINGS)).get(0).get(
                ToolIntegrationConstants.KEY_TOOL_DIRECTORY));
        return configuration;
    }

    @SuppressWarnings("unchecked")
    private Set<EndpointDefinition> createOutputs(Map<String, Object> configurationMap) {
        List<Map<String, String>> definedOutputs =
            (List<Map<String, String>>) configurationMap.get(ToolIntegrationConstants.KEY_ENDPOINT_OUTPUTS);
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

        List<Map<String, Object>> dynamicOutputs =
            (List<Map<String, Object>>) configurationMap.get(ToolIntegrationConstants.KEY_ENDPOINT_DYNAMIC_OUTPUTS);

        if (dynamicOutputs != null) {
            for (Map<String, Object> output : dynamicOutputs) {
                Map<String, Object> description = new HashMap<>();
                description.put(IDENTIFIER, output.get(ToolIntegrationConstants.KEY_ENDPOINT_IDENTIFIER));
                description.put(DEFAULT_DATA_TYPE, output.get(ToolIntegrationConstants.KEY_ENDPOINT_DEFAULT_TYPE));
                List<String> dataTypes = new LinkedList<>();
                dataTypes.addAll((List<String>) output.get(ToolIntegrationConstants.KEY_ENDPOINT_DATA_TYPES));
                description.put(DATA_TYPES, dataTypes);

                Map<String, Object> metadata = (Map<String, Object>) output.get(ToolIntegrationConstants.KEY_ENDPOINT_METADATA);
                description.put(META_DATA, metadata);
                outputs.add(ComponentEndpointModelFactory.createEndpointDefinition(description, EndpointType.OUTPUT));
            }
        }
        return outputs;
    }

    @SuppressWarnings("unchecked")
    private Set<EndpointDefinition> createInputs(Map<String, Object> configurationMap) {
        List<Map<String, String>> definedInputs =
            (List<Map<String, String>>) configurationMap.get(ToolIntegrationConstants.KEY_ENDPOINT_INPUTS);
        Set<EndpointDefinition> inputs = new HashSet<>();
        if (definedInputs != null) {
            for (Map<String, String> input : definedInputs) {
                Map<String, Object> description = new HashMap<>();
                description.put(EndpointDefinitionConstants.KEY_NAME, input.get(ToolIntegrationConstants.KEY_ENDPOINT_NAME));
                description.put(DEFAULT_DATA_TYPE, input.get(ToolIntegrationConstants.KEY_ENDPOINT_DATA_TYPE));
                List<String> dataTypes = new LinkedList<>();
                dataTypes.add(input.get(ToolIntegrationConstants.KEY_ENDPOINT_DATA_TYPE));
                description.put(DATA_TYPES, dataTypes);
                description.put(DEFAULT_DATA_TYPE, input.get(ToolIntegrationConstants.KEY_ENDPOINT_DATA_TYPE));

                // migration code: usage (required, initial, optional) -> constant, single
                String[] inputHandlings;
                if (input.containsKey(ToolIntegrationConstants.KEY_INPUT_HANDLING)) {
                    inputHandlings = StringUtils.splitAndUnescape(input.get(ToolIntegrationConstants.KEY_INPUT_HANDLING));
                    if (input.containsKey(ToolIntegrationConstants.KEY_DEFAULT_INPUT_HANDLING)) {
                        description.put(DEFAULT_INPUT_HANDLING, input.get(ToolIntegrationConstants.KEY_DEFAULT_INPUT_HANDLING));
                    } else {
                        description.put(DEFAULT_INPUT_HANDLING, inputHandlings[0]);
                    }
                } else {
                    inputHandlings = new String[] { EndpointDefinition.InputDatumHandling.Single.name() };
                    if (input.get(ToolIntegrationConstants.KEY_ENDPOINT_USAGE).equals("initial")) {
                        inputHandlings = new String[] { EndpointDefinition.InputDatumHandling.Constant.name() };
                    }
                    description.put(DEFAULT_INPUT_HANDLING, inputHandlings[0]);
                }
                description.put(INPUT_HANDLINGS, Arrays.asList(inputHandlings));

                // migration code: usage (required, initial, optional) -> required, not required
                String[] inputExecutionConstraints;
                if (input.containsKey(ToolIntegrationConstants.KEY_INPUT_EXECUTION_CONSTRAINT)) {
                    inputExecutionConstraints = StringUtils.splitAndUnescape(input
                        .get(ToolIntegrationConstants.KEY_INPUT_EXECUTION_CONSTRAINT));
                    if (input.containsKey(ToolIntegrationConstants.KEY_DEFAULT_INPUT_EXECUTION_CONSTRAINT)) {
                        description.put(DEFAULT_INPUT_EXECUTION_CONSTRAINT,
                            input.get(ToolIntegrationConstants.KEY_DEFAULT_INPUT_EXECUTION_CONSTRAINT));
                    } else {
                        description.put(DEFAULT_INPUT_EXECUTION_CONSTRAINT, inputExecutionConstraints[0]);
                    }
                } else {
                    inputExecutionConstraints = new String[] { EndpointDefinition.InputExecutionContraint.Required.name() };
                    if (input.get(ToolIntegrationConstants.KEY_ENDPOINT_USAGE).equals("optional")) {
                        inputExecutionConstraints = new String[] { EndpointDefinition.InputExecutionContraint.NotRequired.name() };
                    }
                    description.put(DEFAULT_INPUT_EXECUTION_CONSTRAINT, inputExecutionConstraints[0]);
                }
                description.put(INPUT_EXECUTION_CONSTRAINTS, Arrays.asList(inputExecutionConstraints));

                Map<String, Map<String, Object>> metadata = new HashMap<>();
                if ((input.get(ToolIntegrationConstants.KEY_ENDPOINT_DATA_TYPE).equals(DataType.FileReference.name())
                    || input.get(ToolIntegrationConstants.KEY_ENDPOINT_DATA_TYPE).equals(DataType.DirectoryReference.name()))
                    && input.get(ToolIntegrationConstants.KEY_ENDPOINT_FILENAME) != null) {
                    Map<String, Object> metadataFilename = new HashMap<>();
                    metadataFilename.put(EndpointDefinitionConstants.KEY_GUI_NAME, "Filename");
                    metadataFilename.put(EndpointDefinitionConstants.KEY_GUI_POSITION, STRING_0);
                    metadataFilename.put(EndpointDefinitionConstants.KEY_GUIGROUP, "File");
                    List<String> possibleValuesListFilename = new LinkedList<>();
                    possibleValuesListFilename.add(input.get(ToolIntegrationConstants.KEY_ENDPOINT_FILENAME));
                    metadataFilename.put(POSSIBLE_VALUES, possibleValuesListFilename);
                    metadataFilename.put(DEFAULT_VALUE, input.get(ToolIntegrationConstants.KEY_ENDPOINT_FILENAME));
                    metadataFilename.put(EndpointDefinitionConstants.KEY_VISIBILITY, Visibility.developerConfigurable.toString());
                    metadata.put(ToolIntegrationConstants.KEY_ENDPOINT_FILENAME, metadataFilename);
                }

                description.put(META_DATA, metadata);
                inputs.add(ComponentEndpointModelFactory.createEndpointDefinition(description, EndpointType.INPUT));
            }
        }
        if (configurationMap.containsKey(ToolIntegrationConstants.KEY_ENDPOINT_DYNAMIC_INPUTS)) {
            List<Map<String, Object>> dynamicInputs =
                (List<Map<String, Object>>) configurationMap.get(ToolIntegrationConstants.KEY_ENDPOINT_DYNAMIC_INPUTS);
            for (Map<String, Object> input : dynamicInputs) {
                Map<String, Object> description = new HashMap<>();
                description.put(IDENTIFIER, input.get(ToolIntegrationConstants.KEY_ENDPOINT_IDENTIFIER));
                description.put(DEFAULT_DATA_TYPE, input.get(ToolIntegrationConstants.KEY_ENDPOINT_DEFAULT_TYPE));
                List<String> dataTypes = new LinkedList<>();
                dataTypes.addAll((List<String>) input.get(ToolIntegrationConstants.KEY_ENDPOINT_DATA_TYPES));
                description.put(DATA_TYPES, dataTypes);

                Map<String, Object> metadata = (Map<String, Object>) input.get(ToolIntegrationConstants.KEY_ENDPOINT_METADATA);
                description.put(META_DATA, metadata);

                // migration code: usage (required, initial, optional) -> consuming vs. immutable
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
                    inputinputExecutionConstraintOptions.add(EndpointDefinition.InputExecutionContraint.Required.name());
                    inputinputExecutionConstraintOptions.add(EndpointDefinition.InputExecutionContraint.RequiredIfConnected.name());
                    description.put(INPUT_EXECUTION_CONSTRAINTS, inputinputExecutionConstraintOptions);
                    metadata.remove("usage");
                } else {
                    if (input.containsKey(ToolIntegrationConstants.KEY_INPUT_HANDLING_OPTIONS)) {
                        description.put(DEFAULT_INPUT_HANDLING, input.get(ToolIntegrationConstants.KEY_DEFAULT_INPUT_HANDLING));
                        List<String> inputHandlingOptions = new LinkedList<>();
                        inputHandlingOptions.addAll((List<String>) input.get(ToolIntegrationConstants.KEY_INPUT_HANDLING_OPTIONS));
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
        if (toolIntegrationFile.exists() && toolIntegrationFile.isDirectory() && toolIntegrationFile.listFiles().length > 0) {
            log.debug("Initializing tool integration root directory " + toolIntegrationFile.getAbsolutePath());
            for (File toolFolder : toolIntegrationFile.listFiles()) {
                if (toolFolder.isDirectory() && !toolFolder.getName().equals("null")) { // to review: why is this "null" check needed?
                    log.debug("Initializing tool directory " + toolFolder.getAbsolutePath());
                    readToolDirectory(toolFolder, context);
                }
            }
        }
    }

    /**
     * Reads the given configuration file and integrated it as a tool.
     * 
     * @param toolFolder the configuration folder
     * @param context used for integration
     */
    @SuppressWarnings("unchecked")
    private void readToolDirectory(File toolFolder, ToolIntegrationContext context) {
        File configFile = new File(toolFolder, context.getConfigurationFilename());
        if (configFile.exists() && configFile.isFile()) {
            try {
                Map<String, Object> configurationMap =
                    mapper.readValue(configFile,
                        new HashMap<String, Object>().getClass());
                if (!integratedConfiguration.containsKey(context.getPrefixForComponentId()
                    + configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME))) {
                    toolNameToPath.put((String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME), toolFolder.getAbsolutePath());

                    checkIcon(toolFolder, configurationMap);

                    if (!isToolIntegrated(configurationMap, context)) {
                        integrateTool(configurationMap, context);
                    }

                } else {
                    log.warn("Tool with foldername already exists:  " + toolFolder.getName());
                }
            } catch (JsonParseException e) {
                log.error(COULD_NOT_READ_TOOL_CONFIGURATION, e);
            } catch (JsonMappingException e) {
                log.error(COULD_NOT_READ_TOOL_CONFIGURATION, e);
            } catch (IOException e) {
                log.error(COULD_NOT_READ_TOOL_CONFIGURATION, e);
            }
        }
    }

    private void checkIcon(File toolFolder, Map<String, Object> configurationMap) {
        if (configurationMap.get(ToolIntegrationConstants.KEY_TOOL_ICON_PATH) != null) {
            File icon = new File((String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_ICON_PATH));
            if (!icon.isAbsolute()) {
                icon = new File(toolFolder, icon.getName());

            }
        }
    }

    @Override
    public void writeToolIntegrationFileToSpecifiedFolder(String folder, Map<String, Object> configurationMap,
        ToolIntegrationContext information) throws IOException {
        if (!configurationMap.containsKey(ToolIntegrationConstants.KEY_TOOL_INTEGRATION_VERSION)) {
            configurationMap.put(ToolIntegrationConstants.KEY_TOOL_INTEGRATION_VERSION,
                ToolIntegrationConstants.CURRENT_TOOLINTEGRATION_VERSION);
        }
        // TODO : Code for removing deprecated key; should be removed in the future 8/3/16 zur_sa
        @SuppressWarnings("unchecked") Map<String, String> launchSettings =
            ((List<Map<String, String>>) configurationMap.get(ToolIntegrationConstants.KEY_LAUNCH_SETTINGS)).get(0);
        String value = launchSettings.remove(ToolIntegrationConstants.KEY_LIMIT_INSTANCES_OLD);
        if (!launchSettings.containsKey(ToolIntegrationConstants.KEY_LIMIT_INSTANCES) && value != null) {
            launchSettings.put(ToolIntegrationConstants.KEY_LIMIT_INSTANCES, value);
        }

        File toolConfigFile =
            new File(folder, information.getNameOfToolIntegrationDirectory() + File.separator
                + information.getToolDirectoryPrefix() + configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME));
        toolConfigFile.mkdirs();
        handleToolIcon(configurationMap, toolConfigFile);
        handleDoc(configurationMap, toolConfigFile);
        // deprecated, remove when done; see Mantis #16044
        // configurationMap.remove(ToolIntegrationConstants.TEMP_KEY_PUBLISH_COMPONENT);
        Map<String, Object> sortedMap = new TreeMap<>();
        sortedMap.putAll(configurationMap);
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(toolConfigFile, information.getConfigurationFilename()),
            sortedMap);
        toolNameToPath.put((String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME),
            toolConfigFile.getAbsolutePath());
    }

    private void handleDoc(Map<String, Object> configurationMap, File toolConfigFile) {
        if ((String) configurationMap.get(ToolIntegrationConstants.KEY_DOC_FILE_PATH) != null
            && !((String) configurationMap.get(ToolIntegrationConstants.KEY_DOC_FILE_PATH)).isEmpty()) {
            File docfile = new File((String) configurationMap.get(ToolIntegrationConstants.KEY_DOC_FILE_PATH));
            File docDir = new File(toolConfigFile, ToolIntegrationConstants.DOCS_DIR_NAME);

            if (docfile.isAbsolute() && docfile.exists() && docfile.isFile()) {
                if (docDir.exists() && docDir.listFiles().length > 0) {
                    for (File f : docDir.listFiles()) {
                        try {
                            FileUtils.forceDelete(f);
                        } catch (IOException e) {
                            log.error("Could not delete old documentation file: " + f.getAbsolutePath() + ": " + e.getMessage());
                        }
                    }
                }
                File destination = new File(docDir, docfile.getName());
                if (!destination.getAbsolutePath().equals(docfile.getAbsolutePath())) {
                    try {
                        FileUtils.copyFile(docfile, destination);
                        configurationMap.put(ToolIntegrationConstants.KEY_DOC_FILE_PATH, docfile.getName());
                    } catch (IOException e) {
                        log.error("Could not copy documentation to tool directory: ", e);
                    }
                }
            }
        }
    }

    private void handleToolIcon(Map<String, Object> configurationMap, File toolConfigFile) {
        if ((String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_ICON_PATH) != null
            && !((String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_ICON_PATH)).isEmpty()) {
            File icon = new File((String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_ICON_PATH));
            if (configurationMap.get(ToolIntegrationConstants.KEY_UPLOAD_ICON) != null
                && (Boolean) configurationMap.get(ToolIntegrationConstants.KEY_UPLOAD_ICON)) {
                if (icon.exists() && icon.isFile() && icon.isAbsolute()) {
                    File destination = new File(toolConfigFile, icon.getName());
                    if (!destination.getAbsolutePath().equals(icon.getAbsolutePath())) {
                        try {
                            FileUtils.copyFile(icon, destination);
                            configurationMap.remove(ToolIntegrationConstants.KEY_UPLOAD_ICON);
                            configurationMap.put(ToolIntegrationConstants.KEY_TOOL_ICON_PATH, icon.getName());
                        } catch (IOException e) {
                            log.warn("Could not copy icon to tool directory: ", e);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void writeToolIntegrationFile(Map<String, Object> configurationMap, ToolIntegrationContext information) throws IOException {
        String configFolder = information.getRootPathToToolIntegrationDirectory();
        writeToolIntegrationFileToSpecifiedFolder(configFolder, configurationMap, information);
    }

    private void readPublishedComponents(ToolIntegrationContext context) {
        File toolsfolder = new File(context.getRootPathToToolIntegrationDirectory(), context.getNameOfToolIntegrationDirectory());
        if (toolsfolder.exists()) {
            try {
                File publishedComponentsFile = new File(toolsfolder, ToolIntegrationConstants.PUBLISHED_COMPONENTS_FILENAME);
                // deprecated mechanism - the file is only checked to log backwards compatibility warnings; see Mantis #16044
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
    protected void activate() {
        watchManager = fileWatcherManagerBuilder.build(this);

        asyncTaskService.execute(new Runnable() {

            @Override
            @TaskDescription("Initialize all provided ToolIntegrationContexts")
            public void run() {
                ToolIntegrationContext context;
                while ((context = toolIntegrationContextRegistry.fetchNextUninitializedToolIntegrationContext()) != null) {
                    if (!CommandLineArguments.isDoNotStartComponentsRequested()) {
                        log.debug("Registering " + ToolIntegrationContext.class.getSimpleName() + " " + context.getContextId());
                        integrateToolIntegrationContext(context);
                    }
                }
                log.debug("Received termination signal from " + ToolIntegrationContext.class.getSimpleName() + " queue");
                localComponentRegistry.reportToolIntegrationRegistrationComplete();
            }
        });

    }

    /**
     * Deactivate the service (e.g. unregister watcher).
     */
    @Deactivate
    protected void deactivateIntegrationService() {
        watchManager.shutdown();
    }

    @Reference
    protected void bindAuthorizationService(AuthorizationService newInstance) {
        this.authorizationService = newInstance;
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
        // note: this code is still left in place to prevent side effects, but no publication settings are loaded anymore;
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

        final File sourceDirectory = new File(toolNameToPath.get(name),
            ToolIntegrationConstants.DOCS_DIR_NAME);

        final byte[] resultData =
            FileCompressionService.compressDirectoryToByteArray(sourceDirectory, FileCompressionFormat.ZIP, false);

        if (resultData == null) {
            log.error("Was not able to create an archive for documentation due to a compression issue.");
            return null;
        }

        return resultData;
    }

    private ToolIntegrationContext getContextForIdentifier(String identifier) {
        ToolIntegrationContext result = null;
        ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        for (ToolIntegrationContext context : serviceRegistryAccess.getService(ToolIntegrationContextRegistry.class)
            .getAllIntegrationContexts()) {
            if (identifier.startsWith(context.getPrefixForComponentId())) {
                result = context;
            }
        }
        return result;
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

}
