/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.cluster.sge.internal;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.utils.cluster.ClusterJobInformation;
import de.rcenvironment.core.utils.cluster.ClusterJobInformation.ClusterJobState;
import de.rcenvironment.core.utils.cluster.internal.ClusterJobSourceServiceImpl;
import de.rcenvironment.core.utils.cluster.internal.ClusterJobSourceServiceImplTest;
import de.rcenvironment.core.utils.cluster.internal.ClusterJobInformationImpl;
import de.rcenvironment.core.utils.ssh.jsch.SshSessionConfiguration;
import de.rcenvironment.core.utils.ssh.jsch.SshSessionConfigurationFactory;

/**
 * Test cases for {@link SgeClusterService}.
 * @author Doreen Seider
 */
public class SgeClusterServiceTest {
    
    private SgeClusterService informationService;
    
    private ClusterJobSourceServiceImplTest helperTestClass
        = new ClusterJobSourceServiceImplTest();
    
    /** Set up. */
    @Before
    public void setUp() {
        String randomString = RandomStringUtils.random(5);
        SshSessionConfiguration sshConfiguration = SshSessionConfigurationFactory
            .createSshSessionConfigurationWithAuthPhrase(helperTestClass.localHost, helperTestClass.port, randomString, randomString);
        informationService = new SgeClusterService(sshConfiguration, new HashMap<String, String>());
        informationService.bindClusterJobSourceService(new ClusterJobSourceServiceImpl());
    }
    
    /**
     * Test. 
     * @throws IOException if an error occurs 
     **/
    @Test
    public void testParseStdoutForClusterJobInformation() throws IOException {
        final String stdout = IOUtils.toString(getClass().getResourceAsStream("/sge_showq"));
        
        Map<String, ClusterJobInformation> jobInformation = informationService.parseStdoutForClusterJobInformation(stdout);
        
        final int amount = 37;
        assertEquals(amount, jobInformation.size());
        
        ClusterJobInformation information = jobInformation.get("7448");
        assertEquals("INROS-eu-f", information.getJobName());
        assertEquals(ClusterJobState.Running, information.getJobState());
        assertEquals("heil_gr", information.getUser());
        assertEquals("Thu Nov 21 12:01:56", information.getStartTime());
        assertEquals("-73:47:53", information.getRemainingTime());
        assertEquals(ClusterJobInformationImpl.NO_VALUE_SET, information.getQueueTime());
        
        information = jobInformation.get("986");
        assertEquals("S22.0F20.0", information.getJobName());
        assertEquals(ClusterJobState.Waiting, information.getJobState());
        assertEquals("fran_dr", information.getUser());
        assertEquals(ClusterJobInformationImpl.NO_VALUE_SET, information.getStartTime());
        assertEquals(ClusterJobInformationImpl.NO_VALUE_SET, information.getRemainingTime());
        assertEquals("Fri Nov 15 13:57:13", information.getQueueTime());
        
        information = jobInformation.get("10618");
        assertEquals("BFS_ModCD2", information.getJobName());
        assertEquals(ClusterJobState.DepWait, information.getJobState());
        assertEquals("prob_ae", information.getUser());
        assertEquals(ClusterJobInformationImpl.NO_VALUE_SET, information.getStartTime());
        assertEquals(ClusterJobInformationImpl.NO_VALUE_SET, information.getRemainingTime());
        assertEquals("Fri Nov 22 19:15:14", information.getQueueTime());

        information = jobInformation.get("79234");
        assertEquals("C1_kwsg_4", information.getJobName());
        assertEquals(ClusterJobState.Unsched, information.getJobState());
        assertEquals("schr_m2", information.getUser());
        assertEquals(ClusterJobInformationImpl.NO_VALUE_SET, information.getStartTime());
        assertEquals(ClusterJobInformationImpl.NO_VALUE_SET, information.getRemainingTime());
        assertEquals("Thu Mar 6 12:32:58", information.getQueueTime());
        
    }
    
}
