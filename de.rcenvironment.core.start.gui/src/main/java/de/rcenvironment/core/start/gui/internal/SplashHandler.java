/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start.gui.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.branding.IProductConstants;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.splash.BasicSplashHandler;

import de.rcenvironment.core.start.Application;

/**
 * Override splash screen.
 * 
 * @author Sascha Zur
 */
public class SplashHandler extends BasicSplashHandler {

    private static final String SPLASHIMAGE = "splash.bmp";

    private static final String BUNDLE = "de.rcenvironment.core.start";

    private static final String BUNDLE_VERSION = Platform.getBundle(BUNDLE).getHeaders().get("Bundle-Version").toString();

    private static final int GRAY = 117;

    private static final int WHITE = 255;

    private static final int XCOORD = 246;

    private static final int YCOORD = 224;

    private static String releasename;

    @Override
    public void init(Shell splash) {
        super.init(splash);
        /* Load the Release Name from about.mappings of bundle 'de.rcenvironment.core.start' */
        Properties properties = new Properties();
        InputStream resourceAsStream = Application.class.getResourceAsStream("/about.mappings");
        try {
            properties.load(resourceAsStream);
            releasename = " (" + properties.getProperty("1") + ")";
        } catch (IOException e1) {
            e1 = null;
        }

        ImageDescriptor descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(BUNDLE, SPLASHIMAGE);
        Image splashImg = descriptor.createImage();
        GC gc = new GC(splashImg);
        gc.setFont(new Font(gc.getDevice(), "Arial", 7, SWT.BOLD));
        // FIXME: the newly created color object will never be disposed
        gc.setForeground(new Color(null, GRAY, GRAY, GRAY));
        gc.setBackground(new Color(null, WHITE, WHITE, WHITE));
        gc.drawText("Version " + BUNDLE_VERSION.substring(0, BUNDLE_VERSION.lastIndexOf('.')) + releasename, XCOORD, YCOORD);
        splash.setBackgroundMode(SWT.INHERIT_FORCE);
        splash.setBackgroundImage(splashImg);
        splash.addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent e) {
                e.gc.setFont(new Font(e.gc.getDevice(), "Arial", 8, SWT.BOLD));
                e.gc.setForeground(new Color(null, GRAY, GRAY, GRAY));
                e.gc.setBackground(new Color(null, WHITE, WHITE, WHITE));
                e.gc.drawText("Version " + BUNDLE_VERSION.substring(0, BUNDLE_VERSION.lastIndexOf('.')), XCOORD, YCOORD);
            }
        });

        String progressRectString = null;
        IProduct product = Platform.getProduct();
        if (product != null) {
            progressRectString = product
                .getProperty(IProductConstants.STARTUP_PROGRESS_RECT);
        }
        Rectangle progressRect = StringConverter.asRectangle(
            progressRectString, new Rectangle(0, 0, 0, 0));
        setProgressRect(progressRect);
        getContent(); // ensure creation of the progress
    }
}
