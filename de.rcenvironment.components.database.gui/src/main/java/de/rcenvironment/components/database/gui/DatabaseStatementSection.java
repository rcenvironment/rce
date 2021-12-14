/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.database.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Listener;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.components.database.common.DatabaseComponentConstants;
import de.rcenvironment.components.database.common.DatabaseStatement;
import de.rcenvironment.core.component.model.endpoint.api.EndpointChange;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Database statement section.
 *
 * @author Oliver Seebach
 * @author Kathrin Schaffert
 */
public class DatabaseStatementSection extends ValidatingWorkflowNodePropertySection {

    private static final String OUTPUT = "OUTPUT";

    private static final String INPUT = "INPUT";

    private static final String INSERT_BUTTONS_TEXT = "Insert";

    private static final int STATEMENTS_COMPOSITE_MINIMAL_WIDTH = 350;

    private static final int INPUTS_COMBO_MINIMUM_WIDTH = 100;

    private static final int TEMPLATES_COMBO_MINIMUM_WIDTH = 100;

    private static final String ADD_TAB_LABEL = "< + >";

    private static final String NEW_STATEMENT_NAME = "Statement";

    private static Map<String, String> templatesMap = new HashMap<>();

    private CCombo templatesCombo;

    private CCombo inputCombo;

    private CTabFolder statementsFolder;

    private Button insertInputButton;

    static {
        templatesMap = new HashMap<>();
        templatesMap.put(
            DatabaseComponentConstants.SELECT, "SELECT * FROM table_name WHERE column1 = 'value1';");
        templatesMap.put(
            DatabaseComponentConstants.INSERT, "INSERT INTO table_name (id, column1, column2, column3) VALUES ${in:smalltable_input};");
        templatesMap.put(
            DatabaseComponentConstants.DELETE, "DELETE FROM table_name WHERE column1 = 'value1';");
        templatesMap.put(
            DatabaseComponentConstants.UPDATE, "UPDATE table_name SET column2 = 'value2' WHERE column1 = 'value1';");
    }

    public DatabaseStatementSection() {}

    @Override
    public void setInput(IWorkbenchPart part, ISelection selection) {
        super.setInput(part, selection);
        refreshOutputCombos();

        ComponentInstanceProperties config = getConfiguration();

        config.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getNewValue() instanceof EndpointChange) {
                    EndpointType type;
                    if (statementsFolder.isDisposed()) {
                        return;
                    }
                    if (((EndpointChange) evt.getNewValue()).getOldEndpointDescription() != null) {
                        // for refreshing undo
                        type = ((EndpointChange) evt.getNewValue()).getOldEndpointDescription().getEndpointDefinition().getEndpointType();
                    } else {
                        // for refreshing redo
                        type = ((EndpointChange) evt.getNewValue()).getEndpointDescription().getEndpointDefinition().getEndpointType();
                    }
                    if (type.name().equals(INPUT)) {
                        if (getOrderedInputNames().length != 0) {
                            inputCombo.setItems(getOrderedInputNames());
                            inputCombo.setEnabled(true);
                            inputCombo.select(0); // default combo selection
                        } else {
                            inputCombo.setText(DatabaseComponentConstants.NO_INPUT_DEFINED_TEXT);
                            inputCombo.setEnabled(false);
                        }
                    }
                    if (type.name().equals(OUTPUT)) {

                        refreshOutputCombos();
                    }
                }

            }
        });

    }

    @Override
    protected void createCompositeContent(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
        super.createCompositeContent(parent, aTabbedPropertySheetPage);

        TabbedPropertySheetWidgetFactory factory = aTabbedPropertySheetPage.getWidgetFactory();

        final Section sectionStatement = factory.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        sectionStatement.setText("Database Statement");
        sectionStatement.marginWidth = 5;
        sectionStatement.marginHeight = 5;

        Composite mainComposite = new Composite(sectionStatement, SWT.NONE);
        mainComposite.setLayout(new GridLayout(2, false));
        GridData mainData = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH);
        mainComposite.setLayoutData(mainData);
        mainComposite.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        CLabel statementsOrderHintLabel = new CLabel(mainComposite, SWT.NONE);
        GridData statementHintLabelData = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH);
        statementHintLabelData.horizontalSpan = 2;
        statementsOrderHintLabel.setLayoutData(statementHintLabelData);
        statementsOrderHintLabel.setText("Note: The statements are executed sequentially from left to right within one transaction.");
        statementsOrderHintLabel.setImage(ImageManager.getInstance().getSharedImage(StandardImages.INFORMATION_16));

        // 1
        Composite statementsComposite = new Composite(mainComposite, SWT.NONE);
        statementsComposite.setLayout(new GridLayout(1, false));
        GridData statementsData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_BOTH);
        statementsData.widthHint = STATEMENTS_COMPOSITE_MINIMAL_WIDTH;
        statementsComposite.setLayoutData(statementsData);

        statementsFolder = new CTabFolder(statementsComposite, SWT.BORDER);
        GridData statementTabFolderData = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH);
        statementsFolder.setLayoutData(statementTabFolderData);
        statementsFolder.addCTabFolder2Listener(new CTabFolderClosingListener());
        statementsFolder.setSimple(false);

        // 2
        Composite helperElementsComposite = new Composite(mainComposite, SWT.NONE);
        helperElementsComposite.setLayout(new GridLayout(1, false));
        GridData helperElementsData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
        helperElementsComposite.setLayoutData(helperElementsData);

        Group inputsGroup = new Group(helperElementsComposite, SWT.NONE);
        inputsGroup.setLayout(new GridLayout(3, false));
        GridData inputsGroupData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
        inputsGroup.setLayoutData(inputsGroupData);
        inputsGroup.setText("Input");

        Label inputLabel = new Label(inputsGroup, SWT.NONE);
        inputLabel.setText("Input:");

        inputCombo = new CCombo(inputsGroup, SWT.READ_ONLY | SWT.BORDER);
        GridData inputsComboData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
        inputsComboData.minimumWidth = INPUTS_COMBO_MINIMUM_WIDTH;
        inputCombo.setLayoutData(inputsComboData);
        inputCombo.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        insertInputButton = new Button(inputsGroup, SWT.NONE);
        insertInputButton.setText(INSERT_BUTTONS_TEXT);
        insertInputButton.addSelectionListener(new InsertInputButtonListener());

        Group templatesGroup = new Group(helperElementsComposite, SWT.NONE);
        templatesGroup.setText("Templates");
        templatesGroup.setLayout(new GridLayout(3, false));
        GridData templatesGroupData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
        templatesGroup.setLayoutData(templatesGroupData);

        Label templatesLabel = new Label(templatesGroup, SWT.NONE);
        templatesLabel.setText("Template:");

        templatesCombo = new CCombo(templatesGroup, SWT.READ_ONLY | SWT.BORDER);
        GridData templatesComboData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
        templatesComboData.minimumWidth = TEMPLATES_COMBO_MINIMUM_WIDTH;
        templatesCombo.setLayoutData(templatesComboData);
        templatesCombo.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        Button insertTemplateButton = new Button(templatesGroup, SWT.NONE);
        insertTemplateButton.setText(INSERT_BUTTONS_TEXT);
        insertTemplateButton.addSelectionListener(new InsertTemplateButtonListener());

        sectionStatement.setClient(mainComposite);
    }

    private CTabItem addCTabItemToFolder(DatabaseStatement model) {
        CTabItem newCTabItem = new CTabItem(statementsFolder, SWT.CLOSE, model.getIndex());
        newCTabItem.setText(model.getName());
        DatabaseStatementComposite newDatabaseStatementComposite = new DatabaseStatementComposite(statementsFolder, SWT.NONE);
        newDatabaseStatementComposite.createControls();
        // fill gui with model
        if (model.getName() != null && !model.getName().isEmpty()) {
            newDatabaseStatementComposite.getStatementNameText().setText(model.getName());
        }
        if (model.getStatement() != null && !model.getStatement().isEmpty()) {
            newDatabaseStatementComposite.getStatementText().setText(model.getStatement());
        }
        newDatabaseStatementComposite.getWriteToOutputCheckButton().setSelection(model.isWillWriteToOutput());
        if (getOrderedOutputNames().length > 0) {
            if (model.getOutputToWriteTo() != null && !model.getOutputToWriteTo().isEmpty()) {
                newDatabaseStatementComposite.fillOutputComboAndSetSelection(getOrderedOutputNames(), model.getOutputToWriteTo());
            } else {
                newDatabaseStatementComposite.fillOutputCombo(getOrderedOutputNames());
            }
            newDatabaseStatementComposite.getOutputCombo().setEnabled(
                newDatabaseStatementComposite.getWriteToOutputCheckButton().getSelection());
        } else {
            newDatabaseStatementComposite.getOutputCombo().setEnabled(false);
            newDatabaseStatementComposite.getOutputCombo().setText(DatabaseComponentConstants.NO_OUTPUT_DEFINED_TEXT);
        }
        // register listeners

        newDatabaseStatementComposite.getStatementNameText().addFocusListener(new StatementNameFocusListener(newCTabItem));

        newDatabaseStatementComposite.getStatementText().addFocusListener(new FocusListener() {

            @Override
            public void focusLost(FocusEvent arg0) {
                writeCurrentStatementsToProperties();
            }

            @Override
            public void focusGained(FocusEvent arg0) {
                // not needed
            }
        });

        newDatabaseStatementComposite.getWriteToOutputCheckButton().addSelectionListener(new WriteToOutputSelectionChangedListener());
        newDatabaseStatementComposite.getOutputCombo().addSelectionListener(new OutputSelectionChangedListener());
        newCTabItem.setControl(newDatabaseStatementComposite);

        statementsFolder.setSelection(newCTabItem);

        return newCTabItem;
    }

//    private CTabItem addInitialCTabItemToFolder() {
//        DatabaseStatement initialModel = new DatabaseStatement();
//        initialModel.setName(INITIAL_STATEMENT_NAME);
//        initialModel.setIndex(0);
//        CTabItem newItem = addCTabItemToFolder(initialModel);
//        return newItem;
//    }

    private String determineNextValidStatementName(String currentName) {
        List<String> currentStatementNames = getCurrentStatementNames();
        List<Integer> currentUsedIndices = convertStatementNamesToIndices(currentStatementNames);
        int newIndex = 1;
        while (currentUsedIndices.contains(newIndex)) {
            newIndex++;
        }
        currentName = currentName + " (" + newIndex + ")";
        return currentName;
    }

    private List<Integer> convertStatementNamesToIndices(List<String> statementNames) {
        List<Integer> indices = new ArrayList<>();
        for (String name : statementNames) {
            int count = 0;
            if (name.contains("(") && name.contains(")")) {
                try {
                    int indexBegin = name.lastIndexOf("(") + 1;
                    int indexEnd = name.lastIndexOf(")");
                    count = Integer.valueOf(name.substring(indexBegin, indexEnd));
                } catch (NumberFormatException e) {
                    count = 0;
                }
            }
            indices.add(count);
        }
        return indices;
    }

    private CTabItem addNewCTabItem() {
        int indexToSet = statementsFolder.getItemCount() - 1;
        if (statementsFolder.getItemCount() == 0) {
            indexToSet = 0;
        }
        String newStatementName = NEW_STATEMENT_NAME;
        String name = determineNextValidStatementName(newStatementName);
        DatabaseStatement newModel = new DatabaseStatement();
        newModel.setName(name);
        newModel.setIndex(indexToSet);
        CTabItem newItem = addCTabItemToFolder(newModel); // Add before "+" item
        statementsFolder.setSelection(statementsFolder.getItemCount() - 2); // Select new created item
        refreshOutputCombos();
        return newItem;
    }

    private void addPlusCTabItemToFolder() {
        CTabItem addNewItemTab = new CTabItem(statementsFolder, SWT.NONE);
        addNewItemTab.setText(ADD_TAB_LABEL);
        addNewItemTab.setShowClose(false);
    }

    @Override
    public void aboutToBeShown() {
        super.aboutToBeShown();
        // inputs combo
        inputCombo.removeAll();
        inputCombo.setItems(getOrderedInputNames());
        if (inputCombo.getItemCount() > 0) {
            insertInputButton.setEnabled(true);
            inputCombo.setEnabled(true);
            inputCombo.select(0); // default combo selection
        } else {
            inputCombo.setEnabled(false);
            inputCombo.setText(DatabaseComponentConstants.NO_INPUT_DEFINED_TEXT);
            insertInputButton.setEnabled(false);
        }
        // templates combo
        templatesCombo.removeAll();
        templatesCombo.setItems(getOrderedTemplateNames());
        if (templatesCombo.getItemCount() > 0) {
            templatesCombo.select(0); // default combo selection
        }
        // outputs combos
        refreshOutputCombos();

        // statement stuff
        for (CTabItem item : statementsFolder.getItems()) {
            item.dispose();
        }

        List<DatabaseStatement> models = readCurrentDatabaseStatementsFromConfig();

        statementsFolder.setRedraw(false);

        // at least one statement found
        if (!models.isEmpty()) {
            for (DatabaseStatement model : models) {
                addCTabItemToFolder(model);
            }
        }

        // and the plus item
        statementsFolder.setSelection(0);
        addPlusCTabItemToFolder();

        statementsFolder.setRedraw(true);

        // make sure just one listener is registered.
        if (statementsFolder.getData("SelectionListenerToken") == null) {
            statementsFolder.addSelectionListener(new StatementsFolderSelectionListener());
            statementsFolder.setData("SelectionListenerToken", true);
        }
        if (statementsFolder.getData("StatementsFolderListenerToken") == null) {
            statementsFolder.addMouseListener(new StatementsFolderMouseListener());
            statementsFolder.setData("StatementsFolderListenerToken", true);
        }
    }

    private void refreshOutputCombos() {
        for (CTabItem item : statementsFolder.getItems()) {
            if (!item.getText().equals(ADD_TAB_LABEL)) {
                if (item.getControl() instanceof DatabaseStatementComposite) {
                    DatabaseStatementComposite control = (DatabaseStatementComposite) item.getControl();
                    String selection = getOutputToWriteToByStatementName(item.getText());
                    boolean selected = control.getWriteToOutputCheckButton().getSelection();
                    fillOutputsCombo(control.getOutputCombo(), selection, selected);
                }
            }
        }
    }

    private String getOutputToWriteToByStatementName(String statementName) {
        List<DatabaseStatement> statements = readCurrentDatabaseStatementsFromConfig();
        for (DatabaseStatement statement : statements) {
            if (statement.getName().equals(statementName)) {
                return statement.getOutputToWriteTo();
            }
        }
        return null;
    }

    private void fillOutputsCombo(CCombo comboToFill, String currentSelection, boolean enabled) {
        comboToFill.removeAll();
        if (getOutputNamesUnordered().isEmpty()) {
            // if no outputs are there: add placeholder
            comboToFill.setEnabled(false);
            comboToFill.setText(DatabaseComponentConstants.NO_OUTPUT_DEFINED_TEXT);
        } else {
            // else add them and set previous selection
            comboToFill.setEnabled(enabled);
            comboToFill.setItems(getOrderedOutputNames());
            if (currentSelection != null) {
                if (Arrays.asList(comboToFill.getItems()).contains(currentSelection)) {
                    comboToFill.select(comboToFill.indexOf(currentSelection));
                }
            }
        }
    }

    /**
     * Listener to restrict closing of tabs.
     *
     * @author Oliver Seebach
     */
    private final class CTabFolderClosingListener implements CTabFolder2Listener {

        @Override
        public void showList(CTabFolderEvent event) {}

        @Override
        public void restore(CTabFolderEvent event) {}

        @Override
        public void minimize(CTabFolderEvent event) {}

        @Override
        public void maximize(CTabFolderEvent event) {}

        @Override
        public void close(CTabFolderEvent event) {
            // prevent closing of < + > item and if there are just 2 tabs present
            if (event.item instanceof CTabItem) {
                CTabItem selectedItem = (CTabItem) event.item;
                CTabFolder tabFolder = (CTabFolder) event.widget;
                if (selectedItem.getText().equals(ADD_TAB_LABEL) || tabFolder.getItemCount() < 3) {
                    event.doit = false;
                } else {
                    selectedItem.dispose();
                    writeCurrentStatementsToProperties();
                }
            }
        }
    }

    /**
     * Listener to handle item adding.
     *
     * @author Oliver Seebach
     */
    private final class StatementsFolderSelectionListener implements SelectionListener {

        @Override
        public void widgetSelected(SelectionEvent event) {
            if (event.item instanceof CTabItem) {
                CTabItem clickedItem = (CTabItem) event.item;
                if (clickedItem.getText().equals(ADD_TAB_LABEL) && statementsFolder.getItemCount() > 1) {
                    addNewCTabItem();
                    writeCurrentStatementsToProperties();
                }
            }
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent event) {
            widgetSelected(event);
        }
    }

    /**
     * Listener to handle tab closing via mouse wheel.
     *
     * @author Oliver Seebach
     */
    private final class StatementsFolderMouseListener extends MouseAdapter {

        @Override
        public void mouseDown(MouseEvent event) {
            // FIXME: make sure the item where the mouse is over is disposed or discard mouse wheel closing -- seeb_ol, November 2015
            // private static final int MOUSE_WHEEL_BUTTON_ID = 2;
            // if (event.button == MOUSE_WHEEL_BUTTON_ID) {
            // CTabItem item = ((CTabFolder) event.widget).getItem(((CTabFolder) event.widget).getSelectionIndex());
            // CTabFolder tabFolder = (CTabFolder) event.widget;
            // if (!item.getText().equals(ADD_TAB_LABEL) && tabFolder.getItemCount() > 2) {
            // item.dispose();
            // }
            // }
        }
    }

    /**
     * Listener to insert templates into statement textfields.
     *
     * @author Oliver Seebach
     */
    private final class InsertTemplateButtonListener implements SelectionListener {

        @Override
        public void widgetSelected(SelectionEvent event) {
            String templateToInsert = templatesMap.get(templatesCombo.getText());
            CTabItem currentItem = statementsFolder.getItem(statementsFolder.getSelectionIndex());
            if (currentItem.getControl() instanceof DatabaseStatementComposite) {
                DatabaseStatementComposite statementComposite = (DatabaseStatementComposite) currentItem.getControl();
                statementComposite.getStatementText().insert(templateToInsert);
            }
            writeCurrentStatementsToProperties();
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent event) {
            widgetSelected(event);
        }
    }

    /**
     * Listener to add input reference to statement text.
     *
     * @author Oliver Seebach
     */
    private final class InsertInputButtonListener implements SelectionListener {

        @Override
        public void widgetSelected(SelectionEvent event) {

            String inputToInsert = inputCombo.getText();
            String textToInsert = "";
            if (inputToInsert != null && !inputToInsert.isEmpty()) {
                textToInsert = StringUtils.format(DatabaseComponentConstants.INPUT_PLACEHOLDER_PATTERN, inputToInsert);
                CTabItem currentItem = statementsFolder.getItem(statementsFolder.getSelectionIndex());
                if (currentItem.getControl() instanceof DatabaseStatementComposite) {
                    if (!textToInsert.isEmpty()) {
                        DatabaseStatementComposite statementComposite = (DatabaseStatementComposite) currentItem.getControl();
                        statementComposite.getStatementText().insert(textToInsert);
                    }
                }
            }
            writeCurrentStatementsToProperties();
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent event) {
            widgetSelected(event);
        }
    }

    /**
     * Listener to react on statement name modification.
     *
     * @author Oliver Seebach, Kathrin Schaffert
     */
    private final class StatementNameFocusListener implements FocusListener {

        private final CTabItem statementItem;

        // Flag is used to avoid triggering "focusLost", when the composite is initialized for the first time and any user input is done.
        // This is, because the first focus, when initializing the composite, is set to the "Statement Name" text field.
        // Afterwards the flag is set to true and will not become false until the composite is disposed.
        // (K. Schaffert, 25.04.2019)
        private boolean flag;

        private StatementNameFocusListener(CTabItem statementItem) {
            this.statementItem = statementItem;
            flag = false;
        }

        @Override
        public void focusLost(FocusEvent event) {

            // If the first user input (flag = false) is NOT changing the "Statement Name", we avoid writing unsubstantial statements to the
            // configuration. (K. Schaffert, 25.04.2019)
            if (flag || !((Text) event.widget).getText().equals(NEW_STATEMENT_NAME)) {
                statementItem.setText(((Text) event.widget).getText());
                writeCurrentStatementsToProperties();
            }
            flag = true;
        }

        @Override
        public void focusGained(FocusEvent event) {
            // TODO Auto-generated method stub

        }
    }

    /**
     * Listener that reacts on output selection changes.
     *
     * @author Oliver Seebach
     */
    private final class OutputSelectionChangedListener implements SelectionListener {

        @Override
        public void widgetDefaultSelected(SelectionEvent event) {}

        @Override
        public void widgetSelected(SelectionEvent event) {
            writeCurrentStatementsToProperties();
        }

    }

    /**
     * Listener that reacts on output checkbox changes.
     *
     * @author Oliver Seebach
     */
    private final class WriteToOutputSelectionChangedListener implements SelectionListener {

        @Override
        public void widgetDefaultSelected(SelectionEvent event) {
            widgetSelected(event);
        }

        @Override
        public void widgetSelected(SelectionEvent event) {
            boolean selected = false;
            if (event.getSource() instanceof Button) {
                Button clickedButton = (Button) event.getSource();
                selected = clickedButton.getSelection();
            }
            CTabItem currentItem = statementsFolder.getItem(statementsFolder.getSelectionIndex());
            if (currentItem.getControl() instanceof DatabaseStatementComposite) {
                DatabaseStatementComposite statementComposite = (DatabaseStatementComposite) currentItem.getControl();
                if (statementComposite.getOutputCombo().getItemCount() == 0) {
                    statementComposite.getOutputCombo().setEnabled(false);
                } else if (statementComposite.getOutputCombo().getItem(0).equals(DatabaseComponentConstants.NO_OUTPUT_DEFINED_TEXT)) {
                    statementComposite.getOutputCombo().setEnabled(false);
                } else {
                    statementComposite.getOutputCombo().setEnabled(selected);
                }
            }
            writeCurrentStatementsToProperties();
        }

    }

    private List<String> getCurrentStatementNames() {
        List<String> statementNames = new ArrayList<>();
        for (CTabItem tabItem : statementsFolder.getItems()) {
            statementNames.add(tabItem.getText());
        }
        return statementNames;
    }

    private String[] getOrderedInputNames() {
        List<String> inputChannelNames = new ArrayList<String>();
        for (EndpointDescription channelName : getConfiguration().getInputDescriptionsManager().getDynamicEndpointDescriptions()) {
            inputChannelNames.add(channelName.getName());
        }
        Collections.sort(inputChannelNames, String.CASE_INSENSITIVE_ORDER);
        String[] inputChannelNamesArray = new String[inputChannelNames.size()];
        for (int i = 0; i < inputChannelNames.size(); i++) {
            inputChannelNamesArray[i] = inputChannelNames.get(i);
        }
        return inputChannelNamesArray;
    }

    private String[] getOrderedOutputNames() {
        List<String> outputChannelNames = new ArrayList<String>();
        for (EndpointDescription channelName : getConfiguration().getOutputDescriptionsManager().getDynamicEndpointDescriptions()) {
            outputChannelNames.add(channelName.getName());
        }
        Collections.sort(outputChannelNames, String.CASE_INSENSITIVE_ORDER);
        String[] inputChannelNamesArray = new String[outputChannelNames.size()];
        for (int i = 0; i < outputChannelNames.size(); i++) {
            inputChannelNamesArray[i] = outputChannelNames.get(i);
        }
        return inputChannelNamesArray;
    }

    private String[] getOrderedTemplateNames() {
        List<String> templateNames = new ArrayList<String>();
        for (String templateName : templatesMap.keySet()) {
            templateNames.add(templateName);
        }
        Collections.sort(templateNames, String.CASE_INSENSITIVE_ORDER);
        String[] templateNamesArray = new String[templateNames.size()];
        for (int i = 0; i < templateNames.size(); i++) {
            templateNamesArray[i] = templateNames.get(i);
        }
        return templateNamesArray;
    }

    private List<String> getOutputNamesUnordered() {
        List<String> outputNames = new ArrayList<>();
        for (EndpointDescription output : getOutputs()) {
            // add only dynamic endpoints
            if (!output.getEndpointDefinition().isStatic()) {
                outputNames.add(output.getName());
            }
        }
        return outputNames;
    }

    // READ MODEL
    private List<DatabaseStatement> readCurrentDatabaseStatementsFromConfig() {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        List<DatabaseStatement> models = new ArrayList<>();
        try {
            String modelsString = getProperty(DatabaseComponentConstants.DB_STATEMENTS_KEY);
            if (modelsString != null) {
                models =
                    mapper.readValue(modelsString, mapper.getTypeFactory().constructCollectionType(List.class, DatabaseStatement.class));
            }
        } catch (JsonGenerationException | JsonMappingException e) {
            logger.error("Failed to map database component's JSON content to database statements.");
        } catch (IOException e) {
            logger.error("Failed to load database component's JSON content from file system.");
        }
        return models;
    }

    @Override
    protected void setWorkflowNode(WorkflowNode workflowNode) {
        super.setWorkflowNode(workflowNode);
        aboutToBeShown();
    }

    // WRITE MODEL
    private void writeCurrentStatementsToProperties() {
        List<DatabaseStatement> currentStatements = new ArrayList<>();
        if (!statementsFolder.isDisposed()) {
            for (CTabItem item : statementsFolder.getItems()) {
                if (/* !item.getText().isEmpty() && */!item.getText().equals(ADD_TAB_LABEL)) {
                    int index = statementsFolder.indexOf(item); // Position of tab in folder
                    DatabaseStatementComposite control = (DatabaseStatementComposite) item.getControl();
                    DatabaseStatement statement = new DatabaseStatement();
                    statement.setName(control.getStatementNameText().getText());
                    statement.setStatement(control.getStatementText().getText());
                    statement.setWillWriteToOutput(control.getWriteToOutputCheckButton().getSelection());
                    String outputToWriteTo = control.getOutputCombo().getText();
                    statement.setOutputToWriteTo(outputToWriteTo);
                    statement.setIndex(index);
                    currentStatements.add(statement);
                }
            }
            ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
            String currentStatementsString = null;
            try {
                currentStatementsString = mapper.writeValueAsString(currentStatements);
            } catch (JsonGenerationException | JsonMappingException e) {
                logger.error("Failed to map database statements to database component's JSON content.");
            } catch (IOException e) {
                logger.error("Failed to write database component's statements to file system.");
            }
            if (currentStatementsString != null && (getProperty(DatabaseComponentConstants.DB_STATEMENTS_KEY) == null
                || !getProperty(DatabaseComponentConstants.DB_STATEMENTS_KEY).equals(currentStatementsString))) {
                setProperty(DatabaseComponentConstants.DB_STATEMENTS_KEY, currentStatementsString);
            }
        }
    }

    @Override
    protected DatabaseStatementSectionUpdater createUpdater() {
        return new DatabaseStatementSectionUpdater();
    }

    /**
     * Database Statement Section {@link DefaultUpdater} implementation of the handler to update the Statement Section UI.
     * 
     * @author Kathrin Schaffert
     * 
     */
    protected class DatabaseStatementSectionUpdater extends DefaultUpdater {

        @Override
        public void updateControl(Control control, String propertyName, String newValue, String oldValue) {

            List<DatabaseStatement> models = readCurrentDatabaseStatementsFromConfig();

            statementsFolder.setRedraw(false);

            int tabNumber = statementsFolder.getItems().length - 1;
            // refreshing folders after undo
            if (models.size() > tabNumber) {
                for (DatabaseStatement model : models) {
                    boolean val = true;
                    for (int i = 0; i < statementsFolder.getItems().length; i++) {
                        if (statementsFolder.getItems()[i].getText().equals(model.getName())) {
                            val = false;
                            break;
                        }
                    }
                    if (val) {
                        addCTabItemToFolder(model);
                    }
                }
            }
            // refreshing folders after redo
            if (models.size() < tabNumber) {
                for (int i = 0; i < statementsFolder.getItems().length; i++) {
                    boolean val = true;
                    for (DatabaseStatement model : models) {
                        if (statementsFolder.getItems()[i].getText().equals(model.getName())) {
                            val = false;
                            break;
                        }
                    }
                    if (val) {
                        statementsFolder.getItems()[i].dispose();
                    }
                }
            }

            for (DatabaseStatement model : models) {
                CTabItem item = statementsFolder.getItem(model.getIndex());
                if (item.getControl() instanceof DatabaseStatementComposite) {
                    DatabaseStatementComposite dsc = (DatabaseStatementComposite) item.getControl();
                    // refreshing Statement Name
                    if (!dsc.getStatementNameText().getText().equals(model.getName())) {
                        dsc.getStatementNameText().setText(model.getName());
                        item.setText(model.getName());
                    }
                    // refreshing Statement Text
                    if (!dsc.getStatementText().getText().equals(model.getStatement())) {
                        dsc.getStatementText().setText(model.getStatement());
                    }
                    // refreshing Checkbox "write result to output"
                    if (dsc.getWriteToOutputCheckButton().getSelection() != model.isWillWriteToOutput()) {
                        dsc.getWriteToOutputCheckButton().setSelection(model.isWillWriteToOutput());
                        if (model.isWillWriteToOutput()
                            && !model.getOutputToWriteTo().equals(DatabaseComponentConstants.NO_OUTPUT_DEFINED_TEXT)) {
                            dsc.getOutputCombo().setEnabled(true);
                        } else {
                            dsc.getOutputCombo().setEnabled(false);
                        }
                    } else {
                        // refreshing Combo
                        if (!dsc.getOutputCombo().getText().equals(model.getOutputToWriteTo())) {
                            if (model.getOutputToWriteTo().equals("")) {
                                dsc.getOutputCombo().setText("");
                            } else {
                                dsc.getOutputCombo().setText(model.getOutputToWriteTo());
                                if (model.getOutputToWriteTo().equals(DatabaseComponentConstants.NO_OUTPUT_DEFINED_TEXT)) {
                                    dsc.getOutputCombo().setEnabled(true);
                                    dsc.getOutputCombo().setText("");
                                }
                            }
                        }
                    }
                }
            }
            statementsFolder.setRedraw(true);
        }
    }
}
