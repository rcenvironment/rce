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
import org.osgi.framework.Version;

import de.rcenvironment.core.authentication.AuthenticationException;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.component.datamanagement.api.DefaultComponentHistoryDataItem;
import de.rcenvironment.core.component.datamanagement.history.HistoryMetaDataKeys;
import de.rcenvironment.core.datamanagement.MetaDataService;
import de.rcenvironment.core.datamanagement.commons.ComponentInstance;
import de.rcenvironment.core.datamanagement.commons.ComponentRun;
import de.rcenvironment.core.datamanagement.commons.EndpointData;
import de.rcenvironment.core.datamanagement.commons.MetaData;
import de.rcenvironment.core.datamanagement.commons.MetaDataKeys;
import de.rcenvironment.core.datamanagement.commons.MetaDataSet;
import de.rcenvironment.core.datamanagement.commons.WorkflowRun;
import de.rcenvironment.core.datamanagement.commons.WorkflowRunDescription;
import de.rcenvironment.core.datamodel.api.DataModelConstants;
import de.rcenvironment.core.datamodel.api.FinalWorkflowState;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.gui.datamanagement.browser.spi.CommonHistoryDataItemSubtreeBuilderUtils;
import de.rcenvironment.core.gui.datamanagement.browser.spi.ComponentHistoryDataItemSubtreeBuilder;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DMBrowserNode;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DMBrowserNodeConstants;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DMBrowserNodeType;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DMBrowserNodeUtils;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.VersionUtils;
import de.rcenvironment.core.utils.common.concurrent.AsyncExceptionListener;
import de.rcenvironment.core.utils.common.concurrent.CallablesGroup;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;
import de.rcenvironment.core.utils.incubator.DebugSettings;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * @author Jan Flink
 * @author Markus Litz
 * @author Robert Mischke
 * @author Christian Weiss
 * @author Brigitte Boden
 * @author Doreen Seider
 */
public class DMContentProvider implements ITreeContentProvider {

    private static final int HUNDRET = 100;

    private static final double FLOATING_HUNDRET = 100.0d;

    private static final double KILO_BYTE = 1024.0;

    private static final String NODE_TEXT_FORMAT_TITLE_PLUS_STATE = "%s --> %s";

    private static final String NOT_TERMINATED_YET = "not terminated yet";

    private static final String NOT_YET_AVAILABLE = "Not yet available";

    private static final String UNKNOWN = "(unknown)";

    private static final String REMOTE = "remote";

    private static final String LOCAL = "local";

    private static final String NO_BUILDER_ERROR_MESSAGE = "No subtree builder found for history data item with identifier: ";

    private static final String STRING_SLASH = "/";

    private static final String NODE_TEXT_FORMAT_TITLE_PLUS_HOSTNAME = "%s <%s>";

    private static final String NODE_TEXT_FORMAT_TITLE_PLUS_TIMESTAMP_AND_HOST = "%s (%s)  <%s>";

    private static final String COMPONENT_NAME_AND_NODE_TEXT_FORMAT_TITLE_PLUS_TIMESTAMP_AND_HOST = "%s - %s (%s)  <%s>";

    private static final MetaData METADATA_COMPONENT_CONTEXT_ID = new MetaData(
        MetaDataKeys.COMPONENT_CONTEXT_UUID, true, true);

    private static final MetaData METADATA_WORKFLOW_FILES_DELETED = new MetaData(
        MetaDataKeys.WORKFLOW_FILES_DELETED, true, true);

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

    private final boolean verboseLogging = DebugSettings.getVerboseLoggingEnabled(getClass());

    private MetaDataService metaDataService;

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
        metaDataService = serviceRegistryAccess.getService(MetaDataService.class);
        typedDatumSerializer = serviceRegistryAccess.getService(TypedDatumService.class).getSerializer();
        localNodeID = serviceRegistryAccess.getService(PlatformService.class).getLocalNodeId();
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
        final DMBrowserNode node = (DMBrowserNode) parent;

        if (node.areChildrenKnown()) {
            return node.getChildrenAsArray();
        } else {
            final Runnable retrieverTask = new RetrieverTask(node);
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
        final NodeIdentifier workflowNodeId = workflowNode.getNodeIdentifier(); // TODO review: not DM node id?

        synchronized (workflowMetaDataMap) {
            if (workflowMetaDataMap.containsKey(workflowRunID)) {
                return workflowMetaDataMap.get(workflowRunID);
            }
        }

        final long start = System.currentTimeMillis();
        WorkflowRun result = null;
        try {
            log.debug(StringUtils.format("Fetching run data of workflow #%s from %s", workflowRunID, workflowNodeId));
            result = metaDataService.getWorkflowRun(workflowRunID, workflowNodeId);
            log.debug(StringUtils.format("Finished fetching run data of workflow #%s from %s", workflowRunID, workflowNodeId));
        } catch (CommunicationException e) {
            // cache anyway to prevent repeated failing remote requests as this method is called multiple times when building the tree nodes
            synchronized (workflowMetaDataMap) {
                workflowMetaDataMap.put(workflowRunID, null);
            }
            log.debug(StringUtils.format("Failed to fetch run data of workflow #%s from %s", workflowRunID, workflowNodeId));
            throw e; // throw exception to keep logic for 6.3; could be improved though - seid_do
        }
        final long millis = System.currentTimeMillis() - start;
        // TODO review: can "null" really happen here? (see catch block above) - misc_ro, Jul 2015
        if (result == null) {
            log.error(StringUtils.format("Unable to fetch meta data of workflow #%d from node %s", workflowRunID,
                workflowNode.getName()));
        } else {
            if (verboseLogging) {
                log.debug(StringUtils.format("Meta data query for workflow #%d (\'%s\') took %d ms", workflowRunID,
                    result.getWorkflowTitle(), millis));
            }
        }
        // cache even in case of 'null' result to prevent repeated failing requests as this method is called multiple times when
        // building the tree nodes
        synchronized (workflowMetaDataMap) {
            workflowMetaDataMap.put(workflowRunID, result);
        }
        return result;
    }

    private void createChildrenForHistoryRootNode(DMBrowserNode parent) throws CommunicationException {

        final long start = System.currentTimeMillis();
        Set<WorkflowRunDescription> workflowDescriptions = metaDataService.getWorkflowRunDescriptions();

        log.debug(StringUtils.format("query for all workflow run descriptions on all known nodes took %d ms",
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
            mds.setValue(METADATA_WORKFLOW_FILES_DELETED, wfd.getAreFilesDeleted().toString());
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
                if (e.getCause() == null) {
                    // log a compressed message; this includes the case of RemoteOperationExceptions, which (by design) never have a "cause"
                    log.warn("Asynchronous exception during parallel data reference query: " + e.toString());
                } else {
                    // on unexpected errors, log the full stacktrace
                    log.warn("Asynchronous exception during parallel data reference query", e);
                }
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
        String wfNodeTitle = StringUtils.format(NODE_TEXT_FORMAT_TITLE_PLUS_HOSTNAME,
            workflowNode.getName(), workflowNode.getWorkflowHostName());
        String finalState = workflowNode.getMetaData().getValue(METADATA_WORKFLOW_FINAL_STATE);
        if (finalState == null) {
            wfNodeTitle =
                StringUtils.format(NODE_TEXT_FORMAT_TITLE_PLUS_STATE, wfNodeTitle,
                    NOT_TERMINATED_YET);
        } else {
            setWorkflowNodeIconFromFinalState(workflowNode, getFinalStateFromString(finalState));
        }
        workflowNode.setTitle(wfNodeTitle);
    }

    private FinalWorkflowState getFinalStateFromString(String finalState) {
        if (finalState != null) {
            if (finalState.equals(FinalWorkflowState.FINISHED.toString())) {
                return FinalWorkflowState.FINISHED;
            } else if (finalState.equals(FinalWorkflowState.FAILED.toString())) {
                return FinalWorkflowState.FAILED;
            } else if (finalState.equals(FinalWorkflowState.CANCELLED.toString())) {
                return FinalWorkflowState.CANCELLED;
            } else if (finalState.equals(FinalWorkflowState.CORRUPTED.toString())) {
                return FinalWorkflowState.CORRUPTED;
            }
        }
        // default
        return null;
    }

    private void setWorkflowNodeIconFromFinalState(DMBrowserNode workflowNode, FinalWorkflowState finalState) {
        if (finalState != null) {
            // Select state icon for workflow
            switch (finalState) {
            case CANCELLED:
                workflowNode.setIcon(ImageManager.getInstance().getSharedImage(StandardImages.CANCELLED));
                break;
            case FINISHED:
                workflowNode.setIcon(ImageManager.getInstance().getSharedImage(StandardImages.FINISHED));
                break;
            case FAILED:
                workflowNode.setIcon(ImageManager.getInstance().getSharedImage(StandardImages.FAILED));
                break;
            case CORRUPTED:
                workflowNode.setIcon(ImageManager.getInstance().getSharedImage(StandardImages.CORRUPTED));
                break;
            default:
                break;
            }
        } else {
            workflowNode.setIcon(null);
        }

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

        addWorkflowErrorLogFileNode(workflowNode, workflowRun);

        // create run information sub-tree
        DMBrowserNode runInformation = new DMBrowserNode(Messages.runInformationTitle);
        runInformation.setType(DMBrowserNodeType.WorkflowRunInformation);
        FinalWorkflowState finalState = workflowRun.getFinalState();
        Long starttime = workflowRun.getStartTime();
        Long endtime = workflowRun.getEndTime();
        Boolean areFilesDeleted = Boolean.valueOf(workflowNode.getMetaData().getValue(METADATA_WORKFLOW_FILES_DELETED));

        if (workflowRun.getWfFileReference() != null) {
            FileReferenceTD wfFileReference = (FileReferenceTD) typedDatumSerializer.deserialize(workflowRun.getWfFileReference());
            DMBrowserNode wfFileNode = new DMBrowserNode(StringUtils.format(Messages.workflowFile, wfFileReference.getFileName()));
            wfFileNode.setType(DMBrowserNodeType.DMFileResource);
            wfFileNode.setDataReferenceId(wfFileReference.getFileReference());
            wfFileNode.setAssociatedFilename(wfFileReference.getFileName());
            wfFileNode.markAsLeaf();
            runInformation.addChild(wfFileNode);
        }

        NodeIdentifier nodeId = NodeIdentifierFactory.fromNodeId(workflowRun.getControllerNodeID());
        if (nodeId != null) {
            DMBrowserNode.addNewLeafNode(StringUtils.format(Messages.runInformationControllerNode, nodeId.getAssociatedDisplayName()),
                DMBrowserNodeType.InformationText, runInformation);
        }
        // create component run information subtree
        DMBrowserNode componentHostInformation = new DMBrowserNode(Messages.componentRunInformationSubtree);
        componentHostInformation.setType(DMBrowserNodeType.ComponentHostInformation);
        runInformation.addChild(componentHostInformation);

        if (workflowRun.getComponentRuns().isEmpty()) {
            DMBrowserNode.addNewLeafNode("Not (yet) available", DMBrowserNodeType.InformationText, componentHostInformation);
        } else {
            for (final ComponentInstance componentInstance : workflowRun.getComponentRuns().keySet()) {

                if (workflowRun.getComponentRuns().get(componentInstance).size() > 0) {

                    final NodeIdentifier componentRunHostID =
                        NodeIdentifierFactory.fromNodeId(workflowRun.getComponentRuns().get(componentInstance).iterator()
                            .next().getNodeID());

                    if (componentRunHostID != null) {
                        DMBrowserNode compNode = DMBrowserNode.addNewLeafNode(
                            StringUtils.format("%s: %s", componentInstance.getComponentInstanceName(),
                                componentRunHostID.getAssociatedDisplayName()),
                            DMBrowserNodeType.Component, componentHostInformation);
                        MetaDataSet metaDataSet = new MetaDataSet();
                        final String componentName = componentInstance.getComponentInstanceName();
                        metaDataSet.setValue(METADATA_COMPONENT_NAME, componentName);
                        metaDataSet.setValue(METADATA_HISTORY_DATA_ITEM_IDENTIFIER, componentInstance.getComponentID()
                            .split(STRING_SLASH)[0]);
                        compNode.setMetaData(metaDataSet);

                        setComponentIconForDMBrowserNode(compNode);
                    }

                }

            }
            componentHostInformation.sortChildren(DMBrowserNodeUtils.COMPARATOR_BY_NODE_TITLE);
        }

        // create timeline sub-tree
        if (starttime != null) {
            DMBrowserNode.addNewLeafNode(StringUtils.format(Messages.runInformationStarttime, dateFormat.format(new Date(starttime))),
                DMBrowserNodeType.InformationText, runInformation);
        }
        if (endtime != null) {
            DMBrowserNode.addNewLeafNode(StringUtils.format(Messages.runInformationEndtime, dateFormat.format(new Date(endtime))),
                DMBrowserNodeType.InformationText, runInformation);
        } else {
            if (finalState != null && finalState.equals(FinalWorkflowState.CORRUPTED)) {
                DMBrowserNode.addNewLeafNode(StringUtils.format(Messages.runInformationEndtime, UNKNOWN),
                    DMBrowserNodeType.InformationText, runInformation);
            } else {
                DMBrowserNode.addNewLeafNode(StringUtils.format(Messages.runInformationEndtime, NOT_YET_AVAILABLE),
                    DMBrowserNodeType.InformationText, runInformation);
            }
        }
        if (finalState != null) {
            DMBrowserNode.addNewLeafNode(StringUtils.format(Messages.runInformationFinalState, finalState.getDisplayName()),
                DMBrowserNodeType.InformationText,
                runInformation);
        } else {
            DMBrowserNode.addNewLeafNode(StringUtils.format(Messages.runInformationFinalState, NOT_YET_AVAILABLE),
                DMBrowserNodeType.InformationText,
                runInformation);
        }
        if (areFilesDeleted) {
            DMBrowserNode.addNewLeafNode(
                StringUtils.format(Messages.additionalInformation, Messages.runInformationFilesDeleted),
                DMBrowserNodeType.InformationText, runInformation);
        }
        if (workflowRun.getAdditionalInformationIfAvailable() != null) {
            DMBrowserNode.addNewLeafNode(
                StringUtils.format(Messages.runInformationAdditionalInformation, workflowRun.getAdditionalInformationIfAvailable()),
                DMBrowserNodeType.InformationText, runInformation);
        }

        workflowNode.addChild(runInformation);
        final DMBrowserNode timelineDMObject = new DMBrowserNode("Timeline");
        timelineDMObject.setType(DMBrowserNodeType.Timeline);
        workflowNode.addChild(timelineDMObject);
        // create components sub-tree
        final DMBrowserNode componentsNode = new DMBrowserNode("Timeline by Component");
        componentsNode.setType(DMBrowserNodeType.Components);
        workflowNode.addChild(componentsNode);
        if (areFilesDeleted) {
            setFileNodesEnabled(workflowNode, false);
        }
    }

    private void createChildrenForTimelineNode(final DMBrowserNode timelineNode) throws CommunicationException {
        final WorkflowRun workflowRun = getMetaDataForWorkflow(timelineNode.getNodeWithTypeWorkflow());
        if (workflowRun == null) {
            timelineNode.setEnabled(false);
            return;
        }
        for (final ComponentInstance componentInstance : workflowRun.getComponentRuns().keySet()) {
            for (final ComponentRun componentRun : workflowRun.getComponentRuns().get(componentInstance)) {
                final NodeIdentifier componentRunHostID = NodeIdentifierFactory.fromNodeId(componentRun.getNodeID());
                final String componentRunHostName;
                if (componentRunHostID.getIdString().equals(localNodeID.getIdString())) {
                    componentRunHostName = LOCAL;
                } else {
                    componentRunHostName = REMOTE;
                }

                MetaDataSet metaDataSet = new MetaDataSet();
                final Long startTime = componentRun.getStartTime();
                metaDataSet.setValue(METADATA_HISTORY_ORDERING, startTime.toString());
                final String startDateString = dateFormat.format(new Date(startTime));
                final String componentSpecificText = getNodeTitleForComponentRun(componentRun);
                metaDataSet.setValue(METADATA_HISTORY_USER_INFO_TEXT, componentSpecificText);
                final String componentName = componentInstance.getComponentInstanceName();
                metaDataSet.setValue(METADATA_COMPONENT_NAME, componentName);
                metaDataSet.setValue(METADATA_HISTORY_DATA_ITEM_IDENTIFIER, componentInstance.getComponentID().split(STRING_SLASH)[0]);
                DMBrowserNode dmoChild =
                    new DMBrowserNode(StringUtils.format(COMPONENT_NAME_AND_NODE_TEXT_FORMAT_TITLE_PLUS_TIMESTAMP_AND_HOST,
                        componentName, componentSpecificText, startDateString, componentRunHostName), timelineNode);
                // dmoChild.setDataReferenceId(dataReferenceId);
                dmoChild.setMetaData(metaDataSet);
                dmoChild.setType(DMBrowserNodeType.HistoryObject);
                setComponentIconForDMBrowserNode(dmoChild);
                createChildrenForHistoryObjectNode(dmoChild, componentRun);
                // dmoChild.setAssociatedFilename(nodeName);
                timelineNode.addChild(dmoChild);

                addComponentLogFilesNode(dmoChild, componentRun);
            }
        }
        // sort nodes by start time
        timelineNode.sortChildren(DMBrowserNodeUtils.COMPARATOR_BY_HISTORY_TIMESTAMP);
        setFileNodesEnabled(timelineNode,
            !Boolean.valueOf(timelineNode.getNodeWithTypeWorkflow().getMetaData().getValue(METADATA_WORKFLOW_FILES_DELETED)));
    }

    private String getNodeTitleForComponentRun(final ComponentRun componentRun) {
        final String componentSpecificText;
        if (componentRun.getRunCounter() == DataModelConstants.TEAR_DOWN_RUN) {
            componentSpecificText = "Tear down";
        } else if (componentRun.getRunCounter() == DataModelConstants.INIT_RUN) {
            componentSpecificText = "Init";
        } else {
            componentSpecificText = StringUtils.format("Run %d", componentRun.getRunCounter());
        }
        return componentSpecificText;
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

            final String componentHostName;
            if (workflowRun.getComponentRuns().get(componentInstance).size() > 0) {
                if (NodeIdentifierFactory.fromNodeId(
                    workflowRun.getComponentRuns().get(componentInstance).iterator().next().getNodeID()).getIdString()
                    .equals(localNodeID.getIdString())) {
                    componentHostName = LOCAL;
                } else {
                    componentHostName = REMOTE;
                }
            } else {
                componentHostName = "";
            }

            DMBrowserNode componentNode = new DMBrowserNode("");
            componentNode.setType(DMBrowserNodeType.Component);
            componentNode.setMetaData(metaDataSet);
            setComponentIconForDMBrowserNode(componentNode);
            boolean initIncluded = false;
            boolean tearDownIncluded = false;
            for (final ComponentRun componentRun : workflowRun.getComponentRuns().get(componentInstance)) {
                final NodeIdentifier componentRunHostID = NodeIdentifierFactory.fromNodeId(componentRun.getNodeID());
                final String componentRunHostName;
                if (componentRunHostID.getIdString().equals(localNodeID.getIdString())) {
                    componentRunHostName = LOCAL;
                } else {
                    componentRunHostName = REMOTE;
                }

                final Long startTime = componentRun.getStartTime();
                MetaDataSet metaDataSetRun = new MetaDataSet();
                metaDataSetRun.setValue(METADATA_COMPONENT_NAME, componentName);
                metaDataSetRun.setValue(METADATA_HISTORY_DATA_ITEM_IDENTIFIER, componentInstance.getComponentID().split(STRING_SLASH)[0]);
                metaDataSetRun.setValue(METADATA_HISTORY_ORDERING, startTime.toString());
                final String startDateString = dateFormat.format(new Date(startTime));
                final String componentSpecificText = getNodeTitleForComponentRun(componentRun);
                metaDataSetRun.setValue(METADATA_HISTORY_USER_INFO_TEXT, componentSpecificText);
                DMBrowserNode dmoChild = new DMBrowserNode(StringUtils.format(NODE_TEXT_FORMAT_TITLE_PLUS_TIMESTAMP_AND_HOST,
                    componentSpecificText, startDateString, componentRunHostName), componentsNode);
                dmoChild.setMetaData(metaDataSetRun);
                dmoChild.setType(DMBrowserNodeType.HistoryObject);
                setComponentIconForDMBrowserNode(dmoChild);
                createChildrenForHistoryObjectNode(dmoChild, componentRun);
                componentNode.addChild(dmoChild);

                addComponentLogFilesNode(dmoChild, componentRun);
                if (componentRun.getRunCounter() == DataModelConstants.INIT_RUN) {
                    initIncluded = true;
                } else if (componentRun.getRunCounter() == DataModelConstants.TEAR_DOWN_RUN) {
                    tearDownIncluded = true;
                }
            }
            final String finalState = componentInstance.getFinalState();
            String componentNodeText = StringUtils.format("%s (Runs: %d) <%s>", componentName,
                getComponentRunCount(workflowRun, componentInstance, initIncluded, tearDownIncluded), componentHostName);
            if (finalState != null && finalState.equals("FAILED")) {
                componentNodeText = componentNodeText.concat(" [FAILED]");
            }
            componentNode.setTitle(componentNodeText);
            componentsNode.addChild(componentNode);

            componentNode.sortChildren(DMBrowserNodeUtils.COMPARATOR_BY_HISTORY_TIMESTAMP);
        }
        // sort nodes by node title
        componentsNode.sortChildren(DMBrowserNodeUtils.COMPARATOR_BY_NODE_TITLE);
        setFileNodesEnabled(componentsNode,
            !Boolean.valueOf(componentsNode.getNodeWithTypeWorkflow().getMetaData().getValue(METADATA_WORKFLOW_FILES_DELETED)));
    }

    private int getComponentRunCount(WorkflowRun workflowRun, ComponentInstance componentInstance, boolean initIncluded,
        boolean tearDownIncluded) {
        int runCount = workflowRun.getComponentRuns().get(componentInstance).size();
        if (initIncluded) {
            runCount--;
        }
        if (tearDownIncluded) {
            runCount--;
        }
        return runCount;
    }

    /**
     * Creates a browser node for the component log files.
     * 
     * @param componentRun
     */
    private void addComponentLogFilesNode(DMBrowserNode parentNode, ComponentRun componentRun) {
        String logFileRef = componentRun.getLogFile();
        String errorLogFileRef = componentRun.getErrorLogFile();
        boolean logFilesAlwaysStored = VersionUtils.getVersionOfCoreBundles().compareTo(new Version("7.0.0")) >= 0;
        if (logFilesAlwaysStored
            || (logFileRef != null || errorLogFileRef != null)) {

            DMBrowserNode executionLogNode = null;
            for (DMBrowserNode node : parentNode.getChildren()) {
                if (node.getTitle().equals(DMBrowserNodeConstants.NODE_NAME_EXECUTION_LOG)) {
                    executionLogNode = node;
                    break;
                }
            }
            if (executionLogNode != null) {
                // in order to keep it the latest node for all component even if the have stored additional information and thus, created
                // the node "Execution Log" beforehand
                parentNode.removeChild(executionLogNode);
            } else {
                executionLogNode = new DMBrowserNode(DMBrowserNodeConstants.NODE_NAME_EXECUTION_LOG);
                executionLogNode.setType(DMBrowserNodeType.LogFolder);
            }
            if (logFilesAlwaysStored || logFileRef != null) {
                executionLogNode.addChild(createLogFileNode(logFileRef, false));
            }
            if (logFilesAlwaysStored || errorLogFileRef != null) {
                executionLogNode.addChild(createLogFileNode(errorLogFileRef, true));
            }

            parentNode.addChild(executionLogNode);

            // setFileNo/desEnabled(executionLogNode, !componentRun.isReferencesDeleted());
        }
    }

    private void addWorkflowErrorLogFileNode(DMBrowserNode parentNode, WorkflowRun workflowRun) {
        boolean logFilesAlwaysStored = VersionUtils.getVersionOfCoreBundles().compareTo(new Version("7.0.0")) >= 0;
        String errorLogRef = workflowRun.getErrorLogFileReference();
        if (logFilesAlwaysStored || errorLogRef != null) {
            DMBrowserNode errorLogNode = createLogFileNode(errorLogRef, true);
            parentNode.addChild(errorLogNode);
        }
    }

    /**
     * Creates a browser node for a log file.
     */
    private DMBrowserNode createLogFileNode(String logFileRef, boolean errorLog) {
        DMBrowserNode logFileNode;
        if (logFileRef != null) {
            FileReferenceTD logFileReference = (FileReferenceTD) typedDatumSerializer.deserialize(logFileRef);
            logFileNode = new DMBrowserNode(getTitleForLogFileDMBrowserNode(logFileReference));
            logFileNode.setType(DMBrowserNodeType.DMFileResource);
            logFileNode.setDataReferenceId(logFileReference.getFileReference());
            logFileNode.setAssociatedFilename(logFileReference.getFileName());
        } else {
            if (errorLog) {
                logFileNode = new DMBrowserNode("[no error log]");
            } else {
                logFileNode = new DMBrowserNode("[no log]");
            }
            logFileNode.setType(DMBrowserNodeType.Custom);
            logFileNode.setIcon(ImageManager.getInstance().getSharedImage(StandardImages.DATATYPE_FILE_16));
            logFileNode.setEnabled(false);
        }
        logFileNode.markAsLeaf();
        return logFileNode;
    }

    private String getTitleForLogFileDMBrowserNode(FileReferenceTD logFileReference) {
        double fileSizeInKB = logFileReference.getFileSizeInBytes() / KILO_BYTE;
        String nodeTitle;
        if (fileSizeInKB == 0) {
            nodeTitle = StringUtils.format("%s [0 KB]", logFileReference.getFileName());
        } else if (fileSizeInKB < 1) {
            nodeTitle = StringUtils.format("%s [%.2f KB]", logFileReference.getFileName(),
                Math.round(fileSizeInKB * HUNDRET) / FLOATING_HUNDRET);
        } else {
            nodeTitle = StringUtils.format("%s [%d KB]", logFileReference.getFileName(), Math.round(fileSizeInKB));
        }
        return nodeTitle;
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

        final NodeIdentifier componentRunHostID = NodeIdentifierFactory.fromNodeId(componentRun.getNodeID());

        if (componentRunHostID != null) {
            DMBrowserNode.addNewLeafNode(
                StringUtils.format(Messages.componentRunInformationNode, componentRunHostID.getAssociatedDisplayName()),
                DMBrowserNodeType.InformationText, node);
        }
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
                            StringUtils.format(Messages.historyNodeWarningMessage, node.getTitle()));
                        synchronized (warningIsShown) {
                            warningIsShown.remove(node.getPath());
                        }
                    }
                });
            } else {
                builder.buildInitialHistoryDataItemSubtree(historyDataItem, node);
            }
        }
        DefaultComponentHistoryDataItem defaultHistoryDataItem = new DefaultComponentHistoryDataItem();
        if (componentRun.getEndpointData() != null && !componentRun.getEndpointData().isEmpty()) {
            List<EndpointData> ed = new ArrayList<EndpointData>();
            ed.addAll(componentRun.getEndpointData());
            Collections.sort(ed);
            for (EndpointData endpointData : ed) {
                Map<String, String> metaData = endpointData.getEndpointInstance().getMetaData();
                TypedDatum td = typedDatumSerializer.deserialize(endpointData.getDatum());
                switch (endpointData.getEndpointInstance().getEndpointType()) {
                case INPUT:
                    defaultHistoryDataItem.addInput(endpointData.getEndpointInstance().getEndpointName(), td);
                    defaultHistoryDataItem.setInputMetaData(endpointData.getEndpointInstance().getEndpointName(), metaData);
                    break;
                case OUTPUT:
                    defaultHistoryDataItem.addOutput(endpointData.getEndpointInstance().getEndpointName(), td);
                    defaultHistoryDataItem.setOutputMetaData(endpointData.getEndpointInstance().getEndpointName(), metaData);
                    break;
                default:
                    break;
                }
            }
            CommonHistoryDataItemSubtreeBuilderUtils.buildDefaultHistoryDataItemSubtrees(defaultHistoryDataItem, node);
        }
    }

    private void setFileNodesEnabled(DMBrowserNode node, boolean enable) {
        if (!node.isLeafNode() && node.areChildrenKnown()) {
            for (DMBrowserNode childNode : node.getChildren()) {
                setFileNodesEnabled(childNode, enable);
            }
        } else if (node.getType().equals(DMBrowserNodeType.DMFileResource)
            || node.getType().equals(DMBrowserNodeType.DMDirectoryReference)) {
            node.setEnabled(enable);
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
            browserNode.getMetaData().setValue(METADATA_WORKFLOW_FILES_DELETED, String.valueOf(true));
        } catch (CommunicationException e) {
            log.error("Could not delete workflow run files in the database.", e);
        }
    }

    /**
     * Clear cached meta data.
     */
    public void clear() {
        synchronized (workflowMetaDataMap) {
            workflowMetaDataMap.clear();
        }
    }

    /**
     * Clear cached meta data of a specific node.
     * 
     * @param node The node to clear meta data of.
     */
    public void clear(DMBrowserNode node) {
        DMBrowserNode wfNode = node.getNodeWithTypeWorkflow();
        if (wfNode != null) {
            synchronized (workflowMetaDataMap) {
                workflowMetaDataMap.remove(Long.valueOf(wfNode.getMetaData().getValue(METADATA_COMPONENT_CONTEXT_ID)));
            }
        }
    }

    @Override
    public void dispose() {
        synchronized (workflowMetaDataMap) {
            workflowMetaDataMap.clear();
        }
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
