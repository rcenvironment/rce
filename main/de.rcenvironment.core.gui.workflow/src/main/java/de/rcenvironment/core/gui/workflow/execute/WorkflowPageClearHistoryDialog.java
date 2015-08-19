/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.execute;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import de.rcenvironment.core.component.model.configuration.api.PlaceholdersMetaDataDefinition;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowPlaceholderHandler;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.Activator;


/**
 * Dialog for clearing placeholder history.
 * 
 * @author Sascha Zur
 */
public class WorkflowPageClearHistoryDialog extends Dialog{

    private String title;

    private WorkflowPlaceholderHandler weph;

    private WorkflowDescription wd;

    private Map<String, String> guiNameToPlaceholder;

    private Tree componentPlaceholderTree;

    private Composite container;

    private Button deleteAllPasswordHistories;

    protected WorkflowPageClearHistoryDialog(Shell parentShell, String title, WorkflowPlaceholderHandler pd, 
        WorkflowDescription workflowDescription) {
        super(parentShell);
        this.title = title;
        this.weph = pd;
        this.wd = workflowDescription;
        guiNameToPlaceholder = new HashMap<String, String>();
        setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX
            | SWT.APPLICATION_MODAL); 
    }

    @Override
    protected Control createDialogArea(final Composite parent) {

        container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(1, false));
        GridData containergridData = new GridData(GridData.FILL, GridData.FILL,
            true, true); 
        container.setLayoutData(containergridData);


        componentPlaceholderTree = new Tree(container, SWT.MULTI | SWT.CHECK);

        componentPlaceholderTree.setLayoutData(containergridData);
        componentPlaceholderTree.setHeaderVisible(false);
        componentPlaceholderTree.setLinesVisible(true);

        fillTree();

        // resize the row height using a MeasureItem listener
        componentPlaceholderTree.addListener(SWT.MeasureItem, new Listener() {
            @Override
            public void handleEvent(Event event) {
                event.height = 2 * 10;
            }
        });
        Listener listener = new Listener() {
            @Override
            public void handleEvent(Event e) {
                final TreeItem treeItem = (TreeItem) e.item;
                parent.getDisplay().asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        treeItem.getParent().getColumn(0).pack();

                    }
                });
            }
        };
        componentPlaceholderTree.addListener(SWT.Collapse, listener);
        componentPlaceholderTree.addListener(SWT.Expand, listener);
        componentPlaceholderTree.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                checkItems((TreeItem) event.item, ((TreeItem) event.item).getChecked());
            }

            private void checkItems(TreeItem item, boolean checked) {
                item.setChecked(checked);
                if (item.getItemCount() > 0){
                    for (TreeItem it : item.getItems()){
                        checkItems(it, checked);
                    }
                }
            }
        });
        deleteAllPasswordHistories = new Button(container, SWT.CHECK);
        deleteAllPasswordHistories.setText("Delete ALL password histories");
        return container;
    }

    private void clearHistory(TreeItem itComp, String parent, boolean isGlobal) {
        for (WorkflowNode wn : wd.getWorkflowNodes()){
            if (wn.getComponentDescription().getName().equals(itComp.getParentItem().getText())){
                weph.deletePlaceholderHistory(wn.getComponentDescription().getIdentifier(), guiNameToPlaceholder.get(itComp.getText()));
            }
        }
    }

    private void fillTree() {
        TreeColumn column1 = new TreeColumn(componentPlaceholderTree, SWT.LEFT);
        column1.setText("");
        Set<String> componentTypesWithPlaceholder = weph.getIdentifiersOfPlaceholderContainingComponents();
        String[] componentTypesWithPlaceholderArray = 
            componentTypesWithPlaceholder.toArray(new String[componentTypesWithPlaceholder.size()]);
        Arrays.sort(componentTypesWithPlaceholderArray);
        for (String componentID : componentTypesWithPlaceholderArray){
            TreeItem componentIDTreeItem = new TreeItem(componentPlaceholderTree, 0);
            String componentName = wd.getWorkflowNode(weph.getComponentInstances(componentID).get(0))
                .getComponentDescription().getName();
            componentIDTreeItem.setText(0, componentName);
            componentIDTreeItem.setImage(getImage(
                wd.getWorkflowNode(weph.getComponentInstances(componentID).get(0))));
            PlaceholdersMetaDataDefinition metaData = getPlaceholderAttributes(componentName);
            componentIDTreeItem.setExpanded(true);

            if (weph.getPlaceholderNameSetOfComponentID(componentID) != null){  
                List <String> globalPlaceholderOrder = 
                    PlaceholderSortUtils.getPlaceholderOrder(weph.getPlaceholderNameSetOfComponentID(componentID), metaData);
                if (globalPlaceholderOrder == null){
                    globalPlaceholderOrder = new LinkedList <String>();
                }
                for (String componentPlaceholder : globalPlaceholderOrder){
                    TreeItem compPHTreeItem = new TreeItem(componentIDTreeItem, 0);
                    String guiName = metaData.getGuiName(componentPlaceholder);
                    guiNameToPlaceholder.put(guiName, componentPlaceholder);
                    compPHTreeItem.setText(0, guiName);
                    compPHTreeItem.setExpanded(true);
                }
            }
            if (weph.getComponentInstances(componentID) != null){
                List<String> instancesWithPlaceholder = weph.getComponentInstances(componentID);
                instancesWithPlaceholder = PlaceholderSortUtils.sortInstancesWithPlaceholderByName(instancesWithPlaceholder, wd);
                if (instancesWithPlaceholder != null){
                    String compInstances = instancesWithPlaceholder.get(0);
                    Set<String> unsortedInstancePlaceholder = weph.getPlaceholderNameSetOfComponentInstance(compInstances);
                    List<String> sortedInstancePlaceholder = 
                        PlaceholderSortUtils.getPlaceholderOrder(unsortedInstancePlaceholder, metaData);
                    for (String instancePlaceholder : sortedInstancePlaceholder){
                        TreeItem instancePHTreeItem = new TreeItem(componentIDTreeItem, 0);
                        String guiName = metaData.getGuiName(instancePlaceholder);
                        guiNameToPlaceholder.put(guiName, instancePlaceholder);
                        instancePHTreeItem.setText(0, guiName);
                        instancePHTreeItem.setExpanded(true);
                    }
                }
            }

        }
        column1.pack();
    }

    private PlaceholdersMetaDataDefinition getPlaceholderAttributes(String name){
        for (WorkflowNode wn : wd.getWorkflowNodes()){
            if (wn.getComponentDescription().getName().equals(name)){
                return wn.getComponentDescription().getConfigurationDescription()
                    .getComponentConfigurationDefinition().getPlaceholderMetaDataDefinition();
            }
        }
        return null;
    }

    private Image getImage(WorkflowNode element){
        byte[] icon = element.getComponentDescription().getIcon16();
        Image image;
        if (icon != null) {
            image = new Image(Display.getCurrent(), new ByteArrayInputStream(icon));
        } else {
            image = Activator.getInstance().getImageRegistry().get(Activator.IMAGE_RCE_ICON_16);
        }
        return image;
    }

    @Override
    public void create() {
        super.create();
        // dialog title
        getShell().setText(title);
        for (TreeItem it1 : componentPlaceholderTree.getItems()){
            expandItem(it1);
        }

        this.getShell().pack();
        this.getShell().setSize(this.getShell().getSize().x, this.getShell().getSize().y + 3 * 10);
        componentPlaceholderTree.getColumn(0).setWidth(container.getSize().x - 5 * 2);
    } 

    private void expandItem(TreeItem it1) {
        it1.setExpanded(true);
        if (it1.getItems().length > 0){
            for (TreeItem it2 : it1.getItems()){
                expandItem(it2);
            }
        }

    }

    @Override
    protected void okPressed() {

        for (TreeItem it : componentPlaceholderTree.getItems()){
            for (TreeItem itComp : it.getItems()){
                if (itComp.getChecked()){
                    clearHistory(itComp, it.getText(), true);
                }
            }
        }
        if (deleteAllPasswordHistories.getSelection()){
            weph.deleteAllPasswordHistories();
        }
        super.okPressed();
    }

    @Override
    protected Button createButton(Composite parent, int id,
        String label, boolean defaultButton) {
        if (id == IDialogConstants.OK_ID){
            label = Messages.clear;
        }
        return super.createButton(parent, id, label, defaultButton);
    }


}
