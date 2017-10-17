/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.executor;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.PumpStreamHandler;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Utility class to interact with processes in a system dependent way.
 *
 * @author Tobias Rodehutskors
 */
final class ProcessUtils {

    public static final int LINUX_EXIT_CODE_SUCCESS = 0;

    public static final int LINUX_EXIT_CODE_SIGTERM = 143;

    public static final int WINDOWS_EXIT_CODE_SUCCESS = 0;

    public static final int WINDOWS_EXIT_CODE_SIGTERM = 1;

    /**
     * Will be returned if the process is already terminated.
     */
    public static final int WINDOWS_EXIT_CODE_WAIT_NO_CHILDREN = 128;

    /**
     * Will be returned if taskkill returns "there is no running instance of the task.".
     */
    public static final int WINDOWS_EXIT_CODE_EA_LIST_INCONSISTENT = 255;

    private static final int TEN_SECONDS_IN_MILLIS = 10000;

    /** Top-level command token template for Windows invocation. */
    private static final String[] WINDOWS_SHELL_TOKENS = { "cmd.exe", "/c", "[command]" };

    /**
     * Top-level command token template for Linux invocation.
     *
     * Calls setsid to set a unique session id. This session id is used to identify all descendants of this process which may be spawned
     * during the process execution.
     */
    private static final String[] LINUX_SHELL_TOKENS = { "setsid", "/bin/sh", "-c", "[command]" };

    private static final String WINDOWS_KILL_COMMAND_TEMPLATE = "Taskkill /T /F /PID %d";

    private static final String LINUX_TERM_COMMAND_TEMPLATE = "for pid in $(ps -s %d -o pid=); do kill ${pid}; done";

    private static final String LINUX_KILL_COMMAND_TEMPLATE = "for pid in $(ps -s %d -o pid=); do kill -9 ${pid}; done";

    /**
     * Lists the PIDs of all processes with the SID %d.
     */
    private static final String LINUX_CHECK_COMMAND_TEMPLATE = "ps -s %d -o pid=";

    private static final int LINUX_TERM_ATTEMPTS = 3;

    private static final int LINUX_KILL_ATTEMPTS = 3;

    private static final String UNKOWN_PLATFORM_ERROR = "Unkown platform. Currently only Windows and Linux are supported.";

    private ProcessUtils() {}

    /**
     * This interface is needed for JNA to get access to the native windows api.
     */
    interface Kernel32 extends Library {

        Kernel32 INSTANCE = (Kernel32) Native.loadLibrary("kernel32", Kernel32.class);

        /**
         * Retrieves the process identifier of the specified process.
         */

        // CHECKSTYLE:DISABLE (MethodName) - method name needs to be identical to the Kernel32 method name
        int GetProcessId(Long handle);
        // CHECKSTYLE:ENABLE (MethodName)
    }

    static CommandLine constructCommandLine(String command) {

        String[] commandTokens;

        if (Platform.isWindows()) {
            commandTokens = Arrays.copyOf(ProcessUtils.WINDOWS_SHELL_TOKENS, ProcessUtils.WINDOWS_SHELL_TOKENS.length);
            commandTokens[ProcessUtils.WINDOWS_SHELL_TOKENS.length - 1] = command;
        } else if (Platform.isLinux()) {
            commandTokens = Arrays.copyOf(ProcessUtils.LINUX_SHELL_TOKENS, ProcessUtils.LINUX_SHELL_TOKENS.length);
            commandTokens[ProcessUtils.LINUX_SHELL_TOKENS.length - 1] = command;
        } else {
            throw new IllegalStateException(UNKOWN_PLATFORM_ERROR);
        }

        CommandLine cmd = new CommandLine(commandTokens[0]);
        for (int i = 1; i < commandTokens.length; i++) {
            cmd.addArgument(commandTokens[i], false);
        }

        return cmd;
    }

    /**
     * Extracts the system dependent process id from {@link Process}. This method is implemented using Reflection API and JNA and is
     * therefore naturally platform dependent. It is possible to extract the PID from already finished processes for both Windows as well as
     * Linux.
     */
    static int getPid(Process pProcess)
        throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

        int pid;

        if (Platform.isWindows()) {
            // extract the handle from the process object using the reflection API
            Field f = pProcess.getClass().getDeclaredField("handle");
            f.setAccessible(true);
            long processHandle = f.getLong(pProcess);

            // get the process id from the process handle using JNA
            pid = Kernel32.INSTANCE.GetProcessId(processHandle);

        } else if (Platform.isLinux()) {

            // extract the process id from the process object using the reflection API
            Field f = pProcess.getClass().getDeclaredField("pid");
            f.setAccessible(true);
            pid = f.getInt(pProcess);

        } else {
            throw new IllegalStateException(UNKOWN_PLATFORM_ERROR);
        }

        return pid;
    }

    /**
     * Kills the process tree starting with the given process id.
     * 
     * Precondition: For Linux this method assumes that the given process id is equal to a session id and kills all processes with this
     * session id. Therefore, the process needs to be started with the setsid prefix beforehand.
     * 
     * TODO JAVA9 Presumably Java 9 is going to introduce a new method {@link java.lang.Process#toHandle()} which will return a
     * ProcessHandle object. This handles will allow to query the descendants of a process and kill them.
     *
     *
     * @return true, if the process and its descendants have been killed or if the process already finished
     * @exception ExecuteException Thrown if the process id/session id was not found or the command failed for another reason.
     * @throws InterruptedException
     */
    static boolean killProcessTree(int pid) throws ExecuteException, IOException, InterruptedException {

        DefaultExecutor executor = new DefaultExecutor();

        if (Platform.isWindows()) {

            int[] validExitValues =
            { WINDOWS_EXIT_CODE_SUCCESS, WINDOWS_EXIT_CODE_WAIT_NO_CHILDREN, WINDOWS_EXIT_CODE_EA_LIST_INCONSISTENT };
            executor.setExitValues(validExitValues);
            executor.setStreamHandler(new PumpStreamHandler(null));

            CommandLine cl = ProcessUtils.constructCommandLine(StringUtils.format(WINDOWS_KILL_COMMAND_TEMPLATE, pid));

            // check if the returned exit code signals a success
            int exitCode = executor.execute(cl);
            for (int validExitValue : validExitValues) {
                if (exitCode == validExitValue) {
                    return true;
                }
            }

            return false;
        } else if (Platform.isLinux()) {

            // try to terminate the processes first
            boolean terminated = termOrKill(executor, LINUX_TERM_ATTEMPTS, LINUX_TERM_COMMAND_TEMPLATE, pid);

            if (terminated) {
                return true;
            }

            // wait for some seconds
            Thread.sleep(TEN_SECONDS_IN_MILLIS);

            // kill the remaining processes
            return termOrKill(executor, LINUX_KILL_ATTEMPTS, LINUX_KILL_COMMAND_TEMPLATE, pid);
        } else {
            throw new IllegalStateException(UNKOWN_PLATFORM_ERROR);
        }
    }

    // Linux only function
    private static boolean termOrKill(DefaultExecutor executor, int attempts, String terminationCommand, int pid)
        throws ExecuteException, IOException {

        // as child processes can be created asynchronously, we check if we succeeded in terminating them and repeat the termination several
        // times if necessary
        for (int i = 0; i < attempts; i++) {
            // terminate all commands with the given SID
            CommandLine terminate = ProcessUtils.constructCommandLine(StringUtils.format(terminationCommand, pid));
            // returns 0 on success
            // returns 1:
            // since the kill command is not atomic, it can happen that "$(ps -s %d -o pid=)" returns some process ids, whose corresponding
            // processes finish, before the loop can issue the kill command for this process. In this case the script will return 1.
            executor.setExitValues(new int[] { 0, 1 });
            executor.execute(terminate);

            // check if there is any process left
            CommandLine check = ProcessUtils.constructCommandLine(StringUtils.format(LINUX_CHECK_COMMAND_TEMPLATE, pid));
            // 0: found some processes for the given SID
            // 1: found no processes
            executor.setExitValues(new int[] { 0, 1 });
            int checkExitCode = executor.execute(check);

            // exit if all processes died
            if (checkExitCode == 1) {
                return true;
            }
        }

        return false;
    }
}
