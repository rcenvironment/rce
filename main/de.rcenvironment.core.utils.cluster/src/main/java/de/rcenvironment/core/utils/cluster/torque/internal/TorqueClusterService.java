/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.cluster.torque.internal;

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
import de.rcenvironment.core.utils.cluster.ClusterQueuingSystemConstants;
import de.rcenvironment.core.utils.cluster.ClusterService;
import de.rcenvironment.core.utils.cluster.internal.AbstractClusterService;
import de.rcenvironment.core.utils.cluster.internal.ClusterJobInformationImpl;
import de.rcenvironment.core.utils.cluster.internal.ClusterJobTimesInformation;
import de.rcenvironment.core.utils.ssh.jsch.SshSessionConfiguration;

/**
 * TORQUE implementation of {@link ClusterService}.
 * 
 * Note: ClusterService implementations should be an OSGi service --seid_do
 * 
 * @author Doreen Seider
 */
public class TorqueClusterService extends AbstractClusterService {

    private static final int INDEX_JOBID = 0;
    
    private static final int INDEX_USER = 1;
    
    private static final int INDEX_QUEUE = 2;

    private static final int INDEX_JOBNAME = 3;

    private static final int INDEX_JOBSTATE = 9;
    
    private static final int INDEX_REMAININGTIME = 4;

    private static final int INDEX_STARTTIME = 5;
    
    private static final int INDEX_QUEUETIME = 5;
    
    private static final int SECTION_ACTIVE_JOBS = 0;
    
    private static final int SECTION_IDLE_JOBS = 1;
    
    private static final int SECTION_BLOCKED_JOBS = 2;
    
    public TorqueClusterService(SshSessionConfiguration sshConfiguration, Map<String, String> pathToQueuingSystemCommands) {
        super(sshConfiguration, pathToQueuingSystemCommands);
    }

    @Override
    public Set<ClusterJobInformation> fetchClusterJobInformation() throws IOException {
        ensureJschSessionEstablished();
        String stdout = executesCommand(jschSession, buildMainCommand(ClusterQueuingSystemConstants.COMMAND_QSTAT)
            + " -a", REMOTE_WORK_DIR);
        Map<String, ClusterJobInformation> jobInformation = parseStdoutForClusterJobInformation(stdout);

        latestFetchedJobInformation = Collections.unmodifiableMap(jobInformation);
        latestFetch = new Date().getTime();

        ensureJschSessionEstablished();
        stdout = executesCommand(jschSession, buildMainCommand(ClusterQueuingSystemConstants.COMMAND_SHOWQ), REMOTE_WORK_DIR);
        Map<String, ClusterJobTimesInformation> jobTimesInformation = parseStdoutForClusterJobTimesInformation(stdout);
        return enhanceClusterJobInformation(jobInformation, jobTimesInformation);
    }
    
    @Override
    public String cancelClusterJobs(List<String> jobIds) throws IOException {
        StringBuilder commandBuilder = new StringBuilder(buildMainCommand(ClusterQueuingSystemConstants.COMMAND_QDEL) + " ");
        for (String jobId : jobIds) {
            commandBuilder.append(" ");
            commandBuilder.append(jobId);
        }
        ensureJschSessionEstablished();
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
        
        final String regex = "(-+)";

        boolean headerCompleted = false;
        boolean isHeader = false;
        
        
        String[] lines = stdout.split("\n");
        for (String line : lines) {
            headerCompleted = isHeader;
            String[] lineTokens = line.split("(\\s+)");
            if (headerCompleted) {
                ClusterJobInformation information = extractClusterJobInformation(lineTokens);
                jobInformation.put(information.getJobId(), information);
            } else {
                isHeader = true;
                for (String attribute : lineTokens) {
                    if (!attribute.matches(regex)) {
                        isHeader = false;
                        break;
                    }
                }
            }
        }
        return jobInformation;
    }
    
    // visibility is protected for test purposes
    protected Map<String, ClusterJobTimesInformation> parseStdoutForClusterJobTimesInformation(String stdout) {
        Map<String, ClusterJobTimesInformation> information = new HashMap<String, ClusterJobTimesInformation>();
        
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
                    ClusterJobTimesInformation timesInformation = extractClusterJobTimesInformation(lineTokens, section);
                    information.put(timesInformation.getJobId(), timesInformation);
                }
            } else {
                for (String attribute : lineTokens) {
                    if (attribute.matches("JOBNAME")) {
                        inSection = true;
                        break;
                    }
                }
            }
        }

        return information;
    }

    // visibility is protected for test purposes
    protected Set<ClusterJobInformation> enhanceClusterJobInformation(Map<String, ClusterJobInformation> jobInformation,
        Map<String, ClusterJobTimesInformation> jobTimesInformation) {
        
        jobInformation = enhanceClusterJobInformationWithTimesInformation(jobInformation, jobTimesInformation);

        return new HashSet<ClusterJobInformation>(jobInformation.values());
    }
    
    private Map<String, ClusterJobInformation> enhanceClusterJobInformationWithTimesInformation(
        Map<String, ClusterJobInformation> jobInformation, Map<String, ClusterJobTimesInformation> jobTimesInformation) {
        
        for (ClusterJobInformation information : jobInformation.values()) {
            String jobName = information.getJobId().split("\\.")[0];
            if (jobTimesInformation.containsKey(jobName)) {
                ((ClusterJobInformationImpl) information).setClusterJobTimesInformation(jobTimesInformation.get(jobName));
            } else {
                ((ClusterJobInformationImpl) information).setClusterJobTimesInformation(new ClusterJobTimesInformation());
            }
        }
        return jobInformation;
    }
    
    private ClusterJobTimesInformation extractClusterJobTimesInformation(String[] lineTokens, int section) {
        ClusterJobTimesInformation information = new ClusterJobTimesInformation();
        
        information.setJobId(lineTokens[INDEX_JOBID]);

        switch (section) {
        case SECTION_ACTIVE_JOBS:
            information.setRemainingTime(lineTokens[INDEX_REMAININGTIME]);
            information.setStartTime(getTime(lineTokens, INDEX_STARTTIME));
            break;
        case SECTION_IDLE_JOBS:
        case SECTION_BLOCKED_JOBS:
        default:
            information.setQueueTime(getTime(lineTokens, INDEX_QUEUETIME));
            break;
        }
        
        return information;
    }
    
    private String getTime(String[] lineTokens, int startIndex) {
        StringBuilder strBuilder = new StringBuilder();
        for (int i = startIndex; i < lineTokens.length; i++) {
            strBuilder.append(lineTokens[i]);
            strBuilder.append(" ");
        }
        return strBuilder.delete(strBuilder.length() - 1, strBuilder.length()).toString();
    }
    
    private ClusterJobInformation extractClusterJobInformation(String[] lineTokens) {
        ClusterJobInformationImpl information = new ClusterJobInformationImpl();
        
        information.setJobId(lineTokens[INDEX_JOBID]);
        information.setUser(lineTokens[INDEX_USER]);
        information.setQueue(lineTokens[INDEX_QUEUE]);
        information.setJobName(lineTokens[INDEX_JOBNAME]);
        information.setJobState(getClusterJobState(lineTokens[INDEX_JOBSTATE]));
        
        return information;
    }
    
    private ClusterJobState getClusterJobState(String stateToken) {
        ClusterJobState state = ClusterJobState.Unknown;
        if (stateToken.equals("C"))  {
            state = ClusterJobState.Completed;
        } else if (stateToken.equals("E"))  {
            state = ClusterJobState.Exiting;
        } else if (stateToken.equals("H"))  {
            state = ClusterJobState.Held;
        } else if (stateToken.equals("Q"))  {
            state = ClusterJobState.Queued;
        } else if (stateToken.equals("R"))  {
            state = ClusterJobState.Running;
        } else if (stateToken.equals("T"))  {
            state = ClusterJobState.Moved;
        } else if (stateToken.equals("W"))  {
            state = ClusterJobState.Waiting;
        } else if (stateToken.equals("S"))  {
            state = ClusterJobState.Suspended;
        }
        return state;
    }

}
