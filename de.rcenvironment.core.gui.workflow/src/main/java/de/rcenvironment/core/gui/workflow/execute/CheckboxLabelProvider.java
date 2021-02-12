/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.execute;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;

/**
 * {@link CellLabelProvider} class providing a Check box for each row.
 * 
 * @author Goekhan Guerkan
 */
public class CheckboxLabelProvider extends
    StyledCellLabelProvider {

    private static final String COLOR = "COLOR";

    private Updatable updater;

    private List<Button> btnList;

    private boolean isSet;

    public CheckboxLabelProvider() {

        btnList = new ArrayList<Button>();
        isSet = false;
    }

    @Override
    public void update(ViewerCell cell) {
        setRowColor(cell);
        updater.updateCheckBoxColumn(cell);

    }

    void clearButtonList() {

        for (Button btn : btnList) {

            if (btn.getData(TableBehaviour.EDITOR) instanceof TableEditor) {
                TableEditor editor = (TableEditor) btn.getData(TableBehaviour.EDITOR);
                editor.dispose();
            } else {
                TreeEditor editor = (TreeEditor) btn.getData(TableBehaviour.EDITOR);
                editor.dispose();

            }

            btn.dispose();
        }

        btnList.clear();

    }

    /**
     * Sets the color for the current row.
     * 
     * @param cell the row to set the color
     * @param isSet flag value to switch the color.
     */
    private void setRowColor(ViewerCell cell) {

        Color color;

        if (!isSet) {

            if (cell.getViewerRow().getItem().getData(COLOR) == null
                || ((Color) cell.getViewerRow().getItem().getData(COLOR)).isDisposed()) {

                Color firstrow = ColorPalette.getInstance().getFirstRowColor();

                cell.getViewerRow().setBackground(0, firstrow);
                cell.getViewerRow().setBackground(1, firstrow);
                cell.getViewerRow().setBackground(2, firstrow);

                color = firstrow;
                cell.getViewerRow().getItem().setData(COLOR, color);

            } else {

                Color c = (Color) cell.getViewerRow().getItem().getData(COLOR);
                cell.getViewerRow().setBackground(0, c);
                cell.getViewerRow().setBackground(1, c);
                cell.getViewerRow().setBackground(2, c);

                color = (Color) cell.getViewerRow().getItem().getData(COLOR);

            }

            isSet = true;
        } else {

            if (cell.getViewerRow().getItem().getData(COLOR) == null) {

                Color secondRow = ColorPalette.getInstance().getSecondRowColor();

                cell.getViewerRow().setBackground(0, secondRow);
                cell.getViewerRow().setBackground(1, secondRow);
                cell.getViewerRow().setBackground(2, secondRow);

                color = secondRow;
                cell.getViewerRow().getItem().setData(COLOR, color);

            } else {

                Color c = (Color) cell.getViewerRow().getItem().getData(COLOR);

                cell.getViewerRow().setBackground(0, c);
                cell.getViewerRow().setBackground(1, c);
                cell.getViewerRow().setBackground(2, c);

                color = (Color) cell.getViewerRow().getItem().getData(COLOR);

            }
            isSet = false;
        }

        updater.setComboColor(color);

    }

    public void setUpdater(Updatable updater) {
        this.updater = updater;
    }

    public List<Button> getBtnList() {
        return btnList;
    }

}
