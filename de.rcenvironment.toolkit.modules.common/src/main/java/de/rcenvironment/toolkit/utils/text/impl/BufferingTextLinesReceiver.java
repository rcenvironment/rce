/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.utils.text.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.rcenvironment.toolkit.utils.text.TextLinesReceiver;

/**
 * Simple {@link TextLinesReceiver} implementation that collects the lines in an internal {@link List}.
 * <p>
 * IMPORTANT: To avoid overhead in typical use cases, the methods of this class are <b>NOT</b> thread-safe!
 * 
 * @author Robert Mischke
 */
public class BufferingTextLinesReceiver implements TextLinesReceiver {

    private final List<String> buffer = new ArrayList<>();

    @Override
    public void addLine(String line) {
        buffer.add(line);
    }

    @Override
    public void addLines(String... lines) {
        // alternative: addAll(Arrays.asList()) - would require benchmarking to find out
        for (String line : lines) {
            addLine(line);
        }
    }

    @Override
    public void addLines(List<String> lines) {
        buffer.addAll(lines);
    }

    /**
     * @return a link to the internal list (not a detached copy/snapshot)
     */
    public List<String> getCollectedLines() {
        return Collections.unmodifiableList(buffer);
    }

}
