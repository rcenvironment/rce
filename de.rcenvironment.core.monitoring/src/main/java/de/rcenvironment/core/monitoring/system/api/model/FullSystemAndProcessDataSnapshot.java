/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.api.model;

import java.io.Serializable;
import java.util.List;

import de.rcenvironment.core.monitoring.system.internal.SystemMonitoringUtils;

/**
 * The complete model which holds monitoring data.
 * 
 * @author David Scholz
 * @author Robert Mischke
 */
public class FullSystemAndProcessDataSnapshot implements Serializable {

    private static final long serialVersionUID = 4316060053937251752L;

    private double totalNodeCPUusage;

    private double systemIdle;

    private long totalNodeRAMUsage;

    private long totalNodeSystemRAM;

    private List<ProcessInformation> rceSubProcesses;

    private List<ProcessInformation> rceOwnProcesses;

    public FullSystemAndProcessDataSnapshot(double totalNodeCPUusage, long totalNodeRAMUsage, long totalNodeSystemRAM, double systemIdle,
        List<ProcessInformation> rceSubProcesses, List<ProcessInformation> rceOwnProcesses) {
        this.totalNodeCPUusage = totalNodeCPUusage;
        this.totalNodeRAMUsage = totalNodeRAMUsage;
        this.totalNodeSystemRAM = totalNodeSystemRAM;
        this.systemIdle = systemIdle;
        this.rceSubProcesses = rceSubProcesses;
        this.rceOwnProcesses = rceOwnProcesses;
    }

    /**
     * Get rce sub processes. Returns null if list is empty.
     * 
     * @return rce sub processes.
     */
    public List<ProcessInformation> getRceSubProcesses() {
        return rceSubProcesses;
    }

    /**
     * Get rce entities.
     * 
     * @return rce entities.
     */
    public List<ProcessInformation> getRceProcessesInfo() {
        return rceOwnProcesses;
    }

    public double getNodeCPUusage() {
        return totalNodeCPUusage;
    }

    public long getNodeRAMUsage() {
        return totalNodeRAMUsage;
    }

    public long getNodeSystemRAM() {
        return totalNodeSystemRAM;
    }

    public double getIdle() {
        return systemIdle;
    }

    /**
     * Get total rce cpu usage.
     * 
     * @return rce cpu usage.
     */
    public double getTotalRceOwnProcessesCpuUsage() {
        double usage = 0;
        for (ProcessInformation pinf : rceOwnProcesses) {
            usage += pinf.getCpuUsage();
        }
        return usage;
    }

    /**
     * Get total rce ram usage.
     * 
     * @return rce ram usage.
     */
    public long getTotalRceOwnProcessesRamUsage() {
        long usage = 0;
        for (ProcessInformation pinf : rceOwnProcesses) {
            usage += pinf.getRamUsage();
        }
        return usage;
    }

    /**
     * Get total cpu usage of children.
     * 
     * @return children cpu usage.
     */
    public double getTotalSubProcessesCpuUsage() {
        return calcTotalChildrenCpuUsage(rceSubProcesses);
    }

    private double calcTotalChildrenCpuUsage(List<ProcessInformation> list) {
        double usage = 0;
        for (ProcessInformation info : list) {
            usage += info.getCpuUsage() + calcTotalChildrenCpuUsage(info.getChildren());
        }
        return usage;
    }

    /**
     * Get total cpu usage of all other processes.
     * 
     * @return cpu usage of other processes.
     */
    public double getOtherProcessCpuUsage() {
        // TODO check: this was totalNodeCPUusage - getTotalRceOwnProcessesCpuUsage() before, but that seemed semantically wrong? - misc_ro
        // TODO this recomputes the other values; cache them instead
        return SystemMonitoringUtils.clampToPercentageOrNAN(totalNodeCPUusage - getTotalRceOwnProcessesCpuUsage()
            - getTotalSubProcessesCpuUsage());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        final int i = 32;
        int result = 1;
        if (rceOwnProcesses == null) {
            result = prime * result;
        } else {
            result = prime * result + rceOwnProcesses.hashCode();
        }

        if (rceSubProcesses == null) {
            result = prime * result;
        } else {
            result = prime * result + rceSubProcesses.hashCode();
        }

        long temp;
        temp = Double.doubleToLongBits(systemIdle);
        result = prime * result + (int) (temp ^ (temp >>> i));
        temp = Double.doubleToLongBits(totalNodeCPUusage);
        result = prime * result + (int) (temp ^ (temp >>> i));
        result = prime * result + (int) (totalNodeRAMUsage ^ (totalNodeRAMUsage >>> i));
        result = prime * result + (int) (totalNodeSystemRAM ^ (totalNodeSystemRAM >>> i));
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

        FullSystemAndProcessDataSnapshot other = (FullSystemAndProcessDataSnapshot) obj;
        if (rceOwnProcesses == null) {
            if (other.rceOwnProcesses != null) {
                return false;
            }
        } else if (!rceOwnProcesses.equals(other.rceOwnProcesses)) {
            return false;
        }
        if (rceSubProcesses == null) {
            if (other.rceSubProcesses != null) {
                return false;
            }
        } else if (!rceSubProcesses.equals(other.rceSubProcesses)) {
            return false;
        }
        if (Double.doubleToLongBits(systemIdle) != Double.doubleToLongBits(other.systemIdle)) {
            return false;
        }
        if (Double.doubleToLongBits(totalNodeCPUusage) != Double.doubleToLongBits(other.totalNodeCPUusage)) {
            return false;
        }
        if (totalNodeRAMUsage != other.totalNodeRAMUsage) {
            return false;
        }
        if (totalNodeSystemRAM != other.totalNodeSystemRAM) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        final String newLine = "\n";
        final String percent = "%";
        final double maxCPU = 100;
        final double totalRceUsage = getTotalRceOwnProcessesCpuUsage();
        final double totalChildUsage = getTotalSubProcessesCpuUsage();
        final double idle = getIdle();
        StringBuilder sb = new StringBuilder();
        sb.append("Total node cpu usage: " + totalNodeCPUusage + newLine);
        sb.append("Total node ram: " + totalNodeSystemRAM + newLine);
        sb.append("Total used ram: " + totalNodeRAMUsage + newLine);
        sb.append("RCE cpu usage: " + totalRceUsage + percent + newLine);
        sb.append("RCE Sub-Processes: " + totalChildUsage + percent + newLine);
        for (ProcessInformation child : rceSubProcesses) {
            sb.append(child.toString());
        }
        sb.append("System Idle: " + idle + percent + newLine);
        sb.append("Other processes: " + (maxCPU - totalRceUsage - totalChildUsage - idle) + percent + newLine);
        return sb.toString();
    }

}
