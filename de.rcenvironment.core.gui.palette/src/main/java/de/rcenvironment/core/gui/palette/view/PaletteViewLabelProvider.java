/*
 * Copyright 2021-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.palette.view;

import java.util.Optional;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;

import de.rcenvironment.core.gui.palette.view.palettetreenodes.AccessibleComponentNode;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.PaletteTreeNode;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Label Provider for Palette view's TreeViewer.
 *
 * @author Kathrin Schaffert
 * @author Jan Flink
 */
public class PaletteViewLabelProvider extends CellLabelProvider {

    private static final int TOOLTTIP_DISPLAY_DELAY = 1500;

    private static final String ERROR_FALLBACK_LABEL_TEXT = "<Error>";

    private static final String STRING_NAME_WITH_SHORT_KEY = "%s (%s)";

    private Image getDefaultImage() {
        return ImageManager.getInstance().getSharedImage(StandardImages.RCE_LOGO_16);
    }

    @Override
    public void update(ViewerCell cell) {
        if (cell.getElement() instanceof PaletteTreeNode) {
            PaletteTreeNode node = (PaletteTreeNode) cell.getElement();
            Optional<String> shortKey = node.getShortKey();
            if (shortKey.isPresent()) {
                cell.setText(StringUtils.format(STRING_NAME_WITH_SHORT_KEY, node.getDisplayName(), shortKey.get()));
            } else {
                cell.setText(node.getDisplayName());
            }
            cell.setImage(node.getIcon().orElseGet(this::getDefaultImage));
        } else {
            cell.setText(ERROR_FALLBACK_LABEL_TEXT);
        }
    }

    @Override
    public String getToolTipText(Object element) {
        if (element instanceof AccessibleComponentNode) {
            AccessibleComponentNode node = (AccessibleComponentNode) element;
            return node.getComponentTooltip();
        }
        return super.getToolTipText(element);
    }

    @Override
    public int getToolTipDisplayDelayTime(Object object) {
        return TOOLTTIP_DISPLAY_DELAY;
    }
}
