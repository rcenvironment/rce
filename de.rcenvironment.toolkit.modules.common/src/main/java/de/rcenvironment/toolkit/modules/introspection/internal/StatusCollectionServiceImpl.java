/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.introspection.internal;

import java.util.ArrayList;
import java.util.List;

import de.rcenvironment.toolkit.modules.introspection.api.StatusCollectionContributor;
import de.rcenvironment.toolkit.modules.introspection.api.StatusCollectionRegistry;
import de.rcenvironment.toolkit.modules.introspection.api.StatusCollectionService;
import de.rcenvironment.toolkit.utils.internal.StringUtils;
import de.rcenvironment.toolkit.utils.text.impl.BufferingTextLinesReceiver;
import de.rcenvironment.toolkit.utils.text.impl.MultiLineOutputWrapper;

/**
 * Default {@link StatusCollectionService} implementation.
 * 
 * @author Robert Mischke
 */
public class StatusCollectionServiceImpl implements StatusCollectionService, StatusCollectionRegistry {

    private final List<StatusCollectionContributor> contributors = new ArrayList<>();

    @Override
    public synchronized MultiLineOutputWrapper getCollectedDefaultStateInformation() {
        final BufferingTextLinesReceiver buffer = new BufferingTextLinesReceiver();
        for (StatusCollectionContributor c : contributors) {
            final String description = c.getStandardDescription();
            if (description == null) {
                continue;
            }
            buffer.addLine(StringUtils.format("=== %s ===", description));
            c.printDefaultStateInformation(buffer);
        }
        return new MultiLineOutputWrapper(buffer.getCollectedLines()); // safe as the wrapper object is discarded, so no further writes
                                                                           // can happen
    }

    @Override
    public synchronized MultiLineOutputWrapper getCollectedUnfinishedOperationsInformation() {
        final BufferingTextLinesReceiver buffer = new BufferingTextLinesReceiver();
        for (StatusCollectionContributor c : contributors) {
            final String description = c.getUnfinishedOperationsDescription();
            if (description == null) {
                continue;
            }
            buffer.addLine(StringUtils.format("=== %s ===", description));
            c.printUnfinishedOperationsInformation(buffer);
        }
        return new MultiLineOutputWrapper(buffer.getCollectedLines()); // safe as the wrapper object is discarded, so no further writes
                                                                           // can happen
    }

    @Override
    public synchronized void addContributor(StatusCollectionContributor contributor) {
        contributors.add(contributor);
    }

}
