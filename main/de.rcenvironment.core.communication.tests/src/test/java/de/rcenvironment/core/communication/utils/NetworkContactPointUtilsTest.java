/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.rcenvironment.core.communication.model.NetworkContactPoint;

/**
 * {@link NetworkContactPointUtils} test.
 * 
 * @author Robert Mischke
 */
public class NetworkContactPointUtilsTest {

    private static final String DUMMY_BASIC_NCP = "tr:h:5";

    private static final String DUMMY_TRANSPORT_ID = "tr";

    private static final String DUMMY_HOST = "h";

    private static final int DUMMY_PORT = 5;

    private static final String KEY1 = "key1";

    private static final String KEY2 = "key2";

    private static final String VAL1 = "val1";

    private static final String VAL2 = "val2";

    /**
     * Test for proper attribute parsing. Attribute syntax: "transport:host:port(key1=val1,key2=val2)".
     */
    @Test
    public void testAttributeParsing() {
        NetworkContactPoint ncp;
        ncp = NetworkContactPointUtils.parseStringRepresentation(DUMMY_BASIC_NCP);
        testCommonEntries(ncp);
        assertEquals(0, ncp.getAttributes().size());
        ncp = NetworkContactPointUtils.parseStringRepresentation(DUMMY_BASIC_NCP + "()");
        testCommonEntries(ncp);
        assertEquals(0, ncp.getAttributes().size());
        ncp = NetworkContactPointUtils.parseStringRepresentation(DUMMY_BASIC_NCP + "(key1=val1)");
        testCommonEntries(ncp);
        assertEquals(1, ncp.getAttributes().size());
        assertEquals(VAL1, ncp.getAttributes().get(KEY1));
        ncp = NetworkContactPointUtils.parseStringRepresentation(DUMMY_BASIC_NCP + "  (key1=val1,key2=val2)");
        testCommonEntries(ncp);
        assertEquals(2, ncp.getAttributes().size());
        assertEquals(VAL1, ncp.getAttributes().get(KEY1));
        assertEquals(VAL2, ncp.getAttributes().get(KEY2));
        // test most garbled legal string
        ncp = NetworkContactPointUtils.parseStringRepresentation(DUMMY_BASIC_NCP + "( key1  = val1 , key2=val2, key 3=,key4 = )");
        testCommonEntries(ncp);
        assertEquals(4, ncp.getAttributes().size());
        assertEquals(VAL1, ncp.getAttributes().get(KEY1));
        assertEquals(VAL2, ncp.getAttributes().get(KEY2));
        assertEquals("", ncp.getAttributes().get("key 3"));
        assertEquals("", ncp.getAttributes().get("key4"));
    }

    private void testCommonEntries(NetworkContactPoint ncp) {
        assertEquals(DUMMY_TRANSPORT_ID, ncp.getTransportId());
        assertEquals(DUMMY_HOST, ncp.getHost());
        assertEquals(DUMMY_PORT, ncp.getPort());
    }

}
