/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.common.impl;

import java.io.IOException;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.model.NodeInformation;
import de.rcenvironment.core.communication.model.internal.NodeInformationHolder;
import de.rcenvironment.core.communication.model.internal.NodeInformationRegistryImpl;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * A {@link NodeIdentifier} based on a persistent globally unique identifier.
 * 
 * TODO 3.0: rename
 * 
 * @author Robert Mischke
 */
public class NodeIdentifierImpl implements NodeIdentifier {

    private static final long serialVersionUID = -82480269867222031L;

    private String idString;

    private transient NodeInformation metaInformationHolder;

    /**
     * Creates a {@link NodeIdentifier} object from the persistent platform id.
     * 
     * @param id the persistent id to use
     */
    public NodeIdentifierImpl(String id) {
        this.idString = id;
        // note: this is done internally as it the dependency is needed on deserialization anyway
        attachMetaInformationHolder();
    }

    @Override
    public String getIdString() {
        return idString;
    }

    @Override
    public String getAssociatedDisplayName() {
        if (metaInformationHolder == null) {
            return "<unknown>";
        }
        String displayName = metaInformationHolder.getDisplayName();
        if (displayName == null) {
            return "<unknown>";
        }
        return displayName;
    }

    /**
     * Creates an independent clone of this identifier.
     * 
     * @return the clone
     * 
     * @see java.lang.Object#clone()
     */
    @Override
    public NodeIdentifierImpl clone() {
        return new NodeIdentifierImpl(idString);
    }

    // hook into deserialization to attach the metadata information holder
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        attachMetaInformationHolder();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof NodeIdentifierImpl) {
            return idString.equals(((NodeIdentifierImpl) object).idString);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return idString.hashCode();
    }

    @Override
    public String toString() {
        String displayName = metaInformationHolder.getDisplayName();
        if (displayName == null) {
            displayName = "<unnamed>";
        }
        return StringUtils.format("\"%s\" [%s]", displayName, idString);
    }

    /**
     * Access method for unit tests.
     * 
     * @return the assigned {@link NodeInformationHolder}
     */
    protected NodeInformation getMetaInformationHolder() {
        return metaInformationHolder;
    }

    /**
     * Assigns the shared {@link NodeInformationHolder} to the internal field based on the node id.
     */
    private void attachMetaInformationHolder() {
        this.metaInformationHolder = NodeInformationRegistryImpl.getInstance().getNodeInformation(idString);
    }
}
