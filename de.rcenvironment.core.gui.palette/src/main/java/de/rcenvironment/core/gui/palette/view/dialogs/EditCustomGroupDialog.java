/*
 * Copyright 2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.palette.view.dialogs;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.TreeNode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.core.gui.palette.GroupNameValidator;
import de.rcenvironment.core.gui.palette.ToolGroupAssignment;
import de.rcenvironment.core.gui.palette.toolidentification.ToolIdentification;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.ComponentNode;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.PaletteTreeNode;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Dialog for editing custom groups of the PaletteView.
 * 
 * @author Kathrin Schaffert
 *
 */
public class EditCustomGroupDialog extends TitleAreaDialog {

    private final Log log = LogFactory.getLog(getClass());

    private ToolGroupAssignment assignment;

    private PaletteTreeNode groupNode;

    private String previousGroupString;

    private List<PaletteTreeNode> currentGroups;

    private Text editGroupText;

    private boolean groupUpdated = false;


    public EditCustomGroupDialog(Shell parentShell, PaletteTreeNode groupNode, ToolGroupAssignment assignment) {
        super(parentShell);
        this.assignment = assignment;
        this.groupNode = groupNode;
        this.currentGroups = groupNode.getPaletteParent().getSubGroups();
        this.previousGroupString = groupNode.getNodeName();
    }

    @Override
    public void create() {
        super.create();
        setTitle("Edit Group Dialog");
        setMessage(StringUtils.format("Update name of custom group '%s'", groupNode.getNodeName()));
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
        GridData contentData = new GridData(GridData.FILL_HORIZONTAL);
        content.setLayoutData(contentData);

        CLabel label = new CLabel(content, SWT.NONE);
        label.setText("Set Group Name:");

        editGroupText = new Text(content, SWT.BORDER);
        GridData editGroupGridData = new GridData(GridData.FILL_HORIZONTAL);
        editGroupText.setLayoutData(editGroupGridData);
        editGroupText.setEditable(true);
        editGroupText.setText(previousGroupString);
        editGroupText.setSelection(0, editGroupText.getText().length());
        editGroupText.addModifyListener(new EditGroupModifyListener());

        return container;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        getButton(OK).setEnabled(false);
    }

    @Override
    protected void okPressed() {
        String groupString = editGroupText.getText().trim();
        if (!groupString.equals(previousGroupString)) {
            String previousPath = groupNode.getQualifiedGroupName();
            groupNode.setNodeName(groupString);
            if (groupNode.hasChildren()) {
                updateCustomizedAssignments(groupNode.getChildren(), groupNode.getQualifiedGroupName());
                groupNode.getAllSubGroups().stream().filter(TreeNode::hasChildren)
                    .forEach(node -> updateCustomizedAssignments(node.getChildren(), node.getQualifiedGroupName()));
            }
            groupUpdated = true;
            log.debug(StringUtils.format("Renamed group '%s' to '%s'", previousPath, groupNode.getQualifiedGroupName()));
        }
        super.okPressed();
    }

    private void updateCustomizedAssignments(TreeNode[] treeNodes, String qualifiedGroupString) {
        String[] groupPath = assignment.createPathArray(qualifiedGroupString);
        Map<ToolIdentification, String[]> customizedAssignments = assignment.getCustomizedAssignments();
        Arrays.stream(treeNodes).filter(ComponentNode.class::isInstance).map(ComponentNode.class::cast).forEach(node -> {
            updateAssignmentMap(customizedAssignments, node.getToolIdentification().getToolID(),
                groupPath);
        });
        assignment.setCustomizedAssignments(customizedAssignments);
    }

    private void updateAssignmentMap(Map<ToolIdentification, String[]> customizedAssignments, String installationId,
        String[] groupPath) {
        for (Entry<ToolIdentification, String[]> entry : customizedAssignments.entrySet()) {
            if (entry.getKey().getToolID().equals(installationId)) {
                entry.setValue(groupPath);
                break;
            }
        }
    }

    public boolean isGroupUpdated() {
        return groupUpdated;
    }

    private class EditGroupModifyListener implements ModifyListener {

        @Override
        public void modifyText(ModifyEvent arg0) {
            setErrorMessage(null);
            String modifiedText = editGroupText.getText();
            if (modifiedText.equals(previousGroupString)) {
                getButton(OK).setEnabled(false);
                return;
            }
            GroupNameValidator validator = new GroupNameValidator(currentGroups, groupNode.getPaletteParent().isRoot());
            Optional<String> validationMessage = validator.valdiateText(modifiedText);
            getButton(OK).setEnabled(!validationMessage.isPresent());
            if (validationMessage.isPresent()) {
                setErrorMessage(validationMessage.get());
            }
        }
    }

}
