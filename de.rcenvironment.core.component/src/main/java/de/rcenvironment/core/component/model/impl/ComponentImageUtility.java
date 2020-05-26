/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.model.impl;

import java.util.Collection;
import java.util.stream.Collectors;

import de.rcenvironment.core.component.management.api.DistributedComponentEntry;

/**
 * Utility class to provide helper methods for the component image api.
 * 
 * @author Dominik Schneider
 *
 */
public final class ComponentImageUtility {

    private static final int MINUS_ONE = -1;

    private ComponentImageUtility() {}

    /**
     * Cleans a component id of unnecessary elements like "missing" ("missing" occurs if a component is unavailable). Filter works with the
     * fact, that all components start with "de". If the component ids change, this has to be changed too.
     * 
     * @param componentId of a component
     * @return cleaned id without without additional information
     */
    public static String getNormalId(String componentId) {
        int idStart = componentId.indexOf("de");

        if (idStart != MINUS_ONE) {
            return componentId.substring(idStart, componentId.length());
        } else {
            return componentId;
        }
    }

    /**
     * This method removes all duplicates of installations. If there are identical components (identifier and version), the local will be
     * chosen. Otherwise the component whose nodeId is lexicographical smaller will be chosen.
     * 
     * @param inputCollection Collection of component installations which have to be distinct
     * @return a distinct collection of all installations
     */
    public static Collection<DistributedComponentEntry> getDistinctInstallations(Collection<DistributedComponentEntry> inputCollection) {
        // the stream puts all Entries in a map, the ID+version is the key. The map is used to guarantee that there is just one installation
        // per ID. Afterwards just the values are used.
        return inputCollection.stream().collect(
            Collectors.toMap(entry -> entry.getComponentInterface().getIdentifierAndVersion(), entry -> entry, (entryA, entryB) -> {

                if (entryA.getType().isLocal() && !entryB.getType().isLocal()) {
                    return entryA;
                } else if (!entryA.getType().isLocal() && entryB.getType().isLocal()) {
                    return entryB;
                } else if (!entryA.getType().isLocal() && !entryB.getType().isLocal()) {
                    if (entryA.getComponentInterface().getIdentifierAndVersion()
                        .compareTo(entryB.getComponentInterface().getIdentifierAndVersion()) <= 0) {
                        return entryA;
                    } else {
                        return entryB;
                    }
                } else {
                    // this case, that there two local versions of the same tool is forbidden by design and must not occur
                    return null;
                }
            })).values();
    }
}
