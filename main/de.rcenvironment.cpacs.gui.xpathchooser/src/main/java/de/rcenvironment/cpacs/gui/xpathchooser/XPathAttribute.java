/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.cpacs.gui.xpathchooser;


/**
 * Represents an attribute axis, defined by "attribute::" or "@".
 * Not represented this way by the specification, but easier to handle in this
 * object oriented model.
 *
 * @author Heinrich Wendel
 * @author Markus Kunde
 */
public class XPathAttribute extends XPathStep {
    
    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "@" + xValue;
    }
}
