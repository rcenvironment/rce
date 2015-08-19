/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.dlr.sc.chameleon.rce.toolwrapper.component;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.OS;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;

import de.dlr.sc.chameleon.rce.toolwrapper.common.CpacsWrapperInfo;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.cpacs.utils.common.components.ConsoleLogger;

/**
 * This class represents a simulation tool with CPACS support.
 * 
 * @author Markus Kunde
 */
public class CpacsTool {

    /** Default exit code for executing on command line. */
    public static final int DEFAULT_EXITCODE = 0;

    /** Separator for temp directory. */
    private static final String TEMPDIR_SEPARATOR = "_";

    /** Beginning command for linux shell. */
    private static final String LINUX_SHELL_COMMAND = "/bin/sh";

    /** Appendix command for linux shell. */
    private static final String LINUX_SHELL_APPENDIX = "-c";

    /** Beginning command for windows shell. */
    private static final String WINDOWS_SHELL_COMMAND = "cmd.exe /C ";

    /**
     * Concrete configuration read from Cpacs wrapper configuration file. Tool configuration without Cpacs behavior.
     */
    protected CpacsWrapperInfo toolConfiguration;

    /**
     * Concrete configuration (behavior-sensitive content, e. g., CPACSInitial.xml, etc.) of tool in the Cpacs wrapper context.
     */
    protected CpacsToolConfiguration toolCPACSBehaviorConfiguration;

    /** Mapper for XML mapping. */
    private Mapper mapper;

    /** Wrapper and helper for sending lines to rce console logger. */
    private ConsoleLogger consoleLoggerOut;

    /** Wrapper and helper for sending lines to rce console logger. */
    private ConsoleLogger consoleLoggerErr;

    /**
     * Constructor.
     * 
     * @param toolConfiguration cpacsWrapper configuration file.
     */
    public CpacsTool(final CpacsWrapperInfo configurationInfo, ConsoleLogger loggerOut, ConsoleLogger loggerErr) {
        toolConfiguration = configurationInfo;
        consoleLoggerOut = loggerOut;
        consoleLoggerErr = loggerErr;

        mapper = new Mapper(this);

        // create tempDirectory path
        UUID uuid = UUID.randomUUID();

        toolConfiguration.setTempdirectory(toolConfiguration.getTempdirectory()
            + File.separator + System.currentTimeMillis()
            + TEMPDIR_SEPARATOR
            + uuid.toString());

        toolCPACSBehaviorConfiguration =
            new CpacsToolConfiguration(toolConfiguration.getTempdirectory(), toolConfiguration.getToolDirectory());

        if (toolConfiguration == null || toolCPACSBehaviorConfiguration == null) {
            throw new IllegalArgumentException("configuration object(s) should not be null.");
        }
    }

    /**
     * Name of tool.
     * 
     * @return name of tool as String representation.
     */
    public String getName() {
        return toolConfiguration.getToolName();
    }

    /**
     * Description of tool.
     * 
     * @return description of tool as String representation.
     */
    public String getDescription() {
        return toolConfiguration.getToolDescription();
    }

    /**
     * Version of tool.
     * 
     * @return version of tool as String representation.
     */
    public String getVersion() {
        return toolConfiguration.getToolVersion();
    }

    /**
     * Path to icon16 file.
     * 
     * @return path to icon16 file as File representation.
     */
    public File getIcon16() {
        return new File(toolConfiguration.getToolIcon16());
    }

    /**
     * Path to icon32 file.
     * 
     * @return path to icon32 file as File representation.
     */
    public File getIcon32() {
        return new File(toolConfiguration.getToolIcon32());
    }

    /**
     * Copies the tool recursively to temp directory and makes all files executable.
     * 
     * @throws IOException when error occurs during copy
     */
    public void copyToTempDir() throws IOException {
        File tempDir = new File(toolConfiguration.getTempdirectory());
        FileUtils.copyDirectory(new File(toolConfiguration.getToolDirectory()), tempDir,
            toolConfiguration.getIgnoredFiles(), true);
        Iterator<File> it = FileUtils.iterateFiles(tempDir, null, true);
        while (it.hasNext()) {
            File f = it.next();
            if (f.isFile()) {
                f.setExecutable(true, false);
            }
        }
    }

    /**
     * Deletes temp directory recursively.
     * 
     * @throws IOException when error occurs during copy
     */
    public void deleteTempDir() throws IOException {
        FileUtils.deleteDirectory(new File(toolConfiguration.getTempdirectory()));
    }

    /**
     * Executes the pre-command on commandline.
     * 
     * @return exit code of execution
     * @throws ExecuteException when error occurs during execution regarding execution
     * @throws IOException when error occurs during execution regarding file io
     */
    public int executePreCommand() throws ExecuteException, IOException {
        return executeCommandLine(toolConfiguration.getToolPrecommand());
    }

    /**
     * Executes the main-command on commandline.
     * 
     * @return exit code of execution
     * @throws ExecuteException when error occurs during execution regarding execution
     * @throws IOException when error occurs during execution regarding file io
     */
    public int executeMainCommand() throws ExecuteException, IOException {
        return executeCommandLine(toolConfiguration.getToolCommandline());
    }

    /**
     * Executes the post-command on commandline.
     * 
     * @return exit code of execution
     * @throws ExecuteException when error occurs during execution regarding execution
     * @throws IOException when error occurs during execution regarding file io
     */
    public int executePostCommand() throws ExecuteException, IOException {
        return executeCommandLine(toolConfiguration.getToolPostcommand());
    }

    /**
     * Cpacs tool configuration in a cpacs context.
     * 
     * @return cpacs tool configuration
     */
    public final CpacsToolConfiguration getCpacsToolConfiguration() {
        return toolCPACSBehaviorConfiguration;
    }

    /**
     * Executes a commandline command.
     * 
     * @param command command line
     * @return exit code of execution
     * @throws ExecuteException if execution error occurs
     * @throws IOException if IO exception occurs
     */
    private int executeCommandLine(final String command) throws ExecuteException, IOException {
        DefaultExecutor executor = new DefaultExecutor();
        executor.setWorkingDirectory(new File(toolConfiguration.getTempdirectory()));
        executor.setExitValue(DEFAULT_EXITCODE);

        PumpStreamHandler streamHandler = new PumpStreamHandler(consoleLoggerOut, consoleLoggerErr);
        executor.setStreamHandler(streamHandler);

        CommandLine cmd;
        if (OS.isFamilyWindows()) {
            cmd = CommandLine.parse(WINDOWS_SHELL_COMMAND + command);
        } else {
            cmd = new CommandLine(LINUX_SHELL_COMMAND);
            cmd.addArgument(LINUX_SHELL_APPENDIX);
            cmd.addArgument(command, false);
        }
        int exitValue = executor.execute(cmd);

        return exitValue;
    }

    /**
     * Mapping tool output side.
     * 
     * @throws XPathExpressionException
     * 
     * @throws ComponentException if mapping fails.
     * 
     */
    public void mappingOutput() throws ComponentException {
        mapper.mappingOutput();
    }

    /**
     * Mapping tool input side.
     * 
     * @throws ComponentException if mapping fails
     * 
     */
    public void mappingInput() throws ComponentException {
        mapper.mappingInput();
    }

}
