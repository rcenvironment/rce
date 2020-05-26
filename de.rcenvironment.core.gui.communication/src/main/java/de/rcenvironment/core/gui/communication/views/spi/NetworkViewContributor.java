/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.communication.views.spi;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;

import de.rcenvironment.core.gui.communication.views.NetworkView;
import de.rcenvironment.core.gui.communication.views.model.NetworkGraphNodeWithContext;
import de.rcenvironment.core.gui.communication.views.model.NetworkViewModel;

/**
 * Defines a contributor of {@link NetworkView} tree elements. Elements can currently be added at the root level (which creates a completely
 * new subtree), or below each discovered network instance/node's representation.
 * 
 * @author Robert Mischke
 */
public interface NetworkViewContributor {

    /**
     * Injects the current {@link NetworkViewModel} to fetch data from. Currently, a single model instance is used for the whole lifetime of
     * the {@link NetworkView}.
     * 
     * @param currentModel the model instance
     */
    void setCurrentModel(NetworkViewModel currentModel);

    /**
     * Injects the current {@link TreeViewer} to update tree in contributor implementatios.
     * 
     * @param viewer The {@link TreeViewer} of the {@link NetworkView}.
     */
    void setTreeViewer(TreeViewer viewer);

    /**
     * @return the ordering value (lower values = more to the top) to define the position of the contributions to the root (first) level of
     *         the tree; a value of 0 means that this contributor does not provide any root-level elements
     */
    int getRootElementsPriority();

    /**
     * @param parentNode the parent node; the contributor MUST only add its children if it recognizes its anchor point
     * @return the contributed root level elements; typically, these are "folder" nodes
     */
    Object[] getTopLevelElements(Object parentNode);

    /**
     * @return the ordering value (lower values = more to the top) to define the position of the contributions below a network instance's
     *         node; a value of 0 means that this contributor does not provide any network instance child elements
     */
    int getInstanceDataElementsPriority();

    /**
     * @param instanceNode the network instance's node to fetch data from, if necessary
     * @return the contributed child elements; most child elements are "folder" nodes
     */
    Object[] getChildrenForNetworkInstanceNode(NetworkGraphNodeWithContext instanceNode);

    /**
     * @param parentNode the parent node; typically, a {@link ContributedNetworkViewNode}
     * @return whether this element has child nodes
     */
    boolean hasChildren(Object parentNode);

    /**
     * @param parentNode the parent node; typically, a {@link ContributedNetworkViewNode}
     * @return the children of the given parent node
     */
    Object[] getChildren(Object parentNode);

    /**
     * @param node the node to get the parent node of
     * @return the parent node; should not be null
     */
    Object getParent(Object node);

    /**
     * @param node the node to get the display text for
     * @return the display text
     */
    String getText(Object node);

    /**
     * @param node the node to get the image for
     * @return the image; note: take care to reuse images where possible, and when to dispose them
     */
    Image getImage(Object node);

    /**
     * Disposes internal resources - typically, {@link Image} instance.
     */
    void dispose();

}
