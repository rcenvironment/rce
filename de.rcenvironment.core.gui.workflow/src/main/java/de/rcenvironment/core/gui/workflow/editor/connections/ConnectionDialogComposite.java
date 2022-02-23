/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.connections;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.Location;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNodeIdentifier;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.datamodel.api.TypedDatumConverter;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.gui.workflow.EndpointContentProvider;
import de.rcenvironment.core.gui.workflow.EndpointContentProvider.Endpoint;
import de.rcenvironment.core.gui.workflow.EndpointHandlingHelper;
import de.rcenvironment.core.gui.workflow.EndpointLabelProvider;
import de.rcenvironment.core.gui.workflow.editor.commands.ConnectionAddCommand;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Common composite for ConnectionDialog and ConnectionView.
 * 
 * @author Oliver Seebach
 *
 */
public class ConnectionDialogComposite extends Composite {

    private static final Log LOGGER = LogFactory.getLog(ConnectionDialogComposite.class);

    private static final String SLASH = "/"; //$NON-NLS-1$

    private static final String SEMICOLON = ";"; //$NON-NLS-1$

    private static final String ENDPOINT_SEPARATOR = "###";

    private String selectionData;

    private EndpointTreeViewer sourceTreeViewer;

    private EndpointTreeViewer targetTreeViewer;

    private Tree sourceTree;

    private Tree targetTree;

    private ConnectionCanvas canvas;

    private ComponentViewerFilter sourceFilter;

    private ComponentViewerFilter targetFilter;

    private Text sourceFilterText;

    private Text targetFilterText;

    private WorkflowDescription workflowDescription;

    private Group targetGroup;

    private Group sourceGroup;

    private String sourceFilterString = "";

    private String targetFilterString = "";

    private FilterMode defaultFilterMode = FilterMode.ISEXACTLY;

    private FilterMode sourceFilterMode = defaultFilterMode;

    private FilterMode targetFilterMode = defaultFilterMode;

    private boolean wasDoubleClicked;

    private Cursor targetTreeDefaultCursor;

    private Cursor crossCursor = new Cursor(Display.getCurrent(), SWT.CURSOR_CROSS);

    private TypedDatumConverter datumConverter;

    private CommandStack editorsCommandStack;

    private boolean initializedSection;

    /**
     * Filter modes.
     * 
     * @author Oliver Seebach
     */
    public enum FilterMode {
        /** Node name contains filter string. */
        CONTAINS,
        /** Node name starts with filter string. */
        STARTSWITH, /**
         * /** Node name is exactly filter string.
         */
        ISEXACTLY,
        /** Node name is exactly either source or target filter string. */
        DOUBLECLICK
    }

    public ConnectionDialogComposite(Composite parent, int style) {
        super(parent, style);

        // Source Group
        sourceGroup = new Group(this, SWT.NONE);
        sourceGroup.setText(Messages.source);
        GridData gridDataSourceGroup = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gridDataSourceGroup.widthHint = 1;
        sourceGroup.setLayoutData(gridDataSourceGroup);
        GridLayout gridLayoutSourceGroup = new GridLayout(1, false);
        gridLayoutSourceGroup.marginTop = 5;
        gridLayoutSourceGroup.marginWidth = 0;
        gridLayoutSourceGroup.verticalSpacing = 0;
        gridLayoutSourceGroup.marginHeight = 0;
        gridLayoutSourceGroup.horizontalSpacing = 0;

        sourceGroup.setLayout(gridLayoutSourceGroup);
        sourceTreeViewer = new EndpointTreeViewer(sourceGroup, SWT.NONE);
        sourceTree = sourceTreeViewer.getTree();
        sourceTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        sourceTree.setLinesVisible(true);
        sourceFilter = new ComponentViewerFilter();
        sourceTreeViewer.addFilter(sourceFilter);

        // Connection Group
        Group connectionGroup = new Group(this, SWT.NONE);
        connectionGroup.setText(Messages.connections);
        GridData gridDataConnectionGroup = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gridDataConnectionGroup.widthHint = 1;
        connectionGroup.setLayoutData(gridDataConnectionGroup);
        GridLayout gridLayoutConnectionGroup = new GridLayout(1, false);
        gridLayoutConnectionGroup.marginTop = 5;
        gridLayoutConnectionGroup.verticalSpacing = 0;
        gridLayoutConnectionGroup.marginWidth = 0;
        gridLayoutConnectionGroup.marginHeight = 0;
        gridLayoutConnectionGroup.horizontalSpacing = 0;
        connectionGroup.setLayout(gridLayoutConnectionGroup);

        canvas = new ConnectionCanvas(connectionGroup, SWT.NONE);
        canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        canvas.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

        // Target Group
        targetGroup = new Group(this, SWT.NONE);
        targetGroup.setText(Messages.target);
        GridData gridDataTargetGroup = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gridDataTargetGroup.widthHint = 1;
        targetGroup.setLayoutData(gridDataTargetGroup);

        GridLayout gridLayoutTargetGroup = new GridLayout(1, false);
        gridLayoutTargetGroup.marginTop = 5;
        gridLayoutTargetGroup.verticalSpacing = 0;
        gridLayoutTargetGroup.marginWidth = 0;
        gridLayoutTargetGroup.marginHeight = 0;
        gridLayoutTargetGroup.horizontalSpacing = 0;
        targetGroup.setLayout(gridLayoutTargetGroup);
        targetTreeViewer = new EndpointTreeViewer(targetGroup, SWT.NONE);
        targetTree = targetTreeViewer.getTree();
        targetTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        targetTree.setLinesVisible(true);
        targetFilter = new ComponentViewerFilter();
        targetTreeViewer.addFilter(targetFilter);
    }

    public void setWasDoubleClicked(boolean wasDoubleClicked) {
        this.wasDoubleClicked = wasDoubleClicked;
    }

    public void setSourceFilterMode(FilterMode sourceFilterMode) {
        this.sourceFilterMode = sourceFilterMode;
    }

    public void setTargetFilterMode(FilterMode targetFilterMode) {
        this.targetFilterMode = targetFilterMode;
    }

    public Group getTargetGroup() {
        return targetGroup;
    }

    public Group getSourceGroup() {
        return sourceGroup;
    }

    /**
     * Applies the target filter and refreshes both trees.
     */
    public void applyTargetFilter() {
        targetFilter.setExact(false);
        targetFilter.setFilterMode(targetFilterMode);
        targetFilter.setFilterString(targetFilterString);
        if (wasDoubleClicked) {
            targetFilter.setFilterMode(FilterMode.DOUBLECLICK);
            targetFilter.setFilterString(targetFilterString);
        }
        sourceTreeViewer.refresh();
        canvas.redraw();
        targetTreeViewer.refresh();
        targetTreeViewer.expandAll();
        sourceTreeViewer.expandAll();
    }

    /**
     * Applies the source filter and refreshes both trees.
     */
    public void applySourceFilter() {
        sourceFilter.setExact(false);
        sourceFilter.setFilterMode(sourceFilterMode);
        sourceFilter.setFilterString(sourceFilterString);
        if (wasDoubleClicked) {
            sourceFilter.setFilterMode(FilterMode.DOUBLECLICK);
            sourceFilter.setFilterString(sourceFilterString);
        }
        sourceTreeViewer.refresh();
        canvas.redraw();
        targetTreeViewer.refresh();
        targetTreeViewer.expandAll();
        sourceTreeViewer.expandAll();
    }

    public void setSourceFilterString(String sourceFilterString) {
        this.sourceFilterString = sourceFilterString;
    }

    public void setTargetFilterString(String targetFilterString) {
        this.targetFilterString = targetFilterString;
    }

    public Text getSourceFilterText() {
        return sourceFilterText;
    }

    public Text getTargetFilterText() {
        return targetFilterText;
    }

    public void setWorkflowDescription(WorkflowDescription workflowDescription) {
        this.workflowDescription = workflowDescription;
    }

    /**
     * Marks whether the section has just been initialized.
     */
    public void markSectionAsInitialized() {
        initializedSection = true;
    }

    private void selectOutputSource() {
        Object selectedElement = ((ITreeSelection) sourceTreeViewer.getSelection()).getFirstElement();
        selectionData = null;
        if (selectedElement instanceof Endpoint) {
            selectionData = getDataString((Endpoint) selectedElement);
        } else if (selectedElement instanceof WorkflowNode) {
            selectionData = getDataString((WorkflowNode) selectedElement);
        }
    }

    private static String getDataString(WorkflowNodeIdentifier nodeId, String endpointName) {
        return nodeId.toString() + SLASH + endpointName;
    }

    private static String getDataString(Endpoint endpoint) {
        return getDataString(endpoint.getWorkflowNode().getIdentifierAsObject(), endpoint.getEndpointDescription().getName());
    }

    private static String getDataString(WorkflowNode workflowNode) {
        StringBuffer data = new StringBuffer();
        for (EndpointDescription endpointDesc : workflowNode.getComponentDescription().getOutputDescriptionsManager()
            .getEndpointDescriptions()) {
            data.append(getDataString(workflowNode.getIdentifierAsObject(), endpointDesc.getName()) + SEMICOLON);
        }
        return new String(data);
    }

    private boolean performEndpointDrop(String sourceNodeId, String sourceEndpointName, Object targetObject) {
        WorkflowNode sourceNode = null;
        for (final WorkflowNode node : workflowDescription.getWorkflowNodes()) {
            if (node.getIdentifierAsObject().toString().equals(sourceNodeId)) {
                sourceNode = node;
                break;
            }
        }

        // if no node with the respective id was found, perform no drop and log warning
        if (sourceNode == null) {
            LOGGER.warn("Connection could not be created because workflow node with id " + sourceNodeId + " was not found.");
            return false;
        }
        // if the section has been initialized and no source node is selected, perform no drop
        if (initializedSection) {
            return false;
        }

        Endpoint targetEndpoint = null;
        Object currentTarget = targetObject;
        if (currentTarget instanceof Endpoint) {
            targetEndpoint = (Endpoint) currentTarget;
        } else if (currentTarget instanceof WorkflowNode) {
            targetEndpoint = findEndpoint((WorkflowNode) currentTarget, sourceEndpointName);
        }

        // if no matching target endpoint was found
        if (targetEndpoint == null) {
            selectionData = "";
            return false;
        }

        EndpointDescriptionsManager endpointManager = sourceNode.getComponentDescription().getOutputDescriptionsManager();
        DataType sourceDataType = endpointManager.getEndpointDescription(sourceEndpointName).getDataType();
        if (sourceDataType != targetEndpoint.getEndpointDescription().getDataType()
            && !datumConverter.isConvertibleTo(sourceDataType, targetEndpoint.getEndpointDescription().getDataType())) {
            MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.error, StringUtils.format(Messages.incompatibleTypes,
                sourceDataType.getDisplayName(), targetEndpoint.getEndpointDescription().getDataType().getDisplayName()));
            return false;
        }

        // if connection is already connected, show warning and don't perform drop
        Connection connectedTo = null;
        for (Connection c : workflowDescription.getConnections()) {
            if (c.getTargetNode().equals(targetEndpoint.getWorkflowNode())
                && targetEndpoint.getEndpointDescription().getIdentifier().equals(c.getInput().getIdentifier())) {
                connectedTo = c;
                MessageDialog.openError(
                    Display.getCurrent().getActiveShell(),
                    Messages.error,
                    StringUtils.format(Messages.alreadyConnected, targetEndpoint.getName(),
                        connectedTo.getOutput().getName(), connectedTo.getSourceNode().getName()));
                return false;
            }
        }

        // check connections for already existent bendpoints between the two nodes
        List<Location> alreadyExistentBendpoints = new ArrayList<>();
        for (Connection connection : workflowDescription.getConnections()) {
            if ((connection.getSourceNode().getIdentifierAsObject().equals(sourceNode.getIdentifierAsObject())
                && connection.getTargetNode().getIdentifierAsObject().equals(targetEndpoint.getWorkflowNode().getIdentifierAsObject()))) {
                alreadyExistentBendpoints = connection.getBendpoints();
                break;
            } 
        }

        Connection connection = new Connection(sourceNode, endpointManager.getEndpointDescription(sourceEndpointName),
            targetEndpoint.getWorkflowNode(), targetEndpoint.getEndpointDescription(), alreadyExistentBendpoints);
        ConnectionAddCommand command = new ConnectionAddCommand(workflowDescription, connection);

        // in section, execute on editors command stack, otherwise without command stack.
        // TODO add command stack for connection dialog.
        if (editorsCommandStack != null) {
            editorsCommandStack.execute(command);
        } else {
            command.execute();
        }
        return true;

    }

    private Endpoint findEndpoint(WorkflowNode workflowNode, String name) {

        // if two endpoints have the same name, connect them
        for (Endpoint e : EndpointHandlingHelper.getEndpoints(workflowNode, EndpointType.INPUT)) {
            if (e.getEndpointDescription().getName().equals(name)) {
                return e;
            }
        }

        // if two component just have 1 endpoint each and the types match, connect them
        for (Endpoint e : EndpointHandlingHelper.getEndpoints(workflowNode, EndpointType.INPUT)) {
            int targetInputs = workflowNode.getInputDescriptionsManager().getEndpointDescriptions().size();
            WorkflowNode sourceNode = null;
            if (sourceTreeViewer.getSelection() instanceof TreeSelection) {
                TreeSelection selection = (TreeSelection) sourceTreeViewer.getSelection();
                if (selection.getFirstElement() instanceof WorkflowNode) {
                    sourceNode = ((WorkflowNode) selection.getFirstElement());
                }
            }

            if (sourceNode != null) {
                int sourceOutputs = sourceNode.getOutputDescriptionsManager().getEndpointDescriptions().size();
                if (targetInputs == 1 && sourceOutputs == 1) {
                    DataType targetInputDataType =
                        ((EndpointDescription) workflowNode.getInputDescriptionsManager().getEndpointDescriptions().toArray()[0])
                            .getDataType();
                    DataType sourceOutputDataType =
                        ((EndpointDescription) sourceNode.getOutputDescriptionsManager().getEndpointDescriptions().toArray()[0])
                            .getDataType();
                    if (targetInputDataType.equals(sourceOutputDataType)) {
                        return e;
                    }
                }
            }
        }
        return null;
    }

    private void expandSelectedSourceNode() {
        if (sourceTreeViewer.getSelection() instanceof TreeSelection) {
            TreeSelection selection = (TreeSelection) sourceTreeViewer.getSelection();
            if (selection.getFirstElement() instanceof WorkflowNode) {
                WorkflowNode node = (WorkflowNode) selection.getFirstElement();
                for (TreeItem item : sourceTree.getItems()) {
                    if (item.getText().equals(node.getName())) {
                        item.setExpanded(true);
                        sourceTreeViewer.refresh();
                    }
                }
            }
        }
    }

    private void expandTargetNode(Object target) {
        // Object target = getCurrentTarget();
        if (target instanceof WorkflowNode) {
            WorkflowNode node = (WorkflowNode) target;
            for (TreeItem item : targetTree.getItems()) {
                if (item.getText().equals(node.getName())) {
                    item.setExpanded(true);
                    targetTreeViewer.refresh();
                }
            }
        }
    }

    /**
     * Updates the connection trees and the canvas according to the new model.
     * 
     * @param newModel The new model for the connection trees and canvas.
     */
    public void updateConnectionViewer(WorkflowDescription newModel) {
        if (!sourceTree.isDisposed() && !targetTree.isDisposed() && !canvas.isDisposed()) {
            targetTreeViewer.refresh(true);
            sourceTreeViewer.refresh(true);
            canvas.repaint();
        }
    }

    /**
     * Initializes the common connection composite.
     * 
     * @param model The model to be represented in the composite.
     * @param sourceWorkflowNode The preselected source node.
     * @param targetWorkflowNode The preselected target node
     */
    public void initialize(WorkflowDescription model, WorkflowNode sourceWorkflowNode, WorkflowNode targetWorkflowNode) {

        workflowDescription = model;

        sourceTreeViewer.setLabelProvider(new EndpointLabelProvider(EndpointType.OUTPUT));
        targetTreeViewer.setLabelProvider(new EndpointLabelProvider(EndpointType.INPUT));

        sourceTreeViewer.setContentProvider(new EndpointContentProvider(EndpointType.OUTPUT));
        targetTreeViewer.setContentProvider(new EndpointContentProvider(EndpointType.INPUT));

        sourceTreeViewer.setInput(workflowDescription);
        targetTreeViewer.setInput(workflowDescription);

        if (sourceWorkflowNode != null) {
            sourceFilterString = sourceWorkflowNode.getName();
            sourceTreeViewer.expandToLevel(sourceWorkflowNode, 2);
            sourceTreeViewer.setSelection(new StructuredSelection(sourceWorkflowNode));

        }

        if (targetWorkflowNode != null) {
            targetFilterString = targetWorkflowNode.getName();
            targetTreeViewer.expandToLevel(targetWorkflowNode, 2);
            targetTreeViewer.setSelection(new StructuredSelection(targetWorkflowNode));

        }

        canvas.initialize(workflowDescription, sourceTreeViewer, targetTreeViewer);

        // Repaint
        sourceTree.addPaintListener(new PaintListener() {

            @Override
            public void paintControl(PaintEvent event) {
                canvas.repaint();
            }
        });
        targetTree.addPaintListener(new PaintListener() {

            @Override
            public void paintControl(PaintEvent event) {
                canvas.repaint();
            }
        });
        canvas.repaint();

        ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        TypedDatumService typedDatumService = serviceRegistryAccess.getService(TypedDatumService.class);
        datumConverter = typedDatumService.getConverter();

        // DND
        int operations = DND.DROP_COPY | DND.DROP_MOVE;
        Transfer[] transfer = new Transfer[] { TextTransfer.getInstance() };

        sourceTreeViewer.addDragSupport(operations, transfer, new OutputDragSourceListener());

        ViewerDropAdapter dropAdapter = new InputViewerDropAdapter();
        dropAdapter.setFeedbackEnabled(false);
        Transfer[] dropTransfer = new Transfer[] { TextTransfer.getInstance() };
        targetTreeViewer.addDropSupport(operations, dropTransfer, dropAdapter);

        targetTreeViewer.addSelectionChangedListener(new InputTargetSelectionListener());
        sourceTreeViewer.addSelectionChangedListener(new OutputSourceSelectionListener());

        // initialize old cursor
        targetTreeDefaultCursor = targetTree.getCursor();

        targetTree.addMouseMoveListener(new TargetTreeMouseMoveListener());

        // Per default, expand all
        sourceTreeViewer.expandAll();
        targetTreeViewer.expandAll();
    }

    public WorkflowDescription getWorkflowDescription() {
        return workflowDescription;
    }

    public ConnectionCanvas getCanvas() {
        return canvas;
    }

    public EndpointTreeViewer getSourceTreeViewer() {
        return sourceTreeViewer;
    }

    public EndpointTreeViewer getTargetTreeViewer() {
        return targetTreeViewer;
    }

    private void setTargetTreeCursorToDefault() {
        targetTree.setCursor(targetTreeDefaultCursor);
    }

    /**
     * Filter to remove unwanted components in connection dialog.
     * 
     * @author Sascha Zur
     * @author Oliver Seebach
     */
    public class ComponentViewerFilter extends ViewerFilter {

        private String filterString = "";

        private boolean exact = false;

        private FilterMode filterMode = defaultFilterMode;

        public ComponentViewerFilter() {}

        public void setFilterMode(FilterMode filterMode) {
            this.filterMode = filterMode;
        }

        @Override
        public boolean select(Viewer arg0, Object arg1, Object arg2) {
            if (arg2 instanceof WorkflowNode) {
                WorkflowNode item = ((WorkflowNode) arg2);

                // empty filter show all
                if (filterString.isEmpty()) {
                    return true;
                }

                if (filterMode.equals(FilterMode.CONTAINS)) {
                    if (!(item.getName().toLowerCase().contains(filterString.toLowerCase()))) {
                        return false;
                    }
                } else if (filterMode.equals(FilterMode.STARTSWITH)) {
                    if (!(item.getName().toLowerCase().startsWith(filterString.toLowerCase()))) {
                        return false;
                    }
                } else if (filterMode.equals(FilterMode.ISEXACTLY)) {
                    if (!(item.getName().toLowerCase().equals(filterString.toLowerCase()))) {
                        return false;
                    }
                } else if (filterMode.equals(FilterMode.DOUBLECLICK)) {
                    // double click used to behave differently when one connection line contained both directions
                    // for sake of readability this filter mode is still kept separately
                    if (!(item.getName().toLowerCase().equals(filterString.toLowerCase()))) {
                        return false;
                    }
                }
            }
            return true;
        }

        public String getFilterString() {
            return filterString;
        }

        public void setFilterString(String filterString) {
            this.filterString = filterString;
        }

        public boolean isExact() {
            return exact;
        }

        public void setExact(boolean exact) {
            this.exact = exact;
        }

        /**
         * Configures the component filter.
         * 
         * @param filter the text to be used as filter.
         * @param filterModus the filter mode to be applied.
         */
        public void configureFilter(String filter, FilterMode filterModus) {
            this.filterString = filter;
            this.filterMode = filterModus;
        }

    }

    /**
     * Reacts on changes in source tree.
     * 
     * @author Oliver Seebach
     */
    public class OutputSourceSelectionListener implements ISelectionChangedListener {

        @Override
        public void selectionChanged(SelectionChangedEvent e) {
            if (initializedSection) {
                initializedSection = false;
            } else {
                selectOutputSource();
            }

        }
    }

    /**
     * Reacts on changes in target tree and initiates connection pulling.
     * 
     * @author Oliver Seebach
     */
    public class InputTargetSelectionListener implements ISelectionChangedListener {

        @Override
        public void selectionChanged(SelectionChangedEvent event) {
            if (!event.getSelection().isEmpty() && selectionData != null) {
                Object currentTarget = ((ITreeSelection) event.getSelection()).getFirstElement();
                Object currentSource = ((ITreeSelection) sourceTreeViewer.getSelection()).getFirstElement();

                boolean selectionTypesEqual = false;

                if (currentSource != null) {

                    if (currentSource.getClass() == currentTarget.getClass()) {
                        selectionTypesEqual = true;
                    }

                    if (selectionTypesEqual && (currentTarget instanceof Endpoint || currentTarget instanceof WorkflowNode)) {
                        connectEqualTypes(currentTarget);

                    }

                }
                canvas.repaint();
                targetTreeViewer.refresh();
                sourceTreeViewer.refresh();
            }
        }

        // Tries to connect two endpoints with identical types
        private void connectEqualTypes(Object currentTarget) {

            for (String sourceString : selectionData.split(Pattern.quote(SEMICOLON))) {
                String[] splittedSourceString = sourceString.split(Pattern.quote(SLASH), 2);
                if (splittedSourceString.length > 1) {
                    if (performEndpointDrop(splittedSourceString[0], splittedSourceString[1], currentTarget)) {
                        setTargetTreeCursorToDefault();
                    }
                }
            }

        }
    }

    /**
     * Listener for handling dragging outputs.
     * 
     * @author Doreen Seider
     */
    public class OutputDragSourceListener implements DragSourceListener {

        @Override
        public void dragFinished(DragSourceEvent event) {}

        @Override
        public void dragSetData(DragSourceEvent event) {
            Object selectedElement = ((ITreeSelection) sourceTreeViewer.getSelection()).getFirstElement();
            if (selectedElement instanceof Endpoint) {
                selectionData = getDataString((Endpoint) selectedElement);
            } else if (selectedElement instanceof WorkflowNode) {
                selectionData = getDataString((WorkflowNode) selectedElement);
            }
            event.data = selectionData;
        }

        @Override
        public void dragStart(DragSourceEvent event) {
            Object item = ((ITreeSelection) sourceTreeViewer.getSelection()).getFirstElement();
            event.doit = item instanceof Endpoint || item instanceof WorkflowNode;
        }

    }

    /**
     * Handling dropping to inputs.
     * 
     * @author Doreen Seider
     */
    public class InputViewerDropAdapter extends ViewerDropAdapter {

        public InputViewerDropAdapter() {
            super(targetTreeViewer);
        }

        @Override
        public boolean performDrop(Object element) {
            boolean performed = false;
            boolean successful = false;
            Object currentTarget = getCurrentTarget();
            if (currentTarget instanceof Endpoint || currentTarget instanceof WorkflowNode) {

                for (String sourceString : ((String) element).split(Pattern.quote(SEMICOLON))) {
                    String[] splittedSourceString = sourceString.split(Pattern.quote(SLASH), 2);
                    successful = performEndpointDrop(splittedSourceString[0], splittedSourceString[1], currentTarget);
                }
                performed = true;
            }

            if (!successful) {
                expandTargetNode(getCurrentTarget());
                expandSelectedSourceNode();
            }

            canvas.repaint();
            targetTreeViewer.refresh();
            sourceTreeViewer.refresh();

            if (selectionData != null) {
                if (selectionData.isEmpty()) {
                    setTargetTreeCursorToDefault();
                }
            } else if (selectionData == null) {
                setTargetTreeCursorToDefault();
            }

            return performed;
        }

        @Override
        public boolean validateDrop(Object dest, int operation, TransferData transferType) {
            int endpoints = 0;

            if (dest instanceof WorkflowNode) {
                endpoints = ((WorkflowNode) dest).getComponentDescription().getInputDescriptionsManager().getEndpointDescriptions().size();
            }
            boolean selectionValid = false;
            if (selectionData != null) {
                if (!selectionData.isEmpty()) {
                    selectionValid = true;
                }
            }
            Object currentSource = ((ITreeSelection) sourceTreeViewer.getSelection()).getFirstElement();
            boolean selectionTypesEqual = false;
            if (currentSource != null && dest != null) {
                if (currentSource.getClass() == dest.getClass()) {
                    selectionTypesEqual = true;
                }
            }
            boolean valid = TextTransfer.getInstance().isSupportedType(transferType)
                && ((dest instanceof Endpoint || (dest instanceof WorkflowNode && endpoints > 0)))
                && selectionValid && selectionTypesEqual;
            return valid;
        }

    }

    public void setCommandStack(CommandStack editorsCommandStack2) {
        this.editorsCommandStack = editorsCommandStack2;
    }

    /**
     * Mouse movement listener that handles cursor.
     * 
     * @author Oliver Seebach
     * 
     */
    private final class TargetTreeMouseMoveListener implements MouseMoveListener {

        @Override
        public void mouseMove(MouseEvent event) {

            // if nothing is selected as source, no action required
            if (selectionData != null) {

                Object endpointClass = Endpoint.class;
                Object workflowNodeClass = WorkflowNode.class;
                Object currentSource = ((ITreeSelection) sourceTreeViewer.getSelection()).getFirstElement();
                Object targetType = null;

                boolean selectionTypesEqual = false;

                // find tree item the mouse is moving over
                String hoveredComponentName = "";
                String hoveredEndpointName = "";

                Point pt = new Point(event.x, event.y);
                // First level item = node; Second level item = endpoint
                for (TreeItem firstLevelItem : targetTree.getItems()) {
                    if (firstLevelItem.getBounds().contains(pt)) {
                        targetType = workflowNodeClass;
                        hoveredComponentName = firstLevelItem.getText();
                        break;
                    }
                    for (TreeItem secondLevelItem : firstLevelItem.getItems()) {
                        if (secondLevelItem.getBounds().contains(pt)) {
                            targetType = endpointClass;
                            hoveredEndpointName = secondLevelItem.getText();
                            hoveredComponentName = secondLevelItem.getParentItem().getText();
                            break;
                        }
                    }
                }

                if (currentSource != null && targetType != null) {
                    if (currentSource.getClass() == targetType) {
                        selectionTypesEqual = true;
                    }
                }

                // when mouse is not over endpoint or component item.
                if (hoveredComponentName.isEmpty() || hoveredEndpointName.isEmpty()) {
                    setTargetTreeCursorToDefault();
                }

                if (selectionTypesEqual) {
                    for (WorkflowNode node : workflowDescription.getWorkflowNodes()) {
                        if (node.getName().equals(hoveredComponentName) && (targetType == endpointClass)) {
                            EndpointDescription targetEndpoint =
                                node.getInputDescriptionsManager().getEndpointDescription(hoveredEndpointName);
                            String[] sourceCandidates = selectionData.split(SEMICOLON);
                            for (String candidate : sourceCandidates) {
                                checkCandidateForCursorSetting(targetEndpoint, candidate);
                            }
                        } else if (node.getName().equals(hoveredComponentName) && (targetType == workflowNodeClass)
                            && !initializedSection) {
                            targetTreeDefaultCursor = targetTree.getParent().getCursor();
                            targetTree.setCursor(crossCursor);
                            break;
                        }
                    }
                } else {
                    setTargetTreeCursorToDefault();
                }
            }
        }

        private void checkCandidateForCursorSetting(EndpointDescription targetEndpoint, String candidate) {
            boolean isConnected = targetEndpoint.isConnected();
            if (candidate.split(SLASH).length > 1) {
                String sourceComponent = candidate.split(SLASH)[0];
                String sourceEndpoint = candidate.split(SLASH)[1];
                DataType sourceDataType =
                    workflowDescription.getWorkflowNode(new WorkflowNodeIdentifier(sourceComponent)).getOutputDescriptionsManager()
                        .getEndpointDescription(sourceEndpoint).getDataType();
                DataType targetDataType = targetEndpoint.getDataType();
                boolean typeCompatible = (datumConverter.isConvertibleTo(sourceDataType, targetDataType))
                    || (sourceDataType.equals(targetDataType));
                if (!isConnected && typeCompatible && !initializedSection) {
                    targetTreeDefaultCursor = targetTree.getParent().getCursor();
                    targetTree.setCursor(crossCursor);
                } else {
                    setTargetTreeCursorToDefault();
                }
            }
        }
    }

}
