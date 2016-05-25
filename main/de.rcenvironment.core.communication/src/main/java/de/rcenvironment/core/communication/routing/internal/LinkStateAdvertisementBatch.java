/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.routing.internal;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.core.communication.common.NodeIdentifier;

/**
 * Grouping of multiple LSAs in one batch.
 * 
 * @author Phillip Kroll
 */
public class LinkStateAdvertisementBatch extends HashMap<NodeIdentifier, LinkStateAdvertisement> implements Serializable {

    private static final long serialVersionUID = -4462813588655196069L;

    public LinkStateAdvertisementBatch() {}

    public LinkStateAdvertisementBatch(Map<NodeIdentifier, ? extends LinkStateAdvertisement> m) {
        super(m);
    }

}
