/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.xpathchooser.model;

import java.util.ArrayList;
import java.util.List;


/**
 * A simplified representation of a XSD document.
 * Usually an xml document can only have one root,
 * but in this case we need multiple options.
 *
 * @author Heinrich Wendel
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public class XSDDocument {

    /**
     * List of subelements.
     */
    private List<XSDElement> elements = new ArrayList<XSDElement>();
    
    /**
     * Returns the rootElement.
     * @return Returns the rootElement.
     */
    public List<XSDElement> getElements() {
        return elements;
    }
    
}
