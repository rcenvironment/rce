/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.cluster.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.core.utils.cluster.ClusterJobSourceService;
import de.rcenvironment.core.utils.cluster.ClusterQueuingSystem;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;

/**
 * Implementation of {@link ClusterJobSourceService}.
 * @author Doreen Seider
 */
public class ClusterJobSourceServiceImpl implements ClusterJobSourceService {

    private static final long serialVersionUID = 4749823706213153980L;

    private static final String SEPARATOR = "!§$%&";
    
    private Map<String, Map<String, String>> sourceInformation = new HashMap<String, Map<String, String>>();
    
    @Override
    public void addSourceInformation(ClusterQueuingSystem system, String host, int port, String jobId, String source) {
        String clusterIdentifier = createIdentifier(system, host, port);
        if (!sourceInformation.containsKey(clusterIdentifier)) {
            sourceInformation.put(clusterIdentifier, new HashMap<String, String>());
        }
        sourceInformation.get(clusterIdentifier).put(jobId, source);
    }

    @Override
    public void removeSourceInformation(ClusterQueuingSystem system, String host, int port, String jobId) {
        String clusterIdentifier = createIdentifier(system, host, port);
        if (sourceInformation.containsKey(clusterIdentifier)) {
            sourceInformation.get(clusterIdentifier).remove(jobId);
        }
    }

    @Override
    @AllowRemoteAccess
    public Map<String, String> getSourceInformation(ClusterQueuingSystem system, String host, Integer port) {
        try {
            String clusterIdentifier = createIdentifier(system, host, port);
            if (sourceInformation.containsKey(clusterIdentifier)) {
                return Collections.unmodifiableMap(sourceInformation.get(clusterIdentifier));
            } else {
                return new HashMap<String, String>();
            }            
        } catch (RuntimeException e) {
            return new HashMap<String, String>();
        }
    }
    
    private String createIdentifier(ClusterQueuingSystem system, String host, int port) {
        StringBuffer buffer = new StringBuffer(system.toString());
        buffer.append(SEPARATOR);
        buffer.append(host);
        buffer.append(SEPARATOR);
        buffer.append(port);
        return buffer.toString();
    }

}
