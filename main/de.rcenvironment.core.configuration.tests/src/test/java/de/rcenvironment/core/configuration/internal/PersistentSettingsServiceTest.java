/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.internal;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

/**
 * TestCases for PersistentSettingsServiceTest.java.
 * 
 * @author Sascha Zur
 */
public class PersistentSettingsServiceTest {

    private static final String PERSISTENTDATA_FILE = "persSet.json";

    private static final File TMPDIR = createTempDir();

    private PersistentSettingsServiceImpl persSetServ;

    private final String test4 = "test4";

    /**
     * Startup.
     */
    @Before
    public void startUp() {
        persSetServ = new PersistentSettingsServiceImpl();
        File tmp = new File(TMPDIR.getAbsolutePath()
            + File.separator + ".rce" + File.separator + "persistentSettings" + File.separator + "test");
        tmp.mkdirs();
        File tmp2 = new File(tmp.getAbsolutePath() + File.separator + PERSISTENTDATA_FILE);
        try {
            tmp2.createNewFile();
        } catch (IOException e1) {
            persSetServ = null;
        }
        persSetServ.setStorageDirectory(tmp.getAbsolutePath());
    }

    /**
     * Creates a new temporary directory.
     * 
     * @return The File object of the directory.
     */
    private static File createTempDir() {
        File tempFile;
        try {
            tempFile = File.createTempFile("temp", Long.toString(System.nanoTime()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        tempFile.delete();
        tempFile.mkdir();

        return tempFile;
    }

    /**
     * Test.
     */
    @Test
    public void testSaveStore() {
        persSetServ.saveStringValue("test0", "1");
        persSetServ.saveStringValue("test1", "2");
        persSetServ.saveStringValue("test2", "3");
        persSetServ.saveStringValue("test3", "4");
        persSetServ.saveStringValue(test4, "5");
    }

    /**
     * Test.
     */
    @Test
    public void testReadStore() {
        testSaveStore();
        Assert.assertNotNull(persSetServ.readStringValue("test0"));
        Assert.assertNotNull(persSetServ.readStringValue("test3"));
    }

    /**
     * Test.
     */
    @Test
    public void testDelete() {
        persSetServ.delete(test4);
        Assert.assertNull(persSetServ.readStringValue(test4));
        persSetServ.delete("24");
    }

    /**
     * Test.
     */
    @Test
    public void testSaveMap() {
        Map<String, List<String>> testMap = new HashMap<String, List<String>>();
        List<String> testList = new LinkedList<String>();
        testList.add("Dies");
        testList.add("ist");
        testList.add("ein");
        testList.add("Test");
        testMap.put(test4, testList);
        persSetServ.saveMapWithStringList(testMap, "persSetMapTest.json");
    }

    /**
     * Test.
     */
    @Test
    public void testReadMap() {
        testSaveMap();
        Map<String, List<String>> testMap = persSetServ.readMapWithStringList("persSetMapTest.json");
        Assert.assertNotNull(testMap.get(test4));
        Assert.assertEquals("Dies", testMap.get(test4).remove(0));
        Assert.assertEquals("ist", testMap.get(test4).remove(0));
        Assert.assertEquals("ein", testMap.get(test4).remove(0));
        Assert.assertEquals("Test", testMap.get(test4).remove(0));
    }
}
