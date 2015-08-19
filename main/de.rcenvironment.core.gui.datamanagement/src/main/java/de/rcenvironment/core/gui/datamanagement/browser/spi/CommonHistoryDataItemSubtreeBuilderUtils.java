/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.datamanagement.browser.spi;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.datamanagement.api.CommonComponentHistoryDataItem;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.datamanagement.api.DefaultComponentHistoryDataItem;
import de.rcenvironment.core.component.datamanagement.api.EndpointHistoryDataItem;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.datamodel.types.api.MatrixTD;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import de.rcenvironment.core.datamodel.types.api.SmallTableTD;
import de.rcenvironment.core.datamodel.types.api.VectorTD;
import de.rcenvironment.core.gui.datamanagement.browser.Activator;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Provides methods to build subtrees for common component history data, such as inputs, output, or log files.
 * 
 * @author Doreen Seider
 * @author Oliver Seebach
 */
public final class CommonHistoryDataItemSubtreeBuilderUtils {

    /**
     * The title to display for the folder node containing stdout, stderr and exit node data.
     */
    public static final String EXECUTION_LOG_FOLDER_NODE_TITLE = "Execution Log";

    private static final String COLON = ": ";

    private static final Integer MAX_LABEL_LENGTH = 30;

    private static Log logger = LogFactory.getLog(CommonHistoryDataItemSubtreeBuilderUtils.class);

    private CommonHistoryDataItemSubtreeBuilderUtils() {};

    /**
     * Builds subtrees for common component history data: inputs, output, and log files.
     * 
     * @param dataItem data point with common history data
     * @param parent parent node
     */
    public static void buildDefaultHistoryDataItemSubtrees(DefaultComponentHistoryDataItem dataItem, DMBrowserNode parent) {
        buildInputsSubstree(dataItem, parent);
        buildOutputsSubtree(dataItem, parent);
    }

    /**
     * Builds subtrees for common component history data: inputs, output, and log files.
     * 
     * @param dataItem data point with common history data
     * @param parent parent node
     */
    public static void buildCommonHistoryDataItemSubtrees(CommonComponentHistoryDataItem dataItem, DMBrowserNode parent) {

        buildDefaultHistoryDataItemSubtrees(dataItem, parent);
        buildLogsSubtree(dataItem, parent);
    }

    /**
     * Builds input subtree for common component history data.
     * 
     * @param dataItem data point with common history data
     * @param parent parent node
     */
    public static void buildInputsSubstree(DefaultComponentHistoryDataItem dataItem, DMBrowserNode parent) {
        if (dataItem.getInputs().size() > 0) {
            DMBrowserNode inputNode = DMBrowserNode.addNewChildNode("Inputs", DMBrowserNodeType.Input, parent);
            List<String> inputKeyList = sortKeys(dataItem.getInputs().keySet());
            for (String name : inputKeyList) {
                Deque<EndpointHistoryDataItem> inputsDeque = dataItem.getInputs().get(name);
                // for one value per endpoint at one step
                if (inputsDeque.size() == 1) {
                    EndpointHistoryDataItem currentInput = inputsDeque.pop();
                    handleDataItem(currentInput, name, inputNode, parent.getDataReferenceId(), EndpointType.INPUT);
                    // for more than one value per endpoint at one step: endpoint name as child node, values as leaf nodes
                } else if (inputsDeque.size() > 1) {
                    DMBrowserNodeType type = getDMBrowserNodeTypeByDataType(inputsDeque.peekFirst().getValue().getDataType());
                    DMBrowserNode inputNameNode = DMBrowserNode.addNewChildNode(name, type, inputNode);
                    for (EndpointHistoryDataItem item : inputsDeque) {
                        handleDataItem(item, name, inputNameNode, parent.getDataReferenceId(), EndpointType.INPUT);
                    }
                }
            }
        }
    }

    /**
     * Builds outputs subtree for common component history data.
     * 
     * @param dataItem data point with common history data
     * @param parent parent node
     */
    public static void buildOutputsSubtree(DefaultComponentHistoryDataItem dataItem, DMBrowserNode parent) {
        if (dataItem.getOutputs().size() > 0) {
            DMBrowserNode outputNode = DMBrowserNode.addNewChildNode("Outputs", DMBrowserNodeType.Output, parent);
            List<String> outputKeyList = sortKeys(dataItem.getOutputs().keySet());
            for (String name : outputKeyList) {
                Deque<EndpointHistoryDataItem> outputsDeque = dataItem.getOutputs().get(name);
                // for one value per endpoint at one step
                if (outputsDeque.size() == 1) {
                    EndpointHistoryDataItem currentOutput = outputsDeque.pop();
                    handleDataItem(currentOutput, name, outputNode, parent.getDataReferenceId(), EndpointType.OUTPUT);
                } else if (outputsDeque.size() > 1) {
                    // for more than one value per endpoint at one step: endpoint name as child node, values as leaf nodes
                    DMBrowserNodeType type = getDMBrowserNodeTypeByDataType(outputsDeque.peekFirst().getValue().getDataType());
                    DMBrowserNode outputNameNode = DMBrowserNode.addNewChildNode(name, type, outputNode);
                    for (EndpointHistoryDataItem item : outputsDeque) {
                        handleDataItem(item, name, outputNameNode, parent.getDataReferenceId(), EndpointType.OUTPUT);
                    }
                }
            }
        }
    }

    /**
     * Builds logs subtree for common component history data.
     * 
     * @param dataItem data point with common history data
     * @param parent parent node
     */
    public static void buildLogsSubtree(CommonComponentHistoryDataItem dataItem, DMBrowserNode parent) {
        if (dataItem.getLogs().size() > 0 || dataItem.getExitCode() != null) {
            DMBrowserNode logNode = DMBrowserNode.addNewChildNode(EXECUTION_LOG_FOLDER_NODE_TITLE, DMBrowserNodeType.LogFolder, parent);

            if (dataItem.getLogs().size() > 0) {
                List<String> logKeyList = sortKeys(dataItem.getLogs().keySet());
                for (String name : logKeyList) {
                    DMBrowserNode logFileNode = DMBrowserNode.addNewLeafNode(name, DMBrowserNodeType.DMFileResource, logNode);
                    logFileNode.setAssociatedFilename(name);
                    logFileNode.setDataReferenceId(dataItem.getLogs().get(name));
                }
            }
            if (dataItem.getExitCode() != null) {
                DMBrowserNode.addNewLeafNode(String.format("%s: %d", "Exit code", dataItem.getExitCode()),
                    DMBrowserNodeType.InformationText, logNode);
            }
        }
    }

    private static DMBrowserNodeType getDMBrowserNodeTypeByDataType(DataType dataType) {
        DMBrowserNodeType type = null;
        if (dataType == DataType.ShortText) {
            type = DMBrowserNodeType.ShortText;
        } else if (dataType == DataType.Boolean) {
            type = DMBrowserNodeType.Boolean;
        } else if (dataType == DataType.Integer) {
            type = DMBrowserNodeType.Integer;
        } else if (dataType == DataType.Float) {
            type = DMBrowserNodeType.Float;
        } else if (dataType == DataType.Vector) {
            type = DMBrowserNodeType.Vector;
        } else if (dataType == DataType.SmallTable) {
            type = DMBrowserNodeType.SmallTable;
        } else if (dataType == DataType.NotAValue) {
            type = DMBrowserNodeType.Indefinite;
        } else if (dataType == DataType.FileReference) {
            type = DMBrowserNodeType.DMFileResource;
        } else if (dataType == DataType.DirectoryReference) {
            type = DMBrowserNodeType.DMDirectoryReference;
        } else {
            // use information type as fallback
            type = DMBrowserNodeType.InformationText;
        }
        return type;
    }

    private static void recursiveBrowseDirectory(File parentFile, DMBrowserNode parentNode) {
        for (File file : parentFile.listFiles()) {
            // if file is directory and contains something, add child node and go on
            if (file.isDirectory()) {
                if (file.listFiles().length > 0) {
                    DMBrowserNode node = DMBrowserNode.addNewChildNode(file.getName(), DMBrowserNodeType.DMDirectoryReference, parentNode);
                    recursiveBrowseDirectory(file, node);
                } else {
                    DMBrowserNode.addNewLeafNode(file.getName(), DMBrowserNodeType.DMDirectoryReference, parentNode);
                }
            } else {
                // else add leaf node for each item and finish
                DMBrowserNode node = DMBrowserNode.addNewLeafNode(file.getName(), DMBrowserNodeType.DMFileResource, parentNode);
                node.setAssociatedFilename(file.getName());
                node.setFileReferencePath(file.getAbsolutePath());
            }
        }
    }

    private static List<String> sortKeys(Set<String> unsortedKeys) {
        List<String> sortedKeys = new ArrayList<>();
        sortedKeys.addAll(unsortedKeys);
        Collections.sort(sortedKeys);
        return sortedKeys;
    }

    private static void addFileReference(EndpointHistoryDataItem item, DMBrowserNode node) {
        if (item.getValue() instanceof FileReferenceTD) {
            FileReferenceTD fileReference = (FileReferenceTD) item.getValue();
            node.setAssociatedFilename(fileReference.getFileName());
            node.setDataReferenceId(fileReference.getFileReference());
            node.setTitle(String.format("%s: %s", item.getEndpointName(), fileReference.getFileName()));
        }
    }

    private static void addDirectoryReference(EndpointHistoryDataItem item, DMBrowserNode node) {
        if (item.getValue() instanceof DirectoryReferenceTD) {
            DirectoryReferenceTD directoryReference = (DirectoryReferenceTD) item.getValue();
            node.setAssociatedFilename(directoryReference.getDirectoryName());
            node.setDataReferenceId(directoryReference.getDirectoryReference());
            node.setTitle(String.format("%s: %s", item.getEndpointName(), directoryReference.getDirectoryName()));
        }
    }

    private static String handleCommonTextLabel(EndpointHistoryDataItem item, String endpointName, DMBrowserNode node,
        String historyItemDataReferenceId, EndpointType endpointType) {
        String text = item.getValue().toString();
        String formattedLabel = "";
        String fullContent = text;

        formattedLabel += endpointName;
        formattedLabel += COLON;
        if (text.length() > MAX_LABEL_LENGTH) {
            text = text.substring(0, MAX_LABEL_LENGTH - 4) + "...";
        }
        formattedLabel += String.format("'%s'", text);

        node.setTitle(formattedLabel);

        addTextAsFileReferenceToNode(endpointName, fullContent, node, historyItemDataReferenceId, endpointType);

        return formattedLabel;
    }

    private static String handleShortTextLabel(EndpointHistoryDataItem item, String endpointName, DMBrowserNode node,
        String historyItemDataReferenceId, EndpointType endpointType) {
        String text = "";
        String formattedLabel = "";
        String fullContent = "";

        formattedLabel += endpointName;
        formattedLabel += COLON;
        if (item.getValue() instanceof ShortTextTD) {
            ShortTextTD shortText = (ShortTextTD) item.getValue();
            text += shortText.toLengthLimitedString(MAX_LABEL_LENGTH);
            fullContent = shortText.toString();
        }

        formattedLabel += String.format("'%s'", text);
        node.setTitle(formattedLabel);

        addTextAsFileReferenceToNode(endpointName, fullContent, node, historyItemDataReferenceId, endpointType);

        return formattedLabel;
    }

    private static String handleSmallTableLabel(EndpointHistoryDataItem item, String endpointName, DMBrowserNode node,
        String historyItemDataReferenceId, EndpointType endpointType) {
        String formattedLabel = "";
        String fullContent = "";

        formattedLabel += endpointName;
        formattedLabel += COLON;
        if (item.getValue() instanceof SmallTableTD) {
            SmallTableTD smallTable = (SmallTableTD) item.getValue();
            formattedLabel += smallTable.toLengthLimitedString(MAX_LABEL_LENGTH);
            fullContent = smallTable.toString();
        }
        node.setTitle(formattedLabel);

        addTextAsFileReferenceToNode(endpointName, fullContent, node, historyItemDataReferenceId, endpointType);

        return formattedLabel;
    }

    private static String handleVectorLabel(EndpointHistoryDataItem item, String endpointName, DMBrowserNode node,
        String historyItemDataReferenceId, EndpointType endpointType) {
        String formattedLabel = "";
        String fullContent = "";

        formattedLabel += endpointName;
        formattedLabel += COLON;
        if (item.getValue() instanceof VectorTD) {
            VectorTD vector = (VectorTD) item.getValue();
            formattedLabel += vector.toLengthLimitedString(MAX_LABEL_LENGTH);
            fullContent += vector.toString();
        }

        node.setTitle(formattedLabel);

        addTextAsFileReferenceToNode(endpointName, fullContent, node, historyItemDataReferenceId, endpointType);

        return formattedLabel;
    }

    private static String handleMatrixLabel(EndpointHistoryDataItem item, String endpointName, DMBrowserNode node,
        String historyItemDataReferenceId, EndpointType endpointType) {
        String formattedLabel = "";
        String fullContent = "";

        formattedLabel += endpointName;
        formattedLabel += COLON;
        if (item.getValue() instanceof MatrixTD) {
            MatrixTD matrix = (MatrixTD) item.getValue();
            formattedLabel += matrix.toLengthLimitedString(MAX_LABEL_LENGTH);
            fullContent += matrix.toString();
        }

        node.setTitle(formattedLabel);

        addTextAsFileReferenceToNode(endpointName, fullContent, node, historyItemDataReferenceId, endpointType);

        return formattedLabel;
    }

    private static void handleDataItem(EndpointHistoryDataItem item, String name, DMBrowserNode parent,
        String historyItemDataReferenceId, EndpointType endpointType) {
        DataType currentDataType = item.getValue().getDataType();
        DMBrowserNodeType type = getDMBrowserNodeTypeByDataType(currentDataType);
        DMBrowserNode node = null;
        if (currentDataType != DataType.DirectoryReference) {
            node = DMBrowserNode.addNewLeafNode(name, type, parent);
        } else {
            node = DMBrowserNode.addNewChildNode(name, type, parent);
        }

        if (currentDataType == DataType.SmallTable) {
            handleSmallTableLabel(item, name, node, historyItemDataReferenceId, endpointType);
        } else if (currentDataType == DataType.Vector) {
            handleVectorLabel(item, name, node, historyItemDataReferenceId, endpointType);
        } else if (currentDataType == DataType.ShortText) {
            handleShortTextLabel(item, name, node, historyItemDataReferenceId, endpointType);
        } else if (currentDataType == DataType.Boolean
            || currentDataType == DataType.Integer
            || currentDataType == DataType.Float) {
            handleCommonTextLabel(item, name, node, historyItemDataReferenceId, endpointType);
        } else if (currentDataType == DataType.Matrix) {
            handleMatrixLabel(item, name, node, historyItemDataReferenceId, endpointType);
        } else if (currentDataType == DataType.FileReference) {
            addFileReference(item, node);
        } else if (currentDataType == DataType.DirectoryReference) {
            addDirectoryReference(item, node);
            if (!node.isBuiltForDeletionPurpose()) {
                buildSubtreeForDirectoryItem(item, node, parent);
            }
        } else {
            node.setTitle(name + COLON + item.getValue());
        }
    }

    private static void buildSubtreeForDirectoryItem(EndpointHistoryDataItem item, DMBrowserNode node, DMBrowserNode parent) {
        DirectoryReferenceTD directoryReference = null;
        if (item.getValue() instanceof DirectoryReferenceTD) {
            directoryReference = (DirectoryReferenceTD) item.getValue();
        }
        File dir = new File(Activator.getInstance().getBundleSpecificTempDir(), directoryReference.getDirectoryReference());
        if (!dir.mkdir() && (!dir.exists() || dir.isFile())) {
            logger.error("Temp directory could not be created or did already exist as file: " + dir);
            return;
        }

        // Data reference ids are assumed to be unique. The folder name equals the data reference id of the directory. So, it is assumed
        // that if the folder is not empty its content was already created in previous workflow data browsing and is not fetched anew -
        // seid_do
        if (dir.list().length == 0) {
            // create a dummy instance; this is required by the OSGi ServiceRegistryAccess to determine the caller's bundle - misc_ro
            CommonHistoryDataItemSubtreeBuilderUtils dummyInstance = new CommonHistoryDataItemSubtreeBuilderUtils();
            ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(dummyInstance);
            ComponentDataManagementService componentService = serviceRegistryAccess.getService(ComponentDataManagementService.class);

            try {
                componentService.copyDirectoryReferenceTDToLocalDirectory(directoryReference, dir,
                    parent.getNodeWithTypeWorkflow().getNodeIdentifier());
            } catch (IOException e) {
                logger.error("Copying directory from data management to the file system failed");
            }
        }

        if (dir != null) {
            File f = new File(dir, directoryReference.getDirectoryName());
            if (f.listFiles() != null && f.listFiles().length > 0) {
                recursiveBrowseDirectory(new File(dir, directoryReference.getDirectoryName()), node);
            } else {
                node.markAsLeaf();
            }
        }
    }

    private static void addTextAsFileReferenceToNode(String filename, String text, DMBrowserNode node,
        String historyItemDataReferenceId, EndpointType endpointType) {
        if (!node.isBuiltForDeletionPurpose()) {
            File tempFile = null;
            try {
                File tempDir = new File(Activator.getInstance().getBundleSpecificTempDir(), UUID.randomUUID().toString());
                tempDir.mkdir();
                File endpointTempDir = new File(tempDir, endpointType.name());
                endpointTempDir.mkdir();
                tempFile = new File(endpointTempDir, filename);
                if (!tempFile.exists()) {
                    PrintWriter out = new PrintWriter(tempFile);
                    out.write(text);
                    out.flush();
                    out.close();
                }
            } catch (IOException e) {
                logger.error(e.getStackTrace());
            }

            node.setAssociatedFilename(filename);
            node.setFileReferencePath(tempFile.getAbsolutePath());
        }
    }

}
