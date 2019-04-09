/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * This class contains all unit tests of the {@link FileCompressionService} class.
 *
 * @author Tobias Brieden (created previous CompressingHelperTest class)
 * @author Thorsten Sommer
 * @author Robert Mischke
 * 
 *         TODO (p2) remaining test improvements: also move common directory teardown out of individual tests; merge parallel tests;
 *         eliminate redundant assertions (e.g. assertTrue(x!=null) + assertNonNull(x) ) -- misc_ro
 */
@RunWith(Parameterized.class)
public class FileCompressionServiceTest {

    private static final String FORMAT_SECOND_DIR_WITH_FILE = "%ssecond%s%s";

    private static final String FORMAT_SECOND_DIR = "%s%s%s%ssecond";

    private static final String FORMAT_STRING_OUTPUT_FILE_WITH_PATH_SEPARATOR_AND_EXTENSION = "%s%sout.%s";

    private static final String MSG_UNEXPECTED_FILE_WAS_DETECTED = "Unexpected file was detected";

    private static final String MSG_EXPECTED_FILE_WAS_MISSING = "Expected file was missing";

    private static final String MSG_EXPANDING_WAS_NOT_SUCCESSFUL = "Expanding was not successful";

    private static final String MSG_EXPANDING_WAS_SUCCESSFUL = "Expanding was unexpectedly successful";

    private static final String MSG_COMPRESSION_WAS_NOT_SUCCESSFUL = "Compression was not successful";

    private static final String MSG_COMPRESSION_WAS_SUCCESSFUL = "Compression was unexpectedly successful";

    private static final String MSG_RETURNED_BYTE_ARRAY_WAS_EMPTY = "Returned byte array was empty";

    private static final String MSG_RETURNED_BYTE_ARRAY_WAS_NULL = "Returned byte array was null";

    private static final String MSG_RETURNED_BYTE_ARRAY_WAS_NOT_PRESENT = "Returned byte array was not present";

    private static final String MSG_FILE_WAS_EMPTY = "The archive file was empty";

    private static final String FILE_NAME_TEST2 = "test2.PDF";

    private static final String FILE_NAME_TEST = "test.txt";

    private static final String FILE_NAME_NFS = ".nfs000000000095a01200000e8";

    private static final String FILE_NAME_SUBFOLDER = "second";

    private static final String LONG_FILE_NAME_TEST =
        "ThisIsAVeryLongFileNameWithMoreThanHundredCharsForTestingLongFilePathessWithinTheFileCompressionServiceTest.txt";

    private static final String LONG_FILE_NAME_SUBFOLDER =
        "ThisIsAVeryLongFolderNameWithMoreThanHundredCharsForTestingLongFilePathessWithinTheFileCompressionServiceTest";

    private static final String LONG_FILE_NAME_TEST2 =
        "ThisIsAnotherVeryLongFileNameWithMoreThanHundredCharsForTestingLongFilePathessWithinTheFileCompressionServiceTest.pdf";

    private final FileCompressionFormat formatParameter;

    private final TempFileService tempFileService;

    private final List<File> managedTempDirsToDispose = new ArrayList<>();

    /**
     * Parameterized constructor.
     * 
     * @param testFormat the injected archive format to test
     */
    public FileCompressionServiceTest(FileCompressionFormat testFormat) {
        this.formatParameter = testFormat;
        this.tempFileService = TempFileServiceAccess.getInstance();
        // TODO remove this output once tests are named; requires JUnit >4.8
        LogFactory.getLog(getClass()).debug("Running test case parameterized with the " + testFormat + " format");
    }

    /**
     * @return the parameters to run each test with; returning all archive formats to test (currently .zip and .tgz)
     */
    @Parameters(name = " {0} ")
    public static List<Object[]> parameters() {
        return Arrays.asList(new Object[][] {
            { FileCompressionFormat.ZIP }, { FileCompressionFormat.TAR_GZ }
        });
    }

    /**
     * Common, single-run test initialization.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        TempFileServiceAccess.setupUnitTestEnvironment();
    }

    /**
     * Disposes all temp dirs created using {@link #createTempDir()} during the test.
     */
    @After
    public void autoDisposeManagedTempDirs() {
        for (File tempDir : managedTempDirsToDispose) {
            try {
                tempFileService.disposeManagedTempDirOrFile(tempDir);
            } catch (IOException e) {
                LogFactory.getLog(getClass()).warn("Failed to clean up managed test directory " + tempDir.getAbsolutePath());
            }
        }
        managedTempDirsToDispose.clear(); // should be redundant, but doesn't hurt and helps for clarity
    }

    /**
     * A basic compression test: Testing byte array output with 2 files with the {@link FileCompressionService.compressDirectoryToByteArray}
     * method.
     * 
     * @throws IOException I/O issue
     */
    @Test
    public void basicCompressionTest01() throws IOException {
        final File inputDir = createTempDir();

        createAndVerifyFile(inputDir, FILE_NAME_TEST);
        createAndVerifyFile(inputDir, FILE_NAME_TEST2);

        // Compress the folder:
        final byte[] archiveByteArray = FileCompressionService.compressDirectoryToByteArray(inputDir, formatParameter, true);

        assertTrue(MSG_RETURNED_BYTE_ARRAY_WAS_NOT_PRESENT, archiveByteArray != null);
        assertNotNull(MSG_RETURNED_BYTE_ARRAY_WAS_NULL, archiveByteArray);
        assertTrue(MSG_RETURNED_BYTE_ARRAY_WAS_EMPTY, archiveByteArray.length > 0);
    }

    /**
     * 
     * A basic compression test: Testing byte array output and sub-folders with the
     * {@link FileCompressionService.compressDirectoryToByteArray} method.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void basicCompressionTest02() throws IOException {
        final File inputDir = createTempDir();
        final File outputDir = createTempDir();

        createAndVerifyFile(inputDir, FILE_NAME_TEST);
        createAndVerifyFile(inputDir, generateFilenameForTestFile2InSecondDir());

        // Compress the folder:
        final byte[] archiveByteArray = FileCompressionService.compressDirectoryToByteArray(inputDir, formatParameter, true);

        assertTrue(MSG_EXPANDING_WAS_NOT_SUCCESSFUL,
            FileCompressionService.expandCompressedDirectoryFromByteArray(archiveByteArray, outputDir, formatParameter));

        // Check that all files are present:
        final String[] filenameArrayBase = outputDir.list();
        final String[] filenameArrayRoot = new File(outputDir, inputDir.getName()).list();
        final String[] filenameArraySecond =
            new File(String.format(FORMAT_SECOND_DIR, outputDir.getAbsolutePath(), File.separator, inputDir.getName(),
                File.separator)).list();
        assertTrue(MSG_EXPECTED_FILE_WAS_MISSING, ArrayUtils.contains(filenameArrayBase, inputDir.getName()));
        assertTrue(MSG_EXPECTED_FILE_WAS_MISSING, ArrayUtils.contains(filenameArrayRoot, FILE_NAME_TEST));
        assertTrue(MSG_EXPECTED_FILE_WAS_MISSING, ArrayUtils.contains(filenameArrayRoot, FILE_NAME_SUBFOLDER));
        assertTrue(MSG_EXPECTED_FILE_WAS_MISSING, ArrayUtils.contains(filenameArraySecond, FILE_NAME_TEST2));

        assertTrue(MSG_RETURNED_BYTE_ARRAY_WAS_NOT_PRESENT, archiveByteArray != null);
        assertNotNull(MSG_RETURNED_BYTE_ARRAY_WAS_NULL, archiveByteArray);
        assertTrue(MSG_RETURNED_BYTE_ARRAY_WAS_EMPTY, archiveByteArray.length > 0);
    }

    /**
     * 
     * A basic compression test: Testing byte array output and sub-folders with the
     * {@link FileCompressionService.compressDirectoryToByteArray} method.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void basicCompressionTest03() throws IOException {
        final File inputDir = createTempDir();
        final File outputDir = createTempDir();

        createAndVerifyFile(inputDir, FILE_NAME_TEST);
        createAndVerifyFile(inputDir, generateFilenameForTestFile2InSecondDir());

        // Compress the folder:
        final byte[] archiveByteArray = FileCompressionService.compressDirectoryToByteArray(inputDir, formatParameter, false);

        assertTrue(MSG_EXPANDING_WAS_NOT_SUCCESSFUL,
            FileCompressionService.expandCompressedDirectoryFromByteArray(archiveByteArray, outputDir, formatParameter));

        // Check that all files are present:
        final String[] filenameArrayRoot = outputDir.list();
        final String[] filenameArraySecond = new File(String.format(FORMAT_SECOND_DIR, outputDir.getAbsolutePath(), "", "",
            File.separator)).list();
        assertTrue(MSG_EXPECTED_FILE_WAS_MISSING, ArrayUtils.contains(filenameArrayRoot, FILE_NAME_TEST));
        assertTrue(MSG_EXPECTED_FILE_WAS_MISSING, ArrayUtils.contains(filenameArrayRoot, FILE_NAME_SUBFOLDER));
        assertTrue(MSG_EXPECTED_FILE_WAS_MISSING, ArrayUtils.contains(filenameArraySecond, FILE_NAME_TEST2));

        assertTrue(MSG_RETURNED_BYTE_ARRAY_WAS_NOT_PRESENT, archiveByteArray != null);
        assertNotNull(MSG_RETURNED_BYTE_ARRAY_WAS_NULL, archiveByteArray);
        assertTrue(MSG_RETURNED_BYTE_ARRAY_WAS_EMPTY, archiveByteArray.length > 0);

    }

    /**
     * Test the file filters with the {@link FileCompressionService.expandCompressedDirectoryFromByteArray} method.
     * 
     * @throws IOException I/O issue
     */
    @Test
    public void testFileFilter01() throws IOException {
        final File inputDir = createTempDir();
        final File outputDir = createTempDir();

        final FileCompressionFormat format = formatParameter;

        createAndVerifyFile(inputDir, FILE_NAME_TEST);
        createAndVerifyFile(inputDir, FILE_NAME_NFS);
        createAndVerifyFile(inputDir, FILE_NAME_TEST2);

        // Compress and uncompress the folder:
        final byte[] archiveByteArray = FileCompressionService.compressDirectoryToByteArray(inputDir, format, true);
        assertTrue(MSG_RETURNED_BYTE_ARRAY_WAS_NOT_PRESENT, archiveByteArray != null);
        assertNotNull(MSG_RETURNED_BYTE_ARRAY_WAS_NULL, archiveByteArray);
        assertTrue(MSG_RETURNED_BYTE_ARRAY_WAS_EMPTY, archiveByteArray.length > 0);
        assertTrue(MSG_EXPANDING_WAS_NOT_SUCCESSFUL,
            FileCompressionService.expandCompressedDirectoryFromByteArray(archiveByteArray, outputDir, format));

        // Check that all files, except the .nfs file, are present:
        final String[] filenameArray = new File(outputDir, inputDir.getName()).list();
        assertTrue(MSG_EXPECTED_FILE_WAS_MISSING, ArrayUtils.contains(filenameArray, FILE_NAME_TEST));
        assertTrue(MSG_EXPECTED_FILE_WAS_MISSING, ArrayUtils.contains(filenameArray, FILE_NAME_TEST2));
        assertFalse(MSG_UNEXPECTED_FILE_WAS_DETECTED, ArrayUtils.contains(filenameArray, FILE_NAME_NFS));
    }

    /**
     * 
     * This test case ensures the correct handling of the zip slip vulnerability with the
     * {@link FileCompressionService.expandCompressedDirectoryFromFile} method.
     * 
     * @throws IOException I/O issues
     *
     */
    @Test
    public void zipSlip01() throws IOException {
        assumeTrue(formatParameter == FileCompressionFormat.ZIP); // TODO (p1) equivalent test required for .tgz?
        final File affectedArchive = new File("src/test/resources/zip slip test." + formatParameter.getDefaultFileExtension());
        final File outputDir = createTempDir();
        final File testFile = new File(outputDir.getParentFile(), FILE_NAME_TEST);

        try {
            final FileCompressionFormat format = FileCompressionFormat.ZIP;

            // Uncompress the folder:
            assertFalse("An zip slip affected archive was successful expanded",
                FileCompressionService.expandCompressedDirectoryFromFile(affectedArchive, outputDir, format));

            // Check that all files, except the .nfs file, are present:
            final String[] filenameArrayRoot = outputDir.list();
            final String[] filenameArrayParent = outputDir.getParentFile().list();

            assertFalse(MSG_UNEXPECTED_FILE_WAS_DETECTED, ArrayUtils.contains(filenameArrayRoot, FILE_NAME_TEST));
            assertFalse(MSG_UNEXPECTED_FILE_WAS_DETECTED, ArrayUtils.contains(filenameArrayParent, FILE_NAME_TEST));
            assertTrue("Output directory was not empty", filenameArrayRoot.length == 0);
            assertFalse("One file escaped the root while uncompress", testFile.exists());

        } finally {

            // Delete the might existing test.txt:
            if (testFile.exists()) {
                try {
                    testFile.delete();
                } catch (SecurityException e) {
                }
            }

        }
    }

    /**
     * 
     * Test the {@link FileCompressionService.compressDirectoryToFile()} method.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void compressDirectoryToFileTest01() throws IOException {
        final File inputDir = createTempDir();
        final File outputDir = createTempDir();
        final File outputFile = generateArchiveOutputFileReference(outputDir);

        createAndVerifyFile(inputDir, FILE_NAME_TEST);
        createAndVerifyFile(inputDir, File.separator + FILE_NAME_SUBFOLDER + File.separator + FILE_NAME_TEST2);

        assertTrue(MSG_COMPRESSION_WAS_NOT_SUCCESSFUL,
            FileCompressionService.compressDirectoryToFile(inputDir, outputFile, formatParameter, true));
        assertTrue(MSG_EXPECTED_FILE_WAS_MISSING, outputFile.exists());
        assertTrue(MSG_FILE_WAS_EMPTY, outputFile.length() > 0);

    }

    /**
     * 
     * Test the {@link FileCompressionService.compressDirectoryToFile()} method with a none-existing source directory.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void compressDirectoryToFileTest02() throws IOException {
        final File inputDir;
        if (SystemUtils.IS_OS_LINUX) {
            inputDir = new File("/this/path/does/not/exist/on/any/OS");
        } else if (SystemUtils.IS_OS_WINDOWS) {
            inputDir = new File("c:\\this\\path\\does\\not\\exist\\on\\any\\OS");
        } else {
            inputDir = new File("jhgjhgjh");
        }

        final File outputDir = createTempDir();
        final File outputFile = generateArchiveOutputFileReference(outputDir);

        assertFalse(MSG_COMPRESSION_WAS_SUCCESSFUL,
            FileCompressionService.compressDirectoryToFile(inputDir, outputFile, formatParameter, true));
        assertFalse(MSG_UNEXPECTED_FILE_WAS_DETECTED, outputFile.exists());

    }

    /**
     * 
     * Test the {@link FileCompressionService.compressDirectoryToFile()} method without format.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void compressDirectoryToFileTest03NullFormat() throws IOException {

        final File inputDir = createTempDir();
        final File outputDir = createTempDir();
        final File outputFile = generateArchiveOutputFileReference(outputDir);

        final FileCompressionFormat format = null;

        createAndVerifyFile(inputDir, FILE_NAME_TEST);
        createAndVerifyFile(inputDir, File.separator + FILE_NAME_SUBFOLDER + File.separator + FILE_NAME_TEST2);

        assertFalse(MSG_COMPRESSION_WAS_SUCCESSFUL, FileCompressionService.compressDirectoryToFile(inputDir, outputFile, format, true));
        assertFalse(MSG_UNEXPECTED_FILE_WAS_DETECTED, outputFile.exists());

    }

    /**
     * 
     * Test the {@link FileCompressionService.compressDirectoryToFile()} method with null as format.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void compressDirectoryToFileTest04NullFormat() throws IOException {

        final File inputDir = createTempDir();
        final File outputDir = createTempDir();
        final File outputFile = generateArchiveOutputFileReference(outputDir);

        createAndVerifyFile(inputDir, FILE_NAME_TEST);
        createAndVerifyFile(inputDir, File.separator + FILE_NAME_SUBFOLDER + File.separator + FILE_NAME_TEST2);

        assertFalse(MSG_COMPRESSION_WAS_SUCCESSFUL, FileCompressionService.compressDirectoryToFile(inputDir, outputFile, null, true));
        assertFalse(MSG_UNEXPECTED_FILE_WAS_DETECTED, outputFile.exists());

    }

    /**
     * 
     * Test the {@link FileCompressionService.compressDirectoryToFile()} method. The destination is missing.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void compressDirectoryToFileTest05() throws IOException {
        final File inputDir = createTempDir();
        final File output = null;

        createAndVerifyFile(inputDir, FILE_NAME_TEST);
        createAndVerifyFile(inputDir, File.separator + FILE_NAME_SUBFOLDER + File.separator + FILE_NAME_TEST2);

        assertFalse(MSG_COMPRESSION_WAS_SUCCESSFUL,
            FileCompressionService.compressDirectoryToFile(inputDir, output, formatParameter, true));

    }

    /**
     * 
     * Test the {@link FileCompressionService.compressDirectoryToFile()} method. The input directory is missing.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void compressDirectoryToFileTest06() throws IOException {
        final File outputDir = createTempDir();
        final File outputFile = generateArchiveOutputFileReference(outputDir);

        assertFalse(MSG_COMPRESSION_WAS_SUCCESSFUL,
            FileCompressionService.compressDirectoryToFile(null, outputFile, formatParameter, true));
        assertFalse(MSG_UNEXPECTED_FILE_WAS_DETECTED, outputFile.exists());

    }

    /**
     * 
     * Test the {@link FileCompressionService.compressDirectoryToFile()} method without any meaningful value I.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void compressDirectoryToFileTest07NullFormatAndParameters() throws IOException {
        assertFalse(MSG_COMPRESSION_WAS_SUCCESSFUL,
            FileCompressionService.compressDirectoryToFile(null, null,
                null, true));
    }

    /**
     * 
     * Test the {@link FileCompressionService.compressDirectoryToFile()} method without any meaningful value II.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void compressDirectoryToFileTest08NullFormatAndParameters() throws IOException {

        assertFalse(MSG_COMPRESSION_WAS_SUCCESSFUL, FileCompressionService.compressDirectoryToFile(null, null, null, true));
    }

    /**
     * 
     * Test the {@link FileCompressionService.compressDirectoryToFile()} method with file pathes larger than 100 chars.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void compressDirectoryToFileTest09LongPath() throws IOException {

        final File inputDir = createTempDir();
        final File outputDir = createTempDir();
        final File outputFile = generateArchiveOutputFileReference(outputDir);
        final File finalDir = createTempDir();

        createAndVerifyFile(inputDir, LONG_FILE_NAME_TEST);
        createAndVerifyFile(inputDir, LONG_FILE_NAME_SUBFOLDER + File.separator + LONG_FILE_NAME_TEST2);

        assertTrue(MSG_COMPRESSION_WAS_NOT_SUCCESSFUL,
            FileCompressionService.compressDirectoryToFile(inputDir, outputFile, formatParameter, true));
        assertTrue(MSG_EXPECTED_FILE_WAS_MISSING, outputFile.exists());
        assertTrue(MSG_FILE_WAS_EMPTY, outputFile.length() > 0);

        // Expanding:
        assertTrue(MSG_EXPANDING_WAS_NOT_SUCCESSFUL,
            FileCompressionService.expandCompressedDirectoryFromFile(outputFile, finalDir, formatParameter));

        // Check that all files are present:
        final String[] filenameArrayRoot = new File(finalDir, inputDir.getName()).list();
        final String[] filenameArraySubFolder =
            new File(finalDir.getAbsolutePath() + File.separator + inputDir.getName()
                + File.separator + LONG_FILE_NAME_SUBFOLDER).list();
        assertTrue(MSG_EXPECTED_FILE_WAS_MISSING, ArrayUtils.contains(filenameArrayRoot, LONG_FILE_NAME_TEST));
        assertTrue(MSG_EXPECTED_FILE_WAS_MISSING, ArrayUtils.contains(filenameArrayRoot, LONG_FILE_NAME_SUBFOLDER));
        assertTrue(MSG_EXPECTED_FILE_WAS_MISSING, ArrayUtils.contains(filenameArraySubFolder, LONG_FILE_NAME_TEST2));

    }

    /**
     * 
     * Test the {@link FileCompressionService.compressDirectoryToOutputStream} method without an output stream.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void compressDirectoryToOutputStreamTest01() throws IOException {
        final File inputDir = createTempDir();

        createAndVerifyFile(inputDir, FILE_NAME_TEST);
        createAndVerifyFile(inputDir, generateFilenameForTestFile2InSecondDir());

        // Compress the folder:
        assertFalse(MSG_COMPRESSION_WAS_SUCCESSFUL,
            FileCompressionService.compressDirectoryToOutputStream(inputDir, null, formatParameter, true));

    }

    /**
     * 
     * Test the {@link FileCompressionService.compressDirectoryToOutputStream} method without an output stream.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void compressDirectoryToOutputStreamTest02() throws IOException {
        final File inputDir = createTempDir();

        createAndVerifyFile(inputDir, FILE_NAME_TEST);
        createAndVerifyFile(inputDir, generateFilenameForTestFile2InSecondDir());

        // Compress the folder:
        assertFalse(MSG_COMPRESSION_WAS_SUCCESSFUL,
            FileCompressionService.compressDirectoryToOutputStream(inputDir, null, formatParameter, true));

    }

    /**
     * 
     * Test the method {@link FileCompressionService.compressDirectoryToByteStream} method with useless arguments.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void compressDirectoryToByteStreamTest01NullFormat() throws IOException {

        final File inputDir = createTempDir();

        createAndVerifyFile(inputDir, FILE_NAME_TEST);
        createAndVerifyFile(inputDir, generateFilenameForTestFile2InSecondDir());

        // Compress the folder:
        final ByteArrayOutputStream outStream = FileCompressionService.compressDirectoryToByteStream(inputDir, null, true);

        assertFalse(MSG_COMPRESSION_WAS_SUCCESSFUL, outStream != null);

    }

    /**
     * 
     * Test the method {@link FileCompressionService.compressDirectoryToByteStream} method with useless arguments.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void compressDirectoryToByteStreamTest02NullFormat() throws IOException {

        final File inputDir = createTempDir();

        createAndVerifyFile(inputDir, FILE_NAME_TEST);
        createAndVerifyFile(inputDir, generateFilenameForTestFile2InSecondDir());

        // Compress the folder:
        final ByteArrayOutputStream outStream = FileCompressionService.compressDirectoryToByteStream(inputDir, null, true);

        assertFalse(MSG_COMPRESSION_WAS_SUCCESSFUL, outStream != null);

    }

    /**
     * 
     * Test the method {@link FileCompressionService.compressDirectoryToByteArray} method with useless arguments.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void compressDirectoryToByteArrayTest01NullFormat() throws IOException {

        final File inputDir = createTempDir();

        createAndVerifyFile(inputDir, FILE_NAME_TEST);
        createAndVerifyFile(inputDir, generateFilenameForTestFile2InSecondDir());

        // Compress the folder:
        final byte[] outStream = FileCompressionService.compressDirectoryToByteArray(inputDir, null, true);

        assertFalse(MSG_COMPRESSION_WAS_SUCCESSFUL, outStream != null);
    }

    /**
     * 
     * Test the method {@link FileCompressionService.compressDirectoryToByteArray} method with useless arguments.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void compressDirectoryToByteArrayTest02NullFormat() throws IOException {

        final File inputDir = createTempDir();

        createAndVerifyFile(inputDir, FILE_NAME_TEST);
        createAndVerifyFile(inputDir, generateFilenameForTestFile2InSecondDir());

        // Compress the folder:
        final byte[] outStream = FileCompressionService.compressDirectoryToByteArray(inputDir, null, true);

        assertFalse(MSG_COMPRESSION_WAS_SUCCESSFUL, outStream != null);

    }

    /**
     * 
     * Testing the {@link FileCompressionService.expandCompressedDirectoryFromFile} method regarding the expansion in a directory.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void expandToDirTest01() throws IOException {
        final File inputDir = createTempDir();
        final File outputDir = createTempDir();
        final File finalDir = createTempDir();
        final File outputFile = generateArchiveOutputFileReference(outputDir);

        createAndVerifyFile(inputDir, FILE_NAME_TEST);
        createAndVerifyFile(inputDir, generateFilenameForTestFile2InSecondDir());

        // Compress the folder:
        assertTrue(MSG_COMPRESSION_WAS_NOT_SUCCESSFUL,
            FileCompressionService.compressDirectoryToFile(inputDir, outputFile, formatParameter, true));

        // Expanding:
        assertTrue(MSG_EXPANDING_WAS_NOT_SUCCESSFUL,
            FileCompressionService.expandCompressedDirectoryFromFile(outputFile, finalDir, formatParameter));

        // Check that all files are present:
        final String[] filenameArrayBase = finalDir.list();
        final String[] filenameArrayRoot = new File(finalDir, inputDir.getName()).list();
        final String[] filenameArraySecond =
            new File(String.format(FORMAT_SECOND_DIR, finalDir.getAbsolutePath(), File.separator, inputDir.getName(),
                File.separator)).list();
        assertTrue(MSG_EXPECTED_FILE_WAS_MISSING, ArrayUtils.contains(filenameArrayBase, inputDir.getName()));
        assertTrue(MSG_EXPECTED_FILE_WAS_MISSING, ArrayUtils.contains(filenameArrayRoot, FILE_NAME_TEST));
        assertTrue(MSG_EXPECTED_FILE_WAS_MISSING, ArrayUtils.contains(filenameArrayRoot, FILE_NAME_SUBFOLDER));
        assertTrue(MSG_EXPECTED_FILE_WAS_MISSING, ArrayUtils.contains(filenameArraySecond, FILE_NAME_TEST2));
    }

    /**
     * 
     * Testing the {@link FileCompressionService.expandCompressedDirectoryFromFile} method regarding the expansion in a directory. Issue:
     * The destination seems to be a file instead of a directory. Actually, it is not! Java cannot decide if {@code finalFile} is a
     * directory or a file. The current handling: If "a file" is not existing, Java creates a directory with the "filename". In case a file
     * is already existing, Java will know about it.
     * 
     * This test tests a <b>none-existing file</b> "instead" of a directory. Java cannot know this and should create a directory instead.
     * Thus, this is a valid case and should be successfully.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void expandToDirTest02() throws IOException {
        final File inputDir = createTempDir();
        final File outputDir = createTempDir();
        final File finalDir = createTempDir();
        final File outputFile = generateArchiveOutputFileReference(outputDir);
        final File finalFile = generateArchiveOutputFileReference(finalDir);

        createAndVerifyFile(inputDir, FILE_NAME_TEST);
        createAndVerifyFile(inputDir, generateFilenameForTestFile2InSecondDir());

        // Compress the folder:
        assertTrue(MSG_COMPRESSION_WAS_NOT_SUCCESSFUL,
            FileCompressionService.compressDirectoryToFile(inputDir, outputFile, formatParameter, true));

        // Expanding:
        assertTrue(MSG_EXPANDING_WAS_NOT_SUCCESSFUL,
            FileCompressionService.expandCompressedDirectoryFromFile(outputFile, finalFile, formatParameter));

        // Check that all files are present:
        final String[] filenameArrayBase = finalFile.list();
        final String[] filenameArrayRoot = new File(finalFile, inputDir.getName()).list();
        final String[] filenameArraySecond =
            new File(String.format(FORMAT_SECOND_DIR, finalFile.getAbsolutePath(), File.separator, inputDir.getName(),
                File.separator)).list();
        assertTrue(MSG_EXPECTED_FILE_WAS_MISSING, ArrayUtils.contains(filenameArrayBase, inputDir.getName()));
        assertTrue(MSG_EXPECTED_FILE_WAS_MISSING, ArrayUtils.contains(filenameArrayRoot, FILE_NAME_TEST));
        assertTrue(MSG_EXPECTED_FILE_WAS_MISSING, ArrayUtils.contains(filenameArrayRoot, FILE_NAME_SUBFOLDER));
        assertTrue(MSG_EXPECTED_FILE_WAS_MISSING, ArrayUtils.contains(filenameArraySecond, FILE_NAME_TEST2));
    }

    /**
     * 
     * Testing the {@link FileCompressionService.expandCompressedDirectoryFromFile} method regarding the expansion in a directory. Issue:
     * The destination seems to be a file instead of a directory. Actually, it is not! Java cannot decide if {@code finalFile} is a
     * directory or a file. The current handling: If "a file" is not existing, Java creates a directory with the "filename". In case a file
     * is already existing, Java will know about it.
     * 
     * This test tests an <b>existing file</b> instead of a directory. Java should know this. Thus, this is a none-valid case and should be
     * fail.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void expandToDirTest03() throws IOException {
        // TODO (p1): this test fails with TGZ; investigate
        assumeTrue(formatParameter == FileCompressionFormat.ZIP);
        final File inputDir = createTempDir();
        final File outputDir = createTempDir();
        final File finalDir = createTempDir();
        final File outputFile = generateArchiveOutputFileReference(outputDir);
        final File finalFile = generateArchiveOutputFileReference(finalDir);

        createAndVerifyFile(inputDir, FILE_NAME_TEST);
        createAndVerifyFile(inputDir, generateFilenameForTestFile2InSecondDir());
        // TODO (p1) investigate: adapting the extension to the format parameter changes semantics!
        createAndVerifyFile(finalDir, String.format("%sout.zip", File.separator));
        // createAndVerifyFile(finalDir, String.format("%sout." + formatParameter.getDefaultFileExtension(), File.separator));

        // Compress the folder:
        assertTrue(MSG_COMPRESSION_WAS_NOT_SUCCESSFUL,
            FileCompressionService.compressDirectoryToFile(inputDir, outputFile, formatParameter, true));

        // Expanding:
        assertFalse(MSG_EXPANDING_WAS_SUCCESSFUL,
            FileCompressionService.expandCompressedDirectoryFromFile(outputFile, finalFile, formatParameter));

        // Check that all files are present:
        final String[] filenameArrayBase = finalDir.list();
        final String[] filenameArrayRoot = new File(finalDir, inputDir.getName()).list();
        final String[] filenameArraySecond =
            new File(String.format(FORMAT_SECOND_DIR, finalDir.getAbsolutePath(), File.separator, inputDir.getName(),
                File.separator)).list();
        assertFalse(MSG_UNEXPECTED_FILE_WAS_DETECTED, ArrayUtils.contains(filenameArrayBase, inputDir.getName()));
        assertFalse(MSG_UNEXPECTED_FILE_WAS_DETECTED, ArrayUtils.contains(filenameArrayRoot, FILE_NAME_TEST));
        assertFalse(MSG_UNEXPECTED_FILE_WAS_DETECTED, ArrayUtils.contains(filenameArrayRoot, FILE_NAME_SUBFOLDER));
        assertFalse(MSG_UNEXPECTED_FILE_WAS_DETECTED, ArrayUtils.contains(filenameArraySecond, FILE_NAME_TEST2));
    }

    /**
     * 
     * Testing the {@link FileCompressionService.expandCompressedDirectoryFromFile} method regarding the expansion in a directory. Issue:
     * The destination is not given.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void expandToDirTest04() throws IOException {
        final File inputDir = createTempDir();
        final File outputDir = createTempDir();
        final File finalDir = null;
        final File outputFile = generateArchiveOutputFileReference(outputDir);

        createAndVerifyFile(inputDir, FILE_NAME_TEST);
        createAndVerifyFile(inputDir, generateFilenameForTestFile2InSecondDir());

        // Compress the folder:
        assertTrue(MSG_COMPRESSION_WAS_NOT_SUCCESSFUL,
            FileCompressionService.compressDirectoryToFile(inputDir, outputFile, formatParameter, true));

        // Expanding:
        assertFalse(MSG_EXPANDING_WAS_SUCCESSFUL,
            FileCompressionService.expandCompressedDirectoryFromFile(outputFile, finalDir, formatParameter));
    }

    /**
     * 
     * Testing the {@link FileCompressionService.expandCompressedDirectoryFromFile} method regarding the expansion in a directory. Issue:
     * The destination is null.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void expandToDirTest05() throws IOException {
        final File inputDir = createTempDir();
        final File outputDir = createTempDir();
        final File finalDir = null;
        final File outputFile = generateArchiveOutputFileReference(outputDir);

        createAndVerifyFile(inputDir, FILE_NAME_TEST);
        createAndVerifyFile(inputDir, generateFilenameForTestFile2InSecondDir());

        // Compress the folder:
        assertTrue(MSG_COMPRESSION_WAS_NOT_SUCCESSFUL,
            FileCompressionService.compressDirectoryToFile(inputDir, outputFile, formatParameter, true));

        // Expanding:
        assertFalse(MSG_EXPANDING_WAS_SUCCESSFUL,
            FileCompressionService.expandCompressedDirectoryFromFile(outputFile, finalDir, formatParameter));
    }

    /**
     * 
     * Testing the {@link FileCompressionService.expandCompressedDirectoryFromFile} method regarding the expansion in a directory. Issue:
     * The source is empty i.e. contains null.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void expandToDirTest06() throws IOException {
        final File inputDir = createTempDir();
        final File outputDir = createTempDir();
        final File finalDir = createTempDir();
        final File outputFile = generateArchiveOutputFileReference(outputDir);

        createAndVerifyFile(inputDir, FILE_NAME_TEST);
        createAndVerifyFile(inputDir, generateFilenameForTestFile2InSecondDir());

        // Compress the folder:
        assertTrue(MSG_COMPRESSION_WAS_NOT_SUCCESSFUL,
            FileCompressionService.compressDirectoryToFile(inputDir, outputFile, formatParameter, true));

        // Expanding:
        assertFalse(MSG_EXPANDING_WAS_SUCCESSFUL,
            FileCompressionService.expandCompressedDirectoryFromFile(null, finalDir, formatParameter));
    }

    /**
     * 
     * Testing the {@link FileCompressionService.expandCompressedDirectoryFromFile} method regarding the expansion in a directory. Issue:
     * The source is null.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void expandToDirTest07() throws IOException {
        final File inputDir = createTempDir();
        final File outputDir = createTempDir();
        final File finalDir = createTempDir();
        final File outputFile = generateArchiveOutputFileReference(outputDir);

        createAndVerifyFile(inputDir, FILE_NAME_TEST);
        createAndVerifyFile(inputDir, generateFilenameForTestFile2InSecondDir());

        // Compress the folder:
        assertTrue(MSG_COMPRESSION_WAS_NOT_SUCCESSFUL,
            FileCompressionService.compressDirectoryToFile(inputDir, outputFile, formatParameter, true));

        // Expanding:
        assertFalse(MSG_EXPANDING_WAS_SUCCESSFUL,
            FileCompressionService.expandCompressedDirectoryFromFile(null, finalDir, formatParameter));
    }

    /**
     * Testing the {@link FileCompressionService.compressDirectoryToFile} and
     * {@link FileCompressionService.expandCompressedDirectoryFromFile} method regarding the handling of an empty root directories. The
     * archive should not be empty, but contain the empty directory.
     * 
     * @throws IOException I/O issue
     */
    @Test
    public void expandToDirTest08() throws IOException {
        final File inputDir = createTempDir();
        final File outputDir = createTempDir();
        final File finalDir = createTempDir();
        final File outputFile = generateArchiveOutputFileReference(outputDir);

        // Compress the empty folder:
        assertTrue(MSG_COMPRESSION_WAS_NOT_SUCCESSFUL,
            FileCompressionService.compressDirectoryToFile(inputDir, outputFile, formatParameter, true));

        // Expanding:
        assertTrue(MSG_EXPANDING_WAS_NOT_SUCCESSFUL,
            FileCompressionService.expandCompressedDirectoryFromFile(outputFile, finalDir, formatParameter));

        assertTrue(MSG_FILE_WAS_EMPTY, outputFile.length() > 0); // ... at least the header should be there.
        assertTrue(MSG_UNEXPECTED_FILE_WAS_DETECTED, inputDir.list().length == 0);
        assertTrue(MSG_FILE_WAS_EMPTY, finalDir.list().length == 1);
        assertTrue(MSG_UNEXPECTED_FILE_WAS_DETECTED, finalDir.listFiles()[0].list().length == 0);
    }

    /**
     * Testing the {@link FileCompressionService.compressDirectoryToFile} and
     * {@link FileCompressionService.expandCompressedDirectoryFromFile} method regarding the handling of empty directories inside the root
     * directory.
     * 
     * @throws IOException I/O issue
     */
    @Test
    public void expandToDirTest09() throws IOException {
        final File inputDir = createTempDir();
        final File outputDir = createTempDir();
        final File finalDir = createTempDir();
        final File outputFile = generateArchiveOutputFileReference(outputDir);

        createAndVerifyFile(inputDir, FILE_NAME_TEST);
        createAndVerifyDirectory(inputDir, FILE_NAME_SUBFOLDER);

        // Compress
        assertTrue(MSG_COMPRESSION_WAS_NOT_SUCCESSFUL,
            FileCompressionService.compressDirectoryToFile(inputDir, outputFile, formatParameter, true));

        // Expanding:
        assertTrue(MSG_EXPANDING_WAS_NOT_SUCCESSFUL,
            FileCompressionService.expandCompressedDirectoryFromFile(outputFile, finalDir, formatParameter));

        assertTrue(MSG_FILE_WAS_EMPTY, finalDir.list().length == 1);
        assertFalse(MSG_EXPECTED_FILE_WAS_MISSING, finalDir.listFiles()[0].list().length < 2);
        assertFalse(MSG_UNEXPECTED_FILE_WAS_DETECTED, finalDir.listFiles()[0].list().length > 2);
    }

    /**
     * 
     * Testing the {@link FileCompressionService.expandCompressedDirectoryFromInputStream} method regarding the handling of an none-existing
     * input stream.
     * 
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void expandCompressedDirectoryFromInputStreamTest01() throws IOException {
        final File outputDir = createTempDir();

        // Expanding:
        assertFalse(MSG_EXPANDING_WAS_SUCCESSFUL,
            FileCompressionService.expandCompressedDirectoryFromInputStream(null, outputDir, formatParameter));
    }

    /**
     * 
     * Testing the {@link FileCompressionService.expandCompressedDirectoryFromInputStream} method regarding the handling of an none-existing
     * input stream.
     * 
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void expandCompressedDirectoryFromInputStreamTest02() throws IOException {
        final File outputDir = createTempDir();

        // Expanding:
        assertFalse(MSG_EXPANDING_WAS_SUCCESSFUL,
            FileCompressionService.expandCompressedDirectoryFromInputStream(null, outputDir, formatParameter));
    }

    /**
     * 
     * Test the {@link FileCompressionService.expandCompressedDirectoryFromByteStream} method.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void expandCompressedDirectoryFromByteStreamTest01() throws IOException {
        final File inputDir = createTempDir();
        final File outputDir = createTempDir();

        createAndVerifyFile(inputDir, FILE_NAME_TEST);
        createAndVerifyFile(inputDir, generateFilenameForTestFile2InSecondDir());

        // Compress the folder:
        try (ByteArrayOutputStream archiveByteStream =
            FileCompressionService.compressDirectoryToByteStream(inputDir, formatParameter, true)) {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(archiveByteStream.toByteArray())) {
                assertTrue(MSG_EXPANDING_WAS_NOT_SUCCESSFUL, FileCompressionService
                    .expandCompressedDirectoryFromByteStream(bais, outputDir, formatParameter));
            }
        }

        // Check that all files are present:
        final String[] filenameArrayBase = outputDir.list();
        final String[] filenameArrayRoot = new File(outputDir, inputDir.getName()).list();
        final String[] filenameArraySecond =
            new File(String.format(FORMAT_SECOND_DIR, outputDir.getAbsolutePath(), File.separator, inputDir.getName(),
                File.separator)).list();
        assertTrue(MSG_EXPECTED_FILE_WAS_MISSING, ArrayUtils.contains(filenameArrayBase, inputDir.getName()));
        assertTrue(MSG_EXPECTED_FILE_WAS_MISSING, ArrayUtils.contains(filenameArrayRoot, FILE_NAME_TEST));
        assertTrue(MSG_EXPECTED_FILE_WAS_MISSING, ArrayUtils.contains(filenameArrayRoot, FILE_NAME_SUBFOLDER));
        assertTrue(MSG_EXPECTED_FILE_WAS_MISSING, ArrayUtils.contains(filenameArraySecond, FILE_NAME_TEST2));

    }

    /**
     * 
     * Test the {@link FileCompressionService.expandCompressedDirectoryFromByteStream} method. Issue: invalid byte input stream.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void expandCompressedDirectoryFromByteStreamTest02() throws IOException {
        assumeTrue(formatParameter == FileCompressionFormat.ZIP); // TODO provide an equivalent input file or use the same?
        final File outputDir = createTempDir();

        final Path invalidArchive =
            FileSystems.getDefault().getPath("src/test/resources/invalid." + formatParameter.getDefaultFileExtension());

        // Compress the folder:
        try (ByteArrayInputStream bais = new ByteArrayInputStream(Files.readAllBytes(invalidArchive))) {
            assertFalse(MSG_EXPANDING_WAS_SUCCESSFUL, FileCompressionService
                .expandCompressedDirectoryFromByteStream(bais, outputDir, formatParameter));
        }

    }

    /**
     * 
     * Test the {@link FileCompressionService.expandCompressedDirectoryFromByteStream} method. Issue: byte input stream is empty.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void expandCompressedDirectoryFromByteStreamTest03() throws IOException {
        final File outputDir = createTempDir();

        // Compress the folder:
        try (ByteArrayInputStream bais = new ByteArrayInputStream(new byte[] {})) {
            assertFalse(MSG_EXPANDING_WAS_SUCCESSFUL, FileCompressionService
                .expandCompressedDirectoryFromByteStream(bais, outputDir, formatParameter));
        }

    }

    /**
     * 
     * Test the {@link FileCompressionService.expandCompressedDirectoryFromByteStream} method. Issue: byte input stream does not exist.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void expandCompressedDirectoryFromByteStreamTest04() throws IOException {
        final File outputDir = createTempDir();

        // Compress the folder:
        assertFalse(MSG_EXPANDING_WAS_SUCCESSFUL, FileCompressionService
            .expandCompressedDirectoryFromByteStream(null, outputDir, formatParameter));

    }

    /**
     * 
     * Test the {@link FileCompressionService.expandCompressedDirectoryFromByteStream} method. Issue: byte input stream is null.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void expandCompressedDirectoryFromByteStreamTest05() throws IOException {
        final File outputDir = createTempDir();

        // Compress the folder:
        assertFalse(MSG_EXPANDING_WAS_SUCCESSFUL, FileCompressionService
            .expandCompressedDirectoryFromByteStream(null, outputDir, formatParameter));

    }

    /**
     * 
     * Test the {@link FileCompressionService.expandCompressedDirectoryFromByteArray} method. Issue: byte array is empty.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void expandCompressedDirectoryFromByteArrayTest01() throws IOException {
        final File outputDir = createTempDir();

        final byte[] data = {};
        assertFalse(MSG_EXPANDING_WAS_SUCCESSFUL, FileCompressionService
            .expandCompressedDirectoryFromByteArray(data, outputDir, formatParameter));

    }

    /**
     * 
     * Test the {@link FileCompressionService.expandCompressedDirectoryFromByteArray} method. Issue: byte array is not present.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void expandCompressedDirectoryFromByteArrayTest02() throws IOException {
        final File outputDir = createTempDir();

        assertFalse(MSG_EXPANDING_WAS_SUCCESSFUL, FileCompressionService
            .expandCompressedDirectoryFromByteArray(null, outputDir, formatParameter));

    }

    /**
     * 
     * Test the {@link FileCompressionService.expandCompressedDirectoryFromByteArray} method. Issue: byte array is null.
     * 
     * @throws IOException I/O issue
     *
     */
    @Test
    public void expandCompressedDirectoryFromByteArrayTest03() throws IOException {
        final File outputDir = createTempDir();

        assertFalse(MSG_EXPANDING_WAS_SUCCESSFUL, FileCompressionService
            .expandCompressedDirectoryFromByteArray(null, outputDir, formatParameter));

    }

    private File createTempDir() throws IOException {
        final File tempDir = tempFileService.createManagedTempDir();
        managedTempDirsToDispose.add(tempDir);
        return tempDir;
    }

    private File generateArchiveOutputFileReference(final File outputDir) throws IOException {
        return new File(
            String.format(FORMAT_STRING_OUTPUT_FILE_WITH_PATH_SEPARATOR_AND_EXTENSION, outputDir.getCanonicalPath(), File.separator,
                formatParameter.getDefaultFileExtension()));
    }

    private String generateFilenameForTestFile2InSecondDir() {
        // TODO check: why the leading separator?
        return String.format(FORMAT_SECOND_DIR_WITH_FILE, File.separator, File.separator, FILE_NAME_TEST2);
    }

    private File createAndVerifyFile(final File parentDir, final String name) throws IOException {
        final File file = new File(parentDir, name);
        file.getParentFile().mkdirs();
        file.createNewFile();

        assertTrue("Create temporary file for unit test was not successful.", file.isFile() && file.canRead());
        return file;
    }

    private File createAndVerifyDirectory(final File parentDir, final String name) throws IOException {
        final File file = new File(parentDir, name);
        file.mkdir();

        assertTrue("Create temporary directory for unit test was not successful.", file.isDirectory());
        return file;
    }
}
