/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.authorization;

import java.util.Optional;

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

import de.rcenvironment.core.authorization.api.AuthorizationIdRules;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.utils.common.ClipboardHelper;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Dialog to import a group.
 *
 * @author Oliver Seebach
 * @author Jan Flink
 * @author Robert Mischke (changed validation)
 */
public class ImportGroupKeyDialog extends Dialog {

    private static final int DEFAULT_TEXTFIELD_WIDTH = 300;

    private static final int VALIDATION_LABEL_HEIGHT_HINT = 55;

    /**
     * Selection listener to paste a group key frlom clipboard.
     *
     * @author Oliver Seebach
     */
    private final class PasteFromClipboardSelectionListener implements SelectionListener {

        @Override
        public void widgetSelected(SelectionEvent event) {
            String keyFromClipboard = ClipboardHelper.getContentAsStringOrNull();
            if (keyFromClipboard != null && !keyFromClipboard.isEmpty()) {
                importGroupKeyTextfield.setText(keyFromClipboard.trim());
            }
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent event) {
            widgetSelected(event);
        }
    }

    private String keyToImport = "";

    private Text importGroupKeyTextfield;

    protected ImportGroupKeyDialog(Shell parentShell) {
        super(parentShell);
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Import Group Key");
    }

    public String getKeyToImport() {
        return keyToImport;
    }

    @Override
    protected void okPressed() {
        keyToImport = importGroupKeyTextfield.getText();
        super.okPressed();
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        GridLayout containerLayout = new GridLayout(2, false);
        container.setLayout(containerLayout);

        Label importGroupKeyLabel = new Label(container, SWT.NULL);
        importGroupKeyLabel.setText("Group Key to import: ");

        importGroupKeyTextfield = new Text(container, SWT.BORDER);
        GridData importGroupKeyGridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        importGroupKeyGridData.widthHint = DEFAULT_TEXTFIELD_WIDTH;
        importGroupKeyTextfield.setLayoutData(importGroupKeyGridData);

        Composite validationMessage = new Composite(container, SWT.NONE);
        GridData validationMessageGridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        validationMessageGridData.horizontalSpan = 2;
        validationMessage.setLayoutData(validationMessageGridData);
        validationMessage.setLayout(new GridLayout(2, false));
        validationMessage.setVisible(false);
        Label validationIconLabel = new Label(validationMessage, SWT.NONE);
        validationIconLabel.setImage(ImageManager.getInstance().getSharedImage(StandardImages.ERROR_16));
        validationIconLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, true));
        Label validationTextLabel = new Label(validationMessage, SWT.WRAP);
        GridData validationLabelGridData = new GridData(SWT.FILL, SWT.NONE, true, false);
        validationLabelGridData.widthHint = DEFAULT_TEXTFIELD_WIDTH;
        validationLabelGridData.heightHint = VALIDATION_LABEL_HEIGHT_HINT;
        validationTextLabel.setLayoutData(validationLabelGridData);

        importGroupKeyTextfield.addModifyListener(event -> {
            String importString = importGroupKeyTextfield.getText().trim();
            Optional<String> validationError = AuthorizationIdRules.validateAuthorizationGroupImportString(importString);
            if (!importString.isEmpty() && validationError.isPresent()) {
                validationTextLabel.setText(StringUtils.format("%s.", validationError.get().replaceAll("&", "&&")));
                validationMessage.setVisible(true);
            } else {
                validationTextLabel.setText("");
                validationMessage.setVisible(false);
            }
            getButton(IDialogConstants.OK_ID).setEnabled(!importString.isEmpty() && !validationError.isPresent());
        });

        Button pasteFromClipboardButton = new Button(container, SWT.PUSH);
        pasteFromClipboardButton.setText("Paste from Clipboard");
        GridData pasteFromClipboardButtonGridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        pasteFromClipboardButtonGridData.horizontalSpan = 2;
        pasteFromClipboardButtonGridData.horizontalAlignment = SWT.RIGHT;
        pasteFromClipboardButton.setLayoutData(pasteFromClipboardButtonGridData);
        pasteFromClipboardButton.addSelectionListener(new PasteFromClipboardSelectionListener());

        return container;
    }
}
