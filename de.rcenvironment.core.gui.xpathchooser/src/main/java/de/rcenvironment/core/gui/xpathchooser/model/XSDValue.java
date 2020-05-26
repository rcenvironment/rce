/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.xpathchooser.model;

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
     * @see de.rcenvironment.core.gui.xpathchooser.model.XSDTreeItem#getPath()
     */
    @Override
    public String getPath() {
        final StringBuilder path = new StringBuilder(parent.getPath());
        path.append("=\"").append(name).append("\"");
        return path.toString();
    }

}
