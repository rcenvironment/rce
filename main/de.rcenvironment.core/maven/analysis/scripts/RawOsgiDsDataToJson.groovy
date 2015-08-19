/*
 * Copyright (C) 2013 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

import java.util.jar.Manifest;

import groovy.json.JsonBuilder

new RawOsgiDsDataToJsonProcessor().run()

/**
 * Extracts and aggregates OSGI-DS data from OSGI-INF/*.xml files 
 * and writes it to a JSON file. 
 * 
 * @author Robert Mischke
 */
class RawOsgiDsDataToJsonProcessor {
    def run() {
        def builder = new JsonBuilder()
        def json = builder {
        }
        json['components'] = []

        def rootDir = new File('../../..')
        rootDir.listFiles().each {
            File projectDir = it
            findOsgiDsFiles(projectDir, 'OSGI-INF', json)
            findOsgiDsFiles(projectDir, 'OSGI-INF/new', json) // temporary hack
        }

        def jsonOut = new File('target/osgi-ds-data.json')
        jsonOut.write(builder.toPrettyString())
    }

    def findOsgiDsFiles(File dir, subdir, jsonOut) {
        def osgiDsFolder = new File(dir, subdir);
        if (osgiDsFolder.isDirectory()) {
            osgiDsFolder.listFiles().each {
                File osgiDsFile = it
                if (osgiDsFile.isFile()) {
                    println "Parsing $osgiDsFile"
                    parseOsgiDsFile(dir, osgiDsFile, jsonOut)
                }
            }
        }
    }

    def parseOsgiDsFile(File projectDir, File osgiDsFile, jsonOut) {
        try {
            def xml = new XmlParser().parse(osgiDsFile)
            def component = [
                implementation:(xml.implementation.@class[0]),
                project: projectDir.name,
                provides: [],
                references: []]
            xml.service.provide.each {
                component.provides += it.@interface
            }
            xml.reference.each {
                component.references += it.@interface
            }
            jsonOut.components += component
        } catch(e) {
            println e
        }
    }
}

