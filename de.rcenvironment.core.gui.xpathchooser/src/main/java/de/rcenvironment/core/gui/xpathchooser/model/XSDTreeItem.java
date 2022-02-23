/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.xpathchooser.model;

import java.io.Serializable;


/**
 * Common class for all types of items in the xsd tree.
 *
 * @author Heinrich Wendel
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public interface XSDTreeItem extends Serializable {

    /**
     * Returns the name.
     * @return Returns the name.
     */
    String getName();

    /**
     * Sets the name.
     * @param name The name to set.
     */
    void setName(String name);

    /**
     * Returns the parent.
     * @return Returns the parent.
     */
    XSDTreeItem getParent();
    
    /**
     * Sets the parent.
     * @param parent The parent to set.
     */
    void setParent(XSDTreeItem parent);
    
    /**
     * Returns the path of this item in the tree.
     * @return The path of this item in the tree.
     */
    String getPath();
    
    /**
     * Property that marks if the given item was added during runtime.
     * @param dynamic If the given item was added during runtime.
     */
    void setDynamic(boolean dynamic);
    
    /**
     * Property that marks if the given item was added during runtime.
     * @return If the given item was added during runtime.
     */
    boolean isDynamic();
    
}
