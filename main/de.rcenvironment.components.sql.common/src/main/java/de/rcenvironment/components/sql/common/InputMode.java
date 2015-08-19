/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.sql.common;

/**
 * Input mode options.
 * 
 * @author Christian Weiss
 */
public enum InputMode {

    /** Block mode. */
    BLOCK(Messages.inputModeBlockLabel);

    private final String label;

    private InputMode(final String label) {
        if (label == null || label.isEmpty()) {
            throw new IllegalArgumentException();
        }
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Returns the {@link InputMode} with the specified label. The label must match exactly the
     * label of an existing {@link InputMode}.
     * 
     * @param label the label of the {@link InputMode} to return
     * @return the {@link InputMode} with the specified label
     */
    public static InputMode valueOfLabel(final String label) {
        for (final InputMode mode : values()) {
            if (label.equals(mode.getLabel())) {
                return mode;
            }
        }
        throw new IllegalArgumentException(String.format("No Mode with label '%s'.", label));
    }

    /**
     * Returns a <code>String</code> array of the labels of all {@link InputMode}s.
     * 
     * @return the labels of all {@link InputMode}s.
     */
    public static String[] getLabels() {
        final InputMode[] values = values();
        final String[] labels = new String[values.length];
        for (int index = 0; index < values.length; ++index) {
            final InputMode mode = values[index];
            labels[index] = mode.getLabel();
        }
        return labels;
    }

}
