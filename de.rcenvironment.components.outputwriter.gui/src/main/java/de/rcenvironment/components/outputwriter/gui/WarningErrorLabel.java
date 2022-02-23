/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.gui;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;

/**
 * A composite that is able to show both errors and warnings. To this end, it renders two CLabels, which are labelled with symbols for error
 * and warning, respectively. If no error or warning is present, the respective label is not shown.
 * 
 * @author Alexander Weinert
 */
public class WarningErrorLabel extends Composite {

    private static final int COMPOSITE_HEIGHT_HINT = 45;

    private final Text upperText;

    private final Text lowerText;

    private final CLabel upperLabel;

    private final CLabel lowerLabel;

    private final List<String> errors = new LinkedList<>();

    private final List<String> warnings = new LinkedList<>();

    public WarningErrorLabel(Composite parent, int style) {
        super(parent, style);

        this.setVisible(true);

        final GridLayout gd = new GridLayout(2, false);
        gd.marginWidth = 0;
        final GridData thisGridData =
            new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        this.setLayout(gd);
        this.setLayoutData(thisGridData);

        GridData labelGridData =
            new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        labelGridData.heightHint = COMPOSITE_HEIGHT_HINT;
        GridLayout configGroupLayout = new GridLayout();
        configGroupLayout.marginWidth = 0;

        upperLabel = new CLabel(this, SWT.NONE);
        upperLabel.setText("xxxxxxxxxErrors"); // xxx space needed, so that the warning image is displayed correctly
        upperText = new Text(this, SWT.READ_ONLY | SWT.V_SCROLL);
        upperText.setLayoutData(labelGridData);

        lowerLabel = new CLabel(this, SWT.NONE);
        lowerLabel.setText("xxxxxWarnings"); // xxx space needed, so that the warning image is displayed correctly
        lowerText = new Text(this, SWT.READ_ONLY | SWT.V_SCROLL);
        lowerText.setLayoutData(labelGridData);

        refresh();
    }

    /**
     * Appends the given warning to the currently displayed ones.
     * 
     * @param warning The warning to be displayed.
     */
    public void addWarning(String warning) {
        this.warnings.add(warning);
        refresh();
    }

    /**
     * Appends the given set of warnings to the currently displayed ones.
     * 
     * @param warningsParam The warnings do be displayed.
     */
    public void addWarnings(Collection<String> warningsParam) {
        this.warnings.addAll(warningsParam);
        refresh();
    }

    /**
     * After calling this method, no more warnings are shown.
     */
    public void clearWarnings() {
        this.warnings.clear();
        refresh();
    }

    /**
     * Appends the given error to the currently displayed ones.
     * 
     * @param error The error to be displayed.
     */
    public void addError(String error) {
        this.errors.add(error);
        refresh();
    }

    /**
     * Appends the given set of errors to the currently displayed ones.
     * 
     * @param errorsParam The errors do be displayed.
     */
    public void addErrors(Collection<String> errorsParam) {
        this.errors.addAll(errorsParam);
        refresh();
    }

    /**
     * After calling this method, no more errors are shown.
     */
    public void clearErrors() {
        this.errors.clear();
        refresh();
    }

    /**
     * Refresh the widgets to correctly display the warnings and errors stored in this object.
     */
    private void refresh() {
        final Optional<Text> errorText;
        final Optional<Text> warningText;
        final Optional<CLabel> errorLabel;
        final Optional<CLabel> warningLabel;


        // Figure out which label to use for errors and which for warnings. If there are both errors and warnings, we first show errors,
        // then warnings. If there is only either of them, we always use the upper label.
        if (errors.isEmpty()) {
            errorText = Optional.empty();
            errorLabel = Optional.empty();
            if (warnings.isEmpty()) {
                warningText = Optional.empty();
                warningLabel = Optional.empty();
            } else {
                warningText = Optional.of(upperText);
                warningLabel = Optional.of(upperLabel);
            }
        } else {
            errorText = Optional.of(upperText);
            errorLabel = Optional.of(upperLabel);
            if (warnings.isEmpty()) {
                warningText = Optional.empty();
                warningLabel = Optional.empty();
            } else {
                warningText = Optional.of(lowerText);
                warningLabel = Optional.of(lowerLabel);
            }
        }

        upperText.setVisible(false);
        lowerText.setVisible(false);
        upperLabel.setVisible(false);
        lowerLabel.setVisible(false);

        if (errorText.isPresent()) {
            errorLabel.get().setImage(ImageManager.getInstance().getSharedImage(StandardImages.ERROR_16));
            errorLabel.get().setText("Errors:");
            errorLabel.get().setVisible(true);
            errorText.get().setText(String.join("\n", errors));
            errorText.get().setVisible(true);
        }

        if (warningText.isPresent()) {
            warningLabel.get().setImage(ImageManager.getInstance().getSharedImage(StandardImages.WARNING_16));
            warningLabel.get().setText("Warnings:");
            warningLabel.get().setVisible(true);
            warningText.get().setText(String.join("\n", warnings));
            warningText.get().setVisible(true);
        }
    }
}
