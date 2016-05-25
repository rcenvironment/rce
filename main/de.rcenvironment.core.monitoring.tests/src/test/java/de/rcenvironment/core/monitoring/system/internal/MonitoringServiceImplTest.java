/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.monitoring.system.api.OperatingSystemException;
import de.rcenvironment.core.monitoring.system.api.SystemMonitoringDataSnapshot;
import de.rcenvironment.core.utils.common.concurrent.RunnablesGroup;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;

/**
 * 
 * JUnitTest for {@link SystemMonitoringServiceImpl}.
 * 
 * @author David Scholz
 */
public class MonitoringServiceImplTest {

    private static final Log LOGGER = LogFactory.getLog(SystemMonitoringServiceImpl.class);

    private static final long MAX_DELAY = 9000;

    /**
     * Expected exception for {@link OperatingSystemException}.
     */
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private SystemMonitoringServiceImpl monitoringService;

    /**
     * Common setup.
     * 
     * @throws IOException on unexpected failure.
     * @throws OperatingSystemException on unexpected failure.
     */
    @Before
    public void setUp() throws IOException, OperatingSystemException {
        monitoringService = new SystemMonitoringServiceImpl();
        monitoringService.activate(EasyMock.createStrictMock(BundleContext.class));
    }

    /**
     * Common cleanup.
     */
    @After
    public void tearDown() {

    }

    /**
     * 
     * Test if service returns a faultless {@link SystemMonitoringDataSnapshot}.
     * 
     * @throws OperatingSystemException on unexpected test failure.
     * 
     */
    @Test
    public void testMonitoringDataModelContent() throws OperatingSystemException {
        SystemMonitoringDataSnapshot model = monitoringService.getCompleteSnapshot();
        boolean success =
            model != null && model.getNodeSystemRAM() != 0 && model.getRceProcessesInfo() != null
                && model.getRceSubProcesses() != null;
        assertTrue(success);
    }

    /**
     * 
     * Test the caching mechanism in {@link SystemMonitoringServiceImpl}.
     * 
     * @throws ExecutionException on execution failure.
     * @throws InterruptedException on thread interruption.
     * 
     */
    @Test
    public void testCachingMechanism() throws InterruptedException, ExecutionException {

        monitoringService.clearCache();

        Future<SystemMonitoringDataSnapshot> firstCall =
            SharedThreadPool.getInstance().submit(new Callable<SystemMonitoringDataSnapshot>() {

                @Override
                @TaskDescription("Fetching monitoring data model...")
                public SystemMonitoringDataSnapshot call() throws Exception {
                    return monitoringService.getCompleteSnapshot();
                }

            });

        Future<SystemMonitoringDataSnapshot> secCall = SharedThreadPool.getInstance().submit(new Callable<SystemMonitoringDataSnapshot>() {

            @Override
            @TaskDescription("Fetching monitoring data model...")
            public SystemMonitoringDataSnapshot call() throws Exception {
                return monitoringService.getCompleteSnapshot();
            }

        });

        try {
            assertEquals(firstCall.get(MAX_DELAY, TimeUnit.MILLISECONDS), secCall.get(MAX_DELAY, TimeUnit.MILLISECONDS));
        } catch (TimeoutException e) {
            fail("Timed out!");
        }
    }

    /**
     * Test thread safety of kill method.
     * 
     * @throws ExecuteException if process execution fails.
     * @throws IOException if opening file fails.
     */
    @Test
    public void testKillMethodThreadSafety() throws ExecuteException, IOException {
        List<Runnable> runnableList = new ArrayList<>();
        final String epicDummyString = startDummyProcess();
        final int threadNumber = 16;
        for (int i = 0; i <= threadNumber; i++) {
            Runnable killer = new Runnable() {

                @Override
                @TaskDescription("Killing test process...")
                public void run() {
                    killDummyProcess(epicDummyString, true);
                }
            };
            runnableList.add(killer);
        }

        RunnablesGroup group = SharedThreadPool.getInstance().createRunnablesGroup();
        for (Runnable killer : runnableList) {
            group.add(killer);
        }
        group.executeParallel();
    }

    /**
     * 
     * Test for null values.
     * 
     * @throws OperatingSystemException on unexpected failure.
     * 
     */
    @Test
    public void testNullValues() throws OperatingSystemException {
        assertTrue(monitoringService.getChildProcesses(null).isEmpty());
        exception.expect(OperatingSystemException.class);
        exception.expectMessage(OperatingSystemException.ErrorType.NO_SUCH_PROCESS.getMessage());
        monitoringService.getProcessRAMUsage(null);
        monitoringService.getProcessCPUUsage(null);
    }

    /**
     * Test the two different kill signums of {@link SystemMonitoringServiceImpl#kill(Long, Boolean)}.
     * 
     * @throws IOException on unexpected failure.
     * @throws ExecuteException on unexpected failure.
     */
    @Test
    public void testKillSignums() throws ExecuteException, IOException {
        String process = startDummyProcess();
        killDummyProcess(process, true);
        process = startDummyProcess();
        killDummyProcess(process, false);
    }

    /**
     * Test if cpu are values are greater than 100%.
     * 
     * @throws OperatingSystemException on unexpected failure.
     */
    @Test
    public void testCpuValues() throws OperatingSystemException {
        double totalCpu = monitoringService.getTotalCPUUsage();
        double idle = monitoringService.getIdle();
        assertTrue(totalCpu <= 1.0 && !(totalCpu < 0));
        assertTrue(idle <= 1.0 && !(idle < 0));
    }

    /**
     * 
     * Test total ram usage with some tolerance.
     * 
     * @throws OperatingSystemException on unexpected failure.
     */
    @Test
    public void testRamGathering() throws OperatingSystemException {
        long ram = monitoringService.getTotalRAM();
        long totalUsedRam = monitoringService.getCompleteSnapshot().getNodeRAMUsage();
        double usedRamSigar = monitoringService.getTotalUsedRAMPercentage() * ram;
        double usedRamModel = totalUsedRam;
        final float tolerance = 0.5f;

        boolean equal = Math.abs(usedRamSigar / usedRamModel - 1.0f) <= tolerance;
        assertTrue(equal);
    }

    /**
     * 
     * Test process gathering over time.
     * 
     * @throws OperatingSystemException on unexpected failure.
     * 
     */
    @Test
    public void testProcessGatheringOverTime() throws OperatingSystemException {
        final int loop = 100;
        for (int i = 0; i < loop; i++) {
            Map<Long, String> map = monitoringService.getProcesses();
            boolean success = !map.isEmpty() && map != null;
            assertTrue(success);
        }
    }

    /**
     * 
     * Operating system.
     * 
     * @author David Scholz
     */
    public enum OSType {
        /**
         * Operating system.
         */
        Windows, MacOS, Linux, Other
    };

    private OSType getOperatingSystemType() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("linux")) {
            return OSType.Linux;
        } else if (os.contains("windows")) {
            return OSType.Windows;
        } else if (os.contains("mac os") || os.contains("macos") || os.contains("darwin")) {
            return OSType.MacOS;
        } else {
            return OSType.Other;
        }
    }

    private String startDummyProcess() throws ExecuteException, IOException {
        String line = "";
        CommandLine commandLine;
        DefaultExecutor executor = new DefaultExecutor();
        DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
        switch (getOperatingSystemType()) {
        case Windows:
            line = "ping -n 1000 127.0.0.1";
            commandLine = CommandLine.parse(line);
            executor.execute(commandLine, handler);
            executor.setStreamHandler(new PumpStreamHandler(null, null, null));
            line = "PING";
            break;
        case Linux:
            line = "ping -n 1000 127.0.0.1";
            commandLine = CommandLine.parse(line);
            executor.setStreamHandler(new PumpStreamHandler(null, null, null));
            executor.execute(commandLine, handler);
            line = "ping";
            break;
        case MacOS:
            Desktop.getDesktop().open(new File("Terminal.app"));
            break;
        default:
            LOGGER.error("Failed to determine operating system.");
            break;
        }
        return line;
    }

    private void killDummyProcess(String epicDummyString, boolean force) {
        Map<Long, String> processes = null;
        try {
            processes = monitoringService.getProcesses();
        } catch (OperatingSystemException e1) {
            LOGGER.error(e1);
        }

        if (processes != null) {
            for (Map.Entry<Long, String> process : processes.entrySet()) {
                if (process.getValue().contains(epicDummyString)) {
                    LOGGER.info("Found " + epicDummyString + " with id [" + process.getKey() + "] - KILLING IT!");
                    try {
                        monitoringService.kill(process.getKey(), force);
                    } catch (OperatingSystemException e) {
                        if (e.getErrorType().equals(OperatingSystemException.ErrorType.ACCESS_DENIED)) {
                            continue;
                        }
                    }
                }
            }
        }
    }

}
