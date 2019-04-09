/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.cpacs.writer.execution;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.components.cpacs.writer.common.CpacsWriterComponentConstants;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ComponentLog;
import de.rcenvironment.core.component.model.api.LazyDisposal;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.component.xml.XmlComponentHistoryDataItem;
import de.rcenvironment.core.component.xml.api.EndpointXMLService;
import de.rcenvironment.core.datamodel.api.DataTypeException;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.utils.common.LogUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Implementing class for Destination functionality with file support.
 * 
 * @author Markus Kunde
 * @author Markus Litz
 * @author Arne Bachmann
 */
@LazyDisposal
public class CpacsWriterComponent extends DefaultComponent {

    private Log log = LogFactory.getLog(getClass());

    private ComponentContext componentContext;

    private ComponentLog componentLog;

    private DistributedNotificationService notificationService;

    private ComponentDataManagementService dataManagementService;

    private boolean overwriteEachRun = true;

    private XmlComponentHistoryDataItem historyDataItem = null;

    private EndpointXMLService endpointXmlService;

    private File localFolder;

    private File tempFile;

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
        componentLog = componentContext.getLog();
    }

    @Override
    public void start() throws ComponentException {
        dataManagementService = componentContext.getService(ComponentDataManagementService.class);
        endpointXmlService = componentContext.getService(EndpointXMLService.class);
        notificationService = componentContext.getService(DistributedNotificationService.class);

        notificationService.setBufferSize(componentContext.getExecutionIdentifier() + CpacsWriterComponentConstants.RUNTIME_CPACS_UUIDS, 1);

        overwriteEachRun = Boolean.parseBoolean(componentContext.getConfigurationValue(CpacsWriterComponentConstants.SAVE_MODE));

        // determine local storage, if specified, or null, if not
        final String localStore = componentContext.getConfigurationValue(CpacsWriterComponentConstants.LOCAL_STORE_FOLDER);
        if ((localStore != null) && (!localStore.trim().isEmpty())) {
            File folder = new File(localStore);
            if (folder.isAbsolute()) {
                localFolder = folder;
            } else {
                localFolder = null;
                throw new ComponentException("Failed to write CPACS file as the given path is invalid,"
                    + " it must be an absolute one: " + folder.getPath());
            }
        } else {
            localFolder = null;
            componentLog.componentInfo("Incoming CPACS file(s) not stored to local file system as no path is configured");
        }
    }

    @Override
    public void processInputs() throws ComponentException {
        initializeNewHistoryDataItem();

        FileReferenceTD cpacsFileRef = (FileReferenceTD) componentContext.readInput(CpacsWriterComponentConstants.INPUT_NAME_CPACS);

        Map<String, TypedDatum> varInputs = new HashMap<>();
        for (String inputName : componentContext.getInputsWithDatum()) {
            if (componentContext.isDynamicInput(inputName)) {
                varInputs.put(inputName, componentContext.readInput(inputName));
            }
        }

        if (!varInputs.isEmpty() || hasDynamicOutputs()) {
            try {
                tempFile =
                    TempFileServiceAccess.getInstance().createTempFileWithFixedFilename(CpacsWriterComponentConstants.CPACS_FILENAME);
                dataManagementService.copyReferenceToLocalFile(cpacsFileRef.getFileReference(), tempFile,
                    componentContext.getStorageNetworkDestination());
            } catch (IOException e) {
                logError("Failed to write CPACS file into a temporary file (that is required for CPACS Writer) - skip run", e);
                return;
            }

            boolean updated = false;
            try {
                endpointXmlService.updateXMLWithInputs(tempFile, varInputs, componentContext);
                updated = true;
            } catch (DataTypeException e) {
                logError("Failed to add dynamic input values to the CPACS file", e);
            }

            String fileName;
            if (updated) {
                fileName = getFileName("cpacs_with_variables");

                try {
                    cpacsFileRef = dataManagementService.createFileReferenceTDFromLocalFile(componentContext, tempFile, fileName);
                } catch (IOException e) {
                    logError("Failed to store the CPACS file with variables into the data management - "
                        + "if it is not stored in the data management, it can not be sent as output value", e);
                }

                if (!varInputs.isEmpty() && historyDataItem != null) {
                    historyDataItem.setXmlWithVariablesFileReference(cpacsFileRef.getFileReference());
                    storeHistoryDataItem();
                }
            } else {
                fileName = getFileName("cpacs");
            }

            // copy CPACS file to local folder if configured to do so
            if (localFolder != null) {
                try {
                    File file = new File(localFolder, fileName);
                    FileUtils.copyFile(tempFile, file);
                    componentLog.componentInfo("Stored CPACS file to: " + file.getAbsolutePath());
                } catch (IOException e) {
                    logError("Failed to store CPACS file to the local file system", e);
                }
            }
            if (hasDynamicOutputs()) {
                try {
                    endpointXmlService.updateOutputsFromXML(tempFile, componentContext);
                } catch (DataTypeException e) {
                    logError("Failed to read dynamic output values from the CPACS file", e);
                }
            }

        } else {
            String fileName = getFileName("cpacs");
            // write CPACS file to local folder if configured to do so
            if (localFolder != null) {
                try {
                    File file = new File(localFolder, fileName);
                    dataManagementService.copyReferenceToLocalFile(cpacsFileRef.getFileReference(), file,
                        componentContext.getStorageNetworkDestination());
                    componentLog.componentInfo("Stored CPACS file to: " + file.getAbsolutePath());
                } catch (IOException e) {
                    logError("Failed to store CPACS file to the local file system", e);
                }
            }
        }

        sendCPACSFile(cpacsFileRef);

        deleteTempFile();

    }

    private void logError(String message, Exception e) {
        String errorId = LogUtils.logExceptionWithStacktraceAndAssignUniqueMarker(log, message, e);
        componentLog.componentError(message, e, errorId);
    }

    private void sendCPACSFile(FileReferenceTD cpacsFileRef) {
        componentContext.writeOutput(CpacsWriterComponentConstants.OUTPUT_NAME_CPACS, cpacsFileRef);
        notificationService.send(componentContext.getExecutionIdentifier()
            + CpacsWriterComponentConstants.RUNTIME_CPACS_UUIDS, cpacsFileRef.getFileReference());
    }
    
    @Override
    public void completeStartOrProcessInputsAfterFailure() throws ComponentException {
        storeHistoryDataItem();
        deleteTempFile();
    }

    private void initializeNewHistoryDataItem() {
        if (Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            historyDataItem = new XmlComponentHistoryDataItem(CpacsWriterComponentConstants.COMPONENT_ID);
        }
    }

    private void storeHistoryDataItem() {
        if (Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            componentContext.writeFinalHistoryDataItem(historyDataItem);
        }
    }

    private String getFileName(String name) {
        String fileName;
        if (!overwriteEachRun) {
            fileName = StringUtils.format("%s_%d.xml", name, componentContext.getExecutionCount());
        } else {
            fileName = StringUtils.format("%s.xml", name);
        }

        return fileName;
    }

    private boolean hasDynamicOutputs() {
        for (String outputName : componentContext.getOutputs()) {
            if (componentContext.isDynamicOutput(outputName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void dispose() {
        if (componentContext != null && notificationService != null) {
            notificationService.removePublisher(componentContext.getExecutionIdentifier()
                + CpacsWriterComponentConstants.RUNTIME_CPACS_UUIDS);
        }
    }

    private void deleteTempFile() {
        if (tempFile != null) {
            try {
                TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(tempFile);
            } catch (IOException e) {
                LogFactory.getLog(getClass()).error("Failed to delete temp file: " + tempFile.getAbsolutePath(), e);
            }
        }
    }
}
