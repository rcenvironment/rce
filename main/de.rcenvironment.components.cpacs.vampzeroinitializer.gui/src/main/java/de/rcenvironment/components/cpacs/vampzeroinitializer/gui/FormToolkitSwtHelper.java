/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.cpacs.vampzeroinitializer.gui;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;


/**
 * Vampzero GUI SWT Factory.
 * 
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public class FormToolkitSwtHelper extends AbstractSwtHelper {

    /**
     * Color value.
     */
    private static final int WHITE = 255;
    
    /**
     * Widget creator.
     */
    private FormToolkit tk;

    /**
     * Our custom components simplification.
     */
    private WidgetHelper widgetHelper;

    /**
     * The color used mostly.
     */
    private Color whiteColor;

    /**
     * The pane.
     */
    private ScrolledForm form;

    public FormToolkitSwtHelper(final Composite parent, final String title) {
        super(parent.getDisplay());
        whiteColor = new Color(parent.getDisplay(), WHITE, WHITE, WHITE);
        tk = new FormToolkit(parent.getDisplay()); // factory to create fancy form controls and
                                                   // layout
        widgetHelper = new WidgetHelper(tk, whiteColor);
        form = tk.createScrolledForm(parent);
        form.setText(title);
        form.getBody().setLayout(new FillLayout());
    }

    @Override
    public void dispose() {
        super.dispose();
        whiteColor.dispose();
    }

    @Override
    public Composite createMainComposite() {
        return tk.createComposite(form.getBody());
    }

    @Override
    public Composite createComposite(final Composite parent) {
        return widgetHelper.newComposite(parent);
    }

    @Override
    public Composite createComposite(final Composite parent, final int columnsContained, int... columns) {
        return widgetHelper.newComposite(parent, columnsContained, columns);
    }

    @Override
    public Label createSeparator(final Composite parent, final boolean vertical, final int... columns) {
        return widgetHelper.newSeparator(parent, true, columns);
    }

    @Override
    public Text createText(final Composite parent, final String initialText, final int... colSpan) {
        return widgetHelper.newText(parent, initialText, colSpan);
    }

    @Override
    public Button createButton(final Composite parent, final String text, final Listener listener, final int... alignment) {
        return widgetHelper.newButton(parent, text, listener, alignment);
    }

    @Override
    public Label createLabel(final Composite parent, final String text, int... columns) {
        return widgetHelper.newLabel(parent, text, columns);
    }

    @Override
    public Combo createCombo(final Composite parent, final String[] initialTexts, final Listener listener) {
        return widgetHelper.newReadonlyCombo(parent, initialTexts, listener);
    }

    @Override
    public Layout createDefaultLayout(final int columnsContained) {
        return widgetHelper.newDefaultLayout(columnsContained);
    }

}
