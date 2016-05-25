/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.communication.views.spi;

/**
 * Extension for {@link ContributedNetworkViewNode}s that can determine their parent node themselves. If a node implements this interface,
 * this method is called directly instead of querying its {@link NetworkViewContributor}, so {@link NetworkViewContributor}s do not need to
 * forward the getParent() call.
 * 
 * @author Robert Mischke
 */
public interface ContributedNetworkViewNodeWithParent extends ContributedNetworkViewNode {

    /**
     * @return the parent tree node of this node
     */
    Object getParentNode();
}
