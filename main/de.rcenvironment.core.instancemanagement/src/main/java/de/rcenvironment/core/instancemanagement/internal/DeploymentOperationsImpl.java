/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement.internal;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.executor.LocalApacheCommandLineExecutor;
import de.rcenvironment.core.utils.incubator.FileSystemOperations;
import de.rcenvironment.core.utils.incubator.ZipFolderUtil;

/**
 * I/O operations for managing external installations, like downloading, unpacking, deleting etc.
 * 
 * @author Robert Mischke
 */
public class DeploymentOperationsImpl {

    private static final String RCE = "rce";

    private static final int BYTE_TO_MEGABYTE_FACTOR = 1024 * 1024;

    private static final int CONSOLE_SHOW_PROGRESS_INTERVAL = 500;

    private final Log log = LogFactory.getLog(getClass());

    private volatile boolean downloadRunning = false;

    private TextOutputReceiver userOutputReceiver;

    /**
     * Downloads a file from the given URL. If the download fails for any reason, an {@link IOException} is thrown.
     * 
     * @param url the URL to download from
     * @param targetFile the file to write to
     * @param allowOverwrite whether the target file is allowed to already exist
     * @param showProgress whether the progress should be shown on console
     * @throws IOException on download failure, or if the target file already exists with "allowOverwrite" set to false
     */
    public void downloadFile(final String url, final File targetFile, boolean allowOverwrite, boolean showProgress) throws IOException {
        // - MUST detect the case if the URL is invalid or does not exist (e.g., do not just return an empty file)
        // - SHOULD try to detect incomplete downloads (disk full etc), if possible
        String targetPath = targetFile.getAbsolutePath();
        if (!allowOverwrite && targetFile.exists()) {
            throw new IOException("Target file " + targetPath + " does already exist");
        }
        log.debug("Starting download of " + url + " to " + targetPath);

        final long sizeOfRequestedFile = getFileSize(new URL(url));

        if (sizeOfRequestedFile > targetFile.getParentFile().getUsableSpace()) {
            throw new IOException("Download failed, not enough diskspace.");
        }

        // FIXME validate URL and file strings for security

        downloadRunning = true;

        // show download progress
        if (showProgress) {
            SharedThreadPool.getInstance().execute(new Runnable() {

                @Override
                public void run() {
                    while (downloadRunning && targetFile.length() < sizeOfRequestedFile) {
                        userOutputReceiver.addOutput("Downloaded " + targetFile.length() / BYTE_TO_MEGABYTE_FACTOR
                            + " MB of " + sizeOfRequestedFile / BYTE_TO_MEGABYTE_FACTOR + " MB");
                        try {
                            Thread.sleep(CONSOLE_SHOW_PROGRESS_INTERVAL);
                        } catch (InterruptedException e) {
                            log.error("InterruptedException while download progress thread sleeps.");
                        }
                    }
                    userOutputReceiver.addOutput("Download finished.");
                }
            }, "checkSize");

        }

        try {
            FileUtils.copyURLToFile(new URL(url), targetFile);
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
    public void installFromProductZip(File zipFile, File installationDir) throws IOException {
        // - for safety, the installationDir *must not* already exist (to prevent any overwriting)
        // - try to delete the (incomplete) directory on any failure
        if (installationDir.exists()) {
            throw new IOException("Target installation directory does already exist: " + installationDir.getAbsolutePath());
        }
        log.debug("Starting to extract " + zipFile.getAbsolutePath() + " to " + installationDir.getAbsolutePath());
        ZipFolderUtil.extractZipToFolder(installationDir, zipFile);
        // move all content one folder level "up"
        File rceFolder = new File(installationDir, RCE);
        if (!rceFolder.isDirectory()) {
            throw new IOException("Expected 'rce' folder does not exist");
        }
        File newFolder = new File(installationDir, "something");
        rceFolder.renameTo(newFolder);
        for (File contentFile : newFolder.listFiles()) {
            FileUtils.moveToDirectory(contentFile, installationDir, false);
        }
        if (!newFolder.delete()) {
            throw new IOException("Failed to delete the nested 'rce' folder (which should be empty at this point)");
        }
        File execfile = new File(installationDir, RCE);
        if (execfile.exists()) {
            //If the file "rce" exists, this is a linux platform and we have to make the file executable.
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
        for (File file : installationDir.listFiles()) {
            if (file.isFile() && (file.getName().equals("rce.exe") || file.getName().equals(RCE))) {
                containsRCEExecutable = true;
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

        if (containsFeaturesFolder && containsP2Folder && containsPluginsFolder && containsRCEExecutable) {
            FileSystemOperations.deleteSandboxDirectory(installationDir);
        } else {
            throw new IOException("Installation directory seems to be no valid RCE installation");
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
