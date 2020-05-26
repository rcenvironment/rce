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

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TreeItem;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;

/**
 * Updater for a Tree: Implementation of {@link Updatable}.
 * 
 * @author Goekhan Guerkan
 */
public class TreeBehaviour extends AbstractUpdateBehavior {

    private TreeViewer treeViewer;

    private List<InstanceNodeSessionId> missingTargetInstancesList;

    public TreeBehaviour(TreeViewer tree, WorkflowNodeTargetPlatformLabelProvider
        instanceProvider, CheckboxLabelProvider checkboxProvider) {

        super(instanceProvider, checkboxProvider);

        this.treeViewer = tree;

    }

    @Override
    public void updateInstanceColumn(ViewerCell cell) {
        final TreeNode treeNodeCell = (TreeNode) cell.getElement();

        if (treeNodeCell.isChildElement()) {
            updateChild(cell);

        } else {
            updateFather(cell);

        }

        checkIfDisableMasterBtn();
        prepareValuesForMasterCombo();

    }

    @Override
    public void updateCheckBoxColumn(ViewerCell cell) {

        TreeNode node = (TreeNode) cell.getElement();
        TreeEditor editor = new TreeEditor(treeViewer.getTree());
        TreeItem item = (TreeItem) cell.getItem();
        final Button btn = new Button(treeViewer.getTree(), SWT.CHECK);
        synchronizeButtons();
        if (node.isChildElement()) {

            btn.setBackground(ColorPalette.getInstance().getFirstRowColor());
            if (node.getWorkflowNode().isChecked()) {
                btn.setSelection(true);
            } else {
                btn.setSelection(false);
            }

            if (editingSupport.getValues(node.getWorkflowNode()).size() <= 1) {
                btn.setEnabled(false);
            } else {
                btn.setEnabled(true);
            }
            btn.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {

                    saveIndexOfComboBeforeRefresh();
                    Button sourceButton = (Button) e.getSource();
                    TreeNode sourceNode = (TreeNode) sourceButton.getData(TableBehaviour.KEY_CHECK);
                    sourceNode.getWorkflowNode().setChecked(sourceButton.getSelection());

                    boolean selected = checkifSameNodeChildrenSelected(sourceNode);
                    TreeNode treeNodeFather = sourceNode.getFatherNode();

                    Button btnFather = findExactButtonForTreeNode(treeNodeFather);

                    if (selected) {
                        if (!btnFather.getSelection()) {
                            btnFather.setSelection(true);
                        }
                    } else {
                        if (btnFather.getSelection()) {
                            btnFather.setSelection(false);
                        }
                    }
                    if (!checkifAllChildrenSelected()) {

                        treeViewer
                            .getTree()
                            .getColumn(0)
                            .setImage(
                                ImageManager
                                    .getInstance()
                                    .getSharedImage(
                                        StandardImages.CHECK_UNCHECKED));
                        TableBehaviour.allCheckboxesClicked = false;

                    }
                    synchronizeButtons();
                    prepareValuesForMasterCombo();
                    checkIfDisableMasterBtn();

                    setSavedComboIndex();
                }
            });

        } else {

            btn.setBackground(ColorPalette.getInstance().getSecondRowColor());

            boolean checkedAllChildren = true;
            boolean allChildrenDisabled = true;
            for (TreeNode treeNode : node.getChildrenNodes()) {

                if (!treeNode.getWorkflowNode().isChecked()) {
                    checkedAllChildren = false;
                }

                if (editingSupport.getValues(treeNode.getWorkflowNode()).size() > 1) {
                    allChildrenDisabled = false;

                }

            }

            if (checkedAllChildren) {
                btn.setSelection(true);
            } else {
                btn.setSelection(false);
            }

            if (allChildrenDisabled) {
                btn.setEnabled(false);
            }

            btn.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    saveIndexOfComboBeforeRefresh();
                    Button b = (Button) e.getSource();
                    TreeNode treeNode = (TreeNode) b.getData(KEY_CHECK);

                    for (TreeNode node : treeNode.getChildrenNodes()) {
                        node.getWorkflowNode().setChecked(b.getSelection());
                    }
                    for (Button button : btnList) {
                        TreeNode treeN = (TreeNode) button.getData(KEY_CHECK);
                        if (treeN.isChildElement()) {
                            button.setSelection(treeN.getWorkflowNode().isChecked());
                        }
                    }

                    if (!checkifAllChildrenSelected()) {
                        treeViewer
                            .getTree()
                            .getColumn(0)
                            .setImage(
                                ImageManager
                                    .getInstance()
                                    .getSharedImage(
                                        StandardImages.CHECK_UNCHECKED));

                        TableBehaviour.allCheckboxesClicked = false;

                    }
                    prepareValuesForMasterCombo();
                    checkIfDisableMasterBtn();
                    setSavedComboIndex();
                }

            });

        }
        btnList.add(btn);
        btn.setData(KEY_CHECK, node);
        btn.setData(EDITOR, editor);

        editor.grabHorizontal = true;
        editor.setEditor(btn, item, 0);

        isCheckBoxColumnEnabled();
        if (!columnImageActive) {
            treeViewer.getTree().getColumn(0).setImage(checkDisabled);
        } else {
            treeViewer.getTree().getColumn(0).setImage(uncheckedImg);

        }

    }

    private void updateChild(final ViewerCell cell) {

        final TreeNode treeNodeCell = (TreeNode) cell.getElement();

        final CCombo combo = new CCombo(treeViewer.getTree(), SWT.READ_ONLY);

        final TreeItem item = (TreeItem) cell.getItem();
        TreeEditor editor = new TreeEditor(treeViewer.getTree());
        editor.grabHorizontal = true;
        editor.horizontalAlignment = SWT.BEGINNING;

        Color firstRow = ColorPalette.getInstance().getFirstRowColor();

        cell.getViewerRow().setBackground(0, firstRow);
        cell.getViewerRow().setBackground(1, firstRow);
        cell.getViewerRow().setBackground(2, firstRow);

        combo.setData(treeNodeCell);
        combo.setData(EDITOR, editor);
        combo.setBackground(firstRow);

        combo.addListener(SWT.Resize, new Listener() {

            @Override
            public void handleEvent(final Event argEvent) {
                if (combo != null) {
                    combo.setText(combo.getText());
                }
            }
        });

        if (!comboList.contains(combo)) {
            comboList.add(combo);
        }
        editor.setEditor(combo, item, 2);

        treeNodeCell.setCombo(combo);
        if (editingSupport.getValues(treeNodeCell.getWorkflowNode()).size() > 0) {

            for (String value : editingSupport.getValues(treeNodeCell.getWorkflowNode())) {
                combo.add(value);
            }

        } else {

            combo.setEnabled(false);
            combo.setText("No target instance available");
        }
        final Integer selectionIndex = (Integer) editingSupport
            .getValue(treeNodeCell.getWorkflowNode());

        if (selectionIndex != null) {
            combo.select(selectionIndex);

        } else {
            // default selection is the first available element
            combo.select(0);
        }

        if (editingSupport.getHasVersionErrorMap().containsKey(treeNodeCell.getWorkflowNode())) {
            if (editingSupport.getHasVersionErrorMap().get(treeNodeCell.getWorkflowNode())) {
                combo.select(0);

            }
        }

        instanceProvider.handleSelection(combo, treeNodeCell.getWorkflowNode());

        checkForSameSelection(treeNodeCell.getFatherNode());

        combo.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {

                instanceProvider.handleSelection(combo, treeNodeCell.getWorkflowNode());
                treeNodeCell.getFatherNode().getCombo().setText(Messages.combomultiple);
                instanceProvider.getPage().prepareErrorStatement();

                // check if children have the same selection

                checkForSameSelection(treeNodeCell.getFatherNode());

            }

        });

    }

    private void checkForSameSelection(TreeNode father) {

        List<TreeNode> children = father.getChildrenNodes();

        boolean noChildrenWithDifferentInst = false;

        List<CCombo> combos = new ArrayList<CCombo>();

        for (TreeNode treeNode : children) {
            if (treeNode.getCombo() != null) {
                combos.add(treeNode.getCombo());

            }
        }

        if (combos.size() == 1) {
            noChildrenWithDifferentInst = true;
        } else {

            for (int i = 0; i < combos.size() - 1; i++) {

                if (combos.get(i).getSelectionIndex() != combos.get(i + 1).getSelectionIndex()) {
                    noChildrenWithDifferentInst = false;
                    break;
                }

                noChildrenWithDifferentInst = true;
            }
        }
        if (noChildrenWithDifferentInst) {

            LogicalNodeId nodeId = father.getChildrenNodes().get(0).getWorkflowNode().getComponentDescription().getNode();
            father.getCombo().setText(nodeId.getAssociatedDisplayName());

            if (missingTargetInstancesList != null) {

                for (InstanceNodeSessionId missingID : missingTargetInstancesList) {

                    String temp = missingID.toString();
                    String missing = temp.substring(1, temp.indexOf(Messages.bracket) - 2);

                    if (father.getCombo().getText().contains(missing)) {
                        father.getCombo().setText("Contains missing instance");
                    }

                }
            }
        } else {

            father.getCombo().setText(Messages.combomultiple);

        }

    }

    private void updateFather(ViewerCell cell) {

        final TreeNode treeNodeCell = (TreeNode) cell.getElement();
        final CCombo combo = new CCombo(treeViewer.getTree(), SWT.DROP_DOWN);

        final TreeItem item = (TreeItem) cell.getItem();
        TreeEditor editor = new TreeEditor(treeViewer.getTree());
        editor.grabHorizontal = true;
        editor.horizontalAlignment = SWT.BEGINNING;

        Color secondRow = ColorPalette.getInstance().getSecondRowColor();

        cell.getViewerRow().setBackground(0, secondRow);
        cell.getViewerRow().setBackground(1, secondRow);
        cell.getViewerRow().setBackground(2, secondRow);

        combo.setData(treeNodeCell);
        combo.setData(EDITOR, editor);
        combo.setBackground(secondRow);
        combo.setEditable(false);

        combo.addListener(SWT.Resize, new Listener() {

            @Override
            public void handleEvent(final Event argEvent) {

                if (combo != null) {
                    combo.setText(combo.getText());
                }
            }
        });

        if (!comboList.contains(combo)) {
            comboList.add(combo);
        }
        editor.setEditor(combo, item, 2);
        treeNodeCell.setCombo(combo);
        for (String value : editingSupport.getValues(treeNodeCell.getChildrenNodes().get(0).getWorkflowNode())) {
            combo.add(value);
        }
        combo.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {

                CCombo comboSource = (CCombo) e.getSource();

                TreeNode cell = (TreeNode) comboSource.getData();
                for (TreeNode treeNode : cell.getChildrenNodes()) {

                    if (treeNode.getCombo() != null) {

                        treeNode.getCombo().select(combo.getSelectionIndex());
                        instanceProvider.handleSelection(treeNode.getCombo(), treeNode.getWorkflowNode());
                    }
                }

            }

        });

        boolean noAvailableChildren = true;
        boolean allChildrenDisabled = true;

        for (TreeNode treeNode : treeNodeCell.getChildrenNodes()) {

            int size = editingSupport.getValues(treeNode.getWorkflowNode()).size();

            if (size > 0) {
                noAvailableChildren = false;
            }

            if (size > 1) {
                allChildrenDisabled = false;
            }
        }

        if (noAvailableChildren) {

            combo.setEnabled(false);
            combo.setText("No instances available");
        }

        if (allChildrenDisabled) {
            combo.setEnabled(false);
        }
    }

    @Override
    public void refreshColumns() {

        treeViewer.getTree().removeAll();

        checkProvider.clearButtonList();
        instanceProvider.clearComboList();

        treeViewer.getTree().setRedraw(false);
        treeViewer.refresh();

        treeViewer.expandAll();

        if (columnImageActive) {
            if (TableBehaviour.allCheckboxesClicked) {
                treeViewer.getTree().getColumn(0).setImage(ImageManager.getInstance().getSharedImage(StandardImages.CHECK_CHECKED));

            } else {

                treeViewer.getTree().getColumn(0).setImage(ImageManager.getInstance().getSharedImage(StandardImages.CHECK_UNCHECKED));

            }
        }

        treeViewer.getTree().setRedraw(true);
        treeViewer.getTree().redraw();
        treeViewer.getTree().update();

        checkIfDisableMasterBtn();

    }

    @Override
    public boolean useFilter(String filterText, Object element) {

        TreeNode node = (TreeNode) element;

        if (filterText == null) {
            return true;
        }

        if (filterText.equals("")) {
            return true;
        }

        String filterTextSmall = filterText.toLowerCase();

        String targetInstance;
        String componentName;

        if (node.isChildElement()) {
            componentName = node.getWorkflowNode().getName().toLowerCase();
            String temp = node.getWorkflowNode().getComponentDescription().getNode().toString().toLowerCase();
            targetInstance = temp.replaceFirst("\"", "");
        } else {

            if (!checkifShowFatherInstance(node, filterTextSmall)) {
                return false;
            }
            return true;
        }

        if (targetInstance.contains(filterTextSmall) || componentName.contains(filterTextSmall)) {
            return true;
        }

        return false;

    }

    private boolean checkifShowFatherInstance(TreeNode node, String filterTextSmall) {

        boolean hasChildinFilter = false;
        for (TreeNode treeNode : node.getChildrenNodes()) {

            String temp = treeNode.getWorkflowNode().getComponentDescription().getNode().toString().toLowerCase();
            String instanceName = temp.replaceFirst("\"", "");
            String componentName = treeNode.getWorkflowNode().getName().toLowerCase();

            if (instanceName.contains(filterTextSmall) || componentName.contains(filterTextSmall)) {
                hasChildinFilter = true;
                break;
            } else {
                hasChildinFilter = false;

            }

        }

        return hasChildinFilter;

    }

    private Button findExactButtonForTreeNode(TreeNode treeNodeFather) {

        for (Button button : btnList) {

            TreeNode node = (TreeNode) button.getData(KEY_CHECK);

            if (!node.isChildElement()) {

                if (treeNodeFather.getComponentName().equals(node.getComponentName())) {
                    return button;
                }

            }

        }
        // should not happen
        return null;
    }

    // check if all other children of the same father node are selected and update checkbox
    private boolean checkifSameNodeChildrenSelected(TreeNode sourceNode) {
        boolean areChildrenSelected = true;
        for (TreeNode otherchildNode : sourceNode.getFatherNode().getChildrenNodes()) {

            if (!otherchildNode.getWorkflowNode()
                .isChecked()) {

                areChildrenSelected = false;
            }

        }

        return areChildrenSelected;

    }

    private boolean checkifAllChildrenSelected() {
        boolean isAllSelected = true;

        List<Button> allEnabledBtns = new ArrayList<Button>();

        for (Button b : btnList) {

            if (b.isEnabled()) {
                allEnabledBtns.add(b);
            }
        }

        for (Button button : allEnabledBtns) {

            TreeNode temp = (TreeNode) button.getData(KEY_CHECK);

            if (temp.isChildElement()) {
                if (!temp.getWorkflowNode().isChecked()) {
                    isAllSelected = false;
                }
            }

        }
        if (columnImageActive) {
            if (isAllSelected && btnList.size() > 0) {
                TableBehaviour.allCheckboxesClicked = true;
                treeViewer.getTree().getColumn(0).setImage(ImageManager.getInstance().getSharedImage(StandardImages.CHECK_CHECKED));
            }
        }
        return isAllSelected;

    }

    private void synchronizeButtons() {

        for (Button b : btnList) {

            TreeNode node = (TreeNode) b.getData(KEY_CHECK);
            if (node.isChildElement()) {

                b.setSelection(node.getWorkflowNode().isChecked());

            }

        }
    }

    public void setMissingTargetInstancesList(List<InstanceNodeSessionId> missingTargetInstancesList) {
        this.missingTargetInstancesList = missingTargetInstancesList;
    }

}
