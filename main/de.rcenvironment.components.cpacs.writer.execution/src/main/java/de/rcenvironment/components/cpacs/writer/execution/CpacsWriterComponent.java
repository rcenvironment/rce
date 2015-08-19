/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.cpacs.writer.execution;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.components.cpacs.writer.common.CpacsWriterComponentConstants;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.model.api.LazyDisposal;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.component.xml.XmlComponentHistoryDataItem;
import de.rcenvironment.core.component.xml.api.EndpointXMLService;
import de.rcenvironment.core.datamodel.api.DataTypeException;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.cpacs.utils.common.components.ChameleonCommonConstants;
import de.rcenvironment.cpacs.utils.common.components.CpacsChannelFilter;

/**
 * Implementing class for Destination functionality with file support.
 * 
 * @author Markus Kunde
 * @author Markus Litz
 * @author Arne Bachmann
 */
@LazyDisposal
public class CpacsWriterComponent extends DefaultComponent {

    private static final SimpleDateFormat DATEFORMAT = new SimpleDateFormat("yyyyMMdd-HHmmssSSS");

    private static final TempFileService TEMP_MANAGER = TempFileServiceAccess.getInstance();
    
    private ComponentContext componentContext;
    
    private DistributedNotificationService notificationService;

    private ComponentDataManagementService dataManagementService;

    private boolean overwriteEachRun = true;

    /** The dir to save mapped results to. */
    private File tempDir = null;

    private XmlComponentHistoryDataItem historyDataItem = null;
    
    private EndpointXMLService endpointXmlUtils;

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
    }

    @Override
    public void start() throws ComponentException {
        dataManagementService = componentContext.getService(ComponentDataManagementService.class);
        endpointXmlUtils = componentContext.getService(EndpointXMLService.class);
        notificationService = componentContext.getService(DistributedNotificationService.class);

        notificationService.setBufferSize(componentContext.getExecutionIdentifier() + CpacsWriterComponentConstants.RUNTIME_CPACS_UUIDS, 1);

        overwriteEachRun = Boolean.parseBoolean(componentContext.getConfigurationValue(CpacsWriterComponentConstants.SAVE_MODE));

        try {
            tempDir = TEMP_MANAGER.createManagedTempDir("CPACS-Saving-" + componentContext.getExecutionIdentifier());
        } catch (final IOException e) {
            throw new IllegalStateException("Cannot create temporary directory", e);
        }
    }

    
    @Override
    public void processInputs() throws ComponentException {
        initializeNewHistoryDataItem();
        
        // final CpacsRunningState crs = new CpacsRunningState(); // prepare the history object

        String inputUuid = CpacsChannelFilter.getCpacs(ChameleonCommonConstants.CHAMELEON_CPACS_NAME, componentContext);

        final File tempFile = new File(tempDir, ChameleonCommonConstants.CHAMELEON_CPACS_FILENAME);

        // determine local storage, if specified, or null, if not
        final String localStore = componentContext.getConfigurationValue(CpacsWriterComponentConstants.LOCAL_STORE_FOLDER);
        final File localFolder;
        if ((localStore != null) && (!localStore.trim().isEmpty())) {
            File folder = new File(localStore);
            if (folder.isAbsolute()) {
                localFolder = folder;
            } else {
                localFolder = null;
                String message = StringUtils.format("Could not write output file. Path '%s' is not absolute.", folder.getPath());
                componentContext.printConsoleLine(message, ConsoleRow.Type.COMPONENT_OUTPUT);
                LogFactory.getLog(CpacsWriterComponent.class).warn(message);
            }
        } else {
            localFolder = null;
        }

        try {
            dataManagementService.copyReferenceToLocalFile(inputUuid, tempFile, componentContext.getDefaultStorageNodeId());

            // update incoming CPACS with singular input-variables
            Map<String, TypedDatum> varInputs = CpacsChannelFilter.getVariableInputs(componentContext);
            endpointXmlUtils.updateXMLWithInputs(tempFile, varInputs, componentContext);

            final String fileName;
            if (!overwriteEachRun) {
                fileName = padFilename("cpacs_with_variables_", componentContext.getExecutionCount());
            } else {
                fileName = ChameleonCommonConstants.CHAMELEON_CPACS_FILENAME;
            }

            final FileReferenceTD fileReference =
                dataManagementService.createFileReferenceTDFromLocalFile(componentContext, tempFile, fileName);

            final String cpacs = fileReference.getFileReference();

            if (!varInputs.isEmpty() && historyDataItem != null) {
                historyDataItem.setXmlWithVariablesFileReference(cpacs);
            }

            // write output to local folder if configured to do so
            if (localFolder != null) {
                dataManagementService.copyReferenceToLocalFile(cpacs, new File(localFolder, fileName),
                    componentContext.getDefaultStorageNodeId());
            }

            // write output to channel(s)
            componentContext.writeOutput(ChameleonCommonConstants.CHAMELEON_CPACS_NAME, fileReference);
            // update singular output variables with data from CPACS
            endpointXmlUtils.updateOutputsFromXML(tempFile, componentContext);
            notificationService.send(componentContext.getExecutionIdentifier()
                + CpacsWriterComponentConstants.RUNTIME_CPACS_UUIDS, fileReference.getFileReference());
        } catch (IOException | DataTypeException e) {
            throw new ComponentException(e.getMessage(), e);
        }
        storeHistoryDataItem();
    }
    
    @Override
    public void tearDown(FinalComponentState state) {
        switch (state) {
        case FAILED:
            storeHistoryDataItem();
            break;
        default:
            break;
        }
        super.tearDown(state);
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

    /**
     * Take a filename and the counter and create a padded file name.
     * 
     * @param name The file name
     * @param count The file count
     * @return The padded file name
     */
    private String padFilename(final String name, final long counter) {
        String suffix = Long.toString(counter); // decimal count, starting with 0
        while (suffix.length() < 8) {
            suffix = "0" + suffix;
        }
        return name + suffix + "_" + DATEFORMAT.format(new Date()) + ".xml";
    }

    @Override
    public void dispose() {
        if (componentContext != null) {
            notificationService.removePublisher(componentContext.getExecutionIdentifier()
                + CpacsWriterComponentConstants.RUNTIME_CPACS_UUIDS);            
        }
    }
}
