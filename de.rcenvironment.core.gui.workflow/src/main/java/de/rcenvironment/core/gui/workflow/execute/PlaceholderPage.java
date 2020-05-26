/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.execute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
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
import org.eclipse.swt.widgets.TreeItem;

import de.rcenvironment.core.component.model.configuration.api.PlaceholdersMetaDataConstants;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowPlaceholderHandler;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNodeIdentifier;
import de.rcenvironment.core.gui.utils.common.components.PropertyTabGuiHelper;
import de.rcenvironment.core.gui.workflow.Activator;

/**
 * Wizard page for managing placeholders.
 * 
 * @author Sascha Zur
 * @author Brigitte Boden
 * @author Alexander Weinert (extracted {@link PlaceholderPageTreeFactory})
 */
@SuppressWarnings("boxing")
public class PlaceholderPage extends WizardPage {

    private static final Color COLOR_WHITE = Display.getCurrent()
        .getSystemColor(SWT.COLOR_WHITE);

    static final Color COLOR_RED = Display.getCurrent().getSystemColor(
        SWT.COLOR_RED);

    private static final int SHORTTEXT_MAXLENGTH = 140;

    private final WorkflowDescription workflowDescription;

    private WorkflowPlaceholderHandler placeholderHelper;

    private Tree componentPlaceholderTree;

    // maps the hash code of a TreeItem to a control
    private Map<Integer, Control> controlMap;

    private Map<Integer, Button> saveButtonMap;

    private Map<Integer, String> treeItemNameToPlaceholder;

    private Map<Integer, String> treeItemToUUIDMap;

    private final Map<String, Set<String>> placeholderValidators = new HashMap<>();

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
    
        final PlaceholderPageTreeFactory treeFactory = new PlaceholderPageTreeFactory(this, placeholderHelper, placeholderInformationGroup);
        treeFactory.fillTree();
        componentPlaceholderTree = treeFactory.getTree();

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
                        } else if (control instanceof Combo) {
                            Combo current = (Combo) control;
                            parent.setExpanded(true);
                            secondLevel.setExpanded(true);
                        }
                    }
                }
            }
        }
    }

    void addSaveButton(TreeItem item, final Text placeholderText) {
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

    /**
     * Adds an "apply to all" button to those tree items that represent a placeholder that satisfies the following conditions: 1) There
     * exists another instance of the same component that expects a value for a placeholder of the same name and 2) That placeholder has the
     * same datatype as the former one.
     * 
     * @param The main tree shown on the placeholder page
     */
    void placeApplyToAllButtonsWhereNecessary(Tree baseTree) {
        final Map<String, String> placeholderDataTypes = placeholderHelper.getPlaceholdersDataType();

        for (TreeItem componentTree : baseTree.getItems()) {

            // First, we construct a list of all tree items that represent placeholders (i.e., all tree items on the third level of the
            // tree) together with their respective extended datatype, e.g., int, float, file, etc.
            final List<ExtendedTreeItem> componentKeyValues = new ArrayList<>();
            for (TreeItem instanceItem : componentTree.getItems()) {
                for (TreeItem keyValue : instanceItem.getItems()) {
                    final String placeholderKey = instanceItem.getText() + "." + treeItemNameToPlaceholder.get(keyValue.hashCode());
                    // For historical reasons properties of user-integrated tools that are given a value when starting the workflow do not
                    // have a data type assigned to them. Changing this would require migrating existing tool integrations. Hence, we
                    // instead opt for a default data type of "text" if we encounter a placeholder that does not have a data type assigned.
                    // This is reasonable, as it is the most permissible data type and can easily be parsed into other data types.
                    final String dataType = placeholderDataTypes.getOrDefault(placeholderKey, "text");
                    componentKeyValues.add(new ExtendedTreeItem(keyValue, dataType));
                }
            }

            // Intuitively, we now determine the equivalence classes of ExtendedTreeItems, where two ExtendedTreeItems are equivalent if
            // they represent placeholders of the same name and of the same datatype. To this end, we leverage streams in order to group the
            // list of ExtendedTreeItems constructed above first by the names of the represented placeholders, and then by their datatypes/.
            // This use of the stream-API is adapted form examples 2.4 and 2.5 from https://www.baeldung.com/java-groupingby-collector
            final Map<String, Map<String, Set<ExtendedTreeItem>>> groupedTreeItems =
                componentKeyValues.stream()
                    .collect(Collectors.groupingBy(extendedItem -> extendedItem.treeItem.getText(),
                        Collectors.groupingBy(extendedItem -> extendedItem.dataType,
                            Collectors.toSet())));

            // Finally, we check all equivalence classes obtained above. Due to the use of grouping above, no equivalence class is empty or
            // even null. However, we still have to check whether an equivalence class contains more than one placeholder, since we do not
            // want to add an "apply to all" button if "all" just refers to a single placeholder.
            for (Map<String, Set<ExtendedTreeItem>> treeItemGroupByPlaceholderName : groupedTreeItems.values()) {
                for (Set<ExtendedTreeItem> treeItemGroup : treeItemGroupByPlaceholderName.values()) {
                    if (treeItemGroup.size() > 1) {
                        for (ExtendedTreeItem equivalentTreeItem : treeItemGroup) {
                            addApplyToAllButton(equivalentTreeItem, componentKeyValues);
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
            Objects.requireNonNull(treeItem);
            Objects.requireNonNull(dataType);
            this.treeItem = treeItem;
            this.dataType = dataType;
        }
    }

    void addPlaceholderValidator(final String componentName,
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
        placeholderButton.computeSize(SWT.DEFAULT, extendedTreeItem.treeItem.getParent().getItemHeight());
        placeholderButton.addSelectionListener(new ButtonListener(extendedTreeItem, allItems));
        buttonEditor.minimumWidth = placeholderButton.getSize().x;
        buttonEditor.setEditor(placeholderButton, extendedTreeItem.treeItem, 3);
    }

    void addFileChooser(TreeItem item, final String dataType,
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
                        .selectDirectoryFromFileSystemWithPath(getShell(),
                            "Open path...", placeholderText.getText());
                } else {
                    selectedPath = PropertyTabGuiHelper
                        .selectFileFromFileSystem(getShell(),
                            new String[] {}, "Open path...", placeholderText.getText());
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
        return controlMap.get(id);
    }

    static boolean isTextEmpty(Text source) {
        return source.getText() == null || source.getText().equals("");
    }

    protected void validateInput(Text source, String componentName, String placeholderName, boolean isShorttext) {
        if ((!isTextEmpty(source) || isShorttext) && source.getText().length() <= SHORTTEXT_MAXLENGTH) {
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
                                addToHistory = saveButtonMap.get(componentIDItems.hashCode()).getSelection();
                            }
                            placeholderHelper.setGlobalPlaceholderValue(
                                    fullPlaceholder,
                                    getComponentIDByName(componentItems.getText()),
                                    getControlText(componentIDItems.hashCode()),
                                    workflowDescription.getIdentifier(),
                                    addToHistory);
                        }
                    }
                } else {
                    for (TreeItem instancePlaceholderItems : componentIDItems.getItems()) {
                        // instancePlaceholder
                        for (String fullPlaceholder : placeholderHelper
                            .getPlaceholderNamesOfComponentInstance(treeItemToUUIDMap.get(instancePlaceholderItems.hashCode()))) {
                            if (WorkflowPlaceholderHandler.getNameOfPlaceholder(fullPlaceholder)
                                .equals(treeItemNameToPlaceholder.get(instancePlaceholderItems.hashCode()))) {
                                boolean addToHistory = true;
                                if (saveButtonMap.get(instancePlaceholderItems.hashCode()) != null) {
                                    addToHistory = saveButtonMap.get(instancePlaceholderItems.hashCode()).getSelection();
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

    String getComponentIDByName(String name) {
        for (WorkflowNode wn : workflowDescription.getWorkflowNodes()) {
            if (wn.getName().equals(name)) {
                return wn.getComponentDescription().getIdentifier();
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
    
    public void putIntoControlMap(TreeItem item, Control control) {
        this.controlMap.put(item.hashCode(), control);
    }
    
    public WorkflowDescription getWorkflowDescription() {
        return this.workflowDescription;
    }
    
    public WorkflowNode getWorkflowNode(final WorkflowNodeIdentifier wfNodeID) {
        return this.workflowDescription.getWorkflowNode(wfNodeID);
    }
    
    public String getWorkflowIdentifier() {
        return this.workflowDescription.getIdentifier();
    }
    
    public void setWorkflowNodeIDForTreeItem(final TreeItem treeItem, final WorkflowNodeIdentifier wfNodeID) {
        this.treeItemToUUIDMap.put(treeItem.hashCode(), wfNodeID.toString());
    }
    
    public WorkflowNodeIdentifier getWorkflowNodeIDForTreeItem(final TreeItem treeItem) {
        return new WorkflowNodeIdentifier(this.treeItemToUUIDMap.get(treeItem.hashCode()));
    }
    
    public void setPlaceholderForTreeItem(final TreeItem treeItem, final String placeholder) {
        this.treeItemNameToPlaceholder.put(treeItem.hashCode(), placeholder);
    }

    public String getPlaceholderForTreeItem(TreeItem item) {
        return this.treeItemNameToPlaceholder.get(item.hashCode());
    }
    
    public String getPlaceholderDataType(final String wfNodeName, final String placeholderName) {
        return this.placeholderHelper.getPlaceholdersDataType().get(wfNodeName + "." + placeholderName);
    }
}
