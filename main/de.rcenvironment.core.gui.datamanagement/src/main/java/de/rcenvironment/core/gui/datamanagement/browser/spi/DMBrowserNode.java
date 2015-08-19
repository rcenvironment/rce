/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.datamanagement.browser.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.swt.graphics.Image;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.datamanagement.commons.DataReference;
import de.rcenvironment.core.datamanagement.commons.MetaData;
import de.rcenvironment.core.datamanagement.commons.MetaDataKeys;
import de.rcenvironment.core.datamanagement.commons.MetaDataSet;

/**
 * @author Robert Mischke (based on DMObject class by Markus Litz)
 * 
 */
public class DMBrowserNode {

    private static final List<DMBrowserNode> IMMUTABLE_NO_CHILDREN_LIST = Collections.unmodifiableList(new ArrayList<DMBrowserNode>(0));

    private String title;

    private String toolTip;

    private String name;

    private String workflowID;

    private MetaDataSet metaData;

    private DMBrowserNode parent;

    private DMBrowserNodeType type;

    private List<DMBrowserNode> children = null;

    private DataReference dataReference = null;

    private String dataReferenceId = null;

    private String fileReferencePath = null;

    private String associatedFilename = null;

    private String workflowHostName = null;

    private String workflowHostID = null;

    private Image icon = null;

    // if a workflow node is deleted and the subtree was not built before, it will be built prior deletion in order to fetch all related
    // data reference ids. That means, that child nodes, which doesn't have any data reference ids associated (childs included), must not be
    // built at all. For that reason, the underlying purpose can be stored within the node itself and can be requested
    private boolean builtForDeletionPurpose = false;

    private Boolean enabled;

    public DMBrowserNode(String title) {
        this.title = title;
        this.enabled = true;
    }

    public DMBrowserNode(String title, DMBrowserNode parent) {
        this.title = title;
        this.enabled = true;
        setParent(parent);
    }

    /**
     * Convenience method that creates a new node and adds it to its parent.
     * 
     * @param title the title to display
     * @param type the {@link DMBrowserNodeType} for the new node
     * @param parent the parent to add this node to
     * @return the new child node
     */
    public static DMBrowserNode addNewChildNode(String title, DMBrowserNodeType type, DMBrowserNode parent) {
        DMBrowserNode result = new DMBrowserNode(title, parent);
        result.setBuiltForDeletionPurpose(parent.isBuiltForDeletionPurpose());
        result.setType(type);
        parent.addChild(result);
        return result;
    }

    /**
     * Convenience method that creates a leaf node with a pre-defined type and adds it to its parent. Leaf nodes are not meant to (and in
     * fact, cannot) contain children.
     * 
     * @param title the title to display
     * @param type the {@link DMBrowserNodeType} for the new node
     * @param parent the parent to add this node to
     * @return the new child node
     */
    public static DMBrowserNode addNewLeafNode(String title, DMBrowserNodeType type, DMBrowserNode parent) {
        DMBrowserNode result = new DMBrowserNode(title, parent);
        result.setBuiltForDeletionPurpose(parent.isBuiltForDeletionPurpose());
        result.setType(type);
        result.markAsLeaf();
        parent.addChild(result);
        return result;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the path of the node.
     * 
     * @return path of the node
     */
    public String getPath() {
        final StringBuilder builder = new StringBuilder();
        final DMBrowserNode parentNode = getParent();
        if (parentNode != null) {
            builder.append(parentNode.getPath());
        }
        builder.append('/');
        if (type != null) {
            switch (type) {
            case HistoryRoot:
                break;
            case Workflow:
                builder.append("workflow:");
                builder.append(workflowID);
                builder.append("_");
                builder.append(getNodeIdentifier());
                break;
            case HistoryObject:
            default:
                builder.append(getTitle());
            }
        } else {
            builder.append(getTitle());
        }
        return builder.toString();
    }

    public DMBrowserNode getParent() {
        return parent;
    }

    public final DMBrowserNodeType getType() {
        return type;
    }

    public final void setType(DMBrowserNodeType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return getTitle();
    }

    /**
     * Defines that this node is not meant to contain children.
     */
    public void markAsLeaf() {
        children = IMMUTABLE_NO_CHILDREN_LIST;
    }

    public Boolean isLeafNode() {
        return areChildrenKnown() && children.isEmpty();
    }

    /**
     * @return true if and only if the children of this node have already been computed
     */
    public boolean areChildrenKnown() {
        return children != null;
    }

    /**
     * Adds a new child to this node.
     * 
     * @param child the new child to add
     */
    public void addChild(DMBrowserNode child) {
        ensureChildListCreated();
        if (children == IMMUTABLE_NO_CHILDREN_LIST) {
            throw new IllegalStateException("Parent node for addChild was marked as a leaf before");
        }
        if (children.contains(child)) {
            return;
        }
        children.add(child);
        if (child.getParent() != this) {
            child.setParent(this);
        }
    }

    /**
     * @return a read-only list of this nodes children
     */
    public List<DMBrowserNode> getChildren() {
        ensureChildListCreated();
        return Collections.unmodifiableList(children);
    }

    /**
     * Removes a child node.
     * 
     * @param child the child node to remove
     */
    public void removeChild(final DMBrowserNode child) {
        if (children != null) {
            if (children.contains(child)) {
                children.remove(child);
                child.setParent(null);
            }
        }
    }

    /**
     * Resets the children-state of this node to the initial state.
     */
    public void clearChildren() {
        children = null;
    }

    /**
     * @return this nodes children as an array.
     */
    public DMBrowserNode[] getChildrenAsArray() {
        ensureChildListCreated();
        return children.toArray(new DMBrowserNode[0]);
    }

    /**
     * Sorts the children of this node.
     * 
     * @param comparator the {@link Comparator} to use
     */
    public void sortChildren(Comparator<DMBrowserNode> comparator) {
        ensureChildListCreated();
        Collections.sort(children, comparator);
    }

    /**
     * @return the number of children this node has; also returns zero if the children have not been computed yet
     */
    public int getNumChildren() {
        if (children == null) {
            return 0;
        } else {
            return children.size();
        }
    }

    private void ensureChildListCreated() {
        if (children == null) {
            children = new ArrayList<DMBrowserNode>();
        }
    }

    public void setMetaData(MetaDataSet mds) {
        this.metaData = mds;
    }

    public MetaDataSet getMetaData() {
        return metaData;
    }

    public void setDataReference(DataReference dataReference) {
        this.dataReference = dataReference;
    }

    public DataReference getDataReference() {
        return dataReference;
    }

    /**
     * FIXME @weis_cr: javadoc.
     * 
     * @param parent FIXME javadoc
     */
    @Deprecated
    public void setParent(DMBrowserNode parent) {
        // remove child node from old parent node
        if (this.parent != null) {
            this.parent.removeChild(this);
        }
        // save as current parent node
        this.parent = parent;
        // add to current parent node as child node
        if (parent != null && !parent.getChildren().contains(this)) {
            parent.addChild(this);
        }
    }

    public String getDataReferenceId() {
        return dataReferenceId;
    }

    public void setDataReferenceId(String dataReferenceId) {
        this.dataReferenceId = dataReferenceId;
    }

    public String getFileReferencePath() {
        return fileReferencePath;
    }

    public void setFileReferencePath(String fileReferencePath) {
        this.fileReferencePath = fileReferencePath;
    }

    public String getAssociatedFilename() {
        return associatedFilename;
    }

    public void setAssociatedFilename(String associatedFilename) {
        this.associatedFilename = associatedFilename;
    }

    public String getWorkflowHostName() {
        return workflowHostName;
    }

    public void setWorkflowHostName(String workflowHostName) {
        this.workflowHostName = workflowHostName;
    }

    public Image getIcon() {
        return icon;
    }

    public void setIcon(Image icon) {
        this.icon = icon;
    }

    public void setWorkflowID(String workflowID) {
        this.workflowID = workflowID;
    }

    public String getWorkflowID() {
        return workflowID;
    }

    @Override
    public int hashCode() {
        return getPath().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof DMBrowserNode)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        return getPath().equals(((DMBrowserNode) obj).getPath());
    }

    /**
     * 
     * Recursive method to access the node with {@link DMBrowserNodeType#Workflow}.
     * 
     * Returns this node, if the type equals {@link DMBrowserNodeType#Workflow}. If unequal, recursive methode call with the parent node
     * 
     * @return node with the type {@link DMBrowserNodeType#Workflow}
     */
    public DMBrowserNode getNodeWithTypeWorkflow() {
        if (getType() == DMBrowserNodeType.Workflow) {
            return this;
        } else if (getParent() != null) {
            return getParent().getNodeWithTypeWorkflow();
        }
        return null;
    }

    /**
     * 
     * {@link NodeIdentifier} is only available, when node type equals {@link DMBrowserNodeType#Workflow}. Use
     * {@link #getNodeWithTypeWorkflow()} to access the the node identifier.
     * 
     * @return {@link NodeIdentifier}
     */
    public NodeIdentifier getNodeIdentifier() {
        if (dataReference != null && dataReference.getNodeIdentifier() != null) {
            return dataReference.getNodeIdentifier();
        } else {
            String instanceNodeIdentifier = metaData.getValue(new MetaData(MetaDataKeys.NODE_IDENTIFIER, true, true));
            if (instanceNodeIdentifier != null) {
                return NodeIdentifierFactory.fromNodeId(instanceNodeIdentifier);
            }
            return null;
        }
    }

    public void setBuiltForDeletionPurpose(boolean builtForDeletionPurpose) {
        this.builtForDeletionPurpose = builtForDeletionPurpose;
    }

    public boolean isBuiltForDeletionPurpose() {
        return builtForDeletionPurpose;
    }

    public String getWorkflowHostID() {
        return workflowHostID;
    }

    public void setWorkflowHostID(String workflowHostID) {
        this.workflowHostID = workflowHostID;
    }

    public String getToolTip() {
        return toolTip;
    }

    public void setToolTip(String toolTip) {
        this.toolTip = toolTip;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @return true if all children are disabled
     */
    public boolean areAllChildrenDisabled() {
        if (children != null) {
            boolean disabled = children.size() > 0;
            for (DMBrowserNode bn : children) {
                disabled &= !bn.isEnabled();
            }
            return disabled;
        }
        return false;
    }

}
