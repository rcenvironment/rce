/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.dlr.sc.chameleon.rce.toolwrapper.generator;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import de.dlr.sc.chameleon.rce.toolwrapper.common.CpacsComponentConstants;
import de.dlr.sc.chameleon.rce.toolwrapper.common.CpacsWrapperInfo;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.utils.incubator.ImageResize;

/**
 * Takes the (dummy) toolWrapper bundle, generations a working rce component(s) and restarts the bundle. This will be done each time RCE
 * starts up.
 * 
 * @author Markus Litz
 * @author Markus Kunde
 */
public class ToolWrapperGenerator {

    /** Logger. */
    public static final Log LOGGER = LogFactory.getLog(ToolWrapperGenerator.class);

    private static final String APPENDIX_32 = "_32";

    private static final String APPENDIX_16 = "_16";

    private static final int ICON_SIZE_32 = 32;

    private static final int ICON_SIZE_16 = 16;

    private static final String WARN_CONFIGURATION_BROKEN = "Error when restarting bundle. Couldn't find bundle: ";

    private static final String ERR_BUNDLE_START = "Couldn't start bundle: ";

    private static final String RESOURCE_PREFIX = File.separator + "resources" + File.separator;

    private static final String OSGI_INF_PREFIX = "OSGI-INF/";

    private static final String OSGIINF_TEMPLATE = "/resources/template-osgi.xml";

    private static final String CHAMELEON_DEFAULT_ICON_16 = "/resources/logo16.png";

    private static final String CHAMELEON_DEFAULT_ICON_32 = "/resources/logo32.png";

    private static final String TOOLWRAPPER_BUNDLE_ID = "de.rcenvironment.components.toolwrapper.execution";

    private String configurationPath = "";

    private ConfigurationService configurationService;

    private BundleContext bundleContext;

    private String osgiInfTemplate = null;

    private String toolWrapperFileName = null;

    /**
     * The list of tools that should be wrapped.
     */
    private List<CpacsWrapperInfo> toolList = new ArrayList<>();

    /**
     * This method will be called when the bundle is started.
     * 
     * @param context
     * @throws IOException
     */
    protected void activate(final BundleContext context) throws IOException {
        bundleContext = context;

        toolWrapperFileName = getToolWrapperFileName(TOOLWRAPPER_BUNDLE_ID);

        // get OSGI-component xml file as template
        InputStream is = getClass().getResourceAsStream(OSGIINF_TEMPLATE);

        osgiInfTemplate = IOUtils.toString(is);

        if (osgiInfTemplate == null) {
            return;
        }

        // clear the ToolWrapper directories (resources && osgi-inf)
        clearDirectories();

        // read .cpacsWrapper Files, parse and copy them
        readToolWrapperConfigFiles();

        // Generate the osgi-inf/*.xml files and update the Manifest.
        generateOSGIandManifest();

        // restart the ToolWrapper bundle
        restartBundle(TOOLWRAPPER_BUNDLE_ID);
    }

    protected void bindConfigurationService(final ConfigurationService newConfigurationService) {
        configurationService = newConfigurationService;
        configurationPath = configurationService.getConfigurablePath(
            ConfigurablePathId.DEFAULT_WRITEABLE_INTEGRATION_ROOT).getAbsolutePath();
    }

    protected void unbindConfigurationService(final ConfigurationService oldConfigurationService) {
        // nothing to do here, but if this unbind method is missing, OSGi DS
        // failed when disposing component
        // (seems to be a DS bug)
    }

    /**
     * Removes generated and copied files from a previous run.
     */
    private void clearDirectories() {
        String path = toolWrapperFileName + "/" + OSGI_INF_PREFIX;
        File directory = new File(path);
        if (!directory.exists()) {
            LOGGER.error(OSGI_INF_PREFIX + " directory does not exist, try to create it...");
            if (!directory.mkdirs()) {
                LOGGER.error("Could not create directory");
            } else {
                LOGGER.error(OSGI_INF_PREFIX + " directory created");
            }
        }
        for (String fileName : directory.list(new FileNameFilter(".xml"))) {
            File f = new File(path + fileName);
            if (!fileName.equals(CpacsComponentConstants.COMPONENTUPDATERFILE)) {
                f.delete();
            }
        }

        path = toolWrapperFileName + RESOURCE_PREFIX;
        directory = new File(path);
        if (!directory.exists()) {
            LOGGER.error(RESOURCE_PREFIX + " directory does not exist, try to create it...");
            if (!directory.mkdirs()) {
                LOGGER.error("Could not create directory");
            } else {
                LOGGER.error(RESOURCE_PREFIX + " directory created");
            }
        }
        for (String fileName : directory.list(new FileNameFilter(CpacsComponentConstants.CPACSWRAPPER_FILEEXTENTION))) {
            File f = new File(path + fileName);
            if (!fileName.equals(CpacsComponentConstants.COMPONENTUPDATERFILE)) {
                f.delete();
            }
        }
    }

    /**
     * Generates the OSGI-INF files from a template and modifies the manifest.
     */
    private void generateOSGIandManifest() {
        // Iterate through tools...
        for (CpacsWrapperInfo cpacsWrapper : toolList) {

            // Generate OSGI-INF files
            String osgiInf = osgiInfTemplate;
            osgiInf = osgiInf.replace("TOOLNAME", cpacsWrapper.getToolName());

            File icon16source = new File(cpacsWrapper.getToolIcon16());
            File icon32source = new File(cpacsWrapper.getToolIcon32());
            osgiInf = osgiInf.replace("TOOLICON16", icon16source.getName() + APPENDIX_16);
            osgiInf = osgiInf.replace("TOOLICON32", icon32source.getName() + APPENDIX_32);

            try {
                FileUtils.writeStringToFile(new File(toolWrapperFileName + File.separator + OSGI_INF_PREFIX + cpacsWrapper.getToolName()
                    + ".xml"), osgiInf);
            } catch (IOException e) {
                LOGGER.error(e);
            }
        }
    }

    /**
     * Reads in the .cpacsWrapper files which defines how many tools shouw be wrapped and how.
     */
    private void readToolWrapperConfigFiles() {
        String path = configurationPath + File.separator + CpacsComponentConstants.CPACSWRAPPER_CONFIGDIR;
        File directory = new File(path);
        if (!directory.exists()) {
            return;
        }
        toolList = new ArrayList<CpacsWrapperInfo>();
        for (String fileName : directory.list(new FileNameFilter(CpacsComponentConstants.CPACSWRAPPER_FILEEXTENTION))) {
            String fullFileName = path + File.separator + fileName;
            CpacsWrapperInfo aTool;
            try {
                aTool = new CpacsWrapperInfo(fullFileName);
                // add to List of tools
                toolList.add(aTool);

                // copy .cpacsWrapper file to ToolWrapper
                File source = new File(fullFileName);
                File destination =
                    new File(toolWrapperFileName + RESOURCE_PREFIX + aTool.getToolName()
                        + CpacsComponentConstants.CPACSWRAPPER_FILEEXTENTION);
                try {
                    FileUtils.copyFile(source, destination);
                } catch (IOException e) {
                    LOGGER.error(e);
                }

                // Set default icons if empty or does not exist
                try {
                    final File icon16source = new File(aTool.getToolIcon16());
                    final File icon16target = new File(toolWrapperFileName + RESOURCE_PREFIX + icon16source.getName() + APPENDIX_16);
                    if (!icon16source.exists()) {
                        InputStream is = getClass().getResourceAsStream(CHAMELEON_DEFAULT_ICON_16);
                        FileUtils.copyInputStreamToFile(is, icon16target);
                    } else {
                        FileUtils.copyFile(icon16source, icon16target);
                    }
                    BufferedImage bi16 = ImageResize.resize(ImageIO.read(new FileImageInputStream(icon16target)), ICON_SIZE_16);
                    ImageIO.write(bi16, "PNG", icon16target);

                    final File icon32source = new File(aTool.getToolIcon32());
                    final File icon32target = new File(toolWrapperFileName + RESOURCE_PREFIX + icon32source.getName() + APPENDIX_32);
                    if (!icon32source.exists()) {
                        InputStream is = getClass().getResourceAsStream(CHAMELEON_DEFAULT_ICON_32);
                        FileUtils.copyInputStreamToFile(is, icon32target);
                    } else {
                        FileUtils.copyFile(icon32source, icon32target);
                    }
                    BufferedImage bi32 = ImageResize.resize(ImageIO.read(new FileImageInputStream(icon32target)), ICON_SIZE_32);
                    ImageIO.write(bi32, "PNG", icon32target);

                } catch (final IOException e) {
                    LOGGER.error(e);
                }
            } catch (IOException e1) {
                LOGGER.error(e1);
            }

        }
    }

    /**
     * Gives the full path to the bundle.
     * 
     * @param id - bundleID
     * @return
     */
    private String getToolWrapperFileName(final String id) {
        URL url = Platform.getBundle(id).getEntry("/");
        String fName = null;

        try {
            url = FileLocator.resolve(url);
            fName = url.getFile();

            if (fName.startsWith("file:/")) {
                fName = fName.substring(6, fName.length());
            }

            if (fName.endsWith("!/")) {
                fName = fName.substring(0, fName.length() - 2);
            }
        } catch (IOException e) {
            LOGGER.error(e);
        }

        File file = new File(fName);
        fName = file.getAbsolutePath();
        return fName;

    }

    /**
     * Restarts bundles with updated configuration.
     */
    private void restartBundle(final String bundleName) {
        boolean found = false;
        for (final Bundle bundle : bundleContext.getBundles()) {
            if (bundle.getSymbolicName().equals(bundleName)) {
                found = true;
                try {
                    bundle.stop();
                    bundle.start();
                } catch (final BundleException e) {
                    LOGGER.error(ERR_BUNDLE_START + bundle.getSymbolicName(), e);
                } catch (final IllegalStateException e) {
                    LOGGER.error(ERR_BUNDLE_START + bundle.getSymbolicName(), e);
                } catch (final SecurityException e) {
                    LOGGER.error(ERR_BUNDLE_START + bundle.getSymbolicName(), e);
                }
            }
        }
        if (!found) {
            LOGGER.warn(WARN_CONFIGURATION_BROKEN + bundleName);
        }
    }

    /**
     * Filename filter.
     * 
     * @author Markus Kunde
     */
    protected static final class FileNameFilter implements FilenameFilter {

        private String filter;

        public FileNameFilter(String filter) {
            this.filter = filter;
        }

        @Override
        public boolean accept(File dir, String name) {
            if (name.endsWith(filter)) {
                return true;
            }
            return false;
        }
    }
}
