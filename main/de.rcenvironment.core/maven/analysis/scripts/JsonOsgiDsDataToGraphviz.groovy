/*
 * Copyright (C) 2013 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

import java.security.Provider;

import groovy.json.JsonSlurper

new JsonOsgiDsDataToGraphvizProcessor(projectFilter: 'de\\.rcenvironment\\.core\\..*').run()

/**
 * Generates a Graphviz "dot" diagram file from aggregated OSGI-DS data.
 * 
 * @author Robert Mischke
 */
class JsonOsgiDsDataToGraphvizProcessor {

    def projectFilter = '.*'

    def run() {
        def INDENT = '  '

        def jsonContent = new File("target/osgi-ds-data.json").text
        def data = new JsonSlurper().parseText(jsonContent)
        def bundles = data.bundles

        def gvOut = new File('target/osgi-ds-dependencies.gv')
        gvOut.withWriter { gv ->

            def printAtLevel = { level, text ->
                gv.println INDENT * level + text
            }

            printAtLevel 0, 'digraph rce_dependencies {'
            printAtLevel 1, 'graph[charset=latin1,rankdir=TB];'

            def allComponents = data.components
            def filteredComponents = []
            allComponents.each {

                if (it.project =~ projectFilter) {
                    filteredComponents += it
                } else {
                    println "Ignoring component $it.implementation due to project filter"
                }
            }

            def providersByInterface = [:]
            allComponents.each {
                def impl = it.implementation
                it.provides.each {
                    def interf = it
                    if (providersByInterface[interf] == null) providersByInterface[interf] = []
                    providersByInterface[interf] += impl
                }
            }

            filteredComponents.each {
                def impl = it.implementation
                printAtLevel 1, classToId(impl) + '[label="' + classToLabel(impl) + '"];'
            }

            filteredComponents.each {
                def refererImpl = it.implementation
                it.references.each {
                    def refInterface = it
                    def providers = providersByInterface[refInterface]
                    if (providers) {
                        providers.each {
                            def providerImpl = it
                            printAtLevel 1, classToId(refererImpl) + '->' + classToId(providerImpl) + ';'  // + '[label="' + refInterface + '"];'
                        }
                    }
                }
            }

            printAtLevel 0, '}'
        }
    }

    def classToId(name) {
        return 'p_' + name.replace('de.rcenvironment.', '').replaceAll('[^a-zA-Z0-9]', '_')
    }

    def classToLabel(name) {
        return name.replace('de.rcenvironment.', '')
    }
}