/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement.internal;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.FileCompressionFormat;
import de.rcenvironment.core.utils.common.FileCompressionService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.executor.LocalApacheCommandLineExecutor;
import de.rcenvironment.core.utils.incubator.FileSystemOperations;

/**
 * I/O operations for managing external installations, like downloading, unpacking, deleting etc.
 * 
 * @author Robert Mischke
 * @author Brigitte Boden
 * @author Thorsten Sommer (integration of {@link FileCompressionService})
 * @author Lukas Rosenbach
 */
public class DeploymentOperationsImpl {

    private static final String RCE = "rce";

    private static final int BYTE_TO_MEGABYTE_FACTOR = 1024 * 1024;

    private static final int CONSOLE_SHOW_PROGRESS_INTERVAL = 500;

    private static final String DELETION_MARKER_FILE_NAME = "markedForDeletion";

    private final Log log = LogFactory.getLog(getClass());

    private volatile boolean downloadRunning = false;
    
    private volatile boolean downloadFinished = false;

    private TextOutputReceiver userOutputReceiver;

    /**
     * Downloads a file from the given URL. If the download fails for any reason, an {@link IOException} is thrown.
     * 
     * @param url the URL to download from
     * @param targetFile the file to write to
     * @param allowOverwrite whether the target file is allowed to already exist
     * @param showProgress whether the progress should be shown on console
     * @param timeout time in milliseconds until download is canceled
     * @throws IOException on download failure, or if the target file already exists with "allowOverwrite" set to false
     */
    public void downloadFile(final String url, final File targetFile, boolean allowOverwrite,
            boolean showProgress, int timeout) throws IOException {
        // - MUST detect the case if the URL is invalid or does not exist (e.g., do not just return an empty file)
        // - SHOULD try to detect incomplete downloads (disk full etc), if possible
        final String targetPath = targetFile.getAbsolutePath();
        if (targetFile.exists()) {
            if (!allowOverwrite) {
                throw new IOException("Target file " + targetPath + " does already exist");
            } else {
                if (!targetFile.delete()) {
                    throw new IOException("Failed to delete existing download file " + targetPath);
                }
            }
        }
        final File tempDownloadFile = new File(targetFile.getParentFile(), targetFile.getName() + ".tmp");
        log.debug(StringUtils.format("Starting download of %s to %s", url, tempDownloadFile.getAbsolutePath()));

        final long sizeOfRequestedFile = getFileSize(new URL(url));

        if (sizeOfRequestedFile > tempDownloadFile.getParentFile().getUsableSpace()) {
            throw new IOException("Download failed, not enough diskspace.");
        }

        // FIXME validate file strings for security
        
        //validate URL
        try {
            new URI(url);
        } catch (URISyntaxException e){
            throw new IOException("URL is not valid: " + e.getMessage());
        }
        
        
        downloadRunning = true;
        downloadFinished = false;
        
        if (showProgress) {
            // spawn background task to show download progress
            ConcurrencyUtils.getAsyncTaskService().execute("Instance Management: Print download progress to console", () -> {
                final long totalProgressValue = sizeOfRequestedFile / BYTE_TO_MEGABYTE_FACTOR;
                long lastProgressValue = 0; // intended side effect: do not print "0 MB done" progress

                while (downloadRunning && tempDownloadFile.length() < sizeOfRequestedFile) {
                    final long roundedProgressValue = tempDownloadFile.length() / BYTE_TO_MEGABYTE_FACTOR;
                    // do not print the same progress value over and over in case of a slow download
                    if (roundedProgressValue != lastProgressValue) {
                        userOutputReceiver.addOutput("Downloaded " + roundedProgressValue
                            + " MB of " + totalProgressValue + " MB");
                        lastProgressValue = roundedProgressValue;
                    }
                    try {
                        Thread.sleep(CONSOLE_SHOW_PROGRESS_INTERVAL);
                    } catch (InterruptedException e) {
                        log.error("InterruptedException while download progress thread sleeps.");
                    }
                }
                
                if (downloadFinished) {
                    userOutputReceiver.addOutput("Download finished.");
                }
            });

        }
        
        
        try {
            try {
                FileUtils.copyURLToFile(new URL(url), tempDownloadFile, timeout, timeout);
            } catch (IOException e) {
                downloadFinished = false;
                throw new IOException("Download failed: " + e.getMessage());
            }
            boolean renamedSucessfully;
            try {
                renamedSucessfully = tempDownloadFile.renameTo(targetFile);
            } catch (SecurityException e) {
                downloadFinished = false;
                throw new IOException(
                        "Not allowed to write to " + tempDownloadFile.getAbsolutePath() + " or " + targetPath);
            }
            if (!renamedSucessfully) {
                downloadFinished = false;
                throw new IOException(
                    "Failed to move the completed download file " + tempDownloadFile.getAbsolutePath() + " to " + targetPath);
            } else {
                downloadFinished = true;
            }
        } finally {
            downloadRunning = false;
        }
    }

    /**
     * Extracts a product zip and prepares it for execution (e.g. required "chmod" calls).
     * 
     * @param zipFile the product zip file
     * @param installationDir the installation directory; it must not already exist, and will directly contain the main executable on
     *        success (ie no additional "rce/" folder inside this directory)
     * @throws IOException on setup failure; in this case, this method will try to remove the incomplete directory
     */
    public void installFromProductZip(final File zipFile, final File installationDir) throws IOException {
        // - for safety, the installationDir *must not* already exist (to prevent any overwriting)
        // - try to delete the (incomplete) directory on any failure
        if (installationDir.exists()) {
            throw new IOException("Target installation directory does already exist: " + installationDir.getAbsolutePath());
        }
        log.debug("Starting to extract " + zipFile.getAbsolutePath() + " to " + installationDir.getAbsolutePath());

        if (!FileCompressionService.expandCompressedDirectoryFromFile(zipFile, installationDir,
            FileCompressionFormat.ZIP)) {
            log.error("Was not able to install from product zip due to an archive issue.");
            throw new IOException("Was not able to install from product zip due to an archive issue.");
        }

        // move all content one folder level "up"
        final File rceFolder = new File(installationDir, RCE);
        if (!rceFolder.isDirectory()) {
            throw new IOException("Expected 'rce' folder does not exist");
        }
        final File newFolder = new File(installationDir, "something");
        rceFolder.renameTo(newFolder);
        for (final File contentFile : newFolder.listFiles()) {
            FileUtils.moveToDirectory(contentFile, installationDir, false);
        }
        if (!newFolder.delete()) {
            throw new IOException("Failed to delete the nested 'rce' folder (which should be empty at this point)");
        }
        final File execfile = new File(installationDir, RCE);
        if (execfile.exists()) {
            // If the file "rce" exists, this is a linux platform and we have to make the file executable.
            final LocalApacheCommandLineExecutor executor = new LocalApacheCommandLineExecutor(installationDir);
            executor.start(StringUtils.format("chmod +x %s", execfile.getAbsolutePath()));
        }
    }

    /**
     * Deletes a directory set up with {@link #installFromProductZip(File, File)}.
     * 
     * @param installationDir the installation directory to delete
     * @throws IOException on teardown failure; a typical reason is that an instance is still running from this installation
     */
    public void deleteInstallation(File installationDir) throws IOException {
        // folder must contain rce or rce.exe and p2, plugins and features folders
        boolean containsRCEExecutable = false;
        boolean containsP2Folder = false;
        boolean containsPluginsFolder = false;
        boolean containsFeaturesFolder = false;
        boolean containsDeletionMarkerFile = false;
        for (File file : installationDir.listFiles()) {
            if (file.isFile()) {
                if (file.getName().equals("rce.exe") || file.getName().equals(RCE)) {
                    containsRCEExecutable = true;
                } else if (file.getName().equals(DELETION_MARKER_FILE_NAME)) {
                    containsDeletionMarkerFile = true;
                }
            } else if (file.isDirectory()) {
                if (file.getName().equals("p2")) {
                    containsP2Folder = true;
                } else if (file.getName().equals("plugins")) {
                    containsPluginsFolder = true;
                } else if (file.getName().equals("features")) {
                    containsFeaturesFolder = true;
                }
            }
        }

        if ((containsFeaturesFolder && containsP2Folder && containsPluginsFolder && containsRCEExecutable) || containsDeletionMarkerFile) {
            int i = 10;
            final int waitTime = 1000;
            boolean wasDeleted = false;
            do {
                FileSystemOperations.deleteSandboxDirectory(installationDir);
                if (!installationDir.exists()) {
                    wasDeleted = true;
                    break;
                }
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    throw new IOException("Failed to delete installation directory at location " + installationDir.getAbsolutePath()
                        + ". Task was interrupted.");
                }
                i--;
            } while (i > 0);
            if (!wasDeleted) {
                // Create a marker file which indicates the directory should be deleted in a later try.
                File markerFile = new File(installationDir, DELETION_MARKER_FILE_NAME);
                if (!markerFile.exists() && !markerFile.createNewFile()) {
                    throw new IOException("Could not delete installation directory at location " + installationDir.getAbsolutePath()
                        + " and could not create a file marking it for future deletion. Possibly access rights are missing.");
                }

                throw new IOException("Could not delete installation directory at location " + installationDir.getAbsolutePath()
                    + ". Most likely it is used by another program.");
            }
        } else {
            throw new IOException(
                "The existing installation directory " + installationDir.getAbsolutePath()
                    + " does not look like a valid RCE installation; aborting delete operation");
        }
    }

    private int getFileSize(URL url) throws IOException {
        HttpURLConnection httpConnection = null;
        try {
            httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setRequestMethod("HEAD");
            httpConnection.getInputStream();
            return httpConnection.getContentLength();
        } finally {
            if (httpConnection != null) {
                httpConnection.disconnect();
            } else {
                throw new IOException("Failed to get file size of remote file: " + url.toExternalForm());
            }
        }
    }

    public void setUserOutputReceiver(TextOutputReceiver userOutputReceiver) {
        this.userOutputReceiver = userOutputReceiver;
    }

}
