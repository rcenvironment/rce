/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.integration.common.editor;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.PlatformUI;

/**
 * The lower button bar for {@link IntegrationEditorPage} and {@link WorkflowEditorPage}.
 *
 * @author Kathrin Schaffert
 * @author Jan Flink
 */
public class IntegrationEditorButtonBar extends Composite implements SelectionListener {

    protected static final String KEY = "id";

    protected static final String ID_CANCEL = "Cancel";

    protected static final String ID_SAVE = "Save";

    protected static final String ID_NEXT = "Next";

    protected static final String ID_BACK = "Back";

    private static final String ID_HELP = "Help";

    private IntegrationEditor editorPart;

    private Button backButton;

    private Button nextButton;

    private Button saveButton;

    public IntegrationEditorButtonBar(Composite parent, IntegrationEditor editorPart) {
        super(parent, SWT.None);
        this.editorPart = editorPart;
        createEditorButtons();
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent arg0) {
        // Intentionally left empty.
    }

    private void createEditorButtons() {
        setLayout(new GridLayout(2, false));
        setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        ToolBar toolbar = new ToolBar(this, SWT.None);
        ToolItem helpButton = new ToolItem(toolbar, SWT.PUSH | SWT.FLAT);
        helpButton.setImage(JFaceResources.getImage(Dialog.DLG_IMG_HELP));
        helpButton.setData(IntegrationEditorButtonBar.KEY, ID_HELP);
        helpButton.addSelectionListener(this);

        GridData compositeGridData = new GridData();
        compositeGridData.horizontalAlignment = GridData.END;
        compositeGridData.verticalAlignment = GridData.END;
        compositeGridData.grabExcessHorizontalSpace = true;
        compositeGridData.grabExcessVerticalSpace = false;

        Composite buttonComposite = new Composite(this, SWT.NONE);
        buttonComposite.setLayout(new GridLayout(4, true));
        buttonComposite.setLayoutData(compositeGridData);
        buttonComposite.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));


        backButton = createButton(buttonComposite, IDialogConstants.BACK_LABEL, ID_BACK);
        nextButton = createButton(buttonComposite, IDialogConstants.NEXT_LABEL, ID_NEXT);
        saveButton = createButton(buttonComposite, editorPart.getButtonTextIntegrate(), ID_SAVE);
        saveButton.setEnabled(false);
        createButton(buttonComposite, IDialogConstants.CANCEL_LABEL, ID_CANCEL);
    }

    private Button createButton(Composite buttonComposite, String label, String id) {
        Button button = new Button(buttonComposite, SWT.PUSH);
        button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        button.setText(label);
        button.setData(IntegrationEditorButtonBar.KEY, id);
        button.addSelectionListener(this);
        return button;
    }

    
    @Override
    public void widgetSelected(SelectionEvent evt) {

        String buttonId = (String) evt.widget.getData(KEY);
        switch (buttonId) {
        case ID_BACK:
            editorPart.setPreviousPage();
            break;
        case ID_NEXT:
            editorPart.setNextPage();
            break;
        case ID_SAVE:
            editorPart.integrate();
            break;
        case ID_CANCEL:
            editorPart.cancelPressed();
            break;
        case ID_HELP:
            helpPressed();
            break;
        default: // should never happen
        }
    }

    private void helpPressed() {
        PlatformUI.getWorkbench().getHelpSystem().displayDynamicHelp();
    }

    public void setBackButtonEnabled(boolean enable) {
        backButton.setEnabled(enable);
    }

    public void setNextButtonEnabled(boolean enable) {
        nextButton.setEnabled(enable);
    }

    public void setSaveButtonEnabled(boolean enable) {
        saveButton.setEnabled(enable);
    }
}
