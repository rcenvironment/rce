/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.cluster.sge.internal;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.rcenvironment.core.utils.cluster.ClusterJobInformation;
import de.rcenvironment.core.utils.cluster.ClusterJobInformation.ClusterJobState;
import de.rcenvironment.core.utils.cluster.ClusterService;
import de.rcenvironment.core.utils.cluster.internal.AbstractClusterService;
import de.rcenvironment.core.utils.cluster.internal.ClusterJobTimesInformation;
import de.rcenvironment.core.utils.cluster.internal.ClusterJobInformationImpl;
import de.rcenvironment.core.utils.ssh.jsch.SshSessionConfiguration;

/**
 * SGE implementation of {@link ClusterService}.
 * @author Doreen Seider
 */
public class SgeClusterService extends AbstractClusterService {

    private static final int INDEX_JOBID = 0;
    
    private static final int INDEX_JOBNAME = 1;
    
    private static final int INDEX_USER = 2;

    private static final int INDEX_JOBSTATE = 3;
        
    private static final int INDEX_REMAININGTIME = 5;

    private static final int INDEX_QUEUETIME = 6;

    private static final int INDEX_STARTTIME = 6;
    
    private static final int SECTION_ACTIVE_JOBS = 0;
    
    private static final int SECTION_WAITING_JOBS = 1;
    
    private static final int SECTION_WAITING_JOBS_WITH_DEPENDENCIES = 2;
    
    private static final int SECTION_UNSCHEDULED_JOBS = 3;
    
    //    private static final int INDEX_QUEUE = 2;

    // only for OSGi
    @Deprecated
    public SgeClusterService() {}
    
    public SgeClusterService(SshSessionConfiguration sshConfiguration, Map<String, String> pathToQueuingSystemCommands) {
        super(sshConfiguration, pathToQueuingSystemCommands);
    }
    
    @Override
    protected Set<ClusterJobInformation> fetchAndParseClusterJobInformation() throws IOException {
        String stdout = executesCommand(jschSession, buildMainCommand("showq"), REMOTE_WORK_DIR);
        Map<String, ClusterJobInformation> jobInformation = parseStdoutForClusterJobInformation(stdout);

        latestFetchedJobInformation = Collections.unmodifiableMap(jobInformation);
        latestFetch = new Date().getTime();
        
        return new HashSet<ClusterJobInformation>(jobInformation.values());
    }
    
    @Override
    public String cancelClusterJobs(List<String> jobIds) throws IOException {
        StringBuilder commandBuilder = new StringBuilder(buildMainCommand("qdel"));
        for (String jobId : jobIds) {
            commandBuilder.append(" ");
            commandBuilder.append(jobId);
        }
        try {
            executesCommand(jschSession, commandBuilder.toString(), REMOTE_WORK_DIR);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
        return "";
    }
    
    // visibility is protected for test purposes
    protected Map<String, ClusterJobInformation> parseStdoutForClusterJobInformation(String stdout) {
        Map<String, ClusterJobInformation> jobInformation = new HashMap<String, ClusterJobInformation>();
        
        int section = SECTION_ACTIVE_JOBS;
        boolean inSection = false;
        boolean emptyRowPassed = false;
        
        String[] lines = stdout.split("\n");
        for (String line : lines) {
            String[] lineTokens = line.split("(\\s+)");
            if (inSection) {
                if (lineTokens.length <= 1) {
                    if (!emptyRowPassed) {
                        emptyRowPassed = true;                        
                    } else {
                        emptyRowPassed = false;
                        inSection = false;
                        section++;
                    }
                } else {
                    ClusterJobInformation information = extractClusterJobInformation(lineTokens, section);
                    jobInformation.put(information.getJobId(), information);
                }
            } else {
                for (String attribute : lineTokens) {
                    if (attribute.matches("JOBID")) {
                        inSection = true;
                        break;
                    }
                }
            }
        }

        return jobInformation;
    }
    
    private ClusterJobInformation extractClusterJobInformation(String[] lineTokens, int section) {
        ClusterJobInformationImpl information = new ClusterJobInformationImpl();
        
        information.setJobId(lineTokens[INDEX_JOBID]);
        information.setJobName(lineTokens[INDEX_JOBNAME]);
        information.setUser(lineTokens[INDEX_USER]);
        information.setJobState(getClusterJobState(lineTokens[INDEX_JOBSTATE]));
        information.setClusterJobTimesInformation(extractClusterJobTimesInformation(lineTokens, section));
        
        return information;
    }
    
    private ClusterJobState getClusterJobState(String stateToken) {
        try {
            return ClusterJobState.valueOf(stateToken);
        } catch (IllegalArgumentException e) {
            return ClusterJobState.Unknown;
        }
    }
    
    private ClusterJobTimesInformation extractClusterJobTimesInformation(String[] lineTokens, int section) {
        ClusterJobTimesInformation information = new ClusterJobTimesInformation();
        
        information.setJobId(lineTokens[INDEX_JOBID]);

        switch (section) {
        case SECTION_ACTIVE_JOBS:
            information.setRemainingTime(lineTokens[INDEX_REMAININGTIME]);
            information.setStartTime(getTime(lineTokens, INDEX_STARTTIME));
            break;
        case SECTION_WAITING_JOBS:
        case SECTION_WAITING_JOBS_WITH_DEPENDENCIES:
        case SECTION_UNSCHEDULED_JOBS:
        default:
            information.setQueueTime(getTime(lineTokens, INDEX_QUEUETIME));
            break;
        }
        
        return information;
    }
    
    private String getTime(String[] lineTokens, int startIndex) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = startIndex; i < lineTokens.length; i++) {
            stringBuffer.append(lineTokens[i]);
            stringBuffer.append(" ");
        }
        return stringBuffer.delete(stringBuffer.length() - 1, stringBuffer.length()).toString();
    }
    
}
