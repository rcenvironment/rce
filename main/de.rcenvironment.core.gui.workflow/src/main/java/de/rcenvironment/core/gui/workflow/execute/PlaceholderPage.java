/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.execute;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDescription;
import de.rcenvironment.core.component.model.configuration.api.PlaceholdersMetaDataConstants;
import de.rcenvironment.core.component.model.configuration.api.PlaceholdersMetaDataDefinition;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowPlaceholderHandler;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.utils.common.components.PropertyTabGuiHelper;
import de.rcenvironment.core.gui.utils.incubator.NumericalTextConstraintListener;
import de.rcenvironment.core.gui.utils.incubator.WidgetGroupFactory;
import de.rcenvironment.core.gui.workflow.Activator;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Wizard page for managing placeholders.
 * 
 * @author Sascha Zur
 * @author Brigitte Boden
 */
@SuppressWarnings("boxing")
public class PlaceholderPage extends WizardPage {

    private static final Color COLOR_WHITE = Display.getCurrent()
        .getSystemColor(SWT.COLOR_WHITE);

    private static final Color COLOR_RED = Display.getCurrent().getSystemColor(
        SWT.COLOR_RED);

    private static final int HUNDRED = 100;

    private static final Log LOGGER = LogFactory.getLog(PlaceholderPage.class);

    protected final WorkflowDescription workflowDescription;

    protected WorkflowPlaceholderHandler placeholderHelper;

    private Tree componentPlaceholderTree;

    // maps the hash code of a TreeItem to a control
    private Map<Integer, Control> controlMap;

    private final String dot = ".";

    private Map<Integer, Button> saveButtonMap;

    private boolean restoredPasswords = false;

    private Map<Integer, String> treeItemNameToPlaceholder;

    private Map<Integer, String> treeItemToUUIDMap;

    private Map<String, Set<String>> placeholderValidators;

    /**
     * The Constructor.
     */
    public PlaceholderPage(WorkflowDescription workflowDescription) {
        super(Messages.workflowPageName);
        this.workflowDescription = workflowDescription;
        setTitle(Messages.workflowPageTitle);
        setDescription(Messages.configure);

    }

    @Override
    public void createControl(Composite parent) {
        Composite comp = new Composite(parent, SWT.NONE);
        comp.setLayout(new GridLayout(1, false));
        setControl(comp);
        IPreferenceStore prefs = Activator.getInstance().getPreferenceStore();
        String placeholderPersistentSettingsUUID = prefs
            .getString(WorkflowPlaceholderHandler.PLACEHOLDER_PREFERENCES_KEY);
        if (placeholderPersistentSettingsUUID.isEmpty()) {
            placeholderPersistentSettingsUUID = UUID.randomUUID().toString();
            prefs.putValue(WorkflowPlaceholderHandler.PLACEHOLDER_PREFERENCES_KEY,
                placeholderPersistentSettingsUUID);
        }
        placeholderHelper = WorkflowPlaceholderHandler.createPlaceholderDescriptionFromWorkflowDescription(
            workflowDescription, placeholderPersistentSettingsUUID);
        addPlaceholderGroup(comp);

        Button clearHistoryButton = new Button(comp, SWT.NONE);
        clearHistoryButton.setText(Messages.clearHistoryButton);
        clearHistoryButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                WorkflowPageClearHistoryDialog chd = new WorkflowPageClearHistoryDialog(
                    getShell(), Messages.clearHistoryDialogTitle,
                    placeholderHelper, workflowDescription);
                chd.open();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });
        if (placeholderHelper.getIdentifiersOfPlaceholderContainingComponents()
            .size() == 0) {
            clearHistoryButton.setEnabled(false);
        }
    }

    private void addPlaceholderGroup(Composite container) {
        controlMap = new HashMap<>();
        saveButtonMap = new HashMap<>();
        // guiNameToPlaceholder = new HashMap<String, String>();
        treeItemNameToPlaceholder = new HashMap<>();
        treeItemToUUIDMap = new HashMap<>();
        Group placeholderInformationGroup = new Group(container, SWT.NONE);
        placeholderInformationGroup
            .setText(Messages.placeholderInformationHeader);
        placeholderInformationGroup.setLayout(new GridLayout(1, false));
        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.grabExcessVerticalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.grabExcessHorizontalSpace = true;
        placeholderInformationGroup.setLayoutData(gridData);

        componentPlaceholderTree = new Tree(placeholderInformationGroup, SWT.MULTI);

        componentPlaceholderTree.setLayoutData(gridData);
        componentPlaceholderTree.setHeaderVisible(false);
        componentPlaceholderTree.setLinesVisible(true);

        // resize the row height using a MeasureItem listener
        componentPlaceholderTree.addListener(SWT.MeasureItem, new Listener() {

            @Override
            public void handleEvent(Event event) {
                event.height = 2 * 10 + 2;
            }
        });

        fillTree();

        Listener listener = new Listener() {

            @Override
            public void handleEvent(Event e) {
                final TreeItem treeItem = (TreeItem) e.item;
                getShell().getDisplay().asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        treeItem.getParent().getColumn(0).pack();
                        treeItem.getParent()
                            .getColumn(0)
                            .setWidth(
                                treeItem.getParent().getColumn(0)
                                    .getWidth() + 10);
                    }
                });
            }
        };
        componentPlaceholderTree.addListener(SWT.Collapse, listener);
        componentPlaceholderTree.addListener(SWT.Expand, listener);
        deleteEmptyTreeItems();
        openItems();
        componentPlaceholderTree.getColumn(0).pack();
        componentPlaceholderTree.getColumn(0).setWidth(
            componentPlaceholderTree.getColumn(0).getWidth() + 10);

    }

    private void deleteEmptyTreeItems() {
        for (TreeItem tItem : componentPlaceholderTree.getItems()) {
            int itemCount = 0;
            for (TreeItem childItem : tItem.getItems()) {
                itemCount++;
                if (childItem.getItemCount() > 0) {
                    itemCount += childItem.getItemCount();
                } else {
                    if (treeItemNameToPlaceholder.containsKey(tItem.hashCode())) {
                        itemCount++;
                    } else {
                        childItem.dispose();
                        itemCount--;
                    }
                }
            }
            if (itemCount <= 1) {
                tItem.dispose();
            }
        }
    }

    private void openItems() {
        for (TreeItem parent : componentPlaceholderTree.getItems()) {
            for (TreeItem secondLevel : parent.getItems()) {
                if (secondLevel.getItemCount() == 0) {
                    Control control = controlMap.get(secondLevel.hashCode());
                    if (control instanceof Text) {
                        Text current = (Text) control;
                        parent.setExpanded(true); // Always expand. Copy into if
                        // branch, if it
                        // should
                        // only open when nothing is in it
                        if ((current.getText().equals("") || current.getText() == null)) {
                            current.setBackground(COLOR_RED);
                        }
                    }
                } else {
                    for (TreeItem thirdLevel : secondLevel.getItems()) {
                        Control control = controlMap.get(thirdLevel.hashCode());
                        if (control instanceof Text) {
                            Text current = (Text) control;
                            parent.setExpanded(true); // Always expand. Copy
                                                      // into if branch, if it
                            // should only open when nothing is in it
                            secondLevel.setExpanded(true);
                            if (current.getText().equals("")
                                || current.getText() == null) {
                                current.setBackground(COLOR_RED);
                            }
                        } else if (control instanceof Combo) {
                            Combo current = (Combo) control;
                            parent.setExpanded(true);
                            secondLevel.setExpanded(true);
                            if (current.getText().equals("")
                                || current.getText() == null) {
                                current.setBackground(COLOR_RED);
                            }
                        }
                    }
                }
            }
        }
    }

    private void fillTree() {
        TreeColumn column1 = new TreeColumn(componentPlaceholderTree, SWT.LEFT);
        column1.setText("");
        TreeColumn column2 = new TreeColumn(componentPlaceholderTree, SWT.CENTER);
        column2.setText("");
        column2.setWidth(HUNDRED + 5);
        TreeColumn column3 = new TreeColumn(componentPlaceholderTree, SWT.CENTER);
        column3.setText("");
        column3.setWidth(HUNDRED / 2);
        TreeColumn column4 = new TreeColumn(componentPlaceholderTree, SWT.CENTER);
        column4.setText("");
        column4.setWidth(HUNDRED);
        placeholderValidators = new HashMap<>();

        Set<String> componentTypesWithPlaceholder = placeholderHelper.getIdentifiersOfPlaceholderContainingComponents();
        String[] componentTypesWithPlaceholderArray = componentTypesWithPlaceholder
            .toArray(new String[componentTypesWithPlaceholder.size()]);
        Arrays.sort(componentTypesWithPlaceholderArray);
        for (String componentID : componentTypesWithPlaceholderArray) {
            TreeItem componentIDTreeItem = new TreeItem(componentPlaceholderTree, 0);
            String componentName = workflowDescription.getWorkflowNode(placeholderHelper
                .getComponentInstances(componentID).get(0)).getName();
            String abstractComponentName = workflowDescription.getWorkflowNode(placeholderHelper
                .getComponentInstances(componentID).get(0)).getComponentDescription().getName();

            componentIDTreeItem.setText(0, abstractComponentName);
            componentIDTreeItem.setImage(getImage(workflowDescription
                .getWorkflowNode(placeholderHelper.getComponentInstances(componentID).get(0))));
            PlaceholdersMetaDataDefinition placeholderMetaData = getPlaceholderAttributes(componentName);
            List<String> globalPlaceholderOrder = PlaceholderSortUtils
                .getPlaceholderOrder(placeholderHelper.getPlaceholderNameSetOfComponentID(componentID),
                    placeholderMetaData);
            if (globalPlaceholderOrder == null) {
                globalPlaceholderOrder = new LinkedList<>();
            }
            for (String componentPlaceholder : globalPlaceholderOrder) {
                TreeItem compPHTreeItem = new TreeItem(componentIDTreeItem, 0);
                String guiName = placeholderMetaData.getGuiName(componentPlaceholder);
                if (guiName == null) {
                    guiName = "";
                }
                treeItemNameToPlaceholder.put(compPHTreeItem.hashCode(), componentPlaceholder);
                compPHTreeItem.setText(0, guiName);
                String currentPlaceholder = componentID + dot + componentPlaceholder;
                controlMap.put(compPHTreeItem.hashCode(),
                    addSWTHandler(compPHTreeItem, componentName + dot + componentPlaceholder, guiName, ComponentUtils
                        .isEncryptedPlaceholder(currentPlaceholder, WorkflowPlaceholderHandler.getEncryptedPlaceholder()), true));
            }
            List<String> instancesWithPlaceholder = placeholderHelper.getComponentInstances(componentID);
            instancesWithPlaceholder = PlaceholderSortUtils
                .sortInstancesWithPlaceholderByName(
                    instancesWithPlaceholder, workflowDescription);
            for (String compInstances : instancesWithPlaceholder) {
                ConfigurationDescription configDesc = workflowDescription.getWorkflowNode(compInstances).getComponentDescription()
                    .getConfigurationDescription();
                
                boolean hasPlaceholderWithGUIName = false;
                for (String instancePlaceholder : placeholderHelper.getPlaceholderNameSetOfComponentInstance(compInstances)) {
                    boolean isActivePlaceholder = WorkflowPlaceholderHandler.isActivePlaceholder(instancePlaceholder, configDesc);
                    if (isActivePlaceholder) {
                        if ((placeholderMetaData.getGuiName(instancePlaceholder) != null
                            && !placeholderMetaData.getGuiName(instancePlaceholder).isEmpty()
                            || placeholderMetaData.getGuiName("*") != null)) {
                            hasPlaceholderWithGUIName = true;
                        } else {
                            for (Entry<String, String> entry : configDesc.getConfiguration().entrySet()) {
                                if (ConfigurationDescription.isPlaceholder(entry.getValue())
                                    && WorkflowPlaceholderHandler.getNameOfPlaceholder(entry.getValue())
                                        .equals(instancePlaceholder)) {
                                    hasPlaceholderWithGUIName = true;
                                }
                            }
                            if (!hasPlaceholderWithGUIName) {
                                LOGGER
                                    .warn(StringUtils.format("Placeholder %s of component %s has no GUI name defined and will be ignored.",
                                        instancePlaceholder, workflowDescription.getWorkflowNode(compInstances)
                                            .getComponentDescription().getName()));
                            }
                        }
                    }
                }

                if (hasPlaceholderWithGUIName) {
                    TreeItem instanceTreeItem = new TreeItem(componentIDTreeItem, 0);
                    String instanceName = workflowDescription.getWorkflowNode(compInstances).getName();
                    instanceTreeItem.setText(0, instanceName);
                    instanceTreeItem.setImage(getImage(workflowDescription.getWorkflowNode(placeholderHelper
                        .getComponentInstances(componentID).get(0))));
                    List<String> orderedIinstancePlaceholder = PlaceholderSortUtils
                        .getPlaceholderOrder(placeholderHelper.getPlaceholderNameSetOfComponentInstance(compInstances),
                            placeholderMetaData);
                    for (String instancePlaceholder : orderedIinstancePlaceholder) {
                        // active configuration only considers declarative keys
                        // it is needed to consider configuration entries added
                        // at runtime as well as
                        // long as the input provider component adds some
                        // thus, it is checked if either the key is active or it
                        // is not part of the
                        // declarative keys and was added at runtime. those
                        // entries are active per default
                        boolean isActivePlaceholder = WorkflowPlaceholderHandler.isActivePlaceholder(instancePlaceholder, configDesc);
                        if (isActivePlaceholder) {
                            TreeItem instancePHTreeItem = new TreeItem(instanceTreeItem, 0);
                            treeItemToUUIDMap.put(instancePHTreeItem.hashCode(), compInstances);
                            String guiName = placeholderMetaData.getGuiName(instancePlaceholder);

                            if (guiName == null) {
                                guiName = instancePlaceholder;
                            }

                            treeItemNameToPlaceholder.put(instancePHTreeItem.hashCode(), instancePlaceholder);
                            instancePHTreeItem.setText(0, guiName);
                            String currentPlaceholder = componentID + dot + instancePlaceholder;
                            controlMap.put(instancePHTreeItem.hashCode(),
                                addSWTHandler(
                                    instancePHTreeItem,
                                    instanceName + dot + instancePlaceholder,
                                    guiName,
                                    ComponentUtils.isEncryptedPlaceholder(currentPlaceholder,
                                        WorkflowPlaceholderHandler.getEncryptedPlaceholder()),
                                    false));
                        }
                    }
                }
            }
        }
        placeApplyToAllButtonsWhereNecessary(componentPlaceholderTree);
        column1.pack();
    }

    private Control addSWTHandler(TreeItem item, final String placeholderName, final String guiName,
        boolean isEncrypted, boolean isGlobal) {
        TreeEditor textEditor = new TreeEditor(item.getParent());
        Combo booleanCombo = null;
        String finalProposal = null;
        textEditor.horizontalAlignment = SWT.LEFT;
        textEditor.grabHorizontal = true;
        int style = SWT.BORDER;
        boolean isPathField = false;
        boolean isBoolean = false;
        boolean isInteger = false;
        boolean isFloat = false;
        String dataType = placeholderHelper.getPlaceholdersDataType().get(
            placeholderName);
        final String componentName = placeholderName.split("\\" + dot)[0];
        if (dataType != null) {
            if (dataType.equals(PlaceholdersMetaDataConstants.TYPE_FILE) || dataType.equals(PlaceholdersMetaDataConstants.TYPE_DIR)) {
                isPathField = true;
            } else if (dataType.equals(PlaceholdersMetaDataConstants.TYPE_BOOL)) {
                booleanCombo = addBooleanCombo(item);
                isBoolean = true;
            } else if (dataType.equals(PlaceholdersMetaDataConstants.TYPE_INT)) {
                isInteger = true;
            } else if (dataType
                .equals(PlaceholdersMetaDataConstants.TYPE_FLOAT)) {
                isFloat = true;
            }
        }
        if (isEncrypted) {
            style |= SWT.PASSWORD;
        }
        final Text placeholderText = new Text(item.getParent(), style);
        placeholderText.setMessage("No value entered.");
        ModifyListener modifyListener = new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                validateInput((Text) e.getSource(), componentName, guiName);
            }
        };
        placeholderText.addModifyListener(modifyListener);
        if (!restoredPasswords && isEncrypted) {
            WorkflowPlaceholderHandler.restorePasswords(placeholderHelper
                .getComponentInstanceHistory());
            WorkflowPlaceholderHandler.restorePasswords(placeholderHelper
                .getComponentTypeHistory());
            restoredPasswords = true;
        }
        String[] allProposals = {};
        if (!isGlobal) {
            allProposals = placeholderHelper.getInstancePlaceholderHistory(
                treeItemNameToPlaceholder.get(item.hashCode()),
                treeItemToUUIDMap.get(item.hashCode()));
        } else {
            allProposals = placeholderHelper.getComponentPlaceholderHistory(
                treeItemNameToPlaceholder.get(item.hashCode()),
                getComponentIDByName(item.getParentItem().getText()),
                workflowDescription.getIdentifier());
        }
        if (allProposals.length > 0) {
            if (isEncrypted) {
                byte[] decoded = new Base64()
                    .decode(allProposals[allProposals.length - 1]);
                try {
                    allProposals[allProposals.length - 1] = new String(decoded,
                        "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    LOGGER.warn("Could not decode placeholder "
                        + placeholderName, e);
                }
            }
            finalProposal = allProposals[allProposals.length - 1]; // set default value to recent one
        }
        String[] additionalProposals = placeholderHelper
            .getOtherPlaceholderHistoryValues(treeItemNameToPlaceholder
                .get(item.hashCode()));
        if (allProposals.length == 0) {
            allProposals = additionalProposals;
            if (!isEncrypted && allProposals.length > 0) {
                String valueFromOtherComponentInWorkflow = placeholderHelper
                    .getValueFromOtherComponentInWorkflow(
                        treeItemNameToPlaceholder.get(item.hashCode()),
                        workflowDescription.getIdentifier());
                if (valueFromOtherComponentInWorkflow != null) {
                    finalProposal = valueFromOtherComponentInWorkflow;
                } else {
                    finalProposal = allProposals[allProposals.length - 1];
                }
            }
        } else {
            allProposals = additionalProposals;
        }
        if (finalProposal != null && !finalProposal.equals("")) {
            if (!isBoolean) {
                placeholderText.setText(finalProposal);
            } else if (booleanCombo != null) {
                booleanCombo.setText(finalProposal);
            }
        }
        if (isEncrypted) {
            allProposals = new String[0]; // Passwords should not be visible
        }
        SimpleContentProposalProvider scp = new SimpleContentProposalProvider(
            allProposals);
        scp.setFiltering(true);
        ContentProposalAdapter adapter = null;
        adapter = new ContentProposalAdapter(placeholderText,
            new TextContentAdapter(), scp,
            KeyStroke.getInstance(SWT.ARROW_DOWN), null);
        adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
        adapter.setAutoActivationDelay(1);
        adapter.setPropagateKeys(true);
        if (isPathField) {
            addFileChooser(item, dataType, placeholderText);
        }
        if (isBoolean) {
            textEditor.setEditor(booleanCombo, item, 1);
            return booleanCombo;
        }
        if (isTextEmpty(placeholderText)) {
            addPlaceholderValidator(componentName, guiName);
        }
        textEditor.setEditor(placeholderText, item, 1);
        if (isEncrypted) {
            TreeEditor checkButton = new TreeEditor(item.getParent());
            checkButton.horizontalAlignment = SWT.LEFT;
            checkButton.grabHorizontal = true;
            Button checkForSaveButton = new Button(item.getParent(), SWT.CHECK);
            checkForSaveButton.setText("Save");
            checkButton.minimumWidth = checkForSaveButton.getSize().x;
            checkButton.setEditor(checkForSaveButton, item, 2);
            saveButtonMap.put(item.hashCode(), checkForSaveButton);
            if (placeholderText.getText() != null && !placeholderText.getText().equals("")) {
                checkForSaveButton.setSelection(true);
            }
        }
        // componentPlaceholderCount.get(placeholderName) > 1
        if (isFloat) {
            NumericalTextConstraintListener floatListener = new NumericalTextConstraintListener(
                placeholderText, WidgetGroupFactory.ONLY_FLOAT);
            placeholderText.addVerifyListener(floatListener);
        }
        if (isInteger) {
            NumericalTextConstraintListener integerListener = new NumericalTextConstraintListener(
                placeholderText, WidgetGroupFactory.ONLY_INTEGER);
            placeholderText.addVerifyListener(integerListener);
        }
        return placeholderText;
    }

    private void placeApplyToAllButtonsWhereNecessary(Tree baseTree) {
        // put all Items of one ComponentID in one List

        Map<String, String> placeholderDataTypes = placeholderHelper.getPlaceholdersDataType();

        for (TreeItem componentTree : baseTree.getItems()) {
            List<ExtendedTreeItem> componentKeyValues = new ArrayList<>();
            List<ExtendedTreeItem> fullComponentKeyValues = new ArrayList<>();
            for (TreeItem instanceItem : componentTree.getItems()) {
                for (TreeItem keyValue : instanceItem.getItems()) {
                    String dataType = placeholderDataTypes.get(instanceItem.getText() + "."
                        + treeItemNameToPlaceholder.get(keyValue.hashCode()));
                    componentKeyValues.add(new ExtendedTreeItem(keyValue, dataType));
                    fullComponentKeyValues.add(new ExtendedTreeItem(keyValue, dataType));
                }
            }

            for (int i = 0; i < componentKeyValues.size(); i++) {
                boolean buttonForIElementCreated = false;
                for (int j = i + 1; j < componentKeyValues.size(); j++) {

                    if (componentKeyValues.get(i).treeItem.getText().equals(componentKeyValues.get(j).treeItem.getText())) {
                        if (componentKeyValues.get(i).dataType == null || componentKeyValues.get(j).dataType == null) {
                            if (componentKeyValues.get(i).dataType == null && componentKeyValues.get(i).dataType == null) {
                                if (!buttonForIElementCreated) {
                                    addApplyToAllButton(componentKeyValues.get(i), fullComponentKeyValues);
                                    buttonForIElementCreated = true;
                                }
                                addApplyToAllButton(componentKeyValues.get(j), fullComponentKeyValues);
                            }

                        } else if (componentKeyValues.get(i).dataType.equals(componentKeyValues.get(j).dataType)) {
                            if (!buttonForIElementCreated) {
                                addApplyToAllButton(componentKeyValues.get(i), fullComponentKeyValues);
                                buttonForIElementCreated = true;
                            }
                            addApplyToAllButton(componentKeyValues.get(j), fullComponentKeyValues);
                        }

                    }
                }
            }
        }

    }

    /**
     * More like an Object that clues together an Item with a dataType.
     * 
     * @author Jascha Riedel
     */
    private final class ExtendedTreeItem {

        public final TreeItem treeItem;

        public final String dataType;

        ExtendedTreeItem(TreeItem treeItem, String dataType) {
            this.treeItem = treeItem;
            this.dataType = dataType;
        }
    }

    private void addPlaceholderValidator(final String componentName,
        String placeholderName) {
        Set<String> placeholderNames = placeholderValidators.get(componentName);
        if (placeholderNames == null) {
            placeholderNames = new HashSet<>();
        }
        placeholderNames.add(placeholderName);
        placeholderValidators.put(componentName, placeholderNames);
    }

    private void removePlaceholderValidator(final String componentName,
        final String placeholderName) {
        Set<String> placeholderNames = placeholderValidators.get(componentName);
        placeholderNames.remove(placeholderName);
        if (placeholderNames.isEmpty()) {
            placeholderValidators.remove(componentName);
        }
    }

    private void addApplyToAllButton(ExtendedTreeItem extendedTreeItem, List<ExtendedTreeItem> allItems) {
        TreeEditor buttonEditor = new TreeEditor(extendedTreeItem.treeItem.getParent());
        buttonEditor.horizontalAlignment = SWT.LEFT;
        buttonEditor.grabHorizontal = true;
        Button placeholderButton = new Button(extendedTreeItem.treeItem.getParent(), SWT.PUSH);
        placeholderButton.setToolTipText(Messages.applyToAllToolTip);
        placeholderButton.setText(Messages.applyToAll);
        placeholderButton.setSize(placeholderButton.getText().length() * 6, 0);
        placeholderButton.computeSize(SWT.DEFAULT, extendedTreeItem.treeItem.getParent()
            .getItemHeight());
        placeholderButton.addSelectionListener(new ButtonListener(extendedTreeItem, allItems));
        buttonEditor.minimumWidth = placeholderButton.getSize().x;
        buttonEditor.setEditor(placeholderButton, extendedTreeItem.treeItem, 3);
    }

    private void addFileChooser(TreeItem item, final String dataType,
        final Text placeholderText) {
        TreeEditor pathButtonEditor = new TreeEditor(item.getParent());
        pathButtonEditor.horizontalAlignment = SWT.LEFT;
        pathButtonEditor.grabHorizontal = true;
        Button pathChooserButton = new Button(item.getParent(), SWT.PUSH);
        pathChooserButton.setText("...");
        pathChooserButton.setSize(pathChooserButton.getText().length() * 6, 0);
        pathChooserButton.computeSize(SWT.DEFAULT, item.getParent()
            .getItemHeight());
        pathButtonEditor.minimumWidth = pathChooserButton.getSize().x;
        pathChooserButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                final String selectedPath;
                if (dataType.equals(PlaceholdersMetaDataConstants.TYPE_DIR)) {
                    selectedPath = PropertyTabGuiHelper
                        .selectDirectoryFromFileSystem(getShell(),
                            "Open path...");
                } else {
                    selectedPath = PropertyTabGuiHelper
                        .selectFileFromFileSystem(getShell(),
                            new String[] {}, "Open path...");
                }
                if (selectedPath != null) {
                    placeholderText.setText(selectedPath);
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {}
        });
        pathButtonEditor.setEditor(pathChooserButton, item, 2);
    }

    private static Combo addBooleanCombo(TreeItem item) {
        final Combo combo = new Combo(item.getParent(), SWT.READ_ONLY);
        combo.add(Boolean.TRUE.toString().toLowerCase());
        combo.add(Boolean.FALSE.toString().toLowerCase());
        combo.setText(Boolean.TRUE.toString().toLowerCase());
        return combo;
    }

    private static Image getImage(WorkflowNode element) {
        byte[] icon = element.getComponentDescription().getIcon16();
        Image image;
        if (icon != null) {
            image = new Image(Display.getCurrent(), new ByteArrayInputStream(
                icon));
        } else {
            image = Activator.getInstance().getImageRegistry()
                .get(Activator.IMAGE_RCE_ICON_16);
        }
        return image;
    }

    private String getControlText(int id) {
        Control control = controlMap.get(id);
        String text = null;
        if (control instanceof Text) {
            Text current = (Text) control;
            text = current.getText();
        } else if (control instanceof Combo) {
            Combo current = (Combo) control;
            text = current.getText();
        }
        return text;
    }

    protected Control getControl(int id) {
        Control control = controlMap.get(id);
        return control;
    }

    private static boolean isTextEmpty(Text source) {
        return source.getText() == null || source.getText().equals("");
    }

    protected void validateInput(Text source, String componentName,
        String placeholderName) {
        if (!isTextEmpty(source)) {
            if (source.getBackground().equals(COLOR_RED)) {
                source.setBackground(COLOR_WHITE);
                removePlaceholderValidator(componentName, placeholderName);
            }
        } else {
            source.setBackground(COLOR_RED);
            addPlaceholderValidator(componentName, placeholderName);
        }
    }

    /**
     * 
     * Verify placeholderPage for errors. Missing input is defined as an error.
     * 
     * @return whether errors exist or not.
     */
    public boolean validateErrors() {
        return !placeholderValidators.isEmpty();
    }

    /**
     * @return amount of errors
     */
    public int getErrorAmount() {
        return placeholderValidators.size();
    }

    /**
     * 
     * Retrieve the component names which have an error.
     * 
     * @param indicator when its true {@link Messages#missingPlaceholder} is placed behind the name
     * @return set of component names
     */
    public Map<String, String> getComponentNamesWithError(boolean indicator) {
        Set<String> componentNames = placeholderValidators.keySet();
        Map<String, String> newComponentNames = new HashMap<>();
        for (String name : componentNames) {
            String additionalInfos = "";
            if (indicator) {
                additionalInfos = " " + Messages.missingPlaceholder;
            }
            newComponentNames.put(name, additionalInfos);
        }
        return newComponentNames;

    }

    /**
     * Performs all actions to be done when 'Finish' is clicked.
     * 
     * @return placeholder map for each {@link WorkflowNode}
     */
    protected void performFinish() {

        for (TreeItem componentItems : componentPlaceholderTree.getItems()) {
            for (TreeItem componentIDItems : componentItems.getItems()) {
                if (componentIDItems.getItemCount() == 0) {
                    // componentPlaceholder
                    for (String fullPlaceholder : placeholderHelper
                        .getPlaceholderOfComponent(getComponentIDByName(componentItems
                            .getText()))) {
                        if (WorkflowPlaceholderHandler.getNameOfPlaceholder(fullPlaceholder)
                            .equals(treeItemNameToPlaceholder.get(componentIDItems.hashCode()))) {
                            boolean addToHistory = true;
                            if (saveButtonMap.get(componentIDItems.hashCode()) != null) {
                                addToHistory = saveButtonMap.get(componentIDItems.hashCode())
                                    .getSelection();
                            }
                            placeholderHelper
                                .setGlobalPlaceholderValue(
                                    fullPlaceholder,
                                    getComponentIDByName(componentItems
                                        .getText()),
                                    getControlText(componentIDItems
                                        .hashCode()),
                                    workflowDescription.getIdentifier(),
                                    addToHistory);
                        }
                    }
                } else {
                    for (TreeItem instancePlaceholderItems : componentIDItems
                        .getItems()) {
                        // instancePlaceholder
                        for (String fullPlaceholder : placeholderHelper
                            .getPlaceholderNamesOfComponentInstance(treeItemToUUIDMap.get(instancePlaceholderItems.hashCode()))) {
                            if (WorkflowPlaceholderHandler.getNameOfPlaceholder(fullPlaceholder).equals(treeItemNameToPlaceholder
                                .get(instancePlaceholderItems.hashCode()))) {
                                boolean addToHistory = true;
                                if (saveButtonMap.get(instancePlaceholderItems.hashCode()) != null) {
                                    addToHistory = saveButtonMap.get(
                                        instancePlaceholderItems.hashCode()).getSelection();
                                }
                                placeholderHelper.setPlaceholderValue(fullPlaceholder, getComponentIDByName(componentIDItems
                                    .getText()), treeItemToUUIDMap.get(instancePlaceholderItems.hashCode()),
                                    getControlText(instancePlaceholderItems.hashCode()),
                                    workflowDescription.getIdentifier(), addToHistory);
                            }
                        }
                    }
                }
            }
        }
        /// * dispose SWT components */
        // for (Integer key : controlMap.keySet()) {
        // controlMap.get(key).dispose();
        // }
        // componentPlaceholderTree.dispose();
    }

    /**
     * Saves the placeholder settings to persistent settings.
     */
    protected void savePlaceholdersToPersistentSettings() {
        if (WorkflowPlaceholderHandler.getPlaceholderPersistentSettingsUUID() == null) {
            IPreferenceStore prefs = Activator.getInstance()
                .getPreferenceStore();
            String placeholderPersistentSettingsUUID = prefs
                .getString(WorkflowPlaceholderHandler.PLACEHOLDER_PREFERENCES_KEY);
            if (placeholderPersistentSettingsUUID.isEmpty()) {
                WorkflowPlaceholderHandler
                    .setPlaceholderPersistentSettingsUUID(UUID.randomUUID()
                        .toString());
                prefs.putValue(
                    WorkflowPlaceholderHandler.PLACEHOLDER_PREFERENCES_KEY,
                    placeholderPersistentSettingsUUID);
            }
        }
        placeholderHelper.saveHistory();
    }

    protected Map<String, Map<String, String>> getPlaceholders() {
        Map<String, Map<String, String>> placeholdersMap = new HashMap<>();

        for (WorkflowNode wn : workflowDescription.getWorkflowNodes()) {

            Map<String, String> placeholders = new HashMap<>();

            Map<String, String> compTypePlaceholders = placeholderHelper.getPlaceholdersOfComponentType(wn
                .getComponentDescription().getIdentifier());

            if (compTypePlaceholders != null) {
                placeholders.putAll(compTypePlaceholders);
            }

            Map<String, String> compInstPlaceholders = placeholderHelper.getPlaceholdersOfComponentInstance(wn.getIdentifier());
            if (compInstPlaceholders != null) {
                placeholders.putAll(compInstPlaceholders);
            }

            placeholdersMap.put(wn.getIdentifier(), placeholders);
        }

        return placeholdersMap;
    }

    private String getComponentIDByName(String name) {
        for (WorkflowNode wn : workflowDescription.getWorkflowNodes()) {
            if (wn.getName().equals(name)) {
                return wn.getComponentDescription().getIdentifier();
            }
        }
        return null;
    }

    private PlaceholdersMetaDataDefinition getPlaceholderAttributes(String name) {
        for (WorkflowNode wn : workflowDescription.getWorkflowNodes()) {
            if (wn.getName().equals(name)) {
                return wn.getComponentDescription()
                    .getConfigurationDescription()
                    .getComponentConfigurationDefinition()
                    .getPlaceholderMetaDataDefinition();
            }
        }
        return null;
    }

    public Map<String, Set<String>> getPlaceholderValidators() {
        return placeholderValidators;
    }

    protected boolean canFinish() {
        if (componentPlaceholderTree != null) {
            return componentPlaceholderTree.getItemCount() == 0;

        }
        return false;
    }

    @Override
    public void dispose() {
        /* dispose SWT components */
        for (Integer key : controlMap.keySet()) {
            controlMap.get(key).dispose();
        }
        componentPlaceholderTree.dispose();
        super.dispose();
    }

    /**
     * New Button Listener behaving as follows: if item Name and item DataType are the same, value of that Item/Field/Parameter is replaced.
     * 
     * @author Jascha Riedel
     */
    private class ButtonListener extends SelectionAdapter {

        private ExtendedTreeItem extendedTreeItem;

        private List<ExtendedTreeItem> allItems;

        ButtonListener(ExtendedTreeItem extendedTreeItem, List<ExtendedTreeItem> allItems) {
            this.extendedTreeItem = extendedTreeItem;
            this.allItems = allItems;
        }

        @Override
        public void widgetSelected(SelectionEvent event) {
            Control treeItemControl = getControl(extendedTreeItem.treeItem.hashCode());
            Object valueToBeCoppied = null;
            if (treeItemControl instanceof Text) {
                valueToBeCoppied = ((Text) treeItemControl).getText();
            } else if (treeItemControl instanceof Combo) {
                valueToBeCoppied = ((Combo) treeItemControl).getText();
            }
            if (valueToBeCoppied != null && !((String) valueToBeCoppied).isEmpty()) {
                for (int i = 0; i < allItems.size(); i++) {
                    if (extendedTreeItem.treeItem.getText().equals(allItems.get(i).treeItem.getText())) {
                        if (extendedTreeItem.dataType == null && allItems.get(i).dataType == null) {
                            Control toReplaceItem = getControl(allItems.get(i).treeItem.hashCode());
                            if (toReplaceItem instanceof Text) {
                                ((Text) toReplaceItem).setText((String) valueToBeCoppied);
                            } else if (toReplaceItem instanceof Combo) {
                                ((Combo) toReplaceItem).select(((Combo) treeItemControl).indexOf((String) valueToBeCoppied));
                            }
                        } else if (extendedTreeItem.dataType.equals(allItems.get(i).dataType)) {
                            Control toReplaceItem = getControl(allItems.get(i).treeItem.hashCode());
                            if (toReplaceItem instanceof Text) {
                                ((Text) toReplaceItem).setText((String) valueToBeCoppied);
                            } else if (toReplaceItem instanceof Combo) {
                                ((Combo) toReplaceItem).select(((Combo) treeItemControl).indexOf((String) valueToBeCoppied));
                            }
                        }
                    }
                }
            }
        }
    }
}
