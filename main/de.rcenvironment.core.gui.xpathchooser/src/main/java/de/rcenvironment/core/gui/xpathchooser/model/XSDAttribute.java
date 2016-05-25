/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.xpathchooser.model;


/**
 * Simplified representation of an XSD attribute. An XSD attribute only has a name. All other XSD
 * elements are removed.
 * 
 * @author Heinrich Wendel
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public class XSDAttribute extends AbstractXSDValueHolder {
    
    /**
     * Inheritable.
     */
    private static final long serialVersionUID = -3625596480758417409L;

    /**
     * Construct a new XSDAttribute by name and parent.
     * 
     * @param name The name to set.
     * @param parent The parent to set.
     */
    public XSDAttribute(XSDElement parent, String name) {
        super(parent, name);
    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.gui.xpathchooser.model.XSDTreeItem#getPath()
     */
    @Override
    public String getPath() {
        return parent.getPath() + "/@" + name;
    }

}
