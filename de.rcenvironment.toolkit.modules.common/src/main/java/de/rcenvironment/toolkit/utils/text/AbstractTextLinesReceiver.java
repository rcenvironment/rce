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
 * A base class for the common case where a class implementing {@link TextLinesReceiver} only wants to define the handling of each
 * individual text line. For each line sent to one of the multi-line methods, the {@link #addLine(String)} implementation of the subclass is
 * invoked. To prevent unexpected behavior, all multi-line method implementations are <code>final</code>.
 * <p>
 * In cases where the implementing class can provide a more efficient multi-line processing than this, it should implement the
 * {@link TextLinesReceiver} interface directly.
 * 
 * @author Robert Mischke
 */
public abstract class AbstractTextLinesReceiver implements TextLinesReceiver {

    @Override
    public void addLines(final String... lines) {
        for (String line : lines) {
            addLine(line);
        }
    }

    @Override
    public void addLines(final List<String> lines) {
        for (String line : lines) {
            addLine(line);
        }
    }

}
