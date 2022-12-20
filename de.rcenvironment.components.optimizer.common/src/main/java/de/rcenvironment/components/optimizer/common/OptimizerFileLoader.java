/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Static class for loading resources.
 * 
 * @author Sascha Zur
 * @author Kathrin Schaffert (#17821)
 */
public final class OptimizerFileLoader {

    private static final String RESOURCES = "/resources";

    private static ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();

    private static ConfigurationService configService;

    private static Log log = LogFactory.getLog(OptimizerFileLoader.class);

    public OptimizerFileLoader() {

    }

    /**
     * Loads all default method descriptions available.
     * 
     * @param algorithmFolder : To choose the method package (optimizer/doe).
     * @return all descriptions
     * @throws IOException :
     */
    @SuppressWarnings("unchecked")
    public static Map<String, MethodDescription> getAllMethodDescriptions(String algorithmFolder)
        throws IOException {
        // Fill the list of algorithms provided
        Map<String, String> methodNamesToFileLinking = new HashMap<String, String>();

        Bundle bundle = FrameworkUtil.getBundle(OptimizerFileLoader.class).getBundleContext().getBundle();
        @SuppressWarnings("rawtypes") final Enumeration optimizerDirs = bundle.findEntries(RESOURCES + algorithmFolder, "*", false);
        while (optimizerDirs != null && optimizerDirs.hasMoreElements()) {
            final URL elementURL = (URL) optimizerDirs.nextElement();
            final String rawPath = elementURL.getPath();
            @SuppressWarnings("rawtypes") final Enumeration templateDirs = bundle.findEntries(rawPath, "*", false);
            while (templateDirs != null && templateDirs.hasMoreElements()) {
                final URL elementURL2 = (URL) templateDirs.nextElement();
                final String rawPath2 = elementURL2.getPath();
                if (!rawPath2.contains("/.svn/") && elementURL2.getFile().contains("algorithms")) {
                    try (InputStream algorithmsInputStream = OptimizerFileLoader.class.getResourceAsStream(elementURL2.getFile())) {
                        Map<String, String> loadedMethods = mapper.readValue(algorithmsInputStream,
                            new HashMap<String, String>().getClass());

                        if (loadedMethods != null) {
                            for (String methods : loadedMethods.keySet()) {
                                loadedMethods.put(methods, rawPath + loadedMethods.get(methods));
                            }
                            methodNamesToFileLinking.putAll(loadedMethods);
                        }
                    }
                }
            }
        }
        Map<String, MethodDescription> methodDescriptions = new HashMap<String, MethodDescription>();
        for (Entry<String, String> methodKey : methodNamesToFileLinking.entrySet()) {
            try (InputStream newDescriptionInputStream = OptimizerFileLoader.class.getResourceAsStream(
                methodNamesToFileLinking.get(methodKey.getKey()) + ".json")) {
                if (newDescriptionInputStream != null) {
                    MethodDescription newDescription = mapper.readValue(newDescriptionInputStream,
                        MethodDescription.class);
                    if (newDescription != null) {
                        String fullpath = methodNamesToFileLinking.get(methodKey.getKey());
                        String neededPath = fullpath.substring(0, fullpath.lastIndexOf("/"));
                        try (InputStream newCommonInputStream =
                            OptimizerFileLoader.class.getResourceAsStream(neededPath + "/defaults.json")) {
                            newDescription.setCommonSettings(mapper.readValue(newCommonInputStream,
                                new HashMap<String, Map<String, String>>().getClass()));
                        }
                    }
                    methodDescriptions.put(methodKey.getKey(), newDescription);
                }
            }
        }

        // read generic entries
        File configFolder =
            new File(configService.getConfigurablePath(ConfigurablePathId.DEFAULT_WRITEABLE_INTEGRATION_ROOT).getAbsolutePath(),
                "optimizer");
        List<File> genericOptimizerFolder = new LinkedList<File>();
        methodNamesToFileLinking = new HashMap<String, String>();
        if (configFolder.exists() && configFolder.isDirectory()) {
            if (configFolder != null) {
                File[] configFiles = configFolder.listFiles();
                if (configFiles != null) {
                    for (File integrationFolder : configFiles) {
                        boolean sourceFolder = false;
                        boolean guiFolder = false;
                        if (integrationFolder != null) {
                            readIntegrationFiles(genericOptimizerFolder, integrationFolder, sourceFolder, guiFolder);
                        }
                    }
                }
            }
        }
        for (File optimizerFolder : genericOptimizerFolder) {
            File guiConfigFolder = new File(optimizerFolder.getAbsolutePath()
                + File.separator + OptimizerComponentConstants.GENERIC_GUI_CONFIG);
            if (guiConfigFolder.exists()) {
                File[] guiConfigContent = guiConfigFolder.listFiles();
                File algorithmsFile = null;
                if (guiConfigContent != null) {
                    for (File f : guiConfigContent) {
                        if (f.getName().equals(OptimizerComponentConstants.GENERIC_ALGORITHMS_FILE)) {
                            algorithmsFile = f;
                        }
                    }
                }
                Map<String, String> loadedMethods = null;
                if (algorithmsFile != null && algorithmsFile.isFile()) {
                    loadedMethods = mapper.readValue(algorithmsFile, new HashMap<String, String>().getClass());
                    if (loadedMethods != null) {
                        methodNamesToFileLinking.putAll(loadedMethods);
                    }
                }
                if (loadedMethods != null) {
                    for (Entry<String, String> method : loadedMethods.entrySet()) {
                        File newMethod = new File(new File(guiConfigFolder, method.getValue()) + ".json");
                        if (newMethod.exists()) {
                            try {
                                MethodDescription newDescription = mapper.readValue(newMethod, MethodDescription.class);
                                if (newDescription != null) {
                                    String foldername = optimizerFolder.getName();
                                    newDescription.setConfigValue("genericFolder", foldername);
                                    methodDescriptions.put(method.getKey() + " [" + foldername + "'s method]", newDescription);
                                }
                            } catch (IOException e) {
                                log.error(StringUtils.format("The generic algorithm %s could not be loaded: %s", method.getValue(),
                                    e.getMessage()));
                            }
                        }
                    }
                }

            }
        }
        return methodDescriptions;
    }

    private static void readIntegrationFiles(List<File> genericOptimizerFolder, File integrationFolder, boolean sourceFolder,
        boolean guiFolder) {
        File[] integrationFiles = integrationFolder.listFiles();
        if (integrationFiles != null) {
            for (File sourceOrGuiFolder : integrationFiles) {
                if (sourceOrGuiFolder.getName().equals(OptimizerComponentConstants.GENERIC_GUI_CONFIG)
                    && new File(sourceOrGuiFolder, OptimizerComponentConstants.GENERIC_ALGORITHMS_FILE).exists()) {
                    guiFolder = true;
                }
                if (sourceOrGuiFolder.getName().equals(OptimizerComponentConstants.GENERIC_SOURCE)
                    && new File(sourceOrGuiFolder, OptimizerComponentConstants.GENERIC_MAIN_FILE).exists()) {
                    sourceFolder = true;
                }
            }
            if (sourceFolder && guiFolder) {
                genericOptimizerFolder.add(integrationFolder);
            }
        }
    }

    protected void bindConfigurationService(final ConfigurationService configServiceIn) {
        configService = configServiceIn;
    }

    protected void unbindConfigurationService(final OptimizerResultService oldParametricStudyService) {
        configService = null;
    }

    /**
     * load method if it is not available in the current optimizer config.
     * 
     * @param key method to load
     * @return the method
     * @throws IOException from reading the files
     */
    public static MethodDescription loadMethod(String key) throws IOException {
        Map<String, MethodDescription> all = getAllMethodDescriptions("/optimizer");
        return all.get(key);
    }

}
