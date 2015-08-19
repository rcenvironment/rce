/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.toolwrapper.gui.dm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import de.dlr.sc.chameleon.rce.toolwrapper.common.CpacsComponentConstants;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.gui.datamanagement.browser.spi.CommonHistoryDataItemSubtreeBuilderUtils;
import de.rcenvironment.core.gui.datamanagement.browser.spi.ComponentHistoryDataItemSubtreeBuilder;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DMBrowserNode;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DMBrowserNodeType;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;
import de.rcenvironment.cpacs.utils.common.components.ToolWrapperComponentHistoryDataItem;

/**
 * Implementation of {@link ComponentHistoryDataItemSubtreeBuilder} for the Toolwrapper component.
 * 
 * @author Sascha Zur
 */
public class ToolWrapperHistoryDataItemSubtreeBuilder implements ComponentHistoryDataItemSubtreeBuilder {

    private static Map<String, byte[]> componentIcons = Collections.synchronizedMap(new HashMap<String, byte[]>());

    @Override
    public String[] getSupportedHistoryDataItemIdentifier() {
        return new String[] { CpacsComponentConstants.COMPONENT_ID + ".*" };
    }

    @Override
    public Serializable deserializeHistoryDataItem(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        return (Serializable) ois.readObject();
    }

    @Override
    public void buildInitialHistoryDataItemSubtree(Serializable historyDataItem, DMBrowserNode parent) {

        ServiceRegistryAccess registryAccess = ServiceRegistry.createAccessFor(this);
        TypedDatumSerializer serializer = registryAccess.getService(TypedDatumService.class).getSerializer();

        if (historyDataItem instanceof String) {
            ToolWrapperComponentHistoryDataItem historyData;
            try {
                historyData = ToolWrapperComponentHistoryDataItem.fromString((String) historyDataItem, serializer, "");
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
            // CommonHistoryDataItemSubtreeBuilderUtils.buildCommonHistoryDataItemSubtrees(historyData,
            // prent);
            DMBrowserNode incomingNode =
                DMBrowserNode.addNewChildNode("Incoming", DMBrowserNodeType.InformationText, parent);
            DMBrowserNode outgoingNode =
                DMBrowserNode.addNewChildNode("Outgoing", DMBrowserNodeType.InformationText, parent);
            if (historyData.getCpacsInFileReference() != null) {
                DMBrowserNode cpacsWithVariablesNode =
                    DMBrowserNode.addNewLeafNode("Plain CPACS Incoming", DMBrowserNodeType.DMFileResource, incomingNode);
                cpacsWithVariablesNode.setAssociatedFilename("cpacs.xml");
                cpacsWithVariablesNode.setDataReferenceId(historyData.getCpacsInFileReference());
            }

            if (historyData.getCpacsVariableInFileReference() != null) {
                DMBrowserNode cpacsWithVariablesNode =
                    DMBrowserNode.addNewLeafNode("CPACS Incoming with mapped variables", DMBrowserNodeType.DMFileResource, incomingNode);
                cpacsWithVariablesNode.setAssociatedFilename("cpacsWithVariables.xml");
                cpacsWithVariablesNode.setDataReferenceId(historyData.getCpacsVariableInFileReference());
            }
            if (historyData.getToolInFileReference() != null) {
                DMBrowserNode toolInput = DMBrowserNode.addNewLeafNode("Tool Input", DMBrowserNodeType.DMFileResource, incomingNode);
                toolInput.setAssociatedFilename("toolInput.xml");
                toolInput.setDataReferenceId(historyData.getToolInFileReference());
            }
            if (historyData.getIncomingDirectoryReferences() != null) {
                Map<String, String> inDir = historyData.getIncomingDirectoryReferences();
                final SortedSet<String> inputFiles = new TreeSet<String>(inDir.keySet());
                if (inputFiles.size() != 0) {
                    DMBrowserNode incomingDirectoryNode = DMBrowserNode.addNewChildNode("Incoming Directory",
                        DMBrowserNodeType.DMDirectoryReference, incomingNode);
                    for (final String inputFile : inputFiles) {
                        DMBrowserNode fileIncoming = DMBrowserNode.addNewLeafNode(inputFile,
                            DMBrowserNodeType.DMFileResource, incomingDirectoryNode);
                        fileIncoming.setAssociatedFilename(inputFile);
                        fileIncoming.setDataReferenceId(inDir.get(inputFile));
                    }
                }
            }
            if (historyData.getDynamicInputs() != null) {
                DMBrowserNode dynamicInputsNode =
                    DMBrowserNode.addNewChildNode("Incoming Variable Values", DMBrowserNodeType.Resource, incomingNode);
                for (Entry<String, String> input : historyData.getDynamicInputs().entrySet()) {
                    DMBrowserNode.addNewLeafNode("Name: \"" + input.getKey() + "\"; Value: \"" + input.getValue() + "\"",
                        DMBrowserNodeType.InformationText, dynamicInputsNode);
                }
            }
            if (historyData.getCpacsOutFileReference() != null) {
                DMBrowserNode cpacsWithVariablesNode =
                    DMBrowserNode.addNewLeafNode("CPACS Outgoing", DMBrowserNodeType.DMFileResource, outgoingNode);
                cpacsWithVariablesNode.setAssociatedFilename("cpacs.xml");
                cpacsWithVariablesNode.setDataReferenceId(historyData.getCpacsOutFileReference());
            }
            if (historyData.getToolOutFileReference() != null) {
                DMBrowserNode toolInput = DMBrowserNode.addNewLeafNode("Tool Output", DMBrowserNodeType.DMFileResource, outgoingNode);
                toolInput.setAssociatedFilename("toolOutput.xml");
                toolInput.setDataReferenceId(historyData.getToolOutFileReference());
            }
            if (historyData.getOutgoingDirectoryReferences() != null) {
                Map<String, String> outDir = historyData.getOutgoingDirectoryReferences();
                final SortedSet<String> outputFiles = new TreeSet<String>(outDir.keySet());
                if (outputFiles.size() != 0) {
                    DMBrowserNode outgoindDirectoryNode = DMBrowserNode.addNewChildNode("Outgoing Directory",
                        DMBrowserNodeType.DMDirectoryReference, outgoingNode);
                    for (final String outputFile : outputFiles) {
                        DMBrowserNode fileIncoming = DMBrowserNode.addNewLeafNode(outputFile,
                            DMBrowserNodeType.DMFileResource, outgoindDirectoryNode);
                        fileIncoming.setAssociatedFilename(outputFile);
                        fileIncoming.setDataReferenceId(outDir.get(outputFile));
                    }
                }
            }
            if (historyData.getDynamicOutputs() != null) {
                DMBrowserNode dynamicOutputsNode =
                    DMBrowserNode.addNewChildNode("Outgoing Variable Values", DMBrowserNodeType.Resource, outgoingNode);
                for (Entry<String, String> output : historyData.getDynamicOutputs().entrySet()) {
                    DMBrowserNode.addNewLeafNode("Name: \"" + output.getKey() + "\"; Value: \"" + output.getValue() + "\"",
                        DMBrowserNodeType.InformationText, dynamicOutputsNode);
                }
            }
            CommonHistoryDataItemSubtreeBuilderUtils.buildLogsSubtree(historyData, parent);
            DMBrowserNode.addNewLeafNode("Exit Code: " + historyData.getExitCode(),
                DMBrowserNodeType.Integer, parent);
        }
    }

    @Override
    public Image getComponentIcon(String identifier) {
        if (!componentIcons.containsKey(identifier)) {
            ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
            DistributedComponentKnowledgeService componentKnowledgeService =
                serviceRegistryAccess.getService(DistributedComponentKnowledgeService.class);
            Collection<ComponentInstallation> installations =
                componentKnowledgeService.getCurrentComponentKnowledge().getAllInstallations();
            for (ComponentInstallation installation : installations) {
                if (identifier.startsWith(installation.getInstallationId())) {
                    synchronized (componentIcons) {
                        componentIcons.put(identifier, installation.getComponentRevision().getComponentInterface().getIcon16());
                    }
                }
            }
        }
        byte[] icon = null;
        Image image = null;
        synchronized (componentIcons) {
            icon = componentIcons.get(identifier);
            if (icon != null) {
                image = ImageDescriptor.createFromImage(new Image(Display.getCurrent(), new ByteArrayInputStream(icon))).createImage();
            }
        }
        return image;
    }

}
