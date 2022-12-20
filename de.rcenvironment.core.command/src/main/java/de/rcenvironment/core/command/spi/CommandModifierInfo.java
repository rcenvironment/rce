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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.rcenvironment.core.command.common.CommandException;

/**
 * Contains information of all positional and named parameters as well as the flags a command has.
 * 
 * @author Sebastian Nocke
 *
 */
public class CommandModifierInfo {

    private static final String LIST_PARAMETER_SEPERATOR = ",";
    
    private List<AbstractCommandParameter> positionalParameters;
    
    private List<CommandFlag> flags;
    
    private List<NamedParameter> namedParameters;
    
    public CommandModifierInfo() {
        this.positionalParameters = Collections.emptyList();
        this.flags = Collections.emptyList();
        this.namedParameters = Collections.emptyList();
    }
    
    public CommandModifierInfo(List<AbstractCommandParameter> positionalParameters,
            List<CommandFlag> flags, List<NamedParameter> namedParameters) {
        this.positionalParameters = positionalParameters;
        this.flags = flags;
        this.namedParameters = namedParameters;
    }
    
    public CommandModifierInfo(AbstractCommandParameter[] positionalParameters,
            CommandFlag[] flags, NamedParameter[] namedParameters) {
        this.positionalParameters = Arrays.asList(positionalParameters);
        this.flags = Arrays.asList(flags);
        this.namedParameters = Arrays.asList(namedParameters);
    }
    
    public CommandModifierInfo(AbstractCommandParameter[] positionalParameters,
            CommandFlag[] flags) {
        this.positionalParameters = Arrays.asList(positionalParameters);
        this.flags = Arrays.asList(flags);
        this.namedParameters = Collections.emptyList();
    }
    
    public CommandModifierInfo(CommandFlag[] flags, NamedParameter[] namedParameters) {
        this.positionalParameters = Collections.emptyList();
        this.flags = Arrays.asList(flags);
        this.namedParameters = Arrays.asList(namedParameters);
    }
    
    public CommandModifierInfo(NamedParameter[] namedParameters) {
        this.positionalParameters = Collections.emptyList();
        this.flags = Collections.emptyList();
        this.namedParameters = Arrays.asList(namedParameters);
    }
    
    public CommandModifierInfo(AbstractCommandParameter[] positionalParameters, NamedParameter[] namedParameters) {
        this.positionalParameters = Arrays.asList(positionalParameters);
        this.flags = Collections.emptyList();
        this.namedParameters = Arrays.asList(namedParameters);
    }
    
    public CommandModifierInfo(AbstractCommandParameter[] positionalParameters) {
        this.positionalParameters = Arrays.asList(positionalParameters);
        this.flags = Collections.emptyList();
        this.namedParameters = Collections.emptyList();
    }
    
    public CommandModifierInfo(CommandFlag[] flags) {
        this.positionalParameters = Collections.emptyList();
        this.flags = Arrays.asList(flags);
        this.namedParameters = Collections.emptyList();
    }
    
    public boolean isEmpty() {
        return positionalParameters.isEmpty() && flags.isEmpty() && namedParameters.isEmpty();
    }
    
    public List<AbstractCommandParameter> getPositionals() {
        return positionalParameters;
    }
    
    public List<CommandFlag> getFlags() {
        return flags;
    }
    
    public List<NamedParameter> getNamedParameters() {
        return namedParameters;
    }
    
    public ParsedCommandModifiers parseModifiers(CommandContext context) throws CommandException {
        
        List<String> tokens = context.consumeRemainingTokens();
        
        if (!tokens.isEmpty() && isEmpty()) {
            throw CommandException.syntaxError("This command has no flags nor parameters", context);
        }
        
        Map<String, AbstractParsedCommandParameter> parsedNamed = findNamedParameters(tokens, context);
        Set<CommandFlag> parsedFlags = findFlags(tokens);
        List<AbstractParsedCommandParameter> parsedPositionals = parsePositionalParameters(tokens, context);
        
        if (tokens.isEmpty()) {
            return new ParsedCommandModifiers(parsedPositionals, parsedFlags, parsedNamed);
            
        } else {
            throw CommandException.syntaxError("Not all tokens could be parsed", context);    
        }
        
    }
    
    private List<AbstractParsedCommandParameter> parsePositionalParameters(List<String> tokens,
            CommandContext context) throws CommandException {
        
        List<AbstractParsedCommandParameter> parsed = new ArrayList<>();
        
        if (!positionalParameters.isEmpty()) {
            
            for (int i = 0; i < positionalParameters.size(); i++) {
                
                if (tokens.isEmpty()) {
                    addRemainingPositionals(context, parsed, i);
                    break;
                }
                
                AbstractCommandParameter parameter = positionalParameters.get(i);
                
                if (parameter instanceof ListCommandParameter) {
                    
                    String combined = tokens.get(0);
                    
                    while (tokens.get(0).endsWith(LIST_PARAMETER_SEPERATOR)) {
                        combined += tokens.get(1);
                        tokens.remove(0);
                        
                    }
                    
                    tokens.remove(0);
                    
                    parsed.add(parameter.parseToken(combined, context));
                    
                } else {
                    parsed.add(parameter.parseToken(tokens.remove(0), context));
                    
                }
                
            }
            
        }
        
        if (parsed.size() != positionalParameters.size()) {
            throw CommandException.wrongNumberOfParameters(context);
        }
        
        return parsed;
    }
    
    private void addRemainingPositionals(CommandContext context, List<AbstractParsedCommandParameter> parsed, int startIndex)
            throws CommandException {

        for (int i = startIndex; i < positionalParameters.size(); i++) {
            if (positionalParameters.get(i).standardValue().getResult() != null) {
                parsed.add(positionalParameters.get(i).standardValue());
            } else {
                throw CommandException.syntaxError("Not enough positional parameters", context);
            }
        }
        
    }
    
    private Set<CommandFlag> findFlags(List<String> tokens) {
        
        Set<CommandFlag> foundFlags = new HashSet<>();
        
        for (CommandFlag flag : flags) {
            
            for (int i = 0; i < tokens.size(); i++) {
                
                if (flag.isFitting(tokens.get(i))) {
                    
                    foundFlags.add(flag);
                    tokens.remove(i);
                    
                    break;
                    
                }
                
            }
            
        }
        
        return foundFlags;
    }
    
    private Map<String, AbstractParsedCommandParameter> findNamedParameters(List<String> tokens,
            CommandContext context) throws CommandException {
        
        Map<String, AbstractParsedCommandParameter> parsedParameters = new HashMap<>();
        
        for (NamedParameter named : namedParameters) {
            
            boolean found = false;
            
            for (int i = 0; i < tokens.size();) {
                
                if (named.getName().equalsIgnoreCase(tokens.get(i))) {
                    
                    tokens.remove(i);
                    
                    if (named instanceof NamedSingleParameter) {
                        
                        found = true;
                        AbstractCommandParameter parameter = ((NamedSingleParameter) named).getParameterType();
                        
                        if (parameter instanceof ListCommandParameter) {
                            
                            String value = "";
                            
                            while (tokens.get(i).endsWith(LIST_PARAMETER_SEPERATOR)) {
                                value += tokens.remove(i);
                                
                            }
                            
                            value += tokens.remove(i);
                            
                            parsedParameters.put(named.getName(), parameter.parseToken(value, context));
                            
                        } else {
                            
                            parsedParameters.put(named.getName(), parameter.parseToken(tokens.get(i), context));
                            
                            tokens.remove(i);
                            
                        }
                        
                    } else if (named instanceof NamedMultiParameter) {
                        
                        NamedMultiParameter multi = (NamedMultiParameter) named;
                        AbstractCommandParameter[] parameters = multi.getParameterTypes();
                        AbstractParsedCommandParameter[] parsed = new AbstractParsedCommandParameter[parameters.length];
                        
                        int index = 0;
                        
                        while (index < parameters.length) {
                            
                            String token;
                            
                            if (tokens.size() > i) {
                                token = tokens.get(i);
                            } else {
                                token = "-";
                            }
                            
                            if (!token.startsWith("-")) {
                                parsed[index] = parameters[index].parseToken(token, context);
                                tokens.remove(i);
                                
                            } else {
                                if (index < multi.getMinParameters()) {
                                    throw CommandException.syntaxError(named.getName()
                                            + " needs at least " + multi.getMinParameters() + " values", context);
                                }
                                parsed[index] = parameters[index].standardValue();
                                
                            }
                            
                            index++;
                            
                        }
                        
                        parsedParameters.put(named.getName(), new ParsedMultiParameter(parsed));
                        
                    }
                    
                }
                i++;
            }
            
            if (!found && named.getParseWhenNotPresent()) {
                parsedParameters.put(named.getName(), named.getStandardValue());
            }
        }
        
        return parsedParameters;
        
    }
      
}
