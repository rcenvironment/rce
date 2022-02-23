/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.update.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;

import de.rcenvironment.core.component.update.api.DistributedPersistentComponentDescriptionUpdateService;
import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.update.api.PersistentWorkflowDescription;

/**
 * Test cases for {@link PersistentWorkflowDescriptionUpdateServiceImpl}.
 * 
 * @author Sascha Zur
 * @author Robert Mischke (8.0.0 id adaptations)
 */
public class PersistentWorkflowDescriptionUpdateServiceImplTest {

    /**
     * Tests if the workflow version is updated correctly.
     * 
     * @throws IOException on error
     * @throws JsonParseException on error
     */
    @SuppressWarnings("unchecked")
    @Test
    public void test() throws JsonParseException, IOException {
        String persistentDescriptionV3 = "{"
            + "\"identifier\" : \"697261b6-eaf5-44ab-af40-6c161a4f26f8\","
            + "\"workflowVersion\" : \"3\""
            + "}";

        List<PersistentComponentDescription> descriptions = new ArrayList<PersistentComponentDescription>();

        PersistentWorkflowDescription persistentWorkflowDescriptionV3 =
            new PersistentWorkflowDescription(descriptions, persistentDescriptionV3);

        PersistentWorkflowDescriptionUpdateServiceImpl updateService = new PersistentWorkflowDescriptionUpdateServiceImpl();
        DistributedPersistentComponentDescriptionUpdateService componentUpdateServiceMock = EasyMock
            .createMock(DistributedPersistentComponentDescriptionUpdateService.class);
        List<PersistentComponentDescription> componentDescriptions = new LinkedList<PersistentComponentDescription>();
        EasyMock.expect(componentUpdateServiceMock.performComponentDescriptionUpdates(EasyMock.anyInt(),
            EasyMock.anyObject(new LinkedList<PersistentComponentDescription>().getClass()),
            EasyMock.anyBoolean())).andReturn(componentDescriptions).anyTimes();
        EasyMock.replay(componentUpdateServiceMock);

        updateService.bindComponentDescriptionUpdateService(componentUpdateServiceMock);
        persistentWorkflowDescriptionV3 = updateService.performWorkflowDescriptionUpdate(persistentWorkflowDescriptionV3);
        Assert.assertEquals(String.valueOf(WorkflowConstants.CURRENT_WORKFLOW_VERSION_NUMBER),
            persistentWorkflowDescriptionV3.getWorkflowVersion());

    }
}
