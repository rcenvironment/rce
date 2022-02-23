/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.statistics.utils;

import java.util.regex.Pattern;

/**
 * A utility class to create compact single-line stacktrace strings. A typical use case for these is using them as a statistics counter
 * argument, resulting in a statistic about how frequently a code location is called from which code paths.
 * 
 * @author Robert Mischke
 */
public final class CompactStacktraceBuilder {

    private final Pattern matchPattern;

    private final String separator;

    private final boolean includeMethodNames;

    /**
     * Creates an instance with a regular expression defining the class names to include (see below), using a default separator (currently
     * "&lt;"), and including method names.
     * 
     * @param matchRegexp a regular expression that each stack trace element's class must match to be included; all other elements are
     *        omitted, while still adding the separators that would glue them together, which shows how many stacktrace steps were skipped
     */
    public CompactStacktraceBuilder(String matchRegexp) {
        this(matchRegexp, "<", true);
    }

    /**
     * Creates an instance with a regular expression defining the class names to include (see below), and a custom separator that is used to
     * join the stacktrace steps.
     * 
     * @param matchRegexp a regular expression that each stack trace element's class must match to be included; all other elements are
     *        omitted, while still adding the separators that would glue them together, which shows how many stacktrace steps were skipped
     * @param separator a string used to "glue" the stack trace element's class names together
     * @param includeMethodNames whether to include method names in the output, or just the line numbers
     */
    public CompactStacktraceBuilder(String matchRegexp, String separator, boolean includeMethodNames) {
        this.matchPattern = Pattern.compile(matchRegexp);
        this.separator = separator;
        this.includeMethodNames = includeMethodNames;
    }

    /**
     * Returns a compact, single-line stacktrace representation. Its exact content is defined by the constructor arguments used. Common
     * aspects are omitting package and method names, ie logging only class names and line numbers, and truncating the stack trace when a
     * certain class pattern is met or not met anymore.
     * 
     * @return .
     */
    public String getSingleLineStacktrace() {
        return getSingleLineStacktrace(1); // semantically, this should be 0, but must be 1 to exclude the zero-args method itself
    }

    /**
     * Returns a compact, single-line stacktrace representation. Its exact content is defined by the constructor arguments used. Common
     * aspects are omitting package and method names, ie logging only class names and line numbers, and truncating the stack trace when a
     * certain class pattern is met or not met anymore.
     * 
     * @param skipInitial the number of initial stacktrace steps to skip; useful if the call is made in an utility class which should not
     *        appear in the generated output
     * @return .
     */
    public String getSingleLineStacktrace(int skipInitial) {
        StringBuilder buffer = new StringBuilder();
        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        for (int p = skipInitial + 1; p < stackTrace.length; p++) {
            final StackTraceElement traceStep = stackTrace[p];
            final String className = traceStep.getClassName();
            if (buffer.length() != 0) {
                buffer.append(separator);
            }
            if (!matchPattern.matcher(className).matches()) {
                continue; // the separator was still added, marking how many steps were skipped
            }
            buffer.append(className.substring(className.lastIndexOf(".") + 1)); // also works for root package
            if (includeMethodNames) {
                buffer.append(".");
                buffer.append(traceStep.getMethodName());
            }
            buffer.append(":");
            buffer.append(traceStep.getLineNumber());
        }
        return buffer.toString();
    }
}
