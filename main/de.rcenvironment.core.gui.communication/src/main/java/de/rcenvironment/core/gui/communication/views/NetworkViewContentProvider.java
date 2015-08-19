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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import de.rcenvironment.core.communication.common.NetworkGraphNode;
import de.rcenvironment.core.communication.connection.api.ConnectionSetup;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.gui.communication.views.model.NetworkGraphNodeWithContext;
import de.rcenvironment.core.gui.communication.views.model.NetworkGraphNodeWithContext.Context;
import de.rcenvironment.core.gui.communication.views.model.NetworkViewModel;

/**
 * The network view content provider.
 * 
 * @author Robert Mischke
 */
public class NetworkViewContentProvider implements IStructuredContentProvider,
    ITreeContentProvider {

    /**
     * The object representing the network root; added to avoid creating a class without an actual purpose.
     */
    public static final Object NETWORK_ROOT_NODE = new Object();

    /**
     * The object representing the parent of the connection entries.
     */
    public static final Object CONNECTIONS_ROOT_NODE = new Object();

    private static final Object[] EMPTY_ARRAY = new Object[0];

    private NetworkViewModel model;

    private boolean optionRawPropertiesVisible = false;

    private Log log = LogFactory.getLog(getClass());

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
        return new Object[] { NETWORK_ROOT_NODE, CONNECTIONS_ROOT_NODE };
    }

    @Override
    public Object[] getChildren(Object parentNode) {
        if (parentNode == NETWORK_ROOT_NODE) {
            if (model.networkGraphWithProperties == null) {
                return EMPTY_ARRAY;
            }
            Collection<? extends NetworkGraphNode> nodes = model.networkGraphWithProperties.getNodes();
            int i = 0;
            Object[] result = new Object[nodes.size()];
            for (NetworkGraphNode node : nodes) {
                NetworkGraphNodeWithContext nodeWithContext = new NetworkGraphNodeWithContext(node, Context.ROOT);
                nodeWithContext.setAttachedNodeProperties(model.getNodeProperties().get(node.getNodeId()));
                result[i++] = nodeWithContext;
            }
            Arrays.sort(result);
            return result;
        }

        if (parentNode == CONNECTIONS_ROOT_NODE) {
            if (model.connectionSetups == null) {
                return EMPTY_ARRAY;
            }
            final ConnectionSetup[] setups = model.connectionSetups.toArray(new ConnectionSetup[model.connectionSetups.size()]);
            Arrays.sort(setups, new Comparator<ConnectionSetup>() {

                @Override
                public int compare(ConnectionSetup o1, ConnectionSetup o2) {
                    return o1.getDisplayName().compareTo(o2.getDisplayName());
                }
            });
            return setups;
        }

        NetworkGraphNodeWithContext typedParentNode = (NetworkGraphNodeWithContext) parentNode;

        // switch by parent node context
        Object[] result;
        int i = 0;
        switch (typedParentNode.getContext()) {
        case ROOT:
            result = new Object[3];
            if (model.componentKnowledge != null) {
                // only show the "published" folder when there are published components
                Collection<ComponentInstallation> publishedInstallations =
                    model.componentKnowledge.getPublishedInstallationsOnNode(typedParentNode.getNode().getNodeId());
                if (publishedInstallations != null && publishedInstallations.size() != 0) {
                    result[i++] = new NetworkGraphNodeWithContext(typedParentNode, Context.PUBLISHED_COMPONENTS_FOLDER);
                }
                if (typedParentNode.isLocalNode()) {
                    result[i++] = new NetworkGraphNodeWithContext(typedParentNode, Context.LOCAL_COMPONENTS_FOLDER);
                }
            }
            if (optionRawPropertiesVisible) {
                result[i++] = new NetworkGraphNodeWithContext(typedParentNode, Context.RAW_NODE_PROPERTIES_FOLDER);
            }
            // trim to actual length
            result = Arrays.copyOfRange(result, 0, i);
            break;
        case LOCAL_COMPONENTS_FOLDER:
            Collection<ComponentInstallation> localInstallations = determineLocalComponents(typedParentNode);
            result = createNodesForComponentInstallations(typedParentNode, localInstallations);
            break;
        case PUBLISHED_COMPONENTS_FOLDER:
            Collection<ComponentInstallation> publishedInstallations =
                model.componentKnowledge.getPublishedInstallationsOnNode(typedParentNode.getNode().getNodeId());
            // note: the parent node is only created if the list is defined and not empty
            result = createNodesForComponentInstallations(typedParentNode, publishedInstallations);
            break;
        case RAW_NODE_PROPERTIES_FOLDER:
            List<NetworkGraphNodeWithContext> children = new ArrayList<NetworkGraphNodeWithContext>();
            Map<String, String> propertyValueMap = typedParentNode.getAttachedNodeProperties();
            if (propertyValueMap != null) {
                for (Entry<String, String> property : propertyValueMap.entrySet()) {
                    NetworkGraphNodeWithContext newChild = new NetworkGraphNodeWithContext(typedParentNode, Context.RAW_NODE_PROPERTY);
                    newChild.setDisplayText(property.getKey() + ": " + property.getValue());
                    children.add(newChild);
                }
            } else {
                // add placeholder
                NetworkGraphNodeWithContext newChild = new NetworkGraphNodeWithContext(typedParentNode, Context.RAW_NODE_PROPERTY);
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

    @Override
    public boolean hasChildren(Object parent) {
        if (parent == NETWORK_ROOT_NODE) {
            return true;
        } else if (parent == CONNECTIONS_ROOT_NODE) {
            return model.connectionSetups != null && !model.connectionSetups.isEmpty();
        } else if (parent instanceof NetworkGraphNodeWithContext) {
            NetworkGraphNodeWithContext typedParentNode = (NetworkGraphNodeWithContext) parent;
            switch (typedParentNode.getContext()) {
            case ROOT:
                if (optionRawPropertiesVisible || typedParentNode.isLocalNode()) {
                    // this assumes that every client node has at least one local component
                    return true;
                } else {
                    if (model.componentKnowledge == null) {
                        return false;
                    }
                    Collection<ComponentInstallation> publishedInstallations =
                        model.componentKnowledge.getPublishedInstallationsOnNode(typedParentNode.getNode().getNodeId());
                    return publishedInstallations != null && !publishedInstallations.isEmpty();
                }
            case RAW_NODE_PROPERTIES_FOLDER:
                return true; // always has entries or a placeholder
            case LOCAL_COMPONENTS_FOLDER:
                Collection<ComponentInstallation> localInstallations = determineLocalComponents(typedParentNode);
                return !localInstallations.isEmpty();
            case PUBLISHED_COMPONENTS_FOLDER:
                Collection<ComponentInstallation> publishedInstallations =
                    model.componentKnowledge.getPublishedInstallationsOnNode(typedParentNode.getNode().getNodeId());
                return publishedInstallations != null && !publishedInstallations.isEmpty();
            case RAW_NODE_PROPERTY:
            case COMPONENT_INSTALLATION:
                return false;
            default:
                return true;
            }
        }
        return false;
    }

    @Override
    public Object getParent(Object child) {
        if (child == NETWORK_ROOT_NODE || child == CONNECTIONS_ROOT_NODE) {
            return model;
        }
        if (child instanceof NetworkGraphNodeWithContext) {
            NetworkGraphNodeWithContext typedNode = (NetworkGraphNodeWithContext) child;
            NetworkGraphNodeWithContext parent = typedNode.getParent();
            if (parent != null) {
                return parent;
            } else {
                return NETWORK_ROOT_NODE;
            }
        }
        // TODO handle connections setups
        log.warn("getParent() fall-through; returning null");
        return null;
    }

    public void setRawPropertiesVisible(boolean value) {
        this.optionRawPropertiesVisible = value;
    }

    private Collection<ComponentInstallation> determineLocalComponents(NetworkGraphNodeWithContext typedParentNode) {
        Collection<ComponentInstallation> localInstallations = model.componentKnowledge.getLocalInstallations();
        // hide all published local components from "local components" list
        Collection<ComponentInstallation> publishedLocalInstallations =
            model.componentKnowledge.getPublishedInstallationsOnNode(typedParentNode.getNode().getNodeId());
        if (publishedLocalInstallations != null) {
            localInstallations = new ArrayList<ComponentInstallation>(localInstallations);
            localInstallations.removeAll(publishedLocalInstallations);
        }
        return localInstallations;
    }

    private Object[] createNodesForComponentInstallations(NetworkGraphNodeWithContext node,
        Collection<ComponentInstallation> installations) {
        Object[] result = new Object[installations.size()];
        int i = 0;
        for (ComponentInstallation installation : installations) {
            if (installation == null) {
                log.warn("Skipping 'null' component installation for node " + node.getNode().getNodeId());
                continue;
            }
            NetworkGraphNodeWithContext newChild = new NetworkGraphNodeWithContext(node, Context.COMPONENT_INSTALLATION);
            newChild.setComponentInstallation(installation);
            result[i++] = newChild;
        }
        Arrays.sort(result);
        return result;
    }
}
