/*
 * Copyright 2021-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.palette.view.dialogs;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.AccessibleComponentNode;
import de.rcenvironment.core.utils.common.StringUtils;


/**
 * This dialog shows information about the workflow component of the {@link AccessibleComponentNode}.
 *
 * @author Jan Flink
 */
public class ComponentInformationDialog extends TitleAreaDialog {

    private static final int STYLED_TEXT_LINE_SPACING = 2;

    private static final int STYLED_TEXT_MARGIN = 5;

    private static final String MESSAGE_STRING = "Component information about the '%s' component.";

    private static final String STRING_COLON = ":";

    private AccessibleComponentNode node;

    public ComponentInformationDialog(Shell parentShell, AccessibleComponentNode node) {
        super(parentShell);
        this.node = node;
        setDialogHelpAvailable(false);
    }

    @Override
    public void create() {
        super.create();
        setTitle(node.getDisplayName());
        setMessage(StringUtils.format(MESSAGE_STRING, node.getDisplayName()));
        ComponentInterface componentInterface = node.getComponentEntry().getComponentInterface();
        if (componentInterface.getIcon32() != null) {
            setTitleImage(
                new Image(Display.getCurrent(), new ByteArrayInputStream(componentInterface.getIcon32())));
            return;
        }
        if (componentInterface.getIcon24() != null) {
            setTitleImage(
                new Image(Display.getCurrent(), new ByteArrayInputStream(componentInterface.getIcon24())));
            return;
        }
        if (node.getIcon().isPresent()) {
            setTitleImage(node.getIcon().get());
        }
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        StyledText text = new StyledText(container, SWT.MULTI | SWT.WRAP);
        text.setEditable(false);
        text.setEnabled(false);
        text.setMargins(STYLED_TEXT_MARGIN, STYLED_TEXT_MARGIN, STYLED_TEXT_MARGIN, STYLED_TEXT_MARGIN);
        text.setLineSpacing(STYLED_TEXT_LINE_SPACING);
        text.setLayoutData(new GridData(GridData.FILL_BOTH));

        text.setText(node.getComponentInformation());
        autoformatText(text);
        
        return container;
    }

    private void autoformatText(StyledText text) {
        List<StyleRange> ranges = new ArrayList<>();
        for (int i = 0; i < text.getLineCount(); i++) {
            String line = text.getLine(i);
            ranges.add(new StyleRange(text.getOffsetAtLine(i), line.indexOf(STRING_COLON) + 1, null, null, SWT.BOLD));
        }
        text.setStyleRanges(ranges.toArray(new StyleRange[ranges.size()]));
    }


    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);
    }

}
