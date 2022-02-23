/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.utils.internal;

import java.util.IllegalFormatException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Minimal subset of the RCE utility class de.rcenvironment.core.utils.common.StringUtils, only providing the methods required for migrating
 * classes into the toolkit.
 * 
 * TODO reduce to actual authors list
 * 
 * @author Doreen Seider
 * @author Sascha Zur
 * @author Robert Mischke
 * @author Marc Stammerjohann
 */
public final class StringUtils {

    /** Separator used to separate format string and values to be used for a readable fall back message. */
    protected static final String FORMAT_SEPARATOR = ", ";

    private static final Log sharedLog = LogFactory.getLog(StringUtils.class);

    private StringUtils() {}

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

}
