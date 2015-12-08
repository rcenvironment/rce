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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.widgets.Display;

import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.Activator;

/**
 * The composite containing the controls to configure the workflow execution.
 * 
 * @author Christian Weiss
 * @author Goekhan Guerkan
 */

public class WorkflowNodeTargetPlatformLabelProvider extends
    StyledCellLabelProvider {

    private List<CCombo> comboList;

    private TargetNodeEditingSupport editingSupport;

    private WorkflowDescription workflowDescription;

    private Map<WorkflowNode, Image> images = new HashMap<WorkflowNode, Image>();

    private final Set<Resource> resources = new HashSet<Resource>();

    private Map<WorkflowNode, Boolean> nodesValid = new HashMap<WorkflowNode, Boolean>();

    private Map<WorkflowNode, Integer> nodeValueWithError = new HashMap<WorkflowNode, Integer>();

    private IWizard iWizard;

    private WorkflowPage page;

    private Updatable updater;

    /**
     * The constructor.
     * 
     * @param componentsTable
     * @param iWizard
     */
    public WorkflowNodeTargetPlatformLabelProvider(
        TargetNodeEditingSupport editingSupport, WorkflowDescription workflowDescription, IWizard iWizard) {
        this.editingSupport = editingSupport;
        this.workflowDescription = workflowDescription;
        comboList = new ArrayList<CCombo>();
        this.iWizard = iWizard;
    }

    /**
     * Returns the {@link Image} to be used as icon for the given {@link WorkflowNode} or null if none is set. The image is created if it
     * does not exist yet and added to the {@link WorkflowPage#resources} set to be disposed upon disposal of the {@link WizardPage}
     * instance}.
     * 
     * @param workflowNode The {@link WorkflowNode} to get the icon for.
     * @return The icon of the given {@link WorkflowNode} or null if none is set.
     */
    public Image getImage(WorkflowNode workflowNode) {
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

        updater.updateInstanceColumn(cell);

    }

    void handleSelection(CCombo combo, WorkflowNode workflowNode) {

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

        if (!exactMatch) {
            combo.setForeground(ColorPalette.getInstance().getWarningColor());
        } else {

            combo.setForeground(ColorPalette.getInstance().getBlackColor());
        }

        nodesValid.put(wfNode, exactMatch);

        page.prepareErrorStatement();

        if (iWizard.getContainer().getCurrentPage() != null) {
            iWizard.getContainer().updateButtons();
        }
    }

    void clearComboList() {

        for (CCombo combo : comboList) {

            if (combo.getData(TableBehaviour.EDITOR) instanceof TableEditor) {

                WorkflowNode node = (WorkflowNode) combo.getData();
                if (combo.getForeground().equals(ColorPalette.getInstance().getWarningColor())) {
                    nodeValueWithError.put(node, combo.getSelectionIndex());
                } else {
                    if (nodeValueWithError.containsKey(node)) {
                        nodeValueWithError.remove(node);
                    }

                }
                TableEditor editor = (TableEditor) combo.getData(TableBehaviour.EDITOR);
                editor.dispose();
            } else {

                TreeNode node = (TreeNode) combo.getData();
                if (combo.getForeground().equals(ColorPalette.getInstance().getWarningColor())) {
                    nodeValueWithError.put(node.getWorkflowNode(), combo.getSelectionIndex());
                } else {
                    if (nodeValueWithError.containsKey(node.getWorkflowNode())) {
                        nodeValueWithError.remove(node.getWorkflowNode());
                    }

                }
                TreeEditor editor = (TreeEditor) combo.getData(TableBehaviour.EDITOR);
                editor.dispose();

            }

            combo.dispose();
        }
        comboList.clear();

    }

    /**
     * Checks if Map contains invalid nodes (i.e used for error message).
     * 
     * @return true if all nodes are valid
     */
    public boolean areNodesValid() {
        return !nodesValid.values().contains(Boolean.FALSE);
    }

    public Map<WorkflowNode, Boolean> getNodesValidList() {
        return nodesValid;
    }

    /**
     * Disposes all images used in in the table (called from WorkflowPage's dispose).
     */
    public void disposeRescources() {

        for (Resource resource : resources) {
            resource.dispose();
        }

    }

    public List<CCombo> getComboList() {
        return comboList;
    }

    /**
     * Getter for the IWizard to update the buttons.
     * 
     * @return {@link IWizard}
     */
    public IWizard getiWizard() {
        return iWizard;
    }

    public TargetNodeEditingSupport getEditingSupport() {
        return editingSupport;
    }

    public void setUpdater(Updatable updater) {
        this.updater = updater;
    }

    public Set<Resource> getResources() {
        return resources;
    }

    public WorkflowPage getPage() {
        return page;
    }

    public void setPage(WorkflowPage page) {
        this.page = page;
    }

    public WorkflowDescription getWorkflowDescription() {
        return workflowDescription;
    }

    public Map<WorkflowNode, Integer> getNodeValueWithError() {
        return nodeValueWithError;
    }


}
