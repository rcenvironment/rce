/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.utils.common.configuration;

import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.properties.ComboBoxPropertyDescriptor;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * A {@link org.eclipse.ui.views.properties.IPropertyDescriptor} representing a property with a
 * certain set of valid values.
 * 
 * @author Christian Weiss
 */
@Deprecated
public class ListPropertyDescriptor extends ComboBoxPropertyDescriptor {

    /**
     * 
     */
    private static final int NO_SUCH_VALUE_INDEX = -1;

    /** The labels. */
    private final String[] labels;

    /** The value. */
    private final Object[] values;

    /**
     * Instantiates a new list property descriptor.
     * 
     * @param id the id
     * @param displayName the display name
     * @param labels the labels
     */
    public ListPropertyDescriptor(final Object id, final String displayName,
            final String[] labels) {
        this(id, displayName, labels, new Object[labels.length]);
    }

    /**
     * Instantiates a new list property descriptor.
     * 
     * @param id the id
     * @param displayName the display name
     * @param labels the labels
     * @param values the values
     */
    public ListPropertyDescriptor(final Object id, final String displayName,
            final String[] labels, final Object[] values) {
        super(id, displayName, labels);
        this.labels = labels;
        this.values = values;
        // ensure, that labels and data map
        if (labels.length != values.length) {
            throw new IllegalArgumentException(
                    "wrong number of value objects provided");
        }
        // ensure, that labels are all non-null
        for (final String label : labels) {
            if (label == null) {
                throw new IllegalArgumentException(
                        "null labels are not allowed");
            }
        }
        // only accept labels as values
        setValidator(new ICellEditorValidator() {

            @Override
            public String isValid(final Object value) {
                String result = null;
                if (value instanceof Number) {
                    if (!isValidIndex(((Number) value).intValue())) {
                        result = "Not a valid index.";
                    }
                } else if (value instanceof String) {
                    if (!isValidLabel((String) value)) {
                        result = "Not a valid selection.";
                    }
                }
                return result;
            }

            private boolean isValidIndex(final int index) {
                return index >= 0 && index < labels.length;
            }

            private boolean isValidLabel(final Object value) {
                for (final String label : labels) {
                    if (label.equals(value)) {
                        return true;
                    }
                }
                return false;
            }

        });
    }

    /**
     * Returns the index of the given label.
     * 
     * @param label the label
     * @return the index
     */
    public int indexOfLabel(final String label) {
        return indexOfLabel(label, 0);
    }

    /**
     * Returns the index of the given label from the given start position.
     * 
     * @param label the label
     * @param start the start position
     * @return the index
     */
    public int indexOfLabel(final String label, int start) {
        for (int index = start; index < labels.length; ++index) {
            if (labels[index].equals(label)) {
                return index;
            }
        }
        return NO_SUCH_VALUE_INDEX;
    }

    /**
     * Returns the index of the given value.
     * 
     * @param value the value
     * @return the index
     */
    public int indexOfData(final Object value) {
        return indexOfData(value, 0);
    }

    /**
     * Returns the index of the given value from the given start position.
     * 
     * @param value the value
     * @param start the start position
     * @return the index
     */
    public int indexOfData(final Object value, int start) {
        for (int index = start; index < labels.length; ++index) {
            if ((values[index] == null && value == null)
                    || (values[index] != null && values[index].equals(value))) {
                return index;
            }
        }
        return NO_SUCH_VALUE_INDEX;
    }

    /**
     * Checks whether the given index is within the range of the selection.
     * 
     * @param index the index
     */
    private void checkRange(final int index) {
        if (index < 0 || index >= labels.length) {
            throw new IllegalArgumentException("invalid label index: " + index
                    + " [0," + (labels.length - 1) + "]");
        }
    }

    /**
     * Returns the label at the given index.
     * 
     * @param index the index
     * @return the label
     */
    public String getLabel(final int index) {
        checkRange(index);
        return labels[index];
    }

    /**
     * Return the value at the given index.
     * 
     * @param index the index
     * @return the data
     */
    public Object getValue(final int index) {
        checkRange(index);
        return values[index];
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return StringUtils.format("%s: (%d) %s", getClass().getSimpleName(),
                labels.length, labels);
    }

    /**
     * Returns the label provider.
     * 
     * @return the label provider
     */
    @Override
    public ILabelProvider getLabelProvider() {
        if (isLabelProviderSet()) {
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

                private Object lookup(final Object element) {
                    // if the element is known return itself
                    for (final Object value : values) {
                        if (value == element || (value != null && value.equals(element))) {
                            return element;
                        }
                    }
                    // if the element is an instance of integer it might be the index to
                    // the values list, in that case check the index range and return the element at
                    // that index
                    if (element instanceof Integer) {
                        final int index = (Integer) element;
                        if (index >= 0 && index < values.length) {
                            return values[index];
                        }
                    }
                    return element;
                }

            };
        }
        return super.getLabelProvider();
    }

}
