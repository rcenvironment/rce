/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.cpacs.gui.xpathchooser;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import de.rcenvironment.cpacs.gui.xpathchooser.model.XSDAttribute;
import de.rcenvironment.cpacs.gui.xpathchooser.model.XSDElement;
import de.rcenvironment.cpacs.gui.xpathchooser.model.XSDValue;

/**
 * LabelProvider for the XSD TreeView.
 * 
 * @author Heinrich Wendel
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public final class XSDLabelProvider extends LabelProvider implements ITableLabelProvider {

    /**
     * XML element.
     */
    private final Image imageElement;

    /**
     * XML elements.
     */
    private final Image imageElements;

    /**
     * XML attribute.
     */
    private final Image imageAttribute;

    /**
     * XML value.
     */
    private final Image imageValue;

    /**
     * Constructor.
     * 
     * @param display The parent display to register images in.
     */
    public XSDLabelProvider(final Display display) {
        // if started manually (test-mode), we can't use resource stream for unknown reason
        if (getClass().getResourceAsStream("/resources/images/element.gif") == null) {
            imageElement = new Image(display, "./resources/images/element.gif");
            imageElements = new Image(display, "./resources/images/elements.gif");
            imageAttribute = new Image(display, "./resources/images/attribute.gif");
            imageValue = new Image(display, "./resources/images/value.gif");
        } else { // bundle-mode
            imageElement = new Image(display, getClass().getResourceAsStream("/resources/images/element.gif"));
            imageElements = new Image(display, getClass().getResourceAsStream("/resources/images/elements.gif"));
            imageAttribute = new Image(display, getClass().getResourceAsStream("/resources/images/attribute.gif"));
            imageValue = new Image(display, getClass().getResourceAsStream("/resources/images/value.gif"));
        }
    }

    /**
     * Return the text for the given XSD entry and column.
     * 
     * @param element The XSD element to show
     * @param column The column
     * @return The item text
     */
    @Override
    public String getColumnText(final Object element, final int column) {
        if ((element == null) || !(element instanceof XSDElement)) {
            return null;
        }
        final XSDElement e = (XSDElement) element;
        final String string;
        if (column == 0) {
            string = e.getName();
        } else if (column == 1) {
            if (e.getCurrentAttributeName() == null) { // if already initialized
                addElementAttributes(e); // otherwise initialize
            }
            string = e.getCurrentAttributeName();
        } else if (column == 2) {
            if (e.getCurrentAttributeValue() == null) {
                addElementAttributes(e);
            }
            string = e.getCurrentAttributeValue();
        } else {
            string = "";
        }
        return string;
    }

    /**
     * Return the image for the given XSD enty and column.
     * 
     * @param element The XSD element to show
     * @param column The column
     * @return The image
     */
    @Override
    public Image getColumnImage(final Object element, final int column) {
        if ((element == null) || !(element instanceof XSDElement)) {
            return null;
        }
        final XSDElement e = (XSDElement) element;
        final Image image;
        if (column == 0) {
            if (e.getElements().size() > 0) {
                image = imageElements;
            } else {
                image = imageElement;
            }
        } else if (column == 1) {
            if (e.getCurrentAttributeName() == null) {
                addElementAttributes(e);
            }
            if (e.getCurrentAttributeName().equals("")) {
                image = null;
            } else {
                image = imageAttribute;
            }
        } else if (column == 2) {
            if (e.getCurrentAttributeValue() == null) {
                addElementAttributes(e);
            }
            if (e.getCurrentAttributeValue().equals("")) {
                image = null;
            } else {
                image = imageValue;
            }
        } else {
            image = null;
        }
        return image;
    }

    /**
     * Lazily create meta data.
     * 
     * @param e The element to process.
     */
    private void addElementAttributes(final XSDElement e) {
        final Set<String> values = new HashSet<String>();
        for (final XSDAttribute a : e.getAttributes()) {
            if (!a.getName().equals("")) { // only non-empty strings
                for (final XSDValue value : a.getValues()) {
                    values.add(value.getName());
                }
                e.setAttributeValues(a.getName(), values);
            }
            values.clear();
        }
        // set initial value
        final String[] names = e.getAttributeNames();
        if (names.length == 0) {
            e.setAttributeName("");
            e.setAttributeValue("");
        } else {
            e.setAttributeName(names[0]);
            final String[] tmp = e.getAttributeValues();
            if (tmp.length == 0) {
                e.setAttributeValue("");
            } else {
                e.setAttributeValue(tmp[0]);
            }
        }
    }

}
