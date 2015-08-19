/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.cpacs.gui.xpathchooser.model;

import java.util.List;

/**
 * Interface that allows both attribute and elements to hold values.
 *
 * @author Heinrich Wendel
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public interface XSDValueHolder extends XSDTreeItem {
    
    /**
     * Returns the values.
     * @return Returns the values.
     */
    List<XSDValue> getValues();

    /**
     * The values to set.
     * @param values The values to set.
     */
    void setValues(List<XSDValue> values);

}
