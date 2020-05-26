/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.execute;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;

import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;

/**
 * Default Implementation of the Interface. Implements common functions of Tree and Table.
 * 
 * @author Goekhan Guerkan
 *
 */
public abstract class AbstractUpdateBehavior implements Updatable {

    /**
     * One boolean for both table types.
     */
    public static boolean allCheckboxesClicked = false;

    protected boolean columnImageActive = true;

    protected List<CCombo> comboList;

    protected List<Button> btnList;

    protected WorkflowNodeTargetPlatformLabelProvider instanceProvider;

    protected TargetNodeEditingSupport editingSupport;

    protected CheckboxLabelProvider checkProvider;

    protected CCombo masterCombo;

    protected boolean noCheckboxSelected = true;

    protected Button masterButton;

    protected Image uncheckedImg;

    protected Image checkDisabled;

    protected String savedIndex;

    public AbstractUpdateBehavior(WorkflowNodeTargetPlatformLabelProvider
        instanceProvider, CheckboxLabelProvider checkboxProvider) {

        this.btnList = checkboxProvider.getBtnList();
        this.instanceProvider = instanceProvider;
        this.comboList = instanceProvider.getComboList();
        this.editingSupport = instanceProvider.getEditingSupport();
        this.checkProvider = checkboxProvider;

        uncheckedImg = ImageManager.getInstance().getSharedImage(StandardImages.CHECK_UNCHECKED);
        checkDisabled = ImageManager.getInstance().getSharedImage(StandardImages.CHECK_DISABLED);

    }

    @Override
    public void setComboColor(Color color) {}

    @Override
    public void disposeWidgets() {
        checkProvider.clearButtonList();
        instanceProvider.clearComboList();
    }

    void prepareValuesForMasterCombo() {

        // find the combo with most values.
        if (comboList != null && comboList.get(0) != null && masterCombo != null) {

            List<Button> enabledButtons = new ArrayList<Button>();
            for (Button b : btnList) {

                if (b.isEnabled() && b.getSelection()) {
                    enabledButtons.add(b);

                }
            }
            if (enabledButtons.isEmpty()) {
                enableOrDisableMasterCombo();
                return;

            }

            WorkflowNode node = null;
            if (enabledButtons.get(0).getData(KEY_CHECK) instanceof WorkflowNode) {
                node = (WorkflowNode) enabledButtons.get(0).getData(KEY_CHECK);
                setComboValues(node, enabledButtons);
            } else {
                TreeNode treeNode = (TreeNode) enabledButtons.get(0).getData(KEY_CHECK);
                if (treeNode.isChildElement()) {
                    node = treeNode.getWorkflowNode();

                    setComboValues(node, enabledButtons);

                } else {

                    for (TreeNode n : treeNode.getChildrenNodes()) {

                        WorkflowNode wfNode = n.getWorkflowNode();
                        setComboValues(wfNode, enabledButtons);

                    }
                }
            }

            enableOrDisableMasterCombo();

        }
    }

    private void setComboValues(WorkflowNode node, List<Button> enabledButtons) {

        if (node != null) {
            List<String> commonValues = editingSupport.getValues(node);

            WorkflowNode enabledNode = null;

            for (int i = 1; i < enabledButtons.size(); i++) {
                if (enabledButtons.get(i).getData(KEY_CHECK) instanceof WorkflowNode) {
                    enabledNode = (WorkflowNode) enabledButtons.get(i).getData(KEY_CHECK);

                } else {
                    TreeNode n = (TreeNode) enabledButtons.get(i).getData(KEY_CHECK);
                    if (n.isChildElement()) {
                        enabledNode = n.getWorkflowNode();

                    }

                }

                if (enabledNode != null) {
                    List<String> values = editingSupport.getValues(enabledNode);
                    commonValues.retainAll(values);
                }

            }

            Set<String> treeSet = new TreeSet<String>();

            for (String item : commonValues) {
                treeSet.add(item);
            }

            commonValues.clear();
            treeSet.addAll(commonValues);

            masterCombo.removeAll();

            for (String string : treeSet) {
                masterCombo.add(string);
            }

            boolean containsSetAnyAlready = false;

            for (int i = 0; i < masterCombo.getItemCount(); i++) {

                if (masterCombo.getItem(i).equals(Messages.anyRemote)) {
                    containsSetAnyAlready = true;
                    break;
                }
            }

            if (!containsSetAnyAlready) {
                masterCombo.add(Messages.anyRemote);
            }
        }

    }

    private void enableOrDisableMasterCombo() {

        masterCombo.select(0);

        if (noCheckboxSelected) {
            masterCombo.setEnabled(false);
        }

    }

    /**
     * Sets the apply Instance Button to disabled if no checkbox is clicked.
     * 
     * @return true if widgets need to be disabled.
     */
    protected void checkIfDisableMasterBtn() {

        if (masterButton != null) {

            boolean isThereAnyButtonChecked = false;

            for (Button btn : btnList) {

                if (btn.getSelection()) {
                    isThereAnyButtonChecked = true;
                    noCheckboxSelected = false;
                    break;
                } else {

                    noCheckboxSelected = true;
                }

            }

            if (!isThereAnyButtonChecked) {
                masterButton.setEnabled(false);
                masterCombo.setEnabled(false);
            } else {
                masterCombo.setEnabled(true);
                masterButton.setEnabled(true);
            }

        }
    }

    protected boolean isCheckBoxColumnEnabled() {

        for (Button b : btnList) {

            if (b.isEnabled()) {
                columnImageActive = true;
                return true;
            }

        }
        columnImageActive = false;
        return false;

    }

    public void setMasterCombo(CCombo masterCombo) {
        this.masterCombo = masterCombo;
    }

    public void setMasterButton(Button masterButton) {
        this.masterButton = masterButton;
    }

    /**
     * Saves the current selection index, before refresh is invoked.
     */
    public void saveIndexOfComboBeforeRefresh() {

        savedIndex = masterCombo.getText();
    }

    /**
     * Sets the saved index.
     */

    public void setSavedComboIndex() {

        for (int i = 0; i < masterCombo.getItemCount(); i++) {
            if (masterCombo.getItem(i).equals(savedIndex)) {

                masterCombo.select(i);
                return;
            }
        }
    }

}
