/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.datamanagement.browser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;

import de.rcenvironment.core.authentication.AuthenticationException;
import de.rcenvironment.core.communication.api.SimpleCommunicationService;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.component.datamanagement.api.DefaultComponentHistoryDataItem;
import de.rcenvironment.core.component.datamanagement.history.HistoryMetaDataKeys;
import de.rcenvironment.core.datamanagement.DistributedMetaDataService;
import de.rcenvironment.core.datamanagement.commons.ComponentInstance;
import de.rcenvironment.core.datamanagement.commons.ComponentRun;
import de.rcenvironment.core.datamanagement.commons.EndpointData;
import de.rcenvironment.core.datamanagement.commons.MetaData;
import de.rcenvironment.core.datamanagement.commons.MetaDataKeys;
import de.rcenvironment.core.datamanagement.commons.MetaDataSet;
import de.rcenvironment.core.datamanagement.commons.WorkflowRun;
import de.rcenvironment.core.datamanagement.commons.WorkflowRunDescription;
import de.rcenvironment.core.datamodel.api.FinalWorkflowState;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.gui.datamanagement.browser.spi.CommonHistoryDataItemSubtreeBuilderUtils;
import de.rcenvironment.core.gui.datamanagement.browser.spi.ComponentHistoryDataItemSubtreeBuilder;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DMBrowserNode;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DMBrowserNodeType;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DMBrowserNodeUtils;
import de.rcenvironment.core.utils.common.concurrent.AsyncExceptionListener;
import de.rcenvironment.core.utils.common.concurrent.CallablesGroup;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * @author Jan Flink
 * @author Markus Litz
 * @author Robert Mischke
 * @author Christian Weiss
 */
public class DMContentProvider implements ITreeContentProvider {

    private static final String NODE_TEXT_FORMAT_TITLE_PLUS_STATE = "%s --> %s";

    private static final String NOT_TERMINATED_YET = "not terminated yet";

    private static final String NOT_YET_AVAILABLE = "Not yet available";

    private static final String UNKNOWN = "(unknown)";

    private static final String REMOTE = "remote";

    private static final String LOCAL = "local";

    private static final String NO_BUILDER_ERROR_MESSAGE = "No subtree builder found for history data item with identifier: ";

    private static final String STRING_SLASH = "/";

    private static final String NODE_TEXT_FORMAT_TITLE_PLUS_HOSTNAME = "%s <%s>";

    private static final String NODE_TEXT_FORMAT_TITLE_PLUS_TIMESTAMP = "%s (%s)";

    private static final String COMPONENT_NAME_AND_NODE_TEXT_FORMAT_TITLE_PLUS_TIMESTAMP = "%s - %s (%s)";

    private static final MetaData METADATA_COMPONENT_CONTEXT_ID = new MetaData(
        MetaDataKeys.COMPONENT_CONTEXT_UUID, true, true);

    private static final MetaData METADATA_WORKFLOW_HAS_DATAREFERENCES = new MetaData(
        MetaDataKeys.WORKFLOW_HAS_DATAREFERENCES, true, true);

    private static final MetaData METADATA_WORKFLOW_IS_MARKED_FOR_DELETION = new MetaData(
        MetaDataKeys.WORKFLOW_MARKED_FOR_DELETION, true, true);

    private static final MetaData METADATA_COMPONENT_CONTEXT_NAME = new MetaData(
        MetaDataKeys.COMPONENT_CONTEXT_NAME, true, true);

    private static final MetaData METADATA_INSTANCE_NODE_IDENTIFIER = new MetaData(
        MetaDataKeys.NODE_IDENTIFIER, true, true);

    private static final MetaData METADATA_COMPONENT_NAME = new MetaData(
        MetaDataKeys.COMPONENT_NAME, true, true);

    private static final MetaData METADATA_HISTORY_DATA_ITEM_IDENTIFIER = new MetaData(
        HistoryMetaDataKeys.HISTORY_HISTORY_DATA_ITEM_IDENTIFIER, true, true);

    private static final MetaData METADATA_HISTORY_USER_INFO_TEXT = new MetaData(
        HistoryMetaDataKeys.HISTORY_USER_INFO_TEXT, true, true);

    private static final MetaData METADATA_WORKFLOW_FINAL_STATE = new MetaData(MetaDataKeys.WORKFLOW_FINAL_STATE, true, true);

    private static final MetaData METADATA_HISTORY_ORDERING = new MetaData(
        HistoryMetaDataKeys.HISTORY_TIMESTAMP, true, true);

    private static final String EXTENSION_POINT_ID_SUBTREE_BUILDER =
        "de.rcenvironment.core.gui.datamanagement.browser.historysubtreebuilder";

    protected final Log log = LogFactory.getLog(getClass());

    private DistributedMetaDataService metaDataService;

    private Map<String, ComponentHistoryDataItemSubtreeBuilder> historySubtreeBuilders;

    /** Cached results for the MetaDataQuery. */
    private Map<Long, WorkflowRun> workflowMetaDataMap = new HashMap<Long, WorkflowRun>();

    /** Used to format timestamps in MetaData to readable dates. */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final List<DMBrowserNodeContentAvailabilityHandler> contentAvailabilityHandlers =
        new CopyOnWriteArrayList<DMBrowserNodeContentAvailabilityHandler>();

    private final Set<DMBrowserNode> inProgress = new CopyOnWriteArraySet<DMBrowserNode>();

    private final Set<String> warningIsShown = new CopyOnWriteArraySet<String>();

    private NodeIdentifier localNodeID;

    private TypedDatumSerializer typedDatumSerializer;

    public DMContentProvider() throws AuthenticationException {
        ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        metaDataService = serviceRegistryAccess.getService(DistributedMetaDataService.class);
        typedDatumSerializer = serviceRegistryAccess.getService(TypedDatumService.class).getSerializer();
        localNodeID = new SimpleCommunicationService().getLocalNodeId();
        registerBuilders();
    }

    private void registerBuilders() {

        historySubtreeBuilders = new HashMap<String, ComponentHistoryDataItemSubtreeBuilder>();

        // get all extensions
        IConfigurationElement[] config = Platform.getExtensionRegistry()
            .getConfigurationElementsFor(EXTENSION_POINT_ID_SUBTREE_BUILDER);
        for (IConfigurationElement e : config) {
            try {
                final Object o = e.createExecutableExtension("class");
                if (o instanceof ComponentHistoryDataItemSubtreeBuilder) {
                    ComponentHistoryDataItemSubtreeBuilder builder = (ComponentHistoryDataItemSubtreeBuilder) o;
                    for (String supported : builder.getSupportedHistoryDataItemIdentifier()) {
                        // do not allow ambiguous mappings
                        if (historySubtreeBuilders.containsKey(supported)) {
                            throw new IllegalStateException("More than one builder tried to register for key " + supported);
                        }
                        // register
                        historySubtreeBuilders.put(supported, builder);
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Registered subtree builder " + o.getClass());
                    }
                }
            } catch (CoreException ex) {
                log.error("Error registering extension " + e, ex);
            }
        }

    }

    @Override
    public DMBrowserNode[] getChildren(Object parent) {
        final DMBrowserNode[] result = getChildren(parent, true);
        return result;
    }

    /**
     * Returns the child elements of the given parent element with the option to choose between synchronous and asynchronous execution.
     * 
     * @param parent the parent {@link DMBrowserNode}
     * @param async true, if execution shall be performed asynchronously in a different thread
     * @return the children
     */
    public DMBrowserNode[] getChildren(Object parent, boolean async) {
        final DMBrowserNode node = (DMBrowserNode) parent;

        if (node.areChildrenKnown()) {
            return node.getChildrenAsArray();
        } else {
            final Runnable retrieverTask = new RetrieverTask(node);
            if (async) {
                Job job = new Job(Messages.dataManagementBrowser) {

                    @Override
                    protected IStatus run(IProgressMonitor monitor) {
                        try {
                            monitor.beginTask(Messages.fetchingData, 3);
                            monitor.worked(2);
                            retrieverTask.run();
                            monitor.worked(1);
                            return Status.OK_STATUS;
                        } finally {
                            monitor.done();
                        }
                    };
                };
                job.setUser(true);
                job.schedule();
                // return a wait signal node as only child
                final DMBrowserNode waitSignalNode = new DMBrowserNode(
                    Messages.waitSignalNodeLabel);
                waitSignalNode.setType(DMBrowserNodeType.Loading);
                waitSignalNode.markAsLeaf();
                return new DMBrowserNode[] { waitSignalNode };
            } else {
                retrieverTask.run();
                return node.getChildrenAsArray();
            }
        }
    }

    /**
     * Fetching the children of {@link DMBrowserNode}.
     * 
     * @param parent the parent {@link DMBrowserNode}
     */
    public void fetchChildren(Object parent) {
        try {
            createChildrenForNode((DMBrowserNode) parent);
        } catch (CommunicationException e) {
            for (final DMBrowserNodeContentAvailabilityHandler handler : contentAvailabilityHandlers) {
                handler.handleContentRetrievalError((DMBrowserNode) parent, e);
            }
        }
    }

    private void createChildrenForNode(final DMBrowserNode node) throws CommunicationException {
        switch (node.getType()) {
        case HistoryRoot:
            createChildrenForHistoryRootNode(node);
            break;
        case Workflow:
            createChildrenForWorkflowNode(node);
            break;
        case Timeline:
            createChildrenForTimelineNode(node);
            break;
        case Components:
            createChildrenForComponentsNode(node);
            break;
        case Component:
        case HistoryObject:
            break;
        default:
            log.warn("Unexpected node type: " + node.getType().name());
        }
    }

    private WorkflowRun getMetaDataForWorkflow(DMBrowserNode workflowNode) throws CommunicationException {
        // extract the id of the desired workflow
        final Long workflowRunID = Long.valueOf(workflowNode.getMetaData().getValue(METADATA_COMPONENT_CONTEXT_ID));

        if (workflowMetaDataMap.containsKey(workflowRunID)) {
            return workflowMetaDataMap.get(workflowRunID);
        }

        final long start = System.currentTimeMillis();
        WorkflowRun result = metaDataService.getWorkflowRun(workflowRunID, workflowNode.getNodeIdentifier());
        final long millis = System.currentTimeMillis() - start;
        if (result == null) {
            log.warn(String.format("Unable to fetch meta data of workflow %d from node %s.", workflowRunID, workflowNode.getName()));
            return null;
        }
        log.debug(String.format("metadata query for workflow \'%s\' took %d ms", result.getWorkflowTitle(),
            millis));
        workflowMetaDataMap.put(workflowRunID, result);
        return result;
    }

    private void createChildrenForHistoryRootNode(DMBrowserNode parent) throws CommunicationException {

        final long start = System.currentTimeMillis();
        Set<WorkflowRunDescription> workflowDescriptions = metaDataService.getWorkflowRunDescriptions();
        log.debug(String.format("query for all workflow run descriptions on all known nodes took %d ms",
            (System.currentTimeMillis() - start)));

        // map<id, node> to keep track of already-known contexts and their tree nodes
        final Map<String, DMBrowserNode> encounteredContexts = new HashMap<>();
        // map<instanceNode, map<id, node>> to keep track of already-known contexts and their tree nodes sorted by instance node id to allow
        // parallelization later one
        Map<String, Map<String, DMBrowserNode>> encounteredContextsByInstanceNode = new HashMap<>();
        // map<id, node> to keep track of already-known contexts and their tree nodes
        final Map<String, Long> workflowStarts = new HashMap<String, Long>();

        /*
         * Iterate over the results and for each new workflow create a node and register the subnodes as children.
         */
        for (final WorkflowRunDescription wfd : workflowDescriptions) {
            // extract the MetaDataSet
            MetaDataSet mds = new MetaDataSet();
            // extract the id of the workflow
            String contextID = wfd.getWorkflowRunID().toString();
            mds.setValue(METADATA_COMPONENT_CONTEXT_ID, contextID);
            // extract the name of the workflow
            String contextName = wfd.getWorkflowTitle();
            mds.setValue(METADATA_COMPONENT_CONTEXT_NAME, contextName);
            // try to extract the instance node id of the workflow
            String instanceNodeId = wfd.getDatamanagementNodeID();
            mds.setValue(METADATA_INSTANCE_NODE_IDENTIFIER, instanceNodeId);
            if (wfd.getFinalState() != null) {
                mds.setValue(METADATA_WORKFLOW_FINAL_STATE, wfd.getFinalState().toString());
            }
            mds.setValue(METADATA_WORKFLOW_HAS_DATAREFERENCES, wfd.getHasDataReferences().toString());
            mds.setValue(METADATA_WORKFLOW_IS_MARKED_FOR_DELETION, wfd.isMarkedForDeletion().toString());
            // if the workflow already has a node, this node is used to register the child nodes
            DMBrowserNode contextDMObject = null;
            // if the meta data for instance node is not set (as introduced in 5.2 first)
            if (instanceNodeId == null) {
                contextDMObject = encounteredContexts.get(contextID);
            } else {
                if (encounteredContexts.containsKey(instanceNodeId)) {
                    contextDMObject = encounteredContextsByInstanceNode.get(instanceNodeId).get(contextID);
                }
            }
            // if it's a new workflow, a new workflow node is created as parent node
            final String startTimeValue = wfd.getStartTime().toString();
            // fix for mantis #6776: apply "virtual" timestamp value so sorting works again
            mds.setValue(METADATA_HISTORY_ORDERING, startTimeValue);
            final boolean startTimeSet = startTimeValue != null;
            final Long workflowStart = workflowStarts.get(contextID);
            final long startTime;
            if (startTimeSet) {
                startTime = Long.parseLong(startTimeValue);
            } else {
                startTime = 0;
            }
            if (contextDMObject == null) {
                // create the workflow node and set its attributes
                contextDMObject = new DMBrowserNode(contextName, parent);
                contextDMObject.setName(contextName);
                contextDMObject.setMetaData(mds);
                contextDMObject.setType(DMBrowserNodeType.Workflow);
                contextDMObject.setWorkflowID(contextID);
                contextDMObject.setWorkflowHostID(wfd.getControllerNodeID());
                // add workflow node to the child node set of the parent (root) node
                parent.addChild(contextDMObject);
                // register as known workflow
                if (instanceNodeId == null) {
                    encounteredContexts.put(contextID, contextDMObject);
                } else {
                    if (!encounteredContextsByInstanceNode.containsKey(instanceNodeId)) {
                        encounteredContextsByInstanceNode.put(instanceNodeId, new HashMap<String, DMBrowserNode>());
                    }
                    encounteredContextsByInstanceNode.get(instanceNodeId).put(contextID, contextDMObject);
                }
                // save workflow start timestamp
                workflowStarts.put(contextID, startTime);
            } else {
                if (workflowStart > startTime) {
                    workflowStarts.put(contextID, startTime);
                }
            }
        }
        for (Map.Entry<String, DMBrowserNode> entry : encounteredContexts.entrySet()) {
            setupWorkflowNode(entry.getValue());
        }

        // create parallel tasks
        CallablesGroup<Void> callablesGroup = SharedThreadPool.getInstance().createCallablesGroup(Void.class);

        for (String instanceNodeId : encounteredContextsByInstanceNode.keySet()) {
            final Map<String, DMBrowserNode> encounteredContextsPerInstanceNode = encounteredContextsByInstanceNode.get(instanceNodeId);
            callablesGroup.add(new Callable<Void>() {

                @Override
                @TaskDescription("Fetch data reference for workflow nodes by instance node id")
                public Void call() throws Exception {
                    for (Map.Entry<String, DMBrowserNode> entry : encounteredContextsPerInstanceNode.entrySet()) {
                        setupWorkflowNode(entry.getValue());
                    }
                    return null;
                }
            });
        }

        callablesGroup.add(new Callable<Void>() {

            @Override
            @TaskDescription("Fetch data reference for workflow nodes")
            public Void call() throws Exception {
                for (Map.Entry<String, DMBrowserNode> entry : encounteredContexts.entrySet()) {
                    setupWorkflowNode(entry.getValue());
                }
                return null;
            }
        });

        callablesGroup.executeParallel(new AsyncExceptionListener() {

            @Override
            public void onAsyncException(Exception e) {
                log.warn("Asynchronous exception during parallel data reference query", e);
            }
        });

        // sort nodes by start time
        parent.sortChildren(DMBrowserNodeUtils.COMPARATOR_BY_HISTORY_TIMESTAMP);
    }

    private void setupWorkflowNode(DMBrowserNode workflowNode) {
        final NodeIdentifier workflowHostID = NodeIdentifierFactory.fromNodeId(workflowNode.getWorkflowHostID());
        if (workflowHostID.getIdString().equals(localNodeID.getIdString())) {
            workflowNode.setWorkflowHostName(LOCAL);
        } else {
            workflowNode.setWorkflowHostName(REMOTE);
        }
        String wfNodeTitle = String.format(NODE_TEXT_FORMAT_TITLE_PLUS_HOSTNAME,
            workflowNode.getName(), workflowNode.getWorkflowHostName());
        if (workflowNode.getMetaData().getValue(METADATA_WORKFLOW_FINAL_STATE) == null) {
            wfNodeTitle =
                String.format(NODE_TEXT_FORMAT_TITLE_PLUS_STATE, wfNodeTitle,
                    NOT_TERMINATED_YET);
        }
        workflowNode.setTitle(wfNodeTitle);
    }

    private void createChildrenForWorkflowNode(final DMBrowserNode workflowNode) throws CommunicationException {
        final WorkflowRun workflowRun = getMetaDataForWorkflow(workflowNode);
        if (workflowRun == null) {
            workflowNode.setEnabled(false);
            return;
        }
        if (workflowRun.getFinalState() != null) {
            workflowNode.getMetaData().setValue(METADATA_WORKFLOW_FINAL_STATE, workflowRun.getFinalState().toString());
        }
        setupWorkflowNode(workflowNode);
        // create run information sub-tree
        DMBrowserNode runInformation = new DMBrowserNode(Messages.runInformationTitle);
        runInformation.setType(DMBrowserNodeType.WorkflowRunInformation);
        FinalWorkflowState finalState = workflowRun.getFinalState();
        Long starttime = workflowRun.getStartTime();
        Long endtime = workflowRun.getEndTime();
        Boolean areFilesDeleted = false;
        for (Set<ComponentRun> allCrs : workflowRun.getComponentRuns().values()) {
            for (ComponentRun cr : allCrs) {
                areFilesDeleted |= cr.isReferencesDeleted();
            }
        }

        NodeIdentifier nodeId = NodeIdentifierFactory.fromNodeId(workflowRun.getControllerNodeID());
        if (nodeId != null) {
            DMBrowserNode.addNewLeafNode(String.format(Messages.runInformationControllerNode, nodeId.getAssociatedDisplayName()),
                DMBrowserNodeType.InformationText, runInformation);
        }
        if (starttime != null) {
            DMBrowserNode.addNewLeafNode(String.format(Messages.runInformationStarttime, dateFormat.format(new Date(starttime))),
                DMBrowserNodeType.InformationText, runInformation);
        }
        if (endtime != null) {
            DMBrowserNode.addNewLeafNode(String.format(Messages.runInformationEndtime, dateFormat.format(new Date(endtime))),
                DMBrowserNodeType.InformationText, runInformation);
        } else {
            if (finalState != null && finalState.equals(FinalWorkflowState.CORRUPTED)) {
                DMBrowserNode.addNewLeafNode(String.format(Messages.runInformationEndtime, UNKNOWN),
                    DMBrowserNodeType.InformationText, runInformation);
            } else {
                DMBrowserNode.addNewLeafNode(String.format(Messages.runInformationEndtime, NOT_YET_AVAILABLE),
                    DMBrowserNodeType.InformationText, runInformation);
            }
        }
        if (finalState != null) {
            DMBrowserNode.addNewLeafNode(String.format(Messages.runInformationFinalState, finalState.getDisplayName()),
                DMBrowserNodeType.InformationText,
                runInformation);
        } else {
            DMBrowserNode.addNewLeafNode(String.format(Messages.runInformationFinalState, NOT_YET_AVAILABLE),
                DMBrowserNodeType.InformationText,
                runInformation);
        }
        if (areFilesDeleted) {
            DMBrowserNode.addNewLeafNode(String.format(Messages.runInformationAdditionalInformation, Messages.runInformationFilesDeleted),
                DMBrowserNodeType.InformationText,
                runInformation);
        }
        workflowNode.addChild(runInformation);
        // create timeline sub-tree
        final DMBrowserNode timelineDMObject = new DMBrowserNode("Timeline");
        timelineDMObject.setType(DMBrowserNodeType.Timeline);
        workflowNode.addChild(timelineDMObject);
        // create components sub-tree
        final DMBrowserNode componentsNode = new DMBrowserNode("Timeline by Component");
        componentsNode.setType(DMBrowserNodeType.Components);
        workflowNode.addChild(componentsNode);
    }

    private void createChildrenForTimelineNode(final DMBrowserNode timelineNode) throws CommunicationException {
        final WorkflowRun workflowRun = getMetaDataForWorkflow(timelineNode.getNodeWithTypeWorkflow());
        if (workflowRun == null) {
            timelineNode.setEnabled(false);
            return;
        }
        for (final ComponentInstance componentInstance : workflowRun.getComponentRuns().keySet()) {
            for (final ComponentRun componentRun : workflowRun.getComponentRuns().get(componentInstance)) {
                MetaDataSet metaDataSet = new MetaDataSet();
                final Long startTime = componentRun.getStartTime();
                metaDataSet.setValue(METADATA_HISTORY_ORDERING, startTime.toString());
                final String startDateString = dateFormat.format(new Date(startTime));
                final String componentSpecificText = String.format("Run %d", componentRun.getRunCounter());
                metaDataSet.setValue(METADATA_HISTORY_USER_INFO_TEXT, componentSpecificText);
                final String componentName = componentInstance.getComponentInstanceName();
                metaDataSet.setValue(METADATA_COMPONENT_NAME, componentName);
                metaDataSet.setValue(METADATA_HISTORY_DATA_ITEM_IDENTIFIER, componentInstance.getComponentID().split(STRING_SLASH)[0]);
                DMBrowserNode dmoChild = new DMBrowserNode(String.format(COMPONENT_NAME_AND_NODE_TEXT_FORMAT_TITLE_PLUS_TIMESTAMP,
                    componentName, componentSpecificText, startDateString), timelineNode);
                // dmoChild.setDataReferenceId(dataReferenceId);
                dmoChild.setMetaData(metaDataSet);
                dmoChild.setType(DMBrowserNodeType.HistoryObject);
                setComponentIconForDMBrowserNode(dmoChild);
                createChildrenForHistoryObjectNode(dmoChild, componentRun);
                // dmoChild.setAssociatedFilename(nodeName);
                timelineNode.addChild(dmoChild);
            }
        }
        // sort nodes by start time
        timelineNode.sortChildren(DMBrowserNodeUtils.COMPARATOR_BY_HISTORY_TIMESTAMP);
    }

    private void createChildrenForComponentsNode(final DMBrowserNode componentsNode) throws CommunicationException {
        final WorkflowRun workflowRun = getMetaDataForWorkflow(componentsNode.getNodeWithTypeWorkflow());
        if (workflowRun == null) {
            componentsNode.setEnabled(false);
            return;
        }
        for (final ComponentInstance componentInstance : workflowRun.getComponentRuns().keySet()) {
            MetaDataSet metaDataSet = new MetaDataSet();
            final String componentName = componentInstance.getComponentInstanceName();
            metaDataSet.setValue(METADATA_COMPONENT_NAME, componentName);
            metaDataSet.setValue(METADATA_HISTORY_DATA_ITEM_IDENTIFIER, componentInstance.getComponentID().split(STRING_SLASH)[0]);
            DMBrowserNode componentNode =
                new DMBrowserNode(String.format("%s (Runs: %d)", componentName, workflowRun.getComponentRuns().get(componentInstance)
                    .size()));
            componentNode.setType(DMBrowserNodeType.Component);
            componentNode.setMetaData(metaDataSet);
            setComponentIconForDMBrowserNode(componentNode);
            componentsNode.addChild(componentNode);
            for (final ComponentRun componentRun : workflowRun.getComponentRuns().get(componentInstance)) {
                final Long startTime = componentRun.getStartTime();
                MetaDataSet metaDataSetRun = new MetaDataSet();
                metaDataSetRun.setValue(METADATA_COMPONENT_NAME, componentName);
                metaDataSetRun.setValue(METADATA_HISTORY_DATA_ITEM_IDENTIFIER, componentInstance.getComponentID().split(STRING_SLASH)[0]);
                metaDataSetRun.setValue(METADATA_HISTORY_ORDERING, startTime.toString());
                final String startDateString = dateFormat.format(new Date(startTime));
                final String componentSpecificText = String.format("Run %d", componentRun.getRunCounter());
                metaDataSetRun.setValue(METADATA_HISTORY_USER_INFO_TEXT, componentSpecificText);
                DMBrowserNode dmoChild = new DMBrowserNode(String.format(NODE_TEXT_FORMAT_TITLE_PLUS_TIMESTAMP,
                    componentSpecificText, startDateString), componentsNode);
                dmoChild.setMetaData(metaDataSetRun);
                dmoChild.setType(DMBrowserNodeType.HistoryObject);
                setComponentIconForDMBrowserNode(dmoChild);
                createChildrenForHistoryObjectNode(dmoChild, componentRun);
                componentNode.addChild(dmoChild);
            }
            componentNode.sortChildren(DMBrowserNodeUtils.COMPARATOR_BY_HISTORY_TIMESTAMP);
        }
        // sort nodes by node title
        componentsNode.sortChildren(DMBrowserNodeUtils.COMPARATOR_BY_NODE_TITLE);
    }

    private void setComponentIconForDMBrowserNode(DMBrowserNode node) {
        String identifier = node.getMetaData().getValue(METADATA_HISTORY_DATA_ITEM_IDENTIFIER);
        ComponentHistoryDataItemSubtreeBuilder builder = getComponentHistoryDataItemSubtreeBuilder(node);
        if (builder != null) {
            node.setIcon(builder.getComponentIcon(identifier));
        } else {
            log.warn(NO_BUILDER_ERROR_MESSAGE + identifier);
        }
    }

    private void createChildrenForHistoryObjectNode(final DMBrowserNode node, ComponentRun componentRun) {

        ComponentHistoryDataItemSubtreeBuilder builder = getComponentHistoryDataItemSubtreeBuilder(node);
        String historyDataItem = componentRun.getHistoryDataItem();
        if (historyDataItem != null) {
            if (builder == null) {
                String identifier = node.getMetaData().getValue(METADATA_HISTORY_DATA_ITEM_IDENTIFIER);
                final String message = NO_BUILDER_ERROR_MESSAGE + identifier;
                // TODO add warning as a tree node for better visibility?
                log.warn(message);
                Display.getDefault().asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        // for some reason createChildrenForHistoryObjectNode is called twice on error -> prohibit that the error dialog is
                        // shown twice -- seid_do, April 2014
                        synchronized (warningIsShown) {
                            if (warningIsShown.contains(node.getPath())) {
                                return;
                            } else {
                                warningIsShown.add(node.getPath());
                            }
                        }
                        MessageDialog.openWarning(Display.getDefault().getActiveShell(), Messages.historyNodeWarningTitle,
                            String.format(Messages.historyNodeWarningMessage, node.getTitle()));
                        synchronized (warningIsShown) {
                            warningIsShown.remove(node.getPath());
                        }
                    }
                });
            } else {
                builder.buildInitialHistoryDataItemSubtree(historyDataItem, node);
            }
        } else {
            DefaultComponentHistoryDataItem defaultHistoryDataItem = new DefaultComponentHistoryDataItem();
            if (componentRun.getEndpointData() != null && !componentRun.getEndpointData().isEmpty()) {
                List<EndpointData> ed = new ArrayList<EndpointData>();
                ed.addAll(componentRun.getEndpointData());
                Collections.sort(ed);
                for (EndpointData endpointData : ed) {
                    TypedDatum td = typedDatumSerializer.deserialize(endpointData.getDatum());
                    switch (endpointData.getEndpointInstance().getEndpointType()) {
                    case INPUT:
                        defaultHistoryDataItem.addInput(endpointData.getEndpointInstance().getEndpointName(), td);
                        break;
                    case OUTPUT:
                        defaultHistoryDataItem.addOutput(endpointData.getEndpointInstance().getEndpointName(), td);
                        break;
                    default:
                        break;
                    }
                }
                CommonHistoryDataItemSubtreeBuilderUtils.buildDefaultHistoryDataItemSubtrees(defaultHistoryDataItem, node);
            } else {
                node.markAsLeaf();
                node.setToolTip(Messages.toolTipNoHistoryData);
            }
        }
        setFileNodesEnabled(node, !componentRun.isReferencesDeleted());
    }

    private void setFileNodesEnabled(DMBrowserNode node, boolean enabled) {
        if (!node.isLeafNode()) {
            for (DMBrowserNode childNode : node.getChildren()) {
                setFileNodesEnabled(childNode, enabled);
            }
        } else if (node.getType().equals(DMBrowserNodeType.DMFileResource)
            || node.getType().equals(DMBrowserNodeType.DMDirectoryReference)) {
            node.setEnabled(enabled);
        }

    }

    @Override
    public DMBrowserNode[] getElements(Object inputElement) {
        final DMBrowserNode[] result = getChildren(inputElement);
        return result;
    }

    @Override
    public Object getParent(Object element) {
        DMBrowserNode dmo = (DMBrowserNode) element;
        if (element == null) {
            return null;
        }

        return dmo.getParent();
    }

    @Override
    public boolean hasChildren(Object parent) {
        if (parent instanceof DMBrowserNode) {
            DMBrowserNode dmo = (DMBrowserNode) parent;
            if (!dmo.areChildrenKnown()) {
                // children unknown -> report "yes" to allow unfolding
                return true;
            } else {
                // children known -> report "yes" if children list not empty
                return dmo.getNumChildren() != 0;
            }
        } else {
            return true;
        }
    }

    protected boolean deleteWorkflowRun(DMBrowserNode browserNode) {
        try {
            metaDataService.deleteWorkflowRun(Long.valueOf(browserNode.getWorkflowID()),
                NodeIdentifierFactory.fromNodeId(browserNode.getWorkflowHostID()));
            return true;
        } catch (CommunicationException e) {
            log.error("Could not delete workflow run in the database.", e);
        }
        return false;
    }

    protected void deleteWorkflowRunFiles(DMBrowserNode browserNode) {
        try {
            metaDataService.deleteWorkflowRunFiles(Long.valueOf(browserNode.getWorkflowID()),
                NodeIdentifierFactory.fromNodeId(browserNode.getWorkflowHostID()));
        } catch (CommunicationException e) {
            log.error("Could not delete workflow run files in the database.", e);
        }
    }

    /**
     * Clear cached meta data.
     */
    public void clear() {
        workflowMetaDataMap.clear();
    }

    /**
     * Clear cached meta data of a specific node.
     * 
     * @param node The node to clear meta data of.
     */
    public void clear(DMBrowserNode node) {
        DMBrowserNode wfNode = node.getNodeWithTypeWorkflow();
        if (wfNode != null) {
            workflowMetaDataMap.remove(Long.valueOf(wfNode.getMetaData().getValue(METADATA_COMPONENT_CONTEXT_ID)));
        }
    }

    @Override
    public void dispose() {
        workflowMetaDataMap.clear();
    }

    @Override
    public void inputChanged(Viewer arg0, Object arg1, Object arg2) {}

    /**
     * Adds a {@link DMBrowserNodeContentAvailabilityHandler}.
     * 
     * @param contentAvailabilityHandler the {@link DMBrowserNodeContentAvailabilityHandler}
     */
    public void addContentAvailabilityHandler(
        final DMBrowserNodeContentAvailabilityHandler contentAvailabilityHandler) {
        contentAvailabilityHandlers.add(contentAvailabilityHandler);
    }

    /**
     * Removes a {@link DMBrowserNodeContentAvailabilityHandler}.
     * 
     * @param contentAvailabilityHandler the {@link DMBrowserNodeContentAvailabilityHandler}
     */
    public void removeContentAvailabilityHandler(
        final DMBrowserNodeContentAvailabilityHandler contentAvailabilityHandler) {
        contentAvailabilityHandlers.remove(contentAvailabilityHandler);
    }

    /**
     * {@link Runnable} realizing the logic for retrieving the content (childs) of a {@link DMBrowserNode}.
     * 
     * @author Christian Weiss
     * 
     */
    private class RetrieverTask implements Runnable {

        private final DMBrowserNode node;

        public RetrieverTask(final DMBrowserNode node) {
            this.node = node;
        }

        @Override
        public void run() {
            // avoid duplicate synchronous retrievals
            synchronized (inProgress) {
                if (inProgress.contains(node)) {
                    return;
                }
                inProgress.add(node);
            }
            try {
                createChildrenForNode(node);
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                for (final DMBrowserNodeContentAvailabilityHandler handler : contentAvailabilityHandlers) {
                    handler.handleContentAvailable(node);
                }
            } catch (RuntimeException e) {
                for (final DMBrowserNodeContentAvailabilityHandler handler : contentAvailabilityHandlers) {
                    handler.handleContentRetrievalError(node, e);
                }
            } catch (CommunicationException e) {
                for (final DMBrowserNodeContentAvailabilityHandler handler : contentAvailabilityHandlers) {
                    handler.handleContentRetrievalError(node, e);
                }
            } finally {
                synchronized (inProgress) {
                    inProgress.remove(node);
                }
            }
        }

    }

    private ComponentHistoryDataItemSubtreeBuilder getComponentHistoryDataItemSubtreeBuilder(DMBrowserNode node) {
        String identifier = node.getMetaData().getValue(METADATA_HISTORY_DATA_ITEM_IDENTIFIER);
        for (String supportedIdentifier : historySubtreeBuilders.keySet()) {
            if (identifier.matches(supportedIdentifier)) {
                return historySubtreeBuilders.get(supportedIdentifier);
            }
        }
        return null;
    }

}
