/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.toolkit.utils.text.impl;

import java.util.Collections;
import java.util.List;

import de.rcenvironment.toolkit.utils.text.MultiLineOutput;

/**
 * Simple default {@link MultiLineOutput} implementation wrapping a given {@link List} of {@link String}s. The given list object is
 * referenced internally (not cloned/detached), so changes to the list will affect the generated output.
 * 
 * @author Robert Mischke
 */
public class MultiLineOutputWrapper implements MultiLineOutput {

    private List<String> lines;

    public MultiLineOutputWrapper(List<String> lines) {
        this.lines = lines;
    }

    @Override
    public List<String> getLines() {
        return Collections.unmodifiableList(lines);
    }

    @Override
    public String asMultilineString() {
        return asMultilineString("", "\n");
    }

    @Override
    public String asMultilineString(String indent, String lineSeparator) {
        return asMultilineString(null, indent, lineSeparator, null);
    }

    @Override
    public String asMultilineString(String intro, String indent, String lineSeparator, String outro) {
        StringBuilder buffer = new StringBuilder();
        if (intro != null) {
            buffer.append(intro);
            buffer.append(lineSeparator);
        }
        for (String line : lines) {
            buffer.append(indent);
            buffer.append(line);
            buffer.append(lineSeparator);
        }
        if (outro != null) {
            buffer.append(outro);
            buffer.append(lineSeparator);
        }
        // remove last line separator
        if (buffer.length() >= lineSeparator.length()) {
            buffer.setLength(buffer.length() - lineSeparator.length());
        }
        return buffer.toString();
    }

    @Override
    public boolean hasContent() {
        return !lines.isEmpty();
    }
}
