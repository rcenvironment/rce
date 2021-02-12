/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.monitoring.system.api.OperatingSystemException;
import de.rcenvironment.core.monitoring.system.api.SystemMonitoringConstants;
import de.rcenvironment.core.monitoring.system.api.SystemMonitoringDataService;
import de.rcenvironment.core.monitoring.system.api.model.SystemLoadInformation;
import de.rcenvironment.toolkit.utils.common.MockTimeSource;

/**
 * Unit test for {@link SystemLoadInformationCollector}.
 *
 * @author Robert Mischke
 */
public class SystemLoadInformationCollectorTest {

    private static final double DEFAULT_DOUBLE_TEST_EPSILON = 0.001;

    /**
     * Tests the behavior when no low-level data has been acquired yet.
     * 
     * @throws OperatingSystemException on internal errors
     */
    @Test
    public void defaultValuesForNoData() {
        final SystemMonitoringDataService dataSourceMock = EasyMock.createStrictMock(SystemMonitoringDataService.class);
        SystemLoadInformationCollector collector =
            new SystemLoadInformationCollector(dataSourceMock, 10, new MockTimeSource(), 0, Integer.MAX_VALUE);
        SystemLoadInformation systemLoadData;

        systemLoadData = collector.getSystemLoadInformation(5);
        assertTrue(Double.isNaN(SystemMonitoringConstants.CPU_LOAD_UNKNOWN_DEFAULT));
        assertTrue(Double.isNaN(systemLoadData.getCpuLoad()));
        assertEquals(SystemMonitoringConstants.RAM_UNKNOWN_DEFAULT, systemLoadData.getAvailableRam());
        assertEquals(0, systemLoadData.getCpuLoadAvg().getNumSamples());
        assertEquals(0.0, systemLoadData.getCpuLoadAvg().getAverage(), DEFAULT_DOUBLE_TEST_EPSILON); // convention: 0.0 on zero elements
    }

    /**
     * Tests the aggregation of low-level data to higher-level information holders.
     * 
     * @throws OperatingSystemException on internal errors
     */
    @Test
    public void dataFetchingAndAggregation() throws OperatingSystemException {

        final int bufferCapacity = 3;
        final double val02 = 0.2;
        final double val04 = 0.4;
        final double val09 = 0.9;
        final double val05 = 0.5;

        final SystemMonitoringDataService dataSourceMock = EasyMock.createNiceMock(SystemMonitoringDataService.class);
        EasyMock.expect(dataSourceMock.getTotalCPUUsage()).andReturn(val02);
        EasyMock.expect(dataSourceMock.getTotalCPUUsage()).andReturn(val04); // avg = 0.3
        EasyMock.expect(dataSourceMock.getTotalCPUUsage()).andReturn(val09); // avg = 0.5
        EasyMock.expect(dataSourceMock.getTotalCPUUsage()).andReturn(val05); // avg of last 3 = 0.6
        EasyMock.replay(dataSourceMock);

        final MockTimeSource mockTimeSource = new MockTimeSource();
        SystemLoadInformationCollector collector =
            new SystemLoadInformationCollector(dataSourceMock, bufferCapacity, mockTimeSource, 0, Integer.MAX_VALUE);
        SystemLoadInformation systemLoadData;

        // note: the behavior before the first run() is tested separately

        collector.run();
        systemLoadData = collector.getSystemLoadInformation(5);
        assertEquals(val02, systemLoadData.getCpuLoad(), DEFAULT_DOUBLE_TEST_EPSILON);
        assertEquals(1, systemLoadData.getCpuLoadAvg().getNumSamples());
        assertEquals(val02, systemLoadData.getCpuLoadAvg().getAverage(), DEFAULT_DOUBLE_TEST_EPSILON);

        collector.run();
        systemLoadData = collector.getSystemLoadInformation(5);
        assertEquals(val04, systemLoadData.getCpuLoad(), DEFAULT_DOUBLE_TEST_EPSILON);
        assertEquals(2, systemLoadData.getCpuLoadAvg().getNumSamples());
        final double expected03 = 0.3; // CheckStyle is kind of silly for this case
        assertEquals(expected03, systemLoadData.getCpuLoadAvg().getAverage(), DEFAULT_DOUBLE_TEST_EPSILON);

        collector.run(); // capacity reached
        systemLoadData = collector.getSystemLoadInformation(5);
        assertEquals(val09, systemLoadData.getCpuLoad(), DEFAULT_DOUBLE_TEST_EPSILON);
        assertEquals(3, systemLoadData.getCpuLoadAvg().getNumSamples());
        final double expected05 = 0.5;
        assertEquals(expected05, systemLoadData.getCpuLoadAvg().getAverage(), DEFAULT_DOUBLE_TEST_EPSILON);

        collector.run(); // capacity overflow; should replace first sample
        systemLoadData = collector.getSystemLoadInformation(5);
        assertEquals(val05, systemLoadData.getCpuLoad(), DEFAULT_DOUBLE_TEST_EPSILON);
        assertEquals(3, systemLoadData.getCpuLoadAvg().getNumSamples());
        final double expected06 = 0.6;
        assertEquals(expected06, systemLoadData.getCpuLoadAvg().getAverage(), DEFAULT_DOUBLE_TEST_EPSILON);

    }

    /**
     * Tests the reaction to a sudden forward leap in system time, and to update requests that are fired significantly faster than the
     * average update time, both of which happen when the system running the software is put into a suspend state and then resumed.
     * 
     * @throws OperatingSystemException on internal errors
     */
    @Test
    public void behaviorOnTimingAnomalies() throws OperatingSystemException {
        final SystemMonitoringDataService dataSourceMock = EasyMock.createNiceMock(SystemMonitoringDataService.class);
        EasyMock.expect(dataSourceMock.getTotalCPUUsage()).andReturn(0.0);
        EasyMock.replay(dataSourceMock);

        final MockTimeSource mockTimeSource = new MockTimeSource();
        final int intendedUpdateInterval = 1000;
        final int minimumTimeDelta = 800;
        final int maximumTimeDelta = 5000;

        SystemLoadInformationCollector collector =
            new SystemLoadInformationCollector(dataSourceMock, 10, mockTimeSource, minimumTimeDelta, maximumTimeDelta);

        // baseline checks
        assertNumberOfAvailableCpuSamples(collector, 0);
        collector.run(); // first update; should always run
        assertNumberOfAvailableCpuSamples(collector, 1);

        // time has not advanced; call should be ignored
        collector.run();
        assertNumberOfAvailableCpuSamples(collector, 1);

        // still below minimum time; call should be ignored
        mockTimeSource.setCurrentMockTime(minimumTimeDelta - 1);
        collector.run();
        assertNumberOfAvailableCpuSamples(collector, 1);

        mockTimeSource.setCurrentMockTime(minimumTimeDelta);
        collector.run();
        assertNumberOfAvailableCpuSamples(collector, 2);

        mockTimeSource.setCurrentMockTime(intendedUpdateInterval);
        collector.run();
        assertNumberOfAvailableCpuSamples(collector, 2);
    }

    private void assertNumberOfAvailableCpuSamples(SystemLoadInformationCollector collector, final int expected) {
        assertEquals(expected, collector.getSystemLoadInformation(Integer.MAX_VALUE).getCpuLoadAvg().getNumSamples());
    }
}
