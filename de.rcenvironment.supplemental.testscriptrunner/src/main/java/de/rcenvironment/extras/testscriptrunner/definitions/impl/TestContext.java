/**
 * 
 */
package de.rcenvironment.extras.testscriptrunner.definitions.impl;


/**
 * @author Matthias Y. Wagner
 *
 */
public class TestContext {
    private static String testInstanceRootDir = "";
    private static String workflowProjectDir = "";

    public static void setTestedInstanceInstallationRoot(String installationId) {
        testInstanceRootDir = installationId.substring(installationId.indexOf("local:") + 6);
    }
    public static String getTestedInstanceInstallationRoot() {
        return testInstanceRootDir;
    }

    public static void setWorkflowProjectDirectory(String workflowDir) {
        workflowProjectDir = workflowDir;
    }
    public static String getWorkflowProjectDirectory() {
        return workflowProjectDir;
    }

}
