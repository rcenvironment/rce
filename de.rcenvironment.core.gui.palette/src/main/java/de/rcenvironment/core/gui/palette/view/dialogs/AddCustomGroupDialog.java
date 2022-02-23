/*
 * Copyright 2021-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.palette.view.dialogs;

import java.util.List;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
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
import de.rcenvironment.core.gui.palette.view.PaletteViewContentProvider;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.GroupNode;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.PaletteTreeNode;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Dialog for adding custom (sub)groups to the PaletteView.
 * 
 * @author Kathrin Schaffert
 *
 */
public class AddCustomGroupDialog extends TitleAreaDialog {

    private final Log log = LogFactory.getLog(getClass());

    private PaletteViewContentProvider contentProvider;

    private List<PaletteTreeNode> currentGroups;

    private PaletteTreeNode parentNode;

    private String text;

    private Text addGroupText;

    private boolean groupAdded = false;

    public AddCustomGroupDialog(Shell parentShell, PaletteViewContentProvider contentProvider, PaletteTreeNode parentNode,
        boolean isSubgroupDialog) {
        super(parentShell);
        this.contentProvider = contentProvider;
        this.currentGroups = parentNode.getSubGroups();
        this.parentNode = parentNode;
        if (isSubgroupDialog) {
            this.text = "Add subgroup to custom group '" + parentNode.getNodeName() + "'";
        } else {
            this.text = "Add custom top level group";
        }
        setDialogHelpAvailable(false);
    }

    @Override
    public void create() {
        super.create();
        setTitle("Add Group Dialog");
        setMessage(text);
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
        label.setText("Set group name:");

        addGroupText = new Text(content, SWT.BORDER);
        GridData addGroupGridData = new GridData(GridData.FILL_HORIZONTAL);
        addGroupText.setLayoutData(addGroupGridData);
        addGroupText.setEditable(true);
        addGroupText.addModifyListener(new AddGroupModifyListener());

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
        String groupString = addGroupText.getText().trim();
        GroupNode node = (GroupNode) contentProvider.createGroupNode(parentNode, groupString);
        node.setCustomGroup(true);
        groupAdded = true;
        if (parentNode.isRoot()) {
            log.debug(StringUtils.format("Created custom group '%s'.", groupString));
        } else {
            log.debug(StringUtils.format("Created subgroup '%s' in group '%s'.", groupString, parentNode.getQualifiedGroupName()));
        }
        super.okPressed();
    }

    public boolean isGroupAdded() {
        return groupAdded;
    }

    private class AddGroupModifyListener implements ModifyListener {

        @Override
        public void modifyText(ModifyEvent arg0) {
            setErrorMessage(null);
            String modifiedText = addGroupText.getText();
            GroupNameValidator validator = new GroupNameValidator(currentGroups, parentNode.isRoot());
            Optional<String> validationMessage = validator.valdiateText(modifiedText);
            getButton(OK).setEnabled(!validationMessage.isPresent());
            if (validationMessage.isPresent()) {
                setErrorMessage(validationMessage.get());
            }
        }
    }
}
