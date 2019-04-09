/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.sshremoteaccess.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
import de.rcenvironment.core.communication.sshconnection.SshConnectionService;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.management.api.LocalComponentRegistrationService;
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
import de.rcenvironment.core.component.sshremoteaccess.SshRemoteAccessClientService;
import de.rcenvironment.core.component.sshremoteaccess.SshRemoteAccessConstants;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.ServiceUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.ssh.jsch.JschFileTransfer;
import de.rcenvironment.core.utils.ssh.jsch.executor.JSchRCECommandLineExecutor;

/**
 * Default implementation of {@link SshRemoteAccessClientService}.
 *
 * @author Brigitte Boden
 * @author Robert Mischke
 */
@Component(immediate = true)
public class SshRemoteAccessClientServiceImpl implements SshRemoteAccessClientService {

    private static final String SLASH = "/";

    private static final int SIZE_32 = 32;

    private static final int SIZE_16 = 16;

    private static final Log LOG = LogFactory.getLog(SshRemoteAccessClientService.class);

    private static final int UPDATE_TOOLS_INTERVAL_SECS = 10;

    private static LocalComponentRegistrationService registry;

    private PlatformService platformService;

    private SshConnectionService sshService;

    private final Map<String, Map<String, ComponentInstallation>> registeredComponentsPerConnection;

    private final Map<String, Map<String, String>> registeredComponentHashesPerConnection;

    private ScheduledFuture<?> taskFuture;

    private ObjectMapper mapper = new ObjectMapper();

    private Map<String, LogicalNodeId> logicalNodeMap;

    private AuthorizationService authorizationService;

    private TempFileService tempFileService;

    public SshRemoteAccessClientServiceImpl() {
        registeredComponentsPerConnection = Collections.synchronizedMap(new HashMap<String, Map<String, ComponentInstallation>>());
        registeredComponentHashesPerConnection = Collections.synchronizedMap(new HashMap<String, Map<String, String>>());
        logicalNodeMap = new HashMap<>();
    }

    @Override
    public void updateSshRemoteAccessComponents(String connectionId) {

        Map<String, ComponentInstallation> registeredComponents = registeredComponentsPerConnection.get(connectionId);
        if (registeredComponents == null) {
            registeredComponents = Collections.synchronizedMap(new HashMap<String, ComponentInstallation>());
            registeredComponentsPerConnection.put(connectionId, registeredComponents);
        }

        Map<String, String> registeredComponentHashes = registeredComponentHashesPerConnection.get(connectionId);
        if (registeredComponentHashes == null) {
            registeredComponentHashes = Collections.synchronizedMap(new HashMap<String, String>());
            registeredComponentHashesPerConnection.put(connectionId, registeredComponentHashes);
        }

        Session session;
        session = sshService.getAvtiveSshSession(connectionId);

        if (session != null) {
            JSchRCECommandLineExecutor executor = new JSchRCECommandLineExecutor(session);

            List<String> componentIdsReceived = new ArrayList<>();

            getAndRegisterRemoteTools(connectionId, registeredComponents, registeredComponentHashes, executor, componentIdsReceived);

            getAndRegisterRemoteWorkflows(connectionId, registeredComponents, registeredComponentHashes, executor, componentIdsReceived);

            synchronized (registeredComponents) {
                // Check if there are "old" components from this connection that are not available any more.
                for (Iterator<String> it = registeredComponents.keySet().iterator(); it.hasNext();) {
                    String regCompName = it.next();
                    if (!componentIdsReceived.contains(regCompName)) {
                        removeToolAccessComponent(regCompName, connectionId);
                        it.remove();
                    }
                }
            }

        }
    }

    private void getAndRegisterRemoteWorkflows(String connectionId, Map<String, ComponentInstallation> registeredComponents,
        Map<String, String> registeredComponentHashes, JSchRCECommandLineExecutor executor, List<String> componentIdsReceived) {
        // Get remote workflows
        String command = StringUtils.format("ra list-wfs");
        try {
            executor.start(command);
            try (InputStream stdoutStream = executor.getStdout(); InputStream stderrStream = executor.getStderr();) {

                LineIterator it = IOUtils.lineIterator(stdoutStream, (String) null);

                parseOutputStreamForWorkflowList(connectionId, registeredComponents, registeredComponentHashes, componentIdsReceived, it);
                executor.waitForTermination();
            }
        } catch (IOException | InterruptedException | NumberFormatException e1) {
            LOG.error("Executing SSH command (ra list-wfs) failed", e1);
        }
    }

    private void parseOutputStreamForWorkflowList(String connectionId, Map<String, ComponentInstallation> registeredComponents,
        Map<String, String> registeredComponentHashes, List<String> componentIdsReceived, LineIterator it) {
        Integer numberOfWorkflows = null;
        Integer tokensPerWorkflow = null;
        if (it.hasNext()) {
            String line = it.nextLine();
            if (line.equals("")) {
                LOG.error("Could not load the list of available workflows from the remote instance. Reason: " + it.nextLine());
            } else {
                numberOfWorkflows = Integer.parseInt(line);
            }
        }
        if (it.hasNext()) {
            tokensPerWorkflow = Integer.parseInt(it.nextLine());
        }

        if (numberOfWorkflows != null && tokensPerWorkflow != null) {
            if (tokensPerWorkflow != 8) {
                LOG.error("Unkown format of workflow descriptions");
            } else {
                for (int i = 0; i < numberOfWorkflows; i++) {
                    parseSingleWorkflowDescription(connectionId, registeredComponents, registeredComponentHashes, componentIdsReceived, it);
                }
            }
        }
    }

    private void parseSingleWorkflowDescription(String connectionId, Map<String, ComponentInstallation> registeredComponents,
        Map<String, String> registeredComponentHashes, List<String> componentIdsReceived, LineIterator it) {
        String wfName = it.nextLine();
        String wfVersion = it.nextLine();
        String groupName = it.nextLine();
        // Not used yet
        String hostId = it.nextLine();
        String hostName = it.nextLine();
        String inputDefinitions = it.nextLine();
        String outputDefinitions = it.nextLine();
        String hash = it.nextLine();
        String componentId = wfName + "_wf";
        String componentAndHostId = createUniqueToolAndHostId(componentId, hostId, connectionId);
        if (!registeredComponents.containsKey(componentAndHostId)) {
            LOG.info(StringUtils.format("Detected new remote workflow %s (version %s) on host %s.", wfName,
                wfVersion, hostName));
            registerToolAccessComponent(new RemoteAccessComponentDescription(componentId, wfName, wfVersion,
                hostName, hostId, connectionId, true, inputDefinitions,
                outputDefinitions, groupName));
            registeredComponentHashes.put(componentAndHostId, hash);
        } else {
            // If the workflow interface has changed (determine by comparing hash values), replace the old
            // installation by the new one.
            if (!registeredComponentHashes.get(componentAndHostId).equals(hash)) {
                removeToolAccessComponent(componentAndHostId, connectionId);
                registeredComponentHashes.remove(componentAndHostId);
                registerToolAccessComponent(new RemoteAccessComponentDescription(componentId, wfName, wfVersion,
                    hostName, hostId, connectionId, true, inputDefinitions,
                    outputDefinitions, groupName));
                registeredComponentHashes.put(componentAndHostId, hash);
                LOG.info(StringUtils.format("SSH tool %s changed on host %s.", wfName, hostName));
            }
        }
        componentIdsReceived.add(componentAndHostId);
    }

    private void getAndRegisterRemoteTools(String connectionId, Map<String, ComponentInstallation> registeredComponents,
        Map<String, String> registeredComponentHashes, JSchRCECommandLineExecutor executor, List<String> componentIdsReceived) {
        // Get remote tools
        String command = StringUtils.format("ra list-tools");
        String toolDescriptionsString = "";
        try {
            executor.start(command);
            try (InputStream stdoutStream = executor.getStdout(); InputStream stderrStream = executor.getStderr();) {

                executor.waitForTermination();
                toolDescriptionsString = IOUtils.toString(stdoutStream);
            }
        } catch (IOException | InterruptedException e1) {
            LOG.error("Executing SSH command (ra list-tools) failed", e1);
        }

        if (toolDescriptionsString.contains("Command ra list-tools not executed.")) {
            LOG.error("Could not load the list of available tools from the remote instance. Reason: " + toolDescriptionsString);
        } else {
            parseOutputStringForComponentsList(toolDescriptionsString, connectionId, registeredComponents, registeredComponentHashes,
                componentIdsReceived);
        }
    }

    private void parseOutputStringForComponentsList(String toolDescriptionsString, String connectionId,
        Map<String, ComponentInstallation> registeredComponents,
        Map<String, String> registeredComponentHashes, List<String> componentIdsReceived) {
        // Parse output string and register/unregister components
        final CSVFormat csvFormat = CSVFormat.newFormat(' ').withQuote('"').withQuoteMode(QuoteMode.ALL);
        CSVParser parser = null;
        try (Reader toolDescriptionReader = new StringReader(toolDescriptionsString);) {
            parser = csvFormat.parse(toolDescriptionReader);
            for (CSVRecord record : parser.getRecords()) {
                parseSingleComponentDescription(connectionId, registeredComponents, registeredComponentHashes,
                    componentIdsReceived, record);
            }
        } catch (IOException e) {
            LOG.error("Could not parse tool descriptions" + e.toString());
        }
    }

    private void parseSingleComponentDescription(String connectionId, Map<String, ComponentInstallation> registeredComponents,
        Map<String, String> registeredComponentHashes, List<String> componentIdsReceived, CSVRecord record) {
        String toolName;
        String toolVersion;
        String hostName;
        String hostId;
        String inputDefinitions;
        String outputDefinitions;
        String group;
        String hash;
        toolName = record.get(0);
        toolVersion = record.get(1);
        hostId = record.get(2);
        hostName = record.get(3);
        inputDefinitions = record.get(4);
        outputDefinitions = record.get(5);
        group = record.get(6);
        hash = record.get(7);
        // TODO Review, for now just use the toolName as ID
        String toolId = toolName;
        // Id containing tool id and host id; used as unique key for hashmap because the same tool can be available on different
        // remote nodes
        String toolAndHostId = createUniqueToolAndHostId(toolId, hostId, connectionId);
        // If this component was not registered before, register it now.
        if (!registeredComponents.containsKey(toolAndHostId)) {
            LOG.info(StringUtils.format("Detected new SSH tool %s (version %s) on host %s.", toolName, toolVersion,
                hostName));
            registerToolAccessComponent(new RemoteAccessComponentDescription(toolId, toolName, toolVersion,
                hostName,
                hostId, connectionId, false, inputDefinitions, outputDefinitions, group));
            registeredComponentHashes.put(toolAndHostId, hash);
        } else {
            // If this is a new version of a component (determine by comparing hash values), replace the old installation by
            // the
            // new one.
            if (!registeredComponentHashes.get(toolAndHostId).equals(hash)) {
                removeToolAccessComponent(toolAndHostId, connectionId);
                registeredComponentHashes.remove(toolAndHostId);
                registerToolAccessComponent(new RemoteAccessComponentDescription(toolId, toolName, toolVersion,
                    hostName, hostId, connectionId, false, inputDefinitions,
                    outputDefinitions, group));
                registeredComponentHashes.put(toolAndHostId, hash);
                LOG.info(StringUtils.format("SSH tool %s changed to version %s on host %s.", toolName, toolVersion,
                    hostName));
            }
        }
        componentIdsReceived.add(toolAndHostId);
    }

    private String createUniqueToolAndHostId(String toolId, String hostId, String connectionId) {
        return toolId + SLASH + connectionId + SLASH + hostId;
    }

    protected void registerToolAccessComponent(RemoteAccessComponentDescription component) {
        EndpointDefinitionsProvider inputProvider;
        EndpointDefinitionsProvider outputProvider;
        ConfigurationDefinition configuration;

        Set<EndpointDefinition> inputs = parseEndpoints(component.getInputDefinitions(), EndpointType.INPUT);
        inputProvider = ComponentEndpointModelFactory.createEndpointDefinitionsProvider(inputs);

        Set<EndpointDefinition> outputs = parseEndpoints(component.getOutputDefinitions(), EndpointType.OUTPUT);
        outputProvider = ComponentEndpointModelFactory.createEndpointDefinitionsProvider(outputs);

        configuration =
            generateConfiguration(component.getToolName(), component.getToolVersion(), component.getHostName(), component.getHostId(),
                component.getConnectionId(), component.isWorkflow());
        ComponentInterface componentInterface;
        if (component.isWorkflow()) {
            componentInterface =
                new ComponentInterfaceBuilder()
                    .setIdentifier(SshRemoteAccessConstants.COMPONENT_ID + "." + component.getComponentId())
                    // Add "WORKFLOW" to display name
                    .setDisplayName(
                        StringUtils.format("%s (%s) [workflow on %s]", component.getToolName(), component.getToolVersion(),
                            component.getHostName()))
                    .setIcon16(readDefaultToolIcon(SIZE_16))
                    .setIcon32(readDefaultToolIcon(SIZE_32))
                    .setGroupName(component.getGroup())
                    .setVersion(component.getToolVersion())
                    .setInputDefinitionsProvider(inputProvider).setOutputDefinitionsProvider(outputProvider)
                    .setConfigurationDefinition(configuration)
                    .setConfigurationExtensionDefinitions(new HashSet<ConfigurationExtensionDefinition>())
                    .setColor(ComponentConstants.COMPONENT_COLOR_STANDARD)
                    .setShape(ComponentConstants.COMPONENT_SHAPE_STANDARD)
                    .setSize(ComponentConstants.COMPONENT_SIZE_STANDARD)
                    .build();
        } else {
            componentInterface =
                new ComponentInterfaceBuilder()
                    .setIdentifier(SshRemoteAccessConstants.COMPONENT_ID + "." + component.getComponentId())
                    .setDisplayName(StringUtils.format("%s [SSH forwarded]", component.getToolName()))
                    .setIcon16(readDefaultToolIcon(SIZE_16))
                    .setIcon32(readDefaultToolIcon(SIZE_32))
                    .setGroupName(component.getGroup())
                    .setVersion(component.getToolVersion())
                    .setInputDefinitionsProvider(inputProvider).setOutputDefinitionsProvider(outputProvider)
                    .setConfigurationDefinition(configuration)
                    .setConfigurationExtensionDefinitions(new HashSet<ConfigurationExtensionDefinition>())
                    .setColor(ComponentConstants.COMPONENT_COLOR_STANDARD)
                    .setShape(ComponentConstants.COMPONENT_SHAPE_STANDARD)
                    .setSize(ComponentConstants.COMPONENT_SIZE_STANDARD)
                    .build();
        }

        ComponentInstallation ci =
            new ComponentInstallationBuilder()
                .setComponentRevision(
                    new ComponentRevisionBuilder()
                        .setComponentInterface(componentInterface)
                        .setClassName("de.rcenvironment.core.component.sshremoteaccess.SshRemoteAccessClientComponent").build())
                .setNodeId(getLocalLogicalNodeIdForRemoteNode(component.getHostId(), component.getConnectionId()))
                .setInstallationId(
                    createUniqueToolAndHostId(componentInterface.getIdentifierAndVersion(), component.getHostId(),
                        component.getConnectionId()))
                .build();

        registry.registerOrUpdateLocalComponentInstallation(ci);
        registeredComponentsPerConnection.get(component.getConnectionId()).put(
            createUniqueToolAndHostId(component.getComponentId(), component.getHostId(), component.getConnectionId()), ci);
    }

    private LogicalNodeId getLocalLogicalNodeIdForRemoteNode(String remoteNodeId) {
        if (logicalNodeMap.containsKey(remoteNodeId)) {
            return logicalNodeMap.get(remoteNodeId);
        }

        try {
            LogicalNodeId remoteId = NodeIdentifierUtils.parseLogicalNodeIdString(remoteNodeId);
            String recPart = remoteId.getInstanceNodeIdString();
            // If there is no local node yet representing the given remote node, create a new one
            LogicalNodeId logicalNode = platformService.createRecognizableLocalLogicalNodeId(recPart);
            logicalNodeMap.put(remoteNodeId, logicalNode);
            return logicalNode;
        } catch (IdentifierException e) {
            LOG.error("Failed to retrieve logical node id for remote node: " + e.toString());
            return platformService.getLocalDefaultLogicalNodeId();
        }

    }

    private LogicalNodeId getLocalLogicalNodeIdForRemoteNode(String remoteNodeId, String connectionId) {
        String id = remoteNodeId + SLASH + connectionId;
        if (logicalNodeMap.containsKey(id)) {
            return logicalNodeMap.get(id);
        }
        // If there is no local node yet representing the given remote node, create a new one
        // TODO Rework when we have a concept for display names
        LogicalNodeId logicalNode = platformService.createTransientLocalLogicalNodeId();
        logicalNodeMap.put(id, logicalNode);
        return logicalNode;
    }

    protected void removeToolAccessComponent(String toolAndHostId, String connectionId) {
        ComponentInstallation ci = registeredComponentsPerConnection.get(connectionId).get(toolAndHostId);
        if (ci != null) {
            registry.unregisterLocalComponentInstallation(ci.getInstallationId());
        }
    }

    private ConfigurationDefinition generateConfiguration(String toolName, String toolVersion, String hostName, String hostId,
        String connectionId, boolean isWorkflow) {
        List<Object> configuration = new LinkedList<>();
        Map<String, String> readOnlyConfiguration = new HashMap<>();
        readOnlyConfiguration.put(SshRemoteAccessConstants.KEY_TOOL_NAME, toolName);
        readOnlyConfiguration.put(SshRemoteAccessConstants.KEY_TOOL_VERSION, toolVersion);
        readOnlyConfiguration.put(SshRemoteAccessConstants.KEY_CONNECTION, connectionId);
        readOnlyConfiguration.put(SshRemoteAccessConstants.KEY_HOST_ID, hostId);
        readOnlyConfiguration.put(SshRemoteAccessConstants.KEY_HOST_NAME, hostName);
        readOnlyConfiguration.put(SshRemoteAccessConstants.KEY_IS_WORKFLOW, Boolean.toString(isWorkflow));
        return ComponentConfigurationModelFactory.createConfigurationDefinition(configuration, new LinkedList<Object>(),
            new LinkedList<Object>(), readOnlyConfiguration);
    }

    @SuppressWarnings("unchecked")
    private Set<EndpointDefinition> parseEndpoints(String endpointDescriptions, EndpointType type) {

        Set<EndpointDefinition> endpoints = new HashSet<>();
        try {
            Set<Map<String, Object>> rawEndpointData =
                mapper.readValue(endpointDescriptions, HashSet.class);
            for (Map<String, Object> rawEndpoint : rawEndpointData) {
                EndpointDefinition endpoint = ComponentEndpointModelFactory.createEndpointDefinition(rawEndpoint, type);
                endpoints.add(endpoint);
            }

        } catch (IOException e) {
            LOG.error(e.getMessage());
        }

        return endpoints;
    }

    @Reference(unbind = "unbindComponentRegistry")
    protected void bindComponentRegistry(LocalComponentRegistrationService newRegistry) {
        registry = newRegistry;
    }

    protected void unbindComponentRegistry(LocalComponentRegistrationService oldRegistry) {
        registry = ServiceUtils.createFailingServiceProxy(LocalComponentRegistrationService.class);
    }

    @Reference(unbind = "unbindPlatformService")
    protected void bindPlatformService(PlatformService newService) {
        platformService = newService;
    }

    protected void unbindPlatformService(PlatformService service) {
        // TODO (p3) check: is unbinding this non-static field actually necessary?
        platformService = ServiceUtils.createFailingServiceProxy(PlatformService.class);
    }

    @Reference
    protected void bindSSHConnectionService(SshConnectionService newService) {
        sshService = newService;
    }

    // For unit tests
    public SshConnectionService getSshService() {
        return sshService;
    }

    @Override
    public void updateSshRemoteAccessComponents() {
        Collection<String> activeConnectionIds = sshService.getAllActiveSshConnectionSetupIds();
        // for each connection, update the accessible tools
        for (String id : activeConnectionIds) {
            updateSshRemoteAccessComponents(id);
        }
        synchronized (registeredComponentsPerConnection) {
            // Check for connections that are stored, but not active anymore.
            Set<String> outdatedConnectionIds = new HashSet<>(registeredComponentsPerConnection.keySet());
            outdatedConnectionIds.removeAll(activeConnectionIds);
            // Remove all tools from outdated connections.
            for (String id : outdatedConnectionIds) {
                Map<String, ComponentInstallation> registeredComponents = registeredComponentsPerConnection.get(id);
                if (registeredComponents != null) {
                    for (String oldCompName : registeredComponents.keySet()) {
                        removeToolAccessComponent(oldCompName, id);
                    }
                    registeredComponentsPerConnection.remove(id);
                    registeredComponentHashesPerConnection.remove(id);
                }
            }
        }
    }

    /**
     * OSGi-DS life cycle method.
     */
    @Activate
    public void activate() {
        taskFuture = ConcurrencyUtils.getAsyncTaskService()
            .scheduleAtFixedRate("Periodic updating of SSH-accessible remote tools", () -> {
                updateSshRemoteAccessComponents();
            }, TimeUnit.SECONDS.toMillis(UPDATE_TOOLS_INTERVAL_SECS));
        tempFileService = TempFileServiceAccess.getInstance();
    }

    /**
     * OSGi-DS life cycle method.
     */
    @Deactivate
    public void deactivate() {
        if (taskFuture != null) {
            taskFuture.cancel(false);
            taskFuture = null;
        }
    }

    private byte[] readDefaultToolIcon(int iconSize) {
        try (InputStream inputStream = getClass().getResourceAsStream("/icons/tool" + iconSize + ".png")) {
            return IOUtils.toByteArray(inputStream);
        } catch (IOException | NullPointerException e) {
            LOG.error("Failed to read default tool icon: " + e);
            return null;
        }
    }

    @Reference
    protected void bindAuthorizationService(AuthorizationService newInstance) {
        this.authorizationService = newInstance;
    }

    @Override
    public Map<String, String> getListOfToolsWithDocumentation(String toolId) {
        // Maps hashvalue to array [nodeId, connectionId]
        Map<String, String> nodesWithDocumentation = new HashMap<String, String>();
        List<String> relevantConnectionIds = new ArrayList<String>();

        toolId = toolId.replace("de.rcenvironment.remoteaccess.", "");
        String[] splitToolId = toolId.split(SLASH);
        String toolName = splitToolId[0];
        String toolNameAndVersion = splitToolId[0] + SLASH + splitToolId[1];

        // Find all connectionIds for which the given tool is registered
        for (String connectionId : registeredComponentsPerConnection.keySet()) {
            for (String registeredToolId : registeredComponentsPerConnection.get(connectionId).keySet()) {
                if (registeredToolId.startsWith(toolName)) {
                    relevantConnectionIds.add(connectionId);
                }
            }
        }

        // Retrieve documentation list from relevant connections
        for (String connectionId : relevantConnectionIds) {
            Session session;
            session = sshService.getAvtiveSshSession(connectionId);

            if (session != null) {
                JSchRCECommandLineExecutor executor = new JSchRCECommandLineExecutor(session);

                String command = StringUtils.format("ra get-doc-list " + toolNameAndVersion);
                try {
                    executor.start(command);
                    try (InputStream stdoutStream = executor.getStdout(); InputStream stderrStream = executor.getStderr();) {

                        LineIterator it = IOUtils.lineIterator(stdoutStream, (String) null);

                        Integer numberOfNodeIds = null;
                        if (it.hasNext()) {
                            String line = it.nextLine();
                            if (line.equals("")) {
                                LOG.error(
                                    "Could not load the list of documentations from the remote instance. Reason: " + it.nextLine());
                            } else {
                                numberOfNodeIds = Integer.parseInt(line);
                            }
                        }

                        if (numberOfNodeIds != null) {
                            for (int i = 0; i < numberOfNodeIds; i++) {
                                String hashValue = it.nextLine();
                                String nodeId = it.nextLine();
                                LogicalNodeId logicalNode = getLocalLogicalNodeIdForRemoteNode(nodeId, connectionId);
                                nodesWithDocumentation.put(hashValue, logicalNode.getLogicalNodeIdString());
                            }
                        }
                        executor.waitForTermination();
                    }
                } catch (IOException | InterruptedException | NumberFormatException e1) {
                    LOG.error("Executing SSH command (ra get-doc-list) failed", e1);
                }
            }
        }

        return nodesWithDocumentation;
    }

    @Override
    public File downloadToolDocumentation(String toolId, String logicalNodeId, String hashValue) {

        toolId = toolId.replace("de.rcenvironment.remoteaccess.", "");
        String[] splitToolId = toolId.split(SLASH);
        String toolNameAndVersion = splitToolId[0] + SLASH + splitToolId[1];
        
        String sessionId = null;
        String nodeId = null;

        //Get connection ID and remote node id from logical node ID
        for (Entry<String, LogicalNodeId> entry : logicalNodeMap.entrySet()) {
            if (entry.getValue().getLogicalNodeIdString().equals(logicalNodeId)) {
                String[] splitId = entry.getKey().split(SLASH);
                if (splitId.length != 2) {
                    return null;
                }
                nodeId = splitId[0];
                sessionId = splitId[1];
            }
        }
        
        File docFile = null;
        Session session;
        session = sshService.getAvtiveSshSession(sessionId);
        // Initialize scp context
        JSchRCECommandLineExecutor rceExecutor = new JSchRCECommandLineExecutor(session);
        String currentSessionToken = initializeRemoteExecutionContext(rceExecutor);
        if (currentSessionToken == null) {
            return null;
        }

        // Run the remote command
        try {
            // Target directory for the documentation
            File downloadDir = tempFileService.createManagedTempDir();

            String command = StringUtils.format("ra get-tool-doc %s %s %s %s", toolNameAndVersion, nodeId, hashValue, currentSessionToken);
            rceExecutor.start(command);
            try (InputStream stdoutStream = rceExecutor.getStdout(); InputStream stderrStream = rceExecutor.getStderr();) {

                rceExecutor.waitForTermination();
            }

            // Download the documentation
            JschFileTransfer.downloadDirectory(session, StringUtils.format("/ra/%s/output", currentSessionToken),
                downloadDir);

            docFile = new File(downloadDir, "output");
            if (!docFile.exists()) {
                LOG.error("Downloading documentation command failed");
            }
        } catch (IOException | InterruptedException | JSchException e1) {
            LOG.error("Downloading documentation command failed", e1);
        }

        return docFile;
    }

    private String initializeRemoteExecutionContext(JSchRCECommandLineExecutor rceExecutor) {
        String currentSessionToken = null;
        try {
            rceExecutor.start("ra init --compact");
            try (InputStream stdoutStream = rceExecutor.getStdout(); InputStream stderrStream = rceExecutor.getStderr();) {
                rceExecutor.waitForTermination();
                currentSessionToken = IOUtils.toString(stdoutStream).trim();

                if (currentSessionToken.contains("Command ra init --compact not executed.")) {
                    LOG.error("Could not initiate remote tool or workflow execution. Reason: " + currentSessionToken);
                }

                LOG.info("Received session token " + currentSessionToken);

                // Currently, nothing is written to stderr by the server side. Just in case, log error messages here.
                String errStream = IOUtils.toString(stderrStream);
                if (!errStream.isEmpty()) {
                    LOG.error(errStream);
                }
            }
        } catch (IOException | InterruptedException e1) {
            LOG.error("Executing SSH command failed", e1);
        }
        return currentSessionToken;
    }

}
