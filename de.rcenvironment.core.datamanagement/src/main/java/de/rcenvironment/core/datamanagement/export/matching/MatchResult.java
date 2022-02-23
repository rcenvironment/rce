/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.export.matching;

import java.util.LinkedList;
import java.util.List;

/**
 * The result of a comparison between two {@link Matchable} objects.
 *
 * @author Tobias Brieden
 */
public class MatchResult {

    private List<Cause> causes;

    public MatchResult() {
    }

    /**
     * Class used for descibing the cause of a match failure.
     */
    public final class Cause {

        private String cause;

        private MatchResult nestedCause;

        private Cause(String cause, MatchResult nestedCause) {
            this.cause = cause;
            this.nestedCause = nestedCause;
        }

        private Cause(String cause) {
            this(cause, null);
        }

        public String getCause() {
            return cause;
        }

        public MatchResult getNestedCause() {
            return nestedCause;
        }

        /**
         * @return <code>true</code>, if the Cause has a nested cause;
         *         <code>false</code>, otherwise.
         */
        public boolean hasNestedCause() {
            return nestedCause != null;
        }
    }
    public List<Cause> getCauses() {
        return causes;
    }

    /**
     * Adds a failure cause to the match result.
     * 
     * @param cause The description of the failure.
     */
    public void addFailureCause(String cause) {
        if (causes == null) {
            causes = new LinkedList<>();
        }

        causes.add(new Cause(cause));
    }

    /**
     * Adds a failure cause to the match result.
     * 
     * @param cause       The description of the failure.
     * @param nestedCause Another MatchResult further describing the failure.
     */
    public void addFailureCause(String cause, MatchResult nestedCause) {
        if (causes == null) {
            causes = new LinkedList<>();
        }
        causes.add(new Cause(cause, nestedCause));
    }

    /**
     * @return If there are no causes listed why a match failed, the match succeeded
     *         and this method returns <code>true</code>.
     */
    public boolean hasMatched() {
        return causes == null || causes.size() == 0;
    }
    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();

        this.printOneLevelOfMatchResult(builder, "");

        return builder.toString();
    }

    private void printOneLevelOfMatchResult(StringBuilder builder, String indentation) {
        if (this.causes != null) {
            for (Cause cause : this.causes) {
                builder.append(indentation + cause.getCause() + System.lineSeparator());
                if (cause.hasNestedCause()) {
                    cause.getNestedCause().printOneLevelOfMatchResult(builder, indentation + "    ");
                }
            }
        }
    }
}
