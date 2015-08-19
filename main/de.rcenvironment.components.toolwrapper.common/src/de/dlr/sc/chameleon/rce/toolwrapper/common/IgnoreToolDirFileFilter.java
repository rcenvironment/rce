/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.dlr.sc.chameleon.rce.toolwrapper.common;

import java.io.File;
import java.io.FileFilter;


/**
 * FileFilter for ignoring specific files.
 *
 * @author Markus Kunde
 */
public class IgnoreToolDirFileFilter implements FileFilter {

    private String[] patterns = null;
    
    
    /**
     * Constructor for FileFilter for ignoring specific files.
     * 
     * @param rawPattern comma Separated pattern which exclude files
     */
    public IgnoreToolDirFileFilter(String rawPattern) {
        patterns = rawPattern.split(",");
    }
    
    @Override
    public boolean accept(File file) {
        String filename = file.getName();
        
        for (int i = 0; i < patterns.length; i++) {
            if (filename.matches(patterns[i].trim().replace(".", "\\.").replace("*", ".*"))) {
                return false;
            }
        }
        return true;
    } 
}
