/*
 * Copyright 2020-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.communication.views.contributors;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.core.gui.utils.common.ClipboardHelper;

/**
 * Dialog to show the configuration snippet for a connection.
 *
 * @author Kathrin Schaffert
 */
public class ShowConfigurationSnippetDialog extends Dialog {

    private static final int DEFAULT_TEXTFIELD_WIDTH = 300;

    private static final int DEFAULT_TEXTFIELD_HEIGHT = 150;

    private Text configurationSnippetTextfield;

    private String configurationSnippet;

    public ShowConfigurationSnippetDialog(Shell parent, String message) {
        super(parent);

        this.configurationSnippet = message;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Show Configuration Snippet");
        shell.setFocus();
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        RowLayout containerLayout = new RowLayout(3);
        container.setLayout(containerLayout);

        Label configurationSnippetLabel = new Label(container, SWT.NULL);
        configurationSnippetLabel.setText("Configuration Snippet: ");

        configurationSnippetTextfield = new Text(container, SWT.MULTI | SWT.BORDER | SWT.READ_ONLY);
        RowData configurationSnippetRowData = new RowData();
        configurationSnippetRowData.width = DEFAULT_TEXTFIELD_WIDTH;
        configurationSnippetRowData.height = DEFAULT_TEXTFIELD_HEIGHT;
        configurationSnippetTextfield.setLayoutData(configurationSnippetRowData);
        configurationSnippetTextfield.setText(configurationSnippet);

        Button copyToClipboardButton = new Button(container, SWT.PUSH);
        copyToClipboardButton.setText("Copy To Clipboard");
        copyToClipboardButton.addSelectionListener(new CopyToClipboardSelectionListener());

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
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent event) {
            widgetSelected(event);
        }
    }

    Text getConfigurationSnippetTextfield() {
        return configurationSnippetTextfield;
    }

    void setConfigurationSnippetTextfield(Text configurationSnippetTextfield) {
        this.configurationSnippetTextfield = configurationSnippetTextfield;
    }

}
