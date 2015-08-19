/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.execute;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.api.SimpleCommunicationService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.Activator;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * {@link WizardPage} to configure the general workflow execution settings.
 * 
 * @author Christian Weiss
 */
final class WorkflowPage extends WizardPage {

    private static final String PLATFORM_DATA_PREFIX = "platform_index";

    private final WorkflowDescription workflowDescription;

    private final WorkflowExecutionConfigurationHelper helper;

    private WorkflowComposite workflowComposite;

    private String additionalInformation;

    private final Set<Resource> resources = new HashSet<Resource>();

    /**
     * The Constructor.
     */
    public WorkflowPage(final WorkflowExecutionWizard parentWizard) {
        super(Messages.workflowPageName);
        this.workflowDescription = parentWizard.getWorkflowDescription();
        this.helper = parentWizard.getHelper();
        setTitle(Messages.workflowPageTitle);
    }

    /**
     * {@inheritDoc} This includes the {@link Image} resources of table icons.
     * 
     * @see org.eclipse.jface.dialogs.DialogPage#dispose()
     */
    @Override
    public void dispose() {
        for (Resource resource : resources) {
            resource.dispose();
        }
        super.dispose();
    }

    @Override
    public void createControl(Composite parent) {
        // create the composite
        workflowComposite = new WorkflowComposite(parent, SWT.NONE);
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
                String name = WorkflowPage.this.workflowComposite.workflowNameText
                    .getText();
                WorkflowPage.this.workflowDescription.setName(name);
            }

        });
        // configure the workflow controller combo box
        ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry
            .createAccessFor(this);
        PlatformService platformService = serviceRegistryAccess
            .getService(PlatformService.class);
        final NodeIdentifier localNode = platformService.getLocalNodeId();
        workflowComposite.controllerTargetNodeCombo.add(localNode
            .getAssociatedDisplayName()
            + " "
            + Messages.localPlatformSelectionTitle);
        workflowComposite.controllerTargetNodeCombo.setData(
            PLATFORM_DATA_PREFIX + 0, null);
        final List<NodeIdentifier> nodes = helper
            .getWorkflowControllerNodesSortedByName();
        nodes.remove(localNode);
        int index = 0;
        for (NodeIdentifier node : nodes) {
            index++;
            workflowComposite.controllerTargetNodeCombo.add(node
                .getAssociatedDisplayName());
            workflowComposite.controllerTargetNodeCombo.setData(
                PLATFORM_DATA_PREFIX + index, node);
        }
        // select the configured platform or default to the local platform
        NodeIdentifier selectedNode = workflowDescription.getControllerNode();
        if (selectedNode == null || selectedNode.equals(localNode)
            || !nodes.contains(selectedNode)) {
            workflowComposite.controllerTargetNodeCombo.select(0);
        } else {
            workflowComposite.controllerTargetNodeCombo.select(nodes
                .indexOf(selectedNode) + 1);
        }
        index = workflowComposite.controllerTargetNodeCombo.getSelectionIndex();
        NodeIdentifier platform = (NodeIdentifier) workflowComposite.controllerTargetNodeCombo
            .getData(PLATFORM_DATA_PREFIX + index);
        workflowDescription.setControllerNode(platform);
        workflowComposite.controllerTargetNodeCombo
            .addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent event) {
                    int index = workflowComposite.controllerTargetNodeCombo
                        .getSelectionIndex();
                    NodeIdentifier platform = (NodeIdentifier) workflowComposite.controllerTargetNodeCombo
                        .getData(PLATFORM_DATA_PREFIX + index);
                    workflowDescription.setControllerNode(platform);
                }

            });
        // configure the workflow components table viewer
        workflowComposite.componentsTableViewer
            .setContentProvider(new WorkflowDescriptionContentProvider());
        workflowComposite.componentsTableViewer.setInput(workflowDescription);

        workflowComposite.additionalInformationText
            .addKeyListener(new KeyAdapter() {

                @Override
                public void keyReleased(KeyEvent event) {
                    additionalInformation = WorkflowPage.this.workflowComposite.additionalInformationText
                        .getText();
                }
            });

    }

    public String getAdditionalInformation() {
        return additionalInformation;
    }

    @Override
    public boolean canFlipToNextPage() {
        if (workflowComposite.areNodesValid()) {
            setErrorMessage(null);
            return true;
        } else {

            prepareErrorStatement();

            return false;
        }
    }

    private void prepareErrorStatement() {
        
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
        setErrorMessage(Messages.selectExcatMatchtingPlatform + errorNodes);

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
         * {@link CellLabelProvider} class equipping every target platform cell with a distinct editor.
         * 
         * @author Christian Weiss
         */
        private final class WorkflowNodeTargetPlatformLabelProvider extends
            CellLabelProvider {

            private final Table componentsTable;

            private final TargetNodeEditingSupport editingSupport;

            private Map<WorkflowNode, Image> images = new HashMap<WorkflowNode, Image>();

            private Map<WorkflowNode, Boolean> nodesValid = new HashMap<WorkflowNode, Boolean>();

            /**
             * The constructor.
             * 
             * @param componentsTable
             */
            private WorkflowNodeTargetPlatformLabelProvider(
                final Table componentsTable,
                final TargetNodeEditingSupport editingSupport) {
                this.componentsTable = componentsTable;
                this.editingSupport = editingSupport;
            }

            /**
             * Returns the {@link Image} to be used as icon for the given {@link WorkflowNode} or null if none is set. The image is created
             * if it does not exist yet and added to the {@link WorkflowPage#resources} set to be disposed upon disposal of the
             * {@link WizardPage} instance}.
             * 
             * @param workflowNode The {@link WorkflowNode} to get the icon for.
             * @return The icon of the given {@link WorkflowNode} or null if none is set.
             */
            private Image getImage(WorkflowNode workflowNode) {
                // create the image, if it has not been created yet
                if (!images.containsKey(workflowNode)) {
                    final ComponentDescription componentDescription = workflowNode
                        .getComponentDescription();
                    Image image = null;
                    // prefer the 16x16 icon
                    byte[] icon = componentDescription.getIcon16();
                    // if there is no 16x16 icon try the 32x32 one
                    if (icon == null) {
                        icon = componentDescription.getIcon32();
                    }
                    // only create an image, if icon data are available
                    if (icon != null) {
                        image = new Image(Display.getCurrent(),
                            new ByteArrayInputStream(icon));
                        resources.add(image);
                    } else {
                        image = Activator.getInstance().getImageRegistry()
                            .get(Activator.IMAGE_RCE_ICON_16);
                    }
                    images.put(workflowNode, image);
                }
                return images.get(workflowNode);
            }

            @Override
            public void update(ViewerCell cell) {
                final WorkflowNode workflowNode = (WorkflowNode) cell
                    .getElement();
                TableItem item = (TableItem) cell.getViewerRow().getItem();
                Image workflowIcon = getImage(workflowNode);
                item.setImage(workflowIcon);
                TableEditor editor = new TableEditor(componentsTable);
                final CCombo combo = new CCombo(componentsTable, SWT.DROP_DOWN);
                combo.setEditable(false);
                combo.setBackground(Display.getCurrent().getSystemColor(
                    SWT.COLOR_LIST_BACKGROUND));
                editor.grabHorizontal = true;
                editor.setEditor(combo, item, 1);
                for (String value : editingSupport.getValues(workflowNode)) {
                    combo.add(value);
                }
                final Integer selectionIndex = (Integer) editingSupport
                    .getValue(workflowNode);
                if (selectionIndex != null) {
                    combo.select(selectionIndex);
                } else {
                    // default selection is the first available element
                    combo.select(0);
                }
                handleSelection(combo, workflowNode);

                combo.addSelectionListener(new SelectionAdapter() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        handleSelection(combo, workflowNode);
                        getWizard().getContainer().updateButtons();
                    }
                });
            }

            private void handleSelection(CCombo combo, WorkflowNode workflowNode) {
                String identifier = null;
                for (WorkflowNode node : workflowDescription.getWorkflowNodes()) {
                    if (node.getIdentifier().equals(
                        workflowNode.getIdentifier())) {
                        identifier = node.getIdentifier();
                    }
                }
                WorkflowNode wfNode = workflowDescription
                    .getWorkflowNode(identifier);
                editingSupport.setValue(wfNode, combo.getSelectionIndex());
                boolean exactMatch = editingSupport
                    .isNodeExactMatchRegardingComponentVersion(wfNode);
                nodesValid.put(wfNode, exactMatch);

            }

            private boolean areNodesValid() {
                return !nodesValid.values().contains(Boolean.FALSE);
            }

            public Map<WorkflowNode, Boolean> getNodesValidList() {
                return nodesValid;
            }

        }

        /**
         * {@link CellLabelProvider} class providing the name of the {@link WorkflowNode} of the current row.
         * 
         * @author Christian Weiss
         */
        // FIXME static
        private final class WorkflowNodeNameLabelProvider extends
            CellLabelProvider {

            @Override
            public void update(ViewerCell cell) {
                cell.setText(((WorkflowNode) cell.getElement()).getName());
            }
        }

        /** Text field for the name of the selected workflow. */
        private Text workflowNameText;

        /** Table viewer to select target platforms for all components. */
        private TableViewer componentsTableViewer;

        /** Combo box to select the controllers target node. */
        private Combo controllerTargetNodeCombo;

        /** Text field for the additional information of the selected workflow. */
        private Text additionalInformationText;

        private WorkflowNodeTargetPlatformLabelProvider targetNodeLabelProvider;

        /**
         * Creates the composite.
         * 
         * @param parent The parent composite.
         * @param style The style.
         */
        public WorkflowComposite(final Composite parent, int style) {
            super(parent, style);
            setLayout(new GridLayout(1, false));

            Group groupName = new Group(this, SWT.NONE);
            groupName.setLayout(new GridLayout(1, false));
            groupName.setText(Messages.nameGroupTitle);
            groupName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
                false, 1, 1));

            workflowNameText = new Text(groupName, SWT.BORDER);
            workflowNameText.setText("");
            workflowNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,
                true, false, 1, 1));

            Group grpTargetPlatform = new Group(this, SWT.NONE);
            grpTargetPlatform.setLayout(new GridLayout(1, false));
            grpTargetPlatform.setText(Messages.controlTP);
            grpTargetPlatform.setLayoutData(new GridData(SWT.FILL, SWT.TOP,
                true, false, 1, 1));

            controllerTargetNodeCombo = new Combo(grpTargetPlatform,
                SWT.READ_ONLY);
            controllerTargetNodeCombo.setLayoutData(new GridData(SWT.FILL,
                SWT.CENTER, true, false, 1, 1));

            Group grpComponentsTp = new Group(this, SWT.NONE);
            grpComponentsTp.setLayout(new GridLayout(1, false));
            grpComponentsTp.setText(Messages.componentsTP);
            grpComponentsTp.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
                true, true, 1, 1));

            componentsTableViewer = new TableViewer(grpComponentsTp, SWT.BORDER
                | SWT.FULL_SELECTION);
            final Table componentsTable = componentsTableViewer.getTable();
            componentsTable.setLinesVisible(true);
            componentsTable.setHeaderVisible(true);
            final int visibleRows = 5;
            GridData grid = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
            grid.heightHint = (visibleRows + 1)
                * componentsTable.getItemHeight();
            componentsTable.setLayoutData(grid);

            // Set table model for individual components
            String[] titles = { Messages.component, Messages.targetPlatform };
            final int width = 250;

            final NodeIdentifier localNode = new SimpleCommunicationService()
                .getLocalNodeId();
            final TargetNodeEditingSupport editingSupport = new TargetNodeEditingSupport(
                helper, localNode, componentsTableViewer, 1);
            targetNodeLabelProvider = new WorkflowNodeTargetPlatformLabelProvider(
                componentsTable, editingSupport);
            for (int i = 0; i < titles.length; i++) {
                TableViewerColumn column = new TableViewerColumn(
                    componentsTableViewer, SWT.NONE);
                column.getColumn().setText(titles[i]);
                column.getColumn().setWidth(width);
                column.getColumn().setResizable(true);
                column.getColumn().setMoveable(false);
                switch (i) {
                case 0:
                    column.setLabelProvider(new WorkflowNodeNameLabelProvider());
                    break;
                case 1:
                    column.setLabelProvider(targetNodeLabelProvider);
                    break;
                default:
                    throw new AssertionError();
                }
            }

            Group groupAdditionalInformation = new Group(this, SWT.NONE);
            groupAdditionalInformation.setLayout(new GridLayout(1, false));
            groupAdditionalInformation
                .setText(de.rcenvironment.core.gui.workflow.view.list.Messages.additionalInformationColon);
            groupAdditionalInformation.setLayoutData(new GridData(SWT.FILL,
                SWT.CENTER, true, false, 1, 1));

            additionalInformationText = new Text(groupAdditionalInformation,
                SWT.BORDER);
            additionalInformationText.setLayoutData(new GridData(SWT.FILL,
                SWT.CENTER, true, false, 1, 1));
        }

        public boolean areNodesValid() {
            return targetNodeLabelProvider.areNodesValid();
        }

        public List<WorkflowNode> getInvalidNodes() {

            List<WorkflowNode> invalidNodesList = new ArrayList<WorkflowNode>();
            Iterator<Entry<WorkflowNode, Boolean>> entries = targetNodeLabelProvider
                .getNodesValidList().entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry entry = entries.next();
                WorkflowNode key = (WorkflowNode) entry.getKey();
                Boolean value = (Boolean) entry.getValue();

                if (!value) {

                    invalidNodesList.add(key);
                }
            }
            return invalidNodesList;

        }

    }

}
