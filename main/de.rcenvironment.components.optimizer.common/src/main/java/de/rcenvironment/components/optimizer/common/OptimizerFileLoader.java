/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;

/**
 * Static class for loading resources.
 * 
 * @author Sascha Zur
 */
public final class OptimizerFileLoader {

    private static final String RESOURCES = "/resources";

    private static ObjectMapper mapper = new ObjectMapper();

    private static ConfigurationService configService;

    public OptimizerFileLoader() {

    }

    /**
     * Loads all default method descriptions available.
     * 
     * @param algorithmFolder : To choose the method package (optimizer/doe).
     * @return all descriptions
     * @throws JsonParseException :
     * @throws JsonMappingException :
     * @throws IOException :
     */
    @SuppressWarnings("unchecked")
    public static Map<String, MethodDescription> getAllMethodDescriptions(String algorithmFolder)
        throws JsonParseException, JsonMappingException, IOException {
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
                    InputStream algorithmsInputStream = OptimizerFileLoader.class.getResourceAsStream(elementURL2.getFile());
                    Map<String, String> loadedMethods = mapper.readValue(algorithmsInputStream, new HashMap<String, String>().getClass());

                    if (loadedMethods != null) {
                        for (String methods : loadedMethods.keySet()) {
                            loadedMethods.put(methods, rawPath + loadedMethods.get(methods));
                        }
                        methodNamesToFileLinking.putAll(loadedMethods);
                    }
                }
            }
        }
        Map<String, MethodDescription> methodDescriptions = new HashMap<String, MethodDescription>();
        for (Entry<String, String> methodKey : methodNamesToFileLinking.entrySet()) {
            InputStream newDescriptionInputStream = OptimizerFileLoader.class.getResourceAsStream(
                methodNamesToFileLinking.get(methodKey.getKey()) + ".json");
            if (newDescriptionInputStream != null) {
                MethodDescription newDescription = mapper.readValue(newDescriptionInputStream,
                    MethodDescription.class);
                if (newDescription != null) {
                    String fullpath = methodNamesToFileLinking.get(methodKey.getKey());
                    String neededPath = fullpath.substring(0, fullpath.lastIndexOf("/"));
                    InputStream newCommonInputStream =
                        OptimizerFileLoader.class.getResourceAsStream(neededPath + "/defaults.json");
                    newDescription.setCommonSettings(mapper.readValue(newCommonInputStream,
                        new HashMap<String, Map<String, String>>().getClass()));
                }
                methodDescriptions.put(methodKey.getKey(), newDescription);
            }
        }

        // read generic entries
        String configFolder =
            new File(configService.getConfigurablePath(ConfigurablePathId.DEFAULT_WRITEABLE_INTEGRATION_ROOT).getAbsolutePath(),
                "optimizer").getAbsolutePath();
        List<File> genericOptimizerFolder = new LinkedList<File>();
        methodNamesToFileLinking = new HashMap<String, String>();
        File config = new File(configFolder);
        if (config != null && config.exists() && config.isDirectory()) {
            for (File f : config.listFiles()) {
                if (f.getName().contains(algorithmFolder.substring(1) + "_")) {
                    genericOptimizerFolder.add(f);
                }
            }
        }
        for (File optimizerFolder : genericOptimizerFolder) {
            File guiConfigFolder = new File(optimizerFolder.getAbsolutePath()
                + File.separator + OptimizerComponentConstants.GENERIC_GUI_CONFIG);
            if (guiConfigFolder.exists()) {
                File[] guiConfigContent = guiConfigFolder.listFiles();
                File algorithmsFile = null;
                for (File f : guiConfigContent) {
                    if (f.getName().contains("Algorithms")) {
                        algorithmsFile = f;
                    }
                }
                Map<String, String> loadedMethods = null;
                if (algorithmsFile != null && algorithmsFile.isFile()) {
                    loadedMethods = mapper.readValue(algorithmsFile, new HashMap<String, String>().getClass());
                    if (loadedMethods != null) {
                        methodNamesToFileLinking.putAll(loadedMethods);
                    }
                }
                for (Entry<String, String> method : loadedMethods.entrySet()) {
                    File newMethod = new File(guiConfigFolder + File.separator + method.getValue() + ".json");
                    if (newMethod.exists()) {
                        MethodDescription newDescription = mapper.readValue(newMethod,
                            MethodDescription.class);
                        if (newDescription != null && newDescription.getOptimizerPackage() != null) {
                            File newCommonGenericInputFile =
                                new File(guiConfigFolder + File.separator + "/defaults.json");
                            newDescription.setCommonSettings(mapper.readValue(newCommonGenericInputFile,
                                new HashMap<String, Map<String, String>>().getClass()));
                        }
                        if (newDescription != null) {
                            String foldername = optimizerFolder.getName().substring(
                                optimizerFolder.getName().indexOf("_"));
                            newDescription.setConfigValue("genericFolder", foldername);
                            methodDescriptions.put(method.getKey() + " [" + foldername.substring(1) + "'s method]", newDescription);
                        }
                    }
                }

            }
        }
        return methodDescriptions;
    }

    protected void bindConfigurationService(final ConfigurationService configServiceIn) {
        configService = configServiceIn;
    }

    protected void unbindConfigurationService(final OptimizerResultService oldParametricStudyService) {
        configService = null;
    }

}
