/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.generic.execution.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.io.FileUtils;

import de.rcenvironment.components.optimizer.common.MethodDescription;
import de.rcenvironment.components.optimizer.common.OptimizerComponentConstants;
import de.rcenvironment.components.optimizer.common.OptimizerComponentHistoryDataItem;
import de.rcenvironment.components.optimizer.common.execution.CommonPythonAlgorithmExecutor;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.utils.common.CompressingHelper;
import de.rcenvironment.core.utils.common.LogUtils;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * This class provides everything for running the generic optimizer blackbox.
 * 
 * @author Sascha Zur
 */
public class GenericAlgorithmExecutor extends CommonPythonAlgorithmExecutor {

    private File configurationFile;

    private FileReferenceTD configurationFileReference;

    public GenericAlgorithmExecutor(Map<String, MethodDescription> methodConfiguration,
        Map<String, TypedDatum> outputValues, Collection<String> input, ComponentContext ci,
        Map<String, Double> upperMap, Map<String, Double> lowerMap, Map<String, Double> stepValues) throws ComponentException {
        super(methodConfiguration, outputValues, input, ci, upperMap, lowerMap, stepValues, "input.in");
    }

    @Override
    protected void prepareProblem() throws ComponentException {
        try {
            File sourceFolder = getSourceFolder();
            if (sourceFolder.exists()) {
                if (sourceFolder.exists()) {
                    FileUtils.copyDirectoryToDirectory(sourceFolder, workingDir);
                }
            }
            CompressingHelper.unzip(GenericAlgorithmExecutor.class.getResourceAsStream("/resources/RCE_Optimizer_API.zip"),
                new File(workingDir, "source/"));
        } catch (IOException | ArchiveException e) {
            throw new ComponentException("Failed to prepare generic algorithm", e);
        }
        configurationFile = new File(new File(workingDir, OptimizerComponentConstants.GENERIC_SOURCE), "configuration.json");
        writeConfigurationFile(configurationFile);
    }

    private File getSourceFolder() {
        File configFolder =
            new File(compContext.getService(ConfigurationService.class).getConfigurablePath(
                ConfigurablePathId.DEFAULT_WRITEABLE_INTEGRATION_ROOT), "optimizer");
        File sourceFolder = new File(new File(configFolder, methodConfiguration.get(algorithm).getConfigValue("genericFolder")),
            OptimizerComponentConstants.GENERIC_SOURCE);
        return sourceFolder;
    }

    @Override
    public int getOptimalRunNumber() throws ComponentException {
        File result = new File(workingDir, "generic.result");
        try (BufferedReader fr = new BufferedReader(new FileReader(result))) {
            String firstLine = fr.readLine();
            if (firstLine != null) {
                return Integer.parseInt(firstLine);
            }
            throw new ComponentException("Failed to parse result file for information about optimal variables;"
                + " cause: result file is empty");
        } catch (IOException e) {
            throw new ComponentException("Failed to parse result file for information about optimal variables", e);
        }
    }

    @Override
    @TaskDescription("Optimizer Algorithm Executor Generic")
    public void run() {
        try {
            File pythonPathFile = new File(getSourceFolder(), "python_path");
            if (pythonPathFile.exists()) {
                List<String> lines = FileUtils.readLines(pythonPathFile);
                if (lines.size() > 0 && lines.get(0) != null && !lines.get(0).isEmpty()) {
                    String pythonPath = lines.get(0);
                    if (pythonPath == null || pythonPath.isEmpty() || pythonPath.contains("$")) {
                        LOGGER.warn("Failed to find path to Python; trying 'python'");
                        pythonPath = "python";
                    }
                    startProgram(OptimizerComponentConstants.GENERIC_SOURCE
                        + File.separator + OptimizerComponentConstants.GENERIC_MAIN_FILE,
                        pythonPath);
                } else {
                    throw new ComponentException("Could not read python path from file: " + pythonPathFile.getAbsolutePath());
                }
            } else {
                throw new ComponentException(
                    "Could not read python path from file, it does not exist: " + pythonPathFile.getAbsolutePath());
            }
        } catch (ComponentException | IOException e) {
            startFailed.set(true);
            startFailedException = e;
            LOGGER.error("Could not start external algorithm: ", e);
        }
    }

    @Override
    public void writeHistoryDataItem(OptimizerComponentHistoryDataItem historyItem) {
        if (configurationFileReference == null) {
            try {
                configurationFileReference =
                    compContext.getService(ComponentDataManagementService.class).createFileReferenceTDFromLocalFile(compContext,
                        configurationFile, "configuration.json");
            } catch (IOException e) {
                String errorMessage = "Failed to store configuration file into the data management"
                    + "; it is not available in the workflow data browser";
                String errorId = LogUtils.logExceptionWithStacktraceAndAssignUniqueMarker(LOGGER, errorMessage, e);
                compContext.getLog().componentError(errorMessage, e, errorId);

            }
        }
        if (configurationFileReference != null) {
            historyItem.setInputFileReference(configurationFileReference.getFileReference());
        }
    }

}
