/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.authorization.impl;

import de.rcenvironment.core.component.authorization.api.NamedComponentAuthorizationSelector;

/**
 * {@link NamedComponentAuthorizationSelector} implementation.
 *
 * @author Robert Mischke
 */
public class NamedComponentAuthorizationSelectorImpl extends ComponentAuthorizationSelectorImpl
    implements NamedComponentAuthorizationSelector {

    private final String displayName;

    public NamedComponentAuthorizationSelectorImpl(String identifier, String displayName) {
        super(identifier);
        this.displayName = displayName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public int compareTo(NamedComponentAuthorizationSelector o) {
        // note: if necessary in the future, this could be extended to include sort criteria like "component type"
        return displayName.compareTo(o.getDisplayName());
    }

    @Override
    public int compareToIgnoreCase(NamedComponentAuthorizationSelector o) {
        return displayName.compareToIgnoreCase(o.getDisplayName());
    }

}
