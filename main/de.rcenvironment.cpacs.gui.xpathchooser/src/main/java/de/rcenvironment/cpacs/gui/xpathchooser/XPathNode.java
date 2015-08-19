/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.cpacs.gui.xpathchooser;


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
