/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.model.impl;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Tobias Rodehutskors
 */
public class ComponentInterfaceImplTest {

    private byte[] icon1 = new byte[] { 1 };

    private String computeIconHash(byte[] icon16, byte[] icon24, byte[] icon32) {
        ComponentInterfaceImpl ci = new ComponentInterfaceImpl();

        ci.setIcon16(icon16);
        ci.setIcon24(icon24);
        ci.setIcon32(icon32);

        return ci.getIconHash();
    }

    /**
     * Tests if the icon hash can be calculated for null references.
     */
    @Test
    public void testIconHashWithNullIcon() {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", computeIconHash(null, null, null));
    }
    
    /**
     * Tests if the icon hash is different, if ...
     */
    @Ignore("not implemented yet")
    @Test
    public void testIconHashWithDifferentOrdering() {
        assertThat(computeIconHash(null, icon1, icon1), is(not(computeIconHash(icon1, null, icon1))));
    }
}
