/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.api;

/**
 * Class holding constants for loop components.
 * 
 * @author Doreen Seider
 */
public final class LoopComponentConstants {

    /** Key for component's reset behavior. */
    public static final String COMPONENT_IS_RESET_SINK = "rce.component.isDriverComponent";

    /** Constant. */
    public static final String ENDPOINT_NAME_LOOP_DONE = "Done";

    /** Constant. */
    public static final String CONFIG_KEY_IS_NESTED_LOOP = "isNestedLoop_5e0ed1cd";

    /** Constant. */
    public static final String CONFIG_KEY_LOOP_FAULT_TOLERANCE_COMP_FAILURE = "loopFaultTolerance_5e0ed1cd";

    /** Constant. */
    public static final String CONFIG_KEY_LOOP_FAULT_TOLERANCE_NAV = "faultTolerance-NAV_5e0ed1cd";

    /** Constant. */
    public static final String CONFIG_KEY_MAX_RERUN_BEFORE_FAIL_NAV = "maxRerunBeforeFail-NAV_5e0ed1cd";

    /** Constant. */
    public static final String CONFIG_KEY_MAX_RERUN_BEFORE_DISCARD_NAV = "maxRerunBeforeDiscard-NAV_5e0ed1cd";

    /** Constant. */
    public static final String CONFIG_KEY_FAIL_LOOP_ONLY_NAV = "failLoopOnly-NAV_5e0ed1cd";

    /** Constant. */
    public static final String CONFIG_KEY_FINALLY_FAIL_IF_DISCARDED_NAV = "finallyFailIfDiscarded-NAV_5e0ed1cd";

    /** Constant. */
    public static final String ENDPOINT_ID_TO_FORWARD = "toForward";

    /** Constant. */
    public static final String ENDPOINT_ID_START_TO_FORWARD = "startToForward";

    /** Constant. */
    public static final String ENDPOINT_ID_FINAL_TO_FORWARD = "finalToForward";

    /** Constant. */
    public static final String ENDPOINT_STARTVALUE_SUFFIX = "_start";

    /** Constant. */
    public static final String ENDPOINT_STARTVALUE_GROUP = "startValues";

    /** Constant. */
    public static final String META_KEY_LOOP_ENDPOINT_TYPE = "loopEndpointType_5e0ed1cd";

    /** Private Constructor. */
    private LoopComponentConstants() {
        // NOP
    }

    /**
     * How a loop behaves in case of failure (certain component sent not-a-value or failed).
     * 
     * @author Doreen Seider
     */
    public enum LoopBehaviorInCaseOfFailure {
        /**
         * Let the workflow fail.
         */
        Fail,
        /**
         * Discard the evaluation run and continue with next one.
         */
        Discard,
        /**
         * Rerun the evaluation run and fails if maximum of reruns exceeded. Only applicable for "not-a-value" failure. // TODO add separate
         * behavior for component failures and "not-a-value" failures into two enums
         */
        RerunAndFail,
        /**
         * Rerun the evaluation run and discard the evaluation run and continue with next one if maximum of reruns exceeded. Only applicable
         * for "not-a-value" failure. // TODO add separate behavior for component failures and "not-a-value" failures into two enums
         */
        RerunAndDiscard;

        private static LoopBehaviorInCaseOfFailure defaultBehavior = LoopBehaviorInCaseOfFailure.Fail;

        /**
         * @param behavior {@link LoopBehaviorInCaseOfFailure} as string
         * @return appropriate {@link LoopBehaviorInCaseOfFailure}; if given string is <code>null</code> or cannot be parsed to
         *         {@link LoopBehaviorInCaseOfFailure} {@link LoopBehaviorInCaseOfFailure#Fail} is returned as default
         */
        public static LoopBehaviorInCaseOfFailure fromString(String behavior) {
            if (behavior == null) {
                return defaultBehavior;
            } else {
                try {
                    return LoopBehaviorInCaseOfFailure.valueOf(behavior);
                } catch (IllegalArgumentException e) {
                    return defaultBehavior;
                }
            }
        }
    }

}
