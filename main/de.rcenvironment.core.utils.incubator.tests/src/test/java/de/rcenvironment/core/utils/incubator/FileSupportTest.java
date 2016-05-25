/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
import java.util.UUID;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.utils.common.legacy.FileSupport;

/**
 * Test for {@link FileSupport}.
 *
 * @author Doreen Seider
 */
@SuppressWarnings("deprecation") //This is a test for the deprecated class FileSupport.
public class FileSupportTest extends TestCase {

    private File directory;

    private File data;

    @Override
    protected void setUp() throws Exception {
        final Random random = new Random();
        directory = File.createTempFile("test" + random.nextInt(), "");
        directory.delete();
        if (!directory.mkdir()) {
            throw new RuntimeException("Temp directory could not be created.");
        }
        data = new File(directory, "data");
        data.mkdir();
        final File file = new File(data, "test.txt");
        file.createNewFile();
        final PrintWriter out = new PrintWriter(file);
        out.write("Hello, World!");
        out.close();
    }

    @Override
    protected void tearDown() throws Exception {
        delete(directory);
    }

    private void delete(final File file) {
        if (file.isDirectory()) {
            final File[] subFiles = file.listFiles();
            for (final File subFile : subFiles) {
                delete(subFile);
            }
            file.delete();
        } else {
            file.delete();
        }
    }

    /**
     * Test.
     */
    public void testDeleteFile() {
        
        File fileMock = EasyMock.createNiceMock(File.class);
        EasyMock.expect(fileMock.isDirectory()).andReturn(false).anyTimes();
        EasyMock.replay(fileMock);
        FileSupport.deleteFile(fileMock);
        
        File directoryMock = EasyMock.createNiceMock(File.class);
        EasyMock.expect(directoryMock.isDirectory()).andReturn(true);
        EasyMock.expect(directoryMock.listFiles()).andReturn(new File[] { fileMock }).anyTimes();
        EasyMock.replay(directoryMock);
        FileSupport.deleteFile(directoryMock);
        
    }

    /**
     * Test.
     * @throws IOException IOException
     * @throws InterruptedException InterruptedException
     */
    @Test
    public void testZip() throws IOException, InterruptedException {
        final byte[] result = FileSupport.zip(new File(directory, "data"));
        Assert.assertTrue(result.length > 0);
    }

    /**
     * Test.
     * @throws IOException IOException
     * @throws InterruptedException InterruptedException
     */
    @Test
    public void testZipToFile() throws IOException, InterruptedException {
        final File tempFile = new File(directory, UUID.randomUUID().toString() + ".zip");
        FileSupport.zipToFile(data, tempFile);
        Assert.assertTrue(tempFile.exists());
    }

}
