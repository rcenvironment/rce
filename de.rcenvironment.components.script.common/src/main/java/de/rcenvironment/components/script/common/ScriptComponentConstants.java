/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.components.script.common;

import de.rcenvironment.core.component.api.ComponentConstants;

/**
 * Constants uses by the Script component.
 * 
 * @author Doreen Seier
 */
public final class ScriptComponentConstants {

    /** Identifier of the Joiner component. */
    public static final String COMPONENT_ID = ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + "script";
    
    /** Identifiers of the Joiner component. */
    public static final String[] COMPONENT_IDS = new String[] { COMPONENT_ID,
        "de.rcenvironment.components.script.execution.ScriptComponent_Script" };
    
    /** Config key for script language. */
    public static final String SCRIPT_LANGUAGE = "scriptLanguage";
    
    /** Config key for script language. */
    public static final String COMPONENT_SIZE = "componentSize";
    
    /** Default config value. Must be equal to the preconfigured script in '...execution/resources/configuration.json' */
    public static final String DEFAULT_SCRIPT_WITHOUT_COMMENTS_AND_IMPORTS =
        "import sys\n\nsys.stderr.write('Script was not configured')\nsys.stderr.flush()";
    
    /** Default config value. Must be equal to the last line of /resources/default.py */
    public static final String DEFAULT_SCRIPT_LAST_LINE = "sys.stderr.flush()";
    
    /** Input group name. */
    public static final String GROUP_NAME_OR = "or";

    /** Input group name. */
    public static final String GROUP_NAME_AND = "default";
    
    /** Input group name. */
    public static final String PROP_KEY_XOR = "xor";

    private ScriptComponentConstants() {}
    
}
