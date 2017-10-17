/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.gui;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.components.outputwriter.common.OutputWriterComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataDefinition;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointActionType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.utils.incubator.AlphanumericalTextContraintListener;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointEditDialog;

/**
 * Special edit dialog for the output writer.
 * 
 * @author Sascha Zur
 * @author Brigitte Boden
 */
public class OutputWriterEndpointEditDialog extends EndpointEditDialog {

    private static final String NO_DATA_STRING = "-";

    private static final char[] FORBIDDEN_CHARS = new char[] { '/', '\\', ':',
        '*', '?', '\"', '>', '<', '|' };

    private static final int MINUS_ONE = -1;

    private static final String COLON = ":";

    private final Set<String> paths;

    private Text result;

    private CLabel hintLabel;

    public OutputWriterEndpointEditDialog(Shell parentShell, EndpointActionType actionType,
        ComponentInstanceProperties configuration, EndpointType direction,
        String id, boolean isStatic, EndpointMetaDataDefinition metaData, Map<String, String> metadataValues, Set<String> paths) {
        super(parentShell, actionType, configuration, direction, id, isStatic, metaData, metadataValues);
        this.paths = paths;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Control superControl = super.createDialogArea(parent);
        hintLabel = new CLabel((Composite) superControl, SWT.NONE);
        hintLabel
            .setText(
                "You are adding a primitive data type input."
                    + "Therefore, you also\nneed to add this input to a target file in the table below.");
        hintLabel.setImage(ImageManager.getInstance().getSharedImage(StandardImages.INFORMATION_16));
        return superControl;
    }

    
    
    @Override
    protected void createSettings(Map<Integer, String> sortedKeyMap, Composite container) {
        String key = OutputWriterComponentConstants.CONFIG_KEY_FILENAME;
        String text = metaData.getGuiName(key);
        String value = metadataValues.get(key);
        
        final Label nameLabel = new Label(container, SWT.NONE);
        nameLabel.setText(text + "    ");
        result = new Text(container, SWT.SINGLE | SWT.BORDER);
        result.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        if (value == null || value.equals(NO_DATA_STRING)) {
            value = "";
        }
        result.setText(value);
        result.addListener(SWT.Verify, new AlphanumericalTextContraintListener(FORBIDDEN_CHARS));
        widgetToKeyMap.put(result, key);
        result.addModifyListener(new MethodPropertiesModifyListener());
        new Label(container, SWT.NONE).setText("");
        final Composite placeholderComp = new Composite(container, SWT.NONE);
        placeholderComp.setLayout(new GridLayout(2, false));
        placeholderComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        final Combo placeholderCombo =
            OutputWriterGuiUtils.createPlaceholderCombo(placeholderComp, OutputWriterComponentConstants.WORDLIST);
        OutputWriterGuiUtils.createPlaceholderInsertButton(placeholderComp, placeholderCombo, result);

        new Label(container, SWT.NONE).setText(Messages.targetFolder + COLON);
        final Combo directoryCombo = new Combo(container, SWT.READ_ONLY);
        directoryCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        if (!paths.contains(OutputWriterComponentConstants.ROOT_DISPLAY_NAME)) {
            directoryCombo.add(OutputWriterComponentConstants.ROOT_DISPLAY_NAME);
        }
        for (String path : paths) {
            if (!path.equals(NO_DATA_STRING)) {
                directoryCombo.add(path);
            }
        }
        int index = MINUS_ONE;
        for (String item : directoryCombo.getItems()) {
            if (item.equals(metadataValues.get(OutputWriterComponentConstants.CONFIG_KEY_FOLDERFORSAVING))) {
                index = directoryCombo.indexOf(item);
            }
        }
        if (index >= 0) {
            directoryCombo.select(index);
        } else {
            directoryCombo.select(0);
        }
        metadataValues.put(OutputWriterComponentConstants.CONFIG_KEY_FOLDERFORSAVING, "" + directoryCombo.getText());
        new Label(container, SWT.NONE).setText(Messages.subFolder + COLON);
        final Text additionalFolder = new Text(container, SWT.SINGLE | SWT.BORDER);
        additionalFolder.setMessage(Messages.optionalMessage);
        additionalFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        additionalFolder.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent arg0) {
                metadataValues.put(OutputWriterComponentConstants.CONFIG_KEY_FOLDERFORSAVING,
                    OutputWriterComponentConstants.ROOT_DISPLAY_NAME + File.separator + ((Text) arg0.getSource()).getText());
            }
        });

        new Label(container, SWT.NONE).setText("");
        new Label(container, SWT.NONE).setText(Messages.onlyOneSubfolderMessage);
        new Label(container, SWT.NONE).setText("");

        final Composite placeholderComp2 = new Composite(container, SWT.NONE);
        placeholderComp2.setLayout(new GridLayout(2, false));
        placeholderComp2.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        final Combo placeholderCombo2 =
            OutputWriterGuiUtils.createPlaceholderCombo(placeholderComp2, OutputWriterComponentConstants.WORDLIST_SUBFOLDER);
        final Button insertButton2 =
            OutputWriterGuiUtils.createPlaceholderInsertButton(placeholderComp2, placeholderCombo2, additionalFolder);

        if (directoryCombo.getSelectionIndex() > 0) {
            additionalFolder.setEnabled(false);
            placeholderCombo2.setEnabled(false);
            insertButton2.setEnabled(false);
        }

        directoryCombo.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                metadataValues.put(OutputWriterComponentConstants.CONFIG_KEY_FOLDERFORSAVING, "" + ((Combo) arg0.getSource()).getText());
                additionalFolder.setEnabled(((Combo) arg0.getSource()).getText().equals(OutputWriterComponentConstants.ROOT_DISPLAY_NAME));
                placeholderCombo2.setEnabled(((Combo) arg0.getSource()).getText().equals(OutputWriterComponentConstants.ROOT_DISPLAY_NAME));
                insertButton2.setEnabled(((Combo) arg0.getSource()).getText().equals(OutputWriterComponentConstants.ROOT_DISPLAY_NAME));
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });
        additionalFolder.addListener(SWT.Verify, new AlphanumericalTextContraintListener(FORBIDDEN_CHARS));
        
    }
    
    @Override
    public Map<String, String> getMetadataValues() {
        Map<String, String> metaData = super.getMetadataValues();
        if (!currentDataType.equals(DataType.DirectoryReference) && !currentDataType.equals(DataType.FileReference)) {
            //If the input has a simple data type, remove values for target file and target folder
            metaData.put(OutputWriterComponentConstants.CONFIG_KEY_FILENAME, NO_DATA_STRING);
            metaData.put(OutputWriterComponentConstants.CONFIG_KEY_FOLDERFORSAVING, NO_DATA_STRING);
        }
        return metaData;
    }

    @Override
    protected boolean validateMetaDataInputs() {
        List<String> forbiddenFilenames = Arrays.asList(OutputWriterComponentConstants.PROBLEMATICFILENAMES_WIN);
        hintLabel.setVisible(!comboDataType.getText().equals(DataType.FileReference.getDisplayName())
            && !comboDataType.getText().equals(DataType.DirectoryReference.getDisplayName()));
        return super.validateMetaDataInputs() && !forbiddenFilenames.contains(result.getText().toUpperCase());
    }

}
