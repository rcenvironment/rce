/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.cluster.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import de.rcenvironment.core.utils.cluster.ClusterJobInformation.ClusterJobState;

/**
 * Test cases for {@link ClusterJobInformationImpl}.
 * @author Doreen Seider
 */
public class ModifyableClusterJobInformationTest {

    private ClusterJobInformationImpl information = new ClusterJobInformationImpl();
    
    /** Tests for getters and setters. */
    @Test
    public void testGetAndSet() {
        String randomString = RandomStringUtils.random(5);
        information.setJobId(randomString);
        assertEquals(randomString, information.getJobId());

        randomString = RandomStringUtils.random(5);
        information.setJobName(randomString);
        assertEquals(randomString, information.getJobName());
        
        randomString = RandomStringUtils.random(5);
        information.setJobState(ClusterJobState.Completed);
        assertEquals(ClusterJobState.Completed, information.getJobState());
        
        randomString = RandomStringUtils.random(5);
        information.setQueue(randomString);
        assertEquals(randomString, information.getQueue());
        
        randomString = RandomStringUtils.random(5);
        information.setUser(randomString);
        assertEquals(randomString, information.getUser());

        assertNotNull(randomString, information.getQueueTime());
        assertNotNull(randomString, information.getStartTime());
        assertNotNull(randomString, information.getRemainingTime());
        
        randomString = RandomStringUtils.random(5);
        ClusterJobTimesInformation timesInformation = new ClusterJobTimesInformation();
        timesInformation.setQueueTime(randomString);
        information.setClusterJobTimesInformation(timesInformation);
        assertEquals(randomString, information.getQueueTime());
        
        randomString = RandomStringUtils.random(5);
        timesInformation.setRemainingTime(randomString);
        information.setClusterJobTimesInformation(timesInformation);
        assertEquals(randomString, information.getRemainingTime());
        
        randomString = RandomStringUtils.random(5);
        timesInformation.setStartTime(randomString);
        information.setClusterJobTimesInformation(timesInformation);
        assertEquals(randomString, information.getStartTime());
        
        assertNotNull(information.toString());
    }
}
