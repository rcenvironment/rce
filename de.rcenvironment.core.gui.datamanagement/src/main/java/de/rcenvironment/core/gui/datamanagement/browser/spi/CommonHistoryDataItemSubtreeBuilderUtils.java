/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.datamanagement.browser.spi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.component.datamanagement.api.CommonComponentHistoryDataItem;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.datamanagement.api.DefaultComponentHistoryDataItem;
import de.rcenvironment.core.component.datamanagement.api.EndpointHistoryDataItem;
import de.rcenvironment.core.datamanagement.commons.MetaDataKeys;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.DataTypeException;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumConverter;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.datamodel.types.api.MatrixTD;
import de.rcenvironment.core.datamodel.types.api.NotAValueTD;
import de.rcenvironment.core.datamodel.types.api.SmallTableTD;
import de.rcenvironment.core.datamodel.types.api.VectorTD;
import de.rcenvironment.core.gui.datamanagement.browser.Activator;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.StringUtils;
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
     * short text seperator between content and title.
     */
    public static final String COLON = ": ";

    /**
     * max length of short text content.
     */
    public static final int MAX_LABEL_LENGTH = 30;

    private static final String STRING_CONVERSION_INFORMATION =
        "{\"guiName\":\"Converted from data type %s\", \"value\":\"%s\"}";

    private static final String NOT_CONVERTIBLE_MESSAGE = "Datum of type '%s' is not convertible to data type '%s' expected by input '%s'";

    private static final int MAX_NON_PERSISTENT_ENTRIES = 1000;

    private static final String LEAF_TEXT_FORMAT = "%s: %s";

    private static final List<String> META_DATA_KEYS_TO_HIDE = new ArrayList<String>(Arrays.asList(
        // Set all meta data keys that should not be displayed in the workflow data browser.
        MetaDataKeys.DATA_TYPE));

    private static Log logger = LogFactory.getLog(CommonHistoryDataItemSubtreeBuilderUtils.class);

    private static ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();

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
        buildExitCodeNode(dataItem, parent);
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
                    handleDataItem(currentInput, name, inputNode, parent.getDataReferenceId(), EndpointType.INPUT,
                        dataItem.getMetaDataForInput(name));
                    // for more than one value per endpoint at one step: endpoint name as child node, values as leaf nodes
                } else if (inputsDeque.size() > 1) {
                    DMBrowserNodeType type = getDMBrowserNodeTypeByDataType(inputsDeque.peekFirst().getValue().getDataType());
                    DMBrowserNode inputNameNode = DMBrowserNode.addNewChildNode(name, type, inputNode);
                    for (EndpointHistoryDataItem item : inputsDeque) {
                        handleDataItem(item, name, inputNameNode, parent.getDataReferenceId(), EndpointType.INPUT,
                            dataItem.getMetaDataForInput(name));
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
                    handleDataItem(currentOutput, name, outputNode, parent.getDataReferenceId(), EndpointType.OUTPUT,
                        dataItem.getMetaDataForOutput(name));
                } else if (outputsDeque.size() > 1) {
                    // for more than one value per endpoint at one step: endpoint name as child node, values as leaf nodes
                    DMBrowserNodeType type = getDMBrowserNodeTypeByDataType(outputsDeque.peekFirst().getValue().getDataType());
                    DMBrowserNode outputNameNode = DMBrowserNode.addNewChildNode(name, type, outputNode);
                    for (EndpointHistoryDataItem item : outputsDeque) {
                        handleDataItem(item, name, outputNameNode, parent.getDataReferenceId(), EndpointType.OUTPUT,
                            dataItem.getMetaDataForOutput(name));
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
    public static void buildExitCodeNode(CommonComponentHistoryDataItem dataItem, DMBrowserNode parent) {

        if (dataItem.getExitCode() != null) {
            DMBrowserNode executionLogNode = null;
            for (DMBrowserNode node : parent.getChildren()) {
                if (node.getTitle().equals(DMBrowserNodeConstants.NODE_NAME_EXECUTION_LOG)) {
                    executionLogNode = node;
                    break;
                }
            }
            if (executionLogNode == null) {
                executionLogNode = DMBrowserNode.addNewChildNode(DMBrowserNodeConstants.NODE_NAME_EXECUTION_LOG,
                    DMBrowserNodeType.LogFolder, parent);
            }

            if (dataItem.getExitCode() != null) {
                DMBrowserNode.addNewLeafNode(StringUtils.format("%s: %d", "Exit code", dataItem.getExitCode()),
                    DMBrowserNodeType.InformationText, executionLogNode);
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
        } else if (dataType == DataType.Matrix) {
            type = DMBrowserNodeType.Matrix;
        } else {
            // use information type as fallback
            type = DMBrowserNodeType.InformationText;
        }
        return type;
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
            node.setTitle(StringUtils.format(LEAF_TEXT_FORMAT, item.getEndpointName(), fileReference.getFileName()));
        }
    }

    private static void addDirectoryReference(EndpointHistoryDataItem item, DMBrowserNode node) {
        if (item.getValue() instanceof DirectoryReferenceTD) {
            DirectoryReferenceTD directoryReference = (DirectoryReferenceTD) item.getValue();
            node.setAssociatedFilename(directoryReference.getDirectoryName());
            node.setDataReferenceId(directoryReference.getDirectoryReference());
            node.setTitle(StringUtils.format(LEAF_TEXT_FORMAT, item.getEndpointName(), directoryReference.getDirectoryName()));
            node.setDirectoryReferenceTD((DirectoryReferenceTD) item.getValue());
        }
    }

    private static String getAbbreviatedContent(TypedDatum datum) {
        DataType dataType = datum.getDataType();
        switch (dataType) {
        case SmallTable:
            SmallTableTD table = (SmallTableTD) datum;
            return table.toLengthLimitedString(MAX_LABEL_LENGTH);
        case Vector:
            VectorTD vector = (VectorTD) datum;
            return vector.toLengthLimitedString(MAX_LABEL_LENGTH);
        case Matrix:
            MatrixTD matrix = (MatrixTD) datum;
            return matrix.toLengthLimitedString(MAX_LABEL_LENGTH);
        default:
            return datum.toString();
        }
    }

    private static String handleBooleanDigitShortTextLabel(EndpointHistoryDataItem item, String endpointName, DMBrowserNode node) {
        String fullContent = item.getValue().toString();
        return handleLabel(fullContent, org.apache.commons.lang3.StringUtils.abbreviate(fullContent, MAX_LABEL_LENGTH), endpointName, node);
    }

    private static void handleNotAValueLabel(EndpointHistoryDataItem item, String endpointName, DMBrowserNode node) {
        NotAValueTD notAValue = (NotAValueTD) item.getValue();
        String labelText = notAValue.toString();
        if (notAValue.getCause().equals(NotAValueTD.Cause.Failure)) {
            labelText += " [cause: some component failed]";
        } else {
            labelText += " [cause: explicitly sent by some component]";
        }
        node.setTitle(StringUtils.format(LEAF_TEXT_FORMAT, endpointName, labelText));
    }

    private static String handleSmallTableLabel(EndpointHistoryDataItem item, String endpointName, DMBrowserNode node) {
        SmallTableTD table = (SmallTableTD) item.getValue();
        String abbreviatedContent = table.toLengthLimitedString(MAX_LABEL_LENGTH);
        if (table.getColumnCount() * table.getRowCount() > MAX_NON_PERSISTENT_ENTRIES) {
            node.setSmallTableTDAndFileName(table, endpointName);
            return handleLabel(abbreviatedContent, endpointName, node);
        } else {
            return handleLabel(table.toString(), abbreviatedContent, endpointName, node);
        }
    }

    private static String handleMatrixLabel(EndpointHistoryDataItem item, String endpointName, DMBrowserNode node) {
        MatrixTD matrix = (MatrixTD) item.getValue();
        String abbreviatedContent = matrix.toLengthLimitedString(MAX_LABEL_LENGTH);
        if (matrix.getRowDimension() * matrix.getColumnDimension() > MAX_NON_PERSISTENT_ENTRIES) {
            node.setMatrixTDAndFileName(matrix, endpointName);
            return handleLabel(abbreviatedContent, endpointName, node);
        } else {
            return handleLabel(matrix.toString(), abbreviatedContent, endpointName, node);
        }
    }

    private static String handleVectorLabel(EndpointHistoryDataItem item, String endpointName, DMBrowserNode node) {
        VectorTD vector = (VectorTD) item.getValue();
        String abbreviatedContent = vector.toLengthLimitedString(MAX_LABEL_LENGTH);
        if (vector.getRowDimension() > MAX_NON_PERSISTENT_ENTRIES) {
            node.setVectorTDAndFileName(vector, endpointName);
            return handleLabel(abbreviatedContent, endpointName, node);
        } else {
            return handleLabel(vector.toString(), abbreviatedContent, endpointName, node);
        }
    }

    private static String handleLabel(String abbreviatedContent, String endpointName, DMBrowserNode node) {
        String formattedLabel = StringUtils.format(LEAF_TEXT_FORMAT, endpointName, abbreviatedContent);
        node.setTitle(formattedLabel);
        return formattedLabel;
    }

    private static String handleLabel(String fullContent, String abbreviatedContent, String endpointName, DMBrowserNode node) {
        node.setFileContentAndName(fullContent, endpointName);
        return handleLabel(abbreviatedContent, endpointName, node);
    }

    private static void handleDataItem(EndpointHistoryDataItem item, String name, DMBrowserNode parent,
        String historyItemDataReferenceId, EndpointType endpointType, Map<String, String> endpointMetaData) {
        boolean hasMetaData = endpointMetaData != null && !endpointMetaData.isEmpty();
        //Handle older DB entries that did not store the endpoint data type, but only the typed datum data type (<RCE 8.0)
        DataType currentDataType = item.getValue().getDataType();
        if (hasMetaData) {
            if (endpointMetaData.containsKey(MetaDataKeys.DATA_TYPE) && currentDataType != DataType.NotAValue) {
                //Replace currentDataType if the data type of the endoint is stored in the db (>RCE 8.0)
                currentDataType = DataType.byShortName(endpointMetaData.get(MetaDataKeys.DATA_TYPE));
                if (!item.getValue().getDataType().equals(currentDataType)) {
                    try {
                        // create a dummy instance; this is required by the OSGi ServiceRegistryAccess to determine the caller's bundle -
                        // flink
                        CommonHistoryDataItemSubtreeBuilderUtils dummyInstance = new CommonHistoryDataItemSubtreeBuilderUtils();
                        ServiceRegistryAccess registryAccess = ServiceRegistry.createAccessFor(dummyInstance);
                        TypedDatumConverter converter = registryAccess.getService(TypedDatumService.class).getConverter();
                        endpointMetaData.put(MetaDataKeys.DATA_TYPE_CONVERSION,
                            StringUtils.format(
                                STRING_CONVERSION_INFORMATION,
                                item.getValue().getDataType().getDisplayName(), getAbbreviatedContent(item.getValue())));
                        item = new EndpointHistoryDataItem(item.getTimestamp(), item.getEndpointName(),
                            converter.castOrConvert(item.getValue(), currentDataType));

                    } catch (DataTypeException e) {
                        throw new RuntimeException(
                            StringUtils.format(NOT_CONVERTIBLE_MESSAGE, item.getValue().getDataType(), currentDataType, name));
                    }
                }
            }
        }
        DMBrowserNodeType type = getDMBrowserNodeTypeByDataType(currentDataType);
        DMBrowserNode node = null;
        if (currentDataType != DataType.DirectoryReference && (!hasMetaDataToDisplay(endpointMetaData))) {
            node = DMBrowserNode.addNewLeafNode(name, type, parent);
        } else if (hasMetaDataToDisplay(endpointMetaData)) {
            DMBrowserNode outputNameNode = DMBrowserNode.addNewChildNode(name, type, parent);
            node = DMBrowserNode.addNewLeafNode(name, type, outputNameNode);
        } else {
            node = DMBrowserNode.addNewChildNode(name, type, parent);
        }


        switch (currentDataType) {
        case SmallTable:
            handleSmallTableLabel(item, name, node);
            break;
        case Vector:
            handleVectorLabel(item, name, node);
            break;
        case ShortText:
        case Boolean:
        case Integer:
        case Float:
            handleBooleanDigitShortTextLabel(item, name, node);
            break;
        case NotAValue:
            handleNotAValueLabel(item, name, node);
            break;
        case Matrix:
            handleMatrixLabel(item, name, node);
            break;
        case FileReference:
            addFileReference(item, node);
            break;
        case DirectoryReference:
            addDirectoryReference(item, node);
            break;
        default:
            node.setTitle(StringUtils.format(LEAF_TEXT_FORMAT, name, item.getValue()));
        }

        if (hasMetaDataToDisplay(endpointMetaData)) {
            for (Entry<String, String> property : endpointMetaData.entrySet()) {
                if (!property.getKey().equals(MetaDataKeys.DATA_TYPE)) {
                    try {
                        JsonNode tree = mapper.readTree(property.getValue());
                        DMBrowserNode.addNewLeafNode(StringUtils.format("%s: %s", tree.get("guiName").asText(), tree.get("value").asText()),
                            DMBrowserNodeType.InformationText, node.getParent());
                    } catch (IOException e) {
                        logger.error("Could not parse endpoint properties from json string " + property.getValue());
                    }
                }
            }
        }
    }

    private static boolean hasMetaDataToDisplay(Map<String, String> endpointMetaData) {
        if (endpointMetaData != null && !endpointMetaData.isEmpty()) {
            for (String key : endpointMetaData.keySet()) {
                if (!META_DATA_KEYS_TO_HIDE.contains(key)) {
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * Builds directory subtree for endpoint.
     * 
     * @param directoryReference {@link DirectoryReferenceTD} pointing to the directory in the data management
     * @param node {@link DMBrowserNode} of the endpoint
     * @param parent parent {@link DMBrowserNode} of the endpoint {@link DMBrowserNode}
     */
    public static void buildSubtreeForDirectoryItem(DirectoryReferenceTD directoryReference, DMBrowserNode node, DMBrowserNode parent) {
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
                logger.error("Copying directory from data management to the file system failed", e);
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
}
