/*
 * Copyright 2021-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.palette;

import java.util.Collection;
import java.util.stream.Collectors;

import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.management.api.DistributedComponentEntryType;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.utils.common.StringUtils;

class DistributedComponentKnowledgeSanitizer {

    public Collection<DistributedComponentEntry> sanitizeComponentKnowledge(DistributedComponentKnowledge newState) {
        return newState.getAllInstallations().stream().filter(this::isValid).collect(Collectors.toSet());
    }

    private boolean isValid(DistributedComponentEntry entry) {
        if (entry.getDisplayName() == null) {
            throw new IllegalArgumentException(
                StringUtils.format("Received %s with unexpected display name: null.", entry.getClass().getCanonicalName()));
        }
        if (entry.getComponentInstallation().getInstallationId() == null) {
            throw new IllegalArgumentException(
                StringUtils.format("Received tool \"%s\" with unexpected installation id: null.", entry.getDisplayName()));
        }
        if (entry.getType() == null) {
            throw new IllegalArgumentException(StringUtils.format("Received tool \"%s\" with unexpected %s: null.", entry.getDisplayName(),
                DistributedComponentEntryType.class.getCanonicalName()));
        }
        if (entry.getComponentInstallation().getComponentInterface() == null) {
            throw new IllegalArgumentException(
                StringUtils.format("Received tool \"%s\" with unexpected %s: null.", entry.getDisplayName(),
                    ComponentInterface.class.getCanonicalName()));
        }
        ComponentInterface componentInterface = entry.getComponentInstallation().getComponentInterface();
        if (componentInterface.getVersion() == null) {
            throw new IllegalArgumentException(
                StringUtils.format("Received %s with unexpected version: null.", ComponentInterface.class.getCanonicalName()));
        }
        if (componentInterface.getIcon16() == null) {
            throw new IllegalArgumentException(
                StringUtils.format("Received %s with unexpected icon: null.", ComponentInterface.class.getCanonicalName()));
        }
        return true;
    }
}
