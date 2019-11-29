/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.xpathchooser.model;


/**
 * Default implementation of XSDTreeItem.
 *
 * @author Heinrich Wendel
 * @author Markus Kunde
 */
public abstract class AbstractXSDTreeItem implements XSDTreeItem {

    /**
     * Inheritable.
     */
    private static final long serialVersionUID = 1959252371472347242L;

    /**
     * The parent element.
     */
    protected XSDTreeItem parent;

    /**
     * Name of the attribute.
     */
    protected String name;

    /**
     * Property that marks if the item was added during runtime.
     */
    protected boolean dynamic = false;
    

    /**
     * Construct a new XSDValueHolder by name and parent.
     * 
     * @param theParent The parent to set.
     * @param theName The name to set.
     */
    public AbstractXSDTreeItem(final XSDTreeItem theParent, final String theName) {
        super();
        name = theName;
        parent = theParent;
    }
    
    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.gui.xpathchooser.model.XSDTreeItem#getName()
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.gui.xpathchooser.model.XSDTreeItem#setName(java.lang.String)
     */
    @Override
    public void setName(final String theName) {
        name = theName;
    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.gui.xpathchooser.model.XSDTreeItem#getParent()
     */
    @Override
    public XSDTreeItem getParent() {
        return parent;
    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.gui.xpathchooser.model.XSDTreeItem#setParent(de.rcenvironment.core.gui.xpathchooser.model.XSDTreeItem)
     */
    @Override
    public void setParent(final XSDTreeItem theParent) {
        parent = theParent;
    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.gui.xpathchooser.model.XSDTreeItem#isDynamic()
     */
    @Override
    public boolean isDynamic() {
        return dynamic;
    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.gui.xpathchooser.model.XSDTreeItem#setDynamic(boolean)
     */
    @Override
    public void setDynamic(final boolean isDynamic) {
        dynamic = isDynamic;
    }
    
    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof XSDTreeItem) {
            return ((XSDTreeItem) o).getName().equals(name);
        }
        return false;
    }
    
    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return getName().hashCode();
    }
    
}
