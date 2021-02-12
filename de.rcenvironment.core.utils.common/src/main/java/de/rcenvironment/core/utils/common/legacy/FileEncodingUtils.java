/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.legacy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * Helper to load and save string from and to files using a certain encoding.
 * 
 * @author Arne Bachmann
 * @author Sascha Zur
 * @deprecated not worth to do this in an utility class
 */
@Deprecated
public final class FileEncodingUtils {

    private FileEncodingUtils() {
        // utility class
    }

    /**
     * Save unicode string to file.
     * 
     * @param string The Java string to write
     * @param file the file to write to
     * @throws IOException For any error
     */
    public static void saveUnicodeStringToFile(final String string, final File file) throws IOException {
        final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));
        bw.write(string);
        bw.flush();
        bw.close();
    }

    /**
     * Load unicode string from file.
     * 
     * @param file The file
     * @return The string read
     * @throws IOException For any error
     */
    public static String loadUnicodeStringFromFile(final File file) throws IOException {
        StringBuffer content = new StringBuffer();
        try (final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"))) {
            final char[] buffer = new char[1 << (3 * 5)]; // 64k buf.
            int read;
            while ((read = br.read(buffer)) > 0) {
                content.append(buffer, 0, read);
            }
        }
        return content.toString();
    }

}
