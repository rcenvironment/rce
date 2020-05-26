/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.doe.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utils class for the DOE component.
 * 
 * @author Sascha Zur
 */
public final class DOEUtils {

    protected static final Log LOGGER = LogFactory.getLog(DOEUtils.class);

    private DOEUtils() {

    }

    /**
     * Writes the given table into a .csv file in the given Path.
     * 
     * @param tableValues to write
     * @param path to save
     * @param header for the header
     * @return true, if writing was successful
     */
    public static boolean writeTableToCSVFile(Object[][] tableValues, String path, List<String> header) {
        if (path != null && !path.endsWith(DOEConstants.TABLE_FILE_EXTENTION)) {
            path += DOEConstants.TABLE_FILE_EXTENTION;
        }
        try {
            if (path != null) {
                FileWriter fw = new FileWriter(new File(path));
                BufferedWriter bw = new BufferedWriter(fw);
                CSVPrinter printer = CSVFormat.newFormat(';')
                    .withIgnoreSurroundingSpaces().withAllowMissingColumnNames().withRecordSeparator("\n").print(bw);
                for (String head : header) {
                    printer.print(head);
                }
                printer.println();
                for (int i = 0; i < tableValues.length; i++) {
                    printer.printRecord(tableValues[i]);
                }

                printer.flush();
                printer.close();
            }
        } catch (IOException e) {
            LOGGER.error(e);
            return false;
        }
        return true;
    }

    /**
     * Writes the given table into a .csv file in the given Path.
     * 
     * @param tableValues to write
     * @param results map with the returned values in every iteration
     * @param path to save
     * @param currentRun for indexing
     * @param outputs name of all outputs for the header
     * @return true, if writing was successful
     */
    public static boolean writeResultToCSVFile(Object[][] tableValues, Map<Integer, Map<String, Double>> results, String path,
        int currentRun, List<String> outputs) {
        if (path != null && !path.endsWith(DOEConstants.TABLE_FILE_EXTENTION)) {
            path += DOEConstants.TABLE_FILE_EXTENTION;
        }
        try {
            if (path != null && !results.isEmpty() && results.get(0) != null && !results.get(0).isEmpty()) {
                List<String> orderedInputs = new LinkedList<>(results.get(0).keySet());
                Collections.sort(orderedInputs);
                FileWriter fw = new FileWriter(new File(path));
                BufferedWriter bw = new BufferedWriter(fw);
                CSVPrinter printer = CSVFormat.newFormat(';')
                    .withIgnoreSurroundingSpaces().withAllowMissingColumnNames().withRecordSeparator("\n").print(bw);
                for (String outputName : outputs) {
                    printer.print(outputName);
                }
                for (String input : orderedInputs) {
                    printer.print(input);
                }
                printer.println();
                for (int i = 0; i < currentRun; i++) {
                    if (results.get(i) != null) {
                        for (int j = 0; j < tableValues[i].length; j++) {
                            printer.print(tableValues[i][j]);
                        }
                        for (String input : orderedInputs) {
                            printer.print(results.get(i).get(input));
                        }
                    }
                    printer.println();
                }
                printer.flush();
                printer.close();
            }
        } catch (IOException e) {
            LOGGER.error(e);
            return false;
        }
        return true;
    }
}
