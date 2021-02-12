/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.xpathchooser;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;


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
    private Image imageFunction = ImageManager.getInstance().getSharedImage(StandardImages.FUNCTION);
    
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
