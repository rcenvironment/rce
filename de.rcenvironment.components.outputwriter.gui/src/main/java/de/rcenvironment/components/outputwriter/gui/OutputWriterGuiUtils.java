/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.components.outputwriter.common.OutputWriterComponentConstants;

/**
 * Provides GUI elements needed by the OutputWriterEditDialog as well as the OutputLocationEditDialog.
 *
 * @author Brigitte Boden
 * 
 */
public final class OutputWriterGuiUtils {

    private static final String LINE_SEP = System.getProperty("line.separator");

    private OutputWriterGuiUtils() {}

    /**
     * Create a combo for selecting placeholders.
     * 
     * @param parent The parent composite
     * @param placeholders Array of possible placeholders
     * @return combo to select placeholders
     */
    public static CCombo createPlaceholderCombo(Composite parent, String[] placeholders) {
        final CCombo placeholderCombo = new CCombo(parent, SWT.READ_ONLY | SWT.BORDER);
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
    public static Button createPlaceholderInsertButton(Composite parent, final CCombo placeholderCombo, final Text textfield) {
        Button insertButton = new Button(parent, SWT.PUSH);
        insertButton.setText(Messages.insertButtonText);
        insertButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                int positionbuffer = textfield.getCaretPosition();
                String word = escapeSquaredBrackets(placeholderCombo.getText());
                if (word.equals(OutputWriterComponentConstants.PH_LINEBREAK)) {
                    word = word + LINE_SEP;
                }
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
     * Create an "insert" button for a given placeholder combo. Additional method for StyledText fields.
     * 
     * @param parent The parent composite
     * @param placeholderCombo The placeholder selection combo
     * @param textfield The textfield into which the placeholders are inserted
     * @return button to insert placeholders
     */
    public static Button createPlaceholderInsertButton(Composite parent, final CCombo placeholderCombo, final StyledText textfield) {
        Button insertButton = new Button(parent, SWT.PUSH);
        insertButton.setText(Messages.insertButtonText);
        insertButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                int positionbuffer = textfield.getCaretOffset();
                String word = escapeSquaredBrackets(placeholderCombo.getText());
                if (word.equals(OutputWriterComponentConstants.PH_LINEBREAK)) {
                    word = word + LINE_SEP;
                }
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

    private static String escapeSquaredBrackets(String text) {
        final StringBuilder resultBuilder = new StringBuilder("[");
        // We consciously omit the square brackets around the given text, as they should not be escaped
        for (int index = 1; index < text.length() - 1; ++index) {
            final char currentChar = text.charAt(index);
            switch (currentChar) {
            case '[':
                resultBuilder.append("\\[");
                break;
            case ']':
                resultBuilder.append("\\]");
                break;
            case '\\':
                resultBuilder.append("\\\\");
                break;
            default:
                resultBuilder.append(currentChar);
                break;
            }
        }
        resultBuilder.append("]");
        return resultBuilder.toString();
    }
}
