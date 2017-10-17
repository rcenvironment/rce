/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.toolkit.utils.text;

import java.util.List;

/**
 * A simple interface for collecting lines of text data.
 * 
 * @author Robert Mischke
 */
public interface TextLinesReceiver {

    /**
     * @param line a single line to add
     */
    void addLine(String line);

    /**
     * @param lines an array or varargs set of lines to add
     */
    void addLines(String... lines);

    /**
     * @param lines a {@link List} of lines to add
     */
    void addLines(List<String> lines);
}
