/*
 * Copyright 2020-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.monitoring.system.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.monitoring.system.api.model.FullSystemAndProcessDataSnapshot;
import de.rcenvironment.core.monitoring.system.api.model.ProcessInformation;
import de.rcenvironment.core.monitoring.system.api.model.SystemLoadInformation;
import oshi.software.os.OSProcess;

public class SystemMonitoringDataProcessorTest {

    // values of the second system data snapshot for multiple tests
    private static final long FAKE_TOTAL_RAM = 10000L;

    private static final long FAKE_RAM_USAGE = 5000L;

    private static double FAKE_CPU_USAGE = 0.7D;

    private static double FAKE_CPU_USAGE_1 = 0.8D;

    /**
     * Testing that a full system snapshot is never null.
     */
    @Test
    public void testDefaultFullSystemSnapshot() {
        SystemMonitoringDataProcessor dataProcessor = createDataProcesserWithNoInputData();
        FullSystemAndProcessDataSnapshot fullSystemSnapshot = dataProcessor.createFullSystemSnapshot();
        assertNotNull("FullSystemSnapshot is null", fullSystemSnapshot);
    }

    /**
     * Testing that system load information are never null.
     */
    @Test
    public void testDefaultLoadInformation() {
        SystemMonitoringDataProcessor dataProcessor = createDataProcesserWithNoInputData();
        SystemLoadInformation loadInformation = dataProcessor.getSystemLoadInformation(2);
        assertNotNull("SystemLoadInformation is null", loadInformation);
    }

    /**
     * Testing the caching mechanism for fullSystemSnapshots.
     */
    @Test
    public void testCachingOfFullSystemSnapshot() {
        SystemMonitoringDataProcessor dataProcessor = createDataProcessorWithTwoElements();
        FullSystemAndProcessDataSnapshot fullSystemSnapshot1 = dataProcessor.createFullSystemSnapshot();
        FullSystemAndProcessDataSnapshot fullSystemSnapshot2 = dataProcessor.createFullSystemSnapshot();
        // comparing object IDs to test for identity
        assertTrue("Object ids are not identical", fullSystemSnapshot1 == fullSystemSnapshot2);

    }

    @Test
    public void testCalculationOfSystemLoadInformation() {
        SystemMonitoringDataProcessor dataProcessor = createDataProcessorWithTwoElements();
        SystemLoadInformation loadInformation = dataProcessor.getSystemLoadInformation(2);

        assertTrue("RAM usage calculation is wrong", Long.compare(loadInformation.getAvailableRam(), FAKE_TOTAL_RAM - FAKE_RAM_USAGE) == 0);
        assertTrue("CPU usage calculation is wrong", Double.compare(loadInformation.getCpuLoad(), FAKE_CPU_USAGE) == 0);
        assertTrue("Average CPU usage calculation is wrong",
            Double.compare(loadInformation.getCpuLoadAvg().getAverage(), ((FAKE_CPU_USAGE + FAKE_CPU_USAGE_1) / 2)) == 0);
    }

    /**
     * Testing the aggregation of multiple {@link SystemDataSnapshot}s into one {@link FullSystemAndProcessDataSnapshot}.
     */
    @Test
    public void testMonitoringDataModelContent() {
        SystemMonitoringDataProcessor dataProcessor = createDataProcessorWithTwoElements();
        FullSystemAndProcessDataSnapshot model = dataProcessor.createFullSystemSnapshot();

        // model should never be null
        assertNotNull("FullSystemDataSnapshot is null", model);
        assertNotNull("No RCE process info available", model.getRceProcessesInfo());
        assertNotNull("No RCE subprocess info available", model.getRceSubProcesses());
        assertFalse("RCE own process list is empty", model.getRceProcessesInfo().isEmpty());
        assertEquals("Total RAM is wrong", model.getNodeSystemRAM(), FAKE_TOTAL_RAM);
        // ram and cpu usage should be equal to the values of the latest systemDataSnapshot
        assertTrue("RAM usage calculation is wrong", Long.compare(model.getNodeRAMUsage(), FAKE_RAM_USAGE) == 0);
        assertTrue("CPU usage calculation is wrong", Double.compare(model.getNodeCPUusage(), FAKE_CPU_USAGE) == 0);
        // this list should be empty, but if it isn't, render the list as the assertion error message
        String subProcessesInfoString = Arrays.toString(model.getRceSubProcesses().toArray());
        assertTrue(subProcessesInfoString, model.getRceSubProcesses().isEmpty()); // should be actually empty
    }

    /**
     * Testing the correct handling of multiple hierarchically structured subprocesses.
     */
    @Test
    public void testSubProcessHandling() {
        SystemMonitoringDataProcessor dataProcessor = createDataProcessorForSubProcessTest();
        FullSystemAndProcessDataSnapshot model = dataProcessor.createFullSystemSnapshot();

        List<ProcessInformation> rceProcesses = model.getRceProcessesInfo();
        assertTrue("Multiple or non rce root process(es)", rceProcesses.size() == 1); // only 1 main process
        assertEquals("Rce is not root process", rceProcesses.get(0).getName(), "rce");
        List<ProcessInformation> subProcesses = model.getRceSubProcesses();
        assertTrue("Wrong number of child processes on second level", subProcesses.size() == 2); // 2 direct children
        List<ProcessInformation> thirdLevel = subProcesses.stream()
            .flatMap(processInformation -> processInformation.getChildren().stream())
            .collect(Collectors.toList());
        assertTrue("Wrong number of child processes on third level", thirdLevel.size() == 1); // third level only has 1 child
        ProcessInformation child3 = thirdLevel.get(0);
        assertEquals("Wrong child process on third level", child3.getName(), "child3");
    }

    private SystemMonitoringDataProcessor createDataProcesserWithNoInputData() {
        SystemIntegrationAdapter adapter = EasyMock.createNiceMock(SystemIntegrationAdapter.class);
        EasyMock.replay(adapter);
        // inserting empty ring buffer
        RingBuffer<SystemDataSnapshot> ringBuffer = new RingBuffer<>(0);
        return new SystemMonitoringDataProcessor(ringBuffer, adapter);
    }

    private SystemMonitoringDataProcessor createDataProcessorWithTwoElements() {
        // the data processor only uses the total ram from the adapter
        SystemIntegrationAdapter adapter = EasyMock.createNiceMock(SystemIntegrationAdapter.class);
        EasyMock.expect(adapter.getTotalRam()).andStubReturn(FAKE_TOTAL_RAM);
        EasyMock.replay(adapter);

        RingBuffer<SystemDataSnapshot> ringBuffer = new RingBuffer<>(2);

        OSProcess rceProcess = EasyMock.createNiceMock(OSProcess.class);
        EasyMock.expect(rceProcess.getProcessID()).andStubReturn(1);
        EasyMock.replay(rceProcess);
        SystemDataSnapshot snapshot1 =
            new SystemDataSnapshot(0, null, FAKE_CPU_USAGE_1, 6000, rceProcess, new HashSet<OSProcess>());

        OSProcess rceProcess2 = EasyMock.createNiceMock(OSProcess.class);
        EasyMock.expect(rceProcess2.getProcessID()).andStubReturn(1);
        EasyMock.replay(rceProcess2);
        // using the fake values for the second / latest systemDataSnapshot
        SystemDataSnapshot snapshot2 =
            new SystemDataSnapshot(0, null, FAKE_CPU_USAGE, FAKE_RAM_USAGE, rceProcess2,
                new HashSet<OSProcess>());

        ringBuffer.add(snapshot1);
        ringBuffer.add(snapshot2);
        return new SystemMonitoringDataProcessor(ringBuffer, adapter);

    }

    private SystemMonitoringDataProcessor createDataProcessorForSubProcessTest() {
        SystemIntegrationAdapter adapter = EasyMock.createNiceMock(SystemIntegrationAdapter.class);
        EasyMock.replay(adapter);

        // fake rceProcess
        OSProcess rceProcess = EasyMock.createNiceMock(OSProcess.class);
        EasyMock.expect(rceProcess.getProcessID()).andStubReturn(1);
        EasyMock.expect(rceProcess.getName()).andStubReturn("rce");
        EasyMock.replay(rceProcess);

        // fake child processes
        OSProcess child1 = EasyMock.createNiceMock(OSProcess.class);
        EasyMock.expect(child1.getProcessID()).andStubReturn(2);
        EasyMock.expect(child1.getName()).andStubReturn("child1");
        EasyMock.expect(child1.getParentProcessID()).andStubReturn(1);
        EasyMock.replay(child1);

        OSProcess child2 = EasyMock.createNiceMock(OSProcess.class);
        EasyMock.expect(child2.getProcessID()).andStubReturn(3);
        EasyMock.expect(child2.getName()).andStubReturn("child2");
        EasyMock.expect(child2.getParentProcessID()).andStubReturn(1);
        EasyMock.replay(child2);

        OSProcess child3 = EasyMock.createNiceMock(OSProcess.class);
        EasyMock.expect(child3.getProcessID()).andStubReturn(4);
        EasyMock.expect(child3.getName()).andStubReturn("child3");
        EasyMock.expect(child3.getParentProcessID()).andStubReturn(2);
        EasyMock.replay(child3);

        Set<OSProcess> subProcesses = new HashSet<>(Arrays.asList(child1, child2, child3));

        SystemDataSnapshot systemDataSnapshot1 =
            new SystemDataSnapshot(0, null, 0, 0, rceProcess, subProcesses);
        SystemDataSnapshot systemDataSnapshot2 =
            new SystemDataSnapshot(0, null, 0, 0, rceProcess,
                new HashSet<>(subProcesses)); // new set is necessary to create separate lists (otherwise recursion ends in stack overflow)
        RingBuffer<SystemDataSnapshot> ringBuffer = new RingBuffer<SystemDataSnapshot>(2);
        ringBuffer.add(systemDataSnapshot1);
        ringBuffer.add(systemDataSnapshot2);

        return new SystemMonitoringDataProcessor(ringBuffer, adapter);
    }

}
