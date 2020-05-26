/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.backend.metadata.derby.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.configuration.testutils.MockConfigurationService;
import de.rcenvironment.core.datamanagement.DataManagementIdMapping;
import de.rcenvironment.core.datamanagement.FileDataService;
import de.rcenvironment.core.datamanagement.commons.BinaryReference;
import de.rcenvironment.core.datamanagement.commons.ComponentInstance;
import de.rcenvironment.core.datamanagement.commons.ComponentRun;
import de.rcenvironment.core.datamanagement.commons.DataReference;
import de.rcenvironment.core.datamanagement.commons.EndpointInstance;
import de.rcenvironment.core.datamanagement.commons.MetaData;
import de.rcenvironment.core.datamanagement.commons.MetaDataKeys;
import de.rcenvironment.core.datamanagement.commons.MetaDataSet;
import de.rcenvironment.core.datamanagement.commons.WorkflowRun;
import de.rcenvironment.core.datamodel.api.CompressionFormat;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.datamodel.api.FinalComponentRunState;
import de.rcenvironment.core.datamodel.api.FinalWorkflowState;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.testutils.TypedDatumFactoryDefaultStub;
import de.rcenvironment.core.datamodel.testutils.TypedDatumSerializerDefaultStub;
import de.rcenvironment.core.datamodel.testutils.TypedDatumServiceDefaultStub;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.testing.CommonTestOptions;
import de.rcenvironment.toolkit.modules.concurrency.api.CallablesGroup;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Test cases for {@link DerbyMetaDataBackendServiceImpl}.
 * 
 * @author Juergen Klein // TODO review: still correct?
 * @author Jan Flink
 * @author Robert Mischke
 */
public class DerbyMetaDataBackendTest {

    private static final int MILLISECONDS_1000 = 1000;

    // "safety net" test timeout to avoid blocking continuous integration test runs
    private static final int COMPLEX_SCENARIO_TEST_TIMEOUT = 300000;

    private static final String STRING_TEST_RUN = "TestRun";

    private static File tempDirectory;

    private DerbyMetaDataBackendServiceImpl derbyMetaDataBackend;

    private final String bundleName = "superderby";

    private MetaDataSet metaDataSet;

    private TypedDatumFactory typedDatumFactory = new TypedDatumFactoryDefaultStub();

    private TypedDatumSerializer typedDatumSerializer = new TypedDatumSerializerDefaultStub();

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Sets the whole test case up.
     * 
     * @throws IOException on initialization failure
     * @throws RemoteOperationException standard remote operation exception
     */
    @BeforeClass
    public static void setUpTestcase() throws IOException, RemoteOperationException {
        TempFileServiceAccess.setupUnitTestEnvironment();
        tempDirectory = TempFileServiceAccess.getInstance().createManagedTempDir("derby-metadata");
        // perform db startup so the time is not added to the run time of the first test case
        DerbyMetaDataBackendTest dummyTest = new DerbyMetaDataBackendTest();
        dummyTest.setUp();
        dummyTest.tearDown();
    }

    /**
     * Set up.
     * 
     * @throws IOException on initialization failure
     * @throws RemoteOperationException standard remote operation exception
     */
    @Before
    public void setUp() throws IOException, RemoteOperationException {

        metaDataSet = new MetaDataSet();
        metaDataSet.setValue(new MetaData("testkey", true), "testvalue");

        derbyMetaDataBackend = new DerbyMetaDataBackendServiceImpl();
        derbyMetaDataBackend.bindConfigurationService(new DummyConfigurationService());
        derbyMetaDataBackend.bindTypedDatumService(new TypedDatumServiceDefaultStub());

        Bundle bundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(bundle.getSymbolicName()).andReturn(bundleName).anyTimes();
        EasyMock.replay(bundle);
        BundleContext bundleContext = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bundleContext.getBundle()).andReturn(bundle).anyTimes();
        EasyMock.replay(bundleContext);

        // create and set a mock that takes blob deletion requests, and ignores them without error
        FileDataService mockDataService = EasyMock.createNiceMock(FileDataService.class);
        mockDataService.deleteReference(EasyMock.anyObject(String.class));
        EasyMock.expectLastCall().anyTimes();
        EasyMock.replay(mockDataService);
        derbyMetaDataBackend.bindDataService(mockDataService);

        derbyMetaDataBackend.activate(bundleContext);

        System.setProperty("derby.locks.monitor", "true");
        System.setProperty("derby.locks.deadlockTrace", "true");
    }

    /** Tear down. */
    @After
    public void tearDown() {
        derbyMetaDataBackend.deactivate();
    }

    /**
     * Tears down the whole test case.
     * 
     * @throws IOException IOException
     * 
     */
    @AfterClass
    public static void tearDownTestcase() throws IOException {
        // FileUtils.deleteDirectory(tempDirectory);
    }

    /** Test. */
    @Test
    public void testAddWorkflowRun() {
        Long wfRunId =
            derbyMetaDataBackend.addWorkflowRun(STRING_TEST_RUN, DataManagementIdMapping.createDummyNodeIdStringForTesting(),
                DataManagementIdMapping.createDummyNodeIdStringForTesting(),
                System.currentTimeMillis());
        assertNotNull(wfRunId);
    }

    /** Test. */
    @Test
    public void testAddComponentInstances() {
        Long wfRunId =
            derbyMetaDataBackend.addWorkflowRun(STRING_TEST_RUN, DataManagementIdMapping.createDummyNodeIdStringForTesting(),
                DataManagementIdMapping.createDummyNodeIdStringForTesting(),
                System.currentTimeMillis());
        assertNotNull(wfRunId);
        Collection<ComponentInstance> componentInstances = createComponentInstances(4);
        Map<String, Long> result = derbyMetaDataBackend.addComponentInstances(wfRunId, componentInstances);
        assertNotNull(result);
        assertEquals(componentInstances.size(), result.size());
        for (ComponentInstance ci : componentInstances) {
            assertTrue(result.containsKey(ci.getComponentExecutionID()));
        }
    }

    private Collection<ComponentInstance> createComponentInstances(int numComponents) {
        Collection<ComponentInstance> componentInstances = new HashSet<ComponentInstance>();
        String componentID = "de.rcenvironment.testcomponent";
        for (int i = 1; i <= numComponents; i++) {
            // TODO review UUID generation; encapsulate in properly named method
            componentInstances.add(new ComponentInstance(UUID.randomUUID().toString(), componentID, "Testcomponent" + i, null));
        }
        return componentInstances;
    }

    /** Test. */
    @Test
    public void testAddEndpointInstances() {
        Long wfRunId =
            derbyMetaDataBackend.addWorkflowRun(STRING_TEST_RUN, DataManagementIdMapping.createDummyNodeIdStringForTesting(),
                DataManagementIdMapping.createDummyNodeIdStringForTesting(),
                System.currentTimeMillis());
        Collection<ComponentInstance> componentInstances = createComponentInstances(4);
        Map<String, Long> componentInstanceIdMap = derbyMetaDataBackend.addComponentInstances(wfRunId, componentInstances);
        Collection<EndpointInstance> endpointInstances = getEndpointInstances();
        Map<String, Long> result = new HashMap<String, Long>();
        for (Long id : componentInstanceIdMap.values()) {
            result.putAll(derbyMetaDataBackend.addEndpointInstances(id, endpointInstances));
        }
        assertFalse(result.isEmpty());
    }

    private Collection<EndpointInstance> getEndpointInstances() {
        Collection<EndpointInstance> endpointInstances = new HashSet<EndpointInstance>();
        Map<String, String> metaData = new HashMap<String, String>();
        metaData.put(MetaDataKeys.DATA_TYPE, DataType.Float.getShortName());
        endpointInstances.add(new EndpointInstance("X_in", EndpointType.INPUT, metaData));
        endpointInstances.add(new EndpointInstance("Y_in", EndpointType.INPUT, metaData));
        endpointInstances.add(new EndpointInstance("Z_in", EndpointType.INPUT, metaData));
        endpointInstances.add(new EndpointInstance("X_out", EndpointType.OUTPUT, metaData));
        endpointInstances.add(new EndpointInstance("Y_out", EndpointType.OUTPUT, metaData));
        endpointInstances.add(new EndpointInstance("Y_out", EndpointType.OUTPUT, metaData));
        return endpointInstances;
    }

    /** Test. */
    @Test
    public void testAddComponentRun() {
        Long wfRunId =
            derbyMetaDataBackend.addWorkflowRun(STRING_TEST_RUN, DataManagementIdMapping.createDummyNodeIdStringForTesting(),
                DataManagementIdMapping.createDummyNodeIdStringForTesting(),
                System.currentTimeMillis());
        Collection<ComponentInstance> componentInstances = createComponentInstances(4);
        Map<String, Long> componentInstanceIdMap = derbyMetaDataBackend.addComponentInstances(wfRunId, componentInstances);
        for (Long ciid : componentInstanceIdMap.values()) {
            Long id =
                derbyMetaDataBackend.addComponentRun(ciid, DataManagementIdMapping.createDummyNodeIdStringForTesting(), 1,
                    System.currentTimeMillis());
            assertNotNull(id);
            derbyMetaDataBackend.setComponentRunFinished(id, System.currentTimeMillis() + MILLISECONDS_1000,
                FinalComponentRunState.FINISHED);
            Collection<ComponentRun> runs = derbyMetaDataBackend.getComponentRuns(ciid);
            assertFalse(runs.isEmpty());
            assertEquals(1, runs.size());
        }

    }

    /** Test. */
    @Test(timeout = COMPLEX_SCENARIO_TEST_TIMEOUT)
    public void testAddDeleteData() {
        // test single-workflow deletion with the defined components and number of runs per component each
        final int numComponents = 10;
        final int numRunsPerComponent = 50;
        performAddDeleteWorkflowWithDataCycle(numComponents, numRunsPerComponent, 0);
    }

    /** Test. */
    @Test(timeout = COMPLEX_SCENARIO_TEST_TIMEOUT)
    public void testParallelAddDeleteData() {
        // TODO review standard/extended values; commented code are the previously hardcoded ones
        // final int parallelTasks = 50; // low value for CI test runs; increase for manual tests of high-contention problems
        // final int numComponents = 20;
        // final int numRunsPerComponent = 20; // increase in manual testing
        final int parallelTasks = CommonTestOptions.selectStandardOrExtendedValue(10, 50);
        final int numComponents = CommonTestOptions.selectStandardOrExtendedValue(10, 20);
        final int numRunsPerComponent = CommonTestOptions.selectStandardOrExtendedValue(10, 20);

        // the following wait interval can be set to leave the workflow data in the database for some time
        final long minWaitBeforeDeletion = 0;
        final long maxWaitBeforeDeletion = 0;
        CallablesGroup<Boolean> callablesGroup = ConcurrencyUtils.getFactory().createCallablesGroup(Boolean.class);
        for (int i = 0; i < parallelTasks; i++) {
            callablesGroup.add(new Callable<Boolean>() {

                @Override
                @TaskDescription("Parallel add/delete cycle task")
                public Boolean call() throws Exception {
                    try {
                        long wait = minWaitBeforeDeletion + ((long) Math.random() * (maxWaitBeforeDeletion - minWaitBeforeDeletion));
                        performAddDeleteWorkflowWithDataCycle(numComponents, numRunsPerComponent, wait);
                        return true;
                    } catch (RuntimeException e) {
                        log.error("Asynchronous add/delete cycle failed", e);
                        return false;
                    }
                }
            });
        }
        List<Boolean> results = callablesGroup.executeParallel(null);
        int succcessCount = 0;
        for (Boolean result : results) {
            if (result == Boolean.TRUE) {
                succcessCount++;
            }
        }
        assertEquals("One or more parallel add/delete tasks failed; check the log output for asynchronous exceptions", parallelTasks,
            succcessCount);
    }

    private void performAddDeleteWorkflowWithDataCycle(int numComponents, int numRunsPerComponent, long waitBeforeDeleteMsec) {
        Long wfRunId =
            derbyMetaDataBackend.addWorkflowRun(STRING_TEST_RUN, DataManagementIdMapping.createDummyNodeIdStringForTesting(),
                DataManagementIdMapping.createDummyNodeIdStringForTesting(),
                System.currentTimeMillis());
        log.debug(StringUtils.format("Added workflow run (id %d)", wfRunId));
        Collection<ComponentInstance> componentInstances = createComponentInstances(numComponents);
        Map<String, Long> componentInstanceIdMap = derbyMetaDataBackend.addComponentInstances(wfRunId, componentInstances);
        log.debug(StringUtils.format("Added component instances to workflow run id %d", wfRunId));
        Collection<EndpointInstance> endpointInstances = getEndpointInstances();
        Map<String, Long> endpointInstanceIdMap = new HashMap<String, Long>();
        for (Long id : componentInstanceIdMap.values()) {
            endpointInstanceIdMap.putAll(derbyMetaDataBackend.addEndpointInstances(id, endpointInstances));
        }
        log.debug(StringUtils.format("Added endpoint instances to workflow run id %d", wfRunId));
        for (int crunCounter = 1; crunCounter <= numRunsPerComponent; crunCounter++) {
            Set<Long> crunIds = new HashSet<Long>();
            for (Long ciid : componentInstanceIdMap.values()) {
                crunIds.add(
                    derbyMetaDataBackend.addComponentRun(ciid, DataManagementIdMapping.createDummyNodeIdStringForTesting(), crunCounter,
                        System.currentTimeMillis()));
            }
            Iterator<Long> crunIdIterator = crunIds.iterator();
            Iterator<Long> epiIdIterator = endpointInstanceIdMap.values().iterator();
            // TODO review UUID generation; encapsulate in properly named method
            Long id =
                derbyMetaDataBackend.addOutputDatum(crunIdIterator.next(), epiIdIterator.next(),
                    typedDatumSerializer.serialize(typedDatumFactory.createFileReference(UUID.randomUUID().toString(), "test.xml")), 1);
            assertNotNull(id);
            id =
                derbyMetaDataBackend.addOutputDatum(crunIdIterator.next(), epiIdIterator.next(),
                    typedDatumSerializer.serialize(typedDatumFactory.createBoolean(false)), 1);
            assertNotNull(id);
            id =
                derbyMetaDataBackend.addOutputDatum(crunIdIterator.next(), epiIdIterator.next(),
                    typedDatumSerializer.serialize(typedDatumFactory.createFloat(3f)), 1);
            assertNotNull(id);

            Long crunId = crunIdIterator.next();
            derbyMetaDataBackend.addInputDatum(crunId, id, endpointInstanceIdMap.values().iterator().next(), 1);

            // Generate data reference and add it to a component run
            Set<BinaryReference> brefs = new HashSet<BinaryReference>();
            // TODO review UUID generation; encapsulate in properly named method
            String key = UUID.randomUUID().toString();
            brefs.add(new BinaryReference(key, CompressionFormat.GZIP, "1.1"));
            // note: the following line is creating a new instance id on each call; kept this way to maintain pre-8.0 test behavior
            derbyMetaDataBackend.addDataReferenceToComponentRun(crunId,
                new DataReference(key, NodeIdentifierTestUtils.createTestDefaultLogicalNodeId(), brefs));
        }
        log.debug(StringUtils.format("Added %d component runs to workflow run id %d", (numComponents * numRunsPerComponent), wfRunId));
        // mark as finished, otherwise file deletion will (correctly) fail
        derbyMetaDataBackend.setWorkflowRunFinished(wfRunId, System.currentTimeMillis(), FinalWorkflowState.FINISHED);
        log.debug(StringUtils.format("Set workflow run id %d finished", wfRunId));

        assertEquals(STRING_TEST_RUN, derbyMetaDataBackend.getWorkflowRun(wfRunId).getWorkflowTitle());
        log.debug(StringUtils.format("Retrieved worklfow run data of id %d", wfRunId));

        try {
            Thread.sleep(waitBeforeDeleteMsec);
        } catch (InterruptedException e) {
            // wrap for simplicity
            throw new RuntimeException("Interrupted while waiting", e);
        }

        if ((wfRunId % 2) == 0) {
            // Delete data references
            derbyMetaDataBackend.deleteWorkflowRunFiles(wfRunId);
            log.debug(StringUtils.format("Deleted files of workflow run id %d", wfRunId));
        } else {
            // Delete workflow run
            derbyMetaDataBackend.deleteWorkflowRun(wfRunId);
            log.debug(StringUtils.format("Deleted workflow run id %d", wfRunId));
        }
    }

    /** Test. */
    @Test
    public void testGetWorkflowRun() {
        Long wfRunId =
            derbyMetaDataBackend.addWorkflowRun(STRING_TEST_RUN, DataManagementIdMapping.createDummyNodeIdStringForTesting(),
                DataManagementIdMapping.createDummyNodeIdStringForTesting(),
                System.currentTimeMillis());
        Collection<ComponentInstance> componentInstances = createComponentInstances(4);
        Map<String, Long> componentInstanceIdMap = derbyMetaDataBackend.addComponentInstances(wfRunId, componentInstances);
        for (Long ciid : componentInstanceIdMap.values()) {
            derbyMetaDataBackend.addComponentRun(ciid, DataManagementIdMapping.createDummyNodeIdStringForTesting(), 1,
                System.currentTimeMillis());
        }
        WorkflowRun wfrun = derbyMetaDataBackend.getWorkflowRun(wfRunId);
        assertNotNull(wfrun);
    }

    /**
     * Test implementation of <code>ConfigurationService</code>.
     * 
     * @author Doreen Seider
     */
    private class DummyConfigurationService extends MockConfigurationService.ThrowExceptionByDefault {

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getConfiguration(String identifier, Class<T> clazz) {
            if (identifier.equals(bundleName) && clazz == DerbyMetaDataBackendConfiguration.class) {
                return (T) new DerbyMetaDataBackendConfiguration();
            }
            return null;
        }

        @Override
        public File getProfileDirectory() {
            return tempDirectory;
        }

        @Override
        public File getConfigurablePath(ConfigurablePathId pathId) {
            assertEquals(ConfigurablePathId.PROFILE_DATA_MANAGEMENT, pathId);
            return tempDirectory;
        }

    }

}
