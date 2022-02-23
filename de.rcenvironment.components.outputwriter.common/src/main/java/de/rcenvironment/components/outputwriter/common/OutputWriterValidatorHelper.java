/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.components.outputwriter.common;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * A class for helping to validate placeholders in the OutputWriter.
 *
 * @author Dominik Schneider
 * @author Kathrin Schaffert (added StringBuilder to getValidationWarnings method)
 */

public abstract class OutputWriterValidatorHelper {

    /**
     * Placeholder for -1.
     */
    public static final int MINUS_ONE = -1;

    private static final List<Character> ALLOWED_ESCAPES = Arrays.asList('\\', '[', ']');

    private static final String LINE_SEP = System.getProperty("line.separator");

    /**
     * 
     * A method to validate a string with placeholders.
     * 
     * @param text The string to validate.
     * 
     * @return A list of validation errors that may be displayed directly to the user.
     * @author Alexander Weinert
     * 
     */
    public static List<String> getValidationErrors(String text) {
        final List<String> validationErrors = new LinkedList<>();

        if (endsWithOddNumberOfBackslashes(text)) {
            validationErrors.add("Ends with single '\\'. Use '\\\\' to enter a single backslash.");
        }

        final Set<String> unknownEscapes = getInvalidEscapes(text);
        if (!unknownEscapes.isEmpty()) {
            StringBuilder errorMessageBuilder = new StringBuilder();
            errorMessageBuilder.append(String.format("Contains unknown escape characters: %s.", String.join(", ", unknownEscapes)));
            errorMessageBuilder.append('\n');
            errorMessageBuilder.append("Only \\\\, \\[, and \\] are supported, all other characters may be entered directly.");
            validationErrors.add(errorMessageBuilder.toString());
        }

        if (containsNestedBrackets(text)) {
            validationErrors.add("Contains nested brackets '[..[..]..]'.");
        }

        if (containsUnmatchedOpeningBracket(text)) {
            validationErrors.add("Contains unmatched opening bracket '['");
        }

        if (containsUnmatchedClosingBracket(text)) {
            validationErrors.add("Contains unmatched closing bracket ']'");
        }

        return validationErrors;
    }

    private static boolean endsWithOddNumberOfBackslashes(String text) {
        int numberOfTerminalBackslashes = 0;

        for (int i = 0; i < text.length(); ++i) {
            if (text.charAt(i) == '\\') {
                ++numberOfTerminalBackslashes;
            } else {
                numberOfTerminalBackslashes = 0;
            }
        }

        return numberOfTerminalBackslashes % 2 == 1;
    }

    private static boolean containsNestedBrackets(String text) {
        boolean skipChar = false;
        boolean insideBracket = false;

        for (int i = 0; i < text.length(); ++i) {
            if (skipChar) {
                skipChar = false;
                continue;
            }
            final char currentChar = text.charAt(i);
            if (currentChar == '[') {
                if (insideBracket) {
                    return true;
                } else {
                    insideBracket = true;
                }
            } else if (currentChar == ']') {
                insideBracket = false;
            } else if (currentChar == '\\') {
                skipChar = true;
            }
        }

        return false;
    }

    private static boolean containsUnmatchedClosingBracket(String text) {
        boolean skipChar = false;
        int nestingDepth = 0;

        for (int i = 0; i < text.length(); ++i) {
            if (skipChar) {
                skipChar = false;
                continue;
            }
            final char currentChar = text.charAt(i);
            if (currentChar == ']') {
                if (nestingDepth > 0) {
                    --nestingDepth;
                } else {
                    return true;
                }
            } else if (currentChar == '[') {
                ++nestingDepth;
            } else if (currentChar == '\\') {
                skipChar = true;
            }
        }

        return false;
    }

    private static boolean containsUnmatchedOpeningBracket(String text) {
        boolean skipChar = false;
        int nestingDepth = 0;

        for (int i = 0; i < text.length(); ++i) {
            if (skipChar) {
                skipChar = false;
                continue;
            }
            final char currentChar = text.charAt(i);
            if (currentChar == '[') {
                ++nestingDepth;
            } else if (currentChar == ']') {
                nestingDepth = Math.max(nestingDepth - 1, 0);
            } else if (currentChar == '\\') {
                skipChar = true;
            }
        }

        return nestingDepth > 0;
    }

    /**
     * Returns a list of escape sequences that occur in the given text that are not one of the explicitly allowed \[, \], or \\.
     */
    private static Set<String> getInvalidEscapes(String text) {
        final Set<String> invalidEscapes = new HashSet<>();
        boolean skipChar = false;
        // We omit the final character of the string, as this character has already been validated prior to this method.
        for (int i = 0; i < text.length() - 1; ++i) {
            // We would actually like to implement this skipping via manipulating the iteration variable, but checkstyle does not allow us
            // to do so
            if (skipChar) {
                skipChar = false;
                continue;
            }
            final char currentChar = text.charAt(i);
            if (currentChar == '\\') {
                final char nextChar = text.charAt(i + 1);
                if (!ALLOWED_ESCAPES.contains(nextChar)) {
                    invalidEscapes.add(String.format("%s%s", currentChar, nextChar));
                } else {
                    // We skip the escaped character, as it may be a backslash itself
                    skipChar = true;
                }
            }
        }
        return invalidEscapes;
    }

    /**
     * As of now, we only check for unknown placeholders, i.e., the resulting list has at most a single entry. We return a list at this
     * point anyways in order to be consistent with getValidationErrors.
     * 
     * @param warningBuilder String Builder
     * @param text The format string to be checked.
     * @param knownPlaceholders The list of known placeholders that may occur in the given text
     * @return A list of warnings that may be directly displayed to the user.
     */
    public static List<String> getValidationWarnings(StringBuilder warningBuilder, String text, List<String> knownPlaceholders) {
        final List<String> warnings = new LinkedList<>();

        final List<String> placeholders = parsePlaceholders(text);
        final List<String> unknownPlaceholders = new LinkedList<>();
        for (String placeholder : placeholders) {
            if (!knownPlaceholders.contains(placeholder)) {
                unknownPlaceholders.add(placeholder);
            }
        }
        if (!unknownPlaceholders.isEmpty()) {
            warningBuilder.append(String.join(", ", unknownPlaceholders));
            warnings.add(warningBuilder.toString());
        }

        return warnings;
    }

    /**
     * @param text The format string provided by the user
     * @return A list of placeholders in the order that they appear in the given text, without duplicates.
     */
    private static List<String> parsePlaceholders(String text) {
        // We know at this point that the given text only contains the escape sequences \\, \[, and \], that all brackets are matched, that
        // the string contains no nested brackets, and that it does not end with a backslash. This simplifies the parsing at this point.

        final List<String> placeholders = new LinkedList<>();

        StringBuilder placeholderBuilder = new StringBuilder();
        boolean escapeChar = false;
        boolean inPlaceholder = false;
        for (int i = 0; i < text.length(); ++i) {
            final char currentChar = text.charAt(i);

            if (currentChar == '[') {
                if (inPlaceholder) {
                    // If we are inside a placeholder, then this character must be escaped, as otherwise it would imply a nested bracket
                    // structure.
                    placeholderBuilder.append(currentChar);
                } else if (!escapeChar) {
                    placeholderBuilder.append(currentChar);
                    inPlaceholder = true;
                }
                escapeChar = false;
            } else if (currentChar == ']') {
                if (inPlaceholder) {
                    if (escapeChar) {
                        placeholderBuilder.append(currentChar);
                    } else {
                        placeholderBuilder.append(currentChar);
                        final String placeholder = placeholderBuilder.toString();
                        if (!placeholders.contains(placeholder)) {
                            placeholders.add(placeholder);
                        }
                        placeholderBuilder = new StringBuilder();
                        inPlaceholder = false;
                    }
                }
                escapeChar = false;
                // If we encounter a ] outside of a placeholder, it must be escaped, as otherwise it would be unmatched.
            } else if (currentChar == '\\') {
                if (escapeChar) {
                    if (inPlaceholder) {
                        placeholderBuilder.append(currentChar);
                    }
                    escapeChar = false;
                } else {
                    escapeChar = true;
                }
            } else {
                if (inPlaceholder) {
                    placeholderBuilder.append(currentChar);
                }
            }

        }

        return placeholders;
    }

    /**
     * The format string must not contain any validation errors as reported by
     * {@link OutputWriterValidatorHelper#getValidationErrors(String)}.
     * 
     * @param formatString   The string in which placeholders shall be replaced. Must not be null.
     * @param inputMap       A mapping from placeholders to the value they are to be replaced with. Must not be null.
     * @param timestamp      A string representation of the value to be used for the timestamp-placeholder. Must not be null.
     * @param executionCount The value with which the executionCount-placeholder is to be replaced
     * @return The format string in which all placeholders have been replaced with their actual values, if some such value is given.
     *         Otherwise, the placeholder is not replaced. Is never null.
     */
    public static String replacePlaceholders(final String formatString, Map<String, TypedDatum> inputMap, String timestamp,
        int executionCount) {
        final StringBuilder resultBuilder = new StringBuilder();

        final CharacterIterator formatStringIterator = new StringCharacterIterator(formatString);
        char currentChar = formatStringIterator.current();
        while (currentChar != CharacterIterator.DONE) {
            if (currentChar == '\\') {
                // Since we assume that the format string does not contain any validation errors, we can simply parse the escaped character
                // as-is and increment the iterator in order not to handle the same character twice
                final char nextChar = formatStringIterator.next();
                resultBuilder.append(nextChar);
            } else if (currentChar == '[') {
                final String placeholder = parsePlaceholder(formatStringIterator);
                switch (String.format("[%s]", placeholder)) {
                case OutputWriterComponentConstants.PH_LINEBREAK:
                    resultBuilder.append(LINE_SEP);
                    break;
                case OutputWriterComponentConstants.PH_TIMESTAMP:
                    resultBuilder.append(timestamp);
                    break;
                case OutputWriterComponentConstants.PH_EXECUTION_COUNT:
                    resultBuilder.append(Integer.toString(executionCount));
                    break;
                default:
                    if (inputMap.containsKey(placeholder)) {
                        resultBuilder.append(inputMap.get(placeholder));
                    } else {
                        resultBuilder.append(String.format("[%s]", placeholder));
                    }
                }
            } else {
                resultBuilder.append(currentChar);
            }
            currentChar = formatStringIterator.next();
        }
        return resultBuilder.toString();
    }

    /**
     * During the operation of this method, the given formatStringIterator is advanced such that it points to the closing square bracket of
     * the placeholder after termination of this method.
     * 
     * @param formatStringIterator An iterator through some format string, with formatStringIterator.current() returning '['
     * @return The placeholder starting at the opening bracket the formatStringIterator pointed to at the invocation of the method, without
     *         its enclosing square brackets.
     */
    private static String parsePlaceholder(final CharacterIterator formatStringIterator) {
        final StringBuilder resultBuilder = new StringBuilder();
        char currentChar;
        while ((currentChar = formatStringIterator.next()) != ']') {
            if (currentChar == '\\') {
                final char nextChar = formatStringIterator.next();
                resultBuilder.append(nextChar);
            } else {
                resultBuilder.append(currentChar);
            }
        }
        return resultBuilder.toString();
    }

    /**
     * @param formatString   A string potentially containing the placeholders [Timestamp] and [Execution count]. Must not be null.
     * @param timestamp      The value with which the placeholder [Timestamp] shall be replaced. Must not be null.
     * @param executionCount The value with which the placeholder [Execution count] shall be replaced
     * @return The given format string with the given values in place of the placeholders [Timestamp] and [Execution count]. Is never null.
     */
    public static String formatHeader(final String formatString, String timestamp, int executionCount) {
        return replacePlaceholders(formatString, new HashMap<>(), timestamp, executionCount);
    }
}
