/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
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
import de.rcenvironment.core.monitoring.system.api.SystemMonitoringDataService;
import de.rcenvironment.core.monitoring.system.api.model.FullSystemAndProcessDataSnapshot;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.testing.CommonTestOptions;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.RunnablesGroup;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;
import de.rcenvironment.toolkit.modules.objectbindings.api.ObjectBindingsService;

/**
 * Unit test for {@link SystemMonitoringDataServiceImpl}.
 * 
 * @author David Scholz
 * @author Robert Mischke
 */
public class SystemMonitoringServiceImplTest {

    private static final Log LOGGER = LogFactory.getLog(SystemMonitoringDataServiceImpl.class);

    private static final long MAX_DELAY = 9000;

    /**
     * Expected exception for {@link OperatingSystemException}.
     */
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private SystemMonitoringDataServiceImpl systemDataService;

    private SystemMonitoringAggregationServiceImpl aggregationService;

    /**
     * Common setup.
     * 
     * @throws IOException on unexpected failure.
     * @throws OperatingSystemException on unexpected failure.
     */
    @Before
    public void setUp() throws IOException, OperatingSystemException {
        systemDataService = new SystemMonitoringDataServiceImpl();
        systemDataService.activate(EasyMock.createStrictMock(BundleContext.class));

        aggregationService = new SystemMonitoringAggregationServiceImpl();
        aggregationService.bindSystemMonitoringDataService(systemDataService);
        aggregationService.bindObjectBindingsService(EasyMock.createNiceMock(ObjectBindingsService.class));
        aggregationService.bindAsyncTaskService(EasyMock.createNiceMock(AsyncTaskService.class));
        aggregationService.activate(EasyMock.createStrictMock(BundleContext.class));
    }

    /**
     * Common cleanup.
     */
    @After
    public void tearDown() {

    }

    /**
     * 
     * Test if service returns a faultless {@link FullSystemAndProcessDataSnapshot}.
     * 
     * @throws OperatingSystemException on unexpected test failure.
     * 
     */
    @Test
    public void testMonitoringDataModelContent() throws OperatingSystemException {
        FullSystemAndProcessDataSnapshot model = aggregationService.getCompleteSnapshot();
        boolean success =
            model != null && model.getNodeSystemRAM() != 0 && model.getRceProcessesInfo() != null
                && model.getRceSubProcesses() != null;
        assertTrue(success);
    }

    /**
     * 
     * Test the caching mechanism in {@link SystemMonitoringDataServiceImpl}.
     * 
     * @throws ExecutionException on execution failure.
     * @throws InterruptedException on thread interruption.
     * 
     */
    @Test
    public void testCachingMechanism() throws InterruptedException, ExecutionException {

        aggregationService.clearFullSnapshotCache();

        final AsyncTaskService asyncTaskService = ConcurrencyUtils.getAsyncTaskService();

        Future<FullSystemAndProcessDataSnapshot> firstCall =
            asyncTaskService.submit(new Callable<FullSystemAndProcessDataSnapshot>() {

                @Override
                @TaskDescription("Fetching monitoring data model...")
                public FullSystemAndProcessDataSnapshot call() throws Exception {
                    return aggregationService.getCompleteSnapshot();
                }

            });

        Future<FullSystemAndProcessDataSnapshot> secCall = asyncTaskService.submit(new Callable<FullSystemAndProcessDataSnapshot>() {

            @Override
            @TaskDescription("Fetching monitoring data model...")
            public FullSystemAndProcessDataSnapshot call() throws Exception {
                return aggregationService.getCompleteSnapshot();
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
        final String processMarkerString = startDummyProcess();
        final RunnablesGroup group = ConcurrencyUtils.getFactory().createRunnablesGroup();

        final int threadCount = 16;
        for (int i = 0; i <= threadCount; i++) {
            final Runnable killTask = new Runnable() {

                @Override
                @TaskDescription("Killing test process...")
                public void run() {
                    killDummyProcess(processMarkerString, true);
                }
            };
            group.add(killTask);
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
        assertTrue(systemDataService.getChildProcessesAndIds(null).isEmpty());
        exception.expect(OperatingSystemException.class);
        exception.expectMessage(OperatingSystemException.ErrorType.NO_SUCH_PROCESS.getMessage());
        systemDataService.getProcessRAMUsage(null);
        systemDataService.getProcessCPUUsage(null);
    }

    /**
     * Test the two different kill signums of {@link SystemMonitoringDataServiceImpl#kill(Long, Boolean)}.
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
     * Test if CPU used and idle values are valid percentages, represented as 0..1.
     * 
     * @throws OperatingSystemException on unexpected failure.
     */
    @Test
    public void testCpuValuesAreValidOnSingleFetch() throws OperatingSystemException {
        double totalCpu = systemDataService.getTotalCPUUsage();
        assertIsValidZeroToOneValue(totalCpu);
        double idle = systemDataService.getReportedCPUIdle();
        assertIsValidZeroToOneValue(idle);
    }

    /**
     * Verifies that CPU load values are never NaN, even if polled rapidly.
     * <p>
     * Test background: The SIGAR/Humidor libraries seem to return NaN when CPU usage is polled in rapid succession. To ensure that
     * application code always sees valid data, the {@link SystemMonitoringDataService#getTotalCPUUsage()} method should retry/wait
     * accordingly, which is verified by this test case.
     * 
     * @throws OperatingSystemException on unexpected failure.
     */
    @Test
    public void testCpuValuesAreValidOnFrequentPolling() throws OperatingSystemException {
        final int n = CommonTestOptions.selectStandardOrExtendedValue(100, 1000);
        for (int i = 0; i < n; i++) {
            final double totalCpu = systemDataService.getTotalCPUUsage();
            assertIsValidZeroToOneValue(totalCpu);
        }
    }

    /**
     * Test total ram usage with some tolerance.
     * 
     * TODO review; what exactly is compared against each other here?
     * 
     * @throws OperatingSystemException on unexpected failure.
     */
    @Test
    public void testRamGathering() throws OperatingSystemException {
        long ram = systemDataService.getTotalSystemRAM();
        long totalUsedRam = aggregationService.getCompleteSnapshot().getNodeRAMUsage();
        double usedRamSigar = systemDataService.getTotalUsedRAMPercentage() * ram;
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
        final int loop = CommonTestOptions.selectStandardOrExtendedValue(10, 100);
        for (int i = 0; i < loop; i++) {
            Map<Long, String> map = systemDataService.getProcesses();
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

    private void assertIsValidZeroToOneValue(double value) {
        assertFalse(Double.isNaN(value));
        assertTrue(value >= 0.0);
        assertTrue(value <= 1.0);
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

    private void killDummyProcess(String processMarkerString, boolean force) {
        Map<Long, String> processes = null;
        try {
            processes = systemDataService.getProcesses();
        } catch (OperatingSystemException e1) {
            LOGGER.error(e1);
        }

        if (processes != null) {
            for (Map.Entry<Long, String> process : processes.entrySet()) {
                if (process.getValue().contains(processMarkerString)) {
                    LOGGER.info("Found " + processMarkerString + " with id [" + process.getKey() + "] - killing it");
                    try {
                        systemDataService.kill(process.getKey(), force);
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
