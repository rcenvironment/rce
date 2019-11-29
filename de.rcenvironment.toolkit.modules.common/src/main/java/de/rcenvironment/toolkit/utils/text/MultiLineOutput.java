/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.utils.text;

import java.util.List;

/**
 * An interface for containers of multi-line text data, with convenience functions for formatting/aggregating them.
 * 
 * @author Robert Mischke
 */
public interface MultiLineOutput {

    /**
     * @return the individual lines (read-only)
     */
    List<String> getLines();

    /**
     * @return true if there is at least one line of text (regardless of whether it is empty or not)
     */
    boolean hasContent();

    /**
     * @return output equal to {@link #asMultilineString("", "\n")}
     */
    String asMultilineString();

    /**
     * @param indent the prefix to add to each line
     * @param lineSeparator the separator to add after each line except the last
     * 
     * @return output equal to {@link #asMultilineString(null, indent, lineSeparator, null)}
     */
    String asMultilineString(String indent, String lineSeparator);

    /**
     * Formats the lines into a String by the following concatenation:
     * <ul>
     * <li>if "intro" is not null, the intro plus a line separator (unless it is the last line)
     * <li>for each line of content: the "indent" string plus the line, followed by a line separator unless this is the last line <b>and</b>
     * "outro" is null
     * <li>if "outro" is not null, the outro string
     * </ul>
     * In effect, the generated string does never end with the given line separator.
     * 
     * @param intro a string to prefix the line data with; set to null to disable
     * @param indent the prefix to add to each line
     * @param lineSeparator the separator to add after each line except the last
     * @param outro a string to append after the line data; set to null to disable
     * @return the aggregated output; see method description for its format
     */
    String asMultilineString(String intro, String indent, String lineSeparator, String outro);

}
