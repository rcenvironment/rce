/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.LogFactory;

/**
 * A static access singleton that allows classes to use the {@link TempFileService} without introducing a dependency on RCE core classes.
 * <p>
 * TODO add remark about direct OSGi injection when it is available
 * 
 * @author Robert Mischke
 */
public class TempFileServiceAccess {

    /**
     * A simple proxy implementation for cases when non-OSGi code fetches the service before it was initialized by the OSGi framework.
     * 
     * @author Robert Mischke
     */
    private static final class ServiceProxy implements TempFileService {

        public File createManagedTempDir() throws IOException {
            return getNullSafeInstance().createManagedTempDir();
        }

        public File createManagedTempDir(String infoText) throws IOException {
            return getNullSafeInstance().createManagedTempDir(infoText);
        }

        public File createTempFileFromPattern(String filenamePattern) throws IOException {
            return getNullSafeInstance().createTempFileFromPattern(filenamePattern);
        }

        public File createTempFileWithFixedFilename(String filename) throws IOException {
            return getNullSafeInstance().createTempFileWithFixedFilename(filename);
        }

        public File writeInputStreamToTempFile(InputStream is) throws IOException {
            return getNullSafeInstance().writeInputStreamToTempFile(is);
        }

        public void disposeManagedTempDirOrFile(File tempFileOrDir) throws IOException {
            getNullSafeInstance().disposeManagedTempDirOrFile(tempFileOrDir);
        }

        private TempFileService getNullSafeInstance() {
            TempFileService instanceCopy = TempFileServiceAccess.instance; // important not to use getInstance() here
            if (instanceCopy == null) {
                throw new IllegalStateException("A TempFileService method was called while no global instance was available");
            }
            return instanceCopy;
        }
    }

    private static volatile TempFileService instance;

    private static final String UNIT_TEST_RELATIVE_GLOBAL_ROOT_DIR_PATH = "rce-unittest";

    private static TempFileManager currentMmanager;

    protected TempFileServiceAccess() {}

    /**
     * @return a {@link TempFileService} implementation; note that this may be a proxy for the actual service instance
     */
    public static TempFileService getInstance() {
        // note: "instance" is volatile, so no synchronization is needed
        TempFileService instanceCopy = instance;
        // defensive copy to prevent race conditions
        if (instanceCopy != null) {
            return instanceCopy;
        }
        LogFactory.getLog(TempFileServiceAccess.class).debug(
            "A TempFileService instance was requested before the global instance was ready, returning a proxy");
        return new ServiceProxy();
    }

    /**
     * Sets the given folder as the "global root" directory to use. Inside this directory, the "instance root" directories are created and
     * deleted.
     * 
     * @param globalTempDirectoryRoot the global root directory to use; must already exist
     * @param instancePrefix an arbitrary prefix to use for the instance directories created inside the provided global root directory; can
     *        be empty or null
     * @throws IOException on initialization failure
     */
    public static void setupLiveEnvironment(File globalTempDirectoryRoot, String instancePrefix) throws IOException {
        // TODO ignore call if already using same root?
        // TODO "shut down" any pre-existing instance?
        currentMmanager = new TempFileManager(globalTempDirectoryRoot, instancePrefix);
        setInstance(currentMmanager.getServiceImplementation());
    }

    /**
     * Convenience method for unit tests. Sets a hard-coded sub-folder of the system temporary directory as the "global root".
     */
    public static void setupUnitTestEnvironment() {
        if (TempFileServiceAccess.instance == null) {
            try {
                // TODO ignore call if already using same root?
                // TODO "shut down" any pre-existing instance?
                if (currentMmanager == null) {
                    currentMmanager = new TempFileManager(getDefaultTestRootDir(), null, true);
                }
                setInstance(currentMmanager.getServiceImplementation());
            } catch (IOException e) {
                // always expected to work, so using an unchecked exception for test code convenience
                throw new RuntimeException("Failed to initialize unittest temp directory root", e);
            }
        }

    }

    protected static void setInstance(TempFileService newInstance) {
        if (newInstance == null) {
            throw new IllegalArgumentException("Setting the global instance back to 'null' is not allowed");
        }
        TempFileServiceAccess.instance = newInstance;
    }

    protected static File getDefaultTestRootDir() {
        return new File(System.getProperty("java.io.tmpdir"), UNIT_TEST_RELATIVE_GLOBAL_ROOT_DIR_PATH);
    }

    /**
     * "Reset" method for unit/integration tests.
     */
    protected static void discardCurrentSetup() {

        currentMmanager = null;
        TempFileServiceAccess.instance = null;
    }
}
