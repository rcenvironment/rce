/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.help.IWorkbenchHelpSystem;

import de.rcenvironment.core.component.api.ComponentGroupPathRules;
import de.rcenvironment.core.component.api.ComponentIdRules;
import de.rcenvironment.core.component.integration.ToolIntegrationContext;
import de.rcenvironment.core.component.model.impl.ToolIntegrationConstants;
import de.rcenvironment.core.gui.wizards.toolintegration.api.ToolIntegrationWizardPage;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * @author Sascha Zur
 * @author Brigitte Boden
 * @author Kathrin Schaffert
 */
public class ToolCharacteristicsPage extends ToolIntegrationWizardPage {

    private static final String STRING_TOOLNAME_INVALID = "The chosen tool name is not valid.\n %s.";

    private static final String STRING_GROUPNAME_INVALID = "The chosen group name is not valid.\n %s.";

    private static final String STRING_TOOL_NAME_EXISTS =
        "A tool with the name '%s' is already configured within the current RCE profile.\n Note that tool names are not case sensitive.";

    private static final String HELP_CONTEXT_ID = "de.rcenvironment.core.gui.wizard.toolintegration.integration_characteristics";

    private static final String DOTS = "  ...  ";

    private static final String DOC_EXTENTION_NOT_VALID = "Documentation extension not valid. Valid extensions: ";

    private static final String VALID_EXTENSION_SEPERATOR = ", ";

    private static final String DOC_DOES_NOT_EXIST = "Documentation path is invalid.";

    private static final int TOOL_DESCRIPTION_TEXT_HEIGHT = 50;

    private static final String KEY_KEYS = "properties";

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

    private String docValid = "";

    private String iconValid = "";

    private String nameValid = "";

    private String groupValid = "";

    private PathChooserButtonListener docPathChooserButtonListener;

    private String nameOrigin = null;

    protected ToolCharacteristicsPage(String pageName, Map<String, Object> configurationMap, List<String> usedToolnames,
        List<String> groupNames) {
        super(pageName);
        setTitle(pageName);
        setDescription(Messages.firstPageDescription);
        this.configurationMap = configurationMap;
        this.usedToolnames = usedToolnames;
        this.groupNames = groupNames;
        if (configurationMap.get(ToolIntegrationConstants.KEY_UPLOAD_ICON) == null) {
            configurationMap.put(ToolIntegrationConstants.KEY_UPLOAD_ICON, true);
        }
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
                ToolIntegrationConstants.KEY_TOOL_NAME);
        ((GridData) toolNameText.getLayoutData()).horizontalSpan = 2;
        toolNameText.addModifyListener(e -> {
            nameValid = validateName();
            validate(true);
        });
        iconText =
            addLabelAndTextfieldForPropertyToComposite(toolPropertiesGroup, Messages.iconPath, ToolIntegrationConstants.KEY_TOOL_ICON_PATH);
        iconText.setMessage(Messages.iconSizeMessage);
        iconText.addModifyListener(e -> {
            iconValid = validateIcon();
            validate(true);
        });
        GridLayout iconCompLayout = new GridLayout(2, false);
        iconCompLayout.marginWidth = 0;
        Composite iconComp = new Composite(toolPropertiesGroup, SWT.NONE);
        iconComp.setLayout(iconCompLayout);

        Button choosePathButton = new Button(iconComp, SWT.PUSH);
        choosePathButton.setText(DOTS);
        choosePathButton.addSelectionListener(new PathChooserButtonListener(iconText, false, getShell()));
        uploadIconToFolder = new Button(iconComp, SWT.CHECK);
        uploadIconToFolder.setText(Messages.copyIconButtonLabel);
        uploadIconToFolder.setSelection(true);
        uploadIconToFolder.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                configurationMap.put(ToolIntegrationConstants.KEY_UPLOAD_ICON, (uploadIconToFolder.getSelection()));
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);

            }
        });
        groupPathText =
            addLabelAndTextfieldForPropertyToComposite(toolPropertiesGroup, Messages.groupPathText,
                ToolIntegrationConstants.KEY_TOOL_GROUPNAME);
        GridData groupNameTextData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        groupPathText.setLayoutData(groupNameTextData);
        groupPathText.addModifyListener(e -> {
            groupValid = validateGroupPath();
            validate(true);
        });
        Button chooseGroupButton = new Button(toolPropertiesGroup, SWT.PUSH);
        chooseGroupButton.setText(DOTS);
        chooseGroupButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                showGroupSelectionDialog();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });

        documenationText =
            addLabelAndTextfieldForPropertyToComposite(toolPropertiesGroup, "Documentation: ", ToolIntegrationConstants.KEY_DOC_FILE_PATH);
        Button chooseDocButton = new Button(toolPropertiesGroup, SWT.PUSH);
        chooseDocButton.setText(DOTS);

        docPathChooserButtonListener = new PathChooserButtonListener(documenationText, false, getShell());
        chooseDocButton.addSelectionListener(docPathChooserButtonListener);
        documenationText.addModifyListener(e -> {
            docValid = validateDoc();
            validate(true);
        });
        Label toolDescriptionLabel = new Label(toolPropertiesGroup, SWT.NONE);
        toolDescriptionLabel.setText(Messages.toolDescription);
        GridData toolDescriptionLabelData = new GridData();
        toolDescriptionLabelData.verticalAlignment = GridData.BEGINNING;
        toolDescriptionLabel.setLayoutData(toolDescriptionLabelData);
        descriptionTextArea = new Text(toolPropertiesGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
        descriptionTextArea.addModifyListener(new TextModifyListener(descriptionTextArea));
        descriptionTextArea.setData(KEY_KEYS, ToolIntegrationConstants.KEY_TOOL_DESCRIPTION);
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
            ToolIntegrationConstants.KEY_TOOL_INTEGRATOR_NAME);
        integratorEmail = addLabelAndTextfieldForPropertyToComposite(userInformationGroup, Messages.email,
            ToolIntegrationConstants.KEY_TOOL_INTEGRATOR_EMAIL);
        setControl(container);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(this.getControl(),
            HELP_CONTEXT_ID);
        validate(false);

    }

    private String validateDoc() {
        if (documenationText.getText() != null && !documenationText.getText().isEmpty()) {
            File doc = new File(documenationText.getText());
            if (!doc.exists() && !doc.isAbsolute()
                && configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME) != null
                && !((String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME)).isEmpty()) {
                ToolIntegrationContext context = ((ToolIntegrationWizard) getWizard()).getCurrentContext();
                doc = new File(new File(
                    new File(new File(context.getRootPathToToolIntegrationDirectory(), context.getNameOfToolIntegrationDirectory()),
                        (String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME)),
                    ToolIntegrationConstants.DOCS_DIR_NAME), documenationText.getText());
            }
            if (doc.exists()) {
                String extension = FilenameUtils.getExtension(doc.getAbsolutePath());
                if (!ArrayUtils.contains(ToolIntegrationConstants.VALID_DOCUMENTATION_EXTENSIONS, extension)) {
                    StringBuilder allowedExt = new StringBuilder(DOC_EXTENTION_NOT_VALID);
                    for (String current : ToolIntegrationConstants.VALID_DOCUMENTATION_EXTENSIONS) {
                        allowedExt.append(current + VALID_EXTENSION_SEPERATOR);
                    }
                    return allowedExt.toString().substring(0, allowedExt.length() - VALID_EXTENSION_SEPERATOR.length());
                }
            } else {
                return DOC_DOES_NOT_EXIST;
            }
        }
        return "";
    }

    private String validateIcon() {
        if (iconText.getText() != null && !iconText.getText().isEmpty()) {
            try {
                File icon = new File(iconText.getText());
                if (!icon.exists() && !icon.isAbsolute()
                    && (String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME) != null
                    && !((String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME)).isEmpty()) {
                    ToolIntegrationContext context = ((ToolIntegrationWizard) getWizard()).getCurrentContext();
                    icon = new File(new File(
                        new File(context.getRootPathToToolIntegrationDirectory(), context.getNameOfToolIntegrationDirectory()),
                        (String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME)),
                        iconText.getText());
                }
                Image image = ImageIO.read(icon);
                if (image == null) {
                    return "Icon path or file format is invalid. The default icon will be used.";
                }
            } catch (IOException ex) {
                return "Icon path or file format is invalid. The default icon will be used.";
            }
        }
        return "";
    }

    private String validateName() {
        Optional<String> validationResult = ComponentIdRules.validateComponentIdRules(toolNameText.getText());
        if (validationResult.isPresent()) {
            return StringUtils.format(STRING_TOOLNAME_INVALID, validationResult.get());
        }
        String name = toolNameText.getText().trim();
        if (nameOrigin != null && name.equalsIgnoreCase(nameOrigin)) {
            return "";
        }
        Set<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        set.addAll(usedToolnames);
        if (set.contains(name)) {
            set.removeIf((String s) -> !s.trim().equalsIgnoreCase(name));
            return StringUtils.format(STRING_TOOL_NAME_EXISTS, set.iterator().next());
        }
        return "";
    }

    private String validateGroupPath() {
        Optional<String> validationResult = ComponentGroupPathRules.validateComponentGroupPathRules(groupPathText.getText());
        if (!groupPathText.getText().isEmpty() && validationResult.isPresent()) {
            return StringUtils.format(STRING_GROUPNAME_INVALID, validationResult.get());
        }
        return "";
    }

    private void showGroupSelectionDialog() {
        ElementListSelectionDialog dlg =
            new ElementListSelectionDialog(
                getShell(),
                new LabelProvider());
        dlg.setElements(groupNames.toArray());
        dlg.setHelpAvailable(false);
        dlg.setMultipleSelection(false);
        dlg.setStatusLineAboveButtons(false);
        dlg.setMessage(Messages.chooseGroupDlgMessage);
        dlg.setTitle(Messages.chooseGroupDlgTitle);
        if (!groupPathText.getText().isEmpty() && groupNames.contains(groupPathText.getText())) {
            dlg.setInitialSelections(groupPathText.getText());
        }
        if (dlg.open() == Window.OK) {
            groupPathText.setText(dlg.getFirstResult().toString());
        }
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
        propertyText.setData(KEY_KEYS, key);
        propertyText.addModifyListener(new TextModifyListener(propertyText));
        return propertyText;
    }

    /**
     * Listener for writing all configuration into the map when entered.
     * 
     * @author Sascha Zur
     */
    private class TextModifyListener implements ModifyListener {

        private final Text text;

        TextModifyListener(Text text) {
            this.text = text;
        }

        @Override
        public void modifyText(ModifyEvent arg0) {
            configurationMap.put((String) text.getData(KEY_KEYS), text.getText());
        }
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
        setMessage(null, DialogPage.NONE);
        setPageComplete(true);
        String name = (String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME);
        if (name == null
            || name.trim().isEmpty()) {
            setMessage(Messages.toolFilenameInvalid, DialogPage.ERROR);
            setPageComplete(false);
        } else if ((!update) && usedToolnames.contains((configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME)))) {
            setMessage(Messages.toolFilenameUsed, DialogPage.ERROR);
            setPageComplete(false);
        }
        if (!docValid.isEmpty()) {
            setMessage(docValid, DialogPage.ERROR);
            setPageComplete(false);
        }
        if (!groupValid.isEmpty()) {
            setMessage(groupValid, DialogPage.ERROR);
            setPageComplete(false);
        }
        if (!iconValid.isEmpty()) {
            setMessage(iconValid, DialogPage.WARNING);
        }
        if (!nameValid.isEmpty()) {
            setMessage(nameValid, DialogPage.ERROR);
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
        if (configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME) != null) {
            nameOrigin = String.valueOf(configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME));
        } else {
            nameOrigin = null;
        }
        if (configurationMap.get(ToolIntegrationConstants.KEY_UPLOAD_ICON) == null) {
            configurationMap.put(ToolIntegrationConstants.KEY_UPLOAD_ICON, true);
        }
        updatePageValues();

    }

    private void updatePageValues() {
        if ((String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME) != null) {
            toolNameText.setText((String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME));
        } else {
            toolNameText.setText("");
        }
        if (configurationMap.get(ToolIntegrationConstants.KEY_TOOL_ICON_PATH) != null) {
            iconText.setText((String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_ICON_PATH));
        } else {
            iconText.setText("");
        }
        if ((Boolean) configurationMap.get(ToolIntegrationConstants.KEY_UPLOAD_ICON)) {
            uploadIconToFolder.setSelection(true);
        } else {
            uploadIconToFolder.setSelection(false);
        }
        if (configurationMap.get(ToolIntegrationConstants.KEY_DOC_FILE_PATH) != null) {
            documenationText.setText((String) configurationMap.get(ToolIntegrationConstants.KEY_DOC_FILE_PATH));
            File pathToOpen = new File(documenationText.getText());
            if (!pathToOpen.isAbsolute()) {
                ToolIntegrationContext context = ((ToolIntegrationWizard) getWizard()).getCurrentContext();
                pathToOpen = new File(
                    new File(new File(context.getRootPathToToolIntegrationDirectory(), context.getNameOfToolIntegrationDirectory()),
                        (String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME)),
                    ToolIntegrationConstants.DOCS_DIR_NAME);
            }
            docPathChooserButtonListener.setOpenPath(pathToOpen);
        } else {
            documenationText.setText("");
        }
        if (configurationMap.get(ToolIntegrationConstants.KEY_TOOL_GROUPNAME) != null) {
            groupPathText.setText((String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_GROUPNAME));
        } else {
            groupPathText.setText("");
        }
        if (configurationMap.get(ToolIntegrationConstants.KEY_TOOL_DESCRIPTION) != null) {
            descriptionTextArea.setText((String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_DESCRIPTION));
        } else {
            descriptionTextArea.setText("");
        }
        if (configurationMap.get(ToolIntegrationConstants.KEY_TOOL_INTEGRATOR_NAME) != null) {
            integratorName.setText((String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_INTEGRATOR_NAME));
        } else {
            integratorName.setText("");
        }
        if (configurationMap.get(ToolIntegrationConstants.KEY_TOOL_INTEGRATOR_EMAIL) != null) {
            integratorEmail.setText((String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_INTEGRATOR_EMAIL));
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
