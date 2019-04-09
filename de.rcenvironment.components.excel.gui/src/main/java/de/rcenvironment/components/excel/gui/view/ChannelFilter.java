/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.excel.gui.view;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import de.rcenvironment.components.excel.common.ChannelValue;


/**
 * Filter for channel type.
 *
 * @author Markus Kunde
 */
public class ChannelFilter extends ViewerFilter {

    /**
     * Filter.
     */
    private boolean isInputChannel = true;
    
    /**
     * Set filter to true if only inputchannels should be in result.
     * 
     * @param isInput true if filtered result shoulkd contain only inputchannels.
     */
    public void setChannelFilter(final boolean isInput) {
        // Search must be a substring of the existing value
        this.isInputChannel = isInput;
    }
    
    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        ChannelValue cv = (ChannelValue) element;
        return !logicalXOR(cv.isInputValue(), isInputChannel);

    }
    
    /**
     * Logical XOR.
     * 
     * @param x first parameter
     * @param y second parameter
     * @return true if XOR is true
     */
    public static boolean logicalXOR(final boolean x, final boolean y) {
        return ((x || y) && !(x && y));
    }
}
