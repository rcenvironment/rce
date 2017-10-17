/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.utils.incubator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * 
 * Factory for creating several widgets in one step for given composites.
 * 
 * @author Sascha Zur
 * @author Jascha Riedel (#14005)
 */
public final class WidgetGroupFactory {

    /**
     * Option if there shall be no restriction.
     */
    public static final int NONE = 0;

    /**
     * Option if a textfield should only allow float inputs (e.g. no letters).
     */
    public static final int ONLY_FLOAT = 1;

    /**
     * Option if a textfield should only allow integer inputs (e.g. no letters).
     */
    public static final int ONLY_INTEGER = 2;

    /**
     * Option if a textfield should only allow inputs >= 0.
     */
    public static final int GREATER_OR_EQUAL_ZERO = 4;
    
    /**
     * Option if a textfield should only allow inputs > 0.
     */
    public static final int GREATER_ZERO = 16;
    
    /**
     * Option if a textfield should align the text in the (standard is left).
     */
    public static final int ALIGN_CENTER = 16777216;

    private static final String CONTROL_PROPERTY_KEY = "property.control";

    private WidgetGroupFactory() {

    }

    /**
     * Creates at first a new {@link Label} and then a new {@link Text } field in the given
     * composite. The Text will be linked with the given property and have the given width.
     * 
     * @param composite in which the widgets will be created in
     * @param propertyMessage message for the label
     * @param property which will be linked in the Text widget
     * @param textWidth for the Text widget
     * @param function some options for the text field, see constants in {@link WidgetGroupFactory}
     * @return the generated text widget for further use (e.g. adding listener)
     */
    public static LabelAndTextForProperty addLabelAndTextfieldForPropertyToComposite(Composite composite,
        String propertyMessage, String property, int textWidth, final int function) {
        Label propertyLabel = new Label(composite, SWT.NONE);
        propertyLabel.setText(propertyMessage);
        final Text propertyText = new Text(composite, SWT.BORDER | (SWT.CENTER & function));
        propertyText.setData(CONTROL_PROPERTY_KEY, property);
        GridData gridData = new GridData();
        gridData.widthHint = textWidth;
        propertyText.setLayoutData(gridData);
        if (function != WidgetGroupFactory.NONE) {
            propertyText.addVerifyListener(new NumericalTextConstraintListener(propertyText, function));
        }

        LabelAndTextForProperty labelAndText = new LabelAndTextForProperty();
        labelAndText.text = propertyText;
        labelAndText.label = propertyLabel;
        return labelAndText;
    }

    /**
     * Container for {@link Text} and {@link Label}.
     * 
     * @author Doreen Seider
     */
    public static class LabelAndTextForProperty {

        /**
         * Text field for property.
         */
        public Text text = null;

        /**
         * Label for property.
         */
        public Label label = null;

    }
}
