/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.scripting;

/**
 * Container holding constants for scriptable components.
 * 
 * @author Christian Weiss
 */
public final class ScriptableComponentConstants {

    /**
     * Times of script executions.
     * 
     * @author Christian Weiss
     */
    public enum ScriptTime {
        /** Before execution of the functionality. */
        PRE,
        /** After execution of the functionality. */
        POST;

        private String prefix() {
            return name().toLowerCase();
        }

        private String infix() {
            return name().substring(0, 1).toUpperCase() + name().substring(1).toLowerCase();
        }
    }

    /**
     * Modes of component executions.
     * 
     * @author Christian Weiss
     */
    public enum ComponentRunMode {
        /** Initial run. */
        INIT,
        /** Normal (non-initial) run. */
        RUN;

        private String infix() {
            return name().substring(0, 1).toUpperCase() + name().substring(1).toLowerCase();
        }
    }

    /** {@link ScriptTime} before functionality execution. */
    public static final ScriptTime PRE = ScriptTime.PRE;

    /** {@link ScriptTime} after functionality execution. */
    public static final ScriptTime POST = ScriptTime.POST;

    /** {@link ComponentRunMode initial run. */
    public static final ComponentRunMode INIT = ComponentRunMode.INIT;

    /** {@link ComponentRunMode normal (non-initial) run. */
    public static final ComponentRunMode RUN = ComponentRunMode.RUN;

    /** The property key for the pre init script. */
    public static final String PRE_INIT_SCRIPT = "preInitScript";

    /** The property key for the pre init script language. */
    public static final String PRE_INIT_SCRIPT_LANGUAGE = "preInitScriptLanguage";

    /** The property key for the pre init script. */
    public static final String DO_PRE_INIT_SCRIPT = "doPreInitScript";

    /** The property key for the run init script. */
    public static final String PRE_RUN_SCRIPT = "preRunScript";

    /** The property key for the run init script language. */
    public static final String PRE_RUN_SCRIPT_LANGUAGE = "preRunScriptLanguage";

    /** The property key for the pre init script. */
    public static final String POST_INIT_SCRIPT = "postInitScript";

    /** The property key for the pre init script language. */
    public static final String POST_INIT_SCRIPT_LANGUAGE = "postInitScriptLanguage";

    /** The property key for the pre init script. */
    public static final String DO_POST_INIT_SCRIPT = "doPostInitScript";

    /** The property key for the run init script. */
    public static final String POST_RUN_SCRIPT = "postRunScript";

    /** The property key for the run init script language. */
    public static final String POST_RUN_SCRIPT_LANGUAGE = "postRunScriptLanguage";

    /** Factory to create property keys generically. */
    public static final PropertyFactory FACTORY = new PropertyFactory();

    private ScriptableComponentConstants() {
        // do nothing
    }

    /**
     * Factory to create property keys generically.
     * 
     * @author Christian Weiss
     */
    public static final class PropertyFactory {

        private PropertyFactory() {
            // do nothing
        }

        /**
         * Returns the do script property key.
         * 
         * @param scriptTime the {@link ScriptTime}
         * @param componentRunMode the {@link ComponentRunMode}
         * @return the property key of the do script property of the specified context
         */
        public String doScript(final ScriptTime scriptTime, final ComponentRunMode componentRunMode) {
            return String.format("do%s%sScript", scriptTime.infix(), componentRunMode.infix());
        }

        /**
         * Returns the script property key.
         * 
         * @param scriptTime the {@link ScriptTime}
         * @param componentRunMode the {@link ComponentRunMode}
         * @return the property key of the script property of the specified context
         */
        public String script(final ScriptTime scriptTime, final ComponentRunMode componentRunMode) {
            return String.format("%s%sScript", scriptTime.prefix(), componentRunMode.infix());
        }

        /**
         * Returns the script language property key.
         * 
         * @param scriptTime the {@link ScriptTime}
         * @param componentRunMode the {@link ComponentRunMode}
         * @return the property key of the script language property of the specified context
         */
        public String language(final ScriptTime scriptTime, final ComponentRunMode componentRunMode) {
            return String.format("%s%sScriptLanguage", scriptTime.prefix(), componentRunMode.infix());
        }

    }

}
