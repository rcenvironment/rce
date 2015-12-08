/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.scripting;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import de.rcenvironment.core.component.execution.api.ComponentLog;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.ConsoleRow.Type;
import de.rcenvironment.core.scripting.python.PythonOutputWriter;

/**
 * Implementation of {@link PythonOutputWriter}, which forwards output to the workflow console.
 * 
 * @author Doreen Seider
 */
public final class WorkflowConsoleForwardingWriter extends PythonOutputWriter {

    private static final String CONSOLE_ROW_TYPE_NOT_SUPPORTED = "Console row type not supported: ";

    private ComponentLog componentLog;

    private final Type consoleType;
    
    private final CountDownLatch printingLinesFinishedLatch = new CountDownLatch(1);

    public WorkflowConsoleForwardingWriter(Object lock, ComponentLog componentLog, ConsoleRow.Type type) {
        this(lock, componentLog, type, null);
    }

    public WorkflowConsoleForwardingWriter(Object lock, ComponentLog componentLog, ConsoleRow.Type consoleType, File logFile) {
        super(lock, logFile);
        this.componentLog = componentLog;
        if (consoleType != Type.TOOL_OUT && consoleType != Type.TOOL_ERROR) {
            throw new IllegalArgumentException(CONSOLE_ROW_TYPE_NOT_SUPPORTED + consoleType);
        }
        this.consoleType = consoleType;
    }

    @Override
    public void close() throws IOException {
        super.close();
        synchronized (lock) {
            // enqueues a task, which set the compInfo variable to null
            // doing it that way (and not setting the compInfo variable directly here to null), because that ensures that the compInfo
            // variable is set to null not before the last line was forwarded
            executionQueue.enqueue(new Runnable() {

                @Override
                public void run() {
                    // set to null as the WorkflowConsoleForwardingWriter instance are hold by the Jython sript engine
                    // for any length of time
                    componentLog = null;
                }
            });
        }
    }

    @Override
    protected void onNewLineToForward(String line) {
        if (line == null) {
            printingLinesFinishedLatch.countDown();
        } else {
            switch (consoleType) {
            case TOOL_OUT:
                componentLog.toolStdout(line);
                break;
            case TOOL_ERROR:
                componentLog.toolStderr(line);
                break;
            default:
                throw new IllegalArgumentException(CONSOLE_ROW_TYPE_NOT_SUPPORTED + consoleType);
            }
        }
    }
    
    /**
     * Awaits the writer be get closed.
     * 
     * @throws InterruptedException if wait is interrupted
     */
    public void awaitPrintingLinesFinished() throws InterruptedException {
        printingLinesFinishedLatch.await();
    }
    
}
