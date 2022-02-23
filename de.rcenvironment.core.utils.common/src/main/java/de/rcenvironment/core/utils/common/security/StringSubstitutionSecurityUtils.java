/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.security;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Unit test for {@link StringSubstitutionSecurityUtils}.
 * 
 * @author Robert Mischke
 */
// TODO (p1) review pattern lists for completeness
public final class StringSubstitutionSecurityUtils {

    private static final String[] COMMON_FORBIDDEN_PATTERNS = {
        "\"", // breaking out of surrounding double quotes
        "[\\u0000-\\u001f]", // ASCII characters 0-31 (including \0, newlines and tabs)
        "\\\\", // backslash - character escaping, path traversal (escaped for java and regexp -> 4x)
        "/", // forward slash - path traversal
        "\\*", "\\?", // file system wildcards
    };

    // option for unit tests; see setter method
    private static boolean suppressLogMessageOnDeniedSubstitution = false;

    private static final Log LOG = LogFactory.getLog(StringSubstitutionSecurityUtils.class);

    /**
     * Provides identifiers for various contexts that strings may be substituted/inserted into.
     * 
     * @author Robert Mischke
     */
    public enum SubstitutionContext {

        /** Context identifier for Windows batch files. */
        WINDOWS_BATCH(new String[] {
            "%", // variable substitution - access to environment variables ("%env%")
        }),

        /** Context identifier for Linux bash scripts. */
        LINUX_BASH(new String[] {
            "\u0060", // backtick - command substitution
            "\\$", // bash substitution - command substitution ("$(...)"), access to environment variables ("$VAR")
        }),

        /** Context identifier for Jython script code. */
        JYTHON(new String[] {
            "'" // forbid single quote too until proven safe
        });

        private static final String REGEXP_ALTERNATIVES_JOINER = "|";

        private final Pattern forbiddenCharactersRegexp;

        SubstitutionContext(String[] customPatterns) {
            String patternString = StringUtils.join(COMMON_FORBIDDEN_PATTERNS, REGEXP_ALTERNATIVES_JOINER);
            // don't break regexp if there are no custom patterns for a context
            if (customPatterns.length > 0) {
                patternString += REGEXP_ALTERNATIVES_JOINER + StringUtils.join(customPatterns, REGEXP_ALTERNATIVES_JOINER);
            }
            this.forbiddenCharactersRegexp = Pattern.compile(patternString);
        }

        public Pattern getForbiddenCharactersRegexp() {
            return forbiddenCharactersRegexp;
        }
    }

    private StringSubstitutionSecurityUtils() {}

    /**
     * Tests whether the given string can be safely inserted/substituted into the given target context if it is surrounded by double quotes
     * (<code>other text/code "&lt;tested string&gt;" other text/code</code>).
     * 
     * @param string the string content to test
     * @param context the context the string should be tested against (and if it is considered safe, inserted into)
     * @return true if the given string is considered safe; false if it is not
     */
    public static boolean isSafeForSubstitutionInsideDoubleQuotes(String string, SubstitutionContext context) {
        if (string == null) {
            throw new NullPointerException("The substitution string can not be 'null'");
        }
        if (context == null) {
            throw new NullPointerException("Internal error: Subsctitution context is 'null'");
        }
        Matcher matcher = context.getForbiddenCharactersRegexp().matcher(string);
        if (matcher.find()) {
            if (!suppressLogMessageOnDeniedSubstitution) {
                LOG.warn(de.rcenvironment.core.utils.common.StringUtils.format(
                    "Denied string \"%s\" for substitution in context %s because of insecure character sequence <%s>", string,
                    context.name(), matcher.group(0)));
            }
            return false;
        } else {
            return true;
        }
    }

    /**
     * Custom option to make unit tests less verbose.
     * 
     * @param suppress true to suppress the "Denied string ... for substitution" if an insecure string is rejected
     */
    protected static void setSuppressLogMessageOnDeniedSubstitution(boolean suppress) {
        suppressLogMessageOnDeniedSubstitution = suppress;
    }
}
