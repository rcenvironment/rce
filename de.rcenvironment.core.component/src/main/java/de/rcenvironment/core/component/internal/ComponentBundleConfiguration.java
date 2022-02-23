/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.internal;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for the component {@link Bundle}.
 * 
 * @author Doreen Seider
 */
public class ComponentBundleConfiguration {

    private List<String> published = new ArrayList<String>();

    public List<String> getPublished() {
        return published;
    }

    public void setPublished(List<String> components) {
        this.published = components;
    }

    // TODO >6.0.0 rework; bridge code to map the new configuration layout onto the old java bean
    public List<String> getComponents() {
        return published;
    }

    // TODO >6.0.0 rework; bridge code to map the new configuration layout onto the old java bean
    public void setComponents(List<String> components) {
        this.published = components;
    }
}
