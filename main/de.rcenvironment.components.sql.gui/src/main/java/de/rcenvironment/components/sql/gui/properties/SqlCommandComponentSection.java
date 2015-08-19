/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.sql.gui.properties;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.components.sql.common.JDBCProfile;
import de.rcenvironment.components.sql.common.JDBCService;
import de.rcenvironment.components.sql.common.SqlComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodePropertySection;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Configuration section to configure a {@link SqlReaderComponent}.
 * 
 * @author Christian Weiss
 */
public class SqlCommandComponentSection extends WorkflowNodePropertySection {

    private static final String PLACEHOLDER_PATTERN = "${%s}";
    
    private static final String OUTPUT_PATTERN = String.format(PLACEHOLDER_PATTERN, "out:%s");

    private static final int WIDTH_HINT = 200;
    
    private static final int MINIMUM_HEIGHT_TEXTFIELDS = 60;

    private static final int MAXIMUM_HEIGHT_TEXTFIELDS = 500;

    private static final int BORDER_MARGIN = 70;

    private static final int BORDER_MARGIN_BETWEEN_MIN_MAX = 90;

    private static final int GETTING_SMALLER_MARGIN = 80;

    private final Map<String, String> variablesPlaceholders = new HashMap<String, String>();

    private CCombo jdbcProfileCombo;

    private Button sqlInitStatementCheckbox;
    
    private Text sqlInitStatementText;

    private CCombo initVariablesCombo;

    private Button initVariablesInsertButton;

    private Text sqlStatementText;
    
    private Button createSqlStatementButton;

    private CCombo runVariablesCombo;

    private Button runVariablesInsertButton;

    private ServiceRegistryAccess serviceRegistryAccess;

    public SqlCommandComponentSection() {
        serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
    }
    
    @Override
    public void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {
        final TabbedPropertySheetWidgetFactory toolkit = aTabbedPropertySheetPage.getWidgetFactory();
        
        final Composite content = new LayoutComposite(parent);
        content.setLayout(new GridLayout(2, true));
        
        // Connections Sections
        final Composite jdbcContainer = toolkit.createFlatFormComposite(content);
        initConnectionsSection(toolkit, jdbcContainer);
        
        toolkit.createComposite(content);
//        // SQL Section
//        final Composite sqlContainer = toolkit.createFlatFormComposite(content);
//        initSqlSection(toolkit, sqlContainer);

        // Init Section
        final Composite initContainer = toolkit.createFlatFormComposite(content);
        initInitSection(toolkit, initContainer);
        
        // Run Section
        final Composite runContainer = toolkit.createFlatFormComposite(content);
        initRunSection(toolkit, runContainer);
       
        parent.getParent().addListener(SWT.Resize,  new Listener() {
            public void handleEvent(Event e) {

                if (parent.getParent().getSize().y < MINIMUM_HEIGHT_TEXTFIELDS){
                    ((GridData) initContainer.getLayoutData()).heightHint = MINIMUM_HEIGHT_TEXTFIELDS;
                    ((GridData) runContainer.getLayoutData()).heightHint = MINIMUM_HEIGHT_TEXTFIELDS;
                } else if (parent.getParent().getSize().y > MAXIMUM_HEIGHT_TEXTFIELDS + BORDER_MARGIN) {
                    ((GridData) initContainer.getLayoutData()).heightHint = MAXIMUM_HEIGHT_TEXTFIELDS;
                    ((GridData) runContainer.getLayoutData()).heightHint = MAXIMUM_HEIGHT_TEXTFIELDS;
                } else {
                    ((GridData) initContainer.getLayoutData()).heightHint = parent.getParent().getSize().y - BORDER_MARGIN_BETWEEN_MIN_MAX;
                    ((GridData) runContainer.getLayoutData()).heightHint = parent.getParent().getSize().y - BORDER_MARGIN_BETWEEN_MIN_MAX;
                }
                if (initContainer.getSize().y == MAXIMUM_HEIGHT_TEXTFIELDS 
                    && parent.getParent().getSize().y < MAXIMUM_HEIGHT_TEXTFIELDS + GETTING_SMALLER_MARGIN){
                    ((GridData) initContainer.getLayoutData()).heightHint = MAXIMUM_HEIGHT_TEXTFIELDS - BORDER_MARGIN_BETWEEN_MIN_MAX;
                    ((GridData) runContainer.getLayoutData()).heightHint = MAXIMUM_HEIGHT_TEXTFIELDS - BORDER_MARGIN_BETWEEN_MIN_MAX;
                }
            }
        });
    }

    private void initConnectionsSection(final TabbedPropertySheetWidgetFactory toolkit, final Composite jdbcContainer) {
        GridData layoutData;
        layoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
        jdbcContainer.setLayoutData(layoutData);
        jdbcContainer.setLayout(new FillLayout());
        final Section jdbcSection = toolkit.createSection(jdbcContainer, Section.TITLE_BAR | Section.EXPANDED);
        jdbcSection.setText(Messages.readerSectionConnectionSettingTitle);
        final Composite jdbcClient = toolkit.createComposite(jdbcSection);
        layoutData = new GridData(GridData.FILL_HORIZONTAL);
        jdbcClient.setLayoutData(layoutData);
        jdbcClient.setLayout(new GridLayout(2, false));
        final Label jdbcLabel = new Label(jdbcClient, SWT.NONE);
        jdbcLabel.setText(Messages.readerSectionJDBCProfileLabel);
        jdbcProfileCombo = toolkit.createCCombo(jdbcClient, SWT.DROP_DOWN | SWT.READ_ONLY);
        jdbcProfileCombo.setData(CONTROL_PROPERTY_KEY, SqlComponentConstants.METADATA_JDBC_PROFILE_PROPERTY);
        layoutData = new GridData(GridData.FILL_HORIZONTAL);
        jdbcProfileCombo.setLayoutData(layoutData);
        jdbcSection.setClient(jdbcClient);
    }

    private void initInitSection(final TabbedPropertySheetWidgetFactory toolkit, final Composite initContainer) {
        GridData layoutData;
        layoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
        initContainer.setLayoutData(layoutData);
        initContainer.setLayout(new FillLayout());
        final Section initSection = toolkit.createSection(initContainer, Section.TITLE_BAR | Section.EXPANDED);
        initSection.setText(Messages.readerSectionInitSectionTitle);
        final Composite initClient = toolkit.createComposite(initSection);
        initClient.setLayout(new GridLayout(1, false));
        sqlInitStatementText = toolkit.createText(initClient, "", SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.WRAP);
        sqlInitStatementText.setData(CONTROL_PROPERTY_KEY, SqlComponentConstants.METADATA_SQL_INIT_PROPERTY);
        layoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_VERTICAL);
        layoutData.minimumHeight = MINIMUM_HEIGHT_TEXTFIELDS;
        layoutData.heightHint = MINIMUM_HEIGHT_TEXTFIELDS;
        layoutData.widthHint = WIDTH_HINT;
        sqlInitStatementText.setLayoutData(layoutData);
        addHotkeyToTextfield(sqlInitStatementText);
        // Variables Insertion
        final Composite initVariablesInsertionComposite = new Composite(initClient, SWT.NONE);
        initVariablesInsertionComposite.setLayout(new GridLayout(3, false));
        layoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END);
        initVariablesInsertionComposite.setLayoutData(layoutData);
        toolkit.createLabel(initVariablesInsertionComposite, Messages.variablesLabel);
        initVariablesCombo = toolkit.createCCombo(initVariablesInsertionComposite, SWT.DROP_DOWN | SWT.READ_ONLY);
        toolkit.paintBordersFor(initVariablesCombo);
        layoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        initVariablesCombo.setLayoutData(layoutData);
        initVariablesInsertButton = toolkit.createButton(initVariablesInsertionComposite,
                Messages.variablesInsertButtonLabel, SWT.PUSH);
        sqlInitStatementCheckbox = toolkit.createButton(initClient, Messages.readerSectionSQLStatementInitDoLabel, SWT.CHECK);
        sqlInitStatementCheckbox.setData(CONTROL_PROPERTY_KEY, SqlComponentConstants.METADATA_DO_SQL_INIT_PROPERTY);
        layoutData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        sqlInitStatementCheckbox.setLayoutData(layoutData);
        initSection.setClient(initClient);
    }

    private void initRunSection(final TabbedPropertySheetWidgetFactory toolkit, final Composite runContainer) {
        GridData layoutData;
        layoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
        runContainer.setLayoutData(layoutData);
        runContainer.setLayout(new FillLayout());
        final Section runSection = toolkit.createSection(runContainer, Section.TITLE_BAR | Section.EXPANDED);
        runSection.setText(Messages.readerSectionRunSectionTitle);
        final Composite runClient = toolkit.createComposite(runSection);
        runClient.setLayout(new GridLayout(1, false));
        sqlStatementText = toolkit.createText(runClient, "", SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.WRAP);
        sqlStatementText.setData(CONTROL_PROPERTY_KEY, SqlComponentConstants.METADATA_SQL_PROPERTY);
        layoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_VERTICAL);
        layoutData.minimumHeight = MINIMUM_HEIGHT_TEXTFIELDS;
        layoutData.heightHint = MINIMUM_HEIGHT_TEXTFIELDS;
        layoutData.widthHint = WIDTH_HINT;
        sqlStatementText.setLayoutData(layoutData);
        addHotkeyToTextfield(sqlStatementText);
        // Variables Insertion
        final Composite initVariablesInsertionComposite = new Composite(runClient, SWT.NONE);
        initVariablesInsertionComposite.setLayout(new GridLayout(3, false));
        layoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END);
        initVariablesInsertionComposite.setLayoutData(layoutData);
        toolkit.createLabel(initVariablesInsertionComposite, Messages.variablesLabel);
        runVariablesCombo = toolkit.createCCombo(initVariablesInsertionComposite, SWT.DROP_DOWN | SWT.READ_ONLY);
        toolkit.paintBordersFor(runVariablesCombo);
        layoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        runVariablesCombo.setLayoutData(layoutData);
        runVariablesInsertButton = toolkit.createButton(initVariablesInsertionComposite,
                Messages.variablesInsertButtonLabel, SWT.PUSH);
        // auto sql button
        createSqlStatementButton = toolkit.createButton(runClient, Messages.readerSectionGenerateButtonLabel, SWT.PUSH);
        layoutData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        createSqlStatementButton.setLayoutData(layoutData);
        runSection.setClient(runClient);
    }

    @Override
    protected Controller createController() {
        return new ControllerImpl();
    }

    @Override
    protected Synchronizer createSynchronizer() {
        return new SynchronizerImpl();
    }

    private void setJdbcProfile(final String profileLabel) {
        final String oldValue = getJdbcProfile();
        if (!profileLabel.equals(oldValue)) {
            final WorkflowNodeCommand command = new SetConfigurationValueCommand(
                SqlComponentConstants.METADATA_JDBC_PROFILE_PROPERTY,
                oldValue,
                profileLabel);
            execute(command);
        }
    }

    private String getJdbcProfile() {
        final String result = getProperty(SqlComponentConstants.METADATA_JDBC_PROFILE_PROPERTY);
        return result;
    }
    
    private void setSqlStatement(final String sqlStatement) {
        final String oldValue = getSqlStatement();
        if (!sqlStatement.equals(oldValue)) {
            final WorkflowNodeCommand command = new SetConfigurationValueCommand(
                SqlComponentConstants.METADATA_SQL_PROPERTY,
                oldValue,
                sqlStatement);
            execute(command);
        }
    }
    
    private String getSqlStatement() {
        final String result = getProperty(SqlComponentConstants.METADATA_SQL_PROPERTY);
        return result;
    }
    
    private void setAutoSqlStatement() {
        final String sqlStatement = generateSqlStatement();
        final boolean replaceConfirmationResult = MessageDialog.openQuestion(
                Display.getDefault().getActiveShell(), "Replace SQL statement?", "Do you really want to ...?");
        if (replaceConfirmationResult) {
            setSqlStatement(sqlStatement);
        }
    }

    /**
     * Automatically generates a SQL SELECT statement that matches the specification of the outputs.
     * 
     * @return an SQL SELECT statement
     */
    private String generateSqlStatement() {
        final StringBuilder builder = new StringBuilder();
        builder.append("SELECT ");
        /*
         * Select single columns, if data outputs are defined and NO full data outputs exist.
         * Otherwise all columns must be selected. 
         */
        if (hasDataOutputs() && !hasFullDataOutputs()) {
            boolean first = true;
            for (EndpointDescription entry : getDataOutputs()) {
                if (first) {
                    first = false;
                } else {
                    builder.append(", ");
                }
                final String columnName = entry.getName();
                builder.append(columnName);
            }
        } else {
            builder.append("*");
        }
        builder.append(" FROM ${tableName}");
        return builder.toString();
    }

    @Deprecated // use base class functions instead
    protected boolean hasFullDataOutputs() {
        boolean result = !getFullDataOutputs().isEmpty();
        return result;
    }

    @Deprecated // use base class functions instead
    protected Set<EndpointDescription> getFullDataOutputs() {
        final Set<EndpointDescription> result = new HashSet<EndpointDescription>();
        for (EndpointDescription entry : getConfiguration().getOutputDescriptionsManager().getEndpointDescriptions()) {
            final DataType value = entry.getDataType();
            if (DataType.SmallTable == value) {
                result.add(entry);
            }
        }
        return result;
    }

    @Deprecated // use base class functions instead
    protected boolean hasDataOutputs() {
        boolean result = !getDataOutputs().isEmpty();
        return result;
    }

    @Deprecated // use base class functions instead
    protected Set<EndpointDescription> getDataOutputs() {
        final Set<EndpointDescription> result = new HashSet<EndpointDescription>();
        for (EndpointDescription entry : getConfiguration().getOutputDescriptionsManager().getEndpointDescriptions()) {
            final String name = entry.getName();
            if (name.contains(".")) {
                continue;
            }
            final DataType value = entry.getDataType();
            if (DataType.SmallTable != value) {
                result.add(entry);
            }
        }
        return result;
    }

    @Deprecated // use base class functions instead
    @Override
    protected boolean hasInputs() {
        return !getConfiguration().getInputDescriptionsManager().getEndpointDescriptions().isEmpty();
    }

    private boolean isInitCommandEnabled() {
        return sqlInitStatementCheckbox.getSelection();
    }
    
    private boolean isRunCommandEnabled() {
        return !getConfiguration().getInputDescriptionsManager().getEndpointDescriptions().isEmpty();
    }

    private void updateVariableInsertControls() {
        final boolean hasInitVariableReplacements = initVariablesCombo.getItems().length > 0;
        final boolean enableInitVariableInsertControl = hasInitVariableReplacements & isInitCommandEnabled();
        initVariablesCombo.setEnabled(enableInitVariableInsertControl);
        initVariablesInsertButton.setEnabled(enableInitVariableInsertControl);
        if (hasInitVariableReplacements) {
            initVariablesCombo.select(0);
        }
        final boolean hasRunVariableReplacements = runVariablesCombo.getItems().length > 0;
        final boolean enableRunVariableInsertControl = hasRunVariableReplacements & isRunCommandEnabled();
        runVariablesCombo.setEnabled(enableRunVariableInsertControl);
        runVariablesInsertButton.setEnabled(enableRunVariableInsertControl);
        if (hasRunVariableReplacements) {
            runVariablesCombo.select(0);
        }
    }

    @Override
    public void refreshSection() {
        super.refreshSection();
        //
        initVariablesCombo.removeAll();
        runVariablesCombo.removeAll();
        variablesPlaceholders.clear();
        // JDBC Profile
        JDBCService jdbcService = serviceRegistryAccess.getService(JDBCService.class);
        // Generate Button
        createSqlStatementButton.setEnabled(false);
        // JDBC Profile
        jdbcProfileCombo.removeAll();
        jdbcProfileCombo.add(Messages.readerSectionJDBCProfileNullItem);
        final String currentValue = getJdbcProfile();
        boolean containsCurrentValue = currentValue == null;
        final List<JDBCProfile> profiles = jdbcService.getProfiles();
        for (int index = 0; index < profiles.size(); ++index) {
            final JDBCProfile jdbcProfile = profiles.get(index);
            final String label = jdbcProfile.getLabel();
            jdbcProfileCombo.add(label);
            final boolean isCurrentValue = label.equals(currentValue);
            containsCurrentValue |= isCurrentValue;
            if (isCurrentValue) {
                final int comboIndex = index + 1;
                jdbcProfileCombo.select(comboIndex);
            }
        }
        if (currentValue != null && !currentValue.isEmpty() && !containsCurrentValue) {
            final String message = Messages.bind(Messages.currentJdbcProfileButMissing, currentValue);
            jdbcProfileCombo.add(message, 0);
            jdbcProfileCombo.select(0);
        }
//        final boolean enableSelectAll = hasOutputs && hasFullDataOutputs() && hasDataOutputs();
//        selectAllButton.setEnabled(enableSelectAll);
        refreshSqlInitStatement();
        refreshSqlStatement();
        // variable combo
        final ComponentInstanceProperties configuration = getReadableConfiguration();
        // add table name meta replacement value
        final String tableNamePlaceholder = "tableName";
        final String tableNameLabel = Messages.bind(Messages.readerSectionTableNameLabelMeta, tableNamePlaceholder);
        initVariablesCombo.add(tableNameLabel);
        runVariablesCombo.add(tableNameLabel);
        variablesPlaceholders.put(tableNameLabel, String.format(PLACEHOLDER_PATTERN, tableNamePlaceholder));
        // add input replacement values
        for (final EndpointDescription desc : configuration.getInputDescriptionsManager().getEndpointDescriptions()) {
            String inputName = desc.getName();
            final String inputType = configuration.getInputDescriptionsManager().getEndpointDescription(inputName).getDataType().toString();
            final String label = Messages.bind(Messages.variablesInputPattern, inputName, inputType);
            final String placeholder = String.format(PLACEHOLDER_PATTERN, inputName);
            runVariablesCombo.add(label);
            variablesPlaceholders.put(label, placeholder);
        }
        // add output replacement values
        for (final EndpointDescription desc : configuration.getOutputDescriptionsManager().getEndpointDescriptions()) {
            String outputName = desc.getName();
            final String outputType = configuration.getOutputDescriptionsManager().getEndpointDescription(outputName)
                .getDataType().toString();
            final String label = Messages.bind(Messages.variablesOutputPattern, outputName, outputType);
            final String placeholder = String.format(OUTPUT_PATTERN, outputName);
            initVariablesCombo.add(label);
            runVariablesCombo.add(label);
            variablesPlaceholders.put(label, placeholder);
        }
        updateVariableInsertControls();
    }

    protected void refreshSqlStatement() {
        final boolean hasInputs = hasInputs();
        sqlStatementText.setEnabled(hasInputs);
        // Generate Button
        final boolean hasOutputs = hasFullDataOutputs() || hasDataOutputs();
        createSqlStatementButton.setEnabled(hasInputs && hasOutputs);
    }

    protected void refreshSqlInitStatement() {
        sqlInitStatementText.setEnabled(sqlInitStatementCheckbox.getSelection());
        updateVariableInsertControls();
    }
    
    private void addHotkeyToTextfield(final Text textfield){
        textfield.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.stateMask == SWT.CTRL && e.keyCode == 'a'){
                    textfield.selectAll();
                }
            }
        });
    }

    /**
     * Controller implementation.
     * 
     * @author Christian Weiss
     */
    private class ControllerImpl extends DefaultController {

        @Override
        public void widgetSelected(final SelectionEvent event, final Control source) {
            if (source == jdbcProfileCombo) {
                final int selectionIndex = jdbcProfileCombo.getSelectionIndex();
                final String selectedProfileLabel = jdbcProfileCombo.getItem(selectionIndex);
                setJdbcProfile(selectedProfileLabel);
                if (!jdbcProfileCombo.getItem(0).equals(Messages.readerSectionJDBCProfileNullItem) && selectionIndex > 0) {
                    jdbcProfileCombo.remove(0);
                }
            } else if (source == createSqlStatementButton) {
                setAutoSqlStatement();
            } else if (source == initVariablesInsertButton) {
                final int selectionIndex = initVariablesCombo.getSelectionIndex();
                final String selectedLabel = initVariablesCombo.getItem(selectionIndex);
                if (selectionIndex >= 0 && selectionIndex < variablesPlaceholders.size()) {
                    final String placeholder = variablesPlaceholders.get(selectedLabel);
                    replace(sqlInitStatementText, placeholder);
                }
            } else if (source == runVariablesInsertButton) {
                final int selectionIndex = runVariablesCombo.getSelectionIndex();
                final String selectedLabel = runVariablesCombo.getItem(selectionIndex);
                if (selectionIndex >= 0 && selectionIndex < variablesPlaceholders.size()) {
                    final String placeholder = variablesPlaceholders.get(selectedLabel);
                    replace(sqlStatementText, placeholder);
                }
            } else if (source == sqlInitStatementCheckbox) {
                refreshSqlInitStatement();
            }
        }

    }

    /**
     * Synchronizer implementation.
     * 
     * @author Christian Weiss
     */
    private final class SynchronizerImpl extends DefaultSynchronizer {

        @Override
        public void handlePropertyChange(final String propertyName, final String newValue, final String oldValue) {
            super.handlePropertyChange(propertyName, newValue, oldValue);
            if (propertyName.equals(SqlComponentConstants.METADATA_JDBC_PROFILE_PROPERTY)) {
                final String newJdbcProfile = (String) newValue;
                for (int index = 0; index < jdbcProfileCombo.getItemCount(); ++index) {
                    final String label = jdbcProfileCombo.getItem(index);
                    if (label.equals(newJdbcProfile) || (newJdbcProfile == null && label.equals(""))) {
                        jdbcProfileCombo.select(index);
                        break;
                    }
                }
            }
        }

        @Override
        protected void handlePropertyChange(final Control control, final String key, final String newValue,
            final String oldValue) {
            super.handlePropertyChange(control, key, newValue, oldValue);
            if (control == sqlInitStatementCheckbox) {
                refreshSqlInitStatement();
            }
        }
        
//        @Override
//        public void handleChannelEvent(final ChannelEvent event) {
//            super.handleChannelEvent(event);
//            if (event.getNature() == EndpointNature.Input) {
//                refreshSqlStatement();
//            }
//        }

    }

}
