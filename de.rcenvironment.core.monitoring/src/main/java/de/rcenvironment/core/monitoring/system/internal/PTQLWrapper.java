/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.internal;

/**
 * Wrapper class to simplify PTQL query building.
 * 
 * @author David Scholz
 */
public final class PTQLWrapper {

    private PTQLWrapper() {}

    /**
     * 
     * Factory method for query implementation.
     * 
     * @return PTQL query.
     */
    public static Query createQuery() {
        return new Query() {

            @Override
            public String createQueryString(Attribute attribute, Operator operator) {
                return attribute.getAttribute() + operator.getOperator();
            }
        };
    }

    /**
     * PTQL query.
     * 
     * @author David Scholz
     */
    public interface Query {

        /**
         * Creates a query as a string.
         * 
         * @param attribute The PTQL attribute.
         * @param operator The PTQL operator.
         * @return PTQL query as string.
         */
        String createQueryString(Attribute attribute, Operator operator);
    }

    /**
     * PTQL operator.
     * 
     * @author David Scholz
     */
    private interface Operator {

        String getOperator();
    }

    /**
     * 
     * PTQL attribute.
     * 
     * @author David Scholz.
     */
    private interface Attribute {

        String getAttribute();
    }

    /**
     * Get PTQL query for getting the equal operator.
     * 
     * @return PTQL equal query.
     */
    public static Operator eq() {
        return new Operator() {

            @Override
            public String getOperator() {
                return "eq=";
            }
        };
    }

    /**
     * Get PTQL query for getting the not equal operator.
     * 
     * @return PTQL not equal query.
     */
    public static Operator ne() {
        return new Operator() {

            @Override
            public String getOperator() {
                return "ne=";
            }
        };
    }

    /**
     * Get PTQL query for getting for getting the ends with operator.
     * 
     * @return PTQL ends with query.
     */
    public static Operator ew() {
        return new Operator() {

            @Override
            public String getOperator() {
                return "ew=";
            }
        };
    }

    /**
     * Get PTQL query for getting the starts with operator.
     * 
     * @return PTQL starts with query.
     */
    public static Operator sw() {
        return new Operator() {

            @Override
            public String getOperator() {
                return "sw=";
            }
        };
    }

    /**
     * Get PTQL query for getting the contains value (substring) operator.
     * 
     * @return PTQL contains value query.
     */
    public static Operator ct() {
        return new Operator() {

            @Override
            public String getOperator() {
                return "ct=";
            }
        };
    }

    /**
     * Get PTQL query for getting the regular expression value operator.
     * 
     * @return PTQL regular expression value matches querry.
     */
    public static Operator re() {
        return new Operator() {

            @Override
            public String getOperator() {
                return "re=";
            }
        };
    }

    /**
     * Get PTQL query for getting the process id attribute.
     * 
     * @return PTQL attribute for getting pid.
     */
    public static Attribute pid() {
        return new Attribute() {

            @Override
            public String getAttribute() {
                return "Pid.Pid.";
            }
        };
    }

    /**
     * Get PTQL query for getting the file containing the pid attribute.
     * 
     * @return PTQL attribute for getting the file containing the pid.
     */
    public static Attribute file() {
        return new Attribute() {

            @Override
            public String getAttribute() {
                return "Pid.PidFile.";
            }
        };
    }

    /**
     * Get PTQL query for getting the windows service name used to pid from the service manager attribute.
     * 
     * @return PTQL attribute for getting the windows service name.
     */
    public static Attribute serviceName() {
        return new Attribute() {

            @Override
            public String getAttribute() {
                return "Pid.Service.";
            }
        };
    }

    /**
     * Get PTQL query for getting the base name of the process executable attribute.
     * 
     * @return PTQL attribute for getting the state name.
     */
    public static Attribute stateName() {
        return new Attribute() {

            @Override
            public String getAttribute() {
                return "State.Name.";
            }
        };
    }

    /**
     * 
     * Get PTQL query for getting ppid of given process.
     * 
     * @return PTQL attribute for getting ppid.
     */
    public static Attribute statePPID() {
        return new Attribute() {

            @Override
            public String getAttribute() {
                return "State.Ppid.";
            }
        };
    }

    /**
     * Get PTQL query for getting the user name of the process owner attribute.
     * 
     * @return PTQL attribute for getting the name of the process owner.
     */
    public static Attribute credUserName() {
        return new Attribute() {

            @Override
            public String getAttribute() {
                return "CredName.User.";
            }
        };
    }

    /**
     * Get PTQL query for getting the group name of the process owner attribute.
     * 
     * @return PTQL attribute for getting the group name of the process owner.
     */
    public static Attribute credGroupName() {
        return new Attribute() {

            @Override
            public String getAttribute() {
                return "CredName.Group.";
            }
        };
    }

    /**
     * Get PTQL query for getting the user id of the process owner attribute.
     * 
     * @return PTQL attribute for getting the UID of the process owner.
     */
    public static Attribute credUID() {
        return new Attribute() {

            @Override
            public String getAttribute() {
                return "Cred.Uid.";
            }
        };
    }

    /**
     * Get PTQL query for getting the group id of the process owner attribute.
     * 
     * @return PTQL attribute for getting the GID of the process owner.
     */
    public static Attribute credGID() {
        return new Attribute() {

            @Override
            public String getAttribute() {
                return "Cred.Gid.";
            }
        };
    }

    /**
     * Get PTQL query for getting the effective user id of the process owner attribute.
     * 
     * @return PTQL attribute for getting the effective UID of the process owner.
     */
    public static Attribute credEUID() {
        return new Attribute() {

            @Override
            public String getAttribute() {
                return "Cred.Euid.";
            }
        };
    }

    /**
     * Get PTQL query for getting the effective group id of the process owner attribute.
     * 
     * @return PTQL attribute for getting the effective GID of the process owner.
     */
    public static Attribute credEGID() {
        return new Attribute() {

            @Override
            public String getAttribute() {
                return "Cred.Egid.";
            }
        };
    }

    /**
     * Get PTQL query for getting the full path name of the process executable.
     * 
     * @return PTQL attribute for getting the full path name of the process exe.
     */
    public static Attribute exeName() {
        return new Attribute() {

            @Override
            public String getAttribute() {
                return "Exe.Name.";
            }
        };
    }

    /**
     * Get PTQL query for getting the current working directory of the process.
     * 
     * @return PTQL attribute for getting the current working directory of the process.
     */
    public static Attribute cwd() {
        return new Attribute() {

            @Override
            public String getAttribute() {
                return "Exe.Cwd.";
            }
        };
    }

}
