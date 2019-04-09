/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.model.internal;

import java.io.PrintStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import de.rcenvironment.core.communication.model.NodeInformationRegistry;
import de.rcenvironment.core.communication.model.SharedNodeInformationHolder;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.toolkit.modules.concurrency.utils.ThreadsafeAutoCreationMap;

/**
 * Central registry for information gathered about nodes.
 * 
 * @author Robert Mischke
 */
public class NodeInformationRegistryImpl implements NodeInformationRegistry {

    private final ThreadsafeAutoCreationMap<String, SharedNodeInformationHolderImpl> threadSafeIdToHolderAutoMap =
        new ThreadsafeAutoCreationMap<String, SharedNodeInformationHolderImpl>() {

            @Override
            protected SharedNodeInformationHolderImpl createNewEntry(String key) {
                return new SharedNodeInformationHolderImpl();
            }

        };

    @Override
    public SharedNodeInformationHolder getNodeInformationHolder(String id) {
        return getMutableNodeInformationHolder(id);
    }

    @Override
    public void printAllNameAssociations(PrintStream output, String introText) {
        if (introText != null) {
            output.println(introText);
        }
        final Map<String, SharedNodeInformationHolderImpl> snapshot = new TreeMap<>(threadSafeIdToHolderAutoMap.getShallowCopy()); // sorted
        for (Entry<String, SharedNodeInformationHolderImpl> entry : snapshot.entrySet()) {
            final String stringValue = entry.getValue().getDisplayName();
            if (stringValue != null) {
                output.println(StringUtils.format("  %s -> \"%s\"", entry.getKey(), stringValue));
            } else {
                output.println(StringUtils.format("  %s -> <null>", entry.getKey()));
            }
        }
    }

    /**
     * Provides direct, write-enabled access to {@link SharedNodeInformationHolderImpl}s. Not part of the {@link NodeInformationRegistry}
     * interface as it is intended for bundle-internal use only.
     * 
     * @param id the id of the relevant node
     * @return the writable {@link SharedNodeInformationHolderImpl}
     */
    public SharedNodeInformationHolderImpl getMutableNodeInformationHolder(String id) {
        return threadSafeIdToHolderAutoMap.get(id);
    }

}
