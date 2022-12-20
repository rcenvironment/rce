/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.integration.common.editor;

/**
 * Integration Editor Page Interface.
 *
 * @author Jan Flink
 */
public interface IIntegrationEditorPage {

    boolean hasChanges();

    boolean isPageValid();

    void setBackButtonEnabled(boolean enable);

    void setNextButtonEnabled(boolean enable);

    void setSaveButtonEnabled(boolean enable);

}
