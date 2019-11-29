/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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

import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.update.api.PersistentDescriptionFormatVersion;
import de.rcenvironment.core.component.update.spi.PersistentComponentDescriptionUpdater;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Test cases for {@link PersistentComponentDescriptionUpdateServiceImpl}.
 * @author Doreen Seider
 */
public class PersistentComponentDescriptionUpdateServiceImplTest {

    private static final String[] COMPID_WITH_UPDATE = new String[] { "comp.id.update" };
    
    private static final String[] COMPID_WITHOUT_UPDATE = new String[] { "comp.id.noupdate" };
    
    private static final String[] COMPID_WITH_SILENT_UPDATE = new String[] { "comp.id.silentupdate" };
    
    private static final String[] COMPID_WITHOUT_SILENT_UPDATE = new String[] { "comp.id.silentnoupdate" };
    
    private PersistentComponentDescription updatedComponentDescription = EasyMock.createNiceMock(PersistentComponentDescription.class);
    
    /** 
     * Test. 
     * @throws IOException on unexpected errors
     * @throws RemoteOperationException on unexpected errors
     **/
    @Test
    public void test() throws IOException, RemoteOperationException {
        
        PersistentComponentDescriptionUpdater updater = new Updater();
        PersistentComponentDescriptionUpdater nonUpdater = new NonUpdater();
        PersistentComponentDescriptionUpdater silentUpdater = new SilentUpdater();
        PersistentComponentDescriptionUpdater silentNonUpdater = new SilentNonUpdater();
        
        PersistentComponentDescriptionUpdateServiceImpl updaterService = new PersistentComponentDescriptionUpdateServiceImpl();
        
        updaterService.addPersistentComponentDescriptionUpdater(updater);
        updaterService.addPersistentComponentDescriptionUpdater(nonUpdater);
        updaterService.addPersistentComponentDescriptionUpdater(silentUpdater);
        updaterService.addPersistentComponentDescriptionUpdater(silentNonUpdater);
        
        List<PersistentComponentDescription> descriptions = new ArrayList<PersistentComponentDescription>();
        PersistentComponentDescription descriptionWithoutUpdate = createComponentDescriptionWithoutUpdate();
        descriptions.add(descriptionWithoutUpdate);

        // must return false, because only NonUpdater should be asked via hasPersistentComponentDescriptionUpdate()
        assertEquals(PersistentDescriptionFormatVersion.NONE,
            updaterService.getFormatVersionsAffectedByUpdate(descriptions, false));
        // must return false, because only NonUpdater should be asked via hasPersistentComponentDescriptionUpdate() and is not silent
        assertEquals(PersistentDescriptionFormatVersion.NONE,
            updaterService.getFormatVersionsAffectedByUpdate(descriptions, true));

        PersistentComponentDescription descriptionWithUpdate = createComponentDescriptionWithUpdate();
        descriptions.add(descriptionWithUpdate);

        // must return true, because also Updater should be asked via hasPersistentComponentDescriptionUpdate()
        assertEquals(PersistentDescriptionFormatVersion.BEFORE_VERSON_THREE,
            updaterService.getFormatVersionsAffectedByUpdate(descriptions, false));
        // must return false, because also Updater should be asked via hasPersistentComponentDescriptionUpdate() but is not silent
        assertEquals(PersistentDescriptionFormatVersion.NONE,
            updaterService.getFormatVersionsAffectedByUpdate(descriptions, true));
        

        PersistentComponentDescription descriptionWithoutSilentUpdate = createComponentDescriptionWithoutSilentUpdate();
        descriptions.add(descriptionWithoutSilentUpdate);
        // must return false because there is no Updater with a silent update
        assertEquals(PersistentDescriptionFormatVersion.NONE,
            updaterService.getFormatVersionsAffectedByUpdate(descriptions, true));
        
        PersistentComponentDescription descriptionWithSilentUpdate = createComponentDescriptionWithSilentUpdate();
        descriptions.add(descriptionWithSilentUpdate);
        // must return true because now there is an Updater with a silent update
        assertEquals(PersistentDescriptionFormatVersion.BEFORE_VERSON_THREE,
            updaterService.getFormatVersionsAffectedByUpdate(descriptions, true));
        
        // fails if an performPersistentComponentDescriptionUpdate() of NonUpdater is called and thus, IllegalStateException is thrown
        List<PersistentComponentDescription> udpatedDescriptions = updaterService
            .performComponentDescriptionUpdates(PersistentDescriptionFormatVersion.BEFORE_VERSON_THREE, descriptions, false);
        assertEquals(4, udpatedDescriptions.size());
        assertTrue(udpatedDescriptions.contains(updatedComponentDescription));
        assertTrue(udpatedDescriptions.contains(descriptionWithoutUpdate));
        assertFalse(udpatedDescriptions.contains(descriptionWithUpdate));
        
        updaterService.removePersistentComponentDescriptionUpdater(updater);

    }
    
    private PersistentComponentDescription createComponentDescriptionWithUpdate() {
        PersistentComponentDescription description = EasyMock.createNiceMock(PersistentComponentDescription.class);
        EasyMock.expect(description.getComponentIdentifier()).andReturn(COMPID_WITH_UPDATE[0]).anyTimes();
        EasyMock.replay(description);
        return description;
    }
    private PersistentComponentDescription createComponentDescriptionWithSilentUpdate() {
        PersistentComponentDescription description = EasyMock.createNiceMock(PersistentComponentDescription.class);
        EasyMock.expect(description.getComponentIdentifier()).andReturn(COMPID_WITH_SILENT_UPDATE[0]).anyTimes();
        EasyMock.replay(description);
        return description;
    }
    private PersistentComponentDescription createComponentDescriptionWithoutUpdate() {
        PersistentComponentDescription description = EasyMock.createNiceMock(PersistentComponentDescription.class);
        EasyMock.expect(description.getComponentIdentifier()).andReturn(COMPID_WITHOUT_UPDATE[0]).anyTimes();
        EasyMock.replay(description);
        return description;
    }
    private PersistentComponentDescription createComponentDescriptionWithoutSilentUpdate() {
        PersistentComponentDescription description = EasyMock.createNiceMock(PersistentComponentDescription.class);
        EasyMock.expect(description.getComponentIdentifier()).andReturn(COMPID_WITHOUT_SILENT_UPDATE[0]).anyTimes();
        EasyMock.replay(description);
        return description;
    }
    
    /**
     * Dummy class representing updater which has an update.
     * @author Doreen Seider
     */
    class Updater implements PersistentComponentDescriptionUpdater {

        @Override
        public String[] getComponentIdentifiersAffectedByUpdate() {
            return COMPID_WITH_UPDATE;
        }

        @Override
        public int getFormatVersionsAffectedByUpdate(String persistentComponentDescriptionVersion, boolean silent) {
            if (silent){
                return PersistentDescriptionFormatVersion.NONE;
            }
            return PersistentDescriptionFormatVersion.BEFORE_VERSON_THREE;
        }

        @Override
        public PersistentComponentDescription performComponentDescriptionUpdate(int formatVersion,
            PersistentComponentDescription description, boolean silent)
            throws IOException {
            return updatedComponentDescription;
        }
        
    }
    
    /**
     * Dummy class representing updater which hasn't update.
     * @author Doreen Seider
     */
    class NonUpdater implements PersistentComponentDescriptionUpdater {

        @Override
        public String[] getComponentIdentifiersAffectedByUpdate() {
            return COMPID_WITHOUT_UPDATE;
        }

        @Override
        public int getFormatVersionsAffectedByUpdate(String persistentComponentDescriptionVersion, boolean silent) {
            if (silent) {
                return PersistentDescriptionFormatVersion.NONE;
            }
            return PersistentDescriptionFormatVersion.NONE;
        }

        @Override
        public PersistentComponentDescription performComponentDescriptionUpdate(int formatVersion,
            PersistentComponentDescription description, boolean silent)
            throws IOException {
            throw new IllegalStateException("should not be called because of having no update");
        }
        
    }
    
    /**
     * Dummy class representing updater which has an silent update.
     * @author Sascha Zur
     */
    class SilentUpdater implements PersistentComponentDescriptionUpdater {

        @Override
        public String[] getComponentIdentifiersAffectedByUpdate() {
            return COMPID_WITH_SILENT_UPDATE;
        }

        @Override
        public int getFormatVersionsAffectedByUpdate(String persistentComponentDescriptionVersion, boolean silent) {
            if (silent) {
                return PersistentDescriptionFormatVersion.BEFORE_VERSON_THREE;
            } 
            return PersistentDescriptionFormatVersion.NONE;
        }

        @Override
        public PersistentComponentDescription performComponentDescriptionUpdate(int formatVersion,
            PersistentComponentDescription description, boolean silent)
            throws IOException {
            return updatedComponentDescription;
        }
        
    }
 
    /**
     * Dummy class representing updater which hasn't silent update.
     * @author Sascha Zur
     */
    class SilentNonUpdater implements PersistentComponentDescriptionUpdater {

        
        @Override
        public String[] getComponentIdentifiersAffectedByUpdate() {
            return COMPID_WITHOUT_SILENT_UPDATE;
        }

        @Override
        public int getFormatVersionsAffectedByUpdate(String persistentComponentDescriptionVersion, boolean silent) {
            if (silent) {
                return PersistentDescriptionFormatVersion.NONE;
            }
            return PersistentDescriptionFormatVersion.NONE;
        }

        @Override
        public PersistentComponentDescription performComponentDescriptionUpdate(int formatVersion,
            PersistentComponentDescription description, boolean silent)
            throws IOException {
            throw new IllegalStateException("should not be called because of having no update");
        }
        
    }
}
