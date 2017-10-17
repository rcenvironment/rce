/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.xpathchooser.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract implementaiton of the XSDValueHolder interface.
 *
 * @author Heinrich Wendel
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public abstract class AbstractXSDValueHolder extends AbstractXSDTreeItem implements XSDValueHolder {

    /**
     * Inheritable.
     */
    private static final long serialVersionUID = 3354899384420270939L;
    
    /**
     * List of possible values.
     */
    protected List<XSDValue> values = new ArrayList<XSDValue>();
    
    
    /**
     * Constructor.
     * 
     * @param parent See parent.
     * @param name See parent.
     */
    public AbstractXSDValueHolder(XSDTreeItem parent, String name) {
        super(parent, name);
    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.gui.xpathchooser.model.XSDValueHolder#getValues()
     */
    @Override
    public List<XSDValue> getValues() {
        return values;
    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.gui.xpathchooser.model.XSDValueHolder#setValues(java.util.List)
     */
    @Override
    public void setValues(final List<XSDValue> theValues) {
        values = theValues;
    }

}
