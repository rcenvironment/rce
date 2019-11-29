/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.scripting;


/**
 * Enumeration of all supported scripting languages. Usually the set of
 * supported languages should be determined dynamically based on the registered
 * script engines, but as the configuration environment for scripts might be
 * decoupled from their runtime environment, the set of languages available at
 * runtime are not determinable by the configuration facilities (GUIs for
 * example). Therefore a common set of possibly supported languages has to be
 * preset.
 * 
 * @author Christian Weiss
 */
public enum ScriptLanguage {

    /** Jython. */
    Jython("Jython", "py"),
    /** JavaScript. */
    JavaScript("JavaScript", "js"),
    /** Python. */
    Python("Python", "py");

    private final String name;

    private final String extension;

    ScriptLanguage(final String name, final String extension) {
        this.name = name;
        this.extension = extension;
    }

    public String getName() {
        return name;
    }

    public String getExtension() {
        return extension;
    }

    /**
     * Returns the enum constant with the specified name.
     * 
     * @param name
     *            the name of the enum constant to return
     * @return the enum constant with the specified name
     * @throws IllegalArgumentException
     *             if the specified name is unknown
     */
    public static ScriptLanguage getByName(final String name) throws IllegalArgumentException {
        assert name != null && !name.isEmpty();
        ScriptLanguage result = null;
        for (final ScriptLanguage language : values()) {
            if (language.name.equals(name)) {
                result = language;
                break;
            }
        }
        /*
        final List<ScriptEngineFactory> factories = ENGINE_MANAGER
                .getEngineFactories();
    factoriesLoop:
        for (final ScriptEngineFactory factory : factories) {
            if (factory.getNames().contains(name)) {
                for (final ScriptLanguage language : values()) {
                    if (factory.getNames().contains(language.getName())) {
                        result = language;
                        break factoriesLoop;
                    }
                }
            }
        }
        */
        if (result == null) {
            throw new IllegalArgumentException();
        }
        return result;
    }

    /**
     * Returns the enum constant with the specified extension.
     * 
     * @param extension
     *            the extension of the enum constant to return
     * @return the enum constant with the specified extension
     * @throws IllegalArgumentException
     *             if the specified extension is unknown
     */
    public static ScriptLanguage getByExtension(final String extension)
        throws IllegalArgumentException {
        assert extension != null && !extension.isEmpty();
        assert extension.matches("^[-_a-zA-Z0-9]+$");
        ScriptLanguage result = null;
        for (final ScriptLanguage language : values()) {
            if (language.extension.equals(extension)) {
                result = language;
                break;
            }
        }
        if (result == null) {
            throw new IllegalArgumentException();
        }
        return result;
    }

    /**
     * {@link Exception} to signal, that no {@link javax.script.ScriptEngine} is
     * registered to handle scripts of a language.
     * 
     * @author Christian Weiss
     */
    public static class NoEngineException extends RuntimeException {

        private static final long serialVersionUID = -593738405021184970L;

        private final ScriptLanguage language;

        public NoEngineException(final ScriptLanguage language) {
            this.language = language;
        }

        public NoEngineException(final ScriptLanguage language,
                final Throwable cause) {
            super(cause);
            this.language = language;
        }

        public NoEngineException(final String message,
                final ScriptLanguage language) {
            super(message);
            this.language = language;
        }

        public NoEngineException(final String message, final Throwable cause,
                final ScriptLanguage language) {
            super(message, cause);
            this.language = language;
        }

        public ScriptLanguage getLanguage() {
            return language;
        }

    }

}
