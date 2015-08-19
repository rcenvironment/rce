/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.cluster.torque.internal;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import de.rcenvironment.core.utils.cluster.internal.ClusterJobTimesInformation;

/**
 * Test cases for {@link ClusterJobTimesInformation}.
 * @author Doreen Seier
 */
public class ClusterJobTimesTest {
    
    private ClusterJobTimesInformation information = new ClusterJobTimesInformation();

    /** Tests for getters and setters. */
    @Test
    public void testGetAndSet() {
        String randomString = RandomStringUtils.random(5);
        information.setJobId(randomString);
        assertEquals(randomString, information.getJobId());

        randomString = RandomStringUtils.random(5);
        information.setQueueTime(randomString);
        assertEquals(randomString, information.getQueueTime());

        randomString = RandomStringUtils.random(5);
        information.setRemainingTime(randomString);
        assertEquals(randomString, information.getRemainingTime());

        randomString = RandomStringUtils.random(5);
        information.setStartTime(randomString);
        assertEquals(randomString, information.getStartTime());
    }
}
