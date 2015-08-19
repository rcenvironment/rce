/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

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

import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.gui.utils.incubator.AlphanumericalTextContraintListener;
import de.rcenvironment.core.gui.wizards.toolintegration.api.ToolIntegrationWizardPage;

/**
 * @author Sascha Zur
 */
public class ToolCharacteristicsPage extends ToolIntegrationWizardPage {

    private static final int LABEL_WIDTH = 80;

    private static final int TOOL_DESCRIPTION_TEXT_HEIGHT = 50;

    private static final String KEY_KEYS = "properties";

    protected Map<String, Object> configurationMap;

    private Text toolNameText;

    private Text iconText;

    private final List<String> usedToolnames;

    private Text groupNameText;

    private Text descriptionTextArea;

    private Text integratorName;

    private Text integratorEmail;

    private Button uploadIconToFolder;

    private List<String> groupNames;

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
            new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL | GridData.FILL_VERTICAL | GridData.GRAB_VERTICAL);
        container.setLayoutData(containerData);

        Composite toolDataComp = new Composite(container, SWT.NONE);
        toolDataComp.setLayout(new GridLayout(1, false));
        GridData toolData = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
        toolDataComp.setLayoutData(toolData);
        Group toolPropertiesGroup = new Group(toolDataComp, SWT.NONE);
        toolPropertiesGroup.setText(Messages.toolPropGroup);
        toolPropertiesGroup.setLayout(new GridLayout(3, false));
        GridData toolPropertyData =
            new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL | GridData.FILL_VERTICAL | GridData.GRAB_VERTICAL);
        toolPropertiesGroup.setLayoutData(toolPropertyData);
        toolNameText =
            addLabelAndTextfieldForPropertyToComposite(toolPropertiesGroup, Messages.nameRequired,
                ToolIntegrationConstants.KEY_TOOL_NAME);
        ((GridData) toolNameText.getLayoutData()).horizontalSpan = 2;
        toolNameText.addListener(SWT.Verify, new AlphanumericalTextContraintListener(true, false));
        iconText =
            addLabelAndTextfieldForPropertyToComposite(toolPropertiesGroup, Messages.iconPath, ToolIntegrationConstants.KEY_TOOL_ICON_PATH);
        iconText.setMessage(Messages.iconSizeMessage);
        GridLayout iconCompLayout = new GridLayout(2, false);
        iconCompLayout.marginWidth = 0;
        Composite iconComp = new Composite(toolPropertiesGroup, SWT.NONE);
        iconComp.setLayout(iconCompLayout);

        Button choosePathButton = new Button(iconComp, SWT.PUSH);
        choosePathButton.setText("  ...  ");
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
        groupNameText =
            addLabelAndTextfieldForPropertyToComposite(toolPropertiesGroup, Messages.groupNameText,
                ToolIntegrationConstants.KEY_TOOL_GROUPNAME);
        GridData groupNameTextData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        groupNameText.setLayoutData(groupNameTextData);
        Button chooseGroupButton = new Button(toolPropertiesGroup, SWT.PUSH);
        chooseGroupButton.setText("  ...  ");
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
            new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL | GridData.GRAB_VERTICAL);
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
        validate(false);

    }

    private void showGroupSelectionDialog() {
        ElementListSelectionDialog dlg =
            new ElementListSelectionDialog(
                getShell(),
                new LabelProvider()
            );
        dlg.setElements(groupNames.toArray());
        dlg.setHelpAvailable(false);
        dlg.setMultipleSelection(false);
        dlg.setStatusLineAboveButtons(false);
        dlg.setMessage(Messages.chooseGroupDlgMessage);
        dlg.setTitle(Messages.chooseGroupDlgTitle);
        if (!groupNameText.getText().isEmpty() && groupNames.contains(groupNameText.getText())) {
            dlg.setInitialSelections(new String[] { groupNameText.getText() });
        }
        if (dlg.open() == Window.OK) {
            groupNameText.setText(dlg.getFirstResult().toString());
        }
    }

    private Text addLabelAndTextfieldForPropertyToComposite(Composite composite,
        String propertyMessage, String key) {
        Label propertyLabel = new Label(composite, SWT.NONE);
        propertyLabel.setText(propertyMessage);
        GridData labelData = new GridData();
        labelData.widthHint = LABEL_WIDTH;
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

        public TextModifyListener(Text text) {
            this.text = text;
        }

        @Override
        public void modifyText(ModifyEvent arg0) {
            configurationMap.put((String) text.getData(KEY_KEYS), text.getText());
            validate(false);
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
        if (iconText.getText() != null && !iconText.getText().isEmpty()) {
            try {
                File icon = new File(iconText.getText());
                Image image = ImageIO.read(icon);
                if (image == null) {
                    setMessage("Icon path or file format is invalid. The default icon will be used.", DialogPage.WARNING);
                }
            } catch (IOException ex) {
                setMessage("Icon path or file format is invalid. The default icon will be used.", DialogPage.WARNING);
            }
        }
        if (configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME) == null
            || ((String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME)).isEmpty()) {
            setMessage(Messages.toolFilenameInvalid, DialogPage.ERROR);
            setPageComplete(false);
        } else if ((!update) && usedToolnames.contains((configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME)))) {
            setMessage(Messages.toolFilenameUsed, DialogPage.ERROR);
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
        if ((String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME) != null) {
            usedToolnames.add((String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME));
        }
        configurationMap = newConfigurationMap;
        if (configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME) != null) {
            usedToolnames.remove(configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME));
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
        if (configurationMap.get(ToolIntegrationConstants.KEY_TOOL_GROUPNAME) != null) {
            groupNameText.setText((String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_GROUPNAME));
        } else {
            groupNameText.setText("");
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
        if (!(new File(iconText.getText()).isAbsolute())) {
            String configPath = ((ChooseConfigurationPage) getWizard().getPreviousPage(this)).getChoosenConfigPath();
            File toolFolder = new File(configPath).getParentFile();
            if (toolFolder != null) {
                File icon = new File(toolFolder, iconText.getText());
                if (icon.exists() && icon.isFile()) {
                    uploadIconToFolder.setSelection(true);
                    iconText.setText(icon.getAbsolutePath());
                }
            }

        }
        validate(true);
    }

    @Override
    public void performHelp() {
        super.performHelp();
        IWorkbenchHelpSystem helpSystem = PlatformUI.getWorkbench().getHelpSystem();
        helpSystem.displayHelp("de.rcenvironment.core.gui.wizard.toolintegration.integration_characteristics");
    }

    @Override
    public void updatePage() {
        // TODO Auto-generated method stub
    }
}
