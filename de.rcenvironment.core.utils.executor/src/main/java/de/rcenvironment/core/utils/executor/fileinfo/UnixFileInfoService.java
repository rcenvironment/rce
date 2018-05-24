/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.utils.executor.fileinfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.rcenvironment.core.toolkitbridge.transitional.TextStreamWatcherFactory;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.textstream.TextStreamWatcher;
import de.rcenvironment.core.utils.common.textstream.receivers.CapturingTextOutReceiver;
import de.rcenvironment.core.utils.executor.CommandLineExecutor;
import de.rcenvironment.core.utils.executor.fileinfo.internal.AbstractFileService;

/**
 * {@link FileInfoService} implementation for unix systems.
 * 
 * @author Christian Weiss
 */
public class UnixFileInfoService extends AbstractFileService {

    private static final Pattern LS_DIRECTORY_PATTERN = Pattern.compile("^(.*):$");

    private static final Pattern LS_TOTAL_PATTERN = Pattern.compile("^total \\d+$");

    private static final Pattern LS_LINE_PATTERN = Pattern
        .compile("^([-dl])(?:[-r][-w][-xst]){3}(?:\\+)?\\s+\\d+\\s+([-_\\w]+)\\s+([-_\\w]+)\\s+(\\d+)\\s+(\\d+)\\s+(.*)\\s*$");

    private static final Pattern FILE_CMD_DIRECTORY_PATTERN = Pattern.compile(".*: (?:sticky )?directory$");

    private static final Pattern FILE_CMD_SYMLINK_PATTERN = Pattern.compile("^(.*): (?:broken )?symbolic link to `(.*)'$");

    private static final Pattern NON_EXISTING_PATTERN = Pattern.compile("^.*: cannot open `.*' \\(No such file or directory\\)$");

    private static final Pattern PERMISSION_DENIED_PATTERN = Pattern.compile("^.*: cannot access `.*': \\(?Permission denied\\)?$");

    public UnixFileInfoService(final CommandLineExecutor commandLineExecutor) {
        super(commandLineExecutor);
    }

    @Override
    public Collection<FileInfo> listFiles(final String directory, final boolean recursively)
        throws IOException {
        return listContent(directory, recursively, false);
    }

    @Override
    public Collection<FileInfo> listContent(final String directory, final boolean recursively)
        throws IOException {
        return listContent(directory, recursively, true);
    }

    protected Collection<FileInfo> listContent(final String directory, final boolean recursively,
        final boolean directories)
        throws IOException {
        final List<FileInfo> result = new LinkedList<FileInfo>();
        final String commandPattern;
        if (recursively) {
            commandPattern = "ls -R -l -A -b -q --time-style=+%%s --color=never %s";
        } else {
            commandPattern = "ls -l -A -b -q --time-style=+%%s --color=never %s";
        }

        final String command = StringUtils.format(commandPattern, directory);
        try {
            final Output executionOutput = exec(command);
            if (executionOutput.err.length() > 0) {
                throw new IOException(executionOutput.err);
            }
            final String output = executionOutput.out.trim();
            try (final Scanner scanner = new Scanner(output)) {

                String relativePathPrefix = "";
                while (scanner.hasNextLine()) {
                    final String line = scanner.nextLine();
                    if (line.isEmpty()) {
                        continue;
                    }
                    if (LS_TOTAL_PATTERN.matcher(line).matches()) {
                        continue;
                    }
                    final Matcher directoryMatcher = LS_DIRECTORY_PATTERN.matcher(line);
                    if (directoryMatcher.matches()) {
                        relativePathPrefix = directoryMatcher.group(1);
                        if (relativePathPrefix.equals(".")) {
                            relativePathPrefix = "";
                        }
                        relativePathPrefix = relativePathPrefix.replaceAll("\\./", "");
                        continue;
                    }
                    final FileInfo fileInfo = parseFileInfo(line, directory, relativePathPrefix);
                    // e.g. broken links result in null
                    if (fileInfo != null) {
                        // exclude directories if desired
                        if (!directories && fileInfo.isDirectory()) {
                            continue;
                        }
                        result.add(fileInfo);
                    }
                }
            }
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    private FileInfo parseFileInfo(final String line, final String path, final String directory) throws IOException {
        final Matcher matcher = LS_LINE_PATTERN.matcher(line);
        if (!matcher.matches()) {
            throw new IOException(StringUtils.format("LS line does not match pattern: %s", line));
        }
        // type of entry is first char {-, d, l}
        char type = matcher.group(1).charAt(0);
        // parse size
        long size;
        try {
            final String sizeString = matcher.group(4);
            size = Long.parseLong(sizeString);
        } catch (NumberFormatException e) {
            throw new IOException(e);
        }
        // parse modification date
        Date modificationDate = null;
        try {
            final String timestampString = matcher.group(5);
            final long timestamp = Long.parseLong(timestampString) * 1000L;
            modificationDate = new Date(timestamp);
        } catch (NumberFormatException e) {
            throw new IOException(e);
        }
        // parse entry name
        String name = matcher.group(6);
        // if the entry is a link the real type, size and modification time of
        // the linked file needs to be retrieved
        if (type == 'l') {
            // adjust the name to the part before the '-> <link target>'
            name = name.substring(0, name.indexOf("->") - 1);
            // resolve the real target path
            final String targetPath = resolveTarget(path + name);
            // if the link is broken return null
            if (targetPath == null) {
                return null;
            }
            if (isDirectory(targetPath)) {
                type = 'd';
            } else {
                type = '-';
                // only for files get their real size
                size = size(targetPath);
                // only for files get their modification time
                // TODO
            }
        }
        String relativePath;
        if (directory.isEmpty()) {
            relativePath = name;
        } else {
            relativePath = directory + '/' + name;
        }
        // directories are set to size 0
        if (type == 'd') {
            size = 0;
        }
        // remove multiple slashes in path
        relativePath = relativePath.replaceAll("/+", "/");
        final FileInfo fileInfo = new FileInfo(type == '-', relativePath, modificationDate, size);
        return fileInfo;
    }

    @Override
    public boolean isDirectory(final String path) throws IOException {
        final String commandPattern = "file %s";
        final String command = StringUtils.format(commandPattern, path);
        try {
            final Output executionOutput = exec(command);
            if (executionOutput.err.length() > 0) {
                throw new IOException(executionOutput.err);
            } else {
                final String output = executionOutput.out.trim();
                final Matcher matcher1 = FILE_CMD_DIRECTORY_PATTERN.matcher(output);
                if (matcher1.matches()) {
                    return true;
                } else {
                    final Matcher matcher2 = FILE_CMD_SYMLINK_PATTERN.matcher(output);
                    if (matcher2.matches()) {
                        return isDirectory(matcher2.group(2));
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
        return false;
    }

    private String resolveTarget(final String path) throws IOException {
        final String commandPattern = "file %s";
        final String command = StringUtils.format(commandPattern, path);
        try {
            final Output executionOutput = exec(command);
            if (executionOutput.err.length() > 0) {
                throw new IOException(executionOutput.err);
            } else {
                final String output = executionOutput.out.trim();
                Matcher matcher = FILE_CMD_SYMLINK_PATTERN.matcher(output);
                if (matcher.matches()) {
                    final String targetPath = matcher.group(2);
                    if (targetPath.startsWith("/")) {
                        return resolveTarget(targetPath);
                    } else {
                        final String absolutePath = path.substring(0, path.lastIndexOf('/') + 1) + targetPath;
                        return resolveTarget(absolutePath);
                    }
                }
                matcher = NON_EXISTING_PATTERN.matcher(output);
                if (matcher.matches()) {
                    return null;
                }
                return path;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    @Override
    public Long size(final String path) throws IOException {
        if (isDirectory(path)) {
            return 0L;
        }
        final String commandPattern = "du --bytes %s";
        final String command = StringUtils.format(commandPattern, path);
        try {
            final Output executionOutput = exec(command);
            if (executionOutput.err.length() > 0) {
                if (PERMISSION_DENIED_PATTERN.matcher(executionOutput.err.trim()).matches()) {
                    return null;
                }
                throw new IOException(executionOutput.err);
            } else {
                final String output = executionOutput.out.trim();
                final String[] sizeInfo = output.split("\\s+");
                return Long.parseLong(sizeInfo[0]);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    };

    private Output exec(final String commandString) throws IOException, InterruptedException {
        final CommandLineExecutor executor = getCommandLineExecutor();
        // set bash language
        executor.setEnv("LANG", "en_US.UTF8");
        InputStream stdout;
        InputStream stderr;
        executor.start(commandString);
        stdout = executor.getStdout();
        stderr = executor.getStderr();
        final CapturingTextOutReceiver outReceiver = new CapturingTextOutReceiver();
        final CapturingTextOutReceiver errReceiver = new CapturingTextOutReceiver();
        final TextStreamWatcher stdoutWatcher = TextStreamWatcherFactory.create(stdout, outReceiver);
        final TextStreamWatcher stderrWatcher = TextStreamWatcherFactory.create(stderr, errReceiver);
        stdoutWatcher.start();
        stderrWatcher.start();
        executor.waitForTermination();
        stdoutWatcher.waitForTermination();
        stderrWatcher.waitForTermination();
        final Output result = new Output();
        result.out = outReceiver.getBufferedOutput();
        result.err = errReceiver.getBufferedOutput();
        return result;
    }

    /**
     * Information of output type.
     * 
     * @author Christian Weiss
     */
    private static final class Output {

        private String out;

        private String err;
    }

}
