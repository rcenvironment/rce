/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.gui.communication.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import de.rcenvironment.core.communication.common.NetworkGraphNode;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.gui.communication.views.internal.AnchorPoints;
import de.rcenvironment.core.gui.communication.views.model.NetworkGraphNodeWithContext;
import de.rcenvironment.core.gui.communication.views.model.NetworkGraphNodeWithContext.Context;
import de.rcenvironment.core.gui.communication.views.model.NetworkViewModel;
import de.rcenvironment.core.gui.communication.views.spi.ContributedNetworkViewNode;
import de.rcenvironment.core.gui.communication.views.spi.ContributedNetworkViewNodeWithParent;
import de.rcenvironment.core.gui.communication.views.spi.NetworkViewContributor;
import de.rcenvironment.core.gui.communication.views.spi.SelfRenderingNetworkViewNode;
import de.rcenvironment.core.monitoring.system.api.SystemMonitoringDataSnapshot;

/**
 * The network view content provider.
 * 
 * @author Robert Mischke
 * @author David Scholz
 */
public class NetworkViewContentProvider implements IStructuredContentProvider, ITreeContentProvider {

    /**
     * The object representing the network root; added to avoid creating a class without an actual purpose.
     */
    private static final String COLON = ": ";

    private static final Object[] EMPTY_ARRAY = new Object[0];

    private NetworkViewModel model;

    private boolean optionRawPropertiesVisible = false;

    private final List<NetworkViewContributor> rootContributors;

    private final List<NetworkViewContributor> instanceDataContributors;

    private final List<Object> firstLevelNodes = new ArrayList<>();

    private Log log = LogFactory.getLog(getClass());

    public NetworkViewContentProvider(List<NetworkViewContributor> rootContributors,
        List<NetworkViewContributor> instanceDataContributors) {
        this.rootContributors = rootContributors;
        this.instanceDataContributors = instanceDataContributors;
    }

    @Override
    public void inputChanged(Viewer v, Object oldInput, Object newInput) {
        if (newInput == null) {
            // ignore empty input
            return;
        }

        model = (NetworkViewModel) newInput;
    }

    @Override
    public void dispose() {}

    @Override
    public Object[] getElements(Object parent) {
        // keep top-level nodes in a set to recognize them in getParent()
        firstLevelNodes.clear();
        firstLevelNodes.add(AnchorPoints.MAIN_NETWORK_SECTION_PARENT_NODE);
        firstLevelNodes.add(AnchorPoints.SSH_REMOTE_ACCESS_SECTION_PARENT_NODE);

        return firstLevelNodes.toArray();
    }

    @Override
    public Object[] getChildren(Object parentNode) {

        if (parentNode == AnchorPoints.INSTANCES_PARENT_NODE) {
            if (model.networkGraphWithProperties == null) {
                return EMPTY_ARRAY;
            }
            Collection<? extends NetworkGraphNode> nodes = model.networkGraphWithProperties.getNodes();
            int i = 0;
            Object[] result = new Object[nodes.size()];
            for (NetworkGraphNode node : nodes) {
                NetworkGraphNodeWithContext nodeWithContext = new NetworkGraphNodeWithContext(node, Context.ROOT, null);
                nodeWithContext.setAttachedNodeProperties(model.getNodeProperties().get(node.getNodeId()));
                result[i++] = nodeWithContext;
            }
            Arrays.sort(result);
            return result;
        }

        if (parentNode instanceof ContributedNetworkViewNode) {
            if (parentNode instanceof SelfRenderingNetworkViewNode) {
                SelfRenderingNetworkViewNode typedParentNode = (SelfRenderingNetworkViewNode) parentNode;
                // TODO somewhat weird; rework? - misc_ro
                if (!typedParentNode.getHasChildren()) {
                    return EMPTY_ARRAY;
                } else {
                    return typedParentNode.getContributor().getChildren(typedParentNode);
                }
            } else {
                ContributedNetworkViewNode typedParentNode = (ContributedNetworkViewNode) parentNode;
                // if clause to separate old and new approach nodes; fall-through in "null" case for old approach
                if (typedParentNode.getContributor() != null) {
                    return typedParentNode.getContributor().getChildren(typedParentNode);
                }
            }
        }

        if (parentNode == AnchorPoints.MAIN_NETWORK_SECTION_PARENT_NODE
            || parentNode == AnchorPoints.SSH_REMOTE_ACCESS_SECTION_PARENT_NODE) {
            List<Object> result = new ArrayList<>();
            if (parentNode == AnchorPoints.MAIN_NETWORK_SECTION_PARENT_NODE) {
                // old code - hard-coded element
                result.add(AnchorPoints.INSTANCES_PARENT_NODE);
            }
            // new code - contributors
            for (NetworkViewContributor contributor : rootContributors) {
                Object[] contributedNodes = contributor.getTopLevelElements(parentNode);
                if (contributedNodes != null) {
                    for (Object element : contributedNodes) {
                        result.add(element);
                    }
                }
            }
            return result.toArray();
        }

        NetworkGraphNodeWithContext typedParentNode = (NetworkGraphNodeWithContext) parentNode;

        // switch by parent node context
        Object[] result;
        switch (typedParentNode.getContext()) {
        case ROOT:
            List<Object> contribList = new ArrayList<>();
            // new approach
            for (NetworkViewContributor contributor : instanceDataContributors) {
                for (Object contrib : contributor.getChildrenForNetworkInstanceNode(typedParentNode)) {
                    contribList.add(contrib);
                }
            }
            // old approach
            if (optionRawPropertiesVisible) {
                contribList.add(new NetworkGraphNodeWithContext(typedParentNode, Context.RAW_NODE_PROPERTIES_FOLDER, null));
            }
            // trim to actual length
            result = contribList.toArray();
            break;
        case RESOURCE_MONITORING_FOLDER:
            SystemMonitoringDataSnapshot monitoringDataModel = model.getMonitoringDataModelMap().get(typedParentNode.getNode().getNodeId());
            if (monitoringDataModel != null) {
                result =
                    createNodeSystemResources(monitoringDataModel, typedParentNode
                        .getNode().getNodeId());
            } else {
                result = new Object[1];
                result[0] = "Fetching monitoring data...";
            }
            break;
        case RAW_NODE_PROPERTIES_FOLDER:
            List<NetworkGraphNodeWithContext> children = new ArrayList<NetworkGraphNodeWithContext>();
            Map<String, String> propertyValueMap = typedParentNode.getAttachedNodeProperties();
            if (propertyValueMap != null) {
                for (Entry<String, String> property : propertyValueMap.entrySet()) {
                    NetworkGraphNodeWithContext newChild =
                        new NetworkGraphNodeWithContext(typedParentNode, Context.RAW_NODE_PROPERTY, null);
                    newChild.setDisplayText(property.getKey() + COLON + property.getValue());
                    children.add(newChild);
                }
            } else {
                // add placeholder
                NetworkGraphNodeWithContext newChild = new NetworkGraphNodeWithContext(typedParentNode, Context.RAW_NODE_PROPERTY, null);
                newChild.setDisplayText("<unknown>");
                children.add(newChild);
            }
            result = children.toArray(EMPTY_ARRAY);
            Arrays.sort(result);
            break;
        default:
            result = EMPTY_ARRAY;
        }
        return result;
    }

    private Object[] createNodeSystemResources(SystemMonitoringDataSnapshot monitoringDataModel, NodeIdentifier nodeId) {
        return null;
    }

    @Override
    public boolean hasChildren(Object parent) {
        if (parent instanceof SelfRenderingNetworkViewNode) {
            SelfRenderingNetworkViewNode typedParentNode = (SelfRenderingNetworkViewNode) parent;
            return typedParentNode.getHasChildren();
        } else if (parent instanceof ContributedNetworkViewNode && ((ContributedNetworkViewNode) parent).getContributor() != null) {
            // TODO second "if" clause can be removed after full transition
            return ((ContributedNetworkViewNode) parent).getContributor().hasChildren(parent);
        } else if (parent instanceof NetworkGraphNodeWithContext) {
            NetworkGraphNodeWithContext typedParentNode = (NetworkGraphNodeWithContext) parent;
            if (typedParentNode.getContributor() != null) {
                // new approach
                return typedParentNode.getContributor().hasChildren(parent);
            }

            switch (typedParentNode.getContext()) {
            case ROOT:
                // always true now, as each instance's folder contains at least the monitoring data folder
                return true;
            case RAW_NODE_PROPERTIES_FOLDER:
            case RESOURCE_MONITORING_FOLDER:
                return true; // always has entries or a placeholder
            case RAW_NODE_PROPERTY:
                return false;
            default:
                return true;
            }
        } else if (firstLevelNodes.contains(parent) || parent == AnchorPoints.INSTANCES_PARENT_NODE) {
            return true;
        }
        return false;
    }

    @Override
    public Object getParent(Object node) {

        if (node instanceof NetworkGraphNodeWithContext) {
            final NetworkGraphNodeWithContext typedNode = (NetworkGraphNodeWithContext) node;
            final NetworkGraphNodeWithContext parent = typedNode.getParent();
            if (parent != null) {
                return parent;
            } else {
                return AnchorPoints.INSTANCES_PARENT_NODE;
            }
        }

        if (node instanceof ContributedNetworkViewNodeWithParent) {
            return ((ContributedNetworkViewNodeWithParent) node).getParentNode();
        }

        if (node instanceof ContributedNetworkViewNode) {
            final ContributedNetworkViewNode typedNode = (ContributedNetworkViewNode) node;
            final NetworkViewContributor contributor = typedNode.getContributor();
            if (contributor == null) {
                log.warn("getParent() called on contributed node without a contributor: " + node.toString());
                return null;
            }
            Object parent = contributor.getParent(typedNode);
            if (parent == null) {
                log.warn("Contributor returned a null parent for " + node.toString());
            } else if (parent == AnchorPoints.SYMBOLIC_ROOT_NODE) {
                return model; // the actual root node
            } else {
                return parent;
            }
        }

        if (firstLevelNodes.contains(node)) {
            return model;
        }

        log.warn("getParent() fall-through; returning null for node " + node.toString());
        return null;
    }

    public void setRawPropertiesVisible(boolean value) {
        this.optionRawPropertiesVisible = value;
    }

}
