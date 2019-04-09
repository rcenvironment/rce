/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.xpathchooser;


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
