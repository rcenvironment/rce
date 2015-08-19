/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.excel.common;


/**
 * Simple Thread-Class for calling garbage collector manually.
 *
 * @author Markus Kunde
 */
public class GarbageDestroyer implements Runnable {

    @Override
    public void run() {
        System.gc();

    }

}
