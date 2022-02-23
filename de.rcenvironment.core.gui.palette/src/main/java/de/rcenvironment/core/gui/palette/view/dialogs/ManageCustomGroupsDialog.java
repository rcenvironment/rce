/*
 * Copyright 2021-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.palette.view.dialogs;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import de.rcenvironment.core.component.model.api.ComponentImageManagerService;
import de.rcenvironment.core.component.model.impl.ComponentImageManagerImpl.ImagePackage;
import de.rcenvironment.core.gui.palette.PaletteViewConstants;
import de.rcenvironment.core.gui.palette.toolidentification.ToolType;
import de.rcenvironment.core.gui.palette.view.PaletteTreeViewerComparator;
import de.rcenvironment.core.gui.palette.view.PaletteView;
import de.rcenvironment.core.gui.palette.view.PaletteViewContentProvider;
import de.rcenvironment.core.gui.palette.view.PaletteViewTreeListener;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.ComponentNode;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.OfflineComponentNode;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.PaletteTreeNode;
import de.rcenvironment.core.gui.resources.api.ColorManager;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardColors;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;

/**
 * Dialog for removing tools, especially those who are currently not available in the network, from custom groups.
 * 
 * @author Kathrin Schaffert
 * @author Jan Flink
 *
 */
public class ManageCustomGroupsDialog extends TitleAreaDialog {

    private static final int COLUMN_2_WIDTH = 120;

    private static final int COLUMN_1_WIDTH = 80;

    private static final int COLUMN_0_WIDTH = 300;

    private static final String[] TITLES = { "Group Hierachy", "Location", "Currently Available" };

    private final Log log = LogFactory.getLog(getClass());

    private TreeViewer treeViewer;

    private PaletteView paletteView;

    private Button addSubgroupButton;

    private Button editButton;

    private Button deleteButton;

    private Button resetButton;

    private ComponentImageManagerService componentImageManager;

    private PaletteViewContentProvider contentProvider;

    public ManageCustomGroupsDialog(Shell parentShell, PaletteView paletteView) {
        super(parentShell);
        this.paletteView = paletteView;
        this.contentProvider = paletteView.getContentProvider();
        componentImageManager = ServiceRegistry.createAccessFor(this).getService(ComponentImageManagerService.class);
    }

    @Override
    public void create() {
        super.create();
        setTitle("Manage Custom Groups and Group Assignments");
        setMessage(
            "Add, edit or delete custom groups. Reset components to their default groups.");
    }
    
    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent) {

        Composite container = (Composite) super.createDialogArea(parent);

        Composite content = new Composite(container, SWT.NONE);
        content.setLayout(new GridLayout(2, false));
        GridData contentData = new GridData(GridData.FILL_BOTH);
        content.setLayoutData(contentData);

        Tree tree = new Tree(content, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        tree.setHeaderVisible(true);
        tree.setLinesVisible(true);
        treeViewer = new TreeViewer(tree);
        treeViewer.setContentProvider(new ManageGroupsContentProvider());
        treeViewer.setComparator(new PaletteTreeViewerComparator());
        treeViewer.addSelectionChangedListener(new TreeSelectionChangedListener());
        treeViewer.getTree().addTreeListener(new PaletteViewTreeListener(treeViewer));
        GridData viewerGridData = new GridData(GridData.FILL_BOTH);
        treeViewer.getControl().setLayoutData(viewerGridData);

        createColumns();

        treeViewer.setInput(contentProvider.getRootNode());
        treeViewer.expandAll();
        treeViewer.refresh();

        Composite buttonComposite = new Composite(content, SWT.NONE);
        buttonComposite.setLayout(new GridLayout(1, true));
        buttonComposite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

        GridData gridDataButtons = new GridData();

        Button addButton = new Button(buttonComposite, SWT.PUSH);
        addButton.setText("Add Group");
        addButton.addSelectionListener(new AddButtonSelectionListener());
        addButton.setLayoutData(gridDataButtons);

        addSubgroupButton = new Button(buttonComposite, SWT.PUSH);
        addSubgroupButton.setText("Add Subgroup");
        addSubgroupButton.addSelectionListener(new AddSubgroupButtonSelectionListener());
        addSubgroupButton.setLayoutData(gridDataButtons);
        addSubgroupButton.setEnabled(false);

        editButton = new Button(buttonComposite, SWT.PUSH);
        editButton.setText("Edit Group");
        editButton.setLayoutData(gridDataButtons);
        editButton.addSelectionListener(new EditButtonSelectionListener());
        editButton.setEnabled(false);

        deleteButton = new Button(buttonComposite, SWT.PUSH);
        deleteButton.setText("Delete Group");
        deleteButton.setLayoutData(gridDataButtons);
        deleteButton.addSelectionListener(new DeleteButtonSelectionListener());
        deleteButton.setEnabled(false);

        resetButton = new Button(buttonComposite, SWT.PUSH);
        resetButton.setText("Reset Component");
        resetButton.setLayoutData(gridDataButtons);
        resetButton.addSelectionListener(new ResetButtonSelectionListener());
        resetButton.setEnabled(false);

        setHelpAvailable(false);

        return container;
    }

    @Override
    protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
        if (id == CANCEL) {
            return null;
        }
        if (id == OK) {
            return super.createButton(parent, id, "Close", defaultButton);
        }
        return super.createButton(parent, id, label, defaultButton);
    }

    private void createColumns() {
        TreeViewerColumn column0 = createTreeViewerColumn(TITLES[0], SWT.LEFT);
        TreeViewerColumn column1 = createTreeViewerColumn(TITLES[1], SWT.CENTER);
        TreeViewerColumn column2 = createTreeViewerColumn(TITLES[2], SWT.CENTER);
        column0.setLabelProvider(new TableColumnLabelProvider(0));
        column1.setLabelProvider(new TableColumnLabelProvider(1));
        column2.setLabelProvider(new TableColumnLabelProvider(2));
        column0.getColumn().setWidth(COLUMN_0_WIDTH);
        column1.getColumn().setWidth(COLUMN_1_WIDTH);
        column2.getColumn().setWidth(COLUMN_2_WIDTH);
    }

    private TreeViewerColumn createTreeViewerColumn(String title, int type) {
        final TreeViewerColumn viewerColumn = new TreeViewerColumn(treeViewer, type);
        final TreeColumn column = viewerColumn.getColumn();
        column.setText(title);
        column.setResizable(false);
        return viewerColumn;
    }

    @Override
    protected void okPressed() {
        paletteView.setOrganizeGroupsDialog(null);
        super.okPressed();
    }

    private class TableColumnLabelProvider extends ColumnLabelProvider {

        private int columnIndex;

        TableColumnLabelProvider(int columnIndex) {
            this.columnIndex = columnIndex;
        }

        private Image getDefaultImage() {
            return ImageManager.getInstance().getSharedImage(StandardImages.RCE_LOGO_16);
        }

        @Override
        public Image getImage(Object element) {
            if (columnIndex == 0 && element instanceof PaletteTreeNode) {
                PaletteTreeNode node = (PaletteTreeNode) element;
                if (node.isOfflineComponent()) {
                    String componentID = node.getOfflineComponentNode().getToolIdentification().getToolID();
                    ImagePackage imagePackage = componentImageManager.getImagePackage(componentID);
                    return imagePackage.getIcon16(componentID);
                }
                if (node.isGroup()) {
                    return node.getGroupNode().getIcon(treeViewer.getExpandedState(node) && node.hasChildren());
                }
                return node.getIcon().orElseGet(this::getDefaultImage);
            }
            return super.getImage(element);
        }


        @Override
        public Color getForeground(Object element) {
            if (element instanceof OfflineComponentNode) {
                return ColorManager.getInstance().getSharedColor(StandardColors.RCE_DOVE_GRAY);
            }
            return super.getForeground(element);
        }

        @Override
        public String getText(Object element) {
            if (element instanceof PaletteTreeNode) {
                PaletteTreeNode node = (PaletteTreeNode) element;
                if (columnIndex == 0) {
                    return node.getDisplayName();
                }
                if (node instanceof ComponentNode) {
                    ComponentNode componentNode = (ComponentNode) node;
                    if (columnIndex == 1) {
                        if (componentNode.isLocal()) {
                            return "local";
                        } else {
                            return "remote";

                        }
                    }
                    if (columnIndex == 2) {
                        return Boolean.toString(contentProvider.toolIsPresent(componentNode.getNodeName()));
                    }
                }
            }
            return "";
        }
    }

    private class TreeSelectionChangedListener implements ISelectionChangedListener {

        @Override
        public void selectionChanged(SelectionChangedEvent arg0) {
            TreeItem[] items = treeViewer.getTree().getSelection();
            if (items.length == 0) {
                addSubgroupButton.setEnabled(false);
                resetButton.setEnabled(false);
                editButton.setEnabled(false);
                deleteButton.setEnabled(false);
                return;
            }
            Object data = items[0].getData();
            boolean disableButtons = false;
            if (!(data instanceof PaletteTreeNode)) {
                return;
            }
            PaletteTreeNode node0 = (PaletteTreeNode) data;
            if (node0.isAccessibleComponent()) {
                // make sure that only tools are selected
                disableButtons = Stream.of(items).anyMatch(item -> !((PaletteTreeNode) item.getData()).isAccessibleComponent());
            } else if (node0.isGroup()) {
                // make sure that only groups are selected
                disableButtons = Stream.of(items).anyMatch(item -> !((PaletteTreeNode) item.getData()).isGroup());
            }

            if (!disableButtons) {
                resetButton.setEnabled(node0 instanceof ComponentNode);
                editButton.setEnabled(items.length == 1 && node0.isGroup() && node0.getGroupNode().isCustomGroup());
                addSubgroupButton.setEnabled(node0.isGroup() && items.length == 1);
                deleteButton
                    .setEnabled(
                        node0.isGroup() && node0.getGroupNode().isCustomGroup() && !contentProvider.containsAnyToolNodes(node0, false));
                if (data instanceof PaletteTreeNode) {
                    PaletteTreeNode node = (PaletteTreeNode) data;
                    if (Stream.of(PaletteViewConstants.RCE_GROUPS).collect(Collectors.toList()).contains(node.getNodeName())
                        || ToolType.getTopLevelGroupNames().contains(node.getNodeName())) {
                        editButton.setEnabled(false);
                    }
                }
            } else {
                addSubgroupButton.setEnabled(false);
                resetButton.setEnabled(false);
                editButton.setEnabled(false);
                deleteButton.setEnabled(false);
            }
        }
    }

    private class DeleteButtonSelectionListener implements SelectionListener {

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            // no implementation needed
        }

        @Override
        public void widgetSelected(SelectionEvent evt) {
            TreeItem[] items = treeViewer.getTree().getSelection();
            setTreesVisible(false);
            for (TreeItem item : items) {
                if (item.getData() instanceof PaletteTreeNode) {
                    PaletteTreeNode node = (PaletteTreeNode) item.getData();
                    String logNodeName = StringUtils.format("'%s'", node.getQualifiedGroupName());
                    if (node.hasChildren()) {
                        logNodeName = StringUtils.format("%s and subgroup(s) (%s)", logNodeName,
                            String.join(", ",
                                node.getAllSubGroups().stream().sorted().map(PaletteTreeNode::getQualifiedGroupName)
                                    .toArray(String[]::new)));
                    }
                    contentProvider.deleteGroup(node);
                    log.debug(StringUtils.format("Deleted group %s.", logNodeName));
                }
            }
            setTreesVisible(true);
            refreshTrees();
            getButton(OK).setFocus();
        }

    }

    private void setTreesVisible(boolean visible) {
        paletteView.getPaletteTreeViewer().getTree().setVisible(visible);
        treeViewer.getTree().setVisible(visible);
    }

    private class AddButtonSelectionListener implements SelectionListener {

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            // no implementation needed
        }

        @Override
        public void widgetSelected(SelectionEvent arg0) {
            Shell shell = Display.getDefault().getActiveShell();
            AddCustomGroupDialog addDialog = new AddCustomGroupDialog(shell, contentProvider, contentProvider.getRootNode(), false);
            addDialog.open();
            if (addDialog.isGroupAdded()) {
                refreshTrees();
            }
            getButton(OK).setFocus();
        }
    }

    private class AddSubgroupButtonSelectionListener implements SelectionListener {

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            // no implementation needed
        }

        @Override
        public void widgetSelected(SelectionEvent arg0) {
            Shell shell = Display.getDefault().getActiveShell();
            PaletteTreeNode parent = ((PaletteTreeNode) treeViewer.getTree().getSelection()[0].getData());
            AddCustomGroupDialog addDialog = new AddCustomGroupDialog(shell, contentProvider, parent, true);
            addDialog.open();
            if (addDialog.isGroupAdded()) {
                refreshTrees();
            }
            getButton(OK).setFocus();
        }
    }

    private class EditButtonSelectionListener implements SelectionListener {

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            // no implementation needed
        }

        @Override
        public void widgetSelected(SelectionEvent arg0) {
            TreeItem item = treeViewer.getTree().getSelection()[0];

            if (item.getData() instanceof PaletteTreeNode) {
                PaletteTreeNode node = (PaletteTreeNode) item.getData();
                Shell shell = Display.getDefault().getActiveShell();
                EditCustomGroupDialog editDialog = new EditCustomGroupDialog(shell, node, contentProvider.getAssignment());
                editDialog.open();
                if (editDialog.isGroupUpdated()) {
                    refreshTrees();
                }
            }
            getButton(OK).setFocus();
        }
    }

    private void refreshTrees() {
        paletteView.getPaletteTreeViewer().refresh();
        treeViewer.refresh();
    }

    private class ResetButtonSelectionListener implements SelectionListener {

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            // no implementation needed
        }

        @Override
        public void widgetSelected(SelectionEvent arg0) {
            TreeItem[] items = treeViewer.getTree().getSelection();
            if (items.length == 0) {
                return;
            }
            Object[] expandedElements = treeViewer.getExpandedElements();
            ComponentNode[] nodes = Arrays.stream(items).map(TreeItem::getData)
                .filter(ComponentNode.class::isInstance).toArray(ComponentNode[]::new);
            for (ComponentNode nodeToReset : nodes) {
                setTreesVisible(false);
                Object[] expandedElements1 = paletteView.getPaletteTreeViewer().getExpandedElements();
                contentProvider.resetGroup(nodeToReset);
                paletteView.getPaletteTreeViewer().refresh();
                paletteView.getPaletteTreeViewer().setExpandedElements(expandedElements1);
                paletteView.expandToNode(nodeToReset);
                setTreesVisible(true);
            }
            refreshTrees();
            treeViewer.setExpandedElements(expandedElements);
            resetButton.setEnabled(false);
            getButton(OK).setFocus();
            log.debug(StringUtils.format("Group assignment of component(s) '%s' reset to default group(s).",
                String.join(", ",
                    Arrays.stream(nodes).filter(ComponentNode.class::isInstance).map(ComponentNode.class::cast)
                        .sorted()
                        .map(node -> StringUtils.format("%s (%s)", node.getDisplayName(), node.getToolIdentification().getToolID()))
                        .toArray(String[]::new))));
        }
    }

    public TreeViewer getViewer() {
        return treeViewer;
    }

    public void refreshTree() {
        treeViewer.refresh();
    }
}
