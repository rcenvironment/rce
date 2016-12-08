/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.sshremoteaccess.internal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import com.jcraft.jsch.Session;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
import de.rcenvironment.core.communication.sshconnection.SshConnectionService;
import de.rcenvironment.core.component.api.ComponentConstants;
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
import de.rcenvironment.core.component.registration.api.ComponentRegistry;
import de.rcenvironment.core.component.sshremoteaccess.SshRemoteAccessConstants;
import de.rcenvironment.core.component.sshremoteaccess.SshRemoteAccessClientService;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.ServiceUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.ssh.jsch.executor.JSchRCECommandLineExecutor;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Default implementation of {@link SshRemoteAccessClientService}.
 *
 * @author Brigitte Boden
 */
public class SshRemoteAccessClientServiceImpl implements SshRemoteAccessClientService {

    private static final int SIZE_32 = 32;

    private static final int SIZE_16 = 16;

    private static final Log LOG = LogFactory.getLog(SshRemoteAccessClientService.class);

    private static final int UPDATE_TOOLS_INTERVAL_SECS = 10;

    private static ComponentRegistry registry;

    private PlatformService platformService;

    private SshConnectionService sshService;

    private Map<String, Map<String, ComponentInstallation>> registeredComponentsPerConnection;

    private ScheduledFuture<?> taskFuture;

    private volatile boolean started;
    
    private Map<String, LogicalNodeId> logicalNodeMap;

    public SshRemoteAccessClientServiceImpl() {
        registeredComponentsPerConnection = new HashMap<String, Map<String, ComponentInstallation>>();
        logicalNodeMap = new HashMap<String, LogicalNodeId>();
    }

    @Override
    public void updateSshRemoteAccessComponents(String connectionId) {

        Map<String, ComponentInstallation> registeredComponents = registeredComponentsPerConnection.get(connectionId);
        if (registeredComponents == null) {
            registeredComponents = new HashMap<String, ComponentInstallation>();
            registeredComponentsPerConnection.put(connectionId, registeredComponents);
        }

        Session session = null;
        session = sshService.getAvtiveSshSession(connectionId);

        if (session != null) {
            JSchRCECommandLineExecutor executor = new JSchRCECommandLineExecutor(session);

            List<String> componentIdsReceived = new ArrayList<String>();

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

            // Parse output string and register/unregister components
            final CSVFormat csvFormat = CSVFormat.newFormat(' ').withQuote('"').withQuoteMode(QuoteMode.ALL);
            CSVParser parser = null;
            try (final Reader toolDescriptionReader = new StringReader(toolDescriptionsString);) {
                parser = csvFormat.parse(toolDescriptionReader);
                for (CSVRecord record : parser.getRecords()) {
                    String toolName;
                    String toolVersion;
                    String hostName;
                    String hostId;
                    toolName = record.get(0);
                    toolVersion = record.get(1);
                    hostId = record.get(2);
                    hostName = record.get(3);
                    // TODO Review, for now just use the toolName as ID
                    String toolId = toolName;
                    // Id containing tool id and host id; used as unique key for hashmap because the same tool can be available on different
                    // remote nodes
                    String toolAndHostId = createUniqueToolAndHostId(toolId, hostId, connectionId);
                    // If this component was not registered before, register it now.
                    if (!registeredComponents.containsKey(toolAndHostId)) {
                        LOG.info(StringUtils.format("Detected new SSH tool %s (version %s) on host %s.", toolName, toolVersion, hostName));
                        registerToolAccessComponent(toolId, toolName, toolVersion, hostName, hostId, connectionId, false);
                    } else {
                        // If this is a new version of a component, replace the old installation by the new one.
                        if (!registeredComponents.get(toolAndHostId).getComponentRevision().getComponentInterface().getVersion()
                            .equals(toolVersion)) {
                            removeToolAccessComponent(toolAndHostId, connectionId);
                            registerToolAccessComponent(toolId, toolName, toolVersion, hostName, hostId, connectionId, false);
                            LOG.info(StringUtils.format("SSH tool %s changed to version %s on host %s.", toolName, toolVersion,
                                hostName));
                        }
                    }
                    componentIdsReceived.add(toolAndHostId);
                }
            } catch (IOException e) {
                LOG.error("Could not parse tool descriptions" + e.toString());
            }

            // Get remote workflows
            command = StringUtils.format("ra list-wfs");
            try {
                executor.start(command);
                try (InputStream stdoutStream = executor.getStdout(); InputStream stderrStream = executor.getStderr();) {

                    LineIterator it = IOUtils.lineIterator(stdoutStream, (String) null);

                    Integer numberOfWorkflows = null;
                    Integer tokensPerWorkflow = null;
                    if (it.hasNext()) {
                        numberOfWorkflows = Integer.parseInt(it.nextLine());
                    }
                    if (it.hasNext()) {
                        tokensPerWorkflow = Integer.parseInt(it.nextLine());
                    }

                    if (numberOfWorkflows != null && tokensPerWorkflow != null) {
                        if (tokensPerWorkflow != 4) {
                            LOG.error("Unkown format of workflow descriptions");
                        } else {
                            for (int i = 0; i < numberOfWorkflows; i++) {
                                String wfName = it.nextLine();
                                String wfVersion = it.nextLine();
                                // Not used yet
                                String hostId = it.nextLine();
                                String hostName = it.nextLine();
                                String componentId = wfName + "_wf_" + hostId;
                                String toolAndHostId = createUniqueToolAndHostId(componentId, hostId, connectionId);
                                if (!registeredComponents.containsKey(toolAndHostId)) {
                                    LOG.info(StringUtils.format("Detected new remote workflow %s (version %s) on host %s.", wfName,
                                        wfVersion, hostName));
                                    registerToolAccessComponent(componentId, wfName, wfVersion, hostName, hostId, connectionId, true);
                                }
                                componentIdsReceived.add(toolAndHostId);
                            }
                        }
                    }
                    executor.waitForTermination();
                }
            } catch (IOException | InterruptedException e1) {
                LOG.error("Executing SSH command (ra list-wfs) failed", e1);
            }

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
    
    private String createUniqueToolAndHostId(String toolId, String hostId, String connectionId) {
        return toolId + "/" + connectionId + "/" + hostId;
    }

    protected void registerToolAccessComponent(String componentId, String toolName, String toolVersion, String hostName, String hostId,
        String connectionId, boolean isWorkflow) {
        EndpointDefinitionsProvider inputProvider;
        EndpointDefinitionsProvider outputProvider;
        ConfigurationDefinition configuration;

        Set<EndpointDefinition> inputs = createInputs();
        inputProvider = ComponentEndpointModelFactory.createEndpointDefinitionsProvider(inputs);

        Set<EndpointDefinition> outputs = createOutputs();
        outputProvider = ComponentEndpointModelFactory.createEndpointDefinitionsProvider(outputs);

        configuration = generateConfiguration(toolName, toolVersion, hostName, hostId, connectionId, isWorkflow);

        ComponentInterface componentInterface;
        if (isWorkflow) {
            componentInterface = new ComponentInterfaceBuilder()
                .setIdentifier(SshRemoteAccessConstants.COMPONENT_ID + "." + componentId)
                // Add "WORKFLOW" to display name
                .setDisplayName(StringUtils.format("%s (%s) [workflow on %s]", toolName, toolVersion, hostName))
                .setIcon16(readDefaultToolIcon(SIZE_16))
                .setIcon32(readDefaultToolIcon(SIZE_32))
                .setGroupName(SshRemoteAccessConstants.GROUP_NAME_WFS)
                .setVersion(toolVersion)
                .setInputDefinitionsProvider(inputProvider).setOutputDefinitionsProvider(outputProvider)
                .setConfigurationDefinition(configuration)
                .setConfigurationExtensionDefinitions(new HashSet<ConfigurationExtensionDefinition>())
                .setColor(ComponentConstants.COMPONENT_COLOR_STANDARD)
                .setShape(ComponentConstants.COMPONENT_SHAPE_STANDARD)
                .setSize(ComponentConstants.COMPONENT_SIZE_STANDARD)
                .build();
        } else {
            componentInterface = new ComponentInterfaceBuilder()
                .setIdentifier(SshRemoteAccessConstants.COMPONENT_ID + "." + componentId)
                .setDisplayName(StringUtils.format("%s [SSH forwarded]", toolName))
                .setIcon16(readDefaultToolIcon(SIZE_16))
                .setIcon32(readDefaultToolIcon(SIZE_32))
                .setGroupName(SshRemoteAccessConstants.GROUP_NAME_TOOLS)
                .setVersion(toolVersion)
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
                .setNodeId(getLocalLogicalNodeIdForRemoteNode(hostId))
                .setInstallationId(createUniqueToolAndHostId(componentInterface.getIdentifier(), hostId, connectionId))
                .setIsPublished(true)
                .build();
        registry.addComponent(ci);
        registeredComponentsPerConnection.get(connectionId).put(createUniqueToolAndHostId(componentId, hostId, connectionId), ci);
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
            return platformService.getLocalDefaultLogicalNodeId();
        }
        
    }

    protected void removeToolAccessComponent(String toolAndHostId, String connectionId) {
        ComponentInstallation ci = registeredComponentsPerConnection.get(connectionId).get(toolAndHostId);
        if (ci != null) {
            registry.removeComponent(ci.getInstallationId());
        }
    }

    private ConfigurationDefinition generateConfiguration(String toolName, String toolVersion, String hostName, String hostId,
        String connectionId, boolean isWorkflow) {
        List<Object> configuration = new LinkedList<Object>();
        Map<String, String> readOnlyConfiguration = new HashMap<String, String>();
        readOnlyConfiguration.put(SshRemoteAccessConstants.KEY_TOOL_NAME, toolName);
        readOnlyConfiguration.put(SshRemoteAccessConstants.KEY_TOOL_VERSION, toolVersion);
        readOnlyConfiguration.put(SshRemoteAccessConstants.KEY_CONNECTION, connectionId);
        readOnlyConfiguration.put(SshRemoteAccessConstants.KEY_HOST_ID, hostId);
        readOnlyConfiguration.put(SshRemoteAccessConstants.KEY_HOST_NAME, hostName);
        readOnlyConfiguration.put(SshRemoteAccessConstants.KEY_IS_WORKFLOW, Boolean.toString(isWorkflow));
        return ComponentConfigurationModelFactory.createConfigurationDefinition(configuration, new LinkedList<Object>(),
            new LinkedList<Object>(), readOnlyConfiguration);
    }

    private Set<EndpointDefinition> createOutputs() {
        Set<EndpointDefinition> outputs = new HashSet<EndpointDefinition>();
        Map<String, Object> description = new HashMap<String, Object>();
        description.put(SshRemoteAccessConstants.KEY_ENDPOINT_NAME, SshRemoteAccessConstants.OUTPUT_NAME);
        description.put(SshRemoteAccessConstants.KEY_ENDPOINT_DATA_TYPE, DataType.DirectoryReference.name());
        List<String> dataTypes = new LinkedList<String>();
        dataTypes.add(DataType.DirectoryReference.name());
        description.put(SshRemoteAccessConstants.KEY_ENDPOINT_DATA_TYPES, dataTypes);
        outputs.add(ComponentEndpointModelFactory.createEndpointDefinition(description, EndpointType.OUTPUT));

        return outputs;
    }

    private Set<EndpointDefinition> createInputs() {
        Set<EndpointDefinition> inputs = new HashSet<EndpointDefinition>();

        List<String> inputHandlings = new ArrayList<String>();
        inputHandlings.add(EndpointDefinition.InputDatumHandling.Constant.name());
        inputHandlings.add(EndpointDefinition.InputDatumHandling.Single.name());
        inputHandlings.add(EndpointDefinition.InputDatumHandling.Queue.name());

        List<String> inputExecutionConstraints = new ArrayList<String>();
        inputExecutionConstraints.add(EndpointDefinition.InputExecutionContraint.NotRequired.name());
        inputExecutionConstraints.add(EndpointDefinition.InputExecutionContraint.Required.name());
        inputExecutionConstraints.add(EndpointDefinition.InputExecutionContraint.RequiredIfConnected.name());

        // Short Text input
        Map<String, Object> description = new HashMap<String, Object>();
        description.put(SshRemoteAccessConstants.KEY_ENDPOINT_NAME, SshRemoteAccessConstants.INPUT_NAME_SHORT_TEXT);
        description.put(SshRemoteAccessConstants.KEY_ENDPOINT_DATA_TYPE, DataType.ShortText.name());
        List<String> dataTypes = new LinkedList<String>();
        dataTypes.add(DataType.ShortText.name());
        description.put(SshRemoteAccessConstants.KEY_ENDPOINT_DATA_TYPES, dataTypes);
        description.put(SshRemoteAccessConstants.KEY_DEFAULT_INPUT_EXEC_CONSTRAINT,
            EndpointDefinition.InputExecutionContraint.Required.name());
        description.put(SshRemoteAccessConstants.KEY_INPUT_EXEC_CONSTRAINTS, inputExecutionConstraints);
        description.put(SshRemoteAccessConstants.KEY_DEFAULT_INPUT_HANDLING, EndpointDefinition.InputDatumHandling.Queue.name());
        description.put(SshRemoteAccessConstants.KEY_INPUT_HANDLINGS, inputHandlings);
        inputs.add(ComponentEndpointModelFactory.createEndpointDefinition(description, EndpointType.INPUT));

        // Directory input
        Map<String, Object> description2 = new HashMap<String, Object>();
        description2.put(SshRemoteAccessConstants.KEY_ENDPOINT_NAME, SshRemoteAccessConstants.INPUT_NAME_DIRECTORY);
        description2.put(SshRemoteAccessConstants.KEY_ENDPOINT_DATA_TYPE, DataType.DirectoryReference.name());
        List<String> dataTypes2 = new LinkedList<String>();
        dataTypes2.add(DataType.DirectoryReference.name());
        description2.put(SshRemoteAccessConstants.KEY_ENDPOINT_DATA_TYPES, dataTypes2);
        description2.put(SshRemoteAccessConstants.KEY_DEFAULT_INPUT_EXEC_CONSTRAINT,
            EndpointDefinition.InputExecutionContraint.Required.name());
        description2.put(SshRemoteAccessConstants.KEY_INPUT_EXEC_CONSTRAINTS, inputExecutionConstraints);
        description2.put(SshRemoteAccessConstants.KEY_DEFAULT_INPUT_HANDLING, EndpointDefinition.InputDatumHandling.Queue.name());
        description2.put(SshRemoteAccessConstants.KEY_INPUT_HANDLINGS, inputHandlings);

        inputs.add(ComponentEndpointModelFactory.createEndpointDefinition(description2, EndpointType.INPUT));

        return inputs;
    }

    protected void bindComponentRegistry(ComponentRegistry newRegistry) {
        registry = newRegistry;
    }

    protected void unbindComponentRegistry(ComponentRegistry newRegistry) {
        registry = ServiceUtils.createFailingServiceProxy(ComponentRegistry.class);
    }

    protected void bindPlatformService(PlatformService newService) {
        platformService = newService;
    }

    protected void unbindPlatformService(PlatformService newService) {
        platformService = ServiceUtils.createFailingServiceProxy(PlatformService.class);
    }

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
        // Check for connections that are stored, but not active anymore.
        Set<String> outdatedConnectionIds = new HashSet<String>(registeredComponentsPerConnection.keySet());
        outdatedConnectionIds.removeAll(activeConnectionIds);
        // Remove all tools from outdated connections.
        for (String id : outdatedConnectionIds) {
            Map<String, ComponentInstallation> registeredComponents = registeredComponentsPerConnection.get(id);
            if (registeredComponents != null) {
                for (String oldCompName : registeredComponents.keySet()) {
                    removeToolAccessComponent(oldCompName, id);
                }
                registeredComponentsPerConnection.remove(id);
            }
        }
    }

    /**
     * Task for updating remote access components.
     *
     * @author Brigitte Boden
     */
    public class UpdateSSHToolsTask implements Runnable {

        @Override
        @TaskDescription("Periodic updating of SSH-accessible remote tools")
        public void run() {
            updateSshRemoteAccessComponents();
        }
    }

    /**
     * OSGi-DS life cycle method.
     */
    public void activate() {
        taskFuture = ConcurrencyUtils.getAsyncTaskService()
            .scheduleAtFixedRate(new UpdateSSHToolsTask(), TimeUnit.SECONDS.toMillis(UPDATE_TOOLS_INTERVAL_SECS));
        started = true;
    }

    /**
     * OSGi-DS life cycle method.
     */
    public void deactivate() {
        started = false;
        if (taskFuture != null) {
            taskFuture.cancel(false);
            taskFuture = null;
        }
    }

    private byte[] readDefaultToolIcon(int iconSize) {
        try (InputStream inputStream = getClass().getResourceAsStream("/icons/tool" + iconSize + ".png")) {
            return IOUtils.toByteArray(inputStream);
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException | NullPointerException e) {
            return null;
        }
    }

}
