/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.cpacs.vampzeroinitializer.gui;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

/**
 * Helper methods to create widgets.
 * 
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public class WidgetHelper {

    /**
     * Form toolkit.
     */
    private FormToolkit tk;

    /**
     * Some color.
     */
    private Color backgroundColor;

    /**
     * Constructor.
     * 
     * @param formToolkit The eclipse forms toolkit
     * @param bgColor Background color of text fields
     */
    public WidgetHelper(final FormToolkit formToolkit, final Color bgColor) {
        tk = formToolkit;
        backgroundColor = bgColor;
    }

    /**
     * Helper to create labels.
     * 
     * @param parent The parent widget
     * @param labelString The label string
     * @param columns The colspan
     * @return The created label
     */
    public Label newLabel(final Composite parent, final String labelString, final int... columns) {
        final TableWrapData td = new TableWrapData();
        td.align = TableWrapData.RIGHT;
        if ((columns != null) && (columns.length > 0)) {
            td.colspan = columns[0];
        }
        final Label label = tk.createLabel(parent, labelString);
        label.setFont(JFaceResources.getBannerFont());
        label.setLayoutData(td);
        return label;
    }

    /**
     * Helper component to add a button.
     * 
     * @param parent The parent widget
     * @param label The button label
     * @param listener The listener to execute when button is clicked
     * @param alignment TableWrapData.x
     * @return The created button
     */
    public Button newButton(final Composite parent, final String label, final Listener listener, final int... alignment) {
        final TableWrapData td = new TableWrapData();
        if ((alignment != null) && (alignment.length > 0)) {
            td.align = alignment[0];
        }
        final Button button = tk.createButton(parent, label, SWT.PUSH | SWT.FLAT);
        button.setLayoutData(td);
        button.addListener(SWT.Selection, listener);
        return button;
    }

    /**
     * Helper to create text fields.
     * 
     * @param parent The parent widget
     * @param initialContent The contained text
     * @param colSpan The number of cols to span or nothing for 1
     * @return The field
     */
    public Text newText(final Composite parent, final String initialContent, final int... colSpan) {
        final Text text;
        if (initialContent.contains("\n")) {
            text = tk.createText(parent, initialContent, SWT.BORDER | SWT.MULTI | SWT.WRAP);
        } else {
            text = tk.createText(parent, initialContent, SWT.BORDER | 0);
        }
        text.setBackground(backgroundColor);
        final TableWrapData td = new TableWrapData();
        td.align = TableWrapData.FILL;
        if ((colSpan == null) || (colSpan.length == 0)) {
            td.colspan = 1;
        } else {
            td.colspan = colSpan[0];
        }
        text.setSelection(0, text.getText().length());
        text.setLayoutData(td);
        return text;
    }

    /**
     * Create a list widget.
     * 
     * @param parent The parent widget
     * @param selectionListener The listener to perform when an item is selected
     * @param style The style, default is SWT.SINGLE and SWT.BORDER, operation is XOR
     * @return The created list
     */
    public List newList(final Composite parent, final Listener selectionListener, final int... style) {
        final List list;
        if ((style == null) || (style.length < 1)) {
            list = new List(parent, SWT.SINGLE | SWT.BORDER);
        } else {
            list = new List(parent, style[0] ^ (SWT.SINGLE | SWT.BORDER));
        }
        list.setBackground(backgroundColor);
        if (selectionListener != null) {
            list.addListener(SWT.Selection, selectionListener);
        }
        final TableWrapData td = new TableWrapData();
        td.align = TableWrapData.FILL;
        list.setLayoutData(td);
        return list;
    }

    /**
     * Helper to create a radio button.
     * 
     * @param parent The parent widget to append to
     * @param label The label beside the radio button
     * @param style The widget style, e.g. SWT.BORDER
     * @return The created button
     */
    public Button newRadioButton(final Composite parent, final String label, final int... style) {
        final Button radioButton;
        if ((style == null) || (style.length < 1)) {
            radioButton = tk.createButton(parent, label, SWT.RADIO);
        } else {
            radioButton = tk.createButton(parent, label, SWT.RADIO ^ style[0]);
        }
        radioButton.setLayoutData(new TableWrapData());
        return radioButton;
    }

    /**
     * Helper to create a radio button.
     * 
     * @param parent The parent widget to append to
     * @param label The label beside the check box
     * @param columns The col span value
     * @return The created check box
     */
    public Button newCheckbox(final Composite parent, final String label, final int... columns) {
        final Button checkbox = tk.createButton(parent, label, SWT.CHECK);
        final TableWrapData td = new TableWrapData();
        if ((columns != null) && (columns.length > 0)) {
            td.colspan = columns[0];
        }
        checkbox.setLayoutData(td);
        return checkbox;
    }

    /**
     * Create a slider widget.
     * 
     * @param parent The parent composite
     * @param maxValue Maximum allowed value +/-
     * @param listener The listener to call when the value is modified
     * @param style The style to use e.g. SWT.BORDER or SWT.VERTICAL, otherwise NULL
     * @return The slider created
     */
    public Scale newScale(final Composite parent, final int maxValue,
        final Listener listener, final int... style) {
        final Scale scale;
        if ((style != null) && (style.length > 0)) {
            scale = new Scale(parent, (SWT.NULL | SWT.HORIZONTAL) ^ style[0]);
        } else {
            scale = new Scale(parent, SWT.NULL | SWT.HORIZONTAL);
        }
        scale.setMinimum(0);
        scale.setMaximum(2 * maxValue);
        scale.setIncrement(1);
        scale.setPageIncrement(3);
        scale.setSelection(maxValue);
        scale.setLayoutData(new TableWrapData());
        if (listener != null) {
            scale.addListener(SWT.Selection, listener);
        }
        return scale;
    }

    /**
     * Create a separator label.
     * 
     * @param parent The composite
     * @param vertical True if vertical, false if horizontal
     * @param columns The columns to span
     * @return The separator element
     */
    public Label newSeparator(final Composite parent, final boolean vertical, final int... columns) {
        final Label label;
        if (vertical) {
            label = new Label(parent, SWT.SEPARATOR | SWT.VERTICAL);
        } else {
            label = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        }
        final TableWrapData td = new TableWrapData();
        if ((columns != null) && (columns.length > 0)) {
            td.colspan = columns[0];
        }
        td.align = TableWrapData.FILL;
        label.setLayoutData(td);
        return label;
    }

    /**
     * Helper to create a radio button group.
     * 
     * @param parent The parent widget to append to
     * @param layout The composite's layout
     * @param columns The number of columns to span
     * @return The created button group
     */
    public Composite newButtonGroup(final Composite parent, final Layout layout, final int... columns) {
        final Composite group = tk.createComposite(parent);
        final TableWrapData td = new TableWrapData();
        if ((columns != null) && (columns.length > 0)) {
            td.colspan = columns[0];
        }
        group.setLayoutData(td);
        group.setLayout(layout);
        return group;
    }

    /**
     * A new combo.
     * 
     * @param parent The parent composite
     * @param values Values to choose from
     * @param listener Called when a value is selected
     * @return The created combo
     */
    public Combo newReadonlyCombo(final Composite parent, final String[] values, final Listener listener) {
        final Combo combo = new Combo(parent, SWT.READ_ONLY | SWT.SINGLE);
        if (values != null) {
            combo.setItems(values);
        }
        final TableWrapData td = new TableWrapData();
        td.align = TableWrapData.FILL;
        combo.setLayoutData(td);
        if (listener != null) {
            combo.addListener(SWT.Selection, listener);
        }
        return combo;
    }

    /**
     * Create a group.
     * 
     * @param parent The parent composite
     * @param title The group text or none if empty or null
     * @param style The border style
     * @param internalColumns Number of columns inside the group layout
     * @param columns The number of columns to span
     * @return The group just created
     */
    public Group newGroup(final Composite parent, final String title, final int style, final int internalColumns, final int... columns) {
        final Group group = new Group(parent, SWT.NONE);
        final TableWrapData td = new TableWrapData();
        if ((columns != null) && (columns.length > 0)) {
            td.colspan = columns[0];
        }
        td.align = TableWrapData.FILL;
        td.grabHorizontal = true;
        if ((title != null) && (!title.equals(""))) {
            group.setText(title);
        }
        group.setLayoutData(td);
        final Layout designGroupLayout = newDefaultLayout(internalColumns);
        group.setLayout(designGroupLayout);
        tk.adapt(group); // adapt to JFace
        return group;
    }

    /**
     * Create but don't set a layout manager.
     * 
     * @param parent The parent component
     * @return The created widget
     */
    public Composite newComposite(final Composite parent) {
        final Composite composite = tk.createComposite(parent);
        return composite;
    }

    /**
     * Create a new composite, again with a tablewrap layout.
     * 
     * @param parent The parent composite
     * @param internalColumns number internal columns
     * @param columns The number of columns or 1
     * @return The created widget
     */
    public Composite newComposite(final Composite parent, final int internalColumns, final int... columns) {
        final Composite composite = newComposite(parent);
        final TableWrapData td = new TableWrapData();
        td.align = TableWrapData.FILL;
        if ((columns != null) && (columns.length > 0)) {
            td.colspan = columns[0];
        }
        composite.setLayoutData(td);
        final Layout layout = newDefaultLayout(internalColumns);
        composite.setLayout(layout);
        return composite;
    }

    /**
     * Reusable code for the sub layout definition.
     * 
     * @param internalColumns Number of internal columns
     * @return The created layout
     */
    public Layout newDefaultLayout(final int internalColumns) {
        final TableWrapLayout layout = new TableWrapLayout();
        layout.numColumns = internalColumns;
        layout.horizontalSpacing = 5;
        layout.verticalSpacing = 5;
        return layout;
    }

}
