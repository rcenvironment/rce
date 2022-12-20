/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Contains information of all parsed positional and named parameters as well as the given flags.
 * 
 * @author Sebastian Nocke
 *
 */
public class ParsedCommandModifiers {

    private List<AbstractParsedCommandParameter> positionalParameters;
    
    private Set<CommandFlag> activeFlags;
    
    private Map<String, AbstractParsedCommandParameter> activeNamedParameters;
    
    public ParsedCommandModifiers() {
        this.positionalParameters = new ArrayList<>();
        this.activeFlags = new HashSet<>();
        this.activeNamedParameters = new HashMap<>();
    }
    
    public ParsedCommandModifiers(List<AbstractParsedCommandParameter> positionalParameters,
            Set<CommandFlag> activeFlags, Map<String, AbstractParsedCommandParameter> activeNamedParameters) {
        this.positionalParameters = positionalParameters;
        this.activeFlags = activeFlags;
        this.activeNamedParameters = activeNamedParameters;
    }
    
    public ParsedCommandModifiers(AbstractParsedCommandParameter[] positionalParameters,
            CommandFlag[] activeFlags, Map<String, AbstractParsedCommandParameter> activeNamedParameters) {
        this.positionalParameters = Arrays.asList(positionalParameters);
        this.activeFlags = new HashSet<>(Arrays.asList(activeFlags));
        this.activeNamedParameters = activeNamedParameters;
    }
    
    public boolean isEmpty() {
        return positionalParameters.isEmpty() && activeFlags.isEmpty() && activeNamedParameters.isEmpty();
    }
    
    public AbstractParsedCommandParameter getPositionalCommandParameter(int index) {
        if (positionalParameters.size() > index) {
            return positionalParameters.get(index);
        } else {
            return null;
        }
    }
    
    public boolean hasCommandFlag(String flag) {
        Optional<CommandFlag> foundFlag = (activeFlags.stream().filter(f -> f.isFitting(flag)).findFirst());
        return foundFlag.isPresent();   
    }
    
    public Set<CommandFlag> getActiveFlags() {
        return activeFlags;
    }
    
    public boolean hasParsedParameter(String parameter) {
        return activeNamedParameters.get(parameter) != null;
    }
    
    public AbstractParsedCommandParameter getCommandParameter(String parameter) {
        if (activeNamedParameters.containsKey(parameter)) {
            return activeNamedParameters.get(parameter);
        } else {
            return null;
        }
    }
    
    public Set<String> getNamedParameterList() {
        return activeNamedParameters.keySet();
    }
    
}
