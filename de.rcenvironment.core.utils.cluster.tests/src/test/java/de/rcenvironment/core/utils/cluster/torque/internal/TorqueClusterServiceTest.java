/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.cluster.torque.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.utils.cluster.ClusterJobInformation;
import de.rcenvironment.core.utils.cluster.ClusterJobInformation.ClusterJobState;
import de.rcenvironment.core.utils.cluster.internal.ClusterJobInformationImpl;
import de.rcenvironment.core.utils.cluster.internal.ClusterJobTimesInformation;
import de.rcenvironment.core.utils.cluster.internal.TestUtils;
import de.rcenvironment.core.utils.ssh.jsch.SshSessionConfiguration;
import de.rcenvironment.core.utils.ssh.jsch.SshSessionConfigurationFactory;

/**
 * Test cases for {@link TorqueClusterService}.
 * 
 * @author Doreen Seider
 */
public class TorqueClusterServiceTest {
    
    private TorqueClusterService clusterService;
    
    private TestUtils helperTestClass
        = new TestUtils();
    
    /** Set up. */
    @Before
    public void setUp() {
        String randomString = RandomStringUtils.random(5);
        SshSessionConfiguration sshConfiguration = SshSessionConfigurationFactory
            .createSshSessionConfigurationWithAuthPhrase(helperTestClass.localHost, helperTestClass.port, randomString, randomString);
        clusterService = new TorqueClusterService(sshConfiguration, new HashMap<String, String>());
    }
    
    /**
     * Test. 
     * @throws IOException if an error occurs 
     **/
    @Test
    public void testParseStdoutForClusterJobInformation() throws IOException {
        final String stdout = IOUtils.toString(getClass().getResourceAsStream("/torque_qstat"));
        
        Map<String, ClusterJobInformation> jobInformation = clusterService.parseStdoutForClusterJobInformation(stdout);
        
        assertEquals(4, jobInformation.size());
        
        ClusterJobInformation information = jobInformation.get("506.host");
        assertEquals("job", information.getJobName());
        assertEquals(ClusterJobState.Running, information.getJobState());
        assertEquals("qtest", information.getQueue());
        assertEquals("user_1", information.getUser());
        
        information = jobInformation.get("507.host");
        assertEquals("jib", information.getJobName());
        assertEquals(ClusterJobState.Queued, information.getJobState());
        assertEquals("mem", information.getQueue());
        assertEquals("user_2", information.getUser());
        
        information = jobInformation.get("508.host");
        assertEquals("job", information.getJobName());
        assertEquals(ClusterJobState.Completed, information.getJobState());
        assertEquals("qtest", information.getQueue());
        assertEquals("user_1", information.getUser());
        
        information = jobInformation.get("509.host");
        assertEquals("jab", information.getJobName());
        assertEquals(ClusterJobState.Queued, information.getJobState());
        assertEquals("fast", information.getQueue());
        assertEquals("user_3", information.getUser());
    }
    
    /**
     * Test. 
     * @throws IOException if an error occurs 
     **/
    @Test
    public void testParseStdoutForClusterJobTimesInformation() throws IOException {
        final String stdout = IOUtils.toString(getClass().getResourceAsStream("/torque_showq"));
        
        Map<String, ClusterJobTimesInformation> jobTimesInformation = clusterService.parseStdoutForClusterJobTimesInformation(stdout);
        
        assertEquals(3, jobTimesInformation.size());
        
        ClusterJobTimesInformation information = jobTimesInformation.get("606");
        assertEquals(ClusterJobInformationImpl.NO_VALUE_SET, information.getQueueTime());
        assertEquals("Tue Aug 28 16:54:22", information.getStartTime());
        assertEquals("00:30:02", information.getRemainingTime());
        
        information = jobTimesInformation.get("569");
        assertEquals(ClusterJobInformationImpl.NO_VALUE_SET, information.getQueueTime());
        assertEquals("Thu Aug 23 14:15:11", information.getStartTime());
        assertEquals("94:20:20:45", information.getRemainingTime());
        
        information = jobTimesInformation.get("607");
        assertEquals("Tue Aug 28 17:54:24", information.getQueueTime());
        assertEquals(ClusterJobInformationImpl.NO_VALUE_SET, information.getStartTime());
        assertEquals(ClusterJobInformationImpl.NO_VALUE_SET, information.getRemainingTime());
        
    }
    
    /**
     * Test.
     **/
    @Test
    public void testEnhanceClusterJobInformation() {
        String jobId0 = RandomStringUtils.random(5);
        String jobId1 = RandomStringUtils.random(5);
        String jobId2 = helperTestClass.localJobId;
        String jobId3 = helperTestClass.remoteJobId;
        String jobId4 = RandomStringUtils.random(5);
        
        String[] jobIds = new String[] { jobId0, jobId1, jobId2, jobId3 };
        
        Map<String, ClusterJobInformation> jobInformation = new HashMap<String, ClusterJobInformation>();
        Map<String, ClusterJobTimesInformation> jobTimesInformation = new HashMap<String, ClusterJobTimesInformation>();

        for (String jobId : jobIds) {
            ClusterJobInformationImpl information = new ClusterJobInformationImpl();
            information.setJobId(jobId);
            jobInformation.put(jobId, information);
            if (jobId.equals(jobId1) || jobId.equals(jobId3)) {
                ClusterJobTimesInformation timesInformation = new ClusterJobTimesInformation();
                timesInformation.setJobId(jobId);
                jobTimesInformation.put(jobId, timesInformation);
            }
        }
        
        String queueTime1 = RandomStringUtils.random(5);
        jobTimesInformation.get(jobId1).setQueueTime(queueTime1);
        
        String remainingTime3 = RandomStringUtils.random(5);
        String startTime3 = RandomStringUtils.random(5);
        jobTimesInformation.get(jobId3).setRemainingTime(remainingTime3);
        jobTimesInformation.get(jobId3).setStartTime(startTime3);
        
        Set<ClusterJobInformation> resultJobInformation = clusterService
            .enhanceClusterJobInformation(jobInformation, jobTimesInformation);
        
        assertEquals(jobInformation.size(), resultJobInformation.size());
        for (ClusterJobInformation information : resultJobInformation) {
            if (information.getJobId().equals(jobId0)) {
                assertEquals(ClusterJobInformationImpl.NO_VALUE_SET, information.getQueueTime());
                assertEquals(ClusterJobInformationImpl.NO_VALUE_SET, information.getRemainingTime());
                assertEquals(ClusterJobInformationImpl.NO_VALUE_SET, information.getStartTime());
            } else if (information.getJobId().equals(jobId1)) {
                assertEquals(queueTime1, information.getQueueTime());
                assertEquals(ClusterJobInformationImpl.NO_VALUE_SET, information.getRemainingTime());
                assertEquals(ClusterJobInformationImpl.NO_VALUE_SET, information.getStartTime());
            } else if (information.getJobId().equals(jobId2)) {
                assertEquals(ClusterJobInformationImpl.NO_VALUE_SET, information.getQueueTime());
                assertEquals(ClusterJobInformationImpl.NO_VALUE_SET, information.getRemainingTime());
                assertEquals(ClusterJobInformationImpl.NO_VALUE_SET, information.getStartTime());
            } else if (information.getJobId().equals(jobId3)) {
                assertEquals(ClusterJobInformationImpl.NO_VALUE_SET, information.getQueueTime());
                assertEquals(remainingTime3, information.getRemainingTime());
                assertEquals(startTime3, information.getStartTime());
            }
        }
        assertFalse(resultJobInformation.contains(jobId4));
        
    }
}
