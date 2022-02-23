/*
 * Copyright 2021-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.palette.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.gef.commands.CommandStackEvent;
import org.eclipse.gef.commands.CommandStackEventListener;
import org.eclipse.gef.dnd.TemplateTransfer;
import org.eclipse.gef.tools.ConnectionCreationTool;
import org.eclipse.gef.tools.PanningSelectionTool;
import org.eclipse.gef.tools.SelectionTool;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeNode;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import de.rcenvironment.core.component.integration.ToolIntegrationContextRegistry;
import de.rcenvironment.core.component.model.impl.ToolIntegrationConstants;
import de.rcenvironment.core.gui.palette.PaletteViewConstants;
import de.rcenvironment.core.gui.palette.PaletteViewStorage;
import de.rcenvironment.core.gui.palette.view.dialogs.ManageCustomGroupsDialog;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.AccessibleComponentNode;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.CreationToolNode;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.GroupNode;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.PaletteTreeNode;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.workflow.editor.LabelFactory;
import de.rcenvironment.core.gui.workflow.editor.PaletteCreationTool;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditor;
import de.rcenvironment.core.gui.workflow.editor.commands.WorkflowNodeLabelConnectionCreateCommand;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;

/**
 * The RCE Palette view implementation.
 *
 * @author Kathrin Schaffert
 * @author Jan Flink
 * 
 */
public class PaletteView extends ViewPart implements IPartListener2, CommandStackEventListener, FocusListener, IContextProvider {

    private static final String WORKFLOW_LABEL_CONTEXT_ID = "de.rcenvironment.workflowLabelContext";

    private static final String PALETTE_CONTEXT_ID = "de.rcenvironment.paletteContext";

    private TreeViewer paletteTreeViewer;

    private PaletteViewContentProvider paletteViewContentProvider;

    private PaletteTreeViewerComparator paletteTreeViewerComparator;

    private PaletteViewActions paletteViewActions;

    private ManageCustomGroupsDialog organizeGroupsDialog = null;

    private CreationToolNode selectionToolNode;

    private CreationToolNode connectionToolNode;

    private boolean showEmptyGroupsChecked = true;

    private ToolIntegrationContextRegistry toolIntegrationRegistry;

    private PaletteViewStorage paletteViewStorage;

    public PaletteView() {

        this.paletteTreeViewerComparator = new PaletteTreeViewerComparator();
        toolIntegrationRegistry = ServiceRegistry.createAccessFor(this).getService(ToolIntegrationContextRegistry.class);

    }

    @Override
    public void createPartControl(Composite parent) {

        getSite().getPage().addPartListener(this);
        Composite mainComposite = new Composite(parent, SWT.NONE);
        mainComposite.setLayout(new GridLayout(1, false));

        IActionBars actionBars = getViewSite().getActionBars();
        IToolBarManager toolBarManager = actionBars.getToolBarManager();

        paletteTreeViewer = new TreeViewer(mainComposite, SWT.MULTI);
        GridData paletteTreeViewerGridData = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
        paletteTreeViewer.getTree().setLayoutData(paletteTreeViewerGridData);

        this.paletteViewContentProvider = new PaletteViewContentProvider(this);
        this.paletteViewActions = new PaletteViewActions(this);
        this.paletteViewStorage = new PaletteViewStorage(paletteViewContentProvider);

        paletteTreeViewer.setContentProvider(paletteViewContentProvider);
        ColumnViewerToolTipSupport.enableFor(paletteTreeViewer);
        paletteTreeViewer.setLabelProvider(new PaletteViewLabelProvider());
        paletteTreeViewer.setComparator(paletteTreeViewerComparator);
        paletteTreeViewer.addDoubleClickListener(createPaletteViewDoubleClickAdapter());
        paletteTreeViewer.getTree().addFocusListener(this);
        paletteTreeViewer.setAutoExpandLevel(1);

        paletteTreeViewer.getTree()
            .addMenuDetectListener(event -> paletteViewActions.updateContextMenu(paletteTreeViewer.getTree().getSelection()));
        paletteTreeViewer.getTree().addTreeListener(new PaletteViewTreeListener(paletteTreeViewer));
        paletteTreeViewer.getTree().addSelectionListener(createPaletteViewSelectionListener());
        
        DragSource source = new DragSource(paletteTreeViewer.getTree(), DND.DROP_COPY);
        source.setTransfer(TemplateTransfer.getInstance());
        source.addDragListener(new PaletteViewDragSourceListener());

        PaletteTreeNode root = PaletteTreeNode.createRootNode(paletteViewContentProvider);
        paletteViewContentProvider.setRootNode(root);
        root.setChildren(createEditorToolNodes(root));
        paletteTreeViewer.setInput(root);
        getSite().setSelectionProvider(paletteTreeViewer);

        List<String> expandedGroupList = paletteViewStorage.loadFilesFromStorage();
        getContentProvider().updateTree(getContentProvider().getCurrentToolInstallations(), null);

        initExpandedStates(expandedGroupList);

        paletteViewActions.createActions();
        paletteViewActions.addToolbarItems(toolBarManager);
        paletteViewActions.hookContextMenu();

        Optional<WorkflowEditor> editor = getWorkflowEditor();
        if (editor.isPresent()) {
            editor.get().getEditorsCommandStack().addCommandStackEventListener(this);
        }
        selectSelectionToolNode();
    }

    private PaletteTreeNode[] createEditorToolNodes(PaletteTreeNode root) {
        PanningSelectionTool selectionTool = new PanningSelectionTool();
        Optional<WorkflowEditor> editor = getWorkflowEditor();
        if (editor.isPresent()) {
            editor.get().getViewer().getEditDomain().setDefaultTool(selectionTool);
        }
        selectionToolNode = PaletteTreeNode.createBasicToolNode(root, PaletteViewConstants.SELECT, selectionTool,
            ImageManager.getInstance().getSharedImage(StandardImages.ARROW));
        selectionToolNode.setShortKey("ALT+S");
        connectionToolNode =
            PaletteTreeNode.createBasicToolNode(root, PaletteViewConstants.DRAW_CONNECTION, new ConnectionCreationTool(),
                ImageManager.getInstance().getSharedImage(StandardImages.CONNECTION_TOOL));
        connectionToolNode.setShortKey("ALT+D");
        PaletteCreationTool labelTool = new PaletteCreationTool(new LabelFactory());
        CreationToolNode labelNode =
            PaletteTreeNode.createBasicToolNode(root, PaletteViewConstants.ADD_LABEL, labelTool,
                ImageManager.getInstance().getSharedImage(StandardImages.WORKFLOW_LABEL));
        labelNode.setHelpContextID(WORKFLOW_LABEL_CONTEXT_ID);
        PaletteTreeNode[] nodeList = new PaletteTreeNode[3];
        nodeList[0] = selectionToolNode;
        nodeList[1] = connectionToolNode;
        nodeList[2] = labelNode;
        return nodeList;
    }

    public void selectSelectionToolNode() {
        paletteTreeViewer.setSelection(new StructuredSelection(selectionToolNode));
        Optional<WorkflowEditor> editor = getWorkflowEditor();
        if (editor.isPresent()) {
            editor.get().getViewer().getEditDomain().setActiveTool(selectionToolNode.getTool());
        }
        setHelp(null);
    }

    public void selectConnectionToolNode() {
        paletteTreeViewer.setSelection(new StructuredSelection(connectionToolNode));
        Optional<WorkflowEditor> editor = getWorkflowEditor();
        if (editor.isPresent()) {
            editor.get().getViewer().getEditDomain().setActiveTool(connectionToolNode.getTool());
        }
        setHelp(null);
    }

    @Override
    public void dispose() {
        getSite().getPage().removePartListener(this);
        paletteViewContentProvider.dispose();
        super.dispose();
    }

    private void initExpandedStates(List<String> updatedExpandedGroups) {
        paletteTreeViewer.refresh();
        paletteTreeViewer.getTree().setVisible(false);
        checkAndExpandItems(getTreeNodes(), updatedExpandedGroups);
        paletteTreeViewer.getTree().setVisible(true);
    }

    private TreeNode[] getTreeNodes() {
        return Arrays.stream(paletteTreeViewer.getTree().getItems()).map(TreeItem::getData).filter(TreeNode.class::isInstance)
            .map(TreeNode.class::cast).toArray(TreeNode[]::new);
    }

    private void checkAndExpandItems(TreeNode[] treeNodes, List<String> updatedExpandedGroups) {
        Arrays.stream(treeNodes).filter(GroupNode.class::isInstance).map(GroupNode.class::cast).forEach(node -> {
            setExpandedState(node, updatedExpandedGroups.contains(node.getQualifiedGroupName()));
            if (node.hasChildren()) {
                checkAndExpandItems(node.getChildren(), updatedExpandedGroups);
            }
        });
    }

    private IDoubleClickListener createPaletteViewDoubleClickAdapter() {
        return (DoubleClickEvent e) -> {
            IStructuredSelection selection = (IStructuredSelection) e.getSelection();
            if (selection.isEmpty() || ((IStructuredSelection) paletteTreeViewer.getSelection()).size() > 1) {
                return;
            }

            PaletteTreeNode node = (PaletteTreeNode) selection.getFirstElement();
            node.handleDoubleclick(this);
        };
    }

    public Optional<WorkflowEditor> getWorkflowEditor() {
        try {
            IWorkbenchPart part;
            part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
            if (part instanceof WorkflowEditor) {
                return Optional.of((WorkflowEditor) part);
            }
        } catch (NullPointerException e) {
            // A NullPointerException might occur if the e.g. during startup no window, page or editor is active.
            return Optional.empty();
        }
        return Optional.empty();

    }

    private SelectionListener createPaletteViewSelectionListener() {
        return new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                TreeItem[] selection = paletteTreeViewer.getTree().getSelection();
                if (selection.length == 0) {
                    selectSelectionToolNode();
                    return;
                }
                List<PaletteTreeNode> selectedNodes = Stream.of(selection).map(TreeItem::getData).filter(PaletteTreeNode.class::isInstance)
                    .map(PaletteTreeNode.class::cast).collect(Collectors.toList());

                if (selectedNodes.size() > 1) {
                    List<AccessibleComponentNode> newSelection =
                        selectedNodes.stream().filter(AccessibleComponentNode.class::isInstance)
                            .map(AccessibleComponentNode.class::cast)
                            .collect(Collectors.toCollection(ArrayList<AccessibleComponentNode>::new));
                    if (newSelection.isEmpty()) {
                        paletteTreeViewer.setSelection(new StructuredSelection(e.item.getData()));
                    } else {
                        paletteTreeViewer.setSelection(new StructuredSelection(newSelection));
                    }
                }

                Optional<WorkflowEditor> optional = getWorkflowEditor();

                if (selectedNodes.size() == 1) {
                    setHelp(selectedNodes.get(0).getHelpContextID().orElse(getPaletteHelpID()));
                }

                if (!optional.isPresent()) {
                    return;
                }
                WorkflowEditor editor = optional.get();
                if (selectedNodes.size() == 1) {
                    selectedNodes.get(0).handleWidgetSelected(editor);
                } else {
                    editor.getViewer().getEditDomain().loadDefaultTool();
                }
            }
        };

    }

    private String getPaletteHelpID() {
        return PALETTE_CONTEXT_ID;
    }

    private void setHelp(String helpID) {
        if (!getPaletteTreeViewer().getControl().isDisposed()) {
            PlatformUI.getWorkbench().getHelpSystem().setHelp(getPaletteTreeViewer().getControl(), helpID);
        }
    }

    private class PaletteViewDragSourceListener implements DragSourceListener {

        @Override
        public void dragFinished(DragSourceEvent evt) {}

        @Override
        public void dragSetData(DragSourceEvent evt) {
            ITreeSelection selection = paletteTreeViewer.getStructuredSelection();
            PaletteTreeNode node = (PaletteTreeNode) selection.getFirstElement();
            if (node.isAccessibleComponent()) {
                PaletteCreationTool tool = node.getAccessibleComponentNode().getTool();
                evt.data = tool.getFactory();
            }
            if (node.isCreationTool()) {
                CreationToolNode etNode = node.getCreationToolNode();
                if (etNode.getTool() instanceof PaletteCreationTool) {
                    evt.data = ((PaletteCreationTool) etNode.getTool()).getFactory();
                }
            }
        }

        @Override
        public void dragStart(DragSourceEvent evt) {
            ITreeSelection selection = paletteTreeViewer.getStructuredSelection();
            PaletteTreeNode node = (PaletteTreeNode) selection.getFirstElement();
            if (selection.size() == 1 && (node.isAccessibleComponent() || node.isCreationTool())) {
                evt.image = null;
                if (node.isCreationTool()) {
                    evt.doit = node.getCreationToolNode().getTool() instanceof PaletteCreationTool;
                } else {
                    evt.doit = true;
                }
                return;
            }
            evt.doit = false;
        }

    }

    public List<String> getExpandedGroupNameList() {
        return Stream.of(paletteTreeViewer.getExpandedElements()).filter(GroupNode.class::isInstance)
            .map(GroupNode.class::cast)
            .filter(GroupNode::isExpanded)
            .map(GroupNode::getQualifiedGroupName).collect(Collectors.toList());
    }

    public TreeViewer getPaletteTreeViewer() {
        return paletteTreeViewer;
    }

    protected PaletteTreeViewerComparator getPaletteTreeViewerComparator() {
        return paletteTreeViewerComparator;
    }

    protected ManageCustomGroupsDialog getOrganizeGroupsDialog() {
        return organizeGroupsDialog;
    }

    public void setOrganizeGroupsDialog(ManageCustomGroupsDialog organizeGroupsDialog) {
        this.organizeGroupsDialog = organizeGroupsDialog;
    }

    public void setFocus() {
        getPaletteTreeViewer().getControl().setFocus();
    }

    protected void collapseAll() {
        paletteTreeViewer.getTree().setVisible(false);
        if (Arrays.stream(paletteTreeViewer.getTree().getSelection()).map(TreeItem::getData)
            .anyMatch(AccessibleComponentNode.class::isInstance)) {
            selectSelectionToolNode();
        }
        paletteViewContentProvider.setExpandedState(getTreeNodes(), false);
        paletteTreeViewer.getTree().setVisible(true);
    }

    protected void expandAll() {
        paletteTreeViewer.getTree().setVisible(false);
        paletteViewContentProvider.setExpandedState(getTreeNodes(), true);
        paletteTreeViewer.getTree().setVisible(true);
    }

    public void expandToNode(PaletteTreeNode node) {
        List<PaletteTreeNode> nodesToExpand = new ArrayList<>();
        if (node.isGroup()) {
            nodesToExpand.add(node);
        }
        paletteViewContentProvider.addAllParentGroupNodes(node, nodesToExpand);
        nodesToExpand.stream().filter(PaletteTreeNode::isGroup).map(PaletteTreeNode::getGroupNode).forEach(n -> {
            setExpandedState(n, true);
        });
        paletteTreeViewer.refresh();
    }

    public void setExpandedState(GroupNode node, boolean expanded) {
        if (paletteViewContentProvider.isExpandable(node)) {
            paletteViewContentProvider.setExpandedState(node, expanded);
        }
    }

    @Override
    public void partActivated(IWorkbenchPartReference arg0) {
        // Intentionally left empty
    }

    @Override
    public void partBroughtToTop(IWorkbenchPartReference arg0) {
        if (arg0.getPart(false) instanceof WorkflowEditor) {
            WorkflowEditor editor = (WorkflowEditor) arg0.getPart(false);
            ITreeSelection selection = paletteTreeViewer.getStructuredSelection();
            if (selection.size() == 1) {
                Object element = selection.getFirstElement();
                if (element instanceof CreationToolNode) {
                    editor.getViewer().getEditDomain().setActiveTool(((CreationToolNode) element).getTool());
                }
            }
            editor.getEditorsCommandStack().addCommandStackEventListener(this);
        }
    }

    @Override
    public void partClosed(IWorkbenchPartReference arg0) {
        if (arg0.getPart(false).equals(this)) {
            paletteViewStorage.storeConfigurationFiles();
            selectSelectionToolNode();
        }
    }

    @Override
    public void partDeactivated(IWorkbenchPartReference arg0) {
        // Intentionally left empty
    }

    @Override
    public void partHidden(IWorkbenchPartReference arg0) {
        if (arg0.getPart(false) instanceof WorkflowEditor) {
            WorkflowEditor editor = (WorkflowEditor) arg0.getPart(false);
            editor.getViewer().getEditDomain().setActiveTool(selectionToolNode.getTool());
            editor.getEditorsCommandStack().removeCommandStackEventListener(this);
        }
    }

    @Override
    public void partInputChanged(IWorkbenchPartReference arg0) {
        // Intentionally left empty
    }

    @Override
    public void partOpened(IWorkbenchPartReference arg0) {
        // Intentionally left empty
    }

    @Override
    public void partVisible(IWorkbenchPartReference arg0) {
        // Intentionally left empty
    }

    public boolean isShowEmptyGroups() {
        return showEmptyGroupsChecked;
    }

    public void setShowEmptyGroups(boolean show) {
        this.showEmptyGroupsChecked = show;
    }

    @Override
    public void stackChanged(CommandStackEvent evt) {
        // If a new workflow component was added to the workflow, then activate the selection tool.
        if (evt.getDetail() == 1 && evt.getCommand() instanceof WorkflowNodeLabelConnectionCreateCommand) {
            selectSelectionToolNode();
        }
    }

    @Override
    public void focusGained(FocusEvent arg0) {
        // Intentionally left empty
    }

    @Override
    public void focusLost(FocusEvent arg0) {
        Optional<WorkflowEditor> editor = getWorkflowEditor();
        if (editor.isPresent()
            && editor.get().getViewer().getEditDomain().getActiveTool() instanceof SelectionTool) {
            paletteTreeViewer.setSelection(new StructuredSelection(selectionToolNode));
        }
    }

    public PaletteViewContentProvider getContentProvider() {
        return paletteViewContentProvider;
    }

    @Override
    public IContext getContext(Object arg0) {
        ITreeSelection selection = getPaletteTreeViewer().getStructuredSelection();
        if (selection.isEmpty() || selection.size() != 1) {
            return null;
        }
        Object object = selection.getFirstElement();
        if (object instanceof PaletteTreeNode) {
            PaletteTreeNode node = (PaletteTreeNode) object;
            if (node.getHelpContextID().isPresent()) {
                String helpId = node.getHelpContextID().get();
                if (helpId.startsWith("de.rcenvironment.integration.workflow")) {
                    return getContextFromHelpSystem("de.rcenvironment.workflow");
                } else if (toolIntegrationRegistry.hasTIContextMatchingPrefix(helpId)) {
                    return getContextFromHelpSystem(ToolIntegrationConstants.CONTEXTUAL_HELP_PLACEHOLDER_ID);
                } else if (helpId.contains("de.rcenvironment.remoteaccess")) {
                    return getContextFromHelpSystem("de.rcenvironment.remoteaccess.*");
                } else {
                    return getContextFromHelpSystem(helpId);
                }
            }
            return getContextFromHelpSystem(getPaletteHelpID());
        }
        return getContextFromHelpSystem(getPaletteHelpID());
    }

    @Override
    public int getContextChangeMask() {
        return IContextProvider.SELECTION;
    }

    @Override
    public String getSearchExpression(Object arg0) {
        return null;
    }

    protected IContext getContextFromHelpSystem(final String contextId) {
        return HelpSystem.getContext(contextId);
    }

    protected Object getSelectionTool() {
        return selectionToolNode.getTool();
    }
}
