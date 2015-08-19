/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.common;

import de.rcenvironment.core.component.api.ComponentConstants;

/**
 * Constants for OutputWriter.
 * 
 * @author Hendrik Abbenhaus
 * @author Sascha Zur
 * 
 */
public final class OutputWriterComponentConstants {

    /** Identifier of Output Writer component. */
    public static final String COMPONENT_ID = ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + "outputwriter";

    /** placeholder prefix. */
    public static final String PH_PREFIX = "[";

    /** placeholder suffix. */
    public static final String PH_SUFFIX = "]";

    /** placeholder inputname. */
    public static final String PH_INPUTNAME = PH_PREFIX + "Input name" + PH_SUFFIX;

    /** placeholder workflowname. */
    public static final String PH_WORKFLOWNAME = PH_PREFIX + "Workflow name" + PH_SUFFIX;

    /** placeholder timestamp. */
    public static final String PH_TIMESTAMP = PH_PREFIX + "Timestamp" + PH_SUFFIX;

    /** placeholder wf timestamp. */
    public static final String PH_WF_START_TS = PH_PREFIX + "Timestamp at workflow start" + PH_SUFFIX;

    /** placeholder component name. */
    public static final String PH_COMP_NAME = PH_PREFIX + "Component name" + PH_SUFFIX;

    /** placeholder component type. */
    public static final String PH_COMP_TYPE = PH_PREFIX + "Component type" + PH_SUFFIX;

    /** All Placeholder. */
    public static final String[] WORDLIST = new String[] { PH_COMP_NAME, PH_INPUTNAME, PH_TIMESTAMP, PH_WF_START_TS, PH_WORKFLOWNAME };

    /** Constant. */
    public static final String ROOT_DISPLAY_NAME = PH_PREFIX + "root" + PH_SUFFIX;

    /** You can not create Files with these names. */
    public static final String[] PROBLEMATICFILENAMES_WIN = new String[] {
        "CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5",
        "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4",
        "LPT5", "LPT6", "LPT7", "LPT8", "LPT9" };

    /** PropertyName of selected Root. */
    public static final String CONFIG_KEY_ROOT = "SelectedRoot";

    /** PropertyName of the writePath. */
    public static final String CONFIG_KEY_ONWFSTART_ROOT = "OWWritePath";

    /** PropertyName of OnWFStart. */
    public static final String CONFIG_KEY_ONWFSTART = "SelectRootOnWorkflowStart";

    /** Constant. */
    public static final String CONFIG_KEY_BEHAVIOUR = "ifexist";

    /** Constant. */
    public static final String CONFIG_KEY_INPUTS = "inputs";

    /** Constant. */
    public static final String CONFIG_KEY_TYPE = "type";

    /** Constant. */
    public static final String CONFIG_KEY_PATH = "path";

    /** Constant. */
    public static final String CONFIG_KEY_FOLDERFORSAVING = "folderForSaving";

    /** Constant. */
    public static final String CONFIG_KEY_FILENAME = "filename";

    /**
     * type of owtreeitem.
     * 
     * @author Hendrik Abbenhaus
     * 
     * 
     */
    public enum Type {
        /** Type file. */
        FILE,
        /** Type folder. */
        FOLDER,
        /** Type root. */
        ROOT
    };

    /**
     * handle if file exist.
     * 
     * @author Hendrik Abbenhaus
     * 
     */
    public enum Handle {
        /** do nothing. */
        DO_NOTHING,
        /** override. */
        OVERRIDE,
        /** append. */
        APPEND,
        /** auto rename. */
        AUTORENAME
    }

    private OutputWriterComponentConstants() {

    }
}
