/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.uplinktoolaccess.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;
import de.rcenvironment.core.authorization.api.AuthorizationAccessGroupKeyData;
import de.rcenvironment.core.authorization.api.AuthorizationPermissionSet;
import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.authorization.cryptography.api.SymmetricKey;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
import de.rcenvironment.core.communication.uplink.client.session.api.ClientSideUplinkSession;
import de.rcenvironment.core.communication.uplink.client.session.api.DestinationIdUtils;
import de.rcenvironment.core.communication.uplink.client.session.api.SshUplinkConnectionListenerAdapter;
import de.rcenvironment.core.communication.uplink.client.session.api.SshUplinkConnectionService;
import de.rcenvironment.core.communication.uplink.client.session.api.SshUplinkConnectionSetup;
import de.rcenvironment.core.communication.uplink.client.session.api.ToolDescriptor;
import de.rcenvironment.core.communication.uplink.client.session.api.ToolDescriptorListUpdate;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.api.UserComponentIdMappingService;
import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.management.utils.JsonDataEncryptionUtils;
import de.rcenvironment.core.component.management.utils.JsonDataWithOptionalEncryption;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinitionConstants;
import de.rcenvironment.core.component.spi.DistributedComponentKnowledgeListener;
import de.rcenvironment.core.component.uplinktoolaccess.UplinkToolAnnouncementService;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryPublisherAccess;

/**
 * Implementation of {@link UplinkToolAnnouncementService}.
 *
 * @author Brigitte Boden
 */
@Component(immediate = true)
public class UplinkToolAnnouncementServiceImpl implements UplinkToolAnnouncementService {

    // Prefix for authorization groups that will be published over uplink connections
    private static final String AUTHORIZATION_GROUP_PREFIX = "external_";

    private static final String KEY_META_DATA = "metaData";

    private static final String KEY_DEFAULT_DATA_TYPE = "defaultDataType";

    private static final String KEY_DATA_TYPES = "dataTypes";

    private static final String KEY_INPUT_HANDLING_OPTIONS = "inputHandlingOptions";

    private static final String KEY_DEFAULT_INPUT_HANDLING = "defaultInputHandling";

    private static final String KEY_EXECUTION_CONSTRAINT_OPTIONS = "inputExecutionConstraintOptions";

    private static final String KEY_DEFAULT_EXECUTION_CONSTRAINT = "defaultInputExecutionConstraint";

    private static final String KEY_DEFAULT_VALUE = "defaultValue";

    private static final String KEY_POSSIBLE_VALUES = "possibleValues";

    private ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();

    private final Log log = LogFactory.getLog(getClass());

    private ServiceRegistryPublisherAccess serviceRegistryAccess;

    @Reference
    private SshUplinkConnectionService uplinkConnectionService;

    @Reference
    private DistributedComponentKnowledgeService componentKnowledgeService;

    @Reference
    private UserComponentIdMappingService userComponentIdMappingService;

    @Reference
    private PlatformService platformService;

    @Reference
    private AuthorizationService authorizationService;

    private Set<String> publishedDestinationIds;

    /**
     * OSGi life-cycle method.
     */
    public void activate() {
        serviceRegistryAccess = ServiceRegistry.createPublisherAccessFor(this);
        publishedDestinationIds = new HashSet<String>();
        registerChangeListener();
    }

    private void registerChangeListener() {
        serviceRegistryAccess.registerService(DistributedComponentKnowledgeListener.class,
            new DistributedComponentKnowledgeListener() {

                @Override
                public void onDistributedComponentKnowledgeChanged(
                    final DistributedComponentKnowledge newKnowledge) {
                    updateToolAnnouncements();
                }
            });
        uplinkConnectionService.addListener(
            new SshUplinkConnectionListenerAdapter() {

                @Override
                public void onConnected(SshUplinkConnectionSetup setup) {
                    updateToolAnnouncements();
                }
            });
    }

    private synchronized void updateToolAnnouncements() {

        Map<String, List<ToolDescriptor>> toolsToAnnounceByDestinationId = new HashMap<String, List<ToolDescriptor>>();

        // Add all former destination ids so that an empty tool list will be sent if the destination id does not have tools any more
        for (String destinationId : publishedDestinationIds) {
            toolsToAnnounceByDestinationId.put(destinationId, new ArrayList<ToolDescriptor>());
        }

        // Empty set of former destinations
        publishedDestinationIds = new HashSet<String>();

        Map<String, String> destinationIdToDisplayName = new HashMap<>();

        for (Map.Entry<DistributedComponentEntry, AuthorizationPermissionSet> entry : getMatchingPublishedToolsWithAuthGroups()
            .entrySet()) {
            DistributedComponentEntry componentEntry = entry.getKey();

            ComponentInterface compInterface = componentEntry.getComponentInterface();

            String destinationId = DestinationIdUtils.getInternalDestinationIdForLogicalNodeId(componentEntry.getNodeId());
            publishedDestinationIds.add(destinationId);
            String displayName;
            displayName = componentEntry.getComponentInstallation().getNodeIdObject().getAssociatedDisplayName();
            destinationIdToDisplayName.put(destinationId, displayName);

            String userComponentId;
            try {
                userComponentId = userComponentIdMappingService.fromInternalToExternalId(compInterface.getIdentifier());
            } catch (OperationFailureException e1) {
                // Invalid component Id, should not happen
                // Log error and leave this tool out
                log.error("Component Id " + compInterface.getIdentifier() + " could not be converted. Cause: " + e1.getMessage());
                continue;
            }
            try {
                Set<Map<String, Object>> nodeInputSet = generateNodeInputSet(componentEntry);

                Set<Map<String, Object>> nodeOutputSet = generateNodeOutputSet(componentEntry);

                List<Object> rawConfigurationValues =
                    componentEntry.getComponentInterface().getConfigurationDefinition().getRawConfigurationDefinition();

                List<Object> rawConfigurationMetadata =
                    componentEntry.getComponentInterface().getConfigurationDefinition().getRawConfigurationMetaDataDefinition();

                Map<String, String> readOnlyconfig =
                    componentEntry.getComponentInterface().getConfigurationDefinition().getReadOnlyConfiguration().getConfiguration();
                // Not all config values should be sent (scripts etc.)
                Map<String, String> readOnlyConfigToSend = generateReadOnlyConfigToSend(readOnlyconfig);

                UplinkToolAccessComponentDescription toolDescription = new UplinkToolAccessComponentDescription(userComponentId,
                    compInterface.getDisplayName(), compInterface.getVersion(), nodeInputSet, nodeOutputSet, rawConfigurationValues,
                    rawConfigurationMetadata, compInterface.getGroupName(), compInterface.getDocumentationHash(), readOnlyConfigToSend);

                String serializedToolData = mapper.writeValueAsString(toolDescription);

                JsonDataWithOptionalEncryption jsonData;
                Set<String> authGroupIds = new HashSet<String>();

                if (entry.getValue().isPublic()) {
                    jsonData = JsonDataEncryptionUtils.asPublicData(serializedToolData);
                    // Add "public" id
                    authGroupIds.add(entry.getValue().getArbitraryGroup().getFullId());

                } else {
                    // Collect key data for group
                    Map<String, SymmetricKey> keyData = new HashMap<>();

                    for (AuthorizationAccessGroup group : entry.getValue().getAccessGroups()) {
                        final AuthorizationAccessGroupKeyData keyDataForGroup = authorizationService.getKeyDataForGroup(group);
                        if (keyDataForGroup == null) {
                            log.warn("Found no key data for assigned access group " + group.getFullId());
                            continue;
                        }
                        keyData.put(group.getFullId(), keyDataForGroup.getSymmetricKey());
                        authGroupIds.add(group.getFullId());
                    }
                    jsonData = JsonDataEncryptionUtils.encryptForKeys(serializedToolData, keyData,
                        authorizationService.getCryptographyOperationsProvider());
                }

                if (toolsToAnnounceByDestinationId.get(destinationId) == null) {
                    toolsToAnnounceByDestinationId.put(destinationId, new ArrayList<ToolDescriptor>());
                }

                // Create the tool descriptor
                toolsToAnnounceByDestinationId.get(destinationId)
                    .add(new ToolDescriptor(userComponentId, compInterface.getVersion(), authGroupIds,
                        toolDescription.createHashString(), jsonData));

            } catch (IOException e) {
                log.error("An error occured while creating descriptions of the available tools.");
            } catch (OperationFailureException e) {
                log.error("An error occured while encrypting json data.");
            }
        }

        String destinationIdForLocalInstance = DestinationIdUtils
            .getInternalDestinationIdForLogicalNodeId(platformService.getLocalDefaultLogicalNodeId().getLogicalNodeIdString());

        for (SshUplinkConnectionSetup setup : uplinkConnectionService.getAllActiveSshConnectionSetups().values()) {
            ClientSideUplinkSession session = setup.getSession();
            for (String destinationId : toolsToAnnounceByDestinationId.keySet()) {
                // If the gateway option is set, publish all tools, else only publish tools from local instance
                if (setup.isGateway() || destinationId.equals(destinationIdForLocalInstance)) {
                    String qualifiedDestinationId =
                        DestinationIdUtils.getQualifiedDestinationId(setup.getDestinationIdPrefix(), destinationId);
                    try {
                        session.publishToolDescriptorListUpdate(
                            new ToolDescriptorListUpdate(qualifiedDestinationId, destinationIdToDisplayName.get(destinationId),
                                toolsToAnnounceByDestinationId.get(destinationId)));
                    } catch (IOException e) {
                        // TODO sessions need a better log id
                        log.error("Failed to send a tool descriptor update to Uplink session " + qualifiedDestinationId);
                    }
                }
            }
        }
    }

    private Map<String, String> generateReadOnlyConfigToSend(Map<String, String> readOnlyconfig) {
        Map<String, String> readOnlyConfigToSend = new HashMap<String, String>();
        readOnlyConfigToSend.put(ToolIntegrationConstants.KEY_TOOL_NAME,
            readOnlyconfig.get(ToolIntegrationConstants.KEY_TOOL_NAME));
        readOnlyConfigToSend.put(ToolIntegrationConstants.KEY_TOOL_INTEGRATOR_NAME,
            readOnlyconfig.get(ToolIntegrationConstants.KEY_TOOL_INTEGRATOR_NAME));
        readOnlyConfigToSend.put(ToolIntegrationConstants.KEY_TOOL_INTEGRATOR_EMAIL,
            readOnlyconfig.get(ToolIntegrationConstants.KEY_TOOL_INTEGRATOR_EMAIL));
        readOnlyConfigToSend.put(ToolIntegrationConstants.KEY_TOOL_DESCRIPTION,
            readOnlyconfig.get(ToolIntegrationConstants.KEY_TOOL_DESCRIPTION));
        readOnlyConfigToSend.put(ToolIntegrationConstants.KEY_MOCK_MODE_SUPPORTED,
            readOnlyconfig.get(ToolIntegrationConstants.KEY_MOCK_MODE_SUPPORTED));
        readOnlyConfigToSend.put(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_NEVER,
            readOnlyconfig.get(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_NEVER));
        readOnlyConfigToSend.put(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ONCE,
            readOnlyconfig.get(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ONCE));
        readOnlyConfigToSend.put(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ALWAYS,
            readOnlyconfig.get(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ALWAYS));
        readOnlyConfigToSend.put(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_KEEP_ON_ERROR_ITERATION,
            readOnlyconfig.get(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_KEEP_ON_ERROR_ITERATION));
        readOnlyConfigToSend.put(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_KEEP_ON_ERROR_ONCE,
            readOnlyconfig.get(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_KEEP_ON_ERROR_ONCE));
        return readOnlyConfigToSend;
    }

    private Set<Map<String, Object>> generateNodeOutputSet(DistributedComponentEntry componentEntry) {
        Set<Map<String, Object>> nodeOutputSet = new HashSet<>();
        for (EndpointDefinition ed : componentEntry.getComponentInterface().getOutputDefinitionsProvider()
            .getStaticEndpointDefinitions()) {
            Map<String, Object> rawEndpointData = new HashMap<String, Object>();

            rawEndpointData.put(EndpointDefinitionConstants.KEY_NAME, ed.getName());
            rawEndpointData.put(KEY_DATA_TYPES, ed.getPossibleDataTypes());
            rawEndpointData.put(KEY_DEFAULT_DATA_TYPE, ed.getDefaultDataType());
            nodeOutputSet.add(rawEndpointData);
        }
        for (EndpointDefinition ed : componentEntry.getComponentInterface().getOutputDefinitionsProvider()
            .getDynamicEndpointDefinitions()) {
            Map<String, Object> rawEndpointData = new HashMap<String, Object>();

            rawEndpointData.put(EndpointDefinitionConstants.KEY_IDENTIFIER, ed.getIdentifier());
            rawEndpointData.put(KEY_DATA_TYPES, ed.getPossibleDataTypes());
            rawEndpointData.put(KEY_DEFAULT_DATA_TYPE, ed.getDefaultDataType());
            Map<String, Map<String, Object>> rawMetadata = extractRawMetadata(ed);
            rawEndpointData.put(KEY_META_DATA, rawMetadata);
            nodeOutputSet.add(rawEndpointData);
        }
        return nodeOutputSet;
    }

    private Set<Map<String, Object>> generateNodeInputSet(DistributedComponentEntry componentEntry) {
        Set<Map<String, Object>> nodeInputSet = new HashSet<>();
        for (EndpointDefinition ed : componentEntry.getComponentInterface().getInputDefinitionsProvider()
            .getStaticEndpointDefinitions()) {
            Map<String, Object> rawEndpointData = new HashMap<String, Object>();

            rawEndpointData.put(EndpointDefinitionConstants.KEY_NAME, ed.getName());
            rawEndpointData.put(KEY_DATA_TYPES, ed.getPossibleDataTypes());
            rawEndpointData.put(KEY_DEFAULT_DATA_TYPE, ed.getDefaultDataType());
            rawEndpointData.put(KEY_INPUT_HANDLING_OPTIONS, ed.getInputDatumOptions());
            rawEndpointData.put(KEY_DEFAULT_INPUT_HANDLING, ed.getDefaultInputDatumHandling());
            rawEndpointData.put(KEY_EXECUTION_CONSTRAINT_OPTIONS, ed.getInputExecutionConstraintOptions());
            rawEndpointData.put(KEY_DEFAULT_EXECUTION_CONSTRAINT, ed.getDefaultInputExecutionConstraint());
            nodeInputSet.add(rawEndpointData);
        }
        for (EndpointDefinition ed : componentEntry.getComponentInterface().getInputDefinitionsProvider()
            .getDynamicEndpointDefinitions()) {
            Map<String, Object> rawEndpointData = new HashMap<String, Object>();

            rawEndpointData.put(EndpointDefinitionConstants.KEY_IDENTIFIER, ed.getIdentifier());
            rawEndpointData.put(KEY_DATA_TYPES, ed.getPossibleDataTypes());
            rawEndpointData.put(KEY_DEFAULT_DATA_TYPE, ed.getDefaultDataType());
            rawEndpointData.put(KEY_INPUT_HANDLING_OPTIONS, ed.getInputDatumOptions());
            rawEndpointData.put(KEY_DEFAULT_INPUT_HANDLING, ed.getDefaultInputDatumHandling());
            rawEndpointData.put(KEY_EXECUTION_CONSTRAINT_OPTIONS, ed.getInputExecutionConstraintOptions());
            rawEndpointData.put(KEY_DEFAULT_EXECUTION_CONSTRAINT, ed.getDefaultInputExecutionConstraint());
            Map<String, Map<String, Object>> rawMetadata = extractRawMetadata(ed);
            rawEndpointData.put(KEY_META_DATA, rawMetadata);
            nodeInputSet.add(rawEndpointData);
        }
        return nodeInputSet;
    }

    private Map<String, Map<String, Object>> extractRawMetadata(EndpointDefinition ed) {
        Map<String, Map<String, Object>> rawMetadata = new HashMap<String, Map<String, Object>>();
        for (String key : ed.getMetaDataDefinition().getMetaDataKeys()) {
            Map<String, Object> metaDataForKey = new HashMap<String, Object>();
            metaDataForKey.put(EndpointDefinitionConstants.KEY_GUI_NAME, ed.getMetaDataDefinition().getGuiName(key));
            metaDataForKey.put(EndpointDefinitionConstants.KEY_GUI_POSITION,
                Integer.toString(ed.getMetaDataDefinition().getGuiPosition(key)));
            metaDataForKey.put(EndpointDefinitionConstants.KEY_GUIGROUP, ed.getMetaDataDefinition().getGuiGroup(key));
            metaDataForKey.put(KEY_POSSIBLE_VALUES, ed.getMetaDataDefinition().getPossibleValues(key));
            metaDataForKey.put(KEY_DEFAULT_VALUE, ed.getMetaDataDefinition().getDefaultValue(key));
            metaDataForKey.put(EndpointDefinitionConstants.KEY_VISIBILITY,
                ed.getMetaDataDefinition().getVisibility(key));
            rawMetadata.put(key, metaDataForKey);
        }
        return rawMetadata;
    }

    private Map<DistributedComponentEntry, AuthorizationPermissionSet> getMatchingPublishedToolsWithAuthGroups() {
        Map<DistributedComponentEntry, AuthorizationPermissionSet> components =
            new HashMap<>();
        DistributedComponentKnowledge compKnowledge = componentKnowledgeService.getCurrentSnapshot();
        for (DistributedComponentEntry entry : compKnowledge.getKnownSharedInstallations()) {
            ComponentInterface compInterf = entry.getComponentInterface();

            // Only user-integrated tools are published via uplink connections, not RCE standard components
            // Only publish tools on default logical node ids to avoid "echoing" received components back to the relay
            if (compInterf.getIdentifierAndVersion().startsWith("de.rcenvironment.integration") && NodeIdentifierUtils
                .parseArbitraryIdStringToLogicalNodeIdWithExceptionWrapping(entry.getNodeId()).getLogicalNodePart()
                .equals(LogicalNodeId.DEFAULT_LOGICAL_NODE_PART)) {

                if (entry.getDeclaredPermissionSet().isPublic()) {
                    components.put(entry, entry.getDeclaredPermissionSet());
                } else {
                    Set<AuthorizationAccessGroup> sshGroupsForTool = new HashSet<>();
                    // Only tools in the "public" group and groups named "external_[name]" are published to uplink connections
                    for (AuthorizationAccessGroup group : entry.getDeclaredPermissionSet().getAccessGroups()) {
                        if (authorizationService.isPublicAccessGroup(group)
                            || group.getName().toLowerCase().startsWith(AUTHORIZATION_GROUP_PREFIX)) {
                            sshGroupsForTool.add(group);
                        }
                    }
                    if (!sshGroupsForTool.isEmpty()) {
                        components.put(entry, authorizationService.buildPermissionSet(sshGroupsForTool));
                    }
                }
            }
        }
        return components;
    }

}
