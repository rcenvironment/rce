/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.utils.common.configuration;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.PropertyDescriptor;

/**
 * A {@link PropertyDescriptor} representing a property to which only a set of values can be
 * applied, which shall be represented through a selection.
 * 
 * @author Christian Weiss
 */
public class SelectionPropertyDescriptor extends PropertyDescriptor {

    /** Index Value pointing to 'no such element'. */
    private static final int NO_SUCH_VALUE_INDEX = -1;

    /**
     * The interface to be implemented by value providers of the selection values.
     * 
     * @author Christian Weiss
     */
    public interface ValueProvider {

        /**
         * Returns the values.
         * 
         * @return the values
         */
        Object[] getValues();

    }

    /** The value provider. */
    private final ValueProvider valueProvider;

    /** The labels. */
    private List<String> labels;

    /** The values. */
    private List<Object> values;

    /**
     * Instantiates a new {@link SelectionPropertyDescriptor}.
     * 
     * @param id the id
     * @param displayName the display name
     * @param selectionProvider the selection provider
     */
    public SelectionPropertyDescriptor(final Object id,
            final String displayName, final ValueProvider valueProvider) {
        super(id, displayName);
        this.valueProvider = valueProvider;
    }

    /**
     * Creates the property editor.
     * 
     * @param parent the parent
     * @return the cell editor
     */
    @Override
    public CellEditor createPropertyEditor(Composite parent) {
        refresh();
        CellEditor editor = new ComboBoxCellEditor(parent,
                labels.toArray(new String[0]), SWT.READ_ONLY) {

            @Override
            protected void doSetValue(final Object value) {
                if (!(value instanceof Integer)) {
                    super.doSetValue(getIndex(value));
                } else {
                    super.doSetValue(value);
                }
            }

        };
        editor.setValidator(new ICellEditorValidator() {

            @Override
            public String isValid(final Object value) {
                final List<Object> valuesRef = getValues();
                if (valuesRef.contains(value)) {
                    return null;
                }
                if (value instanceof Integer) {
                    final int index = (Integer) value;
                    if (index >= NO_SUCH_VALUE_INDEX && index < valuesRef.size()) {
                        return null;
                    }
                }
                return "invalid value";
            }

        });
        if (getValidator() != null) {
            editor.setValidator(getValidator());
        }
        return editor;
    }

    /**
     * Returns the labels.
     * 
     * @return the labels
     */
    private List<String> getLabels() {
        if (labels == null) {
            refresh();
        }
        return labels;
    }

    /**
     * Returns the label of the given value.
     * 
     * @param value the value
     * @return the label
     */
    public String getLabel(final Object value) {
        final int index = getIndex(value);
        if (index < 0) {
            return null;
        }
        return getLabels().get(index);
    }

    /**
     * Returns the index of the given value.
     * 
     * @param value the value
     * @return the index
     */
    public Integer getIndex(final Object value) {
        final int index = getValues().indexOf(value);
        if (index < 0) {
            return null;
        }
        return index;
    }

    /**
     * Returns the values.
     * 
     * @return the values
     */
    private List<Object> getValues() {
        if (values == null) {
            refresh();
        }
        return values;
    }

    /**
     * Returns the value at the given index.
     * 
     * @param index the index
     * @return the value
     */
    public Object getValue(final Integer index) {
        if (index == NO_SUCH_VALUE_INDEX) {
            return null;
        }
        return values.get(index);
    }

    /**
     * Refresh.
     */
    private void refresh() {
        if (labels == null || values == null) {
            labels = new ArrayList<String>();
            values = new ArrayList<Object>();
        }
        labels.clear();
        values.clear();
        for (final Object value : valueProvider.getValues()) {
            labels.add(getLabelProvider().getText(value));
            values.add(value);
        }
    }

    /**
     * Returns the {@link ILabelProvider}.
     * 
     * @return the {@link ILabelProvider}
     */
    @Override
    public ILabelProvider getLabelProvider() {
        final ILabelProvider labelProvider = super.getLabelProvider();
        return new LabelProvider() {

            @Override
            public Image getImage(final Object element) {
                return labelProvider.getImage(lookup(element));
            }

            @Override
            public String getText(final Object element) {
                return labelProvider.getText(lookup(element));
            }

        };
    }

    private Object lookup(final Object element) {
        // if the element is UNknown and an instance of integer it might be the index to
        // the values list, in that case check the index range and return the element at
        // that index
        final List<Object> valuesRef = getValues();
        if (!valuesRef.contains(element) && element instanceof Integer) {
            final int index = (Integer) element;
            if (index >= 0 && index < valuesRef.size()) {
                return valuesRef.get(index);
            }
        }
        return element;
    }

}
