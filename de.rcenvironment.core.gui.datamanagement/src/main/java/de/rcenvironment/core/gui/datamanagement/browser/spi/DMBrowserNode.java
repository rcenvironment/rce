/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.datamanagement.browser.spi;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.graphics.Image;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.datamanagement.commons.DataReference;
import de.rcenvironment.core.datamanagement.commons.MetaData;
import de.rcenvironment.core.datamanagement.commons.MetaDataKeys;
import de.rcenvironment.core.datamanagement.commons.MetaDataSet;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.MatrixTD;
import de.rcenvironment.core.datamodel.types.api.SmallTableTD;
import de.rcenvironment.core.datamodel.types.api.VectorTD;
import de.rcenvironment.core.gui.datamanagement.browser.Activator;

/**
 * @author Robert Mischke (based on DMObject class by Markus Litz)
 * @author Jan Flink
 * @author Doreen Seider
 * @author Marc Stammerjohann
 * @author Brigitte Boden
 * 
 */
public final class DMBrowserNode {

    private static final AtomicLong TEMP_FILE_SEQUENCE_NUMBER = new AtomicLong(0);

    private static final List<DMBrowserNode> IMMUTABLE_NO_CHILDREN_LIST = Collections.unmodifiableList(new ArrayList<DMBrowserNode>(0));

    private String title;

    private String toolTip;

    private String name;

    private String workflowID;

    private MetaDataSet metaData;

    private DMBrowserNode parent;

    private DMBrowserNodeType type;

    private List<DMBrowserNode> children = null;

    private final Object lockForChildrenAccess = new Object();

    private DataReference dataReference = null;

    private String dataReferenceId = null;

    private String fileContent = null;

    private String fileName = null;

    private String fileReferencePath = null;

    private String associatedFilename = null;

    private String workflowHostName = null;

    private LogicalNodeId workflowHostID = null;

    private Image icon = null;

    // if a workflow node is deleted and the subtree was not built before, it will be built prior deletion in order to fetch all related
    // data reference ids. That means, that child nodes, which doesn't have any data reference ids associated (childs included), must not be
    // built at all. For that reason, the underlying purpose can be stored within the node itself and can be requested
    private boolean builtForDeletionPurpose = false;

    private Boolean enabled;

    private String cachedPath;

    private DirectoryReferenceTD dirRefTD = null;

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
        if (cachedPath != null) {
            return cachedPath;
        }
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
                builder.append(getNodeIdentifier().getInstanceNodeIdString()); // assuming this should be unique enough here
                break;
            default:
                builder.append(getTitle());
            }
        } else {
            builder.append(getTitle());
        }
        cachedPath = builder.toString();
        return cachedPath;
    }

    public DMBrowserNode getParent() {
        return parent;
    }

    public DMBrowserNodeType getType() {
        return type;
    }

    public void setType(DMBrowserNodeType type) {
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
        synchronized (lockForChildrenAccess) {
            children = IMMUTABLE_NO_CHILDREN_LIST;
        }
    }

    /**
     * Returns true, iff the node is a leaf node.
     * 
     * @return true, iff the node is a leaf node
     */
    public Boolean isLeafNode() {
        synchronized (lockForChildrenAccess) {
            return areChildrenKnown() && children.isEmpty();
        }
    }

    /**
     * @return true if and only if the children of this node have already been computed
     */
    public boolean areChildrenKnown() {
        synchronized (lockForChildrenAccess) {
            return children != null;
        }
    }

    /**
     * Adds a new child to this node.
     * 
     * @param child the new child to add
     */
    public void addChild(DMBrowserNode child) {
        synchronized (lockForChildrenAccess) {
            ensureChildListCreated();
            if (children == IMMUTABLE_NO_CHILDREN_LIST) {
                throw new IllegalStateException("Parent node for addChild was marked as a leaf before");
            }
            children.add(child);
            child.setParent(this); // checks internally if the parent is already set
        }
    }

    /**
     * @return a read-only list of this nodes children
     */
    public List<DMBrowserNode> getChildren() {
        synchronized (lockForChildrenAccess) {
            ensureChildListCreated();
            return Collections.unmodifiableList(children);
        }
    }

    /**
     * Removes a child node.
     * 
     * @param child the child node to remove
     */
    public void removeChild(final DMBrowserNode child) {
        synchronized (lockForChildrenAccess) {
            if (children != null) {
                if (children.remove(child)) {
                    child.setParent(null);
                }
            }
        }
    }

    /**
     * Resets the children-state of this node to the initial state.
     */
    public void clearChildren() {
        synchronized (lockForChildrenAccess) {
            children = null;
        }
    }

    /**
     * @return this nodes children as an array.
     */
    public DMBrowserNode[] getChildrenAsArray() {
        synchronized (lockForChildrenAccess) {
            ensureChildListCreated();
            return children.toArray(new DMBrowserNode[0]);
        }
    }

    /**
     * Sorts the children of this node.
     * 
     * @param comparator the {@link Comparator} to use
     */
    public void sortChildren(Comparator<DMBrowserNode> comparator) {
        synchronized (lockForChildrenAccess) {
            ensureChildListCreated();
            Collections.sort(children, comparator);
        }
    }

    /**
     * @return the number of children this node has; also returns zero if the children have not been computed yet
     */
    public int getNumChildren() {
        synchronized (lockForChildrenAccess) {
            if (children == null) {
                return 0;
            } else {
                return children.size();
            }
        }
    }

    private void ensureChildListCreated() {
        synchronized (lockForChildrenAccess) {
            if (children == null) {
                children = new ArrayList<>();
            }
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

    public void setDirectoryReferenceTD(DirectoryReferenceTD directoryReferenceTD) {
        this.dirRefTD = directoryReferenceTD;
    }

    /**
     * @return <code>null</code> in case the {@link #getType()} returns another type than {@link DMBrowserNodeType.DMDirectoryReference}
     */
    public DirectoryReferenceTD getDirectoryReferenceTD() {
        return dirRefTD;
    }

    /**
     * TODO (p3) add JavaDoc (or eliminate, as it is deprecated).
     * 
     * @param parent as above
     */
    @Deprecated
    public void setParent(DMBrowserNode parent) {
        if (this.parent == parent) {
            // nothing to do
            return;
        }

        // remove child node from old parent node
        if (this.parent != null) {
            this.parent.removeChild(this);
        }

        // save as current parent node
        this.parent = parent;
        this.cachedPath = null; // invalidate path as it involves the parent's path
    }

    public String getDataReferenceId() {
        return dataReferenceId;
    }

    public void setDataReferenceId(String dataReferenceId) {
        this.dataReferenceId = dataReferenceId;
    }

    /**
     * @return path to file which should be opened in editor if {@link DMBrowserNode} is double-clicked. File will be created on-demand
     *         (lazy init).
     */
    public String getFileReferencePath() {
        if (fileReferencePath != null && !new File(fileReferencePath).exists()) {
            fileReferencePath = null;
        }
        if (fileReferencePath == null && fileContent != null) {
            writeTempFileForFileContent(fileName, fileContent);
        }
        return fileReferencePath;
    }

    private File createTempFileForFileContent(String filename) {
        File tempDir = new File(Activator.getInstance().getBundleSpecificTempDir(),
            String.valueOf(TEMP_FILE_SEQUENCE_NUMBER.incrementAndGet()));
        tempDir.mkdir();
        File endpointTempDir = new File(tempDir, "endpoints");
        endpointTempDir.mkdir();
        return new File(endpointTempDir, filename);
    }

    private void writeTempFileForFileContent(String filename, String text) {
        File tempFile = null;
        try {
            tempFile = createTempFileForFileContent(filename.trim());
            if (!tempFile.exists()) {
                FileUtils.write(tempFile, text);
            }
        } catch (IOException e) {
            LogFactory.getLog(getClass()).error("Failed to create temporary file for node content", e);
            return;
        }
        setAssociatedFilename(filename);
        setFileReferencePath(tempFile.getAbsolutePath());
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
     * {@link InstanceNodeSessionId} is only available, when node type equals {@link DMBrowserNodeType#Workflow}. Use
     * {@link #getNodeWithTypeWorkflow()} to access the the node identifier.
     * 
     * @return {@link InstanceNodeSessionId}
     */
    public ResolvableNodeId getNodeIdentifier() {
        if (dataReference != null && dataReference.getStorageNodeId() != null) {
            return dataReference.getStorageNodeId();
        } else {
            if (metaData != null) {
                String instanceNodeIdentifier = metaData.getValue(new MetaData(MetaDataKeys.NODE_IDENTIFIER, true, true));
                if (instanceNodeIdentifier != null) {
                    return NodeIdentifierUtils.parseArbitraryIdStringToLogicalNodeIdWithExceptionWrapping(instanceNodeIdentifier);
                }
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

    public LogicalNodeId getWorkflowControllerNode() {
        return workflowHostID;
    }

    public void setWorkflowControllerNode(LogicalNodeId logicalNodeId) {
        this.workflowHostID = logicalNodeId;
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
        synchronized (lockForChildrenAccess) {
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

    /**
     * Set the text content that should be opened in read-only editor on demand.
     * 
     * @param fContent text to open in read-only editor
     * @param fName file name shown in editor
     */
    public void setFileContentAndName(String fContent, String fName) {
        this.fileContent = fContent;
        this.fileName = fName;
    }

    /**
     * Sets {@link SmallTableTD} that should be opened in read-only editor on demand.
     * 
     * @param table {@link SmallTableTD} to open in read-only editor
     * @param fName file name shown in editor
     */
    public void setSmallTableTDAndFileName(SmallTableTD table, String fName) {
        this.fileName = fName;
        writeTempFileForMatrixTD(fName, table);
    }

    /**
     * Sets {@link MatrixTD} that should be opened in read-only editor on demand.
     * 
     * @param matrix {@link MatrixTD} to open in read-only editor
     * @param fName file name shown in editor
     */
    public void setMatrixTDAndFileName(MatrixTD matrix, String fName) {
        this.fileName = fName;
        writeTempFileForMatrixTD(fName, matrix);
    }

    /**
     * Sets {@link MatrixTD} that should be opened in read-only editor on demand.
     * 
     * @param vector {@link VectorTD} to open in read-only editor
     * @param fName file name shown in editor
     */
    public void setVectorTDAndFileName(VectorTD vector, String fName) {
        this.fileName = fName;
        writeTempFileForMatrixTD(fName, vector);
    }

    private void writeTempFileForMatrixTD(String filename, TypedDatum tableMatrixOrVector) {
        // changed @7.0.0: always do this, even if temp file creation fails
        setAssociatedFilename(filename);
        File tempFile = null;
        try {
            tempFile = createTempFileForFileContent(filename);
            if (tempFile != null && !tempFile.exists()) {
                try (FileWriter writer = new FileWriter(tempFile.getAbsoluteFile(), true)) {
                    int[] rowAndColumnCount = getRowAndColumnCount(tableMatrixOrVector);
                    for (int row = 0; row < rowAndColumnCount[0]; row++) {
                        for (int column = 0; column < rowAndColumnCount[1]; column++) {
                            TypedDatum entry = getEntry(row, column, tableMatrixOrVector);
                            if (entry != null) {
                                writer.append(getEntry(row, column, tableMatrixOrVector).toString());
                            } else {
                                writer.append(" ");
                            }
                            if (rowAndColumnCount[1] - column > 1) {
                                writer.append(", ");
                            }
                        }
                        writer.append("\r\n");
                    }
                }
            }
            if (tempFile != null) {
                setFileReferencePath(tempFile.getAbsolutePath());
            }
        } catch (IOException e) {
            LogFactory.getLog(getClass()).error("Failed to create temporary file for node content", e);
            return;
        }
    }

    private int[] getRowAndColumnCount(TypedDatum tableMatrixOrVector) {
        if (tableMatrixOrVector instanceof SmallTableTD) {
            return getRowAndColumnCount((SmallTableTD) tableMatrixOrVector);
        } else if (tableMatrixOrVector instanceof MatrixTD) {
            return getRowAndColumnCount((MatrixTD) tableMatrixOrVector);
        } else if (tableMatrixOrVector instanceof VectorTD) {
            return getRowAndColumnCount((VectorTD) tableMatrixOrVector);
        } else {
            return new int[] { 0, 0 };
        }
    }

    private int[] getRowAndColumnCount(SmallTableTD table) {
        return new int[] { table.getRowCount(), table.getColumnCount() };
    }

    private int[] getRowAndColumnCount(MatrixTD matrix) {
        return new int[] { matrix.getRowDimension(), matrix.getColumnDimension() };
    }

    private int[] getRowAndColumnCount(VectorTD vector) {
        return new int[] { 0, vector.getRowDimension() };
    }

    private TypedDatum getEntry(int rowIndex, int columnIndex, TypedDatum tableMatrixOrVector) {
        if (tableMatrixOrVector instanceof SmallTableTD) {
            return getEntry(rowIndex, columnIndex, (SmallTableTD) tableMatrixOrVector);
        } else if (tableMatrixOrVector instanceof MatrixTD) {
            return getEntry(rowIndex, columnIndex, (MatrixTD) tableMatrixOrVector);
        } else if (tableMatrixOrVector instanceof VectorTD) {
            return getEntry(rowIndex, columnIndex, (VectorTD) tableMatrixOrVector);
        } else {
            return null;
        }
    }

    private TypedDatum getEntry(int rowIndex, int columnIndex, SmallTableTD table) {
        return table.getTypedDatumOfCell(rowIndex, columnIndex);
    }

    private TypedDatum getEntry(int rowIndex, int columnIndex, MatrixTD matrix) {
        return matrix.getFloatTDOfElement(rowIndex, columnIndex);
    }

    private TypedDatum getEntry(int rowIndex, int columnIndex, VectorTD vector) {
        return vector.getFloatTDOfElement(rowIndex);
    }

}
