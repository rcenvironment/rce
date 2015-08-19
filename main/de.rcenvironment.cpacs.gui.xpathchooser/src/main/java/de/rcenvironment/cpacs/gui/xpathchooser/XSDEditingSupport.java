/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.cpacs.gui.xpathchooser;

import java.lang.reflect.Field;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;

import de.rcenvironment.cpacs.gui.xpathchooser.model.XSDElement;

/**
 * Provides the editor for the attributes and values.
 * 
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public class XSDEditingSupport extends EditingSupport {

    /**
     * Index constant.
     */
    private static final int NOT_FOUND = -1;

    /**
     * The column (1 = attribute name, 2 = attribute value).
     */
    private final int column;

    /**
     * The helper.
     */
    private final XPathChooserHelper helper;

    /**
     * The tree.
     */
    private final Tree tree;

    /**
     * The editor.
     */
    private ComboBoxCellEditor editor;

    /**
     * Constructor.
     * 
     * @param aHelper The main class
     * @param viewer The tree column viewer
     * @param aColumn The column number (1..2)
     */
    public XSDEditingSupport(final XPathChooserHelper aHelper, final ColumnViewer viewer, final int aColumn) {
        super(viewer);
        assert aHelper != null;
        assert viewer != null;
        assert aColumn > 0;
        this.column = aColumn;
        this.helper = aHelper;
        tree = ((TreeViewer) viewer).getTree();
    }

    @Override
    protected boolean canEdit(final Object element) {
        assert element instanceof XSDElement;
        return (element != null) && (column >= 1);
    }

    @Override
    protected CellEditor getCellEditor(final Object element) {
        assert element instanceof XSDElement;
        assert column >= 1;
        final XSDElement elem = (XSDElement) element;
        final String[] values = helper.getAttributeValuesForCurrentTreeItem(elem, column);
        assert values != null;
        editor = new ComboBoxCellEditor(tree, values);
        final CCombo control = ((CCombo) editor.getControl());
        control.setEditable(true);
        if (values.length >= 1) {
            final String value = XPathChooserHelper.getCurrentElementValue(elem, column);
            final int index = control.indexOf(value);
            if (index >= 0) { // preselect item
                control.select(index);
            }
        }
        editor.setFocus();
        return editor;
    }

    @Override
    protected Object getValue(final Object element) {
        assert element instanceof XSDElement;
        final XSDElement elem = (XSDElement) element;
        String value = XPathChooserHelper.getCurrentElementValue(elem, column);
        final String[] values = helper.getAttributeValuesForCurrentTreeItem(elem, column);
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) {
                return i;
            }
        }
        return NOT_FOUND;
    }

    @Override
    protected void setValue(final Object element, final Object value) {
        assert element instanceof XSDElement;
        final XSDElement elem = (XSDElement) element;
        try {
            final CCombo control = ((CCombo) editor.getControl());
            final Field textField = control.getClass().getDeclaredField("text"); // private widget
            textField.setAccessible(true);
            final Text text = (Text) textField.get(control);
            final String newValue = text.getText(); // just entered value
            XPathChooserHelper.setCurrentElementValue(elem, column, newValue);
            getViewer().update(element, null);
        } catch (final SecurityException e) {
            int i = 0;
        } catch (final NoSuchFieldException e) {
            int i = 0;
        } catch (final IllegalArgumentException e) {
            int i = 0;
        } catch (final IllegalAccessException e) {
            int i = 0;
        }
    }

}
