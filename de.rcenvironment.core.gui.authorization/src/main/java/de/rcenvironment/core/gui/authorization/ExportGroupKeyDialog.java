/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.authorization;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.core.gui.utils.common.ClipboardHelper;

/**
 * Dialog to export a group.
 *
 * @author Oliver Seebach
 * @author Jan Flink
 */
public class ExportGroupKeyDialog extends Dialog {

    private static final int DEFAULT_TEXTFIELD_WIDTH = 300;

    /**
     * Selection listener to copy a group key to clipboard.
     *
     * @author Oliver Seebach
     */
    private final class CopyToClipboardSelectionListener implements SelectionListener {

        @Override
        public void widgetSelected(SelectionEvent event) {
            ClipboardHelper.setContent(exportGroupKeyTextfield.getText());
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent event) {
            widgetSelected(event);
        }
    }

    private String groupKeyToExport = "";

    private Text exportGroupKeyTextfield;

    protected ExportGroupKeyDialog(Shell parentShell, String groupKeyToExport) {
        super(parentShell);
        this.groupKeyToExport = groupKeyToExport;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Export Group Key");
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        GridLayout containerLayout = new GridLayout(2, false);
        container.setLayout(containerLayout);

        Label exportGroupKeyLabel = new Label(container, SWT.NULL);
        exportGroupKeyLabel.setText("Group Key to export: ");

        exportGroupKeyTextfield = new Text(container, SWT.BORDER | SWT.READ_ONLY);
        GridData exportGroupKeyGridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        exportGroupKeyGridData.widthHint = DEFAULT_TEXTFIELD_WIDTH;
        exportGroupKeyTextfield.setLayoutData(exportGroupKeyGridData);
        exportGroupKeyTextfield.setText(groupKeyToExport);
        exportGroupKeyTextfield.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
        exportGroupKeyTextfield.setSelection(0, exportGroupKeyTextfield.getText().length());

        Button copyToClipboardButton = new Button(container, SWT.PUSH);
        copyToClipboardButton.setText("Copy to Clipboard");
        GridData copyToClipboardButtonGridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        copyToClipboardButtonGridData.horizontalSpan = 2;
        copyToClipboardButtonGridData.horizontalAlignment = SWT.RIGHT;
        copyToClipboardButton.setLayoutData(copyToClipboardButtonGridData);
        copyToClipboardButton.addSelectionListener(new CopyToClipboardSelectionListener());

        return container;
    }
}
