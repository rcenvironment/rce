/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.cluster;

/**
 * Constants related to cluster queuing systems.
 * 
 * @author Doreen Seider
 */
public final class ClusterQueuingSystemConstants {

    /** Command string. */
    public static final String COMMAND_QSUB = "qsub";
    
    /** Command string. */
    public static final String COMMAND_QSTAT = "qstat";

    /** Command string. */
    public static final String COMMAND_QDEL = "qdel";

    /** Command string. */
    public static final String COMMAND_SHOWQ = "showq";

    private ClusterQueuingSystemConstants() { }
}
