/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.xpathchooser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.xpathchooser.model.XSDAttribute;
import de.rcenvironment.core.gui.xpathchooser.model.XSDElement;
import de.rcenvironment.core.gui.xpathchooser.model.XSDValue;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * LabelProvider for the XSD TreeView.
 * 
 * @author Heinrich Wendel
 * @author Arne Bachmann
 * @author Markus Kunde
 * @author Jan Flink
 */
public final class XSDLabelProvider extends LabelProvider implements ITableLabelProvider {

    private static final String COMBO_HINT = "%s  ...";

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
     * XML attributes.
     */
    private Image imageAttributes;

    /**
     * XML attribute value.
     */
    private final Image imageValue;

    /**
     * XML attribute values.
     */
    private final Image imageValues;

    private XPathChooserHelper helper;

    /**
     * Constructor.
     * 
     * @param display The parent display to register images in.
     */
    public XSDLabelProvider(final XPathChooserHelper aHelper) {
        this.helper = aHelper;
        imageElement = ImageManager.getInstance().getSharedImage(StandardImages.ELEMENT);
        imageElements = ImageManager.getInstance().getSharedImage(StandardImages.ELEMENTS);
        imageAttribute = ImageManager.getInstance().getSharedImage(StandardImages.ATTRIBUTE);
        imageAttributes = ImageManager.getInstance().getSharedImage(StandardImages.ATTRIBUTES);
        imageValue = ImageManager.getInstance().getSharedImage(StandardImages.COMMON_TEXT_16);
        imageValues = ImageManager.getInstance().getSharedImage(StandardImages.COMMON_TEXT_NODES_16);
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
        String string;
        if ((column == 1 || column == 2) && e.getCurrentAttributeName() == null) { // if already initialized
            addElementAttributes(e); // otherwise initialize
        }
        switch (column) {
        case 0:
            return e.getName();
        case 1:
            string = e.getCurrentAttributeName();
            break;
        case 2:
            string = e.getCurrentAttributeValue();
            break;
        default:
            return "";
        }
        if (helper.getAttributeValuesForCurrentTreeItem(e, column).length > 1) {
            string = StringUtils.format(COMBO_HINT, string);
        }
        return string;
    }

    /**
     * Return the image for the given XSD entry and column.
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
        if (column == 1 || column == 2) {
            if (e.getCurrentAttributeName() == null) {
                addElementAttributes(e);
            }
            if (e.getCurrentAttributeName().equals("")) {
                return null;
            }
        }
        switch (column) {
        case 0:
            if (!e.getElements().isEmpty()) {
                return imageElements;
            } else {
                return imageElement;
            }
        case 1:
            if (helper.getAttributeValuesForCurrentTreeItem(e, column).length > 1) {
                return imageAttributes;
            }
            return imageAttribute;
        case 2:
            if (helper.getAttributeValuesForCurrentTreeItem(e, column).length > 1) {
                return imageValues;
            }
            return imageValue;
        default:
            return null;
        }
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
            Arrays.sort(names);
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
