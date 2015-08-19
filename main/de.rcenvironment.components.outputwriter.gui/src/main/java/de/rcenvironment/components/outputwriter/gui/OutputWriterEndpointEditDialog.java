/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.gui;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.components.outputwriter.common.OutputWriterComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataDefinition;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.utils.incubator.AlphanumericalTextContraintListener;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointEditDialog;

/**
 * Special edit dialog for the output writer.
 * 
 * @author Sascha Zur
 */
public class OutputWriterEndpointEditDialog extends EndpointEditDialog {

    private static final char[] FORBIDDEN_CHARS = new char[] { '/', '\\', ':',
        '*', '?', '\"', '>', '<', '|' };

    private static final int MINUS_ONE = -1;

    private final Set<String> paths;

    public OutputWriterEndpointEditDialog(Shell parentShell, String title,
        ComponentInstanceProperties configuration, EndpointType direction,
        String id, boolean isStatic, EndpointMetaDataDefinition metaData, Map<String, String> metadataValues, Set<String> paths) {
        super(parentShell, title, configuration, direction, id, isStatic, metaData, metadataValues);
        this.paths = paths;
    }

    @Override
    protected Text createLabelAndTextfield(Composite container, String text, String dataType, String value) {
        final Label nameLabel = new Label(container, SWT.NONE);
        nameLabel.setText(text + "    ");
        final Text result = new Text(container, SWT.SINGLE | SWT.BORDER);
        result.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        result.setText(value);
        result.addListener(SWT.Verify, new AlphanumericalTextContraintListener(FORBIDDEN_CHARS));
        new Label(container, SWT.NONE).setText("");
        final Composite placeholderComp = new Composite(container, SWT.NONE);
        placeholderComp.setLayout(new GridLayout(2, false));
        final Combo placeholderCombo = new Combo(placeholderComp, SWT.READ_ONLY);
        Button insertButton = new Button(placeholderComp, SWT.PUSH);
        insertButton.setText("Insert");
        placeholderCombo.setItems(OutputWriterComponentConstants.WORDLIST);
        placeholderCombo.select(0);
        insertButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                int positionbuffer = result.getCaretPosition();
                String word = placeholderCombo.getText();
                result.insert(word);
                if (result.getText().length() >= (positionbuffer + word.length())) {
                    result.setSelection(positionbuffer + word.length());
                }
                result.forceFocus();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {

            }
        });
        new Label(container, SWT.NONE).setText("Target folder: ");
        final Combo directoryCombo = new Combo(container, SWT.READ_ONLY);
        directoryCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        if (!paths.contains(OutputWriterComponentConstants.ROOT_DISPLAY_NAME)) {
            directoryCombo.add(OutputWriterComponentConstants.ROOT_DISPLAY_NAME);
        }
        for (String path : paths) {
            directoryCombo.add(path);
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
        new Label(container, SWT.NONE).setText("Sub folder: ");
        final Text additionalFolder = new Text(container, SWT.SINGLE | SWT.BORDER);
        additionalFolder.setMessage("Optional");
        additionalFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        additionalFolder.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent arg0) {
                metadataValues.put(OutputWriterComponentConstants.CONFIG_KEY_FOLDERFORSAVING,
                    OutputWriterComponentConstants.ROOT_DISPLAY_NAME + File.separator + ((Text) arg0.getSource()).getText());
            }
        });
        if (directoryCombo.getSelectionIndex() > 0) {
            additionalFolder.setEnabled(false);
        }
        new Label(container, SWT.NONE).setText("");
        new Label(container, SWT.NONE).setText("Note: Currently, only one sub folder is allowed");
        new Label(container, SWT.NONE).setText("");
        final Composite placeholderComp2 = new Composite(container, SWT.NONE);
        placeholderComp2.setLayout(new GridLayout(2, false));
        final Combo placeholderCombo2 = new Combo(placeholderComp2, SWT.READ_ONLY);
        final Button insertButton2 = new Button(placeholderComp2, SWT.PUSH);
        insertButton2.setText("Insert");
        placeholderCombo2.setItems(OutputWriterComponentConstants.WORDLIST);
        placeholderCombo2.select(0);
        insertButton2.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                int positionbuffer = additionalFolder.getCaretPosition();
                String word = placeholderCombo2.getText();
                additionalFolder.insert(word);
                if (additionalFolder.getText().length() >= (positionbuffer + word.length())) {
                    additionalFolder.setSelection(positionbuffer + word.length());
                }
                additionalFolder.forceFocus();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetDefaultSelected(arg0);
            }
        });

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
        return result;

    }

}
