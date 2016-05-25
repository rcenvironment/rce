/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

/**
 * Represents an option item for selecting an endpoint type. Its main application is type-safe
 * configuration of selection lists or dropdown boxes.
 * 
 * TODO move this type to a non-gui package?
 * 
 * @author Robert Mischke
 */
public class TypeSelectionOption {

    private String displayName;

    private String typeName;

    /**
     * Default constructor.
     * 
     * @param displayName
     * @param javaType
     */
    public TypeSelectionOption(String displayName, String javaType) {
        this.displayName = displayName;
        this.typeName = javaType;
    }

    /**
     * Get the user description for this type.
     * @return Returns the user description
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Set the user description for this type.
     * @param displayName the user description
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Get the name of the Java class to use, as returned by {@link Class#getName()}.
     * @return the String representation of the java type
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * Set the name of the Java class to use, as returned by {@link Class#getName()}.
     * @param typeName the String representation of the java type
     */
    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

}
