/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.sql.gui.properties;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import de.rcenvironment.components.sql.common.ColumnType;
import de.rcenvironment.components.sql.common.InputMapping;
import de.rcenvironment.components.sql.common.InputMapping.ColumnMapping;

/**
 * Label provider for {@link ColumnMapping}s.
 * 
 * @author Christian Weiss
 */
public class InputMappingLabelProvider extends LabelProvider implements ITableLabelProvider {

    private InputMapping inputMapping;

    public void setInputMapping(final InputMapping inputMapping) {
        this.inputMapping = inputMapping;
    }

    @Override
    public Image getColumnImage(final Object element, final int columnIndex) {
        checkInputMapping();
        return null;
    }

    @Override
    public String getColumnText(final Object element, final int columnIndex) {
        checkInputMapping();
        if (!(element instanceof ColumnMapping)) {
            throw new IllegalArgumentException();
        }
        final ColumnMapping columnMapping = (ColumnMapping) element;
        final String result;
        switch (columnIndex) {
        case 0:
            final int index = columnMapping.getIndex();
            final int column = index + 1;
            result = Integer.toString(column);
            break;
        case 1:
            String name = columnMapping.getName();
            if (name == null) {
                name = "";
            }
            result = name;
            break;
        case 2:
            final ColumnType type = columnMapping.getType();
            if (type != null) {
                result = type.getLabel();
            } else {
                result = "";
            }
            break;
        default:
            throw new RuntimeException("Illegal column index.");
        }
        return result;
    }

    private void checkInputMapping() {
        if (inputMapping == null) {
            throw new IllegalStateException("No InputMapping set.");
        }
    }

}
