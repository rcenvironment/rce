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
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.integration.CommonToolIntegratorComponent;
import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.DataTypeException;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.scripting.ScriptingService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.cpacs.utils.common.components.CpacsChannelFilter;
import de.rcenvironment.cpacs.utils.common.xml.ComponentVariableMapper;

/**
 * Main class for the CPACS tool integration.
 * 
 * @author Jan Flink
 */
public class CpacsToolIntegratorComponent extends CommonToolIntegratorComponent {

    private static final Log LOG = LogFactory.getLog(CpacsToolIntegratorComponent.class);

    private static final String FILE_SUFFIX_MAPPED = "-mapped";

    private final CpacsMapper cpacsMapper;

    private final ComponentVariableMapper dynamicEndpointMapper;

    private String lastRunToolinputFile;

    private File tmpOutputFile;

    public CpacsToolIntegratorComponent() {
        super();
        cpacsMapper = new CpacsMapper(this);
        dynamicEndpointMapper = new ComponentVariableMapper();
    }
    
    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
    }

    @Override
    public void start() throws ComponentException {
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
                throw new ComponentException(String.format(
                    "%s: Error on reading input of incoming CPACS endpoint. An endpoint '%s' is not configured.",
                    toolName, getCpacsInitialEndpointName()));
            }
            boolean mappedWithVariables = dynamicEndpointMapper.updateXMLWithInputs(cpacsInitial, dynamicInputs, componentContext);
            if (!dynamicInputs.isEmpty() && getHistoryDataItem() != null && mappedWithVariables) {
                String cpacsWithVariablesFileReference =
                    datamanagementService.createTaggedReferenceFromLocalFile(componentContext,
                        new File(cpacsInitial), "cpacsWithVariables.xml");
                getHistoryDataItem().setCpacsWithVariablesFileReference(cpacsWithVariablesFileReference);
            }
            createIntermediateFolders();
            cpacsMapper.mapInput(cpacsInitial);
            if (hasToolspecificinputfile()) {
                if (getHistoryDataItem() != null) {
                    String toolInputFileReference =
                        datamanagementService.createTaggedReferenceFromLocalFile(componentContext,
                            new File(getToolInput()), getToolInputFileName() + FILE_SUFFIX_MAPPED);
                    getHistoryDataItem().setToolInputWithoutToolspecificFileReference(toolInputFileReference);
                }
                cpacsMapper.mergeToolspecificInput();
            }
            if (getHistoryDataItem() != null) {
                File toolInputFile = new File(getToolInput());
                String toolInputFileReference =
                    datamanagementService.createTaggedReferenceFromLocalFile(componentContext,
                        toolInputFile, toolInputFile.getName());
                getHistoryDataItem().setToolInputFile(toolInputFile.getName(), toolInputFileReference);
            }
        } catch (DataTypeException e) {
            throw new ComponentException(String.format("%s: Error on executing dynamic endpoint mapping on input site.", toolName), e);
        } catch (IOException e) {
            throw new ComponentException(String.format("%s: Error on creating mapped file. ", toolName), e);
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
                throw new ComponentException(String.format("%s: Error on generating intermediate tool input/output folders.", toolName), e);
            }
        }
    }

    @Override
    protected boolean needToRun(Map<String, TypedDatum> inputValues, Map<String, String> inputNamesToLocalFile)
        throws ComponentException {
        try {
            String tmpInputFile = FileUtils.readFileToString(new File(getToolInput()));
            // Check the alwaysRun configuration and check for changed input since the last run
            if (isAlwaysRun() || lastRunToolinputFile == null || lastRunStaticInputValues == null
                || (tmpInputFile.compareTo(lastRunToolinputFile) != 0)
                || staticInputsAreNotEqual(inputValues, inputNamesToLocalFile)) {
                lastRunToolinputFile = tmpInputFile;
                return true;
            }
        } catch (IOException e) {
            throw new ComponentException(String.format("%s: Error on reading tool input file. ", toolName), e);
        }
        LOG.debug(String.format("%s: Running current step not required due to non changing input.", toolName));
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
                throw new ComponentException(String.format("%s: Unable to find tool output file '%s'.", toolName, getToolOutput()), e);
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
            cpacsMapper.mapOutput(inputNamesToLocalFile.get(getCpacsInitialEndpointName()));
            dynamicEndpointMapper.updateOutputsFromCPACS(getCpacsResult(), componentContext);
            File resultFile = new File(getCpacsResult());
            FileReferenceTD outgoingCPACSFileReference =
                datamanagementService.createFileReferenceTDFromLocalFile(componentContext, resultFile,
                    componentContext.getConfigurationValue(CpacsToolIntegrationConstants.KEY_CPACS_RESULT_FILENAME));
            try {
                componentContext.writeOutput(componentContext.getConfigurationValue(CpacsToolIntegrationConstants
                    .KEY_CPACS_OUTGOING_ENDPOINTNAME), outgoingCPACSFileReference);
            } catch (NullPointerException e) {
                throw new ComponentException(String.format(
                    "%s: Error on writing output to outgoing CPACS endpoint. An endpoint '%s' is not configured.",
                    toolName, componentContext.getConfigurationValue(CpacsToolIntegrationConstants.KEY_CPACS_OUTGOING_ENDPOINTNAME)), e);
            }
            if (getHistoryDataItem() != null) {
                getHistoryDataItem().addOutput(componentContext.getConfigurationValue(CpacsToolIntegrationConstants
                    .KEY_CPACS_OUTGOING_ENDPOINTNAME), outgoingCPACSFileReference);
            }
        } catch (DataTypeException e) {
            throw new ComponentException(String.format("%s: Error on executing dynamic endpoint mapping on output site. ", toolName), e);
        } catch (IOException e) {
            throw new ComponentException(String.format("%s: Error on creating result file. ", toolName), e);
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
}
