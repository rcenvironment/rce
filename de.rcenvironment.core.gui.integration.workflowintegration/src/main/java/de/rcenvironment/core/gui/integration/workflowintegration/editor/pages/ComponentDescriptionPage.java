/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.integration.workflowintegration.editor.pages;

import java.util.List;
import java.util.Optional;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import de.rcenvironment.core.component.api.ComponentIdRules;
import de.rcenvironment.core.component.integration.IntegrationConstants;
import de.rcenvironment.core.component.integration.IntegrationContext;
import de.rcenvironment.core.component.integration.ToolIntegrationContextRegistry;
import de.rcenvironment.core.component.integration.workflow.WorkflowConfigurationMap;
import de.rcenvironment.core.component.integration.workflow.WorkflowIntegrationConstants;
import de.rcenvironment.core.gui.integration.common.CommonIntegrationGUIConstants;
import de.rcenvironment.core.gui.integration.common.ComponentDescriptionValidator;
import de.rcenvironment.core.gui.integration.common.IntegrationHelper;
import de.rcenvironment.core.gui.integration.common.PathChooserButtonListener;
import de.rcenvironment.core.gui.integration.common.editor.IntegrationEditorPage;
import de.rcenvironment.core.gui.integration.toolintegration.GroupPathChooserButtonListener;
import de.rcenvironment.core.gui.integration.workflowintegration.WorkflowIntegrationController.ConfigurationContext;
import de.rcenvironment.core.gui.integration.workflowintegration.editor.WorkflowIntegrationEditor;
import de.rcenvironment.core.gui.resources.api.ColorManager;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardColors;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.utils.incubator.NumericalTextConstraintListener;
import de.rcenvironment.core.gui.utils.incubator.WidgetGroupFactory;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Editor Page to define the component descriptions of an integrated workflow.
 * 
 * @author Kathrin Schaffert
 * @author Jan Flink
 */
public class ComponentDescriptionPage extends IntegrationEditorPage {

    private static final String DEFAULT_MESSAGE = "Define some information for the workflow component";

    private static final String DOTS = "...";

    private static final int DESCRIPTION_TEXT_HEIGHT_HINT = 50;

    private static final int TEXTFIELD_MIN_WIDTH = 220;

    private static final int SMALL_TEXTFIELD_MIN_WIDTH = 100;

    private static final String HELP_CONTEXT_ID =
        "de.rcenvironment.core.gui.integration.workflowintegration.integration_componentDescription";

    private Color colorWhite = ColorManager.getInstance().getSharedColor(StandardColors.RCE_WHITE);

    private WorkflowIntegrationEditor integrationEditor;

    private Shell shell;

    private IntegrationHelper integrationHelper = new IntegrationHelper();

    private final ServiceRegistryAccess serviceRegistryAccess;

    private ToolIntegrationContextRegistry integrationContextRegistry;

    private IntegrationContext integrationContext;

    private Text wfNameText;

    private Text versionText;

    private Text iconPathText;

    private Text limitExecutionsText;

    private Text groupPathText;

    private Text documentationText;

    private Text descriptionText;

    private Text contactName;

    private Text contactEmail;

    private Button limitExecutionsButton;

    private Button copyIcon;

    private List<String> groupNames;

    private Optional<String> wfNameValidationMessage = Optional.empty();

    private Optional<String> groupValidationMessage = Optional.empty();

    private Optional<String> iconValidationMessage = Optional.empty();

    private Optional<String> documentationValidationMessage = Optional.empty();

    private Optional<String> versionVailidationMessage = Optional.empty();

    private Optional<String> parallelExecutionVailidationMessage = Optional.empty();

    private String originModel;

    public ComponentDescriptionPage(WorkflowIntegrationEditor integrationEditor, CTabFolder container) {
        super(integrationEditor, container, "Workflow Component Description");
        this.integrationEditor = integrationEditor;
        this.shell = container.getShell();

        serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        integrationContextRegistry = serviceRegistryAccess.getService(ToolIntegrationContextRegistry.class);
        this.integrationContext = getCurrentContext();
        this.groupNames = integrationHelper.updateGroupNames(integrationContext.getContextType());
        groupNames.removeIf(item -> item.equals(WorkflowIntegrationConstants.DEFAULT_GROUP_ID));
    }

    @Override
    public void createContent(Composite container) {

        final Group wfCharacteristics = new Group(container, SWT.NONE);
        wfCharacteristics.setText("Workflow Component Characteristics");
        wfCharacteristics.setLayout(new GridLayout(4, false));
        GridData wfCharGridData = new GridData(GridData.FILL_BOTH);
        wfCharacteristics.setLayoutData(wfCharGridData);
        wfCharGridData.verticalIndent = 5;
        wfCharacteristics.setBackground(colorWhite);

        createWfCharacteristics(wfCharacteristics);

        final Group contactInformation = new Group(container, SWT.NONE);
        contactInformation.setText("Contact Information");
        contactInformation.setLayout(new GridLayout(2, false));
        contactInformation.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        contactInformation.setBackground(colorWhite);

        createContactInformation(contactInformation);

        updateDescriptionSettings();
        updateLaunchSettings();
    }

    private void createWfCharacteristics(Group group) {

        wfNameText = createLabelAndTextfield(group, "Name*:", IntegrationConstants.KEY_COMPONENT_NAME);
        wfNameText.addModifyListener(e -> {
            ComponentDescriptionValidator validator = new ComponentDescriptionValidator();
            Optional<String> currentName = integrationEditor.getController().getOriginalName();
            List<String> usedToolnames = integrationHelper.getAlreadyIntegratedComponentNames();
            if (currentName.isPresent()) {
                usedToolnames.remove(currentName.get());
            }
            wfNameValidationMessage = validator.validateName(wfNameText, currentName, usedToolnames);
            updateValidationMessage();
            updateSaveButtonActivation();
            if (!wfNameValidationMessage.isPresent()) {
                integrationEditor.updatePartName();
            }
        });

        createVersionComposite(group);

        groupPathText = createLabelAndTextfield(group, "Group path:", IntegrationConstants.KEY_GROUPNAME);
        groupPathText.addModifyListener(e -> {
            ComponentDescriptionValidator validator = new ComponentDescriptionValidator();
            groupValidationMessage = validator.validateGroupPath(groupPathText);
            updateValidationMessage();
            updateSaveButtonActivation();
        });

        Composite groupComposite = new Composite(group, SWT.NONE);
        groupComposite.setLayout(new GridLayout(2, false));
        groupComposite.setBackground(colorWhite);

        Button chooseGroupButton = new Button(groupComposite, SWT.PUSH);
        chooseGroupButton.setText(DOTS);
        chooseGroupButton.addSelectionListener(new GroupPathChooserButtonListener(groupNames, groupPathText, shell));

        createLimitExecutionComposite(group);

        iconPathText = createLabelAndTextfield(group, "Icon:", IntegrationConstants.KEY_ICON_PATH);
        iconPathText.addModifyListener(e -> {
            ComponentDescriptionValidator validator = new ComponentDescriptionValidator();
            iconValidationMessage = validator.validateIcon(iconPathText, getCurrentContext(),
                (String) integrationEditor.getController().getConfigurationMap().get(IntegrationConstants.KEY_COMPONENT_NAME));
            updateValidationMessage();
            updateSaveButtonActivation();
        });

        Composite iconPathComposite = createChoosePathComposite(group, iconPathText, new String[] { "*.jpg;*.png", "*.jpg", "*.png" });

        copyIcon = new Button(iconPathComposite, SWT.CHECK);
        copyIcon.setData(CommonIntegrationGUIConstants.KEY, IntegrationConstants.KEY_COPY_ICON);
        copyIcon.addSelectionListener(new CheckButtonSelectionListener(copyIcon, integrationEditor,
            ConfigurationContext.COMMON_SETTINGS));

        Label copyIconLabel = new Label(iconPathComposite, SWT.NONE);
        copyIconLabel.setText("Copy icon into configuration folder");
        copyIconLabel.setBackground(colorWhite);

        documentationText = createLabelAndTextfield(group, "Documentation:", IntegrationConstants.KEY_DOC_FILE_PATH);
        documentationText.addModifyListener(e -> {
            ComponentDescriptionValidator validator = new ComponentDescriptionValidator();
            documentationValidationMessage = validator.validateDoc(documentationText, getCurrentContext(),
                (String) integrationEditor.getController().getConfigurationMap().get(IntegrationConstants.KEY_COMPONENT_NAME));
            updateValidationMessage();
            updateSaveButtonActivation();
        });

        createChoosePathComposite(group, documentationText, new String[] { "*.txt;*.pdf", "*.txt", "*.pdf" });

// This functionality will be added in a future release. 
// K.Schaffert, 27.07.2022
//        addImageFile = new Button(documentationComposite, SWT.CHECK);
//        addImageFile.setText("Automatically generate workflow image file");
//        addImageFile.setBackground(colorWhite);
//        addImageFile.setData(CommonIntegrationGUIConstants.KEY, WorkflowIntegrationConstants.KEY_WORKFLOW_IMAGE_FILE);
//        addImageFile.addSelectionListener(new CheckButtonSelectionListener(addImageFile,
//            integrationEditor.getController(), ConfigurationContext.COMMON_SETTINGS));

        final Label descriptionLabel = new Label(group, SWT.None);
        descriptionLabel.setText("Description:");
        GridData descriptionLabelData = new GridData();
        descriptionLabelData.verticalAlignment = GridData.BEGINNING;
        descriptionLabelData.horizontalAlignment = GridData.END;
        descriptionLabel.setLayoutData(descriptionLabelData);
        descriptionLabel.setBackground(colorWhite);

        descriptionText = new Text(group, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
        GridData descriptionLayoutData = new GridData(GridData.FILL_BOTH);
        descriptionLayoutData.horizontalSpan = 3;
        descriptionLayoutData.heightHint = DESCRIPTION_TEXT_HEIGHT_HINT;
        descriptionText.setLayoutData(descriptionLayoutData);
        descriptionText.setData(CommonIntegrationGUIConstants.KEY, IntegrationConstants.KEY_DESCRIPTION);
        descriptionText.addModifyListener(new TextModifyListener(descriptionText, integrationEditor,
            ConfigurationContext.COMMON_SETTINGS));
    }

    private void createLimitExecutionComposite(Group group) {
        GridData compositeGridData = new GridData();
        compositeGridData.horizontalAlignment = GridData.END;
        compositeGridData.grabExcessHorizontalSpace = true;

        Composite limitExecutionsComposite = new Composite(group, SWT.NONE);
        GridLayout limitExecutionGridLayout = new GridLayout(3, false);
        limitExecutionsComposite.setLayout(limitExecutionGridLayout);
        limitExecutionsComposite.setLayoutData(compositeGridData);
        limitExecutionsComposite.setBackground(colorWhite);

        limitExecutionsButton = new Button(limitExecutionsComposite, SWT.CHECK | SWT.RIGHT);
        limitExecutionsButton.addSelectionListener(new LimitExecutionButtonSelectionListener());
        limitExecutionsButton.setData(CommonIntegrationGUIConstants.KEY, IntegrationConstants.KEY_LIMIT_INSTANCES);
        limitExecutionsButton.addSelectionListener(new CheckButtonSelectionListener(limitExecutionsButton,
            integrationEditor, ConfigurationContext.LAUNCH_SETTINGS));
        limitExecutionsButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                validateParallelExecution();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });

        Label limitExecutionLabel = new Label(limitExecutionsComposite, SWT.NONE);
        limitExecutionLabel.setText("Limit parallel executions:");
        limitExecutionLabel.setBackground(colorWhite);

        limitExecutionsText = new Text(limitExecutionsComposite, SWT.BORDER);
        GridData limitExecutionGridData = new GridData();
        limitExecutionGridData.widthHint = SMALL_TEXTFIELD_MIN_WIDTH;
        limitExecutionGridData.horizontalAlignment = GridData.END;
        limitExecutionGridData.grabExcessHorizontalSpace = true;
        limitExecutionsText.setLayoutData(limitExecutionGridData);
        limitExecutionsText.setEnabled(false);
        limitExecutionsText.setData(CommonIntegrationGUIConstants.KEY, IntegrationConstants.KEY_LIMIT_INSTANCES_COUNT);
        limitExecutionsText.addModifyListener(new TextModifyListener(limitExecutionsText,
            integrationEditor, ConfigurationContext.LAUNCH_SETTINGS));
        limitExecutionsText
            .addVerifyListener(new NumericalTextConstraintListener(WidgetGroupFactory.ONLY_INTEGER | WidgetGroupFactory.GREATER_ZERO));
        limitExecutionsText.addModifyListener(e -> validateParallelExecution());
    }

    private void createVersionComposite(Group group) {
        GridData compositeGridData = new GridData();
        compositeGridData.horizontalAlignment = GridData.END;
        compositeGridData.grabExcessHorizontalSpace = true;
        compositeGridData.horizontalSpan = 2;

        Composite versionComposite = new Composite(group, SWT.NONE);
        GridLayout versionGridLayout = new GridLayout(2, false);
        versionComposite.setLayout(versionGridLayout);
        versionComposite.setLayoutData(compositeGridData);
        versionComposite.setBackground(colorWhite);

        final Label versionLabel = new Label(versionComposite, SWT.NONE);
        versionLabel.setText("Version*:");
        versionLabel.setBackground(colorWhite);
        GridData versionLabelGridData = new GridData();
        versionLabelGridData.horizontalAlignment = GridData.END;
        versionLabelGridData.grabExcessHorizontalSpace = true;
        versionLabel.setLayoutData(versionLabelGridData);

        versionText = new Text(versionComposite, SWT.BORDER);
        GridData versionGridData = new GridData(GridData.FILL_HORIZONTAL);
        versionGridData.widthHint = SMALL_TEXTFIELD_MIN_WIDTH;
        versionGridData.grabExcessHorizontalSpace = false;
        versionText.setLayoutData(versionGridData);
        versionText.setData(CommonIntegrationGUIConstants.KEY, IntegrationConstants.KEY_VERSION);
        versionText.addModifyListener(new TextModifyListener(versionText, integrationEditor,
            ConfigurationContext.LAUNCH_SETTINGS));
        versionText.addModifyListener(e -> {
            Optional<String> validationResult = ComponentIdRules.validateComponentVersionRules(versionText.getText());
            if (validationResult.isPresent()) {
                versionVailidationMessage = Optional
                    .of(StringUtils.format("The chosen version is not valid.\n %s", validationResult.get()));
            } else {
                versionVailidationMessage = Optional.empty();
            }
            updateValidationMessage();
            updateSaveButtonActivation();
        });
    }

    private Composite createChoosePathComposite(Group group, Text text, String[] filterNames) {
        Composite iconPathComposite = new Composite(group, SWT.NONE);
        iconPathComposite.setLayout(new GridLayout(3, false));
        iconPathComposite.setBackground(colorWhite);
        GridData iconGridData = new GridData();
        iconGridData.horizontalSpan = 2;
        iconPathComposite.setLayoutData(iconGridData);

        Button choosePathButton = new Button(iconPathComposite, SWT.PUSH);
        choosePathButton.setText(DOTS);
        choosePathButton.addSelectionListener(new PathChooserButtonListener(text, false, filterNames, shell));
        return iconPathComposite;
    }

    private void createContactInformation(Group group) {
        contactName = createLabelAndTextfield(group, "Name:", IntegrationConstants.KEY_INTEGRATOR_NAME);
        contactEmail = createLabelAndTextfield(group, "E-Mail:", IntegrationConstants.KEY_INTEGRATOR_EMAIL);
    }

    private Text createLabelAndTextfield(Composite composite, String labelText, String key) {
        final Label label = new Label(composite, SWT.None);
        label.setText(labelText);
        label.setBackground(colorWhite);
        GridData labelGridData = new GridData();
        labelGridData.horizontalAlignment = GridData.END;
        label.setLayoutData(labelGridData);

        final Text text = new Text(composite, SWT.BORDER);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.widthHint = TEXTFIELD_MIN_WIDTH;
        text.setLayoutData(gridData);
        text.setData(CommonIntegrationGUIConstants.KEY, key);
        text.addModifyListener(
            new TextModifyListener(text, integrationEditor, ConfigurationContext.COMMON_SETTINGS));

        return text;
    }

    private void updateDescriptionSettings() {
        WorkflowConfigurationMap model = integrationEditor.getController().getWorkflowConfigurationModel();
        originModel = model.getRawConfigurationMap().toString();
        wfNameText.setText(model.getToolName());
        iconPathText.setText(model.getIconPath());
        groupPathText.setText(model.getGroupPath());
        documentationText.setText(model.getDocFilePath());
        descriptionText.setText(model.getToolDescription());
        contactName.setText(model.getIntegratorName());
        contactEmail.setText(model.getIntegratorEmail());
        copyIcon.setSelection(model.isCopyIcon());
// This functionality will be added in a future release. 
// K.Schaffert, 27.07
//        addImageFile.setSelection(model.isAddImageFile());
    }

    private void updateLaunchSettings() {
        WorkflowConfigurationMap model = integrationEditor.getController().getWorkflowConfigurationModel();
        versionText.setText(model.getToolVersion());
        boolean checked = model.isLimitInstance();
        limitExecutionsButton.setSelection(checked);
        limitExecutionsText.setEnabled(checked);
        limitExecutionsText.setText(model.getMaxParallelCount());
    }

    private class LimitExecutionButtonSelectionListener implements SelectionListener {

        @Override
        public void widgetSelected(SelectionEvent evt) {
            if (evt.getSource() instanceof Button) {
                limitExecutionsText.setEnabled(((Button) evt.getSource()).getSelection());
            }
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            widgetSelected(arg0);
        }
    }

    private void updateValidationMessage() {
        this.setPageValid(false);
        if (wfNameValidationMessage.isPresent()) {
            setMessage(wfNameValidationMessage.get(), ImageManager.getInstance().getSharedImage(StandardImages.FAILED));
        } else if (versionVailidationMessage.isPresent()) {
            setMessage(versionVailidationMessage.get(),
                ImageManager.getInstance().getSharedImage(StandardImages.FAILED));
        } else if (groupValidationMessage.isPresent()) {
            setMessage(groupValidationMessage.get(), ImageManager.getInstance().getSharedImage(StandardImages.FAILED));
        } else if (documentationValidationMessage.isPresent()) {
            setMessage(documentationValidationMessage.get(),
                ImageManager.getInstance().getSharedImage(StandardImages.FAILED));
        } else if (parallelExecutionVailidationMessage.isPresent()) {
            setMessage(parallelExecutionVailidationMessage.get(),
                ImageManager.getInstance().getSharedImage(StandardImages.FAILED));
        } else if (iconValidationMessage.isPresent()) {
            if (iconValidationMessage.get().equals(ComponentDescriptionValidator.ICON_INVALID)) {
                setMessage(iconValidationMessage.get(), ImageManager.getInstance().getSharedImage(StandardImages.WARNING_16));
                this.setPageValid(true);
            } else {
                setMessage(iconValidationMessage.get(), ImageManager.getInstance().getSharedImage(StandardImages.FAILED));
            }
        } else {
            setMessage(DEFAULT_MESSAGE);
            this.setPageValid(true);
        }
    }

    private void validateParallelExecution() {
        ComponentDescriptionValidator validator = new ComponentDescriptionValidator();
        parallelExecutionVailidationMessage = validator.validateParallelExecution(limitExecutionsText, limitExecutionsButton);
        updateValidationMessage();
        updateSaveButtonActivation();
    }

    public IntegrationContext getCurrentContext() {
        if (integrationContext == null) {
            return integrationContextRegistry.getToolIntegrationContextByType("workflow");
        }
        return integrationContext;
    }

    @Override
    public void update() {
        PlatformUI.getWorkbench().getHelpSystem().setHelp(this.getControl(), HELP_CONTEXT_ID);

    }

    @Override
    public boolean hasChanges() {
        return !integrationEditor.getController().getConfigurationMap().toString().equals(originModel);
    }

}

/**
 * Listener for writing the text configuration into the configurationMap when entered.
 * 
 * @author Kathrin Schaffert
 */
final class TextModifyListener implements ModifyListener {

    private final Text text;

    private final WorkflowIntegrationEditor workflowIntegrationEditor;

    private ConfigurationContext context;

    protected TextModifyListener(Text text, WorkflowIntegrationEditor workflowIntegrationEditor,
        ConfigurationContext context) {
        this.workflowIntegrationEditor = workflowIntegrationEditor;
        this.text = text;
        this.context = context;
    }

    @Override
    public void modifyText(ModifyEvent event) {
        workflowIntegrationEditor.getController().setValue((String) text.getData(CommonIntegrationGUIConstants.KEY), text.getText(),
            context);
        workflowIntegrationEditor.updateDirty();
    }
}

/**
 * Listener for writing the button configuration into the configurationMap when checked.
 * 
 * @author Kathrin Schaffert
 */
final class CheckButtonSelectionListener implements SelectionListener {

    private final Button button;

    private final WorkflowIntegrationEditor workflowIntegrationEditor;

    private ConfigurationContext context;

    protected CheckButtonSelectionListener(Button button, WorkflowIntegrationEditor workflowIntegrationEditor,
        ConfigurationContext context) {
        super();
        this.button = button;
        this.workflowIntegrationEditor = workflowIntegrationEditor;
        this.context = context;
    }

    @Override
    public void widgetSelected(SelectionEvent arg0) {
        if (context.equals(ConfigurationContext.COMMON_SETTINGS)) {
            workflowIntegrationEditor.getController().setValue((String) button.getData(CommonIntegrationGUIConstants.KEY),
                button.getSelection(), context);
        } else if (context.equals(ConfigurationContext.LAUNCH_SETTINGS)) {
            workflowIntegrationEditor.getController().setValue((String) button.getData(CommonIntegrationGUIConstants.KEY),
                Boolean.toString(button.getSelection()), context);
        }
        workflowIntegrationEditor.updateDirty();
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent arg0) {
        widgetSelected(arg0);
    }

}
