/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.utils.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;


/**
 * Similar to FileEncodingUtils, only for IFiles.
 *
 * @author Arne Bachmann
 */
public final class ProjectFileEncodingUtils {
    
    /**
     * The buffer size.
     */
    private static final int BUF_SIZE = 1024 * 8;
    

    /**
     * Utility class.
     */
    private ProjectFileEncodingUtils() {
        // hiding constructor
    }
    

    /**
     * Decode the given byte sequence into a Java string.
     * @param data The data to decode
     * @param encoding The encoding
     * @return The resulting Java string
     */
    public static String decodeString(final byte[] data, final String encoding) {
        return Charset.forName(encoding).decode(ByteBuffer.wrap(data)).toString();
    }

    /**
     * Helper to read a ifile from the workspace into a string.
     * 
     * @param ifile The ifile object
     * @param encoding The file's encoding to decode from
     * @return The file's contents
     * @throws CoreException A
     * @throws IOException B
     */
    public static String loadIfileAsString(final IFile ifile, final String encoding) throws CoreException, IOException {
        final byte[] bytes = loadIfileAsBytes(ifile);
        return decodeString(bytes, encoding);
    }

    /**
     * Helper to read a ifile from the workspace into a byte array.
     * 
     * @param ifile The ifile object
     * @return The file's contents
     * @throws CoreException A
     * @throws IOException B
     */
    public static byte[] loadIfileAsBytes(final IFile ifile) throws CoreException, IOException {
        final InputStream is = ifile.getContents();
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
    
    /**
     * Helper to write an ifile to the workspace from a string.
     * 
     * @param file The ifile object to write to
     * @param string The contents to write
     * @param encoding If set, (un)convert to this
     * @throws CoreException A
     *         RuntimeException if no UTF-8 available
     */
    public static void saveStringAsIfile(final IFile file, final String string, final String encoding) throws CoreException {
        saveBytesAsIfile(file, encodeString(string, encoding));
    }
    
    /**
     * Helper to write an ifile to the workspace from a string.
     * 
     * @param file The ifile object to write to
     * @param bytes The contents to write
     * @throws CoreException A
     */
    public static void saveBytesAsIfile(final IFile file, final byte[] bytes) throws CoreException {
        file.setContents(new ByteArrayInputStream(bytes),
                         /* force if not in sync */ true,
                         /* keep history */ false,
                         /* progress monitor */ null);
    }
    
}
