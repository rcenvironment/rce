/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.xpathchooser;

import de.rcenvironment.core.utils.common.variables.legacy.VariableType;


/**
 * One variable entry as defined by the xpath chooser.
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public final class VariableEntry {
    
    /**
     * Delimiter for to string conversion.
     */
    private static final String SERIALIZE_SEP = ";";

    /**
     * Variable name, only a..z,A..Z,0..9,_.
     */
    private String name;
    
    /**
     * The xpath of the XML location.
     */
    private String xpath;
    
    /**
     * Variable direction.
     */
    private EVariableDirection direction;
    
    /**
     * Data type.
     */
    private VariableType type;

    
    /**
     * Default constructor.
     */
    public VariableEntry() {
        // do nothing
    }
    
    /**
     * Copy constructor.
     * @param copyFrom The origin
     */
    public VariableEntry(final VariableEntry copyFrom) {
        direction = copyFrom.direction;
        name = copyFrom.name;
        xpath = copyFrom.xpath;
        type = copyFrom.type;
    }
    
    /**
     * Initializing constructor.
     * @param aDirection The direction
     * @param aName The name
     * @param anXpath The xpath
     * @param aType the type
     */
    public VariableEntry(final EVariableDirection aDirection, final String aName, final String anXpath, final VariableType aType) {
        direction = aDirection;
        name = aName;
        xpath = anXpath;
        type = aType;
    }
    /**
     * Deserializing constructor.
     * @param fromString The string representation as created in toString
     */
    public VariableEntry(final String fromString) {
        final int firstPos = fromString.indexOf(SERIALIZE_SEP);
        final int secondPos = fromString.indexOf(SERIALIZE_SEP, firstPos + 1);
        final int thirdPos = fromString.indexOf(SERIALIZE_SEP, secondPos + 1);
        direction = EVariableDirection.valueOf(fromString.substring(0, firstPos));
        name = fromString.substring(firstPos + 1, secondPos);
        xpath = fromString.substring(secondPos + 1, thirdPos);
        type = VariableType.valueOf(fromString.substring(thirdPos + 1));
    }
    
    /**
     * Getter.
     * @return the name
     */
    public String getName() {
        return name;
    }

    
    /**
     * Setter.
     * @param aName the name to set
     * @return this
     */
    public VariableEntry setName(final String aName) {
        this.name = aName;
        return this;
    }

    
    /**
     * Getter.
     * @return the xpath
     */
    public String getXpath() {
        return xpath;
    }

    
    /**
     * Setter.
     * @param aXpath the xpath to set
     * @return this
     */
    public VariableEntry setXpath(final String aXpath) {
        this.xpath = aXpath;
        return this;
    }

    
    /**
     * Getter.
     * @return the direction
     */
    public EVariableDirection getDirection() {
        return direction;
    }

    
    /**
     * Setter.
     * @param aDirection the direction to set
     * @return this
     */
    public VariableEntry setDirection(final EVariableDirection aDirection) {
        this.direction = aDirection;
        return this;
    }
    
    /**
     * Get the defined type.
     * @return The type
     */
    public VariableType getType() {
        return type;
    }
    
    /**
     * Set the defined data type.
     * @param aType The type
     * @return The object self
     */
    public VariableEntry setType(final VariableType aType) {
        type = aType;
        return this;
    }
    
    @Override
    public String toString() {
        return direction.name() + SERIALIZE_SEP + name + SERIALIZE_SEP + xpath + SERIALIZE_SEP + type.toString();
    }
    
}
