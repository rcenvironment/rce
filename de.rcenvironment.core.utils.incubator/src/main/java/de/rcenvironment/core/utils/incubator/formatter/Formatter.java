/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator.formatter;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Formats a table of the class ArrayBasedDataTable. Each cell is aligned depending on the content of the given Alignment array.
 *
 * @author Adrian Stock
 * @author Robert Mischke (minor tweaks)
 */
public class Formatter extends ArrayBasedDataTable {

    /**
     * @param table will be aligned in dependency of the given Alignments
     * @return the rendered table.
     */
    public final StringBuilder[] renderTable(DataTable table) {
        StringBuilder[] newTable = new StringBuilder[table.getSizeOfTable()];
        StringBuilder newCell;
        setAmountOfSpace(table);
        for (int i = 0; i < table.getSizeOfTable(); i++) {
            newTable[i] = new StringBuilder();
            newTable[i].append("|"); // TODO make configurable -- misc_ro
        }
        for (int i = 0; i < table.getSizeOfTable(); i++) {
            for (int j = 0; j < table.getSizeOfRow(); j++) {
                newCell = renderCell(table.getRow(i)[j], j, table);
                newTable[i].append(newCell);
                newTable[i].append("|"); // TODO make configurable -- misc_ro
            }
        }
        return newTable;
    }

    private void setAmountOfSpace(DataTable table) {
        int maxAmountOfSpaceInColumn;
        int maxAmountOfSpaceInColumnBeforeAlignmentChar;
        int maxAmountOfSpaceInColumnAfterAlignmentChar;
        for (int i = 0; i < table.getSizeOfRow(); i++) {

            maxAmountOfSpace.add(0);
            maxAmountOfSpaceBeforeAlignmentChar.add(0);
            maxAmountOfSpaceAfterAlignmentChar.add(0);
            maxAmountOfSpaceInColumn = 0;
            maxAmountOfSpaceInColumnBeforeAlignmentChar = 0;
            maxAmountOfSpaceInColumnAfterAlignmentChar = 0;

            for (int j = 0; j < table.getSizeOfTable(); j++) {
                if (table.getRow(j)[i].length() > maxAmountOfSpaceInColumn) {
                    maxAmountOfSpaceInColumn = table.getRow(j)[i].length();
                }
                if (table.getRow(j)[i].contains(Character.toString('.'))) {
                    for (int numberSymbols = 0; numberSymbols < table.getRow(j)[i].length(); numberSymbols++) {
                        if (table.getRow(j)[i].charAt(numberSymbols) == '.') {
                            if (numberSymbols > maxAmountOfSpaceInColumnBeforeAlignmentChar) {
                                maxAmountOfSpaceInColumnBeforeAlignmentChar = numberSymbols;
                            }

                            if (table.getRow(j)[i].length() - numberSymbols - 1 > maxAmountOfSpaceInColumnAfterAlignmentChar) {
                                maxAmountOfSpaceInColumnAfterAlignmentChar = table.getRow(j)[i].length() - numberSymbols - 1;
                            }
                            break;
                        }
                    }
                    maxAmountOfSpaceBeforeAlignmentChar.set(i, maxAmountOfSpaceInColumnBeforeAlignmentChar);
                    maxAmountOfSpaceAfterAlignmentChar.set(i, maxAmountOfSpaceInColumnAfterAlignmentChar);
                }
            }
            maxAmountOfSpace.set(i, maxAmountOfSpaceInColumn);
        }
    }

    private StringBuilder renderCell(String cell, int columnNumber, DataTable table) {
        StringBuilder newCell;
        Alignments alignment = table.getAlignments()[columnNumber];

        switch (alignment) {

        case RIGHT:
            newCell = alignRight(cell, columnNumber, table);
            break;

        case CENTER:
            newCell = alignCenter(cell, columnNumber, table);
            break;

        case CHARALIGNMENT:
            newCell = alignByChar(cell, columnNumber, table);
            break;

        case LEFT:

        default:
            newCell = alignLeft(cell, columnNumber, table);
        }

        return newCell;
    }

    private StringBuilder alignLeft(String cell, int columnNumber, DataTable table) {
        String cellFormat = createFormatExpressionAlignedLeft(maxAmountOfSpace.get(columnNumber));
        StringBuilder renderedCell = new StringBuilder();
        renderedCell.append(StringUtils.format(cellFormat, cell));
        return renderedCell;
    }

    private StringBuilder alignRight(String cell, int columnNumber, DataTable table) {
        String cellFormat = createFormatExpressionAlignedRight(maxAmountOfSpace.get(columnNumber));
        StringBuilder renderedCell = new StringBuilder();
        renderedCell.append(StringUtils.format(cellFormat, cell));
        return renderedCell;
    }

    private StringBuilder alignCenter(String cell, int columnNumber, DataTable table) {
        int space = maxAmountOfSpace.get(columnNumber);
        int spaceBeforeCell = (space - cell.length()) / 2;
        int spaceAfterCell;
        if ((space - cell.length()) % 2 == 1) {
            spaceAfterCell = (space - cell.length()) / 2 + 1;
        } else {
            spaceAfterCell = (space - cell.length()) / 2;
        }
        String cellFormatSpaceBeforeCell = createFormatExpressionAlignedRight(spaceBeforeCell + cell.length());
        String temporaryCell = StringUtils.format(cellFormatSpaceBeforeCell, cell);
        String cellFormatSpaceAfterCell = createFormatExpressionAlignedLeft(spaceAfterCell + temporaryCell.length());
        StringBuilder finalCell = new StringBuilder();
        finalCell.append(StringUtils.format(cellFormatSpaceAfterCell, temporaryCell));
        return finalCell;
    }

    private StringBuilder alignByChar(String cell, int columnNumber, DataTable table) {
        int spaceBeforeAlignmentChar = 0;
        int spaceAfterAlignmentChar = 0;
        for (int j = 0; j < cell.length(); j++) {
            if (cell.charAt(j) == table.getAlignmentCharacter()) {
                spaceBeforeAlignmentChar = j;
                spaceAfterAlignmentChar = cell.length() - j - 1;
                break;
            }
        }
        String cellFormatSpaceBeforeCell = createFormatExpressionAlignedRight(
            maxAmountOfSpaceBeforeAlignmentChar.get(columnNumber) + cell.length() - spaceBeforeAlignmentChar);
        String temporaryCell = StringUtils.format(cellFormatSpaceBeforeCell, cell);
        String cellFormatSpaceAfterCell = createFormatExpressionAlignedLeft(
            maxAmountOfSpaceAfterAlignmentChar.get(columnNumber) + temporaryCell.length() - spaceAfterAlignmentChar);
        StringBuilder finalCell = new StringBuilder();
        finalCell.append(StringUtils.format(cellFormatSpaceAfterCell, temporaryCell));
        return finalCell;
    }

    private String createFormatExpressionAlignedRight(int space) {
        String formatExpression;
        formatExpression = "%" + Integer.toString(space) + "s";
        return formatExpression;
    }

    private String createFormatExpressionAlignedLeft(int space) {
        String formatExpression;
        formatExpression = "%-" + Integer.toString(space) + "s";
        return formatExpression;
    }

}
