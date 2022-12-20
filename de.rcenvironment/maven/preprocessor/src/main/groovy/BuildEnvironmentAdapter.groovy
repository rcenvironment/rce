/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 *
 * Author: Robert Mischke
 */

class BuildEnvironmentAdapter {

    private final mavenProperties
    private final File mainProjectsRootDir
    private final String secondStagePomPath

    BuildEnvironmentAdapter(project) {
        mavenProperties = project.properties

        mainProjectsRootDir = new File(project.basedir, '../../..').canonicalFile
        secondStagePomPath = new File(mainProjectsRootDir, 'de.rcenvironment/maven/secondStage/pom.xml').absolutePath
    }

    def getBuildType() {
        return System.getProperty('rce.maven.buildType') ?: 'snapshot'
    }

    def getBuildVariant() {
        return System.getProperty('rce.maven.buildVariant')
    }

    def getSecondStagePomPathString() {
        return secondStagePomPath
    }

    // TODO move more specific properties into this class?
    def getProperty(String key) {
        return System.getProperty(key)
    }

    def setSecondStageArguments(secondStageArgs) {
        // unlike the System properties that are being read, this must be set on the Maven properties to take effect
        mavenProperties.setProperty('rce.maven.internal.secondStageArguments', secondStageArgs)
    }

    def abortBuild(String message) {
        throw new IllegalArgumentException("Error in configuration preprocessor: $message")
    }
}
