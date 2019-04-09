/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.xpathchooser;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.core.utils.common.variables.legacy.VariableType;


/**
 * Provides the editor for the variable table (name and xpath).
 * 
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public class VariableEditingSupport extends EditingSupport {
    
    /**
     * Allowed values for the type column.
     */
    private static final String[] VALUES = new String[] {
            VariableType.Integer.toString(),
            VariableType.Logic.toString(),
            VariableType.Real.toString(),
            VariableType.String.toString()
    };
    
    /**
     * Reference to the calling class.
     */
    private final XPathChooserHelper helper;
    
    /**
     * The table.
     */
    private final Table table;
    
    /**
     * The column (1 = attribute name, 2 = attribute value).
     */
    private final int column;
    
    
    /**
     * Constructor.
     * @param parent The creating class
     * @param viewer The tree column viewer
     * @param aColumn The column number (1..2)
     */
    public VariableEditingSupport(final XPathChooserHelper parent, final ColumnViewer viewer, final int aColumn) {
        super(viewer);
        assert parent != null;
        assert viewer != null;
        assert aColumn > 0;
        this.helper = parent;
        this.column = aColumn;
        table = ((TableViewer) viewer).getTable();
    }

    
    @Override
    protected boolean canEdit(final Object element) {
        assert element instanceof VariableEntry;
        return (element != null) && (column >= 1);
    }

    @Override
    protected CellEditor getCellEditor(final Object element) {
        assert element instanceof VariableEntry;
        assert column >= 1;
        final VariableEntry variable = (VariableEntry) element;
        helper.updateTreeAttributesFromTableEntry(variable);
        final CellEditor editor;
        if (column == 1) {
            editor = new TextCellEditor(table);
            final Text control = ((Text) editor.getControl());
            control.setText(variable.getName());
            control.setEditable(true);
            control.selectAll();
            control.setFocus();
        } else if (column == 2) {
            editor = new TextCellEditor(table);
            final Text control = ((Text) editor.getControl());
            control.setText(variable.getXpath());
            control.setEditable(true);
            control.selectAll();
            control.setFocus();
        } else if (column == 3) {
            editor = new ComboBoxCellEditor(table, VALUES);
            final CCombo control = ((CCombo) editor.getControl());
            control.setText(variable.getType().toString());
            control.setListVisible(true);
            control.setEditable(true);
            control.setFocus();
        } else {
            editor = null;
        }
        return editor;
    }

    @Override
    protected Object getValue(final Object element) {
        assert element instanceof VariableEntry;
        assert column >= 1;
        final VariableEntry variable = (VariableEntry) element;
        if (column == 1) {
            return variable.getName();
        } else if (column == 2) {
            return variable.getXpath();
        } else if (column == 3) {
            int x = 0;
            for (int i = 0; i < VALUES.length; i ++) {
                if (VALUES[i].equals(variable.getType().toString())) {
                    x = i;
                    break;
                }
            }
            return x; // fallback
        }
        throw new RuntimeException("This should never happen! Missing column?");
    }

    @Override
    protected void setValue(final Object element, final Object value) {}

}
