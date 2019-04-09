/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.communication.views.contributors;

import static de.rcenvironment.core.monitoring.system.api.SystemMonitoringConstants.PERCENTAGE_TO_DISPLAY_VALUE_MULTIPLIER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.spi.NetworkTopologyChangeListener;
import de.rcenvironment.core.communication.spi.NetworkTopologyChangeListenerAdapter;
import de.rcenvironment.core.gui.communication.views.NetworkViewContentProvider;
import de.rcenvironment.core.gui.communication.views.model.NetworkGraphNodeWithContext;
import de.rcenvironment.core.gui.communication.views.spi.ContributedNetworkViewNode;
import de.rcenvironment.core.gui.communication.views.spi.ContributedNetworkViewNodeWithParent;
import de.rcenvironment.core.gui.communication.views.spi.NetworkViewContributor;
import de.rcenvironment.core.gui.communication.views.spi.SelfRenderingNetworkViewNode;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.monitoring.system.api.SystemMonitoringDataPollingManager;
import de.rcenvironment.core.monitoring.system.api.SystemMonitoringDataSnapshotListener;
import de.rcenvironment.core.monitoring.system.api.model.ProcessInformation;
import de.rcenvironment.core.monitoring.system.api.model.FullSystemAndProcessDataSnapshot;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;
import de.rcenvironment.core.utils.incubator.ServiceRegistryPublisherAccess;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;

/**
 * Contributes a subtree for node resource informations.
 * 
 * @author David Scholz
 * @author Robert Mischke
 */
public class MonitoringDataContributor extends NetworkViewContributorBase {

    private static final double DOUBLE_0_01 = 0.01;

    private static final double DOUBLE_0_2 = 0.2;

    private static final double DOUBLE_0_4 = 0.4;

    private static final double DOUBLE_0_6 = 0.6;

    private static final double DOUBLE_0_8 = 0.8;

    private static final double DOUBLE_0_99 = 0.99;

    private static final int MONITORING_DATA_PRIORITY = 15;

    private final Map<InstanceNodeSessionId, ContributedNetworkViewNode> idToNodeMap;

    private final Map<InstanceNodeSessionId, ContributedNetworkViewNode> expansionsMap;

    private final Map<InstanceNodeSessionId, List<RceNode>> nodeIdToRceNodeMap;

    private final Map<InstanceNodeSessionId, List<InstanceResourceInfoNode>> nodeIdToInstanceResourceInfoMap;

    private final SystemMonitoringDataPollingManager pollingManager;

    private final Image cpuMonitorImage0;

    private final Image cpuMonitorImage1;

    private final Image cpuMonitorImage2;

    private final Image cpuMonitorImage3;

    private final Image cpuMonitorImage4;

    private final Image cpuMonitorImage5;

    private final Image cpuMonitorImage6;

    private final Image ramMonitorImage0;

    private final Image ramMonitorImage1;

    private final Image ramMonitorImage2;

    private final Image ramMonitorImage3;

    private final Image ramMonitorImage4;

    private final Image ramMonitorImage5;

    private final Image ramMonitorImage6;

    private final Image sharedFolderImage;

    private final Image dummyImage;

    private final ServiceRegistryPublisherAccess servicePublisher;

    private final Log log = LogFactory.getLog(getClass());

    public MonitoringDataContributor() {

        final ServiceRegistryAccess serviceAccess = ServiceRegistry.createAccessFor(this);
        final CommunicationService communicationService = serviceAccess.getService(CommunicationService.class);
        final AsyncTaskService asyncTaskService = serviceAccess.getService(AsyncTaskService.class);
        pollingManager = new SystemMonitoringDataPollingManager(communicationService, asyncTaskService);

        sharedFolderImage = ImageManager.getInstance().getSharedImage(StandardImages.FOLDER_16);
        dummyImage = ImageManager.getInstance().getSharedImage(StandardImages.INFORMATION_16);
        // TODO this should be handled via arrays, not copy&paste
        cpuMonitorImage0 = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/cpuMonitor/cpuMonitor0_16.gif")).createImage();
        cpuMonitorImage1 = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/cpuMonitor/cpuMonitor1_16.gif")).createImage();
        cpuMonitorImage2 = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/cpuMonitor/cpuMonitor2_16.gif")).createImage();
        cpuMonitorImage3 = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/cpuMonitor/cpuMonitor3_16.gif")).createImage();
        cpuMonitorImage4 = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/cpuMonitor/cpuMonitor4_16.gif")).createImage();
        cpuMonitorImage5 = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/cpuMonitor/cpuMonitor5_16.gif")).createImage();
        cpuMonitorImage6 = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/cpuMonitor/cpuMonitor6_16.gif")).createImage();
        ramMonitorImage0 = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/ramMonitor/ramMonitor0_16.gif")).createImage();
        ramMonitorImage1 = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/ramMonitor/ramMonitor1_16.gif")).createImage();
        ramMonitorImage2 = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/ramMonitor/ramMonitor2_16.gif")).createImage();
        ramMonitorImage3 = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/ramMonitor/ramMonitor3_16.gif")).createImage();
        ramMonitorImage4 = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/ramMonitor/ramMonitor4_16.gif")).createImage();
        ramMonitorImage5 = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/ramMonitor/ramMonitor5_16.gif")).createImage();
        ramMonitorImage6 = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/ramMonitor/ramMonitor6_16.gif")).createImage();

        idToNodeMap = new HashMap<>();
        expansionsMap = new HashMap<>();
        nodeIdToRceNodeMap = new HashMap<>();
        nodeIdToInstanceResourceInfoMap = new HashMap<>();
        servicePublisher = ServiceRegistry.createPublisherAccessFor(this);
        registerChangeListeners();
    }

    /**
     * A tree node representing the root node of the monitoring data.
     * 
     * @author David Scholz
     */
    private class MonitoringDataFolderRootNode implements ContributedNetworkViewNode {

        private final NetworkGraphNodeWithContext instanceNode;

        MonitoringDataFolderRootNode(NetworkGraphNodeWithContext instanceNode) {
            this.instanceNode = instanceNode;
        }

        public NetworkGraphNodeWithContext getInstanceNode() {
            return instanceNode;
        }

        @Override
        public NetworkViewContributor getContributor() {
            return MonitoringDataContributor.this;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            if (instanceNode == null) {
                result = prime * result;
            } else {
                result = prime * result + instanceNode.hashCode();
            }
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
            MonitoringDataFolderRootNode other = (MonitoringDataFolderRootNode) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (instanceNode == null) {
                if (other.instanceNode != null) {
                    return false;
                }
            } else if (!instanceNode.equals(other.instanceNode)) {
                return false;
            }
            return true;
        }

        private MonitoringDataContributor getOuterType() {
            return MonitoringDataContributor.this;
        }

    }

    /**
     * 
     * Context of a {@link InstanceCpuInfoNode}.
     * 
     * @author David Scholz
     */
    public enum InstanceNodeContext {
        // /**
        // * Idle.
        // */
        // IDLE,
        // /**
        // * Sum of other processes.
        // */
        // OTHER_PROCESSES,
        /**
         * Total node cpu usage.
         */
        NODE_CPU_USAGE,
        /**
         * Total node ram usage.
         */
        NODE_RAM_USAGE
        // /**
        // * Total node ram.
        // */
        // TOTAL_NODE_RAM
    }

    /**
     * 
     * A tree node representing the different instance resource informations such as idle and system ram.
     * 
     * @author David Scholz
     * @author Robert Mischke
     */
    private class InstanceResourceInfoNode implements ContributedNetworkViewNodeWithParent {

        private double cpuUsage;

        private double cpuOther;

        private double cpuIdle;

        private long ramUsed;

        private final long ramTotal;

        private final InstanceNodeContext context;

        private final String nodeIdString;

        private final Object parentNode;

        InstanceResourceInfoNode(Object parentNode, InstanceNodeContext context, double cpuUsage, double otherCpuUsage,
            double idleCpu, String nodeId) {
            this.parentNode = parentNode;
            this.cpuUsage = cpuUsage;
            this.context = context;
            this.cpuOther = otherCpuUsage;
            this.cpuIdle = idleCpu;
            this.ramTotal = 0;
            this.nodeIdString = nodeId;
        }

        InstanceResourceInfoNode(Object parentNode, InstanceNodeContext context, long ram, long ramTotal) {
            this.parentNode = parentNode;
            this.ramUsed = ram;
            this.ramTotal = ramTotal;
            this.context = context;
            this.nodeIdString = null;
        }

        @Override
        public Object getParentNode() {
            return parentNode;
        }

        public double getCpuUsage() {
            return cpuUsage;
        }

        public InstanceNodeContext getInstanceNodeContext() {
            return context;
        }

        public long getRam() {
            return ramUsed;
        }

        public void setRam(long ram) {
            this.ramUsed = ram;
        }

        public void setCpuUsage(double usage, double other, double idle) {
            this.cpuUsage = usage;
            this.cpuOther = other;
            this.cpuIdle = idle;
        }

        @Override
        public NetworkViewContributor getContributor() {
            return MonitoringDataContributor.this;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            if (context == null) {
                result = prime * result;
            } else {
                result = prime * result + context.hashCode();
            }
            if (nodeIdString == null) {
                result = prime * result;
            } else {
                result = prime * result + nodeIdString.hashCode();
            }

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
            InstanceResourceInfoNode other = (InstanceResourceInfoNode) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (context != other.context) {
                return false;
            }
            if (nodeIdString == null) {
                if (other.nodeIdString != null) {
                    return false;
                }
            } else if (!nodeIdString.equals(other.nodeIdString)) {
                return false;
            }
            return true;
        }

        private MonitoringDataContributor getOuterType() {
            return MonitoringDataContributor.this;
        }

    }

    /**
     * 
     * A tree node representing process information.
     * 
     * @author David Scholz
     */
    private class ProcessInfoNode implements ContributedNetworkViewNodeWithParent {

        private ProcessInformation processInfo;

        private long pid;

        private Object parentNode;

        ProcessInfoNode(Object parentNode, ProcessInformation processInfo) {
            this.parentNode = parentNode;
            this.processInfo = processInfo;
            this.pid = processInfo.getPid();
        }

        public ProcessInformation getProcessInfo() {
            return processInfo;
        }

        public List<ProcessInformation> getChildren() {
            return processInfo.getChildren();
        }

        public Object getParentNode() {
            return parentNode;
        }

        @Override
        public NetworkViewContributor getContributor() {
            return MonitoringDataContributor.this;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            final int i = 32;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + (int) (pid ^ (pid >>> i));
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
            ProcessInfoNode other = (ProcessInfoNode) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (pid != other.pid) {
                return false;
            }

            return true;
        }

        private MonitoringDataContributor getOuterType() {
            return MonitoringDataContributor.this;
        }

    }

    /**
     * 
     * A tree node representing rce subprocesses or rce instance informations.
     * 
     * @author David Scholz
     */
    private class RceNode implements ContributedNetworkViewNodeWithParent {

        private final boolean typeIsSubProcessRoot;

        private double cpuUsage;

        private List<ProcessInformation> children;

        private String nodeIdString;

        private final Object parentNode;

        RceNode(Object parentNode, boolean typeIsSubProcessRoot, double cpuUsage, List<ProcessInformation> children,
            String nodeIdString) {
            this.parentNode = parentNode;
            this.typeIsSubProcessRoot = typeIsSubProcessRoot;
            this.cpuUsage = cpuUsage;
            this.children = children;
            this.nodeIdString = nodeIdString;
        }

        public boolean getTypeIsSubProcessRoot() {
            return typeIsSubProcessRoot;
        }

        public double getCpuUsage() {
            return cpuUsage;
        }

        public void setCpuUsage(double usage) {
            this.cpuUsage = usage;
        }

        public void setChildren(List<ProcessInformation> children) {
            this.children = children;
        }

        public List<ProcessInformation> getChildren() {
            return children;
        }

        @Override
        public Object getParentNode() {
            return parentNode;
        }

        @Override
        public NetworkViewContributor getContributor() {
            return MonitoringDataContributor.this;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            final int f = 1237;
            final int t = 1231;
            result = prime * result + getOuterType().hashCode();
            if (nodeIdString == null) {
                result = prime * result;
            } else {
                result = prime * result + nodeIdString.hashCode();
            }
            if (typeIsSubProcessRoot) {
                result = prime * result + t;
            } else {
                result = prime * result + f;
            }
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
            RceNode other = (RceNode) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (nodeIdString == null) {
                if (other.nodeIdString != null) {
                    return false;
                }
            } else if (!nodeIdString.equals(other.nodeIdString)) {
                return false;
            }
            if (typeIsSubProcessRoot != other.typeIsSubProcessRoot) {
                return false;
            }
            return true;
        }

        private MonitoringDataContributor getOuterType() {
            return MonitoringDataContributor.this;
        }

    }

    @Override
    public int getRootElementsPriority() {
        return 0; // disabled
    }

    @Override
    public Object[] getTopLevelElements(Object parentNode) {
        return null; // disabled
    }

    @Override
    public int getInstanceDataElementsPriority() {
        return MONITORING_DATA_PRIORITY;
    }

    @Override
    public Object[] getChildrenForNetworkInstanceNode(NetworkGraphNodeWithContext instanceNode) {
        Object[] objectArray = new Object[1];
        objectArray[0] = new MonitoringDataFolderRootNode(instanceNode);
        return objectArray;
    }

    @Override
    public boolean hasChildren(Object parentNode) {
        if (parentNode instanceof MonitoringDataFolderRootNode) {
            return true;
        } else if (parentNode instanceof InstanceResourceInfoNode) {
            return false;
        } else if (parentNode instanceof RceNode) {
            List<ProcessInformation> children = ((RceNode) parentNode).getChildren();
            return !children.isEmpty();
        } else if (parentNode instanceof ProcessInfoNode) {
            List<ProcessInformation> children = ((ProcessInfoNode) parentNode).getChildren();
            return !children.isEmpty();
        }

        return false;
    }

    @Override
    public Object[] getChildren(Object parentNode) {
        if (parentNode instanceof MonitoringDataFolderRootNode) {
            return createMonitoringRootFolderContent(parentNode);
        } else if (parentNode instanceof RceNode) {
            return createRceNodeFolderContent(parentNode);
        } else if (parentNode instanceof ProcessInfoNode) {
            return createProcessInfoNodeContent(parentNode);
        }

        return null;
    }

    @Override
    public Object getParent(Object node) {
        if (node instanceof MonitoringDataFolderRootNode) {
            return ((MonitoringDataFolderRootNode) node).getInstanceNode();
        } else {
            return null; // error; will cause an upstream warning
        }
    }

    @Override
    public String getText(Object node) {

        if (node instanceof MonitoringDataFolderRootNode) {
            return "Monitoring Data";
        } else if (node instanceof InstanceResourceInfoNode) {
            InstanceResourceInfoNode infoNode = (InstanceResourceInfoNode) node;
            String text;
            switch (infoNode.getInstanceNodeContext()) {
            // case IDLE:
            // text = StringUtils.format("System Idle: %.2f%%", infoNode.getCpuUsage() * CONVERT_PERCENTAGE);
            // break;
            // case OTHER_PROCESSES:
            // text = StringUtils.format("Other processes: %.2f%%", infoNode.getCpuUsage() * CONVERT_PERCENTAGE);
            // break;
            case NODE_CPU_USAGE:
                // note: difficult to phrase "processes not related to this instance" understandably;
                // "Non-Instance processes" is the best solution found so far
                text =
                    StringUtils.format("Total CPU usage: %.2f%% (Non-Instance processes: %.2f%%, Idle: %.2f%%)", infoNode.getCpuUsage()
                        * PERCENTAGE_TO_DISPLAY_VALUE_MULTIPLIER, infoNode.cpuOther * PERCENTAGE_TO_DISPLAY_VALUE_MULTIPLIER,
                        infoNode.cpuIdle * PERCENTAGE_TO_DISPLAY_VALUE_MULTIPLIER);
                break;
            // case TOTAL_NODE_RAM:
            // text = StringUtils.format("Total RAM: %d MB", infoNode.getRam());
            // break;
            case NODE_RAM_USAGE:
                text = StringUtils.format("Total RAM usage: %d / %d MiB", infoNode.getRam(), infoNode.ramTotal);
                break;
            default:
                text = "Fail..."; // should never happen. would be funny though..
                break;
            }
            return text;
        } else if (node instanceof RceNode) {
            RceNode rce = (RceNode) node;
            if (rce.getTypeIsSubProcessRoot()) {
                return StringUtils.format("RCE Tools CPU Usage: %.2f%%", rce.getCpuUsage() * PERCENTAGE_TO_DISPLAY_VALUE_MULTIPLIER);
            } else {
                return StringUtils.format("RCE CPU Usage: %.2f%%", rce.getCpuUsage() * PERCENTAGE_TO_DISPLAY_VALUE_MULTIPLIER);
            }
        } else if (node instanceof ProcessInfoNode) {
            ProcessInfoNode processInfo = (ProcessInfoNode) node;
            return StringUtils.format(processInfo.getProcessInfo().getName() + ": %.2f%% [%d]", processInfo.getProcessInfo().getCpuUsage()
                * PERCENTAGE_TO_DISPLAY_VALUE_MULTIPLIER, processInfo.getProcessInfo().getPid());
        }

        return null;
    }

    @Override
    public Image getImage(Object node) {
        if (node instanceof MonitoringDataFolderRootNode || node instanceof RceNode) {
            return sharedFolderImage;
        } else if (node instanceof InstanceResourceInfoNode) {
            InstanceResourceInfoNode infoNode = (InstanceResourceInfoNode) node;
            switch (infoNode.getInstanceNodeContext()) {
            case NODE_CPU_USAGE:
                return getDynamicCPUImage(infoNode.getCpuUsage());
            case NODE_RAM_USAGE:
                return getDynamicRamImage(infoNode.getRam(), infoNode.ramTotal);
            default:
                return dummyImage;
            }
        } else if (node instanceof ProcessInfoNode) {
            ProcessInfoNode infoNode = (ProcessInfoNode) node;
            return getDynamicCPUImage(infoNode.getProcessInfo().getCpuUsage());
        }

        return null;
    }

    private Image getDynamicRamImage(long ram, long ramTotal) {
        double percentage = ((float) ram / (float) ramTotal);
        if (percentage > DOUBLE_0_99) {
            return ramMonitorImage6;
        }
        if (percentage > DOUBLE_0_8) {
            return ramMonitorImage5;
        }
        if (percentage > DOUBLE_0_6) {
            return ramMonitorImage4;
        }
        if (percentage > DOUBLE_0_4) {
            return ramMonitorImage3;
        }
        if (percentage > DOUBLE_0_2) {
            return ramMonitorImage2;
        }
        if (percentage > DOUBLE_0_01) {
            return ramMonitorImage1;
        }
        return ramMonitorImage0;
    }

    private Image getDynamicCPUImage(double percentage) {
        if (percentage > DOUBLE_0_99) {
            return cpuMonitorImage6;
        }
        if (percentage > DOUBLE_0_8) {
            return cpuMonitorImage5;
        }
        if (percentage > DOUBLE_0_6) {
            return cpuMonitorImage4;
        }
        if (percentage > DOUBLE_0_4) {
            return cpuMonitorImage3;
        }
        if (percentage > DOUBLE_0_2) {
            return cpuMonitorImage2;
        }
        if (percentage > DOUBLE_0_01) {
            return cpuMonitorImage1;
        }
        return cpuMonitorImage0;
    }

    @Override
    public void dispose() {
        servicePublisher.dispose();
    }

    @Override
    public void setTreeViewer(TreeViewer viewer) {
        super.setTreeViewer(viewer);
        treeViewer.addTreeListener(getTreeListener());
    }

    private Object[] createMonitoringRootFolderContent(Object parentNode) {
        List<Object> result = new ArrayList<>(7);

        MonitoringDataFolderRootNode rootNode = (MonitoringDataFolderRootNode) parentNode;
        FullSystemAndProcessDataSnapshot model =
            currentModel.getMonitoringDataModelMap().get(rootNode.getInstanceNode().getNode().getNodeId());

        if (model == null) {
            Object[] placeHolder = new Object[1];
            placeHolder[0] = "Fetching monitoring data...";
            return placeHolder;
        }

        final InstanceNodeSessionId nodeId = rootNode.getInstanceNode().getNode().getNodeId();
        final String nodeIdString = nodeId.getInstanceNodeSessionIdString();
        boolean keyExists = false;
        synchronized (nodeIdToInstanceResourceInfoMap) {
            keyExists = nodeIdToRceNodeMap.containsKey(nodeId);
        }
        if (!keyExists) {
            RceNode subProcess =
                new RceNode(parentNode, true, model.getTotalSubProcessesCpuUsage(), model.getRceSubProcesses(), nodeIdString);
            RceNode rce =
                new RceNode(parentNode, false, model.getTotalRceOwnProcessesCpuUsage(), model.getRceProcessesInfo(), nodeIdString);
            List<RceNode> rceNodeList = new ArrayList<>(2);
            rceNodeList.add(rce);
            rceNodeList.add(subProcess);
            synchronized (nodeIdToInstanceResourceInfoMap) {
                nodeIdToRceNodeMap.put(nodeId, rceNodeList);
            }
            expansionsMap.put(nodeId, subProcess);
        } else {
            List<RceNode> rceList = new ArrayList<>();
            synchronized (nodeIdToInstanceResourceInfoMap) {
                rceList.addAll(nodeIdToRceNodeMap.get(nodeId));
            }
            for (RceNode node : rceList) {
                if (node.getTypeIsSubProcessRoot()) {
                    node.setChildren(model.getRceSubProcesses());
                    node.setCpuUsage(model.getTotalSubProcessesCpuUsage());
                } else {
                    node.setChildren(model.getRceProcessesInfo());
                    node.setCpuUsage(model.getTotalRceOwnProcessesCpuUsage());
                }
            }
        }
        boolean valid = false;
        synchronized (nodeIdToInstanceResourceInfoMap) {
            valid = nodeIdToInstanceResourceInfoMap.containsKey(nodeId);
        }
        if (!valid) {
            InstanceResourceInfoNode cpuUsage =
                new InstanceResourceInfoNode(parentNode, InstanceNodeContext.NODE_CPU_USAGE, model.getNodeCPUusage(), model.getIdle(),
                    model.getOtherProcessCpuUsage(), nodeIdString);
            // InstanceResourceInfoNode nodeIdle = new InstanceResourceInfoNode(InstanceNodeContext.IDLE, model.getIdle(), nodeIdString);
            // InstanceResourceInfoNode other =
            // new InstanceResourceInfoNode(InstanceNodeContext.OTHER_PROCESSES, model.getOtherProcessCpuUsage(), nodeIdString);
            // InstanceResourceInfoNode totalRam = new InstanceResourceInfoNode(InstanceNodeContext.TOTAL_NODE_RAM,
            // model.getNodeSystemRAM());
            InstanceResourceInfoNode ramUsage =
                new InstanceResourceInfoNode(parentNode, InstanceNodeContext.NODE_RAM_USAGE, model.getNodeRAMUsage(),
                    model.getNodeSystemRAM());

            List<InstanceResourceInfoNode> instanceResourceInfoList = new ArrayList<>(5);
            instanceResourceInfoList.add(cpuUsage);
            instanceResourceInfoList.add(ramUsage);
            // instanceResourceInfoList.add(nodeIdle);
            // instanceResourceInfoList.add(other);
            synchronized (nodeIdToInstanceResourceInfoMap) {
                nodeIdToInstanceResourceInfoMap.put(nodeId, instanceResourceInfoList);
            }
        } else {
            List<InstanceResourceInfoNode> nodeList = new ArrayList<>();
            synchronized (nodeIdToInstanceResourceInfoMap) {
                nodeList.addAll(nodeIdToInstanceResourceInfoMap.get(nodeId));
            }
            for (InstanceResourceInfoNode node : nodeList) {
                switch (node.getInstanceNodeContext()) {
                // case IDLE:
                // node.setCpuUsage(model.getIdle());
                // break;
                case NODE_CPU_USAGE:
                    node.setCpuUsage(model.getNodeCPUusage(), model.getOtherProcessCpuUsage(), model.getIdle());
                    break;
                // case OTHER_PROCESSES:
                // node.setCpuUsage(model.getOtherProcessCpuUsage());
                // break;
                case NODE_RAM_USAGE:
                    // no need to update total node ram as it won't change.
                    node.setRam(model.getNodeRAMUsage());
                    break;
                // case TOTAL_NODE_RAM:
                // break;
                default:
                    log.info("Wrong context of InstanceResourceInfoNode");
                    break;
                }
            }
        }

        synchronized (nodeIdToInstanceResourceInfoMap) {
            result.addAll(nodeIdToRceNodeMap.get(nodeId));
            result.addAll(nodeIdToInstanceResourceInfoMap.get(nodeId));
        }

        return result.toArray();
    }

    private Object[] createRceNodeFolderContent(Object parentNode) {
        RceNode rce = (RceNode) parentNode;
        List<ProcessInformation> childList = rce.getChildren();
        List<Object> result = new ArrayList<>(childList.size());
        for (ProcessInformation child : childList) {
            ProcessInfoNode processInfoNode = new ProcessInfoNode(parentNode, child);
            result.add(processInfoNode);
        }

        return result.toArray();
    }

    private Object[] createProcessInfoNodeContent(Object parentNode) {
        ProcessInfoNode processInfoNode = (ProcessInfoNode) parentNode;
        List<ProcessInformation> childList = processInfoNode.getChildren();
        List<Object> result = new ArrayList<>(childList.size());
        for (ProcessInformation child : childList) {
            ProcessInfoNode newProcessInfoNode = new ProcessInfoNode(parentNode, child);
            result.add(newProcessInfoNode);
        }

        return result.toArray();
    }

    private ITreeViewerListener getTreeListener() {
        return new ITreeViewerListener() {

            @Override
            public void treeExpanded(TreeExpansionEvent expanded) {
                Object element = expanded.getElement();
                if (element instanceof MonitoringDataFolderRootNode) {
                    startSystemMonitoring(element);
                } else {
                    checkChildrenForSystemMonitoring(true, element);
                }
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent collapsed) {
                Object element = collapsed.getElement();
                if (element instanceof MonitoringDataFolderRootNode) {
                    stopSystemMonitoring((MonitoringDataFolderRootNode) element);
                } else {
                    checkChildrenForSystemMonitoring(false, element);
                }
            }

            /**
             * Checks for an element of the network tree, if a direct or indirect child of this element is an instance of
             * {@link MonitoringDataFolderRootNode} and starts or stops the system monitoring of the corresponding network instance if the
             * node became visible respectively invisible in the network tree.
             * 
             * @param expanding True, if the node was expanded by the user. False, otherwise.
             * @param element The element of the network tree whose state changed.
             */
            private void checkChildrenForSystemMonitoring(boolean expanding, Object element) {

                IContentProvider tmpProvider = treeViewer.getContentProvider();
                if (tmpProvider instanceof ITreeContentProvider) {
                    NetworkViewContentProvider provider = (NetworkViewContentProvider) tmpProvider;

                    Stack<Object> children = new Stack<Object>();
                    children.push(element);
                    while (!children.isEmpty()) {
                        Object child = children.pop();
                        if (child instanceof MonitoringDataFolderRootNode) {
                            if (treeViewer.getExpandedState(child)) {

                                if (expanding) {
                                    startSystemMonitoring(child);
                                } else {
                                    stopSystemMonitoring((MonitoringDataFolderRootNode) child);
                                }
                            }
                        } else if (!(child instanceof SelfRenderingNetworkViewNode)) {
                            // add further children to the stack if they are expanded
                            if (provider.hasChildren(child)) {
                                for (Object tmpChild : provider.getChildren(child)) {
                                    if (treeViewer.getExpandedState(tmpChild)) {
                                        children.add(tmpChild);
                                    }
                                }
                            }
                        }

                    }
                } else {
                    log.debug("The current content provider is not an instance of ITreeContentProvider");
                }
            }

            private void stopSystemMonitoring(MonitoringDataFolderRootNode element) {
                final InstanceNodeSessionId nodeId = (element).getInstanceNode().getNode().getNodeId();
                pollingManager.cancelPollingTask(nodeId);
                synchronized (nodeIdToInstanceResourceInfoMap) {
                    nodeIdToInstanceResourceInfoMap.clear();
                    nodeIdToRceNodeMap.clear();
                }
            }

            private void startSystemMonitoring(Object element) {
                final InstanceNodeSessionId node = ((MonitoringDataFolderRootNode) element).getInstanceNode().getNode().getNodeId();
                // TODO check: this looks like a (minor) memory leak; the map is never reduced - misc_ro, Nov 2015
                idToNodeMap.put(node, (ContributedNetworkViewNode) element);
                if (node != null) {
                    pollingManager.startPollingTask(node, new SystemMonitoringDataSnapshotListener() {

                        @Override
                        public void onMonitoringDataChanged(final FullSystemAndProcessDataSnapshot monitoringModel) {
                            if (display.isDisposed()) {
                                pollingManager.cancelPollingTask(node);
                                return;
                            }
                            display.asyncExec(new Runnable() {

                                @Override
                                public void run() {
                                    currentModel.monitoringDataModelMap.put(node, monitoringModel);
                                    if (treeViewer.getControl().isDisposed()) {
                                        pollingManager.cancelPollingTask(node);
                                        return;
                                    }
                                    final ContributedNetworkViewNode monitoringRootElementForInstance = idToNodeMap.get(node);
                                    if (monitoringRootElementForInstance != null) {
                                        treeViewer.refresh(monitoringRootElementForInstance, true);
                                    } else {
                                        log.debug("Root element is null for node " + node + " - skipping refresh");
                                    }
                                    ContributedNetworkViewNode expansionNode = expansionsMap.get(node);
                                    if (expansionNode != null) {
                                        treeViewer.expandToLevel(expansionNode, TreeViewer.ALL_LEVELS);
                                    } else {
                                        // this will currently happen most of the time until fixed - misc_ro
                                        log.debug("Expansion node is null for node " + node + " - skipping auto-expansion");
                                    }
                                }
                            });

                        }
                    });
                }
            }
        };
    }

    private void registerChangeListeners() {
        servicePublisher.registerService(NetworkTopologyChangeListener.class, new NetworkTopologyChangeListenerAdapter() {

            @Override
            public void onReachableNodesChanged(Set<InstanceNodeSessionId> reachableNodes, Set<InstanceNodeSessionId> addedNodes,
                Set<InstanceNodeSessionId> removedNodes) {
                synchronized (nodeIdToInstanceResourceInfoMap) {
                    for (InstanceNodeSessionId removedNode : removedNodes) {
                        if (nodeIdToInstanceResourceInfoMap.containsKey(removedNode)) {
                            nodeIdToInstanceResourceInfoMap.remove(removedNode);
                        }
                        if (nodeIdToRceNodeMap.containsKey(removedNode)) {
                            nodeIdToRceNodeMap.remove(removedNode);
                        }
                    }
                }
                pollingManager.cancelPollingTasks(removedNodes);
            }
        });
    }

}
