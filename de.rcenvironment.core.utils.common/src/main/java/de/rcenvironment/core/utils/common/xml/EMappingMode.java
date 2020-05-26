/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.common.xml;


/**
 * Mapping mode.
 * 
 * @author Arne Bachmann
 * @author Markus Kunde
 * @author Jan Flink (added DeleteOnly)
 */

public enum EMappingMode {

    /**
     * Append all to end of children list, regardless if same name already exists. 
     */
    Append,

    /**
     * Remove all and insert all, but reuse if name already exists (?).  
     */
    Delete,

    /**
     * Removes all nodes defined by the target path.
     */
    DeleteOnly;
    
}
