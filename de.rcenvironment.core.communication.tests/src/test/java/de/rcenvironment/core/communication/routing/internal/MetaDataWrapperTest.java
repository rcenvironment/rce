/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.communication.routing.internal;

import java.util.Map;

import junit.framework.TestCase;
import de.rcenvironment.core.communication.protocol.MessageMetaData;

/**
 * Unit tests for {@link MessageMetaData}.
 * 
 * @author Phillip Kroll
 * 
 */
public class MetaDataWrapperTest extends TestCase {

    /**
     * Simple testcase.
     */
    public final void testCreate() {
        MessageMetaData data1 = new MessageMetaData();
        MessageMetaData data2 = new MessageMetaData();

        // TODO restore unit test

        // assertTrue(data1.matches(data2.getInnerMap()));
        // assertTrue(data1.matches(data2));
        //
        // data1.setCategoryRouting();
        //
        // assertFalse(data1.matches(data2));
        // assertTrue(data2.matches(data1));
        //
        // data2.setCategoryRouting();
        //
        // assertTrue(data1.matches(data2));
        // assertTrue(data2.matches(data1));
        //
        // data2.setTopicLsa();
        //
        // assertTrue(data1.matches(data2));
        // assertFalse(data2.matches(data1));
        //
        // data1.setTopicRouted();
        //
        // assertFalse(data1.matches(data2));
        // assertFalse(data2.matches(data1));
        //
        // data1.setTopicLsa();
        //
        // assertTrue(data1.matches(data2));
        // assertTrue(data2.matches(data1));

    }

    /**
     * Tests {@link MessageMetaData#getHopCount() and MetaDataWrapper#incHopCount().
     */
    public final void testHopCount() {

        Map<String, String> metaData = MessageMetaData.create().getInnerMap();
        assertEquals(0, MessageMetaData.wrap(metaData).getHopCount());
        MessageMetaData.wrap(metaData).incHopCount();
        assertEquals(1, MessageMetaData.wrap(metaData).getHopCount());

    }

}
