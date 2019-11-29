/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.database.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.components.database.common.DatabaseComponentConstants;
import de.rcenvironment.components.database.common.jdbc.JDBCDriverInformation;
import de.rcenvironment.components.database.common.jdbc.JDBCDriverService;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Database connection sections.
 *
 * @author Oliver Seebach
 * @author Kathrin Schaffert
 */
public class DatabaseConnectionSection extends ValidatingWorkflowNodePropertySection {

    private static JDBCDriverService jdbcDriverService;

    private static final Integer MINIMUM_TEXTFIELD_WIDTH = 150;

    // private final static String DATABASE_FOUND = "Database found.";
    //
    // private final static String DATABASE_NOT_FOUND = "Cannot find database.";
    //
    // private final static String DATABASE_PENDING = "Pending ... ";
    //
    // private CLabel testDatabaseStatusLabel;

    private CCombo databaseConnectorCombo;

    private ServiceRegistryAccess serviceRegistryAccess;

    public DatabaseConnectionSection() {
        serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        jdbcDriverService = serviceRegistryAccess.getService(JDBCDriverService.class);
    }

    @Override
    protected void createCompositeContent(final Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {

        super.createCompositeContent(parent, aTabbedPropertySheetPage);
        TabbedPropertySheetWidgetFactory factory = aTabbedPropertySheetPage.getWidgetFactory();

        final Section sectionDatabase = factory.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        sectionDatabase.setText("Database Connection");
        sectionDatabase.marginWidth = 5;
        sectionDatabase.marginHeight = 5;

        Composite mainComposite = new Composite(sectionDatabase, SWT.NONE);
        mainComposite.setLayout(new GridLayout(1, false));
        mainComposite.setBackground(Display.getCurrent().getSystemColor(1));
        GridData mainData = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH);
        mainComposite.setLayoutData(mainData);

        // 1
        Composite informationLabelComposite = new Composite(mainComposite, SWT.NONE);
        informationLabelComposite.setLayout(new GridLayout(2, false));
        GridData informationLabelData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_BOTH);
        informationLabelData.horizontalSpan = 2;
        informationLabelComposite.setLayoutData(informationLabelData);

        CLabel informationLabel = new CLabel(informationLabelComposite, SWT.LEFT | SWT.SHADOW_NONE);
        informationLabel.setText("Please define the database for this component to use:");

        // 2
        Composite databaseSelectionComposite = new Composite(mainComposite, SWT.NONE);
        databaseSelectionComposite.setLayout(new GridLayout(1, false));
        GridData databaseSelectionData = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_VERTICAL);
        databaseSelectionComposite.setLayoutData(databaseSelectionData);

        Group databaseSelectionGroup = new Group(databaseSelectionComposite, SWT.NONE);
        databaseSelectionGroup.setText("Database");
        databaseSelectionGroup.setLayout(new GridLayout(2, false));
        GridData databaseSelectionGroupData = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_VERTICAL);
        databaseSelectionGroup.setLayoutData(databaseSelectionGroupData);

        // DB NAME
        Label databaseNameLabel = new Label(databaseSelectionGroup, SWT.NONE);
        databaseNameLabel.setText("Database Name: ");
        GridData databaseNameLabelData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        databaseNameLabel.setLayoutData(databaseNameLabelData);

        Text databaseNameText = new Text(databaseSelectionGroup, SWT.BORDER);
        GridData databaseNameTextData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        databaseNameTextData.minimumWidth = MINIMUM_TEXTFIELD_WIDTH;
        databaseNameTextData.widthHint = MINIMUM_TEXTFIELD_WIDTH;
        databaseNameText.setLayoutData(databaseNameTextData);
        databaseNameText.setData(CONTROL_PROPERTY_KEY, DatabaseComponentConstants.DATABASE_NAME);

        // DB CONNECTOR
        Label databaseConnectorLabel = new Label(databaseSelectionGroup, SWT.NONE);
        databaseConnectorLabel.setText("Database Type: ");
        GridData databaseConnectorLabelData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        databaseConnectorLabel.setLayoutData(databaseConnectorLabelData);

        databaseConnectorCombo = new CCombo(databaseSelectionGroup, SWT.READ_ONLY | SWT.BORDER);
        GridData databaseConnectorData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        databaseConnectorData.minimumWidth = MINIMUM_TEXTFIELD_WIDTH;
        databaseConnectorData.widthHint = MINIMUM_TEXTFIELD_WIDTH;
        databaseConnectorCombo.setLayoutData(databaseConnectorData);
        databaseConnectorCombo.setData(CONTROL_PROPERTY_KEY, DatabaseComponentConstants.DATABASE_CONNECTOR);

        for (JDBCDriverInformation jdbcDriver : jdbcDriverService.getRegisteredJDBCDrivers()) {
            databaseConnectorCombo.add(jdbcDriver.getDisplayName());
        }
        if (databaseConnectorCombo.getItemCount() > 0) {
            databaseConnectorCombo.select(0);
        }

        // DB HOST
        Label databaseHostLabel = new Label(databaseSelectionGroup, SWT.NONE);
        databaseHostLabel.setText("Database Host: ");
        GridData databaseHostLabelData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        databaseHostLabel.setLayoutData(databaseHostLabelData);

        Text databaseHostText = new Text(databaseSelectionGroup, SWT.BORDER);
        GridData databaseHostTextData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        databaseHostTextData.minimumWidth = MINIMUM_TEXTFIELD_WIDTH;
        databaseHostTextData.widthHint = MINIMUM_TEXTFIELD_WIDTH;
        databaseHostText.setLayoutData(databaseHostTextData);
        databaseHostText.setData(CONTROL_PROPERTY_KEY, DatabaseComponentConstants.DATABASE_HOST);

        // DB PORT
        Label databasePortLabel = new Label(databaseSelectionGroup, SWT.NONE);
        databasePortLabel.setText("Database Port: ");
        GridData databasePortLabelData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        databasePortLabel.setLayoutData(databasePortLabelData);

        Text databasePortText = new Text(databaseSelectionGroup, SWT.BORDER);
        GridData databasePortTextData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        databasePortTextData.minimumWidth = MINIMUM_TEXTFIELD_WIDTH;
        databasePortTextData.widthHint = MINIMUM_TEXTFIELD_WIDTH;
        databasePortText.setLayoutData(databasePortTextData);
        databasePortText.setData(CONTROL_PROPERTY_KEY, DatabaseComponentConstants.DATABASE_PORT);

        // DB SCHEME
        Label databaseSchemeLabel = new Label(databaseSelectionGroup, SWT.NONE);
        databaseSchemeLabel.setText("Default Scheme: ");
        GridData databaseSchemeLabelData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        databaseSchemeLabel.setLayoutData(databaseSchemeLabelData);

        Text databaseSchemeText = new Text(databaseSelectionGroup, SWT.BORDER);
        GridData databaseSchemeTextData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        databaseSchemeTextData.minimumWidth = MINIMUM_TEXTFIELD_WIDTH;
        databaseSchemeTextData.widthHint = MINIMUM_TEXTFIELD_WIDTH;
        databaseSchemeText.setLayoutData(databaseSchemeTextData);
        databaseSchemeText.setData(CONTROL_PROPERTY_KEY, DatabaseComponentConstants.DATABASE_SCHEME);

        // TEST DB
        // Label testDBPlaceholderLabel = new Label(databaseSelectionGroup, SWT.NONE);
        // testDBPlaceholderLabel.setText("");
        // GridData testDBPlaceholderLabelData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        // testDBPlaceholderLabel.setLayoutData(testDBPlaceholderLabelData);
        //
        // Button testDatabaseButton = new Button(databaseSelectionGroup, SWT.PUSH);
        // testDatabaseButton.setText("Test Connection");
        // testDatabaseButton.addSelectionListener(new TestDatabaseButtonListener());
        // GridData testDatabaseButtonData = new GridData(GridData.HORIZONTAL_ALIGN_END);
        // testDatabaseButton.setLayoutData(testDatabaseButtonData);

        // INFORMATION LABEL
        CLabel databaseSchemeInformationLabel = new CLabel(mainComposite, SWT.NONE);
        databaseSchemeInformationLabel
            .setText("Please note: 'Default Scheme' is the scheme you would define using the 'USE <scheme_name>' command.");
        databaseSchemeInformationLabel.setImage(ImageManager.getInstance().getSharedImage(StandardImages.INFORMATION_16));
        GridData databaseSchemeInformationLabelData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        // databaseSchemeInformationLabelData.horizontalSpan = 2;
        databaseSchemeInformationLabel.setLayoutData(databaseSchemeInformationLabelData);

        sectionDatabase.setClient(mainComposite);
    }

    // Currently not required -- seeb_ol, November 2015
    // private final class TestDatabaseButtonListener implements SelectionListener {
    //
    // @Override
    // public void widgetSelected(SelectionEvent event) {
    //
    // testDatabaseStatusLabel.setText(DATABASE_PENDING);
    //
    // DatabaseConnection databaseConnectionToBeTested = new DatabaseConnection();
    //
    // // do test procedure here!
    // boolean connectionEstablished = false;
    //
    // if (connectionEstablished) {
    // testDatabaseStatusLabel.setText(DATABASE_FOUND);
    // } else {
    // testDatabaseStatusLabel.setText(DATABASE_NOT_FOUND);
    // }
    //
    // }
    //
    // @Override
    // public void widgetDefaultSelected(SelectionEvent event) {
    // widgetSelected(event);
    // }
    // }
    //
    // private final class AddDatabaseButtonListener implements SelectionListener {
    //
    // @Override
    // public void widgetSelected(SelectionEvent event) {
    //
    // DatabaseConnectionAddEditDialog databaseEditDialog =
    // new DatabaseConnectionAddEditDialog(Display.getCurrent().getActiveShell(), DatabaseManagementActionType.ADD);
    // databaseEditDialog.open();
    //
    // }
    //
    // @Override
    // public void widgetDefaultSelected(SelectionEvent event) {
    // widgetSelected(event);
    // }
    // }
    //
    // private final class EditDatabaseButtonListener implements SelectionListener {
    //
    // @Override
    // public void widgetSelected(SelectionEvent event) {
    //
    // DatabaseConnection dbConnection = new DatabaseConnection();
    // DatabaseConnectionAddEditDialog databaseEditDialog =
    // new DatabaseConnectionAddEditDialog(Display.getCurrent().getActiveShell(),
    // dbConnection, DatabaseManagementActionType.EDIT);
    // databaseEditDialog.open();
    // }
    //
    // @Override
    // public void widgetDefaultSelected(SelectionEvent event) {
    // widgetSelected(event);
    // }
    // }
    //
    // private final class ManageDatabaseButtonListener implements SelectionListener {
    //
    // @Override
    // public void widgetSelected(SelectionEvent event) {
    //
    // DatabaseManagementDialog managementDialog = new DatabaseManagementDialog(Display.getCurrent().getActiveShell());
    // managementDialog.open();
    //
    // }
    //
    // @Override
    // public void widgetDefaultSelected(SelectionEvent event) {
    // widgetSelected(event);
    // }
    // }

    @Override
    public void refreshSection() {
        super.refreshSection();
        String configString =
            getConfiguration().getConfigurationDescription().getConfigurationValue(DatabaseComponentConstants.DATABASE_HOST);
        // when db component is instantiated for the first time the configuration values have to be set with a default string ""
        // this is necessary for the correct functionality of undo mechanism
        // Kathrin Schaffert, Feb 2019
        if (configString == null) {
            getConfiguration().getConfigurationDescription().setConfigurationValue(DatabaseComponentConstants.DATABASE_NAME, "");
            getConfiguration().getConfigurationDescription().setConfigurationValue(DatabaseComponentConstants.DATABASE_HOST, "");
            getConfiguration().getConfigurationDescription().setConfigurationValue(DatabaseComponentConstants.DATABASE_PORT, "");
            getConfiguration().getConfigurationDescription().setConfigurationValue(DatabaseComponentConstants.DATABASE_SCHEME, "");
        }
    }

}
