/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.authentication.User;
import de.rcenvironment.core.authorization.AuthorizationException;
import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.communication.testutils.CommunicationServiceDefaultStub;
import de.rcenvironment.core.communication.testutils.PlatformServiceDefaultStub;
import de.rcenvironment.core.datamanagement.FileDataService;
import de.rcenvironment.core.datamanagement.MetaDataService;
import de.rcenvironment.core.datamanagement.backend.DataBackend;
import de.rcenvironment.core.datamanagement.backend.MetaDataBackendService;
import de.rcenvironment.core.datamanagement.commons.BinaryReference;
import de.rcenvironment.core.datamanagement.commons.ComponentInstance;
import de.rcenvironment.core.datamanagement.commons.ComponentRun;
import de.rcenvironment.core.datamanagement.commons.DataReference;
import de.rcenvironment.core.datamanagement.commons.EndpointData;
import de.rcenvironment.core.datamanagement.commons.EndpointInstance;
import de.rcenvironment.core.datamanagement.commons.WorkflowRun;
import de.rcenvironment.core.datamanagement.commons.WorkflowRunDescription;
import de.rcenvironment.core.datamanagement.commons.WorkflowRunTimline;
import de.rcenvironment.core.datamanagement.testutils.FileDataServiceDefaultStub;
import de.rcenvironment.core.datamodel.api.CompressionFormat;
import de.rcenvironment.core.datamodel.api.FinalComponentState;
import de.rcenvironment.core.datamodel.api.FinalWorkflowState;
import de.rcenvironment.core.datamodel.api.TimelineIntervalType;

/**
 * Test cases for {@link DataServiceImpl}.
 * 
 * @author Doreen Seider
 */
public class DataServiceImplTest {

    private User certificate;

    private NodeIdentifier pi;

    private UUID drId;

    private URI location = URI.create("test");

    private FileDataServiceImpl fileDataService;

    private DataReference dr;

    private DataReference newestDr;

    /** Set up. */
    @Before
    public void setUp() {
        pi = NodeIdentifierFactory.fromHostAndNumberString("na klar:6");
        drId = UUID.randomUUID();

        certificate = EasyMock.createNiceMock(User.class);
        EasyMock.expect(certificate.isValid()).andReturn(true).anyTimes();
        EasyMock.replay(certificate);

        Set<BinaryReference> birefs = new HashSet<BinaryReference>();
        birefs.add(new BinaryReference(UUID.randomUUID().toString(), CompressionFormat.GZIP, "1"));

        dr = new DataReference(drId.toString(), pi, birefs);
        birefs = new HashSet<BinaryReference>();
        birefs.add(new BinaryReference(UUID.randomUUID().toString(), CompressionFormat.GZIP, "1"));
        newestDr = new DataReference(UUID.randomUUID().toString(), pi, birefs);

        fileDataService = new FileDataServiceImpl();
        fileDataService.bindCommunicationService(new DummyCommunicationService());
        fileDataService.bindPlatformService(new PlatformServiceDefaultStub());
        fileDataService.activate(EasyMock.createNiceMock(BundleContext.class));

        new BackendSupportTest().setUp();
        new BackendSupport().activate(BackendSupportTest.createBundleContext(new DummyCatalogBackend(), new DummyDataBackend()));
    }

    /** Test. */
    @Test
    public void testDeleteReference() {
        fileDataService.deleteReference(dr.getBinaryReferences().iterator().next().getBinaryReferenceKey());
    }

    /**
     * Test implementation of the {@link CommunicationService}.
     * 
     * @author Doreen Seider
     */
    private class DummyCommunicationService extends CommunicationServiceDefaultStub {

        @Override
        public Object getService(Class<?> iface, NodeIdentifier nodeId, BundleContext bundleContext)
            throws IllegalStateException {
            Object service = null;
            if (iface.equals(MetaDataService.class)) {
                service = new DummyMetaDataService();
            } else if (iface.equals(FileDataService.class)) {
                service = new DummyFileDataService();
            }
            return service;
        }

    }

    /**
     * Test implementation of the {@link MetaDataService}.
     * 
     * @author Doreen Seider
     */
    private class DummyMetaDataService implements MetaDataService {

        @Override
        public Long addWorkflowRun(String workflowTitle, String workflowControllerNodeId, String workflowDataManagementNodeId,
            Long starttime) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Map<String, Long> addComponentInstances(Long workflowRunId, Collection<ComponentInstance> componentInstances) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Map<String, Long> addEndpointInstances(Long componentInstanceId, Collection<EndpointInstance> endpointInstances) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Long addComponentRun(Long componentInstanceId, String nodeId, Integer count, Long starttime) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void addInputDatum(Long componentRunId, Long typedDatumId, Long endpointInstanceId, Integer count) {
            // TODO Auto-generated method stub

        }

        @Override
        public Long addOutputDatum(Long componentRunId, Long endpointInstanceId, String datum, Integer count) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void addWorkflowRunProperties(Long workflowRunId, Map<String, String> properties) {
            // TODO Auto-generated method stub

        }

        @Override
        public void addComponentInstanceProperties(Long componentInstanceId, Map<String, String> properties) {
            // TODO Auto-generated method stub

        }

        @Override
        public void addComponentRunProperties(Long componentRunId, Map<String, String> properties) {
            // TODO Auto-generated method stub

        }

        @Override
        public void setOrUpdateHistoryDataItem(Long componentRunId, String historyDataItem) {
            // TODO Auto-generated method stub

        }

        @Override
        public void setWorkflowRunFinished(Long workflowRunId, Long endtime, FinalWorkflowState finalState) {
            // TODO Auto-generated method stub

        }

        @Override
        public void setComponentRunFinished(Long componentRunId, Long endtime) {
            // TODO Auto-generated method stub

        }

        @Override
        public void setComponentInstanceFinalState(Long componentInstanceId, FinalComponentState finalState) {
            // TODO Auto-generated method stub

        }

        @Override
        public SortedSet<WorkflowRunDescription> getWorkflowRunDescriptions() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public WorkflowRun getWorkflowRun(Long workflowRunId) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Collection<ComponentRun> getComponentRuns(Long componentInstanceId) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Collection<EndpointData> getInputData(Long componentRunId) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Collection<EndpointData> getOutputData(Long componentRunId) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Map<String, String> getWorkflowRunProperties(Long workflowRunId) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Map<String, String> getComponentRunProperties(Long componentRunId) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void addTimelineInterval(Long workflowRunId, TimelineIntervalType intervalType, long starttime) {
            // TODO Auto-generated method stub

        }

        @Override
        public Long addTimelineInterval(Long workflowRunId, TimelineIntervalType intervalType, long starttime, Long relatedComponentId) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setTimelineIntervalFinished(Long timelineIntervalId, long endtime) {
            // TODO Auto-generated method stub

        }

        @Override
        public WorkflowRunTimline getWorkflowTimeline(Long workflowRunId) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Boolean deleteWorkflowRun(Long workflowRunId) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Boolean deleteWorkflowRunFiles(Long workflowRunId) {
            // TODO Auto-generated method stub
            return null;
        }

    }

    /**
     * Test implementation of the {@link FileDataService}.
     * 
     * @author Doreen Seider
     */
    private class DummyFileDataService extends FileDataServiceDefaultStub {

        @Override
        public InputStream getStreamFromDataReference(User proxyCertificate, DataReference dataReference, Boolean calledFromRemote)
            throws AuthorizationException {
            if (proxyCertificate == certificate && dataReference.equals(newestDr)) {
                return new InputStream() {

                    @Override
                    public int read() throws IOException {
                        return 0;
                    }
                };
            }
            return null;
        }

    }

    /**
     * Test implementation of {@link MetaDataBackendService}.
     * 
     * @author Doreen Seider
     */
    private class DummyCatalogBackend implements MetaDataBackendService {

        @Override
        public Long addWorkflowRun(String workflowTitle, String workflowControllerNodeId, String workflowDataManagementNodeId,
            Long starttime) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Map<String, Long> addComponentInstances(Long workflowRunId, Collection<ComponentInstance> componentInstances) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Map<String, Long> addEndpointInstances(Long componentInstanceId, Collection<EndpointInstance> endpointInstances) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Long addComponentRun(Long componentInstanceId, String nodeId, Integer count, Long starttime) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void addInputDatum(Long componentRunId, Long typedDatumId, Long endpointInstanceId, Integer count) {
            // TODO Auto-generated method stub

        }

        @Override
        public Long addOutputDatum(Long componentRunId, Long endpointInstanceId, String datum, Integer count) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void addWorkflowRunProperties(Long workflowRunId, Map<String, String> properties) {
            // TODO Auto-generated method stub

        }

        @Override
        public void addComponentInstanceProperties(Long componentInstanceId, Map<String, String> properties) {
            // TODO Auto-generated method stub

        }

        @Override
        public void addComponentRunProperties(Long componentRunId, Map<String, String> properties) {
            // TODO Auto-generated method stub

        }

        @Override
        public void setOrUpdateHistoryDataItem(Long componentRunId, String historyDataItem) {
            // TODO Auto-generated method stub

        }

        @Override
        public void setOrUpdateTimelineDataItem(Long workflowRunId, String timelineDataItem) {
            // TODO Auto-generated method stub

        }

        @Override
        public void setWorkflowRunFinished(Long workflowRunId, Long endtime, FinalWorkflowState finalState) {
            // TODO Auto-generated method stub

        }

        @Override
        public void setComponentRunFinished(Long componentRunId, Long endtime) {
            // TODO Auto-generated method stub

        }

        @Override
        public void setComponentInstanceFinalState(Long componentInstanceId, FinalComponentState finalState) {
            // TODO Auto-generated method stub

        }

        @Override
        public SortedSet<WorkflowRunDescription> getWorkflowRunDescriptions() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public WorkflowRun getWorkflowRun(Long workflowRunId) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Collection<ComponentRun> getComponentRuns(Long componentInstanceId) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Collection<EndpointData> getInputData(Long componentRunId) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Collection<EndpointData> getOutputData(Long componentRunId) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Map<String, String> getWorkflowRunProperties(Long workflowRunId) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Map<String, String> getComponentRunProperties(Long componentRunId) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void addTimelineInterval(Long workflowRunId, TimelineIntervalType intervalType, long starttime) {
            // TODO Auto-generated method stub

        }

        @Override
        public Long addTimelineInterval(Long workflowRunId, TimelineIntervalType intervalType, long starttime, Long relatedComponentId) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setTimelineIntervalFinished(Long timelineIntervalId, long endtime) {
            // TODO Auto-generated method stub

        }

        @Override
        public WorkflowRunTimline getWorkflowTimeline(Long workflowRunId) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Boolean deleteWorkflowRun(Long workflowRunId) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Long addDataReferenceToComponentRun(Long componentRunId, DataReference dataReference) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Long addDataReferenceToComponentInstance(Long componentInstanceId, DataReference dataReference) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Long addDataReferenceToWorkflowRun(Long workflowRunId, DataReference dataReference) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public DataReference getDataReference(String dataReferenceKey) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void addBinaryReference(Long dataReferenceId, BinaryReference binaryReference) {
            // TODO Auto-generated method stub
        }

        @Override
        public Boolean deleteWorkflowRunFiles(Long workflowRunId) {
            // TODO Auto-generated method stub
            return null;
        }

    }

    /**
     * Test implementation of {@link DataBackend}.
     * 
     * @author Doreen Seider
     */
    private class DummyDataBackend implements DataBackend {

        @Override
        public URI suggestLocation(UUID guid) {
            return null;
        }

        @Override
        public long put(URI loc, Object object) {
            return 0;
        }

        @Override
        public boolean delete(URI loc) {
            return false;
        }

        @Override
        public Object get(URI loc) {
            return null;
        }

    }
}
