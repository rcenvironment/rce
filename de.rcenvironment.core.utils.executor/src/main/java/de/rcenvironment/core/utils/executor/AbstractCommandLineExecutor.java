/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.executor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.exec.OS;

/**
 * Common base class for {@link CommandLineExecutor} implementations.
 * 
 * @author Robert Mischke
 * 
 */
public abstract class AbstractCommandLineExecutor implements CommandLineExecutor {

    protected final Map<String, String> env = new HashMap<String, String>();

    @Override
    public void setEnv(String key, String value) {
        if (value != null) {
            env.put(key, value);
        } else {
            env.remove(key);
        }
    }

    @Override
    public String getEnv(String key) {
        return env.get(key);
    }

    /**
     * A default implementation that concatenates the individual commands with "&&" and delegates the resulting string to
     * {@link #start(String)}. IF a different behavior is needed, subclasses should override this method without calling super().
     * 
     * @param commandStrings the command lines to execute
     * 
     * @throws IOException if an I/O error occurs on start of the target executable
     */
    @Override
    public void startMultiLineCommand(String[] commandStrings) throws IOException {
        final StringBuilder sb = new StringBuilder();
        // TODO needs testing!
        for (String line : commandStrings) {
            // Check for empty lines and comments
            if (!line.isEmpty() && !(OS.isFamilyWindows() && (line.toLowerCase().startsWith("rem") || line.startsWith("::")))
                && !(OS.isFamilyUnix() && line.startsWith("#"))) {
                if (sb.length() != 0) {
                    // note: the "&&" is not wrapped in spaces to avoid modifying expected parameters
                    // (for example, "echo test" -> "echo test ")
                    sb.append("&&");
                }
                sb.append(line);
            }
        }
        start(sb.toString());
    }

}
