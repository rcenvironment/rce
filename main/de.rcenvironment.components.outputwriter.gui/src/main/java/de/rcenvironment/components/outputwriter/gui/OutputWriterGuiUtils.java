/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.outputwriter.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;


/**
 * Provides GUI elements needed by the OutputWriterEditDialog as well as the OutputLocationEditDialog.
 *
 * @author Brigitte Boden
 * 
 */
public final class OutputWriterGuiUtils {
    
    private OutputWriterGuiUtils(){}

    /**
     * Create a combo for selecting placeholders.
     * 
     * @param parent The parent composite
     * @param placeholders Array of possible placeholders
     * @return combo to select placeholders
     */
    public static Combo createPlaceholderCombo(Composite parent, String[] placeholders) {
        final Combo placeholderCombo = new Combo(parent, SWT.READ_ONLY);
        placeholderCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        
        placeholderCombo.setItems(placeholders);
        placeholderCombo.select(0);

        return placeholderCombo;
    }

    /**
     * Create an "insert" button for a given placeholder combo.
     * 
     * @param parent The parent composite
     * @param placeholderCombo The placeholder selection combo
     * @param textfield The textfield into which the placeholders are inserted
     * @return button to insert placeholders
     */
    public  static Button createPlaceholderInsertButton(Composite parent, final Combo placeholderCombo, final Text textfield) {
        Button insertButton = new Button(parent, SWT.PUSH);
        insertButton.setText(Messages.insertButtonText);
        insertButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                int positionbuffer = textfield.getCaretPosition();
                String word = placeholderCombo.getText();
                textfield.insert(word);
                if (textfield.getText().length() >= (positionbuffer + word.length())) {
                    textfield.setSelection(positionbuffer + word.length());
                }
                textfield.forceFocus();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {

            }
        });
        return insertButton;
    }
    
    
    /**
     * Create an "insert" button for a given placeholder combo.
     * Additional method for StyledText fields.
     * 
     * @param parent The parent composite
     * @param placeholderCombo The placeholder selection combo
     * @param textfield The textfield into which the placeholders are inserted
     * @return button to insert placeholders
     */
    public  static Button createPlaceholderInsertButton(Composite parent, final Combo placeholderCombo, final StyledText textfield) {
        Button insertButton = new Button(parent, SWT.PUSH);
        insertButton.setText(Messages.insertButtonText);
        insertButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                int positionbuffer = textfield.getCaretOffset();
                String word = placeholderCombo.getText();
                textfield.insert(word);
                if (textfield.getText().length() >= (positionbuffer + word.length())) {
                    textfield.setSelection(positionbuffer + word.length());
                }
                textfield.forceFocus();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {

            }
        });
        return insertButton;
    }
}
