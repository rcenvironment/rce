/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.internal;

import java.net.URI;
import java.util.UUID;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.datamanagement.commons.DataReference;
import de.rcenvironment.core.datamanagement.commons.MetaDataSet;

/**
 * Test cases for {@link DistributedMetaDataServiceImpl}.
 * 
 * @author Doreen Seider
 */
public class DistributedMetaDataServiceImplTest {

    private final URI location = URI.create("test");

    private MetaDataServiceImpl metaDataService;

    private NodeIdentifier pi;

    private UUID referenceID;

    private DataReference reference;

    private DataReference notReachableReference;

    private MetaDataSet mds;

    // /**
    // * Set up.
    // */
    // @Before
    // public void setUp() {
    // certificateMock = EasyMock.createNiceMock(User.class);
    // pi = NodeIdentifierFactory.fromHostAndNumberString("horst:1");
    // referenceID = UUID.randomUUID();
    // reference = new DataReference(DataReferenceType.fileObject, referenceID, pi, location);
    // notReachableReference =
    // new DataReference(DataReferenceType.fileObject, referenceID,
    // NodeIdentifierFactory.fromHostAndNumberString("notreachable:1"), location);
    // mds = new MetaDataSet();
    //
    // metaDataService = new DistributedMetaDataServiceImpl();
    // metaDataService.activate(EasyMock.createNiceMock(BundleContext.class));
    // metaDataService.bindCommunicationService(new DummyCommunicationService());
    // }
    //
    // /**
    // * Test.
    // */
    // @Test
    // public void testGetMetaDataSet() {
    // MetaDataSet set = metaDataService.getMetaDataSet(certificateMock, reference);
    // assertNotNull(set);
    // set = metaDataService.getMetaDataSet(certificateMock, notReachableReference);
    // assertNull(set);
    // }
    //
    // /**
    // * Test.
    // */
    // @Test
    // public void testUpdateMetaDataSet() {
    // metaDataService.updateMetaDataSet(certificateMock, reference, mds);
    // metaDataService.updateMetaDataSet(certificateMock, notReachableReference, mds);
    // }
    //
    // /**
    // * Test implementation of the {@link CommunicationService}.
    // *
    // * @author Doreen Seider
    // */
    // private class DummyCommunicationService extends CommunicationServiceDefaultStub {
    //
    // @Override
    // public Object getService(Class<?> iface, NodeIdentifier nodeId, BundleContext bundleContext)
    // throws IllegalStateException {
    // return getService(iface, new HashMap<String, String>(), nodeId, bundleContext);
    // }
    //
    // @Override
    // public Object getService(Class<?> iface, Map<String, String> properties, NodeIdentifier nodeId,
    // BundleContext bundleContext) throws IllegalStateException {
    // if (nodeId.equals(pi)) {
    // return new DummyMetaDataService();
    // } else {
    // return new NotReachableDummyMetaDataService();
    // }
    // }
    // }
    //
    // /**
    // * Test implementation of the {@link MetaDataService}.
    // *
    // * @author Doreen Seider
    // */
    // private class DummyMetaDataService implements MetaDataService {
    //
    // @Override
    // public void updateMetaDataSet(User proxyCertificate, DataReference dataReference,
    // MetaDataSet metaDataSet) throws AuthorizationException {
    // if (!(proxyCertificate.equals(certificateMock) && dataReference.equals(reference))) {
    // throw new RuntimeException();
    // }
    //
    // }
    //
    // @Override
    // public MetaDataSet getMetaDataSet(User proxyCertificate, DataReference dataReference)
    // throws AuthorizationException {
    // if (proxyCertificate.equals(certificateMock) && dataReference.equals(reference)) {
    // return mds;
    // } else {
    // throw new RuntimeException();
    // }
    // }
    //
    // }
    //
    // /**
    // * Not reachable test implementation of the {@link MetaDataService}.
    // *
    // * @author Doreen Seider
    // */
    // private class NotReachableDummyMetaDataService implements MetaDataService {
    //
    // @Override
    // public void updateMetaDataSet(User proxyCertificate, DataReference dataReference,
    // MetaDataSet metaDataSet) throws AuthorizationException {
    // throw new UndeclaredThrowableException(null);
    //
    // }
    //
    // @Override
    // public MetaDataSet getMetaDataSet(User proxyCertificate, DataReference dataReference)
    // throws AuthorizationException {
    // throw new UndeclaredThrowableException(null);
    // }
    // }
}
