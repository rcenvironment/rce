/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.integration.workflowintegration.editor.pages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;

import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.gui.integration.common.editor.IntegrationEditorPage;
import de.rcenvironment.core.gui.integration.workflowintegration.editor.WorkflowIntegrationEditor;
import de.rcenvironment.core.gui.integration.workflowintegration.editor.mappingtreenodes.ComponentNode;
import de.rcenvironment.core.gui.integration.workflowintegration.editor.mappingtreenodes.MappingNode;
import de.rcenvironment.core.gui.resources.api.ColorManager;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardColors;
import de.rcenvironment.core.gui.resources.api.StandardImages;

/**
 * Mapping Page to define inputs, outputs and property keys of an integrated workflow.
 * 
 * @author Kathrin Schaffert
 * @author Jan Flink
 */
public class MappingPage extends IntegrationEditorPage {

    private static final int COLUMN4_WIDTH = 200;

    private static final int COLUMN3_WIDTH = 300;

    private static final int COLUMN2_WIDTH = 80;

    private static final int COLUMN1_WIDTH = 70;

    private static final int COLUMN0_WIDTH = 200;

    private static final String DEFAULT_MESSAGE =
        "Select inputs and outputs to serve as the inputs and outputs, respectively, for the integrated workflow component.\n"
            + "Note that the inputs with the constraint 'Required' are mandatory.";

    private static final String[] TABLE_TITLES = { "Component", "Type", "DataType", "Details", "Mapped Name" };

    private static final String HELP_CONTEXT_ID = "de.rcenvironment.core.gui.integration.workflowintegration.integration_mapping";

    private CheckboxTreeViewer treeViewer;

    private MappingTreeContentProvider contentProvider;

    private WorkflowIntegrationEditor integrationEditor;

    private Action resetExternalNameAction;

    private MenuManager treeViewerMenuManager;

    private Action useInternalNameAction;

    private Action checkNodeAction;

    private Action uncheckNodeAction;

    public MappingPage(WorkflowIntegrationEditor integrationEditor, CTabFolder container, MappingTreeContentProvider contentProvider) {
        super(integrationEditor, container, "Mapping");
        this.integrationEditor = integrationEditor;
        this.contentProvider = contentProvider;
    }

    @Override
    public void createContent(Composite container) {
        Composite tools = new Composite(container, SWT.None);
        tools.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END));
        GridLayout toolbarLayout = new GridLayout(2, false);
        toolbarLayout.marginWidth = 0;
        tools.setLayout(toolbarLayout);
        tools.setBackground(ColorManager.getInstance().getSharedColor(StandardColors.RCE_WHITE));
        ToolBar toolbar = new ToolBar(tools, SWT.None);
        toolbar.setBackground(ColorManager.getInstance().getSharedColor(StandardColors.RCE_WHITE));
        ToolItem expandAllButton = new ToolItem(toolbar, SWT.PUSH);
        expandAllButton.setImage(ImageManager.getInstance().getSharedImage(StandardImages.EXPAND_ALL));
        expandAllButton.addListener(SWT.Selection, event -> {
            treeViewer.getControl().setVisible(false);
            treeViewer.expandAll();
            treeViewer.getControl().setVisible(true);
        });
        ToolItem collapseAllButton = new ToolItem(toolbar, SWT.PUSH);
        collapseAllButton.setImage(ImageManager.getInstance().getSharedImage(StandardImages.COLLAPSE_ALL));
        collapseAllButton.addListener(SWT.Selection, event -> {
            treeViewer.getControl().setVisible(false);
            treeViewer.collapseAll();
            treeViewer.getControl().setVisible(true);
        });

        Tree tree = new Tree(container, SWT.BORDER | SWT.FULL_SELECTION | SWT.CHECK | SWT.MULTI);
        tree.setHeaderVisible(true);
        tree.setLinesVisible(true);

        treeViewer = new CheckboxTreeViewer(tree);
        treeViewer.setContentProvider(contentProvider);
        GridData viewerGridData = new GridData(GridData.FILL_BOTH);
        treeViewer.getControl().setLayoutData(viewerGridData);
        treeViewer.setInput(contentProvider.getRoot());
        treeViewer.setComparator(new MappingTreeComparator());
        createColumns();
        new MappingTreeFilterWidget(tools, treeViewer);

        treeViewer.setCheckStateProvider(new ICheckStateProvider() {

            @Override
            public boolean isGrayed(Object element) {
                if (element instanceof MappingNode) {
                    return !((MappingNode) element).isCheckable();
                }
                return false;
            }

            @Override
            public boolean isChecked(Object element) {
                if (element instanceof MappingNode) {
                    return ((MappingNode) element).isChecked();
                }
                if (element instanceof ComponentNode && ((ComponentNode) element).hasChildren()) {
                    return Arrays.stream(((ComponentNode) element).getChildren()).filter(MappingNode.class::isInstance)
                        .map(MappingNode.class::cast).allMatch(MappingNode::isChecked);
                }
                return false;
            }
        });
        addTreeListeners();
        treeViewer.refresh();
        setMessage(DEFAULT_MESSAGE);

        treeViewerMenuManager = hookContextMenu();
        createActions();
    }

    private void addTreeListeners() {
        treeViewer.addCheckStateListener(event -> {
            if (event.getElement() instanceof ComponentNode) {
                setSubtreeChecked((ComponentNode) event.getElement(), event.getChecked());
            }
            if (event.getElement() instanceof MappingNode) {
                MappingNode node = (MappingNode) event.getElement();
                if (node.isCheckable()) {
                    node.setChecked(event.getChecked());
                    validateMappedName(node);
                }
            }
            integrationEditor.updateDirty();
            treeViewer.refresh();
        });
        treeViewer.getControl().addListener(SWT.MouseDoubleClick, event -> {
            Point clickCoords = new Point(event.x, event.y);
            if (treeViewer.getCell(clickCoords) == null) {
                return;
            }
            TreeItem item = treeViewer.getTree().getItem(clickCoords);
            ViewerCell cell = treeViewer.getCell(clickCoords);
            if (cell.getViewerRow().getBounds().contains(clickCoords)
                && item.getData() instanceof ComponentNode) {
                treeViewer.setExpandedState(item.getData(), !treeViewer.getExpandedState(item.getData()));
            }
            if (item.getData() instanceof MappingNode) {
                MappingNode node = (MappingNode) item.getData();
                if (node.isCheckable()) {
                    node.setChecked(!node.isChecked());
                    validateMappedName(node);
                    integrationEditor.updateDirty();
                    treeViewer.refresh();
                    treeViewer.editElement(node, 4);
                }
            }
        });
        treeViewer.getTree().addListener(SWT.Selection, event -> {
            List<MappingNode> selectedMappingNodes = getSelectedMappingNodes();
            if (selectedMappingNodes.size() == 1 && selectedMappingNodes.iterator().next().isChecked()) {
                treeViewer.editElement(event.item.getData(), 4);
            }
        });
        treeViewer.getTree().addMenuDetectListener(e -> updateContextMenu());
        treeViewer.getTree().addListener(SWT.KeyDown, e -> {
            if (e.character == SWT.ESC) {
                treeViewer.setSelection(StructuredSelection.EMPTY);
            }
        });
    }

    private void updateContextMenu() {
        treeViewerMenuManager.removeAll();
        List<MappingNode> selectedMappingNodes = getSelectedMappingNodes();
        if (selectedMappingNodes.isEmpty()) {
            return;
        }
        boolean allNodesChecked = selectedMappingNodes.stream().allMatch(MappingNode::isChecked);
        boolean anyNodeUnchecked = selectedMappingNodes.stream().filter(MappingNode::isCheckable).anyMatch(node -> !node.isChecked());
        boolean anyNodeChecked = selectedMappingNodes.stream().filter(MappingNode::isCheckable).anyMatch(MappingNode::isChecked);
        useInternalNameAction.setEnabled(allNodesChecked
            && selectedMappingNodes.stream().anyMatch(n -> !n.getExternalName().equals(n.getInternalName())));
        resetExternalNameAction.setEnabled(allNodesChecked
            && selectedMappingNodes.stream().anyMatch(n -> !n.getExternalName().equals(n.getDefaultExternalName())));
        checkNodeAction.setEnabled(anyNodeUnchecked);
        uncheckNodeAction.setEnabled(anyNodeChecked);
        treeViewerMenuManager.add(useInternalNameAction);
        treeViewerMenuManager.add(resetExternalNameAction);
        treeViewerMenuManager.add(new Separator());
        treeViewerMenuManager.add(checkNodeAction);
        treeViewerMenuManager.add(uncheckNodeAction);
    }

    private List<MappingNode> getSelectedMappingNodes() {
        TreeItem[] selectedItems = treeViewer.getTree().getSelection();
        return Arrays.stream(selectedItems).map(TreeItem::getData)
            .filter(MappingNode.class::isInstance).map(MappingNode.class::cast).collect(Collectors.toList());
    }

    private MenuManager hookContextMenu() {
        MenuManager menuManager = new MenuManager();

        Menu treeContextMenu = menuManager.createContextMenu(treeViewer.getControl());
        treeViewer.getControl().setMenu(treeContextMenu);
        return menuManager;
    }

    private void createActions() {
        useInternalNameAction = createUseInternalNameAction();
        resetExternalNameAction = createResetExternalNameActions();
        checkNodeAction = createCheckAction();
        uncheckNodeAction = createUncheckAction();
    }

    private Action createResetExternalNameActions() {
        return new Action("Reset Mapped Name to Default") {

            @Override
            public void run() {
                getSelectedMappingNodes().stream().filter(MappingNode::isChecked).forEach(node -> {
                    node.setDefaultExternalName();
                    validateMappedName(node);
                });
                treeViewer.refresh();
            }
        };
    }

    private Action createUseInternalNameAction() {
        return new Action("Use Origin Name as Mapped Name") {

            @Override
            public void run() {
                getSelectedMappingNodes().stream().filter(MappingNode::isChecked).forEach(node -> {
                    node.setExternalName(node.getInternalName());
                    validateMappedName(node);
                });
                treeViewer.refresh();
            }
        };
    }

    private Action createCheckAction() {
        return new Action("Enable mapping", ImageManager.getInstance().getImageDescriptor(StandardImages.CHECK_CHECKED)) {

            @Override
            public void run() {
                getSelectedMappingNodes().stream().filter(MappingNode::isCheckable)
                    .filter(node -> !node.isChecked()).forEach(node -> {
                        node.setChecked(true);
                        validateMappedName(node);
                    });
                treeViewer.refresh();
            }
        };
    }

    private Action createUncheckAction() {
        return new Action("Disable mapping", ImageManager.getInstance().getImageDescriptor(StandardImages.CHECK_UNCHECKED)) {

            @Override
            public void run() {
                getSelectedMappingNodes().stream().filter(MappingNode::isCheckable)
                    .filter(MappingNode::isChecked).forEach(node -> {
                        node.setChecked(false);
                        validateMappedName(node);
                    });
                treeViewer.refresh();
            }
        };
    }

    public void refreshTree() {
        treeViewer.refresh();
    }

    private void validateMappedName(MappingNode node) {
        node.setNameValid(
            !node.isChecked()
                || !contentProvider.getMappedNamesOfOtherCheckedNodes(node).contains(node.getExternalName().trim().toLowerCase()));
        contentProvider.updateValidationOfOtherNodes(node);
        validateMappedNames();
    }

    private void validateMappedNames() {
        if (contentProvider.hasInvalidMappedNames()) {
            setPageValid(false);
            setMessage("At least one mapped name is not valid. Note that mapped names of the same type must be unique.",
                ImageManager.getInstance().getSharedImage(StandardImages.FAILED));
        } else {
            setPageValid(true);
            setMessage(DEFAULT_MESSAGE);
        }
        updateSaveButtonActivation();
    }

    private void setSubtreeChecked(ComponentNode componentNode, boolean checked) {
        if (!componentNode.hasChildren()) {
            return;
        }
        Arrays.stream(componentNode.getChildren()).filter(MappingNode.class::isInstance).map(MappingNode.class::cast)
            .forEach(subnode -> {
                if (subnode.isCheckable()) {
                    subnode.setChecked(checked);
                    validateMappedName(subnode);
                }
            });

    }

    private void createColumns() {
        TreeViewerColumn col0 = createTreeViewerColumn(TABLE_TITLES[0], COLUMN0_WIDTH);
        TreeViewerColumn col1 = createTreeViewerColumn(TABLE_TITLES[1], COLUMN1_WIDTH);
        TreeViewerColumn col2 = createTreeViewerColumn(TABLE_TITLES[2], COLUMN2_WIDTH);
        TreeViewerColumn col3 = createTreeViewerColumn(TABLE_TITLES[3], COLUMN3_WIDTH);
        TreeViewerColumn col4 = createTreeViewerColumn(TABLE_TITLES[4], COLUMN4_WIDTH);

        col0.setLabelProvider(new MappingTreeColumnLabelProvider(0));
        col1.setLabelProvider(new MappingTreeColumnLabelProvider(1));
        col2.setLabelProvider(new MappingTreeColumnLabelProvider(2));
        col3.setLabelProvider(new MappingTreeColumnLabelProvider(3));
        col4.setLabelProvider(new MappingTreeColumnLabelProvider(4));
        col4.setEditingSupport(new TextEditingSupport(treeViewer));
    }

    private TreeViewerColumn createTreeViewerColumn(String title, int width) {
        final TreeViewerColumn viewerColumn = new TreeViewerColumn(treeViewer, SWT.LEFT);
        final TreeColumn column = viewerColumn.getColumn();
        column.setText(title);
        column.setWidth(width);
        return viewerColumn;
    }

    private final class MappingTreeColumnLabelProvider extends ColumnLabelProvider {

        private int columnIndex;

        private MappingTreeColumnLabelProvider(int columnIndex) {
            super();
            this.columnIndex = columnIndex;
        }

        @Override
        public String getText(Object element) {
            if (element instanceof ComponentNode) {
                ComponentNode componentNode = (ComponentNode) element;
                if (columnIndex == 0) {
                    return componentNode.getComponentName();
                }
            }
            if (element instanceof MappingNode) {
                MappingNode exposedEndpointDefinition = (MappingNode) element;
                if (columnIndex == 0) {
                    return exposedEndpointDefinition.getInternalName();
                }
                if (columnIndex == 1) {
                    return exposedEndpointDefinition.getMappingType().toString();
                }
                if (columnIndex == 2) {
                    return exposedEndpointDefinition.getDataType().toString();
                }
                if (columnIndex == 3) {
                    return exposedEndpointDefinition.getDetails();
                }
                if (columnIndex == 4) {
                    return exposedEndpointDefinition.getExternalName();
                }
            }
            return "";
        }

        @Override
        public Color getForeground(Object element) {
            if (element instanceof MappingNode) {
                MappingNode node = (MappingNode) element;
                if (!node.isNameValid()) {
                    return PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_RED);
                }
                if (!node.isChecked()) {
                    return PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
                }
            }
            return super.getForeground(element);
        }

    }

    private class TextEditingSupport extends EditingSupport {

        private ValidatingTextCellEditor editor;

        TextEditingSupport(ColumnViewer viewer) {
            super(viewer);
            this.editor = new ValidatingTextCellEditor(treeViewer);
        }

        @Override
        protected boolean canEdit(Object object) {
            if (object instanceof MappingNode) {
                return ((MappingNode) object).isChecked();
            }
            return false;
        }

        @Override
        protected CellEditor getCellEditor(Object element) {
            if (element instanceof MappingNode) {
                return editor;
            }
            return null;
        }

        @Override
        protected Object getValue(Object arg0) {
            return ((MappingNode) arg0).getExternalName();
        }

        @Override
        protected void setValue(Object element, Object text) {
            if (element instanceof MappingNode && text instanceof String) {
                MappingNode node = (MappingNode) element;
                node.setExternalName(text.toString().trim());
                validateMappedName(node);
                integrationEditor.updateDirty();
                treeViewer.refresh();
            }
        }
    }

    private class ValidatingTextCellEditor extends TextCellEditor {

        ValidatingTextCellEditor(TreeViewer treeViewer) {
            super(treeViewer.getTree());
            setValidator(new MappingValidator(treeViewer));
        }

        @Override
        protected void valueChanged(boolean oldValidState, boolean newValidState) {
            if (!newValidState) {
                setMessage(getErrorMessage(), ImageManager.getInstance().getSharedImage(StandardImages.FAILED));
            } else {
                setMessage(DEFAULT_MESSAGE);
            }
            super.valueChanged(oldValidState, newValidState);
        }


        @Override
        protected void focusLost() {
            validateMappedNames();
            super.focusLost();
        }

    }

    @Override
    public void update() {
        treeViewer.getControl().setVisible(false);
        List<Object> expandedElements = new ArrayList<>(Arrays.asList(treeViewer.getExpandedElements()));
        List<TreeItem> oldItems = Arrays.asList(treeViewer.getTree().getItems());
        Map<ComponentNode, Boolean> oldNodes = new HashMap<>();
        oldItems.stream().map(TreeItem::getData).filter(ComponentNode.class::isInstance)
            .map(ComponentNode.class::cast).forEach(node -> oldNodes.put(node, hasCheckedChildren(node)));
        WorkflowDescription workflow = integrationEditor.getController().getWorkflowDescription().clone();
        contentProvider.updateContent(workflow);
        treeViewer.refresh();
        List<TreeItem> newItems = Arrays.asList(treeViewer.getTree().getItems());
        newItems.stream().map(TreeItem::getData).filter(ComponentNode.class::isInstance)
            .map(ComponentNode.class::cast)
            .filter(node -> (!oldNodes.keySet().contains(node) || !oldNodes.get(node)) && hasCheckedChildren(node))
            .forEach(expandedElements::add);
        treeViewer.setExpandedElements(expandedElements.toArray());
        treeViewer.getControl().setVisible(true);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(this.getControl(), HELP_CONTEXT_ID);
    }

    private boolean hasCheckedChildren(ComponentNode node) {
        return Arrays.stream(node.getChildren()).filter(MappingNode.class::isInstance).map(MappingNode.class::cast)
            .anyMatch(MappingNode::isChecked);
    }

    @Override
    public boolean hasChanges() {
        return contentProvider.hasChanges();
    }
}
