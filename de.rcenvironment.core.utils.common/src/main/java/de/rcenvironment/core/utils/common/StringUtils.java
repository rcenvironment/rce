/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class for {@link String} objects.
 * 
 * @author Doreen Seider
 * @author Sascha Zur
 * @author Robert Mischke
 * @author Marc Stammerjohann
 */
public final class StringUtils {

    /**
     * The human-readable error message for a charset violation.
     */
    public static final String COMMON_VALID_INPUT_CHARSET_ERROR =
        "Invalid character: only letters, digits, space, and the characters _.,-+() are allowed";

    /**
     * The human-readable error message for an invalid first character.
     */
    public static final String COMMON_VALID_INPUT_FIRST_CHARACTER_ERROR =
        "Invalid first character: it must be a letter, a digit, or the underscore (\"_\")";

    /**
     * The human-readable error message for an invalid last character.
     */
    public static final String COMMON_VALID_INPUT_LAST_CHARACTER_ERROR = "Invalid last character";

    /**
     * A regular expression to check whether a given string fits the "common valid input" charset. Empty strings are accepted by this
     * regexp.
     */
    protected static final Pattern COMMON_VALID_INPUT_CHARSET_REGEXP = Pattern.compile("^[a-zA-Z0-9 _\\.,\\-\\+\\(\\)]*$");

    /**
     * A regular expression to check whether a given first character fits the "common valid input" rules.
     */
    protected static final Pattern COMMON_VALID_INPUT_FIRST_CHARACTER_REGEXP = Pattern.compile("^[a-zA-Z0-9_].*");

    /**
     * A regular expression to check whether a given last character fits the "common valid input" rules.
     */
    protected static final Pattern COMMON_VALID_INPUT_LAST_CHARACTER_REGEXP = Pattern.compile(".*[^ ]$"); // "^ " = no space

    /** Separator used to separate two semantically different Strings put into an single one. */
    protected static final String SEPARATOR = ":";

    /** Character used to escape the separator. */
    protected static final String ESCAPE_CHARACTER = "\\";

    /** Separator used to separate format string and values to be used for a readable fall back message. */
    protected static final String FORMAT_SEPARATOR = ", ";

    /**
     * Represents an empty array after concatenization.
     * 
     * TODO use "\[]" for better readability? "\" could still be parsed for compatibility - misc_ro
     */
    protected static final String EMPTY_ARRAY_PLACEHOLDER = ESCAPE_CHARACTER;

    /**
     * Represents a null string as part of a serialized and concatenated array.
     */
    protected static final String NULL_STRING_PLACEHOLDER = ESCAPE_CHARACTER + "0"; // "\0"

    private static final Log sharedLog = LogFactory.getLog(StringUtils.class);

    private StringUtils() {}

    private static String escapeCharacter(String rawString, char characterToEscape) {
        final StringBuilder result = new StringBuilder();
        final StringCharacterIterator iterator = new StringCharacterIterator(rawString);
        char character = iterator.current();
        while (character != CharacterIterator.DONE) {
            if (character == characterToEscape) {
                result.append(ESCAPE_CHARACTER);
                result.append(characterToEscape);
            } else {
                result.append(character);
            }
            character = iterator.next();
        }
        return result.toString();
    }

    private static String unescapeCharacter(String escapedString, char characterToUnescape) {
        final StringBuilder result = new StringBuilder();
        final StringCharacterIterator iterator = new StringCharacterIterator(escapedString);
        char character = iterator.current();
        while (character != CharacterIterator.DONE) {
            if (character == ESCAPE_CHARACTER.toCharArray()[0]) {
                character = iterator.next();
                if (character == characterToUnescape) {
                    result.append(characterToUnescape);
                } else {
                    result.append(ESCAPE_CHARACTER);
                    result.append(character);
                }
            } else {
                result.append(character);
            }
            character = iterator.next();
        }
        return result.toString();
    }

    /**
     * Escapes the separator within the given String object.
     * 
     * @param rawString The {@link String} to escape.
     * @return the escaped {@link String}.
     */
    public static String escapeSeparator(String rawString) {
        return escapeCharacter(rawString, SEPARATOR.toCharArray()[0]);
    }

    /**
     * Replaces in the given {@link String} escaped separator with the separator itself.
     * 
     * @param escapedString The {@link String} to unescape.
     * @return the unescaped {@link String}.
     */
    public static String unescapeSeparator(String escapedString) {
        return unescapeCharacter(escapedString, SEPARATOR.toCharArray()[0]);
    }

    /**
     * Splits the given {@link String} around the separator.
     * 
     * @param completeString the {@link String} to split.
     * @return the splitted String as array.
     */
    public static String[] splitAndUnescape(String completeString) {

        StringBuilder part = new StringBuilder();
        List<String> parts = new ArrayList<String>();
        int escapeCount = 0;

        // special case: empty array
        if (completeString.equals(EMPTY_ARRAY_PLACEHOLDER)) {
            return new String[0];
        }

        for (int i = 0; i < completeString.length(); i++) {
            if (completeString.charAt(i) == ESCAPE_CHARACTER.charAt(0)) {
                escapeCount++;
                part.append(completeString.charAt(i));
            } else if (completeString.charAt(i) == SEPARATOR.charAt(0)) {
                if (escapeCount % 2 == 0) {
                    parts.add(part.toString());
                    part = new StringBuilder();
                } else {
                    part.append(completeString.charAt(i));
                }
                escapeCount = 0;
            } else {
                part.append(completeString.charAt(i));
                escapeCount = 0;
            }
        }
        parts.add(part.toString());
        String[] partsArray = new String[parts.size()];
        partsArray = parts.toArray(partsArray);
        for (int i = 0; i < partsArray.length; i++) {
            partsArray[i] = unwrap(partsArray[i]);
        }
        return partsArray;
    }

    /**
     * Strings the given parts together to one String and escapes separator if needed.
     * 
     * @param parts the given String parts which needs to string together.
     * @return The String containing all parts separated by an separator.
     */
    public static String escapeAndConcat(String... parts) {
        // special case: empty array
        if (parts.length == 0) {
            return EMPTY_ARRAY_PLACEHOLDER;
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (String part : parts) {
            String escapedPart = wrap(part);
            stringBuilder.append(escapedPart);
            stringBuilder.append(SEPARATOR);
        }
        return stringBuilder.substring(0, stringBuilder.length() - 1);
    }

    /**
     * Strings the given parts together to one String and escapes separator if needed.
     * 
     * @param parts the given String parts which needs to string together.
     * @return The String containing all parts separated by an separator.
     */
    public static String escapeAndConcat(List<String> parts) {
        return escapeAndConcat(parts.toArray(new String[parts.size()]));
    }

    /**
     * Escapes the given string so it can be directly embedded into a JSON file as a string value.
     * 
     * @param input the input string
     * @param addDoubleQuotes whether the returned value should be enclosed in double quotes; if false, the returned content MUST be placed
     *        between existing double quotes!
     * @return the escaped string
     */
    public static String escapeAsJsonStringContent(String input, boolean addDoubleQuotes) {
        String escaped = input.replace("\\", "\\\\").replace("\"", "\\\"");
        if (addDoubleQuotes) {
            return format("\"%s\"", escaped);
        } else {
            return escaped;
        }
    }

    /**
     * Checks whether the given string is either empty, or is comprised only of characters from the "common valid input set" (see
     * {@link #COMMON_VALID_INPUT_CHARSET_ERROR} for a human-readable description).
     * 
     * @param input the input string to test
     * @return null if successful, or a human-readable error message on violations
     */
    public static String checkAgainstCommonInputRules(String input) {
        if (input.isEmpty()) {
            return null;
        }
        if (!COMMON_VALID_INPUT_CHARSET_REGEXP.matcher(input).matches()) {
            return COMMON_VALID_INPUT_CHARSET_ERROR;
        }
        if (!COMMON_VALID_INPUT_FIRST_CHARACTER_REGEXP.matcher(input).matches()) {
            return COMMON_VALID_INPUT_FIRST_CHARACTER_ERROR;
        }
        if (!COMMON_VALID_INPUT_LAST_CHARACTER_REGEXP.matcher(input).matches()) {
            return COMMON_VALID_INPUT_LAST_CHARACTER_ERROR;
        }
        return null; // passed
    }

    /**
     * Returns the given string instance as a non-null reference as result. If the given string reference is a null reference an empty
     * string "" will be returned.
     * 
     * @param text the text
     * @return a valid <code>String</code> instance, an empty string if the parameter was a null reference
     */
    public static String nullSafe(final String text) {
        return nullSafe(text, "");
    }

    /**
     * Returns the given string instance as a non-null reference as result. If the given string reference is a null reference the default
     * value will be returned.
     * 
     * @param text the text
     * @param defaultValue the default value to return if the text is a null reference
     * @return a valid <code>String</code> instance, an empty string if the parameter was a null reference
     */
    public static String nullSafe(final String text, final String defaultValue) {
        if (text != null) {
            return text;
        } else {
            return defaultValue;
        }
    }

    /**
     * Tries to parse an integer from the given string (using {@link Integer#parseInt(String)}), and returns the given default value if a
     * {@link NumberFormatException} occurs.
     * 
     * @param input the string to parse
     * @param defaultValue the default value to use on error
     * @return the parsed value, or the default
     */
    public static int nullSafeParseInt(String input, int defaultValue) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the {@link #toString()} output of the given {@link Object}, or the default value if it is null.
     * 
     * @param reference an object or null
     * @param defaultValue the value to return if the first parameter is null
     * @return the {@link #toString()} output of the given {@link Object}, or the default value if it is null
     */
    public static String nullSafeToString(Object reference, String defaultValue) {
        if (reference != null) {
            return reference.toString();
        } else {
            return defaultValue;
        }
    }

    /**
     * Performs all escape steps so the string is ready for concatenation.
     * 
     * @param input the original string
     * @return the escaped/wrapped representation
     */
    private static String wrap(String input) {
        // special case: null string
        if (input == null) {
            return NULL_STRING_PLACEHOLDER;
        }

        String escapedPart = StringUtils.escapeCharacter(input, ESCAPE_CHARACTER.toCharArray()[0]);
        escapedPart = StringUtils.escapeSeparator(escapedPart);
        return escapedPart;
    }

    /**
     * Reverts all escape steps to retrieve the original string from a part that resulted from splitting.
     * 
     * @param wrapped the escaped/wrapped representation
     * @return the original string
     */
    private static String unwrap(String wrapped) {
        // special case: null string
        if (NULL_STRING_PLACEHOLDER.equals(wrapped)) {
            return null;
        }

        String temp = unescapeSeparator(wrapped);
        temp = unescapeCharacter(temp, ESCAPE_CHARACTER.toCharArray()[0]);
        return temp;
    }

    /**
     * Fault tolerant implementation of {@link String#format(String, Object...)}. If the {@link IllegalFormatException} is thrown, the raw
     * format string is concatenated with the values.
     * 
     * @param format A format string
     * @param args Arguments to replace the placeholder in the format string
     * 
     * @return a formatted string or a concatenated string
     */
    public static String format(String format, Object... args) {
        String result = null;
        try {
            result = String.format(format, args);
        } catch (IllegalFormatException e) {
            String values = "";
            for (int i = 0; i < args.length; i++) {
                if (i == 0) {
                    values = values.concat(String.valueOf(args[i]));
                } else {
                    values = values.concat(FORMAT_SEPARATOR + String.valueOf(args[i]));
                }
            }
            result = format;
            if (!values.isEmpty()) {
                result = result.concat(FORMAT_SEPARATOR + values);
            }
            sharedLog.warn(StringUtils.format(
                "Format error. Review the format string and the number of values.\n Format String: %s" + FORMAT_SEPARATOR
                    + "Values: %s", format, values));
        }
        return result;
    }

    /**
     * @param input the string to test
     * @return true if the string is null or empty
     */
    public static boolean isNullorEmpty(String input) {
        return input == null || input.isEmpty();
    }

}
