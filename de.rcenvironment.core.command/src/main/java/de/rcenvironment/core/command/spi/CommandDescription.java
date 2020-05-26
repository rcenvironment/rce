/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

/**
 * Represents a help entry for a single command provided by a {@link CommandPlugin}.
 * 
 * @author Robert Mischke
 */
public class CommandDescription implements Comparable<CommandDescription> {

    private final String staticPart;

    private final String dynamicPart;

    private final String firstLine;

    private final String[] additionalLines;

    private final boolean isDeveloperCommand;

    public CommandDescription(String staticPart, String dynamicPart, boolean isDeveloperCommand, String firstLine,
        String... additionalLines) {
        this.staticPart = staticPart;
        this.dynamicPart = dynamicPart;
        this.firstLine = firstLine;
        this.additionalLines = additionalLines;
        this.isDeveloperCommand = isDeveloperCommand;
    }

    public String getStaticPart() {
        return staticPart;
    }

    public String getDynamicPart() {
        return dynamicPart;
    }

    public String getFirstLine() {
        return firstLine;
    }

    public String[] getAdditionalLines() {
        return additionalLines;
    }

    public boolean isDeveloperCommand() {
        return isDeveloperCommand;
    }

    @Override
    public int compareTo(CommandDescription o) {
        return staticPart.compareTo(o.staticPart);
    }

    /**
     * @return true if the dynamic part is non-null and not empty
     */
    public boolean hasDynamicPart() {
        return dynamicPart != null && !dynamicPart.isEmpty();
    }

}
