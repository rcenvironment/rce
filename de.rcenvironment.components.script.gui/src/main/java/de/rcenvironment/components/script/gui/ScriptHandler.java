/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.script.gui;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;


/**
 * Utility for handling script in editor.
 *
 * @author Arne Bachmann
 * @author Doreen Seider
 */
public final class ScriptHandler {

    private static final int BUF_SIZE = 1024 * 8;

    private ScriptHandler() {}
    
    /**
     * Decode the given byte sequence into a String.
     * @param data The data to decode
     * @param encoding The encoding
     * @return The resulting String
     */
    public static String decodeString(final byte[] data, final String encoding) {
        return Charset.forName(encoding).decode(ByteBuffer.wrap(data)).toString();
    }

    /**
     * Reads a script from the workspace into a String.
     * @param file The script representing {@link IFile} object
     * @param encoding The scripts's encoding to decode from
     * @return The scripts's contents as String
     * @throws CoreException if an error occurs
     * @throws IOException if an error occurs
     */
    public static String loadSciptAsString(final IFile file, final String encoding) throws CoreException, IOException {
        final byte[] bytes = loadScriptAsBytes(file.getContents());
        return decodeString(bytes, encoding);
    }

    /**
     * Reads a {@link InputStream} into a byte array.
     * 
     * @param is {@link InputStream} (with script contents)
     * @return The scripts's contents as byte arrays.
     * @throws CoreException if an error occurs.
     * @throws IOException if an error occurs.
     */
    public static byte[] loadScriptAsBytes(final InputStream is) throws CoreException, IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final byte[] buffer = new byte[BUF_SIZE];
        int num; 
        while ((num = is.read(buffer)) > 0) {
            baos.write(buffer, 0, num);
        }
        is.close();
        baos.close();   // effect-less
        return baos.toByteArray();
    }
    
    /**
     * Encode a Java string to byte array. 
     * @param string The string to encode
     * @param encoding The encoding to produce
     * @return The resulting byte sequence
     */
    public static byte[] encodeString(final String string, final String encoding) {
        final ByteBuffer bb = Charset.forName(encoding).encode(string);
        final byte[] buffer = bb.array();
        return Arrays.copyOfRange(buffer, 0, bb.limit());
    }
    
}
