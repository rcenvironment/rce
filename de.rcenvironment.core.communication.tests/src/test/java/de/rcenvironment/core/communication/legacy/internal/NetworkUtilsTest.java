/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.legacy.internal;

import junit.framework.TestCase;

/**
 * Unit tests for <code>NetworkUtils</code>.
 * 
 * @author Heinrich Wendel
 */
public class NetworkUtilsTest extends TestCase {

    /**
     * Test isHostInNetwork for success.
     */
    public void testIsHostInNetworkForSuccess() {
        NetworkUtils.isHostInNetwork("192.168.0.2", "192.168.0.0/24");
        NetworkUtils.isHostInNetwork("192.168.0.1", "192.168.0.0");
        NetworkUtils.isHostInNetwork("abc2", "abc2");
        NetworkUtils.isHostInNetwork("abc3", "192.168.0.0");
        NetworkUtils.isHostInNetwork("abc", "192.168.0.0/24");
        NetworkUtils.isHostInNetwork("192.168.0.1", "abc");
    }

    /**
     * Test addressToByte for success.
     */
    public void testAddressToByteForSuccess() {
        NetworkUtils.addressToByte("192.168.0.15");
    }

    /**
     * tests convertFromCidrToNetmask for success.
     */
    public void testConvertFromCidrToNetmaskForSuccess() {
        NetworkUtils.convertFromCidrToNetmask("32");
    }

    /**
     * Test isHostInNetwork for failure.
     */
    public void testIsHostInNetworkForFailure() {
        try {
            NetworkUtils.isHostInNetwork(null, "192.168.1.0/24");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            NetworkUtils.isHostInNetwork("192.168.0.3", null);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        assertFalse(NetworkUtils.isHostInNetwork("192.168.02", "192.168.2.0/24"));

        assertFalse(NetworkUtils.isHostInNetwork("192.168.0.5", "192.168.0.0/"));
    }

    /**
     * Test addressToByte for failure.
     */
    public void testAddressToByteForFailure() {
        try {
            NetworkUtils.addressToByte(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            NetworkUtils.addressToByte("192.168.2");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * tests convertFromCidrToNetmask for failure.
     */
    public void testConvertFromCidrToNetmaskForFailure() {
        try {
            NetworkUtils.convertFromCidrToNetmask(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            NetworkUtils.convertFromCidrToNetmask("adsf");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            NetworkUtils.convertFromCidrToNetmask("35");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * Test isHostInNetwork for sanity.
     */
    public void testIsHostInNetworkForSanity() {
        assertTrue(NetworkUtils.isHostInNetwork("192.168.0.2", "192.168.0.0/16"));
        assertTrue(NetworkUtils.isHostInNetwork("192.167.1.2", "192.167.0.0/16"));
        assertFalse(NetworkUtils.isHostInNetwork("192.168.1.2", "192.169.0.0/16"));
        assertTrue(NetworkUtils.isHostInNetwork("abcdf", "abcdf"));
        assertFalse(NetworkUtils.isHostInNetwork("abcd", "abcd.de"));
        assertFalse(NetworkUtils.isHostInNetwork("192.168.1.2", "abcd.def"));
        assertFalse(NetworkUtils.isHostInNetwork("gsd", "192.168.0.0/16"));
        assertFalse(NetworkUtils.isHostInNetwork("gsda", "192.169.0.0/16"));
        assertFalse(NetworkUtils.isHostInNetwork("gsd", "192.169.0.0"));
    }

    /**
     * Test addressToByte for sanity.
     */
    public void testAddressToByteForSanity() {
        byte[] address = new byte[4];
        final int oneninetwo = 192;
        final int onesixeight = 168;
        address[0] = new Integer(oneninetwo).byteValue();
        address[1] = new Integer(onesixeight).byteValue();
        address[2] = new Integer(0).byteValue();
        address[3] = new Integer(7).byteValue();

        byte[] addressGot = NetworkUtils.addressToByte("192.168.0.7");
        for (int i = 0; i < 4; i++) {
            if (addressGot[i] != address[i]) {
                fail();
            }
        }
    }

    /**
     * tests convertFromCidrToNetmask for sanity.
     */
    public void testConvertFromCidrToNetmaskForSanity() {
        assertEquals(NetworkUtils.convertFromCidrToNetmask("32"), "255.255.255.255");
        assertEquals(NetworkUtils.convertFromCidrToNetmask("25"), "255.255.255.128");
    }
}
