/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.xpathchooser;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;

import de.rcenvironment.core.gui.xpathchooser.model.XSDElement;

/**
 * Provides the editor for the attributes and values.
 * 
 * @author Arne Bachmann
 * @author Markus Kunde
 * @author Adrian Stock
 * @author Jan Flink
 */
public class XSDEditingSupport extends EditingSupport {


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
     * The treeViwer.
     */
    private final TreeViewer treeViewer;

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
        this.treeViewer = (TreeViewer) viewer;
        tree = ((TreeViewer) viewer).getTree();
    }

    @Override
    protected boolean canEdit(final Object element) {
        assert element != null;
        assert element instanceof XSDElement;
        assert column >= 1;
        return (helper.getAttributeValuesForCurrentTreeItem((XSDElement) element, column).length > 1);
    }

    @Override
    protected CellEditor getCellEditor(final Object element) {
        assert element instanceof XSDElement;
        assert column >= 1;
        final XSDElement elem = (XSDElement) element;
        final String[] values = helper.getAttributeValuesForCurrentTreeItem(elem, column);
        assert values != null;
        final String value = XPathChooserHelper.getCurrentElementValue(elem, column);
        Arrays.sort(values);
        editor = new ComboBoxCellEditor(tree, values);
        final CCombo control = ((CCombo) editor.getControl());

        control.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                control.setEnabled(false);
                setValue(element, XPathChooserHelper.getCurrentElementValue(elem, column));
                tree.forceFocus();
                helper.selectItem();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                // Nothing to do here.
            }

        });
        control.setEditable(false);
        final int index = control.indexOf(value);
        if (index >= 0) { // preselect item
            control.select(index);
        }

        editor.setFocus();
        return editor;
    }

    @Override
    protected Object getValue(final Object element) {
        assert element instanceof XSDElement;
        final XSDElement elem = (XSDElement) element;
        String value = XPathChooserHelper.getCurrentElementValue(elem, column);
        return ((CCombo) editor.getControl()).indexOf(value);
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
            if (column == 1) { 
                XPathChooserHelper.setCurrentElementValue(elem, 2, elem.getAttributeValues()[0]);
            }
            getViewer().update(element, null);
        } catch (final SecurityException e) {
            LogFactory.getLog(getClass()).debug("Catched SecurityException");
        } catch (final NoSuchFieldException e) {
            LogFactory.getLog(getClass()).debug("Catched NoSuchFieldException");
        } catch (final IllegalArgumentException e) {
            LogFactory.getLog(getClass()).debug("Catched IllegalArgumentException");
        } catch (final IllegalAccessException e) {
            LogFactory.getLog(getClass()).debug("Catched IllegalAccessException");
        }
    }

}
