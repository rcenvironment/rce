/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.cpacs.component.integration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

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
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.integration.CommonToolIntegratorComponent;
import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.component.xml.api.EndpointXMLService;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.DataTypeException;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.scripting.ScriptingService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.xml.XSLTErrorHandler;
import de.rcenvironment.core.utils.incubator.xml.XMLException;
import de.rcenvironment.core.utils.incubator.xml.api.XMLMapperConstants;
import de.rcenvironment.core.utils.incubator.xml.api.XMLMapperService;
import de.rcenvironment.core.utils.incubator.xml.api.XMLSupportService;
import de.rcenvironment.cpacs.utils.common.components.CpacsChannelFilter;

/**
 * Main class for the CPACS tool integration.
 * 
 * @author Jan Flink
 */
public class CpacsToolIntegratorComponent extends CommonToolIntegratorComponent {

    private static final Log LOG = LogFactory.getLog(CpacsToolIntegratorComponent.class);

    private static final String FILE_SUFFIX_MAPPED = "-mapped";

    private static final String STRING_CPACS_RESULT_FILE_CREATED = "CPACS result file created (%s)): %s";

    private static final String STRING_TOOL_OUTPUT_FILE_EXISTS = "Tool output file exists: %s";

    private static final String SUFFIX_MAPPED = "-mapped";

    private static final String STRING_TOOL_INPUT_FILE_NOT_FOUND = "Tool input file '%s' not found.";

    private static final String STRING_XML_ERROR_DURING_MAPPING = "XML error during %s mapping.";

    private static final String STRING_TOOL_INPUT_CREATED = "Tool input file created (%s)): %s";

    private static final String STRING_MAPPING_USAGE = "%s: Use %s %s mapping";

    private static final String STRING_MAPPING_TYPE_XML = "pairing";

    private static final String STRING_MAPPING_TYPE_XSL = "raw XSLT";

    private static final String STRING_MAPPING_DIRECTION_INPUT = "input";

    private static final String STRING_MAPPING_DIRECTION_OUTPUT = "output";

    private static final String STRING_MAPPING_DIRECTION_TOOLSPECIFIC = "tool sepecific input";

    private static final String STRING_MAPPING_FILE_NOT_FOUND = "Mapping file '%s' not found.";

    private static final String STRING_ERROR_SOLVING_FILE_EXTENSION = "Error solving file extension of mapping file '%s'.";

    private static final String CREATE_MAPPING_XSLT_FILEPATH = "/resources/CreateMapping.xslt";

    private static final String XMLFILE_SEPARATOR = "/";

    private XMLMapperService xmlMapper;

    private XMLSupportService xmlSupport;

    private EndpointXMLService dynamicEndpointMapper;

    private String lastRunToolinputFile;

    private File tmpOutputFile;

    public CpacsToolIntegratorComponent() {
        super();
    }

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
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
        Map<String, TypedDatum> dynamicInputs =
            CpacsChannelFilter.getVariableInputs(inputValues, getCpacsInitialEndpointName(), componentContext);
        try {
            String cpacsInitial = inputNamesToLocalFile.get(getCpacsInitialEndpointName());
            if (cpacsInitial == null) {
                throw new ComponentException(StringUtils.format(
                    "%s: Error on reading input of incoming CPACS endpoint. An endpoint '%s' is not configured.",
                    toolName, getCpacsInitialEndpointName()));
            }
            performInputMappingWithGlobalMappingLock(cpacsInitial, dynamicInputs);
            if (getHistoryDataItem() != null) {
                File toolInputFile = new File(getToolInput());
                String toolInputFileReference =
                    datamanagementService.createTaggedReferenceFromLocalFile(componentContext,
                        toolInputFile, toolInputFile.getName());
                getHistoryDataItem().setToolInputFile(toolInputFile.getName(), toolInputFileReference);
            }
        } catch (IOException e) {
            throw new ComponentException(StringUtils.format("%s: Error on creating mapped file. ", toolName), e);
        }
    }
    
    private void performInputMappingWithGlobalMappingLock(String cpacsInitial, Map<String, TypedDatum> dynamicInputs)
        throws ComponentException, IOException {
        synchronized (XMLMapperConstants.GLOBAL_MAPPING_LOCK) {
            performInputMapping(cpacsInitial, dynamicInputs);
        }
    }

    private void performInputMapping(String cpacsInitial, Map<String, TypedDatum> dynamicInputs) throws ComponentException, IOException {
        try {
            dynamicEndpointMapper.updateXMLWithInputs(new File(cpacsInitial), dynamicInputs, componentContext);
        } catch (DataTypeException e) {
            throw new ComponentException(StringUtils.format("%s: Error on executing dynamic endpoint mapping on input site.", toolName), e);
        }
        if (!dynamicInputs.isEmpty() && getHistoryDataItem() != null) {
            String cpacsWithVariablesFileReference =
                datamanagementService.createTaggedReferenceFromLocalFile(componentContext,
                    new File(cpacsInitial), "cpacsWithVariables.xml");
            getHistoryDataItem().setCpacsWithVariablesFileReference(cpacsWithVariablesFileReference);
        }
        createIntermediateFolders();
        final File mappingFile = new File(getInputMapping());

        if (mappingFile.exists()) {
            if (mappingFile.getName().endsWith(CpacsToolIntegrationConstants.FILE_SUFFIX_XML)) {
                LOG.debug(StringUtils.format(STRING_MAPPING_USAGE, getToolName(),
                    STRING_MAPPING_TYPE_XML,
                    STRING_MAPPING_DIRECTION_INPUT));
                try {
                    Document mappingDoc =
                        transformXMLMapping(mappingFile.getAbsolutePath(), cpacsInitial, org.apache.commons.lang3.StringUtils.EMPTY);
                    // Build tool input document
                    final Document myToolDoc = xmlSupport.createDocument();
                    Document cpacsIncoming = xmlSupport.readXMLFromFile(new File(cpacsInitial));
                    xmlMapper.transformXMLFileWithXMLMappingInformation(cpacsIncoming, myToolDoc, mappingDoc);
                    String toolInputFilePath = getToolInput();
                    xmlSupport.writeXMLtoFile(myToolDoc, new File(toolInputFilePath));
                    LOG.debug(StringUtils.format(STRING_TOOL_INPUT_CREATED,
                        toolInputFilePath, String.valueOf(new File(toolInputFilePath).exists())));
                } catch (XPathExpressionException | XMLException e) {
                    throw new ComponentException(StringUtils.format(STRING_XML_ERROR_DURING_MAPPING,
                        STRING_MAPPING_DIRECTION_INPUT), e);
                }
            } else if (mappingFile.getName().endsWith(CpacsToolIntegrationConstants.FILE_SUFFIX_XSL)) {
                LOG.debug(StringUtils.format(STRING_MAPPING_USAGE, getToolName(),
                    STRING_MAPPING_TYPE_XSL,
                    STRING_MAPPING_DIRECTION_INPUT));
                try {
                    xmlMapper.transformXMLFileWithXSLT(new File(cpacsInitial), new File(getToolInput()), mappingFile);
                } catch (XMLException e) {
                    throw new ComponentException(StringUtils.format(STRING_XML_ERROR_DURING_MAPPING,
                        STRING_MAPPING_DIRECTION_INPUT), e);
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
                String toolInputFileReference =
                    datamanagementService.createTaggedReferenceFromLocalFile(componentContext,
                        new File(getToolInput()), getToolInputFileName() + FILE_SUFFIX_MAPPED);
                getHistoryDataItem().setToolInputWithoutToolspecificFileReference(toolInputFileReference);
            }
            final File mappingFile1 = new File(getToolspecificInputMapping());

            if (mappingFile1.exists()) {
                if (mappingFile1.getName().endsWith(CpacsToolIntegrationConstants.FILE_SUFFIX_XML)) {
                    LOG.debug(StringUtils.format(STRING_MAPPING_USAGE, getToolName(),
                        STRING_MAPPING_TYPE_XML,
                        STRING_MAPPING_DIRECTION_TOOLSPECIFIC));

                    Document mappedDoc;
                    try {
                        mappedDoc = xmlSupport.readXMLFromFile(new File(getToolInput()));
                        final Document toolDoc = xmlSupport.readXMLFromFile(new File(getToolspecificInputData()));
                        final Document mappingDoc = transformXMLMapping(getToolspecificInputMapping(),
                            getToolspecificInputData(), getToolInput());

                        xmlMapper.transformXMLFileWithXMLMappingInformation(toolDoc, mappedDoc, mappingDoc);

                        // overwrite old tool input file
                        xmlSupport.writeXMLtoFile(mappedDoc, new File(getToolInput()));
                    } catch (XPathExpressionException | XMLException e) {
                        throw new ComponentException(StringUtils.format(
                            STRING_XML_ERROR_DURING_MAPPING, STRING_MAPPING_DIRECTION_TOOLSPECIFIC), e);
                    }
                } else if (mappingFile1.getName().endsWith(CpacsToolIntegrationConstants.FILE_SUFFIX_XSL)) {
                    LOG.debug(StringUtils.format(STRING_MAPPING_USAGE, getToolName(),
                        STRING_MAPPING_TYPE_XSL,
                        STRING_MAPPING_DIRECTION_TOOLSPECIFIC));
                    File toolInputMapped = new File(getToolInput() + SUFFIX_MAPPED);
                    File toolInput = new File(getToolInput());
                    try {
                        FileUtils.copyFile(toolInput, toolInputMapped, true);
                        xmlMapper.transformXMLFileWithXSLT(new File(getToolInput() + SUFFIX_MAPPED), new File(getToolInput()),
                            new File(getToolspecificInputMapping()));
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
                throw new ComponentException(
                    StringUtils.format("%s: Error on generating intermediate tool input/output folders.", toolName), e);
            }
        }
    }

    @Override
    protected boolean needToRun(Map<String, TypedDatum> inputValues, Map<String, String> inputNamesToLocalFile)
        throws ComponentException {
        if (isAlwaysRun()){
            return true;
        }
        try {
            String tmpInputFile = FileUtils.readFileToString(new File(getToolInput()));
            // Check the alwaysRun configuration and check for changed input since the last run
            if (lastRunToolinputFile == null || lastRunStaticInputValues == null
                || (tmpInputFile.compareTo(lastRunToolinputFile) != 0)
                || staticInputsAreNotEqual(inputValues, inputNamesToLocalFile)) {
                lastRunToolinputFile = tmpInputFile;
                return true;
            }
        } catch (IOException e) {
            throw new ComponentException(StringUtils.format("%s: Error on reading tool input file. ", toolName), e);
        }
        LOG.debug(StringUtils.format("%s: Running current step not required due to non changing input.", toolName));
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
                        } catch (IOException e1) {
                            LOG.error(e1);
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
                        } catch (IOException e1) {
                            LOG.error(e1);
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
                    LOG.error(e);
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
    protected void afterCommandExecution(Map<String, TypedDatum> inputValues, Map<String, String> inputNamesToLocalFile)
        throws ComponentException {
        if (getHistoryDataItem() != null) {
            String toolOutputFileReference;
            try {
                File outputFile = new File(getToolOutput());
                toolOutputFileReference = datamanagementService.createTaggedReferenceFromLocalFile(componentContext,
                    outputFile, outputFile.getName());
                getHistoryDataItem().setToolOutputFile(outputFile.getName(), toolOutputFileReference);
            } catch (IOException e) {
                throw new ComponentException(StringUtils.format("%s: Unable to find tool output file '%s'.", toolName, getToolOutput()), e);
            }
        }
    }

    @Override
    protected void afterPostScriptExecution(Map<String, TypedDatum> inputValues, Map<String, String> inputNamesToLocalFile)
        throws ComponentException {
        try {
            if (!isAlwaysRun()) {
                if (needsToRun) {
                    tmpOutputFile = TempFileServiceAccess.getInstance().createTempFileFromPattern("cpacsToolOutput-*.xml");
                    FileUtils.copyFile(new File(getToolOutput()), tmpOutputFile);
                } else {
                    FileUtils.copyFile(tmpOutputFile, new File(getToolOutput()));
                }
            }
            String cpacsInitial = inputNamesToLocalFile.get(getCpacsInitialEndpointName());
            if (new File(getToolOutput()).exists()) {
                LOG.debug(StringUtils.format(STRING_TOOL_OUTPUT_FILE_EXISTS, new File(getToolOutput()).exists()));
                performOutputMappingWithGlobalMappingLock(cpacsInitial);
            }
            dynamicEndpointMapper.updateOutputsFromXML(new File(getCpacsResult()), componentContext);
            File resultFile = new File(getCpacsResult());
            FileReferenceTD outgoingCPACSFileReference =
                datamanagementService.createFileReferenceTDFromLocalFile(componentContext, resultFile,
                    componentContext.getConfigurationValue(CpacsToolIntegrationConstants.KEY_CPACS_RESULT_FILENAME));
            try {
                componentContext.writeOutput(componentContext.getConfigurationValue(CpacsToolIntegrationConstants
                    .KEY_CPACS_OUTGOING_ENDPOINTNAME), outgoingCPACSFileReference);
            } catch (NullPointerException e) {
                throw new ComponentException(StringUtils.format(
                    "%s: Error on writing output to outgoing CPACS endpoint. An endpoint '%s' is not configured.",
                    toolName, componentContext.getConfigurationValue(CpacsToolIntegrationConstants.KEY_CPACS_OUTGOING_ENDPOINTNAME)), e);
            }
            if (getHistoryDataItem() != null) {
                getHistoryDataItem().addOutput(componentContext.getConfigurationValue(CpacsToolIntegrationConstants
                    .KEY_CPACS_OUTGOING_ENDPOINTNAME), outgoingCPACSFileReference);
            }
        } catch (DataTypeException e) {
            throw new ComponentException(StringUtils.format("%s: Error on executing dynamic endpoint mapping on output site. ", toolName),
                e);
        } catch (IOException e) {
            throw new ComponentException(StringUtils.format("%s: Error on creating result file. ", toolName), e);
        }

    }
    
    private void performOutputMappingWithGlobalMappingLock(String cpacsInitial) throws ComponentException {
        synchronized (XMLMapperConstants.GLOBAL_MAPPING_LOCK) {
            performOutputMapping(cpacsInitial);
        }
    }
    
    private void performOutputMapping(String cpacsInitial) throws ComponentException {
        final File mappingFile = new File(getOutputMapping());

        if (mappingFile.exists()) {
            if (mappingFile.getName().endsWith(CpacsToolIntegrationConstants.FILE_SUFFIX_XML)) {
                LOG.debug(StringUtils.format(STRING_MAPPING_USAGE, getToolName(),
                    STRING_MAPPING_TYPE_XML,
                    STRING_MAPPING_DIRECTION_OUTPUT));
                try {
                    Document toolDoc;
                    toolDoc = xmlSupport.readXMLFromFile(new File(getToolOutput()));
                    // Build CPACS-Result-File through mapping
                    final Document mappingDoc = transformXMLMapping(mappingFile.getAbsolutePath(),
                        getToolOutput(), cpacsInitial);

                    Document cpacsInOut = xmlSupport.readXMLFromFile(new File(cpacsInitial));

                    xmlMapper.transformXMLFileWithXMLMappingInformation(toolDoc, cpacsInOut, mappingDoc);

                    // Update CPACS-File
                    String cpacsResultFilePath = getCpacsResult();
                    xmlSupport.writeXMLtoFile(cpacsInOut, new File(cpacsResultFilePath));
                    LOG.debug(StringUtils.format(STRING_CPACS_RESULT_FILE_CREATED,
                        cpacsResultFilePath, String.valueOf(new File(cpacsResultFilePath).exists())));
                } catch (XPathExpressionException | XMLException e2) {
                    throw new ComponentException(StringUtils.format(STRING_XML_ERROR_DURING_MAPPING,
                        STRING_MAPPING_DIRECTION_OUTPUT), e2);
                }

            } else if (mappingFile.getName().endsWith(CpacsToolIntegrationConstants.FILE_SUFFIX_XSL)) {
                LOG.debug(StringUtils.format(STRING_MAPPING_USAGE, getToolName(),
                    STRING_MAPPING_TYPE_XSL,
                    STRING_MAPPING_DIRECTION_OUTPUT));
                try {
                    xmlMapper.transformXMLFileWithXSLT(new File(cpacsInitial), new File(getCpacsResult()), mappingFile);
                } catch (XMLException e1) {
                    throw new ComponentException(StringUtils.format(STRING_XML_ERROR_DURING_MAPPING,
                        STRING_MAPPING_DIRECTION_OUTPUT), e1);
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
        return executionToolDirectory + File.separator + componentContext.getConfigurationValue(CpacsToolIntegrationConstants
            .KEY_MAPPING_INPUT_FILENAME);
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
        return outputDirectory + File.separator + componentContext.getConfigurationValue(CpacsToolIntegrationConstants
            .KEY_CPACS_RESULT_FILENAME);
    }

    protected String getOutputMapping() {
        return executionToolDirectory + File.separator + componentContext.getConfigurationValue(CpacsToolIntegrationConstants
            .KEY_MAPPING_OUTPUT_FILENAME);
    }

    protected String getToolOutput() {
        return executionToolDirectory + File.separator + componentContext.getConfigurationValue(CpacsToolIntegrationConstants
            .KEY_TOOL_OUTPUT_FILENAME);
    }

    protected String getToolName() {
        return toolName;
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
            final InputStream inStream = this.getClass().getResourceAsStream(CREATE_MAPPING_XSLT_FILEPATH);
            final Transformer transformer1 = transformerFac.newTransformer(new StreamSource(inStream));
            transformer1.setErrorListener(new XSLTErrorHandler());
            final DOMSource mappingSrc = new DOMSource(xmlSupport.readXMLFromFile(new File(mappingFilename)));
            final Document tempDoc = xmlSupport.createDocument();
            final DOMResult tempXSLT = new DOMResult(tempDoc);
            transformer1.transform(mappingSrc, tempXSLT);
            // Now transform the resulting mapping XSLT to the final mapping file which
            // only contains mapping elements and no more xsl elements like loops, conditions etc.
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
        } catch (final NullPointerException | TransformerException | XMLException e) {
            throw new XMLException("XML-Transformation failed: " + e.toString());
        }
    }
}
