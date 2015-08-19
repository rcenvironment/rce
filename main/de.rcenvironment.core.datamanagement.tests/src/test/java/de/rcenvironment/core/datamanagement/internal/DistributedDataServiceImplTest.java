/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.internal;

import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
import de.rcenvironment.core.datamanagement.DataService;
import de.rcenvironment.core.datamanagement.commons.BinaryReference;
import de.rcenvironment.core.datamanagement.commons.DataReference;
import de.rcenvironment.core.datamodel.api.CompressionFormat;

/**
 * Test cases for {@link DistributedDataServiceImpl}.
 * 
 * @author Doreen Seider
 */
public class DistributedDataServiceImplTest {

    private TestDistributedDataServiceImpl dataService;

    private User certificateMock;

    private NodeIdentifier pi;

    private UUID referenceID;

    private URI location = URI.create("test");

    private DataReference reference;

    private DataReference notReachableReference;

    /**
     * Set up.
     */
    @Before
    public void setUp() {
        certificateMock = EasyMock.createNiceMock(User.class);
        pi = NodeIdentifierFactory.fromHostAndNumberString("horst:1");
        referenceID = UUID.randomUUID();
        Set<BinaryReference> birefs = new HashSet<BinaryReference>();
        birefs.add(new BinaryReference(UUID.randomUUID().toString(), CompressionFormat.GZIP, "1"));

        reference = new DataReference(referenceID.toString(), pi, birefs);
        birefs = new HashSet<BinaryReference>();
        birefs.add(new BinaryReference(UUID.randomUUID().toString(), CompressionFormat.GZIP, "1"));
        notReachableReference =
            new DataReference(referenceID.toString(), NodeIdentifierFactory.fromHostAndNumberString("notreachable:1"), birefs);

        dataService = new TestDistributedDataServiceImpl();
        dataService.activate(EasyMock.createNiceMock(BundleContext.class));
        dataService.bindCommunicationService(new DummyCommunicationService());
    }

    /**
     * Test.
     */
    @Test
    public void testDeleteReference() {
        dataService.deleteReference(reference);
        dataService.deleteReference(notReachableReference);
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
            return getService(iface, new HashMap<String, String>(), nodeId, bundleContext);
        }

        @Override
        public Object getService(Class<?> iface, Map<String, String> properties, NodeIdentifier nodeId,
            BundleContext bundleContext) throws IllegalStateException {
            if (nodeId.equals(pi)) {
                return new DummyDataService();
            } else {
                return new NotReachableDummyDataService();
            }
        }

    }

    /**
     * Test implementation of the {@link DataService}.
     * 
     * @author Doreen Seider
     */
    private class DummyDataService implements DataService {

        @Override
        public void deleteReference(String dataReference) throws AuthorizationException {

        }

    }

    /**
     * Not reachable test implementation of the {@link DataService}.
     * 
     * @author Doreen Seider
     */
    private class NotReachableDummyDataService implements DataService {

        @Override
        public void deleteReference(String dataReference) throws AuthorizationException {
            throw new UndeclaredThrowableException(null);
        }

    }

    /**
     * Test class used to test the abstract {@link DistributedDataServiceImpl} class.
     * 
     * @author Doreen Seider
     */
    class TestDistributedDataServiceImpl extends DistributedDataServiceImpl {

        protected void activate(BundleContext bundleContext) {
            context = bundleContext;
        }

        protected void bindCommunicationService(CommunicationService newCommunicationService) {
            communicationService = newCommunicationService;
        }

    }

}
