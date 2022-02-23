/*
 * Copyright 2021-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.palette;

import java.util.Collection;

import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.spi.DistributedComponentKnowledgeListener;
import de.rcenvironment.core.gui.palette.view.PaletteView;
import de.rcenvironment.core.gui.palette.view.PaletteViewContentProvider;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;

/**
 * 
 * Listener to update the {@link PaletteView} on {@link DistributedComponentKnowledge} changed.
 * 
 * @author Kathrin Schaffert
 *
 */
public class ComponentKnowledgeUpdateListener implements DistributedComponentKnowledgeListener {

    private static final int DELAY = 1000;

    private final AsyncTaskService taskService;

    private final PaletteViewContentProvider contentProvider;

    private Collection<DistributedComponentEntry> scheduledNewState = null;

    private DistributedComponentKnowledgeSanitizer sanitizer = new DistributedComponentKnowledgeSanitizer();

    private boolean isPaletteUpdateScheduled = false;

    public ComponentKnowledgeUpdateListener(PaletteViewContentProvider contentProvider) {
        this.taskService = ServiceRegistry.createPublisherAccessFor(this).getService(AsyncTaskService.class);
        this.contentProvider = contentProvider;
    }

    @Override
    public synchronized void onDistributedComponentKnowledgeChanged(DistributedComponentKnowledge newState) {
        scheduledNewState = sanitizer.sanitizeComponentKnowledge(newState);
        if (!isPaletteUpdateScheduled) {
            taskService.scheduleAfterDelay("update Palette", this::performUpdate, DELAY);
            isPaletteUpdateScheduled = true;
        }
    }

    private synchronized void performUpdate() {
        if (!contentProvider.getPaletteView().getPaletteTreeViewer().getTree().isDisposed()) {
            contentProvider.refreshPaletteView(scheduledNewState);
        }
        isPaletteUpdateScheduled = false;
    }
}
