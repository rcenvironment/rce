/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.communication.views.model;

import java.util.Map;

import de.rcenvironment.core.communication.common.NetworkGraphNode;
import de.rcenvironment.core.communication.common.WorkflowHostUtils;
import de.rcenvironment.core.communication.nodeproperties.NodeProperty;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.gui.communication.views.spi.ContributedNetworkViewNode;
import de.rcenvironment.core.gui.communication.views.spi.NetworkViewContributor;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Representation of a tree node within the subtree of a {@link NetworkGraphNode}, including the subtree root.
 * 
 * @author Robert Mischke
 */
public class NetworkGraphNodeWithContext implements Comparable<NetworkGraphNodeWithContext>, ContributedNetworkViewNode {

    // These constants are only used in our implementation of compareTo. They are moved to class-wide constants in order to stop checkstyle
    // complaining about magic numbers.
    private static final int THIS_SMALLER_THAN_OTHER = -1;

    private static final int THIS_GREATER_THAN_OTHER = 1;

    /**
     * This context defines what information aspect of a {@link NetworkGraphNode} is represented by this tree node, or the subtree below it.
     * 
     * @author Robert Mischke
     */
    public enum Context {
        /**
         * The network node itself.
         */
        ROOT,
        /**
         * The parent folder of the {@link NodeProperty} values of this node.
         */
        RAW_NODE_PROPERTIES_FOLDER,
        /**
         * A single {@link NodeProperty}.
         */
        RAW_NODE_PROPERTY,
        /**
         * A single {@link ComponentInstallation}.
         */
        COMPONENT_INSTALLATION,
        /**
         * System resource informations.
         */
        RESOURCE_MONITORING_FOLDER
    }

    private final NetworkGraphNode node;

    private final Context context;

    private Map<String, String> attachedNodeProperties;

    private ComponentInstallation componentInstallation;

    private String displayText;

    private final NetworkGraphNodeWithContext parent;

    // the (optional) contributor of this node, which is also used for fetching its children
    private final NetworkViewContributor contributor;

    // The optional ComponentEntry containing additional information about the publication of this component on the remote instance.
    private DistributedComponentEntry distributedComponentEntry;

    public NetworkGraphNodeWithContext(NetworkGraphNodeWithContext parent, Context context, NetworkViewContributor contributor) {
        this.parent = parent;
        this.node = parent.node;
        this.attachedNodeProperties = parent.attachedNodeProperties;
        this.context = context;
        this.contributor = contributor;
    }

    public NetworkGraphNodeWithContext(NetworkGraphNode node, Context context, NetworkViewContributor contributor) {
        this.parent = null;
        this.node = node;
        this.context = context;
        this.contributor = contributor;
    }

    public NetworkGraphNodeWithContext getParent() {
        return parent;
    }

    public NetworkGraphNode getNode() {
        return node;
    }

    public Context getContext() {
        return context;
    }

    public Map<String, String> getAttachedNodeProperties() {
        return attachedNodeProperties;
    }

    public void setAttachedNodeProperties(Map<String, String> attachedNodeProperties) {
        this.attachedNodeProperties = attachedNodeProperties;
    }

    public ComponentInstallation getComponentInstallation() {
        return componentInstallation;
    }

    public void setComponentInstallation(ComponentInstallation componentInstallation) {
        this.componentInstallation = componentInstallation;
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    @Override
    public NetworkViewContributor getContributor() {
        return contributor;
    }

    public void setDistributedComponentEntry(DistributedComponentEntry entry) {
        this.distributedComponentEntry = entry;
    }

    public DistributedComponentEntry getDistributedComponentEntry() {
        return distributedComponentEntry;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof NetworkGraphNodeWithContext)) {
            return false;
        }
        NetworkGraphNodeWithContext other = (NetworkGraphNodeWithContext) obj;
        // check node and context
        if (!node.getNodeId().equals(other.node.getNodeId()) || !context.equals(other.context)) {
            return false;
        }
        // note: context is known to be the same, so it is not checked for equality again
        if (context == Context.COMPONENT_INSTALLATION) {
            if (this.componentInstallation == null && other.componentInstallation == null) {
                return this == other;
            } else if (this.componentInstallation == null || other.componentInstallation == null) {
                // Since we did not enter the previous branch of the if-statement, we know at this point that one of the component
                // installations is not null. Hence, the two network nodes must represent different objects.
                return false;
            } else {
                if (componentInstallation.getInstallationId() == null) {
                    throw new IllegalStateException("ComponentInstallation has a 'null' id: " + componentInstallation.toString());
                }
                if (other.componentInstallation.getInstallationId() == null) {
                    throw new IllegalStateException(
                        "Other componentInstallation has a 'null' id: " + other.componentInstallation.toString());
                }
                return componentInstallation.getInstallationId().equals(other.componentInstallation.getInstallationId());
            }
        }
        if (context == Context.RAW_NODE_PROPERTY) {
            return displayText.equals(other.displayText);
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = node.getNodeId().hashCode() ^ context.hashCode();
        if (displayText != null) {
            hashCode ^= displayText.hashCode();
        }
        if (componentInstallation != null) {
            hashCode ^= componentInstallation.getInstallationId().hashCode();
        }
        return hashCode;
    }

    @Override
    public int compareTo(NetworkGraphNodeWithContext other) {
        if (context != other.context) {
            throw new IllegalStateException("Unexpected comparison with different contexts");
        }
        switch (context) {
        case ROOT:
            return getDisplayNameOfNode().compareTo(other.getDisplayNameOfNode());
        case COMPONENT_INSTALLATION:
            // We want to sort the component installations first by accessibility (where accessible components precede inaccessible ones),
            // and by their display name as a secondary criterion.
            if (this.componentInstallation != null && other.componentInstallation != null) {
                return this.componentInstallation.getComponentInterface().getDisplayName()
                    .compareTo(other.componentInstallation.getComponentInterface().getDisplayName());
            } else if (this.componentInstallation == null && other.componentInstallation != null) {
                return THIS_GREATER_THAN_OTHER;
            } else if (this.componentInstallation != null && other.componentInstallation == null) {
                return THIS_SMALLER_THAN_OTHER;
            } else {
                final int thisHashCode = this.getDistributedComponentEntry().hashCode();
                final int otherHashCode = other.getDistributedComponentEntry().hashCode();
                if (thisHashCode < otherHashCode) {
                    return THIS_SMALLER_THAN_OTHER;
                } else if (thisHashCode == otherHashCode) {
                    return 0;
                } else {
                    return THIS_GREATER_THAN_OTHER;
                }
            }
        case RAW_NODE_PROPERTY:
            return displayText.compareTo(other.displayText);
        default:
            throw new IllegalStateException("Unexpected comparison for context " + context);
        }
    }

    /**
     * @return the display of this node, as determined from the attached properties; if none is present, "<unknown>" is returned
     */
    public String getDisplayNameOfNode() {
        String displayName = "<unknown>";
        if (attachedNodeProperties != null) {
            String propertyValue = attachedNodeProperties.get("displayName");
            if (propertyValue != null) {
                displayName = propertyValue;
            }
        }
        return displayName;
    }

    public boolean isLocalNode() {
        return node.isLocalNode();
    }

    /**
     * @return true if this node's properties indicate that it is a "workflow host"
     */
    public boolean isWorkflowHost() {
        if (attachedNodeProperties == null) {
            return false;
        } else {
            return WorkflowHostUtils.doNodePropertiesIndicateWorkflowHost(attachedNodeProperties);
        }
    }

    @Override
    public String toString() {
        // very simple; does not cover component installations etc.
        return StringUtils.format("%s [%s]", getDisplayNameOfNode(), context);
    }

}
