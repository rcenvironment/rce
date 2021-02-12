/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.xpathchooser;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility method to parse XPath expressions.
 * 
 * @author Heinrich Wendel
 * @author Markus Kunde
 */
public final class XPathParser {

    private static final String SEP = "/";
    private static final String START = "[A-Za-z_:]";
    private static final String NAME = "[A-Za-z0-9$§%\\.,;\\-_:]*";
    private static final String QNAME = START + NAME;
    private static final String QVAL = "\"?[^<&\"]+\"?";
    private static final String OPT_VAL = "(=" + QVAL + ")?";
    private static final String QOPTVALNAME = QNAME + OPT_VAL;
    private static final String PREDICATE = "([/]?" + QOPTVALNAME + "(/" + QOPTVALNAME + ")*" + "(/@" + QNAME + OPT_VAL + ")?|(@"
            + QOPTVALNAME + ")?)";

    /**
     * Private constructor for utility class.
     */
    private XPathParser() {

    }

    /**
     * Splits a xpath path expression into its components.
     * 
     * @param expr The xpath expression to split.
     * @return The parsed XPathExpr.
     */
    public static XPathLocation parse(final String expr) {
        final XPathLocation xpath = new XPathLocation();

        boolean inString = false;
        boolean inPredicate = false;
        XPathStep cur = null;
        for (int i = 0; i < expr.length(); i++) {
            final String c = expr.substring(i, i + 1);
            if (c.equals(SEP) && !inString && !(cur instanceof XPathPredicate)) {
                if (i == 0) {
                    xpath.setAbsolute(true);
                }
                cur = null;
            } else if (c.equals("@") && !inString && !(cur instanceof XPathPredicate)) {
                cur = new XPathAttribute();
                xpath.getSteps().add(cur);
            } else if (c.equals("[") && !inString && !(cur instanceof XPathPredicate)) {
                if (cur instanceof XPathNode) {
                    inPredicate = true;
                    ((XPathNode) cur).setPredicate(new XPathPredicate());
                    cur = ((XPathNode) cur).getPredicate();
                }
            } else if (c.equals("]") && !inString) {
                cur = null;
                inPredicate = false;
            } else if (c.equals("=") && !inString && !inPredicate) {   // should be the last element of the path
                cur = new XPathValue();
                xpath.getSteps().add(cur);
            } else {
                if (cur == null) {
                    cur = new XPathNode();
                    xpath.getSteps().add(cur);
                }
                cur.setValue(cur.getValue() + c);
            }

            if (c.equals("\"")) {
                inString = !inString;
            }
        }
        return xpath;
    }

    /**
     * Validates if we can parse the given xpath. That does not mean that it is generally not a
     * valid xpath, but not for our cases at least.
     * 
     * @param path
     *            The xpath path expression to validate.
     * @return True or false.
     */
    public static boolean isValid(String path) {
        final XPathLocation loc = parse(path);
        final List<XPathStep> steplist = loc.getSteps();
        boolean correct = true;
        for (final XPathStep step: steplist) {
            if (step instanceof XPathNode) {
                final XPathPredicate pred = ((XPathNode) step).getPredicate();
                if (pred != null) {
                    if (!Pattern.matches(PREDICATE, pred.getValue())) {
                        correct = false;
                        break;
                    }
                }
                final String nonpred = ((XPathNode) step).getValue();
                if (!Pattern.matches(QNAME, nonpred)) {
                    correct = false;
                    break;
                }
            } else if (step instanceof XPathAttribute) {
                final int index = steplist.indexOf(step);
                if (index < steplist.size() - 2) { // allowed on two last positions
                    correct = false;
                    break;
                }
                if (index == steplist.size() - 2) {
                    final XPathStep lastStep = steplist.get(steplist.size() - 1);
                    if (!(lastStep instanceof XPathValue) || (!Pattern.matches(NAME, step.getValue()))) {
                        correct = false;
                    }
                    break;
                }
                if (!Pattern.matches(QOPTVALNAME, step.getValue())) {
                    correct = false;
                    break;
                }
            } else {
                correct = false;
                break;
            }
        }
        return correct;
    }

    /**
     * Returns only the base component, i.e. everything but the last step of a path.
     * 
     * @param path The path to split the base from.
     * @return The base component.
     */
    public static String splitBase(final String path) {
        final XPathLocation expr = XPathParser.parse(path);
        if (expr.getSteps().size() > 0) {
            final XPathStep step = expr.getSteps().remove(expr.getSteps().size() - 1);
            if (step instanceof XPathValue) {
                expr.getSteps().remove(expr.getSteps().size() - 1);
            }
        }

        return expr.toString();
    }

    /**
     * Removes a part from the beginning of the path for set root, thus ignoring the predicates.
     * 
     * @param org  The original path.
     * @param remove The part to remove.
     * @return The path with the beginning replaced
     */
    public static String replacePath(final String org, final String remove) {
        if (remove.equals("") || org.equals("")) {
            return "";
        }
        final XPathLocation orgLocation = parse(org);
        final List<XPathStep> orgSteps = orgLocation.getSteps();
        final List<XPathStep> removeSteps = parse(remove).getSteps();
        for (int i = 0; i < removeSteps.size(); i ++) {
            if (orgSteps.get(0).getValue().equals(removeSteps.get(i).getValue())) {
                orgSteps.remove(0);
            } else {
                break;
            }
        }
        final XPathLocation retLocation = new XPathLocation();
        retLocation.setAbsolute(orgLocation.isAbsolute());
        retLocation.getSteps().addAll(orgSteps);
        final String ret = retLocation.toString();
        if (ret.equals(SEP)) {
            return "";
        }
        return ret;
//        final XPathLocation orgPath = parse(org);
//        for (final XPathStep step: orgPath.getSteps()) {
//            if (step instanceof XPathNode) {
//                ((XPathNode) step).setPredicate(null);
//            }
//        }
//        final String ret = orgPath.toString();
//        return ret.replaceFirst(Pattern.quote(remove), "");
    }
    
    /**
     * Removes a part from the beginning of the path.
     * 
     * @param org  The path to remove leading stuff from, including predicates.
     * @param remove The part to remove.
     * @return The path with the beginning removed.
     */
    public static String removeLeadingPath(final String org, final String remove) {
        if (remove.equals("")) {
            return org;
        }
        final XPathLocation orgLocation = parse(org);
        final List<XPathStep> orgSteps = orgLocation.getSteps();
        final List<XPathStep> removeSteps = parse(remove).getSteps();
        for (int i = 0; i < removeSteps.size(); i ++) {
            if (orgSteps.get(0).toString().equals(removeSteps.get(i).toString())) {
                orgSteps.remove(0);
            } else {
                break;
            }
        }
        final XPathLocation retLocation = new XPathLocation();
        retLocation.setAbsolute(orgLocation.isAbsolute());
        retLocation.getSteps().addAll(orgSteps);
        final String ret = retLocation.toString();
        if (ret.equals(SEP)) {
            return "";
        }
        return ret;
    }
    
    /**
     * Helper function to get slash-less and predicate-less xpath elements.
     * @param xpath The xpath to separate
     * @return The string array with step names
     */
    public static String[] parseValuesToStrings(final String xpath) {
        final XPathLocation location = parse(xpath);
        final List<XPathStep> steps = location.getSteps();
        final String[] ret = new String[steps.size()];
        for (int i = 0; i < steps.size(); i ++) {
            ret[i] = steps.get(i).getValue();
        }
        return ret;
    }

    /**
     * Helper function to get slash-less xpath elements including predicates.
     * @param xpath The xpath to separate
     * @return The string array with step names and predicates
     */
    public static String[] parseToStrings(final String xpath) {
        final XPathLocation location = parse(xpath);
        final List<XPathStep> steps = location.getSteps();
        final String[] ret = new String[steps.size()];
        for (int i = 0; i < steps.size(); i ++) {
            ret[i] = steps.get(i).toString();
        }
        return ret;
    }
    
    /**
     * Helper function to remove all predicates from an xpath.
     * @param xpath the xpath
     * @return The cleaned version of the xpath.
     */
    public static String removePredicates(final String xpath) {
        final XPathLocation location = parse(xpath);
        final List<XPathStep> steps = location.getSteps();
        final StringBuilder ret = new StringBuilder();
        for (int i = 0; i < steps.size(); i ++) {
            ret.append(SEP).append(steps.get(i).getValue());
        }
        return ret.toString();
    }

}
