/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.execute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.SystemUtils;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * {@link WizardPage} to configure the general workflow execution settings.
 * 
 * @author Christian Weiss
 * @author Goekhan Guerkan
 * @author Robert Mischke (minor change)
 */
final class WorkflowPage extends WizardPage {

    private static final String PLATFORM_DATA_PREFIX = "platform_index";

    private static final String FILTER = "Filter";

    private static final String ALL = "All";

    private static final int WIDTH_NAME_COLUMN = 200;

    private static final int WIDTH_BOX_COLUMN = 60;

    private static final int WIDTH_INSTANCE_COLUMN = 250;

    private static final int SCROLLBAR_CORRECTION = 16;

    private static final int TABLE_WIDTH = WIDTH_BOX_COLUMN + WIDTH_NAME_COLUMN + WIDTH_INSTANCE_COLUMN;

    private int tableHeight;

    private final WorkflowDescription workflowDescription;

    private final NodeIdentifierConfigurationHelper nodeIdConfigHelper;

    private WorkflowComposite workflowComposite;

    private TargetNodeEditingSupport editingSupport;

    private WorkflowNodeTargetPlatformLabelProvider targetNodeLabelProviderTree;

    private WorkflowNodeTargetPlatformLabelProvider targetNodeLabelProviderTable;

    private CheckboxLabelProvider checkboxProviderTree;

    private CheckboxLabelProvider checkboxProviderTable;

    private List<NodeIdentifier> missingInstances;

    private TreeContentProvider treeContenProvider;

    private final NodeIdentifier localNodeId;

    private boolean tableViewActive = true;

    public WorkflowPage(WorkflowDescription workflowDescription, NodeIdentifierConfigurationHelper exeHelper) {

        super(Messages.workflowPageName);

        this.workflowDescription = workflowDescription;
        setDescription(Messages.configure);
        this.nodeIdConfigHelper = exeHelper;
        setTitle(Messages.workflowPageTitle);

        for (WorkflowNode node : workflowDescription.getWorkflowNodes()) {
            node.setInit(false);
        }

        ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        PlatformService platformService = serviceRegistryAccess.getService(PlatformService.class);
        localNodeId = platformService.getLocalNodeId();
    }

    @Override
    public void dispose() {
        targetNodeLabelProviderTree.disposeRescources();
        targetNodeLabelProviderTable.disposeRescources();
        ColorPalette.getInstance().disposeColors();
        super.dispose();
    }

    @Override
    public void createControl(Composite parent) {
        // create the composite
        workflowComposite = new WorkflowComposite(parent, SWT.NONE);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        final int heightHint = 485;
        data.heightHint = heightHint;
        parent.setLayoutData(data);
        setControl(workflowComposite);
        // configure the workflow name text field
        String workflowName = workflowDescription.getName();
        if (workflowName != null) {
            workflowComposite.workflowNameText.setText(workflowName);
        }
        workflowComposite.workflowNameText.setFocus();
        workflowComposite.workflowNameText.selectAll();
        workflowComposite.workflowNameText.addKeyListener(new KeyAdapter() {

            @Override
            public void keyReleased(KeyEvent event) {
                String name = WorkflowPage.this.workflowComposite.workflowNameText.getText();
                WorkflowPage.this.workflowDescription.setName(name);
            }

        });

        setListenersForControllerInstanceCombo();

        refreshControllersTargetInstance();

        // configure the workflow components table viewer
        workflowComposite.componentsTableViewer
            .setContentProvider(new WorkflowDescriptionContentProvider(SWT.UP, TableSortSelectionListener.COLUMN_NAME));
        workflowComposite.componentsTableViewer.setInput(workflowDescription);

        workflowComposite.additionalInformationText
            .addKeyListener(new KeyAdapter() {

                @Override
                public void keyReleased(KeyEvent event) {
                    workflowDescription.setAdditionalInformation(
                        WorkflowPage.this.workflowComposite.additionalInformationText.getText());
                }
            });

        treeContenProvider = new TreeContentProvider();
        TableBehaviour.allCheckboxesClicked = false;

    }

    private void setListenersForControllerInstanceCombo() {

        // workaround to refresh the Combo Widget when network changes happen.

        workflowComposite.addListener(SWT.MouseMove, new Listener() {

            @Override
            public void handleEvent(Event arg0) {

                refreshControllersTargetInstance();

            }
        });

        workflowComposite.controllerTargetNodeCombo
            .addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent event) {
                    int index = workflowComposite.controllerTargetNodeCombo
                        .getSelectionIndex();
                    NodeIdentifier platform = (NodeIdentifier) workflowComposite.controllerTargetNodeCombo
                        .getData(PLATFORM_DATA_PREFIX + index);
                    workflowDescription.setControllerNode(platform);
                    refreshControllersTargetInstance();

                }

            });

    }

    /**
     * Refreshs the Controllers Instance Combo.
     */
    public void refreshControllersTargetInstance() {
        CCombo comboControllerTarget = workflowComposite.controllerTargetNodeCombo;

        if (comboControllerTarget != null) {

            comboControllerTarget.removeAll();

            // configure the workflow controller combo box
            comboControllerTarget.add(localNodeId.getAssociatedDisplayName()
                + " " + Messages.localPlatformSelectionTitle);
            comboControllerTarget.setData(PLATFORM_DATA_PREFIX + 0, null);
            final List<NodeIdentifier> nodes = nodeIdConfigHelper
                .getWorkflowControllerNodesSortedByName();
            nodes.remove(localNodeId);
            int index = 0;

            for (NodeIdentifier node : nodes) {
                index++;
                comboControllerTarget.add(node
                    .getAssociatedDisplayName());
                comboControllerTarget.setData(
                    PLATFORM_DATA_PREFIX + index, node);

            }
            // select the configured platform or default to the local platform
            NodeIdentifier selectedNode = workflowDescription.getControllerNode();
            if (selectedNode == null || selectedNode.equals(localNodeId)
                || !nodes.contains(selectedNode)) {
                comboControllerTarget.select(0);

            } else {
                int indexNode = nodes
                    .indexOf(selectedNode) + 1;
                comboControllerTarget.select(indexNode);

            }
            index = comboControllerTarget.getSelectionIndex();

            NodeIdentifier platform = (NodeIdentifier) comboControllerTarget
                .getData(PLATFORM_DATA_PREFIX + index);
            workflowDescription.setControllerNode(platform);
        }
    }

    @Override
    public boolean canFlipToNextPage() {

        if (hasInstanceError()) {
            return false;
        }

        if (!workflowComposite.areNodesValid()) {
            prepareErrorStatement();
            return false;
        }

        setErrorMessage(null);
        setDescription(Messages.configure);

        return true;

    }

    private boolean hasInstanceError() {
        if (missingInstances != null) {
            for (NodeIdentifier id : missingInstances) {

                for (WorkflowNode node : workflowDescription.getWorkflowNodes()) {

                    if (node.getComponentDescription().getNode().equals(id)) {
                        return true;
                    }
                }

            }
        }

        return false;
    }

    public void prepareErrorStatement() {

        List<WorkflowNode> nodes = new ArrayList<WorkflowNode>(workflowComposite.getInvalidNodes());

        // Sorting of invalid nodes in list
        Collections.sort(nodes);

        // list only the first three nodes in error message to ensure readability
        int count = 0;
        String errorNodes = " ";
        for (WorkflowNode node : nodes) {
            if (++count > 3) {
                errorNodes += ", ...";
                break;
            }
            if (!errorNodes.equals(" ")) {
                errorNodes += ", ";
            }
            errorNodes += node.getName();
        }

        if (count > 0) {
            setDescription(null);
            setErrorMessage(Messages.selectExcatMatchtingPlatform + errorNodes);
        }

    }

    protected boolean canFinish() {

        if (hasInstanceError()) {
            return false;

        }

        if (!workflowComposite.areNodesValid()) {
            return false;
        }

        return true;
    }

    protected String getWorkflowName() {
        return workflowDescription.getName();
    }

    protected NodeIdentifier getControllerNodeId() {
        return workflowDescription.getControllerNode();
    }

    protected Map<String, ComponentInstallation> getComponentInstallations() {
        Map<String, ComponentInstallation> cmpInstallations = new HashMap<>();
        for (WorkflowNode wfNode : workflowDescription.getWorkflowNodes()) {
            cmpInstallations.put(wfNode.getIdentifier(), wfNode.getComponentDescription().getComponentInstallation());
        }
        return cmpInstallations;
    }

    protected String getAdditionalInformation() {
        return workflowDescription.getAdditionalInformation();
    }

    public WorkflowComposite getWorkflowComposite() {
        return workflowComposite;
    }

    /**
     * The composite containing the controls to configure the workflow execution.
     * 
     * @author Christian Weiss
     */
    public class WorkflowComposite extends Composite {

        /**
         * {@link CellLabelProvider} class providing the name of the {@link WorkflowNode} of the current row.
         * 
         * @author Christian Weiss
         * @author Goekhan Guerkan
         */
        private class WorkflowNodeNameLabelProvider extends
            StyledCellLabelProvider {

            @Override
            protected void paint(Event event, Object element) {

                final int offset = 26;
                final int offsetIcon = 5;

                Rectangle bounds = event.getBounds();

                WorkflowNode node = (WorkflowNode) element;
                event.gc.setForeground(ColorPalette.getInstance().getBlackColor());
                event.gc.drawImage(targetNodeLabelProviderTree.getImage(node), bounds.x + offsetIcon, bounds.y);
                event.gc.drawText(node.getName(), bounds.x + offset, bounds.y);
            }

        }

        /**
         * {@link CellLabelProvider} class providing the name of the {@link WorkflowNode} or the WorkflowComponentType of the current row
         * for the tree. (depends if node is leaf or parent).
         * 
         * @author Goekhan Guerkan
         */

        private final class WorkflowNodeNameLabelTreeProvider extends
            StyledCellLabelProvider {

            private final int offsetName = 30;

            private final int offsetPic = 10;

            @Override
            protected void paint(Event event, Object element) {
                TreeNode node = (TreeNode) element;
                Rectangle bounds = event.getBounds();

                GC gc = event.gc;
                TreeItem item = (TreeItem) event.item;

                event.detail &= ~SWT.SELECTED;
                // MouseOver:
                event.detail &= ~SWT.HOT;

                gc.setBackground(item.getBackground(event.index));
                gc.setForeground(ColorPalette.getInstance().getBlackColor());
                gc.fillRectangle(event.x, event.y, event.width, event.height);

                if (node.isChildElement()) {

                    final int positionnName = bounds.x + offsetName;
                    final int postionPicture = bounds.x + offsetPic;

                    gc.drawImage(targetNodeLabelProviderTree.getImage(node.getWorkflowNode()), postionPicture, bounds.y);
                    gc.drawText(node.getComponentName(), positionnName, bounds.y);

                } else {

                    final int positionCompText = 5;
                    Font font = new Font(Display.getCurrent(), "Arial", 10, SWT.ITALIC);
                    gc.setFont(font);
                    gc.drawText(node.getComponentName(), bounds.x + positionCompText, bounds.y);
                    font.dispose();
                }

            }

        }

        /** Text field for the name of the selected workflow. */
        private Text workflowNameText;

        /** Text field for filter. */
        private Text search;

        /** Table viewer to select target platforms for all components. */
        private TableViewer componentsTableViewer;

        /** Table viewer to select target platforms for all components. */
        private TreeViewer componentsTreeViewer;

        /** Combo box to select the controllers target node. */
        private CCombo controllerTargetNodeCombo;

        /** Combo box to select the controllers target node. */
        private Button groupbyComponentCheck;

        /** Group for bulk Operation. */
        private Group grpComponentsTp;

        /** Text field for the additional information of the selected workflow. */
        private Text additionalInformationText;

        /** Filter for the table. */
        private Filter filterTable;

        /** Filter for the tree. */
        private Filter filterTree;

        /** Updater for the tree. */
        private TreeBehaviour treeUpdater;

        /** Updater for the table. */

        private TableBehaviour tableUpdater;

        /** Image to "fake" check box in a TableColumn. SWT does not support check box for a column */

        private Image checkedImg;

        /** Image to "fake" check box in a TableColumn. */

        private Image uncheckedImg;

        /** Image to "fake" disabled check box in a TableColumn. */

        /**
         * Creates the composite.
         * 
         * @param parent The parent composite.
         * @param style The style.
         */

        public WorkflowComposite(final Composite parent, int style) {
            super(parent, style);
            final int defaultHeight = 200;
            final ScrolledComposite sc = new ScrolledComposite(this, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
            sc.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, defaultHeight).create());
            sc.setExpandHorizontal(true);
            sc.setExpandVertical(true);

            final Composite containerMain = new Composite(sc, SWT.NULL);
            containerMain.setLayout(GridLayoutFactory.swtDefaults().numColumns(1).create());

            checkedImg = ImageManager.getInstance().getSharedImage(StandardImages.CHECK_CHECKED);
            uncheckedImg = ImageManager.getInstance().getSharedImage(StandardImages.CHECK_UNCHECKED);

            setLayout(new GridLayout(1, false));
            Group groupName = new Group(containerMain, SWT.NONE);
            groupName.setLayout(new GridLayout(1, false));
            groupName.setText(Messages.nameGroupTitle);
            groupName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
                false, 1, 1));

            workflowNameText = new Text(groupName, SWT.BORDER);
            workflowNameText.setText("");
            workflowNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,
                true, false, 1, 1));

            Group grpTargetPlatform = new Group(containerMain, SWT.NONE);

            grpTargetPlatform.setLayout(new GridLayout(1, false));
            grpTargetPlatform.setText(Messages.controlTP);
            grpTargetPlatform.setLayoutData(new GridData(SWT.FILL, SWT.TOP,
                true, false, 1, 1));

            controllerTargetNodeCombo = new CCombo(grpTargetPlatform,
                SWT.READ_ONLY | SWT.BORDER);
            controllerTargetNodeCombo.setLayoutData(new GridData(SWT.FILL,
                SWT.CENTER, true, false, 1, 1));
            controllerTargetNodeCombo.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

            grpComponentsTp = new Group(containerMain, SWT.NONE);
            grpComponentsTp.setLayout(new GridLayout(4, false));
            grpComponentsTp.setText(Messages.componentsTP);
            grpComponentsTp.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
                true, true, 1, 1));

            final int tableHWin = 200;
            final int tableHLinux = 170;

            // SWT.ICON , SWT.SEARCH is not supported with windows.
            if (SystemUtils.IS_OS_LINUX) {
                search = new Text(grpComponentsTp, SWT.SEARCH | SWT.CANCEL | SWT.ICON_SEARCH | SWT.BORDER | SWT.FILL);
                tableHeight = tableHLinux;

                search.addSelectionListener(new SelectionAdapter() {

                    public void widgetDefaultSelected(SelectionEvent e) {
                        if (e.detail == SWT.CANCEL) {
                            if (componentsTableViewer.getTable().isVisible()) {
                                search.setText("");
                                filterTable.setSearchText("");
                                refreshTable();
                                repackTable();

                            } else {

                                search.setText("");

                                filterTree.setSearchText("");

                                refreshTree();
                                repackTree();

                            }

                        }

                    };

                });
            } else {
                search = new Text(grpComponentsTp, SWT.BORDER | SWT.FILL);
                tableHeight = tableHWin;
            }

            GridData searchData = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
            searchData.minimumWidth = WIDTH_NAME_COLUMN;
            search.setLayoutData(searchData);
            search.setMessage(FILTER);

            search.addKeyListener(new KeyAdapter() {

                @Override
                public void keyReleased(KeyEvent e) {

                    Text text = (Text) e.getSource();
                    if (componentsTableViewer.getTable().isVisible()) {

                        filterTable.setSearchText(text.getText());

                        refreshTable();

                    } else {

                        filterTree.setSearchText(text.getText());

                        refreshTree();

                        // repackTree();

                    }

                }

            });

            groupbyComponentCheck = new Button(grpComponentsTp, SWT.CHECK);
            groupbyComponentCheck.setText("Group by component");
            GridData groupByComponentData = new GridData(SWT.END, SWT.FILL, false, false, 2, 1);
            groupbyComponentCheck.setLayoutData(groupByComponentData);
            groupbyComponentCheck.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {

                    tableUpdater.saveIndexOfComboBeforeRefresh();
                    treeUpdater.saveIndexOfComboBeforeRefresh();

                    changeTable();

                    if (componentsTableViewer.getTable().isVisible()) {
                        refreshTable();

                    } else {
                        refreshTree();

                    }

                    tableUpdater.setSavedComboIndex();
                    treeUpdater.setSavedComboIndex();
                }
            });

            componentsTableViewer = new TableViewer(grpComponentsTp, SWT.H_SCROLL
                | SWT.V_SCROLL | SWT.BORDER);

            componentsTableViewer.getTable().addControlListener(new ResizeListener());

            final Table componentsTable = componentsTableViewer.getTable();
            componentsTable.setHeaderVisible(true);
            final int visibleRows = 5;
            GridData data = new GridData(SWT.FILL, SWT.FILL,
                true, true, 4, 1);
            data.minimumHeight = tableHeight;
            data.heightHint = (visibleRows + 1)
                * componentsTable.getItemHeight();
            componentsTable.setLayoutData(data);

            editingSupport = new TargetNodeEditingSupport(nodeIdConfigHelper, localNodeId, componentsTableViewer, 1);

            buildTable();
            buildTree();

            GridData dataApplyInstance = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
            final Button applyInstanceBtn = new Button(grpComponentsTp, SWT.PUSH);
            applyInstanceBtn.setText(Messages.applyTargetInstance);
            applyInstanceBtn.setLayoutData(dataApplyInstance);
            applyInstanceBtn.setEnabled(false);

            GridData dataCombo = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
            dataCombo.verticalAlignment = SWT.CENTER;
            final CCombo comboTargetInstance = new CCombo(grpComponentsTp, SWT.READ_ONLY | SWT.BORDER);
            comboTargetInstance.setEnabled(false);
            comboTargetInstance.setLayoutData(dataCombo);
            comboTargetInstance.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
            comboTargetInstance.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    applyInstanceBtn.setFocus();
                }

            });

            applyInstanceBtn.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {

                    if (componentsTreeViewer.getTree().isVisible()) {

                        for (CCombo combo : targetNodeLabelProviderTree.getComboList()) {
                            if (combo.isVisible() && combo.getData() instanceof TreeNode) {

                                TreeNode n = (TreeNode) combo.getData();

                                if (n.getWorkflowNode() != null) {
                                    setInstanceFromComboToNodes(n.getWorkflowNode(), combo);
                                }
                            }

                        }

                        refreshTree();

                    } else {

                        for (CCombo combo : targetNodeLabelProviderTable.getComboList()) {
                            if (combo.isVisible()) {
                                if (combo.getData() instanceof WorkflowNode) {

                                    WorkflowNode node = (WorkflowNode) combo.getData();
                                    setInstanceFromComboToNodes(node, combo);
                                }
                            }
                        }

                    }
                }

                private void setInstanceFromComboToNodes(WorkflowNode node, CCombo combo) {

                    if (node.isChecked()) {

                        String instanceText = comboTargetInstance.getText();
                        if (instanceText.equals(Messages.anyRemote)) {

                            setAnyRemote(combo);
                            return;
                        }

                        for (int i = 0; i < combo.getItemCount(); i++) {

                            if (combo.getItem(i).equals(instanceText)) {
                                combo.select(i);
                            }
                        }

                        targetNodeLabelProviderTable.handleSelection(combo, node);

                    }

                }

            });

            Group groupAdditionalInformation = new Group(containerMain, SWT.NONE);
            groupAdditionalInformation.setLayout(new GridLayout(1, false));
            groupAdditionalInformation
                .setText(de.rcenvironment.core.gui.workflow.view.list.Messages.additionalInformationColon);
            groupAdditionalInformation.setLayoutData(new GridData(SWT.FILL, SWT.END, true, false, 1, 1));

            additionalInformationText = new Text(groupAdditionalInformation, SWT.BORDER);
            additionalInformationText.setLayoutData(new GridData(SWT.FILL, SWT.END, true, false, 1, 1));

            tableUpdater.setMasterCombo(comboTargetInstance);
            treeUpdater.setMasterCombo(comboTargetInstance);
            tableUpdater.setMasterButton(applyInstanceBtn);
            treeUpdater.setMasterButton(applyInstanceBtn);

            sc.setContent(containerMain);
            sc.setMinSize(containerMain.computeSize(SWT.DEFAULT, SWT.DEFAULT));

            setControl(this);

        }

        private void setAnyRemote(CCombo combo) {

            if (componentsTreeViewer.getTree().isVisible()) {

                if (combo.isVisible() && combo.getData() instanceof TreeNode) {

                    TreeNode n = (TreeNode) combo.getData();

                    if (n.getWorkflowNode() != null) {

                        setRandomlyRemote(n.getWorkflowNode(), combo);
                    }
                }

            } else {

                if (combo.isVisible()) {
                    if (combo.getData() instanceof WorkflowNode) {

                        WorkflowNode node = (WorkflowNode) combo.getData();
                        setRandomlyRemote(node, combo);
                    }
                }
            }

        }

        private void setRandomlyRemote(WorkflowNode node, CCombo combo) {

            if (node.isChecked()) {
                int i = editingSupport.setRemoteValue(node);

                if (i >= 0) {

                    combo.select(i);

                }
            }

        }

        private void changeTable() {

            GridData dataTable = (GridData) componentsTableViewer.getTable().getLayoutData();
            GridData dataTree = (GridData) componentsTreeViewer.getTree().getLayoutData();

            if (componentsTreeViewer.getTree().isVisible()) {
                dataTree.exclude = true;
                componentsTreeViewer.getTree().setVisible(false);
            } else {
                dataTree.exclude = false;
                filterTree.setSearchText(search.getText());
                refreshTree();
                componentsTreeViewer.getTree().setVisible(true);
                tableViewActive = false;
            }
            if (componentsTableViewer.getTable().isVisible()) {
                dataTable.exclude = true;
                componentsTableViewer.getTable().setVisible(false);
            } else {
                filterTable.setSearchText(search.getText());
                tableUpdater.setCurrentlyUsedSortingColumn(1); // back to default after change of view.
                tableUpdater.refreshColumns();
                dataTable.exclude = false;
                componentsTableViewer.getTable().setVisible(true);
                tableViewActive = true;

            }
            componentsTableViewer.getControl().getParent().getParent().pack();
            componentsTableViewer.getControl().getParent().getParent().layout(true, true);

        }

        private void buildTree() {

            componentsTreeViewer = new TreeViewer(grpComponentsTp, SWT.H_SCROLL
                | SWT.V_SCROLL | SWT.BORDER);
            componentsTreeViewer.getTree().addControlListener(new ResizeListener());
            targetNodeLabelProviderTree = new WorkflowNodeTargetPlatformLabelProvider(
                editingSupport, workflowDescription, getWizard());
            checkboxProviderTree = new CheckboxLabelProvider();
            targetNodeLabelProviderTree.setPage(WorkflowPage.this);
            componentsTreeViewer.getTree().addListener(SWT.Expand, new Listener() {

                public void handleEvent(Event e) {
                    componentsTreeViewer.getTree().redraw();
                }
            });
            componentsTreeViewer.getTree().addListener(SWT.Collapse, new Listener() {

                public void handleEvent(Event e) {
                    componentsTreeViewer.getTree().redraw();
                }
            });
            treeUpdater = new TreeBehaviour(componentsTreeViewer, targetNodeLabelProviderTree, checkboxProviderTree);
            targetNodeLabelProviderTree.setUpdater(treeUpdater);
            checkboxProviderTree.setUpdater(treeUpdater);
            componentsTreeViewer.setContentProvider(new TreeContentProvider());
            componentsTreeViewer.setInput(workflowDescription);

            componentsTreeViewer.getTree().setHeaderVisible(true);
            final int visibleRows = 5;
            GridData dataOfTree = new GridData(SWT.FILL, SWT.FILL,
                true, true, 1, 1);
            dataOfTree.horizontalSpan = 10;
            dataOfTree.exclude = true;
            dataOfTree.minimumHeight = tableHeight;
            dataOfTree.heightHint = (visibleRows + 1)
                * componentsTreeViewer.getTree().getItemHeight();
            componentsTreeViewer.getTree().setLayoutData(dataOfTree);
            componentsTreeViewer.getTree().setVisible(false);

            final TreeViewerColumn columnViewerBox =
                createTreeColumn(componentsTreeViewer, checkboxProviderTree, "", WIDTH_BOX_COLUMN);
            columnViewerBox.getColumn().setImage(uncheckedImg);
            columnViewerBox.getColumn().setText(ALL);
            columnViewerBox.getColumn().addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {

                    treeUpdater.saveIndexOfComboBeforeRefresh();

                    if (treeUpdater.isCheckBoxColumnEnabled()) {

                        if (columnViewerBox.getColumn().getImage().equals(uncheckedImg)) {
                            TableBehaviour.allCheckboxesClicked = true;
                            columnViewerBox.getColumn().setImage(checkedImg);
                            for (Button btn : checkboxProviderTree.getBtnList()) {

                                if (btn.isEnabled()) {

                                    TreeNode node = (TreeNode) btn.getData(TableBehaviour.KEY_CHECK);
                                    if (node.isChildElement()) {
                                        node.getWorkflowNode().setChecked(true);
                                    }
                                    btn.setSelection(true);
                                }
                            }

                        } else {

                            for (Button btn : checkboxProviderTree.getBtnList()) {

                                if (btn.isEnabled()) {
                                    TreeNode node = (TreeNode) btn.getData(TableBehaviour.KEY_CHECK);
                                    if (node.isChildElement()) {
                                        node.getWorkflowNode().setChecked(false);
                                    }
                                    btn.setSelection(false);
                                }
                            }
                            TableBehaviour.allCheckboxesClicked = false;
                            columnViewerBox.getColumn().setImage(uncheckedImg);
                        }
                        treeUpdater.prepareValuesForMasterCombo();
                        treeUpdater.checkIfDisableMasterBtn();
                        treeUpdater.setSavedComboIndex();
                    }
                }
            });

            TreeViewerColumn columnViewer =
                createTreeColumn(componentsTreeViewer, new WorkflowNodeNameLabelTreeProvider(), Messages.component, WIDTH_NAME_COLUMN);
            componentsTreeViewer.getTree().setSortColumn(columnViewer.getColumn());
            componentsTreeViewer.getTree().setSortDirection(SWT.UP);
            columnViewer.getColumn().addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    treeUpdater.saveIndexOfComboBeforeRefresh();
                    int direction = componentsTreeViewer.getTree().getSortDirection();

                    if (direction == SWT.UP) {
                        direction = SWT.DOWN;
                    } else {
                        direction = SWT.UP;
                    }
                    componentsTreeViewer.getTree().setSortDirection(direction);

                    refreshTree();
                    treeUpdater.setSavedComboIndex();
                }
            });

            componentsTreeViewer.setSorter(new ViewerSorter() {

                @Override
                public int compare(Viewer viewer, Object node, Object otherNode) {

                    TreeViewer treeViewer = (TreeViewer) viewer;
                    TreeNode nodeOne = (TreeNode) node;
                    TreeNode nodeTwo = (TreeNode) otherNode;

                    if (treeViewer.getTree().getSortDirection() == SWT.DOWN) {
                        return nodeTwo.getComponentName().compareTo(nodeOne.getComponentName());
                    } else {
                        return nodeOne.getComponentName().compareTo(nodeTwo.getComponentName());
                    }
                }
            });

            createTreeColumn(componentsTreeViewer, targetNodeLabelProviderTree, Messages.targetPlatform, WIDTH_INSTANCE_COLUMN);
            filterTree = new Filter(treeUpdater);
            componentsTreeViewer.addFilter(filterTree);
            componentsTreeViewer.refresh();
            componentsTreeViewer.getTree().setVisible(false);
            // Disabled Selection, as selection overrides the color of the background and it has no use in this case.
            componentsTreeViewer.getTree().addListener(SWT.EraseItem, new Listener() {

                public void handleEvent(Event event) {
                    // Selection: ( On linux it highlights the text white if disabled, included white background it's bad).
                    event.detail &= ~SWT.SELECTED;
                    // MouseOver:
                    event.detail &= ~SWT.HOT;
                    GC gc = event.gc;
                    TreeItem item = (TreeItem) event.item;
                    gc.setBackground(item.getBackground(event.index));
                    gc.fillRectangle(event.x, event.y, event.width, event.height);

                }
            });
        }

        /**
         * Repacks tree, to clear a drawing bug.
         */
        private void repackTree() {
            componentsTreeViewer.getTree().getParent().pack();
            componentsTreeViewer.getTree().getParent().getParent().pack();
            componentsTreeViewer.getControl().getParent().getParent().layout(true, true);

        }

        /**
         * Repacks tree, to clear a drawing bug.
         */
        private void repackTable() {
            componentsTableViewer.getTable().getParent().pack();
            componentsTableViewer.getTable().getParent().getParent().pack();
            componentsTableViewer.getControl().getParent().getParent().layout(true, true);

        }

        private TreeViewerColumn createTreeColumn(TreeViewer treeViewer, CellLabelProvider provider,
            String columnHeader, int width) {

            TreeViewerColumn column = new TreeViewerColumn(
                componentsTreeViewer, SWT.HIDE_SELECTION);

            column.getColumn().setAlignment(SWT.LEFT);

            column.setLabelProvider(provider);

            column.getColumn().setText(columnHeader);
            column.getColumn().setWidth(width);
            column.getColumn().setResizable(true);
            column.getColumn().setMoveable(false);

            return column;

        }

        private void buildTable() {

            targetNodeLabelProviderTable = new WorkflowNodeTargetPlatformLabelProvider(
                editingSupport, workflowDescription, getWizard());

            checkboxProviderTable = new CheckboxLabelProvider();
            tableUpdater = new TableBehaviour(componentsTableViewer, targetNodeLabelProviderTable, checkboxProviderTable);
            checkboxProviderTable.setUpdater(tableUpdater);
            targetNodeLabelProviderTable.setUpdater(tableUpdater);
            targetNodeLabelProviderTable.setPage(WorkflowPage.this);

            filterTable = new Filter(tableUpdater);
            componentsTableViewer.addFilter(filterTable);

            WorkflowNodeNameLabelProvider providerNames = new WorkflowNodeNameLabelProvider();

            final TableColumn columnCheck = createTableColumn(componentsTableViewer, checkboxProviderTable, "", WIDTH_BOX_COLUMN);
            columnCheck.setText(ALL);
            columnCheck.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {

                    tableUpdater.saveIndexOfComboBeforeRefresh();

                    if (tableUpdater.isCheckBoxColumnEnabled()) {

                        if (columnCheck.getImage().equals(uncheckedImg)) {

                            columnCheck.setImage(checkedImg);
                            TableBehaviour.allCheckboxesClicked = true;

                            for (Button btn : checkboxProviderTable.getBtnList()) {

                                if (btn.isEnabled()) {

                                    WorkflowNode node = (WorkflowNode) btn.getData(TableBehaviour.KEY_CHECK);

                                    node.setChecked(true);
                                    btn.setSelection(true);
                                }

                            }
                        } else {

                            for (Button btn : checkboxProviderTable.getBtnList()) {

                                if (btn.isEnabled()) {

                                    WorkflowNode node = (WorkflowNode) btn.getData(TableBehaviour.KEY_CHECK);

                                    node.setChecked(false);
                                    btn.setSelection(false);

                                }

                            }

                            TableBehaviour.allCheckboxesClicked = false;
                            columnCheck.setImage(uncheckedImg);
                        }
                        tableUpdater.prepareValuesForMasterCombo();
                        tableUpdater.checkIfDisableMasterBtn();
                        tableUpdater.setSavedComboIndex();

                    }
                }
            });

            columnCheck.setImage(uncheckedImg);

            TableColumn columnName = createTableColumn(componentsTableViewer, providerNames, Messages.component, WIDTH_NAME_COLUMN);
            columnName.setData(TableSortSelectionListener.COLUMN_NAME);

            TableColumn columnInstance =
                createTableColumn(componentsTableViewer, targetNodeLabelProviderTable, Messages.targetPlatform, WIDTH_INSTANCE_COLUMN);
            columnInstance.setData(TableSortSelectionListener.COLUMN_INSTANCE);

            TableSortSelectionListener listenerColumnOne = new TableSortSelectionListener(componentsTableViewer, columnName, SWT.DOWN);
            listenerColumnOne.setUpdaterTable(tableUpdater);

            TableSortSelectionListener listenerColumnTwo = new TableSortSelectionListener(componentsTableViewer, columnInstance, SWT.DOWN);
            listenerColumnTwo.setUpdaterTable(tableUpdater);

            componentsTableViewer.getTable().setSortColumn(columnName);
            componentsTableViewer.getTable().setSortDirection(SWT.UP);

            componentsTableViewer.getTable().addListener(SWT.EraseItem, new EraseListener());

        }

        private TableColumn createTableColumn(TableViewer viewer, CellLabelProvider provider, String text,
            int width) {

            TableViewerColumn column = new TableViewerColumn(
                viewer, SWT.FILL);

            column.setLabelProvider(provider);

            column.getColumn().setAlignment(SWT.LEFT);
            column.getColumn().setText(text);
            column.getColumn().setWidth(width);
            column.getColumn().setResizable(true);
            column.getColumn().setMoveable(false);

            return column.getColumn();
        }

        /**
         * CLass to improve visibility of rows.
         */
        private class EraseListener implements Listener {

            @Override
            public void handleEvent(Event event) {
                // Selection: ( On linux it highlights the text white if disabled, included white background it's bad).
                event.detail &= ~SWT.SELECTED;

                // MouseOver:
                event.detail &= ~SWT.HOT;

                GC gc = event.gc;
                TableItem item = (TableItem) event.item;
                gc.setBackground(item.getBackground(event.index));
                gc.fillRectangle(event.x, event.y, event.width, event.height);

            }

        }

        public boolean areNodesValid() {
            if (componentsTableViewer.getTable().isVisible()) {
                return targetNodeLabelProviderTable.areNodesValid();

            } else {
                return targetNodeLabelProviderTree.areNodesValid();
            }
        }

        private void refreshTree() {

            componentsTreeViewer.getTree().setRedraw(false);
            treeUpdater.saveIndexOfComboBeforeRefresh();
            treeUpdater.disposeWidgets();
            componentsTreeViewer.setContentProvider(treeContenProvider);

            treeUpdater.refreshColumns();
            componentsTreeViewer.getTree().pack();
            treeUpdater.setSavedComboIndex();
            repackTree();

            componentsTreeViewer.getTree().setRedraw(true);

        }

        private void refreshTable() {

            componentsTableViewer.getTable().setRedraw(false);
            tableUpdater.saveIndexOfComboBeforeRefresh();

            tableUpdater.disposeWidgets();
            tableUpdater.refreshColumns();
            tableUpdater.setSavedComboIndex();

            componentsTableViewer.getTable().pack();
            repackTable();

            componentsTableViewer.getTable().setRedraw(true);

        }

        public List<WorkflowNode> getInvalidNodes() {
            Iterator<Entry<WorkflowNode, Boolean>> entries;
            List<WorkflowNode> invalidNodesList = new ArrayList<WorkflowNode>();

            if (componentsTableViewer.getTable().isVisible()) {
                entries = targetNodeLabelProviderTable
                    .getNodesValidList().entrySet().iterator();
            } else {

                entries = targetNodeLabelProviderTree
                    .getNodesValidList().entrySet().iterator();
            }

            while (entries.hasNext()) {
                Entry<WorkflowNode, Boolean> entry = entries.next();
                WorkflowNode key = (WorkflowNode) entry.getKey();
                Boolean value = (Boolean) entry.getValue();
                editingSupport.getHasVersionErrorMap().put(key, false);

                if (!value) {

                    invalidNodesList.add(key);
                    editingSupport.getHasVersionErrorMap().put(key, true);

                }
            }
            return invalidNodesList;

        }

        public void refreshContent() {

            nodeIdConfigHelper.refreshInstallations();
            refreshControllersTargetInstance();

            if (tableViewActive) {
                refreshTable();
            } else {

                refreshTree();

            }

        }

        /**
         * Listener called to resize the second column.
         */
        public final class ResizeListener extends ControlAdapter {

            public void controlResized(ControlEvent event) {

                resizeTables();
            }

        }

        private void resizeTables() {

            int offSet = 0;
            int scrollBarWidth = 0;

            if (componentsTableViewer.getTable().getItemCount() > 9) {
                scrollBarWidth = 0;
            } else {
                scrollBarWidth = SCROLLBAR_CORRECTION + 1;

            }

            if (!componentsTreeViewer.getTree().isVisible()) {

                offSet = componentsTableViewer.getTable().getClientArea().width - TABLE_WIDTH - scrollBarWidth;

            } else {

                offSet = componentsTreeViewer.getTree().getClientArea().width - TABLE_WIDTH - scrollBarWidth;
            }

            if (offSet > 0) {
                componentsTableViewer.getTable().getColumn(2).setWidth(WIDTH_INSTANCE_COLUMN + offSet);
                componentsTreeViewer.getTree().getColumn(2).setWidth(WIDTH_INSTANCE_COLUMN + offSet);

            }

        }

    }

}
