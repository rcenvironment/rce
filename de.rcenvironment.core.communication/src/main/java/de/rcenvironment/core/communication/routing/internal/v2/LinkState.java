/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.routing.internal.v2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Represents the parsed link state of a node. This class is immutable.
 * 
 * @author Robert Mischke
 */
public final class LinkState {

    private final List<Link> links;

    public LinkState(Collection<Link> newLinks) {
        links = Collections.unmodifiableList(new ArrayList<Link>(newLinks));
    }

    public List<Link> getLinks() {
        return links;
    }

    @Override
    public String toString() {
        return "OutLinks=" + links.toString();
    }
}
