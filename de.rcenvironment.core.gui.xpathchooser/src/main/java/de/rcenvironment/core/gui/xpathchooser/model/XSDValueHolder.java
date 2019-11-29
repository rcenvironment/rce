/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.xpathchooser.model;

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
