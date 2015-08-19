/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.cpacs.gui.xpathchooser.model;

/**
 * Simplified representation of a value an attribute or element can have.
 * 
 * @author Heinrich Wendel
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public class XSDValue extends AbstractXSDTreeItem {

    /**
     * Inheritable.
     */
    private static final long serialVersionUID = -8551127908016367260L;

    /**
     * Constructor.
     * 
     * @param parent See parent.
     * @param name See parent.
     */
    public XSDValue(XSDValueHolder parent, String name) {
        super(parent, name);
    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.cpacs.gui.xpathchooser.model.XSDTreeItem#getPath()
     */
    @Override
    public String getPath() {
        final StringBuilder path = new StringBuilder(parent.getPath());
        path.append("=\"").append(name).append("\"");
        return path.toString();
    }

}
