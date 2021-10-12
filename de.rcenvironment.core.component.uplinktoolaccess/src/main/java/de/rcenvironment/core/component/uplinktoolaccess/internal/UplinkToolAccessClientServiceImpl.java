/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.uplinktoolaccess.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;
import de.rcenvironment.core.authorization.api.AuthorizationAccessGroupListener;
import de.rcenvironment.core.authorization.api.AuthorizationPermissionSet;
import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.uplink.client.session.api.ClientSideUplinkSession;
import de.rcenvironment.core.communication.uplink.client.session.api.SshUplinkConnectionListener;
import de.rcenvironment.core.communication.uplink.client.session.api.SshUplinkConnectionListenerAdapter;
import de.rcenvironment.core.communication.uplink.client.session.api.SshUplinkConnectionService;
import de.rcenvironment.core.communication.uplink.client.session.api.SshUplinkConnectionSetup;
import de.rcenvironment.core.communication.uplink.client.session.api.ToolDescriptor;
import de.rcenvironment.core.communication.uplink.client.session.api.ToolDescriptorListUpdate;
import de.rcenvironment.core.communication.uplink.client.session.api.UplinkLogicalNodeMappingService;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.UserComponentIdMappingService;
import de.rcenvironment.core.component.integration.documentation.ToolDocumentationProvider;
import de.rcenvironment.core.component.integration.documentation.ToolIntegrationDocumentationService;
import de.rcenvironment.core.component.management.api.LocalComponentRegistrationService;
import de.rcenvironment.core.component.management.utils.JsonDataEncryptionUtils;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInstallationBuilder;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.api.ComponentInterfaceBuilder;
import de.rcenvironment.core.component.model.api.ComponentRevisionBuilder;
import de.rcenvironment.core.component.model.configuration.api.ComponentConfigurationModelFactory;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDefinition;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationExtensionDefinition;
import de.rcenvironment.core.component.model.endpoint.api.ComponentEndpointModelFactory;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinitionsProvider;
import de.rcenvironment.core.component.uplinktoolaccess.UplinkToolAccessClientService;
import de.rcenvironment.core.component.uplinktoolaccess.UplinkToolAccessConstants;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.ServiceUtils;
import de.rcenvironment.core.utils.common.SizeValidatedDataSource;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;

/**
 * Default implementation of {@link UplinkToolAccessClientService}.
 *
 * @author Brigitte Boden
 * @author Robert Mischke (minor changes)
 */
@Component(immediate = true)
public class UplinkToolAccessClientServiceImpl implements UplinkToolAccessClientService {

    private static final String SLASH = "/";

    private static final int SIZE_32 = 32;

    private static final int SIZE_16 = 16;

    private static final Log LOG = LogFactory.getLog(UplinkToolAccessClientServiceImpl.class);

    private static LocalComponentRegistrationService registry;

    @Reference
    private UserComponentIdMappingService userComponentIdMappingService;

    @Reference
    private SshUplinkConnectionService sshUplinkService;

    @Reference
    private ToolIntegrationDocumentationService toolDocService;

    private final Map<String, Map<String, ComponentInstallation>> registeredComponentsPerDestinationId;

    private final Map<String, Map<String, String>> registeredComponentHashesPerDestinationId;

    private ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();

    private final Log log = LogFactory.getLog(getClass());

    // Stores destination ids per connection and the last received publication entries for each destination id
    private final Map<String, Map<String, ToolDescriptorListUpdate>> destinationIdsAndPublicationEntriesPerConnection;

    @Reference
    private AuthorizationService authorizationService;

    @Reference
    private UplinkLogicalNodeMappingService logicalNodeMappingService;

    public UplinkToolAccessClientServiceImpl() {
        registeredComponentsPerDestinationId = Collections.synchronizedMap(new HashMap<String, Map<String, ComponentInstallation>>());
        registeredComponentHashesPerDestinationId = Collections.synchronizedMap(new HashMap<String, Map<String, String>>());
        destinationIdsAndPublicationEntriesPerConnection = new HashMap<>();
    }

    @Reference(unbind = "unbindComponentRegistry")
    protected void bindComponentRegistry(LocalComponentRegistrationService newRegistry) {
        registry = newRegistry;
    }

    protected void unbindComponentRegistry(LocalComponentRegistrationService oldRegistry) {
        registry = ServiceUtils.createFailingServiceProxy(LocalComponentRegistrationService.class);
    }

    /**
     * OSGi-DS life cycle method.
     */
    @Activate
    public void activate() {
        registerListener();
    }

    private void registerListener() {
        authorizationService.addAuthorizationAccessGroupListener(
            new AuthorizationAccessGroupListener() {

                @Override
                public void onAvailableAuthorizationAccessGroupsChanged(List<AuthorizationAccessGroup> accessGroups) {
                    // Parse all cached publication entries again
                    for (Map.Entry<String, Map<String, ToolDescriptorListUpdate>> entry : destinationIdsAndPublicationEntriesPerConnection
                        .entrySet()) {
                        String connectionId = entry.getKey();
                        for (ToolDescriptorListUpdate publicationEntries : entry.getValue().values()) {
                            updateAndRegisterRemoteTools(publicationEntries, connectionId);
                        }
                    }
                }
            });

        SshUplinkConnectionListener listener = new SshUplinkConnectionListenerAdapter() {

            @Override
            public void onPublicationEntriesChanged(ToolDescriptorListUpdate publicationEntries, String connectionId) {
                updateAndRegisterRemoteTools(publicationEntries, connectionId);
            }

            @Override
            public void onConnectionClosed(SshUplinkConnectionSetup setup, boolean willAutoRetry) {
                removeAllToolsForConnection(setup.getId());
                destinationIdsAndPublicationEntriesPerConnection.remove(setup.getId());
            }

        };
        sshUplinkService.addListener(listener);
    }

    private void updateAndRegisterRemoteTools(ToolDescriptorListUpdate publicationEntries, String connectionId) {

        Map<String, ComponentInstallation> registeredComponents =
            registeredComponentsPerDestinationId.get(publicationEntries.getDestinationId());
        if (registeredComponents == null) {
            registeredComponents = Collections.synchronizedMap(new HashMap<String, ComponentInstallation>());
            registeredComponentsPerDestinationId.put(publicationEntries.getDestinationId(), registeredComponents);
        }

        Map<String, String> registeredComponentHashes =
            registeredComponentHashesPerDestinationId.get(publicationEntries.getDestinationId());
        if (registeredComponentHashes == null) {
            registeredComponentHashes = Collections.synchronizedMap(new HashMap<String, String>());
            registeredComponentHashesPerDestinationId.put(publicationEntries.getDestinationId(), registeredComponentHashes);
        }

        if (destinationIdsAndPublicationEntriesPerConnection.get(connectionId) == null) {
            destinationIdsAndPublicationEntriesPerConnection.put(connectionId, new HashMap<String, ToolDescriptorListUpdate>());
        }
        destinationIdsAndPublicationEntriesPerConnection.get(connectionId).put(publicationEntries.getDestinationId(), publicationEntries);

        List<String> componentIdsReceivedAndRegistered = new ArrayList<>();

        // Id containing tool id and host id; used as unique key for hashmap because the same tool can be available on different
        // remote nodes
        LogicalNodeId logicalNodeId =
            logicalNodeMappingService.getLocalLogicalNodeIdForDestinationIdAndUpdateName(publicationEntries.getDestinationId(),
                publicationEntries.getDisplayName());
        if (logicalNodeId == null) {
            // If no logical node for this combination exists yet, create one and register a tool documentation provider for the new
            // node.
            logicalNodeId = logicalNodeMappingService.createOrGetLocalLogicalNodeIdForDestinationId(publicationEntries.getDestinationId(),
                publicationEntries.getDisplayName());
            registerToolDocumentationProvider(logicalNodeId);
        }

        for (ToolDescriptor toolDesc : publicationEntries.getToolDescriptors()) {
            readAndRegisterSingleTool(connectionId, publicationEntries.getDestinationId(), registeredComponents, registeredComponentHashes,
                componentIdsReceivedAndRegistered, toolDesc, logicalNodeId);
        }

        synchronized (registeredComponents) {
            // Check if there are "old" components from this connection that are not available any more.
            for (Iterator<String> it = registeredComponents.keySet().iterator(); it.hasNext();) {
                String regCompName = it.next();
                if (!componentIdsReceivedAndRegistered.contains(regCompName)) {
                    removeToolAccessComponent(regCompName, publicationEntries.getDestinationId());
                    it.remove();
                    registeredComponentHashes.remove(regCompName);
                }
            }
        }

    }

    private void registerToolDocumentationProvider(LogicalNodeId logicalNodeId) {
        toolDocService.registerToolDocumentationProvider(new ToolDocumentationProvider() {

            @Override
            public byte[] provideToolDocumentation(String identifier, String nodeId, String hashValue) throws IOException {
                final Optional<SizeValidatedDataSource> result = downloadToolDocumentation(identifier, nodeId, hashValue);
                if (!result.isPresent()) {
                    // TODO maintaining the existing behavior for now; should return an Optional as well instead
                    return null;
                }
                SizeValidatedDataSource dataSource = result.get();
                // TODO rework to forward the stream instead of a byte array
                final long size = dataSource.getSize();
                // FIXME only a stopgap check; this max array size is far too large
                if (size > Integer.MAX_VALUE) {
                    throw new IllegalArgumentException();
                }
                byte[] buffer = new byte[(int) size];
                IOUtils.readFully(dataSource.getStream(), buffer);
                if (!dataSource.receivedCompletely()) {
                    throw new IOException("Received incomplete download for documentation id " + identifier);
                }
                return buffer;
            }
        }, logicalNodeId.getLogicalNodeIdString());
    }

    private void readAndRegisterSingleTool(String connectionId, String destinationId,
        Map<String, ComponentInstallation> registeredComponents, Map<String, String> registeredComponentHashes,
        List<String> componentIdsReceivedAndRegistered, ToolDescriptor toolDesc, LogicalNodeId logicalNodeId) {

        try {

            AuthorizationPermissionSet permissionSet = null;

            if (toolDesc.getAuthGroupIds().size() == 1 && toolDesc.getAuthGroupIds().iterator().next().equals("public")) {
                permissionSet = authorizationService.getDefaultAuthorizationObjects().permissionSetPublicInLocalNetwork();
            } else {
                Set<AuthorizationAccessGroup> declaredAuthGroups = authorizationService.representRemoteGroupIds(toolDesc.getAuthGroupIds());
                Set<AuthorizationAccessGroup> accessibleAuthGroups = authorizationService.intersectWithAccessibleGroups(declaredAuthGroups);
                permissionSet = authorizationService.buildPermissionSet(accessibleAuthGroups);

                if (accessibleAuthGroups.isEmpty()) {
                    // For now, print only debug message as groups are not filtered on relay yet.
                    log.debug("No local authorization group match for '" + toolDesc.getToolId() + "/" + toolDesc.getToolVersion()
                        + "'; tool will not be registered");
                    return;
                }

            }

            String decryptedToolData;
            if (JsonDataEncryptionUtils.isPublic(toolDesc.getSerializedToolData())) {
                decryptedToolData = JsonDataEncryptionUtils.getPublicData(toolDesc.getSerializedToolData());
            } else {
                AuthorizationAccessGroup groupForDecryption = permissionSet.getArbitraryGroup();
                decryptedToolData = JsonDataEncryptionUtils.attemptDecryption(toolDesc.getSerializedToolData(),
                    groupForDecryption.getFullId(), authorizationService.getKeyDataForGroup(groupForDecryption).getSymmetricKey(),
                    authorizationService.getCryptographyOperationsProvider());
            }

            UplinkToolAccessComponentDescription compDesc =
                mapper.readValue(decryptedToolData, UplinkToolAccessComponentDescription.class);

            // Sanity check: Deserialized component description should be consistent with ToolMetaData
            if (!compDesc.getComponentId().equals(toolDesc.getToolId()) || !compDesc.getToolVersion().equals(toolDesc.getToolVersion())) {
                log.warn("Mismatch between tool descriptor and serialized tool data; cannot register tool.");
                return;
            }

            String toolInstallationId =
                createUniqueToolInstallationId(toolDesc.getToolId(), toolDesc.getToolVersion(), logicalNodeId);
            // If this component was not registered before, register it now.
            if (!registeredComponents.containsKey(toolInstallationId)) {
                LOG.debug(
                    StringUtils.format("Detected new Uplink tool %s (version %s) on host %s", toolDesc.getToolId(),
                        toolDesc.getToolVersion(), destinationId));
                registerToolAccessComponent(toolInstallationId, compDesc, connectionId, destinationId, permissionSet,
                    logicalNodeId);
                registeredComponentHashes.put(toolInstallationId, toolDesc.getToolDataHash());
            } else {
                // If this is a new version of a component (determine by comparing hash values), replace the old installation by
                // the new one.
                if (!registeredComponentHashes.get(toolInstallationId).equals(toolDesc.getToolDataHash())) {
                    removeToolAccessComponent(toolInstallationId, destinationId);
                    registeredComponentHashes.remove(toolInstallationId);
                    registerToolAccessComponent(toolInstallationId, compDesc, connectionId, destinationId, permissionSet,
                        logicalNodeId);
                    registeredComponentHashes.put(toolInstallationId, toolDesc.getToolDataHash());
                    LOG.info(StringUtils.format("Remote tool %s changed to version %s on host %s.", toolDesc.getToolId(),
                        toolDesc.getToolVersion(), destinationId));
                } else {
                    // The tools hash values have not changed, check if authorization groups have changed
                    if (!registry
                        .getComponentPermissionSet(
                            registry.getComponentSelector(registeredComponents.get(toolInstallationId)), true)
                        .equals(permissionSet)) {
                        if (!sshUplinkService.getConnectionSetup(connectionId).isGateway()) {
                            /*
                             * Fix for #0017682: Without this check, the remote Uplink tool's permissions -- intersected with the local
                             * authorization group memberships -- were always applied as local tool permissions, effectively re-announcing
                             * the tool in the local network, even if the connection was not configured to act as an Uplink "Gateway". This
                             * only happened if this specific code path was triggered, though: the initial registration methods above
                             * correctly perform the "is Gateway" check internally.
                             * 
                             * The fix is to still maintain the remote authorization for anything above (e.g. registering the tool in the
                             * first place, and storing the authorization for tool execution), but reduce it to local-only permission when
                             * updating the local permissions (the code below).
                             */
                            permissionSet = authorizationService.getDefaultAuthorizationObjects().permissionSetLocalOnly();
                        }
                        registry.setComponentPermissions(
                            registry.getComponentSelector(registeredComponents.get(toolInstallationId)), permissionSet);
                    }
                }
            }
            componentIdsReceivedAndRegistered.add(toolInstallationId);
        } catch (IOException | OperationFailureException e) {
            log.warn("Could not deserialize tool data: " + e.getMessage());
        }

    }

    private String createUniqueToolInstallationId(String toolId, String toolVersion, LogicalNodeId logicalNodeId) {
        return toolId + SLASH + toolVersion + SLASH + logicalNodeId.getLogicalNodePart();
    }

    protected void registerToolAccessComponent(String installationId, UplinkToolAccessComponentDescription component, String connectionId,
        String destinationId, AuthorizationPermissionSet permissionSet, LogicalNodeId nodeId) {
        EndpointDefinitionsProvider inputProvider;
        EndpointDefinitionsProvider outputProvider;
        ConfigurationDefinition configuration;

        Set<EndpointDefinition> inputs = createEndpointDefinitions(component.getInputDefinitions(), EndpointType.INPUT);
        inputProvider = ComponentEndpointModelFactory.createEndpointDefinitionsProvider(inputs);

        Set<EndpointDefinition> outputs = createEndpointDefinitions(component.getOutputDefinitions(), EndpointType.OUTPUT);
        outputProvider = ComponentEndpointModelFactory.createEndpointDefinitionsProvider(outputs);

        configuration =
            generateConfiguration(component, destinationId, connectionId, permissionSet);

        try {
            String internalComponentId = userComponentIdMappingService.fromExternalToInternalId(component.getComponentId());
            ComponentInterface componentInterface;

            componentInterface =
                new ComponentInterfaceBuilder()
                    .setIdentifier(internalComponentId)
                    .setDisplayName(component.getToolName())
                    .setIcon16(readDefaultToolIcon(SIZE_16))
                    .setIcon32(readDefaultToolIcon(SIZE_32))
                    .setGroupName(component.getPaletteGroup())
                    .setVersion(component.getToolVersion())
                    .setDocumentationHash(component.getToolDocumentationHash())
                    .setInputDefinitionsProvider(inputProvider).setOutputDefinitionsProvider(outputProvider)
                    .setConfigurationDefinition(configuration)
                    .setConfigurationExtensionDefinitions(new HashSet<ConfigurationExtensionDefinition>())
                    .setColor(ComponentConstants.COMPONENT_COLOR_STANDARD)
                    .setShape(ComponentConstants.COMPONENT_SHAPE_STANDARD)
                    .setSize(ComponentConstants.COMPONENT_SIZE_STANDARD)
                    .build();

            ComponentInstallation ci =
                new ComponentInstallationBuilder()
                    .setComponentRevision(
                        new ComponentRevisionBuilder()
                            .setComponentInterface(componentInterface)
                            .setClassName("de.rcenvironment.core.component.uplinktoolaccess.UplinkToolAccessClientComponent").build())
                    .setNodeId(nodeId)
                    .setInstallationId(installationId)
                    .setIsMappedCompoent(true)
                    .build();

            registry.registerOrUpdateLocalComponentInstallation(ci);
            if (sshUplinkService.getConnectionSetup(connectionId).isGateway()) {
                registry.setComponentPermissions(registry.getComponentSelector(ci), permissionSet);
            } else {
                registry.setComponentPermissions(registry.getComponentSelector(ci),
                    authorizationService.getDefaultAuthorizationObjects().permissionSetLocalOnly());
            }
            registeredComponentsPerDestinationId.get(destinationId).put(installationId, ci);
        } catch (OperationFailureException e) {
            // Invalid component Id, should not happen
            // Log error and leave this tool out
            log.error("Component Id " + component.getComponentId() + " could not be converted. Cause: " + e.getMessage());
        }

    }

    protected void removeToolAccessComponent(String installationId, String destinationId) {
        ComponentInstallation ci = registeredComponentsPerDestinationId.get(destinationId).get(installationId);
        if (ci != null) {
            registry.setComponentPermissions(registry.getComponentSelector(ci),
                authorizationService.getDefaultAuthorizationObjects().permissionSetLocalOnly());
            registry.unregisterLocalComponentInstallation(ci.getInstallationId());
        }
    }

    // Remove all tools published for this connection, if any exist
    private void removeAllToolsForConnection(String connectionId) {
        if (destinationIdsAndPublicationEntriesPerConnection.containsKey(connectionId)) {
            for (String destinationId : destinationIdsAndPublicationEntriesPerConnection.get(connectionId).keySet()) {
                Map<String, ComponentInstallation> compsForDestination = registeredComponentsPerDestinationId.get(destinationId);
                if (compsForDestination != null) {
                    for (String installationId : compsForDestination.keySet()) {
                        removeToolAccessComponent(installationId, destinationId);
                    }
                }
                registeredComponentsPerDestinationId.remove(destinationId);
                registeredComponentHashesPerDestinationId.remove(destinationId);
            }
        }
    }

    private ConfigurationDefinition generateConfiguration(UplinkToolAccessComponentDescription component, String destinationId,
        String connectionId, AuthorizationPermissionSet permissionSet) {
        Map<String, String> readOnlyConfiguration = component.getReadOnlyConfig();
        readOnlyConfiguration.put(UplinkToolAccessConstants.KEY_TOOL_ID, component.getComponentId());
        readOnlyConfiguration.put(UplinkToolAccessConstants.KEY_TOOL_VERSION, component.getToolVersion());
        readOnlyConfiguration.put(UplinkToolAccessConstants.KEY_CONNECTION, connectionId);
        readOnlyConfiguration.put(UplinkToolAccessConstants.KEY_DESTINATION_ID, destinationId);
        // Like in local RCE network: For now,
        // arbitrarily choose the first group shared by the local instance and the component host; no obvious criterion to choose by
        readOnlyConfiguration.put(UplinkToolAccessConstants.KEY_AUTH_GROUP_ID, permissionSet.getArbitraryGroup().getFullId());
        return ComponentConfigurationModelFactory.createConfigurationDefinition(component.getConfigurationValues(),
            new LinkedList<Object>(), component.getConfigurationMetaData(), readOnlyConfiguration);
    }

    private Set<EndpointDefinition> createEndpointDefinitions(Set<Map<String, Object>> rawEndpointData, EndpointType type) {

        Set<EndpointDefinition> endpoints = new HashSet<>();
        for (Map<String, Object> rawEndpoint : rawEndpointData) {
            EndpointDefinition endpoint = ComponentEndpointModelFactory.createEndpointDefinition(rawEndpoint, type);
            endpoints.add(endpoint);
        }

        return endpoints;
    }

    private byte[] readDefaultToolIcon(int iconSize) {
        try (InputStream inputStream = getClass().getResourceAsStream("/icons/tool" + iconSize + ".png")) {
            return IOUtils.toByteArray(inputStream);
        } catch (IOException | NullPointerException e) {
            LOG.error("Failed to read default tool icon: " + e);
            return null;
        }
    }

    @Override
    public Optional<SizeValidatedDataSource> downloadToolDocumentation(String toolIdAndVersion, String logicalNodeId, String hashValue) {
        // Find matching source id
        String destinationId = logicalNodeMappingService
            .getDestinationIdForLogicalNodeId(logicalNodeId);

        if (destinationId == null) {
            log.warn("No destinationId could be found for logical node id " + logicalNodeId);
        }

        // Find matching connection
        String connectionId = null;

        for (Entry<String, Map<String, ToolDescriptorListUpdate>> entry : destinationIdsAndPublicationEntriesPerConnection.entrySet()) {
            if (entry.getValue().containsKey(destinationId)) {
                connectionId = entry.getKey();
                break;
            }
        }

        String docReferenceId = toolIdAndVersion + SLASH + hashValue;

        // TODO check size
        Optional<ClientSideUplinkSession> optionalUplinkSession = sshUplinkService.getActiveSshUplinkSession(connectionId);
        if (!optionalUplinkSession.isPresent()) {
            log.warn("Tool documentation download requested, but the required Uplink connection is not active anymore");
            return Optional.empty();
        }
        return optionalUplinkSession.get().fetchDocumentationData(destinationId, docReferenceId);
    }

}
