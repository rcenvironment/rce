/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.cluster.view.internal;

import java.util.Set;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import de.rcenvironment.core.utils.cluster.ClusterJobInformation;

/**
 * Take the whole content to structured pieces.
 * 
 * @author Doreen Seider
 */
public class ClusterJobInformationContentProvider implements IStructuredContentProvider {

    @Override
    public Object[] getElements(final Object inputElement) {
        if (!(inputElement instanceof ClusterJobInformationModel)) {
            throw new IllegalArgumentException();
        }
        final ClusterJobInformationModel jobInformationModel = (ClusterJobInformationModel) inputElement;
        final Set<ClusterJobInformation> jobInformationEntries = jobInformationModel.getClusterJobInformation();
        return jobInformationEntries.toArray();
    }

    @Override
    public void dispose() {
        // do nothing
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        // do nothing
    }

}
