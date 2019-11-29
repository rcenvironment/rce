/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.communication.views.model;

import org.eclipse.swt.graphics.Image;

import de.rcenvironment.core.gui.communication.views.spi.NetworkViewContributor;
import de.rcenvironment.core.gui.communication.views.spi.SelfRenderingNetworkViewNode;

/**
 * A simple implementation of {@link SelfRenderingNetworkViewNode} that is given all its values at creation.
 * 
 * @author Robert Mischke
 */
public class SimpleNetworkViewNode implements SelfRenderingNetworkViewNode {

    private final String text;

    private final Image image;

    private final NetworkViewContributor relevantContributor;

    private final boolean hasChildren;

    private Object contextData;

    public SimpleNetworkViewNode(String text, Image image, NetworkViewContributor relevantContributor, boolean hasChildren) {
        this(text, image, null, relevantContributor, hasChildren);
    }

    public SimpleNetworkViewNode(String text, Image image, Object contextData, NetworkViewContributor relevantContributor,
        boolean hasChildren) {
        this.relevantContributor = relevantContributor;
        this.text = text;
        this.contextData = contextData;
        this.image = image;
        this.hasChildren = hasChildren;
    }

    @Override
    public NetworkViewContributor getContributor() {
        return relevantContributor;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public Image getImage() {
        return image;
    }

    @Override
    public boolean getHasChildren() {
        return hasChildren;
    }

    public Object getContextData() {
        return contextData;
    }

}
