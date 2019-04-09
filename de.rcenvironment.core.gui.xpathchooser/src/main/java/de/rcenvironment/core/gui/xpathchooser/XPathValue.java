/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.xpathchooser;

/**
 * Represents all contens of the last attribute value.
 *
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public class XPathValue extends XPathStep {


    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "=" + xValue;
    }   

}
