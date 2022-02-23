/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.xpathchooser;


/**
 * Represents a node tests, e.g. the name of a node.
 *
 * @author Heinrich Wendel
 * @author Markus Kunde
 */
public class XPathNode extends XPathStep {

    /**
     * Predicate associated to this NodeTest.
     */
    private XPathPredicate xPredicate = null;
    
    /**
     * Sets the predicate.
     * @param predicate The predicate to set.
     */
    public void setPredicate(XPathPredicate predicate) {
        this.xPredicate = predicate;
    }

    /**
     * Returns the predicate.
     * @return Returns the predicate.
     */
    public XPathPredicate getPredicate() {
        return xPredicate;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        String s = xValue;
        if (xPredicate != null) {
            s = s + xPredicate.toString();
        }
        return s;
    }

}
