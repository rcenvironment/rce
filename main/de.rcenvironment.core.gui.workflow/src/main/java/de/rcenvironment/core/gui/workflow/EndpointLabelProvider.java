/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.workflow.editor.connections.EndpointTreeViewer;

/**
 * {@link LabelProvider} for the contents of the {@link EndpointTreeViewer}.
 * 
 * @author Heinrich Wendel
 */
public class EndpointLabelProvider extends LabelProvider {

    private static final int ICON_SIZE = 16;

    private Log log = LogFactory.getLog(EndpointLabelProvider.class);

    private Image componentImage = ImageManager.getInstance().getSharedImage(StandardImages.RCE_LOGO_16);

    private Image inputImage = ImageManager.getInstance().getSharedImage(StandardImages.INPUT_16);

    private Image outputImage = ImageManager.getInstance().getSharedImage(StandardImages.OUTPUT_16);

    private ImageDescriptor inputDecorationIcon = ImageDescriptor.createFromURL(
        EndpointLabelProvider.class.getResource("/resources/icons/inputDecorationArrow.gif"));

    private Image booleanIcon = ImageDescriptor.createFromURL(
        EndpointLabelProvider.class.getResource("/resources/icons/datatypes/boolean.gif")).createImage();

    private Image integerIcon = ImageDescriptor.createFromURL(
        EndpointLabelProvider.class.getResource("/resources/icons/datatypes/integer.gif")).createImage();

    private Image floatIcon = ImageDescriptor.createFromURL(
        EndpointLabelProvider.class.getResource("/resources/icons/datatypes/float.gif")).createImage();

    private Image vectorIcon = ImageDescriptor.createFromURL(
        EndpointLabelProvider.class.getResource("/resources/icons/datatypes/vector.gif")).createImage();

    private Image matrixIcon = ImageDescriptor.createFromURL(
        EndpointLabelProvider.class.getResource("/resources/icons/datatypes/matrix.gif")).createImage();

    private Image smallTableIcon = ImageDescriptor.createFromURL(
        EndpointLabelProvider.class.getResource("/resources/icons/datatypes/smallTable.gif")).createImage();

    private Image shortTextIcon = ImageDescriptor.createFromURL(
        EndpointLabelProvider.class.getResource("/resources/icons/datatypes/shortText.gif")).createImage();

    private Image fileReferenceIcon = ImageDescriptor.createFromURL(
        EndpointLabelProvider.class.getResource("/resources/icons/datatypes/file.gif")).createImage();

    private Image dateTimeIcon = ImageDescriptor.createFromURL(
        EndpointLabelProvider.class.getResource("/resources/icons/datatypes/dateTime.gif")).createImage();

    private Image directoryReferenceIcon = ImageDescriptor.createFromURL(
        EndpointLabelProvider.class.getResource("/resources/icons/datatypes/directory.gif")).createImage();

    private Map<String, Image> componentImages = new HashMap<String, Image>();

    private EndpointType type;

    private DecorationOverlayIcon decorationOverlayIcon;

    private Map<Image, Image> decoratorCache = new HashMap<>();

    public EndpointLabelProvider(EndpointType type) {
        this.type = type;
    }

    @Override
    public String getText(Object element) {
        String name;
        if (element instanceof WorkflowNode) {
            name = ((WorkflowNode) element).getName();
        } else if (element instanceof EndpointContentProvider.Endpoint) {
            name = ((EndpointContentProvider.Endpoint) element).getName();
        } else {
            name = ""; //$NON-NLS-1$
        }
        return name;
    }

    @Override
    public Image getImage(Object element) {
        Image image = null;
        if (element instanceof WorkflowNode) {
            ComponentDescription componentDesc = ((WorkflowNode) element).getComponentDescription();
            if (componentImages.containsKey(componentDesc.getIdentifier())) {
                image = componentImages.get(componentDesc.getIdentifier());
            } else {
                byte[] icon = componentDesc.getIcon16();
                if (icon != null) {
                    image = new Image(Display.getCurrent(), new ByteArrayInputStream(icon));
                    if (!image.isDisposed() && image != null) {
                        componentImages.put(componentDesc.getIdentifier(), image);
                    }
                } else {
                    image = componentImage;
                }
            }
        } else if (element instanceof EndpointContentProvider.Endpoint) {
            if (type == EndpointType.INPUT) {
                image = inputImage;
            } else {
                image = outputImage;
            }
            if (((EndpointContentProvider.Endpoint) element).getEndpointDescription().getDataType() == DataType.ShortText) {
                image = shortTextIcon;
            } else if (((EndpointContentProvider.Endpoint) element).getEndpointDescription().getDataType() == DataType.Boolean) {
                image = booleanIcon;
            } else if (((EndpointContentProvider.Endpoint) element).getEndpointDescription().getDataType() == DataType.Integer) {
                image = integerIcon;
            } else if (((EndpointContentProvider.Endpoint) element).getEndpointDescription().getDataType() == DataType.Float) {
                image = floatIcon;
            } else if (((EndpointContentProvider.Endpoint) element).getEndpointDescription().getDataType() == DataType.Vector) {
                image = vectorIcon;
            } else if (((EndpointContentProvider.Endpoint) element).getEndpointDescription().getDataType() == DataType.Matrix) {
                image = matrixIcon;
            } else if (((EndpointContentProvider.Endpoint) element).getEndpointDescription().getDataType() == DataType.SmallTable) {
                image = smallTableIcon;
            } else if (((EndpointContentProvider.Endpoint) element).getEndpointDescription().getDataType() == DataType.DateTime) {
                image = dateTimeIcon;
            } else if (((EndpointContentProvider.Endpoint) element).getEndpointDescription().getDataType() == DataType.FileReference) {
                image = fileReferenceIcon;
            } else if (((EndpointContentProvider.Endpoint) element).getEndpointDescription().getDataType() == DataType.DirectoryReference) {
                image = directoryReferenceIcon;
            }
            // mark already connected inputs
            if (type == EndpointType.INPUT & ((EndpointContentProvider.Endpoint) element).getEndpointDescription().isConnected()) {

                if (decoratorCache.keySet().contains(image)) {
                    return decoratorCache.get(image);
                } else {
                    ImageDescriptor[] decorations = new ImageDescriptor[5];
                    /*
                     * The indices of the array correspond to the values of the 5 overlay constants defined on IDecoration 0 =
                     * IDecoration.TOP_LEFT 1 = IDecoration.TOP_RIGHT 2 = IDecoration.BOTTOM_LEFT 3 = IDecoration.BOTTOM_RIGHT 4 =
                     * IDecoration.UNDERLAY
                     */
                    decorations[0] = inputDecorationIcon;
                    decorationOverlayIcon = new DecorationOverlayIcon(image, decorations, new Point(ICON_SIZE, ICON_SIZE));
                    Image originalImage = image;
                    image = decorationOverlayIcon.createImage();
                    decoratorCache.put(originalImage, image);
                    return image;
                }
            }
        }
        if (image == null || image.isDisposed()) {
            log.warn("Image for " + element + " is null or disposed.");
        }
        return image;
    }

    @Override
    public void dispose() {
        shortTextIcon.dispose();
        booleanIcon.dispose();
        integerIcon.dispose();
        floatIcon.dispose();
        vectorIcon.dispose();
        matrixIcon.dispose();
        smallTableIcon.dispose();
        dateTimeIcon.dispose();
        fileReferenceIcon.dispose();
        directoryReferenceIcon.dispose();
        for (Image image : componentImages.values()) {
            image.dispose();
        }
        for (Image image : decoratorCache.values()) {
            image.dispose();
        }
        decoratorCache.clear();
    }
}
