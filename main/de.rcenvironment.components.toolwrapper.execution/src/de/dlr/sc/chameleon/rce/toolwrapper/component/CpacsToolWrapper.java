/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.dlr.sc.chameleon.rce.toolwrapper.component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.dlr.sc.chameleon.rce.toolwrapper.common.CpacsComponentConstants;
import de.dlr.sc.chameleon.rce.toolwrapper.common.CpacsWrapperInfo;
import de.dlr.sc.chameleon.rce.toolwrapper.common.ToolWrapperRunningState;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.datamodel.api.DataTypeException;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.utils.incubator.ZipFolderUtil;
import de.rcenvironment.cpacs.utils.common.components.ChameleonCommonConstants;
import de.rcenvironment.cpacs.utils.common.components.ConsoleLogger;
import de.rcenvironment.cpacs.utils.common.components.ToolWrapperComponentHistoryDataItem;
import de.rcenvironment.cpacs.utils.common.xml.ComponentVariableMapper;

/**
 * Wrapper of CPACS and Tool behavior.
 * 
 * @author Markus Kunde
 * @author Markus Litz
 * @author Jan Flink
 */

@Deprecated
public class CpacsToolWrapper {

    private static final int INITIALCRC = -12345;

    private static final String XML_EMPTY = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    /** Our logger instance. */
    private static final Log LOGGER = LogFactory.getLog(CpacsToolWrapper.class);

    private final CpacsTool tool;

    private final ToolWrapper component;

    private ToolWrapperRunningState twrs;

    private final CpacsWrapperInfo toolConfiguration;

    private final ConsoleLogger consoleLoggerErr;

    private final ConsoleLogger consoleLoggerOut;

    /** Last run's crc for the incoming zip. */
    private long lastZipCrc = INITIALCRC;

    /** Current run's crc for the incoming zip. */
    private long newZipCrc = INITIALCRC;

    /**
     * The toolinput.xml as string. This String reflects the last configuration which was generated before the tool was started.
     */
    private String lastRunToolinputFile = null;

    private String directoryReference = null;

    /**
     * Constructor.
     * 
     * @param is input stream with cpacs wrapper info
     * @param ci component instance information of RCE component
     * @throws IOException if error occurs on access to inputstream
     */
    public CpacsToolWrapper(ToolWrapper comp, final InputStream is) throws IOException {
        toolConfiguration = new CpacsWrapperInfo(is);
        component = comp;
        consoleLoggerOut =
            new ConsoleLogger(component.getComponentContext(), ToolWrapper.notificationService, ConsoleRow.Type.STDOUT);
        consoleLoggerErr =
            new ConsoleLogger(component.getComponentContext(), ToolWrapper.notificationService, ConsoleRow.Type.STDERR);
        tool = new CpacsTool(toolConfiguration, consoleLoggerOut, consoleLoggerErr);
    }

    /**
     * Preparing (setUp) of tool in the beginning of the tool session.
     * 
     * @return true if successful. Otherwise false.
     * @throws IOException if error occurs on file access
     */
    public boolean prepare() throws IOException {
        boolean returnValue = false;
        try {
            tool.copyToTempDir();
            createDirectories();
            returnValue = true;
        } catch (IOException e) {
            LOGGER.error(e);
            String message = "Error preparing " + tool.getName() + ": " + e.getMessage();
            twrs = new ToolWrapperRunningState();
            twrs.setError(new Throwable(message));
            throw new IOException(message, e);
        }

        return returnValue;
    }

    /**
     * Creates necessary directories if not already there.
     * 
     * @throws IOException
     */
    private void createDirectories() throws IOException {
        final File inputDirName = new File(tool.toolCPACSBehaviorConfiguration.getInputDir());
        final File outputDirName = new File(tool.toolCPACSBehaviorConfiguration.getOutputDir());
        final File serverReturnDirName = new File(tool.toolCPACSBehaviorConfiguration.getReturnDirectory());
        final File serverIncomingDirName = new File(tool.toolCPACSBehaviorConfiguration.getIncomingDirectory());

        if (!inputDirName.exists()) {
            inputDirName.mkdirs();
        }

        if (!outputDirName.exists()) {
            outputDirName.mkdirs();
        }

        if (!serverReturnDirName.exists()) {
            serverReturnDirName.mkdirs();
        } else { // if exists, clear contents
            FileUtils.deleteDirectory(serverReturnDirName);
            serverReturnDirName.mkdirs();
        }

        if (!serverIncomingDirName.exists()) {
            serverIncomingDirName.mkdirs();
        } else { // if exists, clear contents
            FileUtils.deleteDirectory(serverIncomingDirName);
            serverIncomingDirName.mkdirs();
        }
    }

    /**
     * Clean up of tool at the end of the tool session.
     * 
     * @return true if successful. Otherwise false.
     */
    public boolean cleanUp() {
        boolean returnValue = false;

        if (toolConfiguration.isDeletetempdirectory()) {
            try {
                tool.deleteTempDir();
                returnValue = true;
            } catch (IOException e) {
                LOGGER.error(e);
            }
        } else {
            returnValue = true;
        }

        return returnValue;
    }

    private void registerLogFiles() throws IOException {
        FileUtils.touch(new File(tool.toolCPACSBehaviorConfiguration.getLogFileNameStdOut()));
        FileUtils.touch(new File(tool.toolCPACSBehaviorConfiguration.getLogFileNameStdErr()));
        consoleLoggerOut.registerAdditionalOutputStream(new FileOutputStream(new File(tool.toolCPACSBehaviorConfiguration
            .getLogFileNameStdOut()), true));
        consoleLoggerErr.registerAdditionalOutputStream(new FileOutputStream(new File(tool.toolCPACSBehaviorConfiguration
            .getLogFileNameStdErr()), true));
    }

    /**
     * Tool run of CPACS tool.
     * 
     * @param cpacs CPACS dataset
     * @param directory directory where tool should run
     * @param variableInputs input of variables
     * @return UUID of new CPACS dataset
     * @throws IOException thrown in i/o related things
     * @throws ComponentException thrown when component has an illegal state
     * @throws DataTypeException thrown when dynamic datatype channels act with CPACS and something went wrong
     */
    public String toolRun(final String cpacs, final String directory, final Map<String, TypedDatum> variableInputs)
        throws IOException, ComponentException, DataTypeException {
        String returnValue = null;
        final ComponentVariableMapper varMapper = new ComponentVariableMapper();
        twrs = new ToolWrapperRunningState();
        ToolWrapperComponentHistoryDataItem historyDataItem =
            new ToolWrapperComponentHistoryDataItem(component.getComponentContext().getComponentIdentifier());
        try {
            cleanUpBeforeCpacsComponentRun(); // Clean up tool temp dir before every run.
            // Register additional log/err file
            registerLogFiles();
            // Copy Cpacs from data management into temp working directory of tool
            ToolWrapper.dataManagementService.copyReferenceToLocalFile(cpacs,
                new File(tool.toolCPACSBehaviorConfiguration.getCpacsInitial()),
                component.getComponentContext().getDefaultStorageNodeId());
            twrs.setCpacsIn(cpacs); // History browser
            historyDataItem.setCpacsInFileReference(cpacs);
            // update Cpacs with input-variables.
            varMapper.updateXMLWithInputs(tool.toolCPACSBehaviorConfiguration.getCpacsInitial(), variableInputs,
                component.getComponentContext());
            twrs.setDynamicChannelsInEntry(varMapper.getInputValues());
            historyDataItem.setDynamicInputs(varMapper.getInputValues());
            String uuidCpacsVarIn =
                ToolWrapper.dataManagementService.createTaggedReferenceFromLocalFile(component.getComponentContext(),
                    new File(tool.toolCPACSBehaviorConfiguration.getCpacsInitial()), ChameleonCommonConstants.CHAMELEON_CPACS_FILENAME);
            twrs.setCpacsVariableIn(uuidCpacsVarIn);
            historyDataItem.setCpacsVariableInFileReference(uuidCpacsVarIn);
            if (directory != null) { // write incoming data directory (extract zip into incomingDir)
                File incomingZip =
                    new File(tool.toolCPACSBehaviorConfiguration.getIncomingDirectory(), CpacsComponentConstants.RETURN_ZIP_NAME);
                ToolWrapper.dataManagementService.copyReferenceToLocalFile(directory.toString(), incomingZip,
                    component.getComponentContext().getDefaultStorageNodeId());
                ZipFolderUtil.extractZipToFolder(new File(tool.toolCPACSBehaviorConfiguration.getIncomingDirectory()),
                    new File(incomingZip.getAbsolutePath()));
                newZipCrc = FileUtils.checksumCRC32(incomingZip);
                FileUtils.deleteQuietly(incomingZip);
            }
            try {
                for (File file : FileUtils.listFiles(new File(tool.toolCPACSBehaviorConfiguration.getIncomingDirectory()), null, true)) {
                    String uuidFile = ToolWrapper.dataManagementService.createTaggedReferenceFromLocalFile(component.getComponentContext(),
                        file, file.getName());
                    twrs.setIncomingDirectoryEntry(file.getName(), uuidFile);
                }
                historyDataItem.setIncomingDirectoryReference(twrs.getIncomingDirectory());
            } catch (IllegalArgumentException e) {
                LOGGER.debug("No incoming directory available.");
            }

            tool.mappingInput(); // input mapping (generate tool input file)
            String uuidToolIn =
                ToolWrapper.dataManagementService.createTaggedReferenceFromLocalFile(component.getComponentContext(),
                    new File(tool.toolCPACSBehaviorConfiguration.getToolInput()), CpacsComponentConstants.TOOL_INPUT_FILENAME);
            twrs.setToolIn(uuidToolIn);
            historyDataItem.setToolInFileReference(uuidToolIn);
            final String errorMessage = String.format("Executing tool '%s' (v. %s) failed", toolConfiguration.getToolName(),
                toolConfiguration.getToolVersion());
            try {
                String tmpToolInputFile = FileUtils.readFileToString(new File(tool.toolCPACSBehaviorConfiguration.getToolInput()));

                if (toolConfiguration.isAlwaysrun() || lastRunToolinputFile == null
                    || (tmpToolInputFile.compareTo(lastRunToolinputFile) != 0) || newZipCrc != lastZipCrc) {
                    lastZipCrc = newZipCrc;
                    lastRunToolinputFile = tmpToolInputFile;
                    cleanUpBeforeWrappedToolRun();
                    tool.executePreCommand(); // execute tool
                    tool.executeMainCommand();
                    tool.executePostCommand();
                }
                tool.mappingOutput(); // output mapping (generate outgoing Cpacs)
            } catch (RuntimeException | IOException | ComponentException e) {
                LOGGER.error(errorMessage, e);
                throw new ComponentException(errorMessage, e);
            } finally {
                // read outgoing data directory (create zip and write to outgoing ZIP-Channel).
                if (component.getComponentContext().getOutputs().contains(ChameleonCommonConstants.DIRECTORY_CHANNELNAME)
                    && new File(tool.toolCPACSBehaviorConfiguration.getReturnDirectory()).exists()) {
                    File outputDirZipped = new File(tool.toolCPACSBehaviorConfiguration.getReturnZipName());
                    ZipFolderUtil.zipFolderContent(new File(tool.toolCPACSBehaviorConfiguration.getReturnDirectory()), outputDirZipped);
                    String uuidDirectory = ToolWrapper.dataManagementService.createTaggedReferenceFromLocalFile(component
                        .getComponentContext(), outputDirZipped, CpacsComponentConstants.RETURN_ZIP_NAME);
                    directoryReference = uuidDirectory;
                }
                try {
                    for (File file : FileUtils.listFiles(new File(tool.toolCPACSBehaviorConfiguration.getReturnDirectory()), null, true)) {
                        String uuidFile = ToolWrapper.dataManagementService.createTaggedReferenceFromLocalFile(component
                            .getComponentContext(), file, file.getName());
                        twrs.setOutgoingDirectoryEntry(file.getName(), uuidFile);
                    }
                    historyDataItem.setOutgoingDirectoryReference(twrs.getOutgoingDirectory());
                } catch (IllegalArgumentException e) {
                    LOGGER.debug("No return directory available.");
                }

                // Test if cpacs is corrupted.
                String cpacsResult = FileUtils.readFileToString(new File(tool.toolCPACSBehaviorConfiguration.getCpacsResult()));
                if (cpacsResult == null || cpacsResult.isEmpty() || cpacsResult.equalsIgnoreCase(XML_EMPTY)) {
                    throw new ComponentException("Outgoing CPACS is empty. An error may have occured during server-side execution.");
                }
            }
            // Copy out and err logs into data management
            String uuidLogOut;
            String uuidLogErr;
            try {
                uuidLogOut =
                    ToolWrapper.dataManagementService.createTaggedReferenceFromLocalFile(component.getComponentContext(),
                        new File(tool.toolCPACSBehaviorConfiguration.getLogFileNameStdOut()), CpacsComponentConstants.LOGFILE_NAME_STDOUT);
                uuidLogErr =
                    ToolWrapper.dataManagementService.createTaggedReferenceFromLocalFile(component.getComponentContext(),
                        new File(tool.toolCPACSBehaviorConfiguration.getLogFileNameStdErr()), CpacsComponentConstants.LOGFILE_NAME_STDERR);
                // History browser
                twrs.setToolLogOutIn(uuidLogOut);
                twrs.setToolLogErrIn(uuidLogErr);
                historyDataItem.addLog("Tool Out-Log", uuidLogOut);
                historyDataItem.addLog("Tool Err-Log", uuidLogErr);
            } catch (RuntimeException e) {
                LOGGER.debug("No Std out or error written to data management.");
            }
            consoleLoggerOut.closeAndReleaseStreams();
            consoleLoggerErr.closeAndReleaseStreams();
            String uuidToolOut =
                ToolWrapper.dataManagementService.createTaggedReferenceFromLocalFile(component.getComponentContext(),
                    new File(tool.toolCPACSBehaviorConfiguration.getToolOutput()), CpacsComponentConstants.TOOL_OUTPUT_FILENAME);
            twrs.setToolOut(uuidToolOut);
            historyDataItem.setToolOutFileReference(uuidToolOut);
            // read output-variables from Cpacs.
            varMapper.updateOutputsFromXML(tool.toolCPACSBehaviorConfiguration.getCpacsResult(),
                ChameleonCommonConstants.CHAMELEON_CPACS_NAME, component.getComponentContext());
            twrs.setDynamicChannelsOutEntry(varMapper.getOutputValues());
            historyDataItem.setDynamicOutputs(varMapper.getOutputValues());
            // Copy Cpacs into data management from temp working directory.
            String uuid =
                ToolWrapper.dataManagementService.createTaggedReferenceFromLocalFile(component.getComponentContext(),
                    new File(tool.toolCPACSBehaviorConfiguration.getCpacsResult()), ChameleonCommonConstants.CHAMELEON_CPACS_FILENAME);
            returnValue = uuid;
            twrs.setCpacsOut(returnValue);
            historyDataItem.setCpacsOutFileReference(returnValue);
        } catch (IOException e) {
            twrs.setError(e);
            collectingRecoveryInformationOnFailure();
            throw e;
        } catch (DataTypeException e1) {
            twrs.setError(e1);
            collectingRecoveryInformationOnFailure();
            throw e1;
        } finally {
            // component.dataManagementService.addHistoryDataPoint(twrs, component.getComponentInstanceInformation().getName());
            component.getComponentContext().writeFinalHistoryDataItem(historyDataItem);
        }
        
        return returnValue;
    }

    private void collectingRecoveryInformationOnFailure() throws IOException {
        // Trying to get explicit output information
        // Write explicit toolOut and returnDirectory
        if (twrs.getToolOut() == null && new File(tool.toolCPACSBehaviorConfiguration.getToolOutput()).exists()) {
            String uuidToolOut = ToolWrapper.dataManagementService.createTaggedReferenceFromLocalFile(component.getComponentContext(),
                    new File(tool.toolCPACSBehaviorConfiguration.getToolOutput()), CpacsComponentConstants.TOOL_OUTPUT_FILENAME);
            twrs.setToolOut(uuidToolOut);
        }

        if (twrs.getOutgoingDirectory() == null && new File(tool.toolCPACSBehaviorConfiguration.getReturnDirectory()).exists()) {
            try {
                for (File file : FileUtils.listFiles(new File(tool.toolCPACSBehaviorConfiguration.getReturnDirectory()), null, true)) {
                    String uuidFile = ToolWrapper.dataManagementService.createTaggedReferenceFromLocalFile(component
                        .getComponentContext(), file, file.getName());
                    twrs.setOutgoingDirectoryEntry(file.getName(), uuidFile);
                }
            } catch (IllegalArgumentException iae) {
                LOGGER.debug("No return directory available.");
            }
        }

        if (twrs.getToolLogOut() == null && new File(tool.toolCPACSBehaviorConfiguration.getLogFileNameStdOut()).exists()) {
            String uuidLogOut;
            try {
                uuidLogOut = ToolWrapper.dataManagementService.createTaggedReferenceFromLocalFile(component.getComponentContext(),
                        new File(tool.toolCPACSBehaviorConfiguration.getLogFileNameStdOut()),
                        CpacsComponentConstants.LOGFILE_NAME_STDOUT);

                // History browser
                twrs.setToolLogOutIn(uuidLogOut);
            } catch (RuntimeException re) {
                LOGGER.debug("No Std out written to data management.");
            }
        }

        if (twrs.getToolLogErr() == null && new File(tool.toolCPACSBehaviorConfiguration.getLogFileNameStdErr()).exists()) {
            String uuidLogErr;
            try {
                uuidLogErr = ToolWrapper.dataManagementService.createTaggedReferenceFromLocalFile(component.getComponentContext(),
                        new File(tool.toolCPACSBehaviorConfiguration.getLogFileNameStdErr()),
                        CpacsComponentConstants.LOGFILE_NAME_STDERR);

                // History browser
                twrs.setToolLogErrIn(uuidLogErr);
            } catch (RuntimeException re) {
                LOGGER.debug("No Std err written to data management.");
            }
        }
    }

    /**
     * Returns reference of zipped directory file or null if non-existing.
     * 
     * @return reference of zipped directory file
     */
    public String getDirectoryUUID() {
        String tmpUUID = directoryReference;
        directoryReference = null;
        return tmpUUID;
    }

    private boolean cleanUpBeforeWrappedToolRun() {
        FileUtils.deleteQuietly(new File(tool.toolCPACSBehaviorConfiguration.getToolOutput()));
        try {
            FileUtils.cleanDirectory(new File(tool.toolCPACSBehaviorConfiguration.getReturnDirectory()));
        } catch (IOException e) {
            LOGGER.debug("cleaning up before wrapped tool run fails.");
        } catch (IllegalArgumentException e) {
            LOGGER.debug("cleaning up before wrapped tool run fails.");
        }

        return true;
    }

    private boolean cleanUpBeforeCpacsComponentRun() {

        FileUtils.deleteQuietly(new File(tool.toolCPACSBehaviorConfiguration.getCpacsInitial()));
        FileUtils.deleteQuietly(new File(tool.toolCPACSBehaviorConfiguration.getCpacsResult()));
        FileUtils.deleteQuietly(new File(tool.toolCPACSBehaviorConfiguration.getToolInput()));
        try {
            FileUtils.cleanDirectory(new File(tool.toolCPACSBehaviorConfiguration.getIncomingDirectory()));
        } catch (IOException e) {
            LOGGER.debug("cleaning up before cpacs component run fails.");
        } catch (IllegalArgumentException e) {
            LOGGER.debug("cleaning up before cpacs component run fails.");
        }

        FileUtils.deleteQuietly(new File(tool.toolCPACSBehaviorConfiguration.getReturnZipName()));
        FileUtils.deleteQuietly(new File(tool.toolCPACSBehaviorConfiguration.getLogFileNameStdOut()));
        FileUtils.deleteQuietly(new File(tool.toolCPACSBehaviorConfiguration.getLogFileNameStdErr()));

        return true;
    }
}
