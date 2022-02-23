/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.xpathchooser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * This is a very simple representation of an xpath path expressions. It
 * supports only steps, attribute axis, node tests and predicates, while
 * not parsing predicates further.
 *
 * @author Heinrich Wendel
 * @author Markus Kunde
 */
public class XPathLocation {

    /**
     * Is this path absolute?
     */
    private boolean xAbsolute = false;
    
    /**
     * List of steps of this path.
     */
    private List<XPathStep> steps = new ArrayList<XPathStep>();
    
    /**
     * Sets the absolute.
     * @param absolute The absolute to set.
     */
    public void setAbsolute(boolean absolute) {
        this.xAbsolute = absolute;
    }

    /**
     * Returns the absolute.
     * @return Returns the absolute.
     */
    public boolean isAbsolute() {
        return xAbsolute;
    }

    /**
     * Returns the steps.
     * @return Returns the steps.
     */
    public List<XPathStep> getSteps() {
        return steps;
    }
    
    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final StringBuilder path = new StringBuilder();
        if (xAbsolute) {
            path.append("/");
        }
        if (steps.size() > 0) {
            path.append(steps.get(0));
            final Iterator<XPathStep> it = steps.listIterator(1);
            while (it.hasNext()) {
                final XPathStep step = it.next();
                if (!((step instanceof XPathPredicate) || (step instanceof XPathValue))) {
                    path.append("/");
                }
                path.append(step);
            }
        }
        return path.toString();
    }

}
