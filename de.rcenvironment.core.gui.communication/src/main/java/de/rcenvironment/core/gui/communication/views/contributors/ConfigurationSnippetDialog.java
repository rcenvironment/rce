/*
 * Copyright 2020-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.communication.views.contributors;

import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.core.gui.resources.api.ColorManager;
import de.rcenvironment.core.gui.resources.api.StandardColors;
import de.rcenvironment.core.gui.utils.common.ClipboardHelper;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Dialog to show the configuration snippet for a connection.
 *
 * @author Kathrin Schaffert
 * @author Jan Flink
 */
public class ConfigurationSnippetDialog extends TitleAreaDialog {

    private static final String CONFIGURATION_NODES_TEMPLATE = "\"%s\" : {\r\n\t\"%s\" : {\r\n\t\t\"%s\" : {\r\n";

    private static final String CONFIGURATION_ENTRY_TEMPLATE = "\t\t\t\"%s\" : %s";

    private static final String CLOSING_BRACKET_TEMPLATE = "\r\n\t\t}\r\n\t}\r\n}";

    private Text configurationSnippetTextfield;

    private String configurationSnippet;

    public ConfigurationSnippetDialog(Shell parent, String topLevelNode, String secondLevelNode, String configurationNode,
        Map<String, Object> configurationEntries) {
        super(parent);
        setDialogHelpAvailable(false);
        configurationSnippet = buildSnippet(topLevelNode, secondLevelNode, configurationNode, configurationEntries);
    }

    private String buildSnippet(String topLevelNode, String secondLevelNode, String configurationNode,
        Map<String, Object> configurationEntries) {
        StringBuilder builder = new StringBuilder();
        builder.append(StringUtils.format(CONFIGURATION_NODES_TEMPLATE, topLevelNode, secondLevelNode, configurationNode));
        builder.append(String.join(",\r\n", configurationEntries.entrySet().stream()
            .map(entry -> StringUtils.format(CONFIGURATION_ENTRY_TEMPLATE, entry.getKey(), getValueString(entry.getValue())))
            .toArray(String[]::new)));
        builder.append(CLOSING_BRACKET_TEMPLATE);
        return builder.toString();
    }

    private String getValueString(Object value) {
        if (value instanceof String) {
            return StringUtils.format("\"%s\"", String.valueOf(value));
        }
        return String.valueOf(value);
    }

    @Override
    public void create() {
        super.create();
        setTitle("Configuration Snippet");
        setMessage("In order to persist this connection, the configuration snippet "
            + "can be copied into the profile's configuration file (configuration.json).");
    }


    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        
        configurationSnippetTextfield = new Text(container, SWT.MULTI | SWT.READ_ONLY);
        configurationSnippetTextfield.setBackground(ColorManager.getInstance().getSharedColor(StandardColors.RCE_WHITE));
        configurationSnippetTextfield.setText(configurationSnippet);
        configurationSnippetTextfield.setLayoutData(new GridData(GridData.FILL_BOTH));

        GridData buttonData = new GridData();
        buttonData.horizontalIndent = 2;
        buttonData.verticalIndent = 4;

        Button copyToClipboardButton = new Button(container, SWT.PUSH);
        copyToClipboardButton.setText("Copy To Clipboard");
        copyToClipboardButton.setLayoutData(buttonData);
        copyToClipboardButton.addSelectionListener(new CopyToClipboardSelectionListener());

        GridData noteData = new GridData();
        noteData.horizontalIndent = 4;
        noteData.verticalIndent = 4;
        noteData.widthHint = getInitialSize().x;

        Label note = new Label(container, SWT.WRAP);
        note.setText("Please ensure to keep the JSON scheme when copying the snippet into the configuration file. \n"
            + "After restarting RCE, the connection is permanently available in the Network View.");
        note.setLayoutData(noteData);

        return container;
    }

    /**
     * Selection listener to copy the configuration snippet to clipboard.
     *
     * @author Kathrin Schaffert
     */
    private final class CopyToClipboardSelectionListener implements SelectionListener {

        @Override
        public void widgetSelected(SelectionEvent event) {
            ClipboardHelper.setContent(configurationSnippetTextfield.getText());
            MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "Copy to Clipboard",
                "The configuration snippet was successfully copied to the clipboard.");
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent event) {
            widgetSelected(event);
        }
    }

}
