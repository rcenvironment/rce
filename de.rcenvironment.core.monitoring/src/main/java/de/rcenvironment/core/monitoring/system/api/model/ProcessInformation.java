/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.monitoring.system.api.model;

import java.io.Serializable;
import java.util.List;

/**
 * Holds information about a process.
 *
 * @author David Scholz
 */
public class ProcessInformation implements Serializable {

    private static final long serialVersionUID = -7273457883589693406L;

    private List<ProcessInformation> children;
    
    private double cpuUsage;
    
    private long ramUsage;
    
    private long pid;
    
    private String name;
    
    public ProcessInformation(long pid, String name, List<ProcessInformation> children, double cpuUsage, long ramUsage) {
        this.pid = pid;
        this.name = name;
        this.children = children;
        this.cpuUsage = cpuUsage;
        this.ramUsage = ramUsage;
    }
    
    public List<ProcessInformation> getChildren() {
        return children;
    }

    public double getCpuUsage() {
        return cpuUsage;
    }
    
    public long getRamUsage() {
        return ramUsage;
    }

    public long getPid() {
        return pid;
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        final int i = 32;
        int result = 1;
        if (children == null) {
            result = prime * result;
        } else {
            result = prime * result + children.hashCode();
        }
        long temp;
        temp = Double.doubleToLongBits(cpuUsage);
        result = prime * result + (int) (temp ^ (temp >>> i));
        if (name == null) {
            result = prime * result;
        } else {
            result = prime * result + name.hashCode();
        }
        result = prime * result + (int) (pid ^ (pid >>> i));
        result = prime * result + (int) (ramUsage ^ (ramUsage >>> i));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
            
        if (obj == null) {
            return false;
        }
            
        if (getClass() != obj.getClass()) {
            return false;
        }
            
        ProcessInformation other = (ProcessInformation) obj;
        if (children == null) {
            if (other.children != null) {
                return false;
            }      
        } else if (!children.equals(other.children)) {
            return false;
        }      
        if (Double.doubleToLongBits(cpuUsage) != Double.doubleToLongBits(other.cpuUsage)) {
            return false;
        }       
        if (name == null) {
            if (other.name != null) {
                return false;
            }         
        } else if (!name.equals(other.name)) {
            return false;
        }      
        if (pid != other.pid) {
            return false;
        }         
        if (ramUsage != other.ramUsage) {
            return false;
        }
            
        return true;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb = getChildInfo(children, 0, sb);
        return sb.toString();
    }
    
    private StringBuilder getChildInfo(List<ProcessInformation> c, int level, StringBuilder sb) {
        toLevelString(level, sb);
        for (ProcessInformation pinf : c) {
            getChildInfo(pinf.getChildren(), level + 1, sb);
        }
        return sb;
    }
    
    private void toLevelString(int lvl, StringBuilder sb) {
        for (int i = 0; i <= lvl; i++) {
            sb.append("    ");
        }
        sb.append(name + ": " + cpuUsage + "\n");
    }
    
}
