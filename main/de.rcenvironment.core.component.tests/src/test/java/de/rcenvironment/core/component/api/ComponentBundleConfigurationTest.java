/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.api;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.rcenvironment.core.component.internal.ComponentBundleConfiguration;

/**
 * Test cases for {@link ComponentBundleConfigurationTest}.
 * 
 * @author Doreen Seider
 */
public class ComponentBundleConfigurationTest {

    /**
     * Test default values and setter.
     */
    @Test
    public void testDefaultValuesAndSetter() {
        ComponentBundleConfiguration config = new ComponentBundleConfiguration();

        assertEquals(0, config.getPublished().size());

        List<String> comps = new ArrayList<String>();
        comps.add("comp.id");

        config.setPublished(comps);

        assertEquals(1, config.getPublished().size());
    }

}
