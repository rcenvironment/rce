/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.integration.cpacs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.integration.CommonToolIntegratorComponent;
import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.component.xml.api.EndpointXMLService;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.DataTypeException;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.datamodel.types.api.NotAValueTD.Cause;
import de.rcenvironment.core.scripting.ScriptingService;
import de.rcenvironment.core.utils.common.LogUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.xml.XMLException;
import de.rcenvironment.core.utils.common.xml.XSLTErrorHandler;
import de.rcenvironment.core.utils.common.xml.api.XMLMapperService;
import de.rcenvironment.core.utils.common.xml.api.XMLSupportService;
import de.rcenvironment.toolkit.utils.text.AbstractTextLinesReceiver;

/**
 * Main class for the CPACS tool integration.
 * 
 * @author Jan Flink
 * @author Doreen Seider (logging)
 */
public class CpacsToolIntegratorComponent extends CommonToolIntegratorComponent {

    private static final Log LOG = LogFactory.getLog(CpacsToolIntegratorComponent.class);

    private static final String FILE_SUFFIX_MAPPED = "-mapped";

    private static final String STRING_CPACS_RESULT_FILE_CREATED = "Created CPACS result file '%s'.";

    private static final String SUFFIX_MAPPED = "-mapped";

    private static final String STRING_NOT_AVAILABLE_IN_WDB = "; it is not available in the workflow data browser";

    private static final String STRING_TOOL_INPUT_FILE_NOT_FOUND = "Tool input file '%s' not found";

    private static final String STRING_XML_ERROR_DURING_MAPPING = "Failed to perform %s mapping";

    private static final String STRING_TOOL_INPUT_CREATED = "Created tool input file '%s'.";

    private static final String STRING_MAPPING_USAGE = "Using %s %s mapping...";

    private static final String STRING_MAPPING_TYPE_XML = "pairing";

    private static final String STRING_MAPPING_TYPE_XSL = "raw XSLT";

    private static final String STRING_TOOL_MAPPING_DIRECTION_INPUT = "tool input";

    private static final String STRING_MAPPING_DIRECTION_INPUT = "input";

    private static final String STRING_TOOL_MAPPING_DIRECTION_OUTPUT = "tool output";

    private static final String STRING_MAPPING_DIRECTION_OUTPUT = "output";

    private static final String STRING_MAPPING_DIRECTION_TOOLSPECIFIC = "tool specific input";

    private static final String STRING_MAPPING_FILE_NOT_FOUND = "Mapping file '%s' not found";

    private static final String STRING_ERROR_SOLVING_FILE_EXTENSION = "Failed to resolve file extension of mapping file '%s'";

    private static final String CREATE_MAPPING_XSLT_FILEPATH = "/resources/CreateMapping.xslt";

    private static final String XMLFILE_SEPARATOR = "/";

    /**
     * Implementation of TextLinesReceiver for CpacsToolIntegratorComponent.
     *
     * @author Brigitte Boden
     */
    private final class CpacsToolIntegratorTextLinesReceiver extends AbstractTextLinesReceiver {

        @Override
        public void addLine(String line) {
            componentContext.getLog().componentInfo(line);
        }
    }

    private XMLMapperService xmlMapper;

    private XMLSupportService xmlSupport;

    private EndpointXMLService dynamicEndpointMapper;

    private File lastRunToolinputFile;

    private File tmpOutputFile;

    public CpacsToolIntegratorComponent() {
        super();
    }

    @Override
    public void start() throws ComponentException {
        dynamicEndpointMapper = componentContext.getService(EndpointXMLService.class);
        xmlMapper = componentContext.getService(XMLMapperService.class);
        xmlSupport = componentContext.getService(XMLSupportService.class);
        super.start();
        lastRunToolinputFile = null;
    }

    @Override
    protected void beforePreScriptExecution(Map<String, TypedDatum> inputValues, Map<String, String> inputNamesToLocalFile)
        throws ComponentException {

        Map<String, TypedDatum> dynamicInputs = new HashMap<>();
        for (String inputName : componentContext.getInputsWithDatum()) {
            if (componentContext.isDynamicInput(inputName)) {
                dynamicInputs.put(inputName, componentContext.readInput(inputName));
            }
        }

        String cpacsInitial = inputNamesToLocalFile.get(getCpacsInitialEndpointName());
        if (cpacsInitial == null) {
            throw new ComponentException(StringUtils.format(
                "Failed to read CPACS from input '%s'; input is not configured", getCpacsInitialEndpointName()));
        }
        performDynamicInputMapping(cpacsInitial, dynamicInputs);

        if (!isMockMode()) {
            performInputMapping(cpacsInitial, dynamicInputs);
            if (getHistoryDataItem() != null) {
                try {
                    File toolInputFile = new File(getToolInput());
                    String toolInputFileReference =
                        datamanagementService.createTaggedReferenceFromLocalFile(componentContext,
                            toolInputFile, toolInputFile.getName());
                    getHistoryDataItem().setToolInputFile(toolInputFile.getName(), toolInputFileReference);
                } catch (IOException e) {
                    String errorMessage = "Failed to store tool input file into the data management"
                        + STRING_NOT_AVAILABLE_IN_WDB;
                    String errorId = LogUtils.logExceptionWithStacktraceAndAssignUniqueMarker(LOG, errorMessage, e);
                    componentContext.getLog().componentError(errorMessage, e, errorId);
                }
            }
        }
    }

    private void performDynamicInputMapping(String cpacsInitial, Map<String, TypedDatum> dynamicInputs) throws ComponentException {
        try {
            dynamicEndpointMapper.updateXMLWithInputs(new File(cpacsInitial), dynamicInputs, componentContext);
        } catch (DataTypeException e) {
            throw new ComponentException("Failed to map dynamic input values into CPACS file", e);
        }
        if (!dynamicInputs.isEmpty() && getHistoryDataItem() != null) {
            try {
                String cpacsWithVariablesFileReference = datamanagementService.createTaggedReferenceFromLocalFile(componentContext,
                    new File(cpacsInitial), "cpacsWithVariables.xml");
                getHistoryDataItem().setCpacsWithVariablesFileReference(cpacsWithVariablesFileReference);
            } catch (IOException e) {
                String errorMessage = "Failed to store CPACS file with dynamic input values into the data management"
                    + STRING_NOT_AVAILABLE_IN_WDB;
                String errorId = LogUtils.logExceptionWithStacktraceAndAssignUniqueMarker(LOG, errorMessage, e);
                componentContext.getLog().componentError(errorMessage, e, errorId);
            }
        }
    }

    private void performInputMapping(String cpacsInitial, Map<String, TypedDatum> dynamicInputs) throws ComponentException {

        createIntermediateFolders();
        final File mappingFile = new File(getInputMapping());

        if (mappingFile.exists()) {
            if (mappingFile.getName().endsWith(CpacsToolIntegrationConstants.FILE_SUFFIX_XML)) {
                componentLog.componentInfo(StringUtils.format(STRING_MAPPING_USAGE,
                    STRING_MAPPING_TYPE_XML, STRING_MAPPING_DIRECTION_INPUT));
                try {
                    Document mappingDoc =
                        transformXMLMapping(mappingFile.getAbsolutePath(), cpacsInitial, org.apache.commons.lang3.StringUtils.EMPTY);
                    // Build tool input document
                    String toolInputFilePath = getToolInput();
                    xmlMapper.transformXMLFileWithXMLMappingInformation(new File(cpacsInitial), new File(toolInputFilePath), mappingDoc);
                    if (new File(toolInputFilePath).exists()) {
                        componentLog.componentInfo(StringUtils.format(STRING_TOOL_INPUT_CREATED,
                            toolInputFilePath));
                    }
                } catch (XPathExpressionException | XMLException e) {
                    throw new ComponentException(StringUtils.format(STRING_XML_ERROR_DURING_MAPPING,
                        STRING_TOOL_MAPPING_DIRECTION_INPUT), e);
                }
            } else if (mappingFile.getName().endsWith(CpacsToolIntegrationConstants.FILE_SUFFIX_XSL)) {
                componentLog.componentInfo(StringUtils.format(STRING_MAPPING_USAGE, STRING_MAPPING_TYPE_XSL,
                    STRING_MAPPING_DIRECTION_INPUT));
                try {
                    xmlMapper.transformXMLFileWithXSLT(new File(cpacsInitial), new File(getToolInput()), mappingFile,
                        new CpacsToolIntegratorTextLinesReceiver());
                } catch (XMLException e) {
                    throw new ComponentException(StringUtils.format(STRING_XML_ERROR_DURING_MAPPING,
                        STRING_TOOL_MAPPING_DIRECTION_INPUT), e);
                }
            } else {
                throw new ComponentException(StringUtils.format(
                    STRING_ERROR_SOLVING_FILE_EXTENSION, getInputMapping()));
            }
        } else {
            throw new ComponentException(StringUtils.format(STRING_MAPPING_FILE_NOT_FOUND,
                getInputMapping()));
        }
        if (hasToolspecificinputfile()) {
            if (getHistoryDataItem() != null) {
                try {
                    String toolInputFileReference =
                        datamanagementService.createTaggedReferenceFromLocalFile(componentContext,
                            new File(getToolInput()), getToolInputFileName() + FILE_SUFFIX_MAPPED);
                    getHistoryDataItem().setToolInputWithoutToolspecificFileReference(toolInputFileReference);
                } catch (IOException e) {
                    String errorMessage = "Failed to store tool input file into the data management"
                        + STRING_NOT_AVAILABLE_IN_WDB;
                    String errorId = LogUtils.logExceptionWithStacktraceAndAssignUniqueMarker(LOG, errorMessage, e);
                    componentContext.getLog().componentError(errorMessage, e, errorId);
                }
            }

            final File mappingFile1 = new File(getToolspecificInputMapping());

            if (mappingFile1.exists()) {
                if (mappingFile1.getName().endsWith(CpacsToolIntegrationConstants.FILE_SUFFIX_XML)) {
                    componentLog.componentInfo(StringUtils.format(STRING_MAPPING_USAGE,
                        STRING_MAPPING_TYPE_XML,
                        STRING_MAPPING_DIRECTION_TOOLSPECIFIC));

                    try {
                        final Document mappingDoc = transformXMLMapping(getToolspecificInputMapping(),
                            getToolspecificInputData(), getToolInput());

                        // overwrite old tool input file
                        xmlMapper.transformXMLFileWithXMLMappingInformation(new File(getToolspecificInputData()), new File(getToolInput()),
                            mappingDoc);

                    } catch (XPathExpressionException | XMLException e) {
                        throw new ComponentException(StringUtils.format(
                            STRING_XML_ERROR_DURING_MAPPING, STRING_MAPPING_DIRECTION_TOOLSPECIFIC), e);
                    }
                } else if (mappingFile1.getName().endsWith(CpacsToolIntegrationConstants.FILE_SUFFIX_XSL)) {
                    componentLog.componentInfo(StringUtils.format(STRING_MAPPING_USAGE,
                        STRING_MAPPING_TYPE_XSL,
                        STRING_MAPPING_DIRECTION_TOOLSPECIFIC));
                    File toolInputMapped = new File(getToolInput() + SUFFIX_MAPPED);
                    File toolInput = new File(getToolInput());
                    try {
                        FileUtils.copyFile(toolInput, toolInputMapped, true);
                        xmlMapper.transformXMLFileWithXSLT(new File(getToolInput() + SUFFIX_MAPPED), new File(getToolInput()),
                            new File(getToolspecificInputMapping()), null);
                    } catch (IOException e) {
                        throw new ComponentException(StringUtils.format(
                            STRING_TOOL_INPUT_FILE_NOT_FOUND,
                            getToolInput()), e);
                    } catch (XMLException e) {
                        throw new ComponentException(StringUtils.format(
                            STRING_XML_ERROR_DURING_MAPPING, STRING_MAPPING_DIRECTION_TOOLSPECIFIC));
                    }

                } else {
                    throw new ComponentException(StringUtils.format(
                        STRING_ERROR_SOLVING_FILE_EXTENSION, getToolspecificInputMapping()));
                }
            } else {
                throw new ComponentException(StringUtils.format(
                    STRING_MAPPING_FILE_NOT_FOUND, getToolspecificInputMapping()));
            }
        }
    }

    private void createIntermediateFolders() throws ComponentException {
        if (copyToolBehaviour.equals(ToolIntegrationConstants.VALUE_COPY_TOOL_BEHAVIOUR_ALWAYS)
            || copyToolBehaviour.equals(ToolIntegrationConstants.VALUE_COPY_TOOL_BEHAVIOUR_ONCE)) {
            try {
                String inputParent = new File(getToolInput()).getParent();
                String outputParent = new File(getToolOutput()).getParent();
                if (inputParent != null && !inputParent.equals(executionToolDirectory.getAbsolutePath())) {
                    FileUtils.forceMkdir(new File(inputParent));
                }
                if (outputParent != null && !outputParent.equals(executionToolDirectory.getAbsolutePath())) {
                    FileUtils.forceMkdir(new File(outputParent));
                }

            } catch (IOException e) {
                throw new ComponentException("Failed to generate intermediate tool input/output folders", e);
            }
        }
    }

    @Override
    protected boolean needToRun(Map<String, TypedDatum> inputValues, Map<String, String> inputNamesToLocalFile)
        throws ComponentException {
        if (isAlwaysRun() || isMockMode()) {
            return true;
        }
        try {
            String tmpInputFile = FileUtils.readFileToString(new File(getToolInput()));
            if (lastRunToolinputFile != null) {
                String lastRunToolinput = FileUtils.readFileToString(lastRunToolinputFile);
                // Check the alwaysRun configuration and check for changed input since the last run
                if (lastRunToolinput == null || lastRunStaticInputValues == null
                    || (tmpInputFile.compareTo(lastRunToolinput) != 0)
                    || staticInputsAreNotEqual(inputValues, inputNamesToLocalFile)) {
                    lastRunToolinput = tmpInputFile;
                    TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(lastRunToolinputFile);
                    lastRunToolinputFile = TempFileServiceAccess.getInstance().createTempFileFromPattern("CPACSLastRun*.xml");
                    FileUtils.write(lastRunToolinputFile, tmpInputFile);
                    return true;
                }
            } else {
                lastRunToolinputFile = TempFileServiceAccess.getInstance().createTempFileFromPattern("CPACSLastRun*.xml");
                FileUtils.write(lastRunToolinputFile, tmpInputFile);
                return true;
            }
        } catch (IOException e) {
            throw new ComponentException("Failed to read tool input file", e);
        }
        componentLog.componentInfo("Skipping tool execution as input values not changed compared to previous run");
        return false;
    }

    private boolean staticInputsAreNotEqual(Map<String, TypedDatum> inputValues, Map<String, String> inputNamesToLocalFile) {
        for (String inputName : inputValues.keySet()) {
            if (!inputName.equals(getCpacsInitialEndpointName())
                && componentContext.isStaticInput(inputName)) {
                if (lastRunStaticInputValues.containsKey(inputName)) {
                    if (componentContext.getInputDataType(inputName) == DataType.FileReference) {
                        String uuidLastRun = ((FileReferenceTD) lastRunStaticInputValues.get(inputName)).getFileReference();
                        String uuidCurrentRun = ((FileReferenceTD) (inputValues.get(inputName))).getFileReference();
                        try {
                            File tempDir = TempFileServiceAccess.getInstance().createManagedTempDir();
                            File lastFile = new File(tempDir, ((FileReferenceTD) lastRunStaticInputValues.get(inputName)).getFileName());
                            File currentFile =
                                new File(tempDir, ((FileReferenceTD) inputValues.get(inputName)).getFileName());
                            datamanagementService.copyReferenceToLocalFile(uuidLastRun, lastFile, componentContext
                                .getDefaultStorageNodeId());
                            datamanagementService.copyReferenceToLocalFile(uuidCurrentRun, currentFile, componentContext
                                .getDefaultStorageNodeId());
                            if (compareFiles(tempDir, lastFile, currentFile)) {
                                return true;
                            }
                        } catch (IOException e) {
                            String errorMessage = "Failed to read file (previous input value) from the data management;"
                                + " consider input values as changed";
                            String errorId = LogUtils.logExceptionWithStacktraceAndAssignUniqueMarker(LOG, errorMessage, e);
                            componentLog.componentError(errorMessage, e, errorId);
                            return true;
                        }

                    } else if (componentContext.getInputDataType(inputName) == DataType.DirectoryReference) {
                        try {
                            File tempDir = TempFileServiceAccess.getInstance().createManagedTempDir();
                            File lastDir =
                                new File(tempDir, ((DirectoryReferenceTD) lastRunStaticInputValues.get(inputName)).getDirectoryName());
                            File currentDir = new File(tempDir, ((DirectoryReferenceTD) inputValues.get(inputName)).getDirectoryName()
                                + "_curr");
                            datamanagementService.copyDirectoryReferenceTDToLocalDirectory(componentContext,
                                (DirectoryReferenceTD) lastRunStaticInputValues.get(inputName), lastDir);
                            datamanagementService.copyDirectoryReferenceTDToLocalDirectory(componentContext,
                                (DirectoryReferenceTD) inputValues.get(inputName),
                                currentDir);
                            if (!compareDirectories(tempDir, lastDir, currentDir)) {
                                return true;
                            }
                        } catch (IOException e) {
                            String errorMessage = "Failed to read directory (previous input value) from the data management;"
                                + " consider input values as changed";
                            String errorId = LogUtils.logExceptionWithStacktraceAndAssignUniqueMarker(LOG, errorMessage, e);
                            componentLog.componentError(errorMessage, e, errorId);
                            return true;
                        }

                    } else {
                        if (!lastRunStaticInputValues.get(inputName).equals(inputValues.get(inputName))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean compareDirectories(File tempDir, File lastDir, File currentDir) {
        File[] lastList = lastDir.listFiles();
        File[] currentList = currentDir.listFiles();
        String md5Last = expandFiles(lastList, "");
        String md5Current = expandFiles(currentList, "");
        return md5Last.equals(md5Current);
    }

    private String expandFiles(File[] dirFiles, String md5in) {
        String md5out = md5in;
        Arrays.sort(dirFiles);
        for (File file : dirFiles) {
            if (file.isDirectory() && !file.isHidden()) {
                md5out += expandFiles(file.listFiles(), md5out);
            } else if (file.isFile() && !file.isHidden()) {
                byte[] fileInput;
                try {
                    fileInput = FileUtils.readFileToByteArray(file);
                    md5out += DigestUtils.md5Hex(fileInput);
                } catch (IOException e) {
                    LOG.error("Failed to expand files", e);
                    md5out = "";
                }
            }
        }
        return md5out;
    }

    private boolean compareFiles(File tempDir, File lastFile, File currentFile) throws IOException {
        if (!FileUtils.contentEquals(lastFile, currentFile)) {
            lastFile.delete();
            currentFile.delete();
            TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(tempDir);
            return true;
        }
        lastFile.delete();
        currentFile.delete();
        TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(tempDir);
        return false;
    }

    @Override
    protected void afterPostScriptExecution(Map<String, TypedDatum> inputValues, Map<String, String> inputNamesToLocalFile)
        throws ComponentException {

        // if not-a-value was sent to the CPACS output, send not-a-value to all of the dynamic outputs and skip output mapping as no more
        // values must be sent to the CPACS output
        Set<String> outputsWithNotAValue = getOutputsWithNotAValueWritten();
        if (outputsWithNotAValue.contains(getCpacsOutputName())) {
            writeNotAValueToDynamicOutputs(outputsWithNotAValue);
            componentLog.componentInfo("not-a-value was sent to the output '%s' to which the resulting CPACS file is intended to "
                + "be written to; thus, output mapping is skipped, the resulting CPACS file is not sent and not-a-value is "
                + "sent to all of the dynamic outputs (if existent) which would extract some value from the resulting CPACS file");
            return;
        }

        File outputFile = new File(getToolOutput());

        if (needsToRun) {
            if (!outputFile.exists()) {
                throw new ComponentException(
                    StringUtils.format(
                        "Failed to perform output mapping. Tool output file is missing after post script execution: %s",
                        getToolOutput()));
            }
            if (!isAlwaysRun()) {
                try {
                    if (tmpOutputFile != null && tmpOutputFile.exists()) {
                        TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(tmpOutputFile);
                    }
                    tmpOutputFile = TempFileServiceAccess.getInstance().createTempFileFromPattern("cpacsToolOutput-*.xml");
                    FileUtils.copyFile(outputFile, tmpOutputFile);
                } catch (IOException e) {
                    throw new ComponentException(
                        "Failed to generate the temporary file for caching tool results.");
                }
            }
        } else {
            try {
                FileUtils.copyFile(tmpOutputFile, outputFile);
            } catch (IOException e) {
                throw new ComponentException(
                    "Failed to perform output mapping after skipped tool execution. "
                        + "The temporary tool output file of last execution is missing.");
            }
        }

        if (getHistoryDataItem() != null) {
            try {
                String toolOutputFileReference = datamanagementService.createTaggedReferenceFromLocalFile(componentContext,
                    outputFile, outputFile.getName());
                getHistoryDataItem().setToolOutputFile(outputFile.getName(), toolOutputFileReference);
            } catch (IOException e) {
                throw new ComponentException(StringUtils.format("Failed to find tool output file: %s", getToolOutput()));
            }
        }
        try {

            String cpacsInitial = inputNamesToLocalFile.get(getCpacsInitialEndpointName());
            performOutputMapping(cpacsInitial);
            dynamicEndpointMapper.updateOutputsFromXML(new File(getCpacsResult()), componentContext);
            File resultFile = new File(getCpacsResult());
            FileReferenceTD outgoingCPACSFileReference =
                datamanagementService.createFileReferenceTDFromLocalFile(componentContext, resultFile,
                    componentContext.getConfigurationValue(CpacsToolIntegrationConstants.KEY_CPACS_RESULT_FILENAME));
            componentContext.writeOutput(getCpacsOutputName(), outgoingCPACSFileReference);
            if (getHistoryDataItem() != null) {
                getHistoryDataItem().addOutput(getCpacsOutputName(), outgoingCPACSFileReference);
            }
        } catch (DataTypeException e) {
            throw new ComponentException("Failed to extract dynamic output values from CPACS", e);
        } catch (IOException e) {
            throw new ComponentException("Failed to create result CPACS file", e);
        } catch (NullPointerException e) {
            throw new ComponentException(StringUtils.format(
                "Failed to write output to CPACS output '%s'.", getCpacsOutputName()), e);
        }

    }

    private void writeNotAValueToDynamicOutputs(Set<String> outputsWithNotAValue) {
        TypedDatumFactory typedDatumFactory = componentContext.getService(TypedDatumService.class).getFactory();
        for (String outputName : componentContext.getOutputs()) {
            if (componentContext.isDynamicOutput(outputName) && !outputsWithNotAValue.contains(outputName)) {
                componentContext.writeOutput(outputName, typedDatumFactory.createNotAValue(Cause.InvalidInputs));
            }
        }
    }

    private void performOutputMapping(String cpacsInitial) throws ComponentException {
        final File mappingFile = new File(getOutputMapping());

        if (mappingFile.exists()) {
            if (mappingFile.getName().endsWith(CpacsToolIntegrationConstants.FILE_SUFFIX_XML)) {
                componentLog.componentInfo(StringUtils.format(STRING_MAPPING_USAGE,
                    STRING_MAPPING_TYPE_XML,
                    STRING_TOOL_MAPPING_DIRECTION_OUTPUT));
                try {
                    // Build CPACS-Result-File through mapping
                    final Document mappingDoc = transformXMLMapping(mappingFile.getAbsolutePath(),
                        getToolOutput(), cpacsInitial);

                    String cpacsResultFilePath = getCpacsResult();
                    File resultFile = new File(cpacsResultFilePath);
                    FileUtils.copyFile(new File(cpacsInitial), resultFile);

                    xmlMapper.transformXMLFileWithXMLMappingInformation(new File(getToolOutput()), resultFile, mappingDoc);

                    if (resultFile.exists()) {
                        componentLog.componentInfo(StringUtils.format(STRING_CPACS_RESULT_FILE_CREATED, cpacsResultFilePath));
                    }
                } catch (XPathExpressionException | IOException | XMLException e) {
                    throw new ComponentException(StringUtils.format(STRING_XML_ERROR_DURING_MAPPING,
                        STRING_TOOL_MAPPING_DIRECTION_OUTPUT), e);
                }

            } else if (mappingFile.getName().endsWith(CpacsToolIntegrationConstants.FILE_SUFFIX_XSL)) {
                componentLog.componentInfo(StringUtils.format(STRING_MAPPING_USAGE,
                    STRING_MAPPING_TYPE_XSL,
                    STRING_MAPPING_DIRECTION_OUTPUT));
                try {
                    xmlMapper.transformXMLFileWithXSLT(new File(cpacsInitial), new File(getCpacsResult()), mappingFile,
                        new CpacsToolIntegratorTextLinesReceiver());
                } catch (XMLException e) {
                    throw new ComponentException(StringUtils.format(STRING_XML_ERROR_DURING_MAPPING,
                        STRING_MAPPING_DIRECTION_OUTPUT), e);
                }
            } else {
                throw new ComponentException(StringUtils.format(
                    STRING_ERROR_SOLVING_FILE_EXTENSION, getOutputMapping()));
            }
        } else {
            throw new ComponentException(StringUtils.format(STRING_MAPPING_FILE_NOT_FOUND,
                getOutputMapping()));
        }
    }

    @Override
    protected void initializeNewHistoryDataItem() {
        if (Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            historyDataItem = new CpacsIntegrationHistoryDataItem(componentContext.getComponentIdentifier());
        }
    }

    private CpacsIntegrationHistoryDataItem getHistoryDataItem() {
        return (CpacsIntegrationHistoryDataItem) historyDataItem;
    }

    protected String getCpacsInitialEndpointName() {
        return componentContext.getConfigurationValue(CpacsToolIntegrationConstants.KEY_CPACS_INITIAL_ENDPOINTNAME);
    }

    protected String getToolInput() {
        return executionToolDirectory + File.separator + getToolInputFileName();
    }

    private String getToolInputFileName() {
        return componentContext.getConfigurationValue(CpacsToolIntegrationConstants.KEY_TOOL_INPUT_FILENAME);
    }

    protected String getInputMapping() {
        return executionToolDirectory + File.separator
            + componentContext.getConfigurationValue(CpacsToolIntegrationConstants.KEY_MAPPING_INPUT_FILENAME);
    }

    protected boolean hasToolspecificinputfile() {
        return componentContext.getConfigurationValue(CpacsToolIntegrationConstants.KEY_HAS_TOOLSPECIFIC_INPUT) != null
            && Boolean.parseBoolean(componentContext.getConfigurationValue(CpacsToolIntegrationConstants.KEY_HAS_TOOLSPECIFIC_INPUT));
    }

    protected boolean isAlwaysRun() {
        return componentContext.getConfigurationValue(CpacsToolIntegrationConstants.KEY_ALWAYS_RUN) == null
            || Boolean.parseBoolean(componentContext.getConfigurationValue(CpacsToolIntegrationConstants.KEY_ALWAYS_RUN));
    }

    protected String getToolspecificInputMapping() {
        return executionToolDirectory + File.separator
            + componentContext.getConfigurationValue(CpacsToolIntegrationConstants.KEY_TOOLSPECIFICMAPPING_FILENAME);
    }

    protected String getToolspecificInputData() {
        return executionToolDirectory + File.separator
            + componentContext.getConfigurationValue(CpacsToolIntegrationConstants.KEY_TOOLSPECIFICINPUTDATA_FILENAME);
    }

    protected String getCpacsResult() {
        return outputDirectory + File.separator
            + componentContext.getConfigurationValue(CpacsToolIntegrationConstants.KEY_CPACS_RESULT_FILENAME);
    }

    protected String getCpacsOutputName() {
        return componentContext.getConfigurationValue(CpacsToolIntegrationConstants.KEY_CPACS_OUTGOING_ENDPOINTNAME);
    }

    protected String getOutputMapping() {
        return executionToolDirectory + File.separator
            + componentContext.getConfigurationValue(CpacsToolIntegrationConstants.KEY_MAPPING_OUTPUT_FILENAME);
    }

    protected String getToolOutput() {
        if (!isMockMode()) {
            return executionToolDirectory + File.separator
                + componentContext.getConfigurationValue(CpacsToolIntegrationConstants.KEY_TOOL_OUTPUT_FILENAME);
        } else {
            return executionToolDirectory + File.separator
                + componentContext.getConfigurationValue(CpacsToolIntegrationConstants.KEY_MOCK_TOOL_OUTPUT_FILENAME);
        }
    }

    @Override
    protected void bindScriptingService(ScriptingService service) {
        super.bindScriptingService(service);
    }

    @Override
    protected void bindComponentDataManagementService(ComponentDataManagementService compDataManagementService) {
        super.bindComponentDataManagementService(compDataManagementService);
    }

    /**
     * --- MOVED HERE FROM OLD CPACSMAPPER ---
     * 
     * Transforms a XML mapping stylesheet to the final mapping XML document by surrounding it with a XSLT header and executing this XSLT
     * stylesheet. This method rearranges, e.g., xslt loops to simple source/target mappings.
     * 
     * @param mappingFilename The file name of the mapping file to be transformed.
     * @param sourceFilename The file name of an input (source) file.
     * @param targetFilename The file name of the target file, in which the source file should be imported. If content is empty ("") a new
     *        file will be created.
     * 
     * @return Returns the final mapping XML document as DOM document.
     * @throws ComponentException Thrown if mapping fails.
     */
    private Document transformXMLMapping(final String mappingFilename, final String sourceFilename, final String targetFilename)
        throws XMLException {
        try {
            final TransformerFactory transformerFac = TransformerFactory.newInstance();
            transformerFac.setErrorListener(new XSLTErrorHandler());

            // First read in the mapping XML file and transform it to a valid
            // XSLT stylesheet by surrounding it with the appropiate stylesheet elements.
            // This is done via the stylesheet CreateMapping.xslt which is loaded from
            // the jar file or the package path.
            try (InputStream inStream = this.getClass().getResourceAsStream(CREATE_MAPPING_XSLT_FILEPATH)) {
                final Transformer transformer1 = transformerFac.newTransformer(new StreamSource(inStream));
                transformer1.setErrorListener(new XSLTErrorHandler());
                final DOMSource mappingSrc = new DOMSource(xmlSupport.readXMLFromFile(new File(mappingFilename)));
                final Document tempDoc = xmlSupport.createDocument();
                final DOMResult tempXSLT = new DOMResult(tempDoc);
                transformer1.transform(mappingSrc, tempXSLT);

                // Now transform the resulting mapping XSLT to the final mapping file which
                // only contains mapping elements and no more xsl elements like loops, conditions
                // etc.
                final DOMSource sourceXSLT = new DOMSource(tempDoc);
                final Transformer transformer2 = transformerFac.newTransformer(sourceXSLT);
                transformer2.setErrorListener(new XSLTErrorHandler());

                transformer2.setParameter("sourceFilename", sourceFilename.replace("\\", XMLFILE_SEPARATOR));
                transformer2.setParameter("targetFilename", targetFilename.replace("\\", XMLFILE_SEPARATOR));

                final DOMSource source = new DOMSource(xmlSupport.createDocument());
                final Document resultDoc = xmlSupport.createDocument();
                final DOMResult result = new DOMResult(resultDoc);
                transformer2.transform(source, result);
                return resultDoc;
            }
        } catch (final NullPointerException | TransformerException | XMLException | IOException e) {
            throw new XMLException("XML-Transformation failed: " + e.toString());
        }
    }

    @Override
    public void tearDown(FinalComponentState state) {
        super.tearDown(state);
        try {
            if (lastRunToolinputFile != null) {
                TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(lastRunToolinputFile);
            }
        } catch (IOException e) {
            LOG.error("Failed to delete temp file: " + lastRunToolinputFile.getAbsolutePath(), e);
        }
        try {
            if (tmpOutputFile != null) {
                TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(tmpOutputFile);
            }
        } catch (IOException e) {
            LOG.error("Failed to delete temp file: " + tmpOutputFile.getAbsolutePath(), e);
        }
    }
}
