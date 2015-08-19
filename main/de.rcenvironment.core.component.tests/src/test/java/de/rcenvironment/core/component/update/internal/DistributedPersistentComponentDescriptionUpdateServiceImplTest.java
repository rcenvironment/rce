/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.update.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.communication.testutils.CommunicationServiceDefaultStub;
import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.update.api.PersistentComponentDescriptionUpdateService;
import de.rcenvironment.core.component.update.api.PersistentDescriptionFormatVersion;
import de.rcenvironment.core.component.update.spi.PersistentComponentDescriptionUpdater;

/**
 * Test cases for {@link DistributedPersistentComponentDescriptionUpdateServiceImpl}.
 * @author Doreen Seider
 */
public class DistributedPersistentComponentDescriptionUpdateServiceImplTest {
    
    private static final String NODEID_WITH_UPDATE = "node.id.update";
    
    private PersistentComponentDescription updatedComponentDescription = EasyMock.createNiceMock(PersistentComponentDescription.class);
    
    /**
     * Test.
     * @throws IOException on error.
     */
    @Test
    public void test() throws IOException {
        
        DistributedPersistentComponentDescriptionUpdateServiceImpl updaterService
            = new DistributedPersistentComponentDescriptionUpdateServiceImpl();
        
        updaterService.bindCommunicationService(new TestCommunicationService());
        
        List<PersistentComponentDescription> descriptions = new ArrayList<PersistentComponentDescription>();
        PersistentComponentDescription descriptionWithoutUpdate = createLocalComponentDescriptionWithoutUpdate();
        descriptions.add(descriptionWithoutUpdate);
        
        assertEquals(PersistentDescriptionFormatVersion.NONE,
            updaterService.getFormatVersionsAffectedByUpdate(descriptions, false));
        
        PersistentComponentDescription descriptionWithUpdate = createRemoteComponentDescriptionWithUpdate();
        descriptions.add(descriptionWithUpdate);

        assertEquals(PersistentDescriptionFormatVersion.BEFORE_VERSON_THREE,
            updaterService.getFormatVersionsAffectedByUpdate(descriptions, false));
        
        List<PersistentComponentDescription> udpatedDescriptions = updaterService
            .performComponentDescriptionUpdates(PersistentDescriptionFormatVersion.BEFORE_VERSON_THREE, descriptions, false);

        assertEquals(2, udpatedDescriptions.size());
        assertTrue(udpatedDescriptions.contains(updatedComponentDescription));
        assertTrue(udpatedDescriptions.contains(descriptionWithoutUpdate));
        assertFalse(udpatedDescriptions.contains(descriptionWithUpdate));
    }
    
    /**
     * Implementation of CommunicationService for test purposes.
     * @author Doreen Seider
     */
    class TestCommunicationService extends CommunicationServiceDefaultStub {
     
        @Override
        public Object getService(Class<?> iface, NodeIdentifier nodeId, BundleContext bundleContext)
            throws IllegalStateException {
            if (nodeId == null) {
                return new LocalPersistentComponentDescriptionUpdateService();
            } else if (nodeId.getIdString().equals(NODEID_WITH_UPDATE)) {
                return new RemotePersistentComponentDescriptionUpdateService();
            }
            return null;
        }
    }
    
    /**
     * Dummy implementation of {@link PersistentComponentDescriptionUpdater}.
     * @author Doreen Seider
     */
    class LocalPersistentComponentDescriptionUpdateService implements PersistentComponentDescriptionUpdateService  {

        @Override
        public int getFormatVersionsAffectedByUpdate(List<PersistentComponentDescription> descriptions, Boolean silent) {
            return PersistentDescriptionFormatVersion.NONE;
        }

        @Override
        public List<PersistentComponentDescription> performComponentDescriptionUpdates(Integer formatVersion,
            List<PersistentComponentDescription> descriptions, Boolean silent) throws IOException {
            throw new IllegalStateException();
        }
        
    }
    
    /**
     * Dummy implementation of {@link PersistentComponentDescriptionUpdater}.
     * @author Doreen Seider
     */
    class RemotePersistentComponentDescriptionUpdateService implements PersistentComponentDescriptionUpdateService  {

        @Override
        public int getFormatVersionsAffectedByUpdate(List<PersistentComponentDescription> descriptions, Boolean silent) {
            return PersistentDescriptionFormatVersion.BEFORE_VERSON_THREE;
        }

        @Override
        public List<PersistentComponentDescription> performComponentDescriptionUpdates(Integer formatVersion,
            List<PersistentComponentDescription> descriptions, Boolean silent) throws IOException {
            List<PersistentComponentDescription> descs = new ArrayList<PersistentComponentDescription>();
            descs.add(updatedComponentDescription);
            return descs;
        }
        
    }
    
    private PersistentComponentDescription createRemoteComponentDescriptionWithUpdate() {
        PersistentComponentDescription description = EasyMock.createNiceMock(PersistentComponentDescription.class);
        EasyMock.expect(description.getComponentNodeIdentifier()).andReturn(NodeIdentifierFactory
            .fromNodeId(NODEID_WITH_UPDATE)).anyTimes();
        EasyMock.replay(description);
        return description;
    }
    
    private PersistentComponentDescription createLocalComponentDescriptionWithoutUpdate() {
        PersistentComponentDescription description = EasyMock.createNiceMock(PersistentComponentDescription.class);
        EasyMock.expect(description.getComponentNodeIdentifier()).andReturn(null).anyTimes();
        EasyMock.replay(description);
        return description;
    }
    
}
