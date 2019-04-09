/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.evaluationmemory.common;

import de.rcenvironment.core.component.api.ComponentConstants;

/**
 * Constants used by the memory component.
 * 
 * @author Doreen Seider
 * @author Alexander Weinert
 */
public final class EvaluationMemoryComponentConstants {

    /**
     * Enum for the behavior of the evaluation memory component when there are multiple recorded evaluations inside the tolerance-interval
     * of the current input-values.
     * 
     * @author Alexander Weinert
     */
    public enum OverlapBehavior {
        /**
         * Re-evaluate using the given input values.
         */
        STRICT,

        /**
         * Return any stored evaluations. The precise evaluation returned is implementation-defined.
         */
        LENIENT;

        /**
         * Convenience method for converting the string stored in the tool configuration to an instance of this enum. Since the workflow can
         * be changed by the user manually on the disk, and since there may be legacy configuration files that do not specify an overlap
         * behavior at all, we have to check for both "errors" individually. In both cases, we default to the "safe" option of strict
         * overlap handling
         * 
         * @param configValue A string representation of the overlap behavior taken from the component configuration. May be null.
         * @return An instance of this enum that has the given string representation, or a safe default if no such instance exists.
         */
        public static OverlapBehavior parseConfigValue(final String configValue) {
            // We cannot simply parse the obtained string into an enum since it may be a) null or b) a value that does not correspond to any
            // enum value. We check case a) before parsing and case b) by catching an exception. In both cases, we default to the (safe)
            // option of strict overlap handling. Moreover, overlapBehavior ought to be final, but this does not compile, since Java
            // believes it to be potentially set in the try-block before the catch-block is executed.
            if (configValue == null) {
                return OverlapBehavior.STRICT;
            } else {
                try {
                    return OverlapBehavior.valueOf(configValue);
                } catch (IllegalArgumentException e) {
                    return OverlapBehavior.STRICT;
                }
            }
        }
    }

    /** Constants. */
    public static final String COMPONENT_ID = ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + "evaluationmemory";

    /** Constants. */
    public static final String CONFIG_SELECT_AT_WF_START = "select_at_wf_start";

    /** Constants. */
    public static final String CONFIG_MEMORY_FILE = "mem_file";

    /** Constants. */
    public static final String CONFIG_CONSIDER_LOOP_FAILURES = "store_failures";

    /** Constants. */
    public static final String CONFIG_MEMORY_FILE_WF_START = "mem_file_wf_start";

    /** Constants. The id under which the tolerance overlap behavior is stored in the data of the button which sets it */
    public static final String CONFIG_KEY_TOLERANCE_OVERLAP_BEHAVIOR = "tolerance_overlap_behavior";

    /** Constants. */
    public static final String PLACEHOLDER_MEMORY_FILE_PATH = "${memory_file_path}";

    /** Constants. */
    public static final String ENDPOINT_ID_TO_EVALUATE = "to_evaluate";

    /** Constants. */
    public static final String ENDPOINT_ID_EVALUATION_RESULTS = "evaluation_results";

    /** Constant. The id under which the tolerance for a given input is stored in the metadata for that input */
    public static final String META_TOLERANCE = "tolerance";

    /** Constant. Percentage sign character */
    public static final String PERCENTAGE_SIGN = "%";

    private EvaluationMemoryComponentConstants() {}

}
