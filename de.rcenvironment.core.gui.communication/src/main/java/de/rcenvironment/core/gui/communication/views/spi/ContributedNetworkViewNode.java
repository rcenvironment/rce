/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.communication.views.spi;


/**
 * Base interface for tree elements created and/or handled by {@link NetworkViewContributor}s.
 * 
 * @author Robert Mischke
 */
public interface ContributedNetworkViewNode {

    /**
     * @return the {@link NetworkViewContributor} that created this element, and/or is responsible for defining text/images/children for it
     */
    NetworkViewContributor getContributor();
}
