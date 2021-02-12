/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.utils.common.configuration;

import org.eclipse.jface.viewers.LabelProvider;

/**
 * A {@link org.eclipse.ui.views.properties.IPropertyDescriptor} representing an
 * <code>boolean</code> valued property.
 * 
 * @author Christian Weiss
 */
public class BooleanPropertyDescriptor extends SelectionPropertyDescriptor {

    private static final String[] LABELS = new String[] { Messages.trueLabel, Messages.falseLabel };

    private static final Boolean[] DATA = new Boolean[] { Boolean.TRUE, Boolean.FALSE };

    public BooleanPropertyDescriptor(Object id, String displayName) {
        super(id, displayName, new ValueProvider() {

            @Override
            public Object[] getValues() {
                return DATA;
            }

        });
        setLabelProvider(new LabelProvider() {

            @Override
            public String getText(final Object element) {
                for (int index = 0; index < DATA.length; ++index) {
                    if (DATA[index].equals(element)) {
                        return LABELS[index];
                    }
                }
                if (element == null) {
                    return null;
                }
                throw new IllegalArgumentException();
            }

        });
    }

}
