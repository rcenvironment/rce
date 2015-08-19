/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.inputprovider.common;

import de.rcenvironment.core.component.api.ComponentConstants;

/**
 * InputProviderComponentConstants class.
 * 
 * @author Sascha Zur
 * @author Mark Geiger
 */
public final class InputProviderComponentConstants {

    /** Constant. */
    public static final String META_VALUE = "startValue";

    /** Constant. */
    public static final String META_STARTVALUEBOOLEAN = "startvalueboolean";

    /** Constant. */
    public static final String META_FILESOURCETYPE = "fileSourceType";
    
    /** Identifier of the Joiner component. */
    public static final String COMPONENT_ID = ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + "inputprovider";
    
    /** Identifiers of the Joiner component. */
    public static final String[] COMPONENT_IDS = new String[] { COMPONENT_ID,
        "de.rcenvironment.components.inputprovider.execution.InputProviderComponent_Input Provider" };

    /** Constant. */
    public static final String PLACEHOLDER_FOMRAT = "%s (%s)";

    /**
     * Available sources for files.
     * 
     * @author Doreen Seider
     */
    public enum FileSourceType {

        /** If file is chosen at workflow start. */
        atWorkflowStart,

        /** If file was chosen from workflow project. */
        fromProject,

        /** If file was chosen from file system. */
        fromFileSystem;
    }

    private InputProviderComponentConstants() {

    }

}
