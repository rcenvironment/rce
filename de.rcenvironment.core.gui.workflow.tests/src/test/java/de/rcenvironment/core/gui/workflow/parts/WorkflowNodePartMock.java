/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.workflow.parts;

import org.easymock.EasyMock;
import org.eclipse.draw2d.IFigure;
import org.eclipse.swt.graphics.Image;

import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Mock class to use in tests instead of an actual {@link WorkflowNodePart}. Since that class uses some heavy dependencies during
 * construction, we cannot easily use EasyMock to mock this class at the point where it is used. Instead, we provide this mock to use in
 * tests of objects that expect a WorkflowNodePart
 * 
 * @author Alexander Weinert
 */
public class WorkflowNodePartMock extends WorkflowNodePart {

    @Override
    protected ServiceRegistryAccess getServiceRegistryAccess() {
        return EasyMock.createNiceMock(ServiceRegistryAccess.class);
    }

    @Override
    protected Image getErrorImage() {
        return null;
    }

    @Override
    protected Image getWarningImage() {
        return null;
    }

    @Override
    protected Image getLocalImage() {
        return null;
    }

    @Override
    protected Image getImitationModeImage() {
        return null;
    }

    @Override
    protected Image getDeprecatedImage() {
        return null;
    }

    @Override
    protected Image getSharedImage(StandardImages standardImage) {
        return null;
    }

    @Override
    protected IFigure createImageFigure(Image image, int offsetX, int offsetY, int size, boolean visible) {
        return null;
    }

    @Override
    protected IFigure createImageFigure(Image image, int offsetX, int offsetY, int size, String tooltip, boolean visible) {
        return null;
    }

}
