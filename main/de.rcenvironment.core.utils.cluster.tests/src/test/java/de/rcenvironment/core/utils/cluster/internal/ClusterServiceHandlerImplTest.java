/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.cluster.internal;

import static org.junit.Assert.assertNotSame;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import de.rcenvironment.core.utils.cluster.ClusterQueuingSystem;
import de.rcenvironment.core.utils.cluster.ClusterService;

/**
 * Test cases for {@link ClusterServiceManagerImpl}.
 * @author Doreen Seider
 */
public class ClusterServiceHandlerImplTest {

    private ClusterServiceManagerImpl handler = new ClusterServiceManagerImpl();
    
    /**
     * Test if services are cached correctly
     * . 
     * @throws IOException on error
     **/
    @Test
    public void testCreateSshBasedClusterJobInformationService() throws IOException {
        String host0 = RandomStringUtils.random(5);
        Map<String, String> commandsPaths0 = new HashMap<>();
        int port0 = 9;
        String user0 = RandomStringUtils.random(5);
        String passwd0 = RandomStringUtils.random(5);

        String host1 = RandomStringUtils.random(5);
        Map<String, String> commandsPaths1 = new HashMap<>();
        commandsPaths1.put("command", "path");
        int port1 = 10;
        String user1 = RandomStringUtils.random(5);
        String passwd1 = RandomStringUtils.random(5);

        ClusterService service0 = handler.retrieveSshBasedClusterService(
            ClusterQueuingSystem.TORQUE, commandsPaths0, host0, port0, user0, passwd1);
        
        ClusterService service1 = handler.retrieveSshBasedClusterService(
            ClusterQueuingSystem.TORQUE, commandsPaths0, host0, port0, user0, passwd0);
        
        ClusterService service2 = handler.retrieveSshBasedClusterService(
            ClusterQueuingSystem.TORQUE, commandsPaths0, host0, port0, user1, passwd0);
        
        ClusterService service3 = handler.retrieveSshBasedClusterService(
            ClusterQueuingSystem.TORQUE, commandsPaths0, host0, port1, user0, passwd0);
        
        ClusterService service4 = handler.retrieveSshBasedClusterService(
            ClusterQueuingSystem.TORQUE, commandsPaths0, host1, port0, user0, passwd0);
        
        ClusterService service5 = handler.retrieveSshBasedClusterService(
            ClusterQueuingSystem.TORQUE, commandsPaths1, host0, port0, user0, passwd0);

        assertNotSame(service0, service1);
        assertNotSame(service0, service2);
        assertNotSame(service0, service3);
        assertNotSame(service0, service4);
        assertNotSame(service0, service5);
        assertNotSame(service2, service3);
        assertNotSame(service2, service4);
        assertNotSame(service2, service5);
        assertNotSame(service3, service4);
        assertNotSame(service3, service5);
        assertNotSame(service4, service5);
        
    }
}
