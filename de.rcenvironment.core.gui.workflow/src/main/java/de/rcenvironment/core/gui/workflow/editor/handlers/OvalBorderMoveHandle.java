/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.editor.handlers;

import org.eclipse.draw2d.Border;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.handles.MoveHandle;

/**
 * A new handle for oval borders.
 * 
 * @author Sascha Zur
 * @author Oliver Seebach
 * 
 */
public class OvalBorderMoveHandle extends MoveHandle {

    private static final int BORDERWIDTH = 2;

    public OvalBorderMoveHandle(GraphicalEditPart owner) {
        super(owner);
    }

    @Override
    public Border getBorder() {
        RoundedLineBorder border = new RoundedLineBorder();
        border.setWidth(BORDERWIDTH);
        return border;
    }

    @Override
    protected void paintFigure(Graphics graphics) {
        graphics.drawOval(new Rectangle(getBounds().x, getBounds().y, getBounds().width - 1, getBounds().height - 1));
    }
    
    
    /**
     * Oval shaped border.
     *
     * @author Oliver Seebach
     */
    class RoundedLineBorder extends LineBorder {

        RoundedLineBorder() {
            super();
        }   
        

        /**
         * {@inheritDoc}
         *
         * @see org.eclipse.draw2d.LineBorder#getInsets(org.eclipse.draw2d.IFigure)
         */
        @Override
        public Insets getInsets(IFigure figure) {
            return new Insets(getWidth());
        }
        
        /**
         * {@inheritDoc}
         *
         * @see org.eclipse.draw2d.LineBorder#paint(org.eclipse.draw2d.IFigure, org.eclipse.draw2d.Graphics,
         *  org.eclipse.draw2d.geometry.Insets)
         */
        @Override
        public void paint(IFigure figure, Graphics graphics, Insets insets) {
            Rectangle rect = getPaintRectangle(figure, insets);
            rect.setSize(rect.width - 1, rect.height - 1); // -1 as oval doesn't fit otherwise
            tempRect.setBounds(rect);
            tempRect.shrink(getWidth() / 2, getWidth() / 2);

            graphics.setLineWidth(getWidth());
            graphics.setLineStyle(getStyle());
            if (getColor() != null) {
                graphics.setForegroundColor(getColor());
            } else {
                graphics.setForegroundColor(figure.getForegroundColor());
            }

            graphics.drawOval(tempRect);
        }
        
    }
}
