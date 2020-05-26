/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.execute;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDescription;
import de.rcenvironment.core.component.model.configuration.api.PlaceholdersMetaDataConstants;
import de.rcenvironment.core.component.model.configuration.api.PlaceholdersMetaDataDefinition;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowPlaceholderHandler;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNodeIdentifier;
import de.rcenvironment.core.gui.utils.incubator.NumericalTextConstraintListener;
import de.rcenvironment.core.gui.utils.incubator.WidgetGroupFactory;
import de.rcenvironment.core.utils.common.StringUtils;

public class PlaceholderPageTreeFactory {

    static final int DEFAULT_COLUMN_WIDTH = 100;

    private static final Log LOGGER = LogFactory.getLog(PlaceholderPageTreeFactory.class);

    private final PlaceholderPage placeholderPage;
    
    private final WorkflowPlaceholderHandler placeholderHelper;

    private final Tree componentPlaceholderTree;

    private boolean restoredPasswords = false;

    public PlaceholderPageTreeFactory(final PlaceholderPage placeholderPage, final WorkflowPlaceholderHandler placeholderHandler,
        final Group parentGroup) {

        this.placeholderPage = placeholderPage;
        this.placeholderHelper = placeholderHandler;
        this.componentPlaceholderTree = new Tree(parentGroup, SWT.MULTI);

        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.grabExcessVerticalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.grabExcessHorizontalSpace = true;

        componentPlaceholderTree.setLayoutData(gridData);
        componentPlaceholderTree.setHeaderVisible(true);
        componentPlaceholderTree.setLinesVisible(true);

        // resize the row height using a MeasureItem listener
        componentPlaceholderTree.addListener(SWT.MeasureItem, event -> event.height = 2 * 10 + 2);
    }

    public Tree getTree() {
        return this.componentPlaceholderTree;
    }

    void fillTree() {
        TreeColumn displayNameColumn = appendLeftAlignedColumnToTree();

        appendValueColumnToTree();
        appendBrowseColumnToTree();
        appendApplyToAllColumnToTree();

        String[] componentTypesWithPlaceholderArray =
            getComponentTypesWithPlaceholders(placeholderHelper.getIdentifiersOfPlaceholderContainingComponents());

        for (String componentID : componentTypesWithPlaceholderArray) {
            appendSingleComponentTypeToTree(componentID);
        }

        placeholderPage.placeApplyToAllButtonsWhereNecessary(componentPlaceholderTree);
        displayNameColumn.pack();
    }

    /**
     * Adds table entries for all instances of the given component ID. If no instance of the given component features any placeholders, no
     * table entry is added.
     * 
     * @param componentID The ID of the component for which table entries are to be added
     */
    protected void appendSingleComponentTypeToTree(String componentID) {
        final WorkflowNode workflowNode = getArbitraryWorkflowNodeForComponent(componentID);

        final String componentName = workflowNode.getName();
        final TreeItem componentTypeTreeItem = createTreeItemForComponentType(workflowNode);

        appendGlobalPlaceholdersToTree(componentTypeTreeItem, componentID, componentName);

        List<WorkflowNodeIdentifier> instancesWithPlaceholder = getComponentInstancesWithPlaceholders(componentID);
        for (WorkflowNodeIdentifier componentInstanceID : instancesWithPlaceholder) {
            final ConfigurationDescription configDesc =
                placeholderPage.getWorkflowNode(componentInstanceID).getComponentDescription()
                    .getConfigurationDescription();

            final boolean hasPlaceholderWithGUIName =
                determineWhetherInstanceHasPlaceholdersWithDisplayName(componentInstanceID, componentName, configDesc);
            
            if (!hasPlaceholderWithGUIName) {
                // As we are unable to display any placeholders of this component instance to the user, we do not add a tree item for this
                // instance. Since we do not add a tree item for this instance, we can furthermore skip adding any children to that tree
                // item.
                continue;
            }

            String instanceName = placeholderPage.getWorkflowNode(componentInstanceID).getName();
            TreeItem instanceTreeItem = createTreeItemForComponentInstance(componentTypeTreeItem, instanceName, getImage(workflowNode));

            final Optional<PlaceholdersMetaDataDefinition> placeholderMetaData = getPlaceholderMetaData(componentName);
            List<String> orderedInstancePlaceholders = PlaceholderSortUtils.sortGlobalPlaceholders(
                    placeholderHelper.getPlaceholderNameSetOfComponentInstance(componentInstanceID.toString()),
                    placeholderMetaData.orElse(null));

            for (String instancePlaceholder : orderedInstancePlaceholders) {
                // active configuration only considers declarative keys. It is needed to consider configuration entries added at runtime
                // as well as long as the input provider component adds some thus, it is checked if either the key is active or it is
                // not part of the declarative keys and was added at runtime. Those entries are active per default
                boolean isActivePlaceholder = WorkflowPlaceholderHandler.isActivePlaceholder(instancePlaceholder, configDesc);
                if (!isActivePlaceholder) {
                    continue;
                }

                TreeItem instancePHTreeItem = new TreeItem(instanceTreeItem, 0);
                placeholderPage.setWorkflowNodeIDForTreeItem(instancePHTreeItem, componentInstanceID);
                String guiName = placeholderMetaData.map(metaData -> metaData.getGuiName(instancePlaceholder)).orElse(instancePlaceholder);

                placeholderPage.setPlaceholderForTreeItem(instancePHTreeItem, instancePlaceholder);
                instancePHTreeItem.setText(0, guiName);
                String currentPlaceholder = StringUtils.format("%s.%s", componentID, instancePlaceholder);

                placeholderPage.putIntoControlMap(instancePHTreeItem,
                    buildPlaceholderText(
                        instancePHTreeItem,
                        StringUtils.format("%s.%s", instanceName, instancePlaceholder),
                        guiName,
                        ComponentUtils.isEncryptedPlaceholder(currentPlaceholder, WorkflowPlaceholderHandler.getEncryptedPlaceholder()),
                        false));
            }
        }
    }

    protected TreeItem createTreeItemForComponentInstance(final TreeItem componentTypeTreeItem, final String instanceName, Image image) {
        TreeItem instanceTreeItem = new TreeItem(componentTypeTreeItem, 0);
        instanceTreeItem.setText(0, instanceName);
        instanceTreeItem.setImage(image);
        return instanceTreeItem;
    }

    protected boolean determineWhetherInstanceHasPlaceholdersWithDisplayName(WorkflowNodeIdentifier workflowNodeInstance,
        final String componentName, ConfigurationDescription configDesc) {

        boolean hasPlaceholderWithGUIName = false;
        final Optional<PlaceholdersMetaDataDefinition> placeholderMetaData = getPlaceholderMetaData(componentName);
        for (String instancePlaceholder : placeholderHelper.getPlaceholderNameSetOfComponentInstance(workflowNodeInstance.toString())) {
            boolean isActivePlaceholder = WorkflowPlaceholderHandler.isActivePlaceholder(instancePlaceholder, configDesc);
            if (isActivePlaceholder) {
                final Optional<String> guiName = placeholderMetaData.map(metaData -> metaData.getGuiName(instancePlaceholder));
                final Optional<String> defaultGuiName = placeholderMetaData.map(metaData -> metaData.getGuiName("*"));
                if ((guiName.isPresent() && !guiName.get().isEmpty() || defaultGuiName.isPresent())) {
                    return true;
                } else {
                    for (String configurationValues : configDesc.getConfiguration().values()) {
                        if (ConfigurationDescription.isPlaceholder(configurationValues)
                            && WorkflowPlaceholderHandler.getNameOfPlaceholder(configurationValues).equals(instancePlaceholder)) {
                            hasPlaceholderWithGUIName = true;
                        }
                    }
                    if (!hasPlaceholderWithGUIName) {
                        LOGGER.warn(StringUtils.format("Placeholder %s of component %s has no GUI name defined and will be ignored.",
                            instancePlaceholder,
                            placeholderPage.getWorkflowNode(workflowNodeInstance).getComponentDescription().getName()));
                    }
                    return hasPlaceholderWithGUIName;
                }
            } else {
                return false;
            }
        }
        return false;
    }

    protected List<WorkflowNodeIdentifier> getComponentInstancesWithPlaceholders(String componentID) {
        List<String> instancesWithPlaceholder = placeholderHelper.getComponentInstances(componentID);
        instancesWithPlaceholder = PlaceholderSortUtils
            .sortInstancesWithPlaceholderByName(instancesWithPlaceholder, placeholderPage.getWorkflowDescription());
        return instancesWithPlaceholder.stream().map(WorkflowNodeIdentifier::new).collect(Collectors.toList());
    }

    // TODO: The parameter componentName should not be required here
    protected void appendGlobalPlaceholdersToTree(final TreeItem parent, String componentID, final String componentName) {
        final Optional<PlaceholdersMetaDataDefinition> placeholderMetaData = getPlaceholderMetaData(componentName);
        final List<String> globalPlaceholders = PlaceholderSortUtils.sortGlobalPlaceholders(
            placeholderHelper.getGlobalPlaceholdersForComponentID(componentID), placeholderMetaData.orElse(null));

        for (String componentPlaceholder : globalPlaceholders) {
            TreeItem compPHTreeItem = new TreeItem(parent, 0);
            Optional<String> guiName = placeholderMetaData.map(metaData -> metaData.getGuiName(componentPlaceholder));

            placeholderPage.setPlaceholderForTreeItem(compPHTreeItem, componentPlaceholder);
            compPHTreeItem.setText(0, guiName.orElse(""));

            final String currentPlaceholder = StringUtils.format("%s.%s", componentID, componentPlaceholder);
            placeholderPage.putIntoControlMap(compPHTreeItem,
                buildPlaceholderText(compPHTreeItem, StringUtils.format("%s.%s", componentName, componentPlaceholder),
                    guiName.orElse(""),
                    ComponentUtils.isEncryptedPlaceholder(currentPlaceholder, WorkflowPlaceholderHandler.getEncryptedPlaceholder()),
                    true));
        }
    }

    private Optional<PlaceholdersMetaDataDefinition> getPlaceholderMetaData(String name) {
        return placeholderPage.getWorkflowDescription().getWorkflowNodes().stream()
            .filter(workflowNode -> workflowNode.getName().equals(name))
            .map(workflowNode -> workflowNode.getComponentDescription()
                .getConfigurationDescription()
                .getComponentConfigurationDefinition()
                .getPlaceholderMetaDataDefinition())
            .findAny();
    }

    protected WorkflowNode getArbitraryWorkflowNodeForComponent(String componentID) {
        final String workflowNodeID = placeholderHelper.getComponentInstances(componentID).get(0);
        return placeholderPage.getWorkflowNode(new WorkflowNodeIdentifier(workflowNodeID));
    }

    protected TreeItem createTreeItemForComponentType(final WorkflowNode workflowNode) {
        final TreeItem componentIDTreeItem = new TreeItem(componentPlaceholderTree, 0);
        componentIDTreeItem.setText(0, workflowNode.getComponentDescription().getName());
        componentIDTreeItem.setImage(getImage(workflowNode));
        return componentIDTreeItem;
    }

    protected void appendApplyToAllColumnToTree() {
        appendCenterAlignedColumnToTree(DEFAULT_COLUMN_WIDTH);
    }

    protected void appendBrowseColumnToTree() {
        appendCenterAlignedColumnToTree(DEFAULT_COLUMN_WIDTH / 2);
    }

    protected void appendValueColumnToTree() {
        appendCenterAlignedColumnToTree(DEFAULT_COLUMN_WIDTH + 5);
    }

    protected String[] getComponentTypesWithPlaceholders(Set<String> componentTypesWithPlaceholders) {
        Set<String> componentTypesWithPlaceholder = componentTypesWithPlaceholders;
        String[] componentTypesWithPlaceholderArray = componentTypesWithPlaceholder
            .toArray(new String[componentTypesWithPlaceholder.size()]);
        Arrays.sort(componentTypesWithPlaceholderArray);
        return componentTypesWithPlaceholderArray;
    }

    Control buildPlaceholderText(TreeItem item, final String placeholderName, final String guiName,
        boolean isEncrypted, boolean isGlobal) {
        TreeEditor textEditor = new TreeEditor(item.getParent());
        Combo booleanCombo = null;
        textEditor.horizontalAlignment = SWT.LEFT;
        textEditor.grabHorizontal = true;
        int style = SWT.BORDER | SWT.SINGLE;
        boolean isPathField = false;
        boolean isBoolean = false;
        boolean isInteger = false;
        boolean isFloat = false;
        String dataType = placeholderHelper.getPlaceholdersDataType().get(placeholderName);
        final String componentName = placeholderName.split("\\.")[0];
        if (dataType != null) {
            if (dataType.equals(PlaceholdersMetaDataConstants.TYPE_FILE) || dataType.equals(PlaceholdersMetaDataConstants.TYPE_DIR)) {
                isPathField = true;
            } else if (dataType.equals(PlaceholdersMetaDataConstants.TYPE_BOOL)) {
                booleanCombo = createBooleanCombo(item);
                isBoolean = true;
            } else if (dataType.equals(PlaceholdersMetaDataConstants.TYPE_INT)) {
                isInteger = true;
            } else if (dataType.equals(PlaceholdersMetaDataConstants.TYPE_FLOAT)) {
                isFloat = true;
            }
        }
        if (isEncrypted) {
            style |= SWT.PASSWORD;
        }
        final Text placeholderText = new Text(item.getParent(), style);
        boolean isShorttext = !(isFloat || isInteger || isBoolean || isPathField);
        if (!isShorttext) {
            placeholderText.setMessage("No value entered.");
        }

        ModifyListener modifyListener =
            event -> placeholderPage.validateInput((Text) event.getSource(), componentName, guiName, isShorttext);

        placeholderText.addModifyListener(modifyListener);
        if (!restoredPasswords && isEncrypted) {
            WorkflowPlaceholderHandler.restorePasswords(placeholderHelper.getComponentInstanceHistory());
            WorkflowPlaceholderHandler.restorePasswords(placeholderHelper.getComponentTypeHistory());
            restoredPasswords = true;
        }

        String[] allProposals =
            getAllProposalsForPlaceholder(item, isEncrypted, isGlobal, booleanCombo, isBoolean, placeholderText);
        buildAndSetContentProposalAdapter(placeholderText, allProposals);

        if (isPathField) {
            placeholderPage.addFileChooser(item, dataType, placeholderText);
        }
        if (isBoolean) {
            textEditor.setEditor(booleanCombo, item, 1);
            return booleanCombo;
        }
        if (PlaceholderPage.isTextEmpty(placeholderText) && !isShorttext) {
            placeholderPage.addPlaceholderValidator(componentName, guiName);
            placeholderText.setBackground(PlaceholderPage.COLOR_RED);
        }
        textEditor.setEditor(placeholderText, item, 1);
        if (isEncrypted) {
            placeholderPage.addSaveButton(item, placeholderText);
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

    private static Combo createBooleanCombo(TreeItem item) {
        final Combo combo = new Combo(item.getParent(), SWT.READ_ONLY);
        combo.add(Boolean.TRUE.toString().toLowerCase());
        combo.add(Boolean.FALSE.toString().toLowerCase());
        combo.setText(Boolean.TRUE.toString().toLowerCase());
        return combo;
    }

    protected void buildAndSetContentProposalAdapter(final Text placeholderText, String[] allProposals) {
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
    }

    protected String[] getAllProposalsForPlaceholder(TreeItem item, boolean isEncrypted, boolean isGlobal, Combo booleanCombo,
        boolean isBoolean, final Text placeholderText) {
        String[] allProposals;
        if (!isGlobal) {
            allProposals = placeholderHelper.getInstancePlaceholderHistory(
                placeholderPage.getPlaceholderForTreeItem(item),
                placeholderPage.getWorkflowNodeIDForTreeItem(item).toString());
        } else {
            allProposals = placeholderHelper.getComponentPlaceholderHistory(
                placeholderPage.getPlaceholderForTreeItem(item),
                placeholderPage.getComponentIDByName(item.getParentItem().getText()),
                placeholderPage.getWorkflowIdentifier());
        }

        String finalProposal = null;
        if (allProposals.length > 0) {
            if (isEncrypted) {
                byte[] decoded = new Base64().decode(allProposals[allProposals.length - 1]);
                allProposals[allProposals.length - 1] = new String(decoded, StandardCharsets.UTF_8);
            }
            finalProposal = allProposals[allProposals.length - 1]; // set default value to recent one
        }

        String[] additionalProposals = placeholderHelper.getOtherPlaceholderHistoryValues(placeholderPage.getPlaceholderForTreeItem(item));
        if (allProposals.length == 0) {
            allProposals = additionalProposals;
            if (!isEncrypted && allProposals.length > 0) {
                String valueFromOtherComponentInWorkflow = placeholderHelper.getValueFromOtherComponentInWorkflow(
                        placeholderPage.getPlaceholderForTreeItem(item),
                        placeholderPage.getWorkflowIdentifier());
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
        return allProposals;
    }

    private TreeColumn appendLeftAlignedColumnToTree() {
        final TreeColumn returnValue = new TreeColumn(componentPlaceholderTree, SWT.LEFT);
        returnValue.setText("");
        return returnValue;
    }

    private TreeColumn createCenterAlignedTreeColumn() {
        final TreeColumn returnValue = new TreeColumn(componentPlaceholderTree, SWT.CENTER);
        returnValue.setText("");
        return returnValue;
    }

    private TreeColumn appendCenterAlignedColumnToTree(int width) {
        final TreeColumn returnValue = createCenterAlignedTreeColumn();
        returnValue.setWidth(width);
        return returnValue;
    }

    private Image getImage(WorkflowNode element) {
        return element.getComponentDescription().getIcon16();
    }

}
