/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.cpacs.vampzeroinitializer.gui.model;

/**
 * A named item.
 * 
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public interface Named {

    /**
     * Returns the name.
     * 
     * @return name
     */
    String getName();

    /**
     * Sets the name.
     * 
     * @param aName name
     * @return name
     */
    Named setName(final String aName);

}
