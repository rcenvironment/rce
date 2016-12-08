/*
 * Copyright (C) 2006-2016 DLR, Germany
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

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.communication.testutils.CommunicationServiceDefaultStub;
import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.update.api.PersistentDescriptionFormatVersion;
import de.rcenvironment.core.component.update.api.RemotablePersistentComponentDescriptionUpdateService;
import de.rcenvironment.core.component.update.spi.PersistentComponentDescriptionUpdater;

/**
 * Test cases for {@link DistributedPersistentComponentDescriptionUpdateServiceImpl}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (8.0.0 id adaptations)
 */
public class DistributedPersistentComponentDescriptionUpdateServiceImplTest {

    private static final InstanceNodeSessionId NODE_ID_WITH_UPDATE = NodeIdentifierTestUtils
        .createTestInstanceNodeSessionIdWithDisplayName("with update");

    private PersistentComponentDescription updatedComponentDescription = EasyMock.createNiceMock(PersistentComponentDescription.class);

    /**
     * Test.
     * 
     * @throws IOException on error.
     */
    @Test
    public void test() throws IOException {

        DistributedPersistentComponentDescriptionUpdateServiceImpl updaterService =
            new DistributedPersistentComponentDescriptionUpdateServiceImpl();

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
     * 
     * @author Doreen Seider
     */
    class TestCommunicationService extends CommunicationServiceDefaultStub {

        @Override
        public <T> T getRemotableService(Class<T> iface, ResolvableNodeId nodeId) {
            if (nodeId == null) {
                return (T) new LocalComponentDescriptionUpdateService();
            } else if (nodeId.isSameInstanceNodeAs(NODE_ID_WITH_UPDATE)) {
                return (T) new RemoteComponentDescriptionUpdateService();
            }
            return null;
        }
    }

    /**
     * Dummy implementation of {@link PersistentComponentDescriptionUpdater}.
     * 
     * @author Doreen Seider
     */
    class LocalComponentDescriptionUpdateService implements RemotablePersistentComponentDescriptionUpdateService {

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
     * 
     * @author Doreen Seider
     */
    class RemoteComponentDescriptionUpdateService implements RemotablePersistentComponentDescriptionUpdateService {

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
        EasyMock.expect(description.getComponentNodeIdentifier()).andReturn(NODE_ID_WITH_UPDATE.convertToDefaultLogicalNodeId()).anyTimes();
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
