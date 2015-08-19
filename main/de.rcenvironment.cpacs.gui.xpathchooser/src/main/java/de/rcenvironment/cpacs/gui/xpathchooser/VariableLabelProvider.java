/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.cpacs.gui.xpathchooser;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;


/**
 * LabelProvider for the XSD TreeView.
 *
 * @author Heinrich Wendel
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public final class VariableLabelProvider extends LabelProvider implements ITableLabelProvider {
    
    
    /**
     * For XPaths.
     */
    private final Image imageFunction;
    
    /**
     * Constructor.
     * @param display The parent display to register images in.
     */
    public VariableLabelProvider(final Display display) {
        // if started manually (test-mode), we can't use resource stream for unknown reason
        if (getClass().getResourceAsStream("/resources/images/input16.png") == null) {
            imageFunction = new Image(display, "./resources/images/function12.png");
        } else { // bundle-mode
            imageFunction = new Image(display, getClass().getResourceAsStream("/resources/images/function12.png"));
        }
    }
    
    /**
     * Return the text for the given XSD entry and column.
     * @param element The XSD element to show
     * @param column The column
     * @return The item text
     */
    @Override
    public String getColumnText(final Object element, final int column) {
        if ((element == null) || !(element instanceof VariableEntry)) {
            return null;
        }
        final VariableEntry v = (VariableEntry) element;
        final String string;
        if (column == 0) {
            string = v.getXpath();
        } else {
            string = "";
        }
        return string;
    }
    
    /**
     * Return the image for the given XSD enty and column.
     * @param element The XSD element to show
     * @param column The column
     * @return The image
     */
    @Override
    public Image getColumnImage(final Object element, final int column) {
        if ((element == null) || !(element instanceof VariableEntry)) {
            return null;
        }
        final VariableEntry v = (VariableEntry) element;
        final Image image;
        if (column == 0) {
            if (v.getXpath().equals("")) {
                image = null;
            } else {
                image = imageFunction;
            }
        } else {
            image = null;
        }
        return image;
    }

}
