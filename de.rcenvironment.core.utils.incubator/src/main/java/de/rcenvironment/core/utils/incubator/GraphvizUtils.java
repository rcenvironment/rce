/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.rcenvironment.core.toolkitbridge.transitional.TextStreamWatcherFactory;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.common.textstream.TextStreamWatcher;
import de.rcenvironment.core.utils.common.textstream.receivers.PrefixingTextOutForwarder;
import de.rcenvironment.core.utils.executor.CommandLineExecutor;
import de.rcenvironment.core.utils.executor.LocalCommandLineExecutor;

/**
 * Utility class for preparing data for the "Graphviz" tools (http://www.graphviz.org/) and invoking its commands. Note that Graphviz must
 * be installed and available via the PATH variable for the command invocations.
 * 
 * @author Robert Mischke
 */
public final class GraphvizUtils {

    private GraphvizUtils() {
        // prevent instantiation
    }

    /**
     * Builder class for "dot" Graphviz files.
     * 
     * @author Robert Mischke
     */
    public static class DotFileBuilder {

        private List<String> vertexOrder = new ArrayList<String>();

        private List<String> edgeScriptLines = new ArrayList<String>();

        private Map<String, String> vertexProperties = new HashMap<String, String>();

        // private Map<String, String> edgeProperties = new HashMap<String, String>();

        private String graphName;

        public DotFileBuilder(String graphName) {
            this.graphName = graphName;
        }

        /**
         * Adds a vertex to the underlying graph.
         * 
         * @param id the internal string id of the vertex; should be alphanumeric and start with a letter
         * @param label the label to annotate the node with; double quotes must be escaped (TODO test/review behaviour)
         */
        public void addVertex(String id, String label) {
            vertexOrder.add(id);
            vertexProperties.put(id, StringUtils.format("label=\"%s\"", escapeValue(label)));
        }

        /**
         * Adds an additional property to an already-added vertex. Note that the new key-value pair is simply appended; previous properties
         * with the same key are preserved in the generated script file.
         * 
         * @param id the id of the already-added vertex
         * @param key the key of the new property
         * @param value the value of the new property
         */
        public void addVertexProperty(String id, String key, String value) {
            String oldValue = vertexProperties.get(id);
            // append new key-value pair
            String newValue = StringUtils.format("%s,%s=\"%s\"", oldValue, key, escapeValue(value));
            vertexProperties.put(id, newValue);
        }

        /**
         * Adds an edge to the underlying graph.
         * 
         * @param fromId the internal id of the source node
         * @param toId the internal id of the target node
         * @param label the label to annotate the edge with; double quotes must be escaped (TODO test/review behaviour)
         */
        public void addEdge(String fromId, String toId, String label) {
            // simple, direct approach; change to a map to allow adding properties later
            edgeScriptLines.add(StringUtils.format("    v_%s->v_%s[label=\"%s\"];\n",
                escapeVertexId(fromId), escapeVertexId(toId), escapeValue(label)));
        }

        /**
         * Adds an edge to the underlying graph.
         * 
         * @param fromId the internal id of the source node
         * @param toId the internal id of the target node
         * @param label the label to annotate the edge with; double quotes must be escaped (TODO test/review behaviour)
         * @param properties key/value properties to set for the edge
         */
        public void addEdge(String fromId, String toId, String label, Map<String, String> properties) {
            String additionalProperties = "";
            if (properties != null) {
                for (Map.Entry<String, String> entry : properties.entrySet()) {
                    additionalProperties =
                        StringUtils.format("%s,%s=\"%s\"", additionalProperties, entry.getKey(), escapeValue(entry.getValue()));
                }
            }
            edgeScriptLines.add(StringUtils.format("    v_%s->v_%s[label=\"%s\"%s];\n",
                escapeVertexId(fromId), escapeVertexId(toId), escapeVertexId(label), additionalProperties));
        }

        /**
         * Returns the generated script content. It is typically written to a file and then processed by the Graphviz "dot" command.
         * 
         * @return the script content
         */
        public String getScriptContent() {
            StringBuilder buffer = new StringBuilder();
            buffer.append("digraph ");
            buffer.append(graphName);
            buffer.append(" {\n");
            for (String vertexId : vertexOrder) {
                buffer.append(StringUtils.format("    v_%s[%s];\n", escapeVertexId(vertexId), vertexProperties.get(vertexId)));
            }
            for (String edge : edgeScriptLines) {
                buffer.append(edge);
            }
            buffer.append("}\n");
            return buffer.toString();
        }

        private String escapeVertexId(String original) {
            // guard against "null" values
            if (original == null) {
                throw new NullPointerException();
            }
            String result = original;
            result = result.replaceAll("-", "_");
            return result;
        }

        private String escapeValue(String original) {
            // guard against "null" values
            if (original == null) {
                return "<null>";
            }
            // TODO test if this covers all possible escapes
            String result = original;
            result = result.replaceAll("\"", "\\\\\""); // replacement: ["] -> [\"]
            return result;
        }

    }

    /**
     * Creates a builder (see "builder pattern") for "dot" Graphviz files.
     * 
     * @param graphName the internal name of the graph
     * @return the created builder
     */
    public static DotFileBuilder createDotFileBuilder(String graphName) {
        return new DotFileBuilder(graphName);
    }

    /**
     * Invokes the Graphviz "dot" command to render a Grapviz dot script file to a PNG file.
     * 
     * @param gvFile the file containing the dot script
     * @param pngFile the output PNG file to be written
     * @param outputReceiver a receiver for text output during invocation
     * @return true on successful execution
     */
    public static boolean renderDotFileToPng(File gvFile, File pngFile, TextOutputReceiver outputReceiver) {

        try {
            CommandLineExecutor executor = new LocalCommandLineExecutor(gvFile.getParentFile());
            // inherit PATH variable
            executor.setEnv("PATH", System.getenv("PATH"));
            // invoke
            // TODO handle spaces/quotes/... internally
            executor.start("dot " + gvFile.getAbsolutePath() + " -Tpng -o " + pngFile.getAbsolutePath());
            TextOutputReceiver stdoutCapture = new PrefixingTextOutForwarder("StdOut: ", outputReceiver);
            TextOutputReceiver stderrCapture = new PrefixingTextOutForwarder("StdErr: ", outputReceiver);
            TextStreamWatcher outWatcher = TextStreamWatcherFactory.create(executor.getStdout(), stdoutCapture);
            TextStreamWatcher errWatcher = TextStreamWatcherFactory.create(executor.getStderr(), stderrCapture);
            outWatcher.start();
            errWatcher.start();
            int exitCode = executor.waitForTermination();
            if (exitCode == 0) {
                return true;
            }
            outputReceiver.addOutput("WARNING: Running graphviz returned an exit code of " + exitCode);
        } catch (IOException e) {
            outputReceiver.onFatalError(e);
        } catch (InterruptedException e) {
            outputReceiver.onFatalError(e);
        }
        return false;
    }

}
