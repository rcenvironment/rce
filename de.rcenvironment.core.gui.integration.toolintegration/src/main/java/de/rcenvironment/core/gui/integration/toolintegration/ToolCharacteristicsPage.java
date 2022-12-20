/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.integration.toolintegration;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.IWorkbenchHelpSystem;

import de.rcenvironment.core.component.integration.IntegrationConstants;
import de.rcenvironment.core.component.integration.IntegrationContext;
import de.rcenvironment.core.gui.integration.common.CommonIntegrationGUIConstants;
import de.rcenvironment.core.gui.integration.common.ComponentDescriptionValidator;
import de.rcenvironment.core.gui.integration.common.PathChooserButtonListener;
import de.rcenvironment.core.gui.integration.toolintegration.api.ToolIntegrationWizardPage;

/**
 * @author Sascha Zur
 * @author Brigitte Boden
 * @author Kathrin Schaffert
 */
public class ToolCharacteristicsPage extends ToolIntegrationWizardPage {

    private static final String HELP_CONTEXT_ID = "de.rcenvironment.core.gui.wizard.toolintegration.integration_characteristics";

    private static final String DOTS = "  ...  ";

    private static final int TOOL_DESCRIPTION_TEXT_HEIGHT = 50;

    protected Map<String, Object> configurationMap;

    private Text toolNameText;

    private Text iconText;

    private final List<String> usedToolnames;

    private Text groupPathText;

    private Text descriptionTextArea;

    private Text integratorName;

    private Text integratorEmail;

    private Button uploadIconToFolder;

    private List<String> groupNames;

    private Text documenationText;

    private Optional<String> docValidationMessage = Optional.empty();

    private Optional<String> iconValidationMessage = Optional.empty();

    private Optional<String> nameValidationMessage = Optional.empty();

    private Optional<String> groupValidationMessage = Optional.empty();

    private PathChooserButtonListener docPathChooserButtonListener;

    private Optional<String> nameOrigin = Optional.empty();

    protected ToolCharacteristicsPage(String pageName, Map<String, Object> configurationMap, List<String> usedToolnames,
        List<String> groupNames) {
        super(pageName);
        setTitle(pageName);
        setDescription(Messages.firstPageDescription);
        this.configurationMap = configurationMap;
        this.usedToolnames = usedToolnames;
        this.groupNames = groupNames;
        configurationMap.putIfAbsent(IntegrationConstants.KEY_COPY_ICON, true);
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        container.setLayout(layout);
        GridData containerData =
            new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
        container.setLayoutData(containerData);

        Composite toolDataComp = new Composite(container, SWT.NONE);
        toolDataComp.setLayout(new GridLayout(1, false));
        GridData toolData = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
        toolDataComp.setLayoutData(toolData);
        Group toolPropertiesGroup = new Group(toolDataComp, SWT.NONE);
        toolPropertiesGroup.setText(Messages.toolPropGroup);
        toolPropertiesGroup.setLayout(new GridLayout(3, false));
        GridData toolPropertyData =
            new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
        toolPropertiesGroup.setLayoutData(toolPropertyData);
        toolNameText =
            addLabelAndTextfieldForPropertyToComposite(toolPropertiesGroup, Messages.nameRequired,
                IntegrationConstants.KEY_COMPONENT_NAME);
        ((GridData) toolNameText.getLayoutData()).horizontalSpan = 2;
        toolNameText.addModifyListener(e -> {
            ComponentDescriptionValidator validator = new ComponentDescriptionValidator();
            nameValidationMessage = validator.validateName(toolNameText, nameOrigin, usedToolnames);
            validate(true);
        });
        iconText =
            addLabelAndTextfieldForPropertyToComposite(toolPropertiesGroup, Messages.iconPath, IntegrationConstants.KEY_ICON_PATH);
        iconText.setMessage(Messages.iconSizeMessage);
        iconText.addModifyListener(e -> {
            ComponentDescriptionValidator validator = new ComponentDescriptionValidator();
            iconValidationMessage = validator.validateIcon(iconText, ((ToolIntegrationWizard) getWizard()).getCurrentContext(),
                (String) configurationMap.get(IntegrationConstants.KEY_COMPONENT_NAME));
            validate(true);
        });
        GridLayout iconCompLayout = new GridLayout(2, false);
        iconCompLayout.marginWidth = 0;
        Composite iconComp = new Composite(toolPropertiesGroup, SWT.NONE);
        iconComp.setLayout(iconCompLayout);

        Button choosePathButton = new Button(iconComp, SWT.PUSH);
        choosePathButton.setText(DOTS);
        choosePathButton
            .addSelectionListener(new PathChooserButtonListener(iconText, false, new String[] { "*.jpg;*.png", "*.jpg", "*.png" }, getShell()));
        uploadIconToFolder = new Button(iconComp, SWT.CHECK);
        uploadIconToFolder.setText(Messages.copyIconButtonLabel);
        uploadIconToFolder.setSelection(true);
        uploadIconToFolder.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                configurationMap.put(IntegrationConstants.KEY_COPY_ICON, uploadIconToFolder.getSelection());
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);

            }
        });
        groupPathText =
            addLabelAndTextfieldForPropertyToComposite(toolPropertiesGroup, Messages.groupPathText,
                IntegrationConstants.KEY_GROUPNAME);
        GridData groupNameTextData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        groupPathText.setLayoutData(groupNameTextData);
        groupPathText.addModifyListener(e -> {
            ComponentDescriptionValidator validator = new ComponentDescriptionValidator();
            groupValidationMessage = validator.validateGroupPath(groupPathText);
            validate(true);
        });
        Button chooseGroupButton = new Button(toolPropertiesGroup, SWT.PUSH);
        chooseGroupButton.setText(DOTS);
        chooseGroupButton.addSelectionListener(new GroupPathChooserButtonListener(groupNames, groupPathText, getShell()));

        documenationText =
            addLabelAndTextfieldForPropertyToComposite(toolPropertiesGroup, "Documentation: ", IntegrationConstants.KEY_DOC_FILE_PATH);
        Button chooseDocButton = new Button(toolPropertiesGroup, SWT.PUSH);
        chooseDocButton.setText(DOTS);

        docPathChooserButtonListener =
            new PathChooserButtonListener(documenationText, false, new String[] { "*.txt;*.pdf", "*.txt", "*.pdf"}, getShell());
        chooseDocButton.addSelectionListener(docPathChooserButtonListener);
        documenationText.addModifyListener(e -> {
            ComponentDescriptionValidator validator = new ComponentDescriptionValidator();
            docValidationMessage = validator.validateDoc(documenationText, ((ToolIntegrationWizard) getWizard()).getCurrentContext(),
                (String) configurationMap.get(IntegrationConstants.KEY_COMPONENT_NAME));
            validate(true);
        });
        Label toolDescriptionLabel = new Label(toolPropertiesGroup, SWT.NONE);
        toolDescriptionLabel.setText(Messages.toolDescription);
        GridData toolDescriptionLabelData = new GridData();
        toolDescriptionLabelData.verticalAlignment = GridData.BEGINNING;
        toolDescriptionLabel.setLayoutData(toolDescriptionLabelData);
        descriptionTextArea = new Text(toolPropertiesGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
        descriptionTextArea.setData(CommonIntegrationGUIConstants.KEY, IntegrationConstants.KEY_DESCRIPTION);
        descriptionTextArea.addModifyListener(
            ignored -> configurationMap.put((String) descriptionTextArea.getData(CommonIntegrationGUIConstants.KEY),
                descriptionTextArea.getText()));
        descriptionTextArea.addTraverseListener(new DescriptionTraverseListener());

        GridData descriptionData =
            new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_BOTH | GridData.GRAB_VERTICAL);
        descriptionData.heightHint = TOOL_DESCRIPTION_TEXT_HEIGHT;
        descriptionData.horizontalSpan = 2;
        descriptionTextArea.setLayoutData(descriptionData);

        Group userInformationGroup = new Group(toolDataComp, SWT.NONE);
        userInformationGroup.setText(Messages.userInformationGroup);
        userInformationGroup.setLayout(new GridLayout(2, false));
        GridData userInfoData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        userInformationGroup.setLayoutData(userInfoData);

        integratorName = addLabelAndTextfieldForPropertyToComposite(userInformationGroup, Messages.nameIntegrator,
            IntegrationConstants.KEY_INTEGRATOR_NAME);
        integratorEmail = addLabelAndTextfieldForPropertyToComposite(userInformationGroup, Messages.email,
            IntegrationConstants.KEY_INTEGRATOR_EMAIL);
        setControl(container);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(this.getControl(),
            HELP_CONTEXT_ID);
        validate(false);

    }

    private Text addLabelAndTextfieldForPropertyToComposite(Composite composite,
        String propertyMessage, String key) {
        Label propertyLabel = new Label(composite, SWT.NONE);
        propertyLabel.setText(propertyMessage);
        Point prefSize = propertyLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        GridData labelData = new GridData();
        labelData.widthHint = prefSize.x;
        propertyLabel.setLayoutData(labelData);
        final Text propertyText = new Text(composite, SWT.BORDER);
        GridData gridDataText = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        propertyText.setLayoutData(gridDataText);
        propertyText.setData(CommonIntegrationGUIConstants.KEY, key);
        propertyText.addModifyListener(
            ignored -> configurationMap.put((String) propertyText.getData(CommonIntegrationGUIConstants.KEY), propertyText.getText()));
        return propertyText;
    }

    /**
     * Listener allowing to leave the description field by pressing TAB (instead of inserting a tab into the text).
     *
     * @author bode_br
     */

    private class DescriptionTraverseListener implements TraverseListener {

        @Override
        public void keyTraversed(TraverseEvent e) {
            if (e.detail == SWT.TRAVERSE_TAB_NEXT) {
                e.doit = true;
            }
        }
    }

    private void validate(boolean update) {
        setMessage(null, IMessageProvider.NONE);
        setPageComplete(true);
        String name = (String) configurationMap.get(IntegrationConstants.KEY_COMPONENT_NAME);
        if (name == null
            || name.trim().isEmpty()) {
            setMessage(Messages.toolFilenameInvalid, IMessageProvider.ERROR);
            setPageComplete(false);
        } else if ((!update) && usedToolnames.contains((configurationMap.get(IntegrationConstants.KEY_COMPONENT_NAME)))) {
            setMessage(Messages.toolFilenameUsed, IMessageProvider.ERROR);
            setPageComplete(false);
        }
        if (docValidationMessage.isPresent()) {
            setMessage(docValidationMessage.get(), IMessageProvider.ERROR);
            setPageComplete(false);
        }
        if (groupValidationMessage.isPresent()) {
            setMessage(groupValidationMessage.get(), IMessageProvider.ERROR);
            setPageComplete(false);
        }
        if (iconValidationMessage.isPresent()) {

            if (iconValidationMessage.get().equals(ComponentDescriptionValidator.ICON_INVALID)) {
                setMessage(iconValidationMessage.get(), IMessageProvider.WARNING);
                setPageComplete(true);
            } else {
                setMessage(iconValidationMessage.get(), IMessageProvider.ERROR);
                setPageComplete(false);
            }
        }
        if (nameValidationMessage.isPresent()) {
            setMessage(nameValidationMessage.get(), IMessageProvider.ERROR);
            setPageComplete(false);
        }

    }

    /**
     * Sets a new configurationMap and updates all fields.
     * 
     * @param newConfigurationMap new map
     */
    @Override
    public void setConfigMap(Map<String, Object> newConfigurationMap) {
        configurationMap = newConfigurationMap;
        if (configurationMap.get(IntegrationConstants.KEY_COMPONENT_NAME) != null) {
            nameOrigin = Optional.of(String.valueOf(configurationMap.get(IntegrationConstants.KEY_COMPONENT_NAME)));
        }
        configurationMap.putIfAbsent(IntegrationConstants.KEY_COPY_ICON, true);
        updatePageValues();

    }

    private void updatePageValues() {
        if ((String) configurationMap.get(IntegrationConstants.KEY_COMPONENT_NAME) != null) {
            toolNameText.setText((String) configurationMap.get(IntegrationConstants.KEY_COMPONENT_NAME));
        } else {
            toolNameText.setText("");
        }
        if (configurationMap.get(IntegrationConstants.KEY_ICON_PATH) != null) {
            iconText.setText((String) configurationMap.get(IntegrationConstants.KEY_ICON_PATH));
        } else {
            iconText.setText("");
        }
        uploadIconToFolder.setSelection(Boolean.TRUE.equals(configurationMap.get(IntegrationConstants.KEY_COPY_ICON)));
        if (configurationMap.get(IntegrationConstants.KEY_DOC_FILE_PATH) != null) {
            documenationText.setText((String) configurationMap.get(IntegrationConstants.KEY_DOC_FILE_PATH));
            File pathToOpen = new File(documenationText.getText());
            if (!pathToOpen.isAbsolute()) {
                IntegrationContext context = ((ToolIntegrationWizard) getWizard()).getCurrentContext();
                pathToOpen = new File(
                    new File(new File(context.getRootPathToToolIntegrationDirectory(), context.getNameOfToolIntegrationDirectory()),
                        (String) configurationMap.get(IntegrationConstants.KEY_COMPONENT_NAME)),
                    IntegrationConstants.DOCS_DIR_NAME);
            }
            docPathChooserButtonListener.setOpenPath(pathToOpen);
        } else {
            documenationText.setText("");
        }
        if (configurationMap.get(IntegrationConstants.KEY_GROUPNAME) != null) {
            groupPathText.setText((String) configurationMap.get(IntegrationConstants.KEY_GROUPNAME));
        } else {
            groupPathText.setText("");
        }
        if (configurationMap.get(IntegrationConstants.KEY_DESCRIPTION) != null) {
            descriptionTextArea.setText((String) configurationMap.get(IntegrationConstants.KEY_DESCRIPTION));
        } else {
            descriptionTextArea.setText("");
        }
        if (configurationMap.get(IntegrationConstants.KEY_INTEGRATOR_NAME) != null) {
            integratorName.setText((String) configurationMap.get(IntegrationConstants.KEY_INTEGRATOR_NAME));
        } else {
            integratorName.setText("");
        }
        if (configurationMap.get(IntegrationConstants.KEY_INTEGRATOR_EMAIL) != null) {
            integratorEmail.setText((String) configurationMap.get(IntegrationConstants.KEY_INTEGRATOR_EMAIL));
        } else {
            integratorEmail.setText("");
        }
        validate(true);
    }

    @Override
    public void performHelp() {
        super.performHelp();
        IWorkbenchHelpSystem helpSystem = PlatformUI.getWorkbench().getHelpSystem();
        helpSystem.displayHelp(HELP_CONTEXT_ID);
    }

    @Override
    public void updatePage() {
        // TODO Auto-generated method stub
    }

}
