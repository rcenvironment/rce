/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.cpacs.utils.common.xml.internal;


/**
 * Mapping mode.
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public enum EMappingMode {

    /**
     * Append all to end of children list, regardless if same name already exists. 
     */
    Append,

    /**
     * Remove all and insert all, but reuse if name already exists (?).  
     */
    Delete;
    
}
