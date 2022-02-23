/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 *
 * Author: Robert Mischke
 */

class RCEBuildPreprocessor {

	// Note: The following maps were introduced at a time when several external p2 repositories 
	// (e.g. Dakota and TiGLViewer binaries) were referenced independently during the build.
	// As of 8.1.0 and later, a simple assignment would suffice, but there is no real drawback in 
	// keeping the map approach either (e.g. for future extensions). -- misc_ro, June 2017

	final repositoryUrlSegments = [
		'platform': 'platform',
		'intermediate': 'intermediate'
	]

	final defaultRepositoryUrlSuffixes = [
		'snapshot': [
			'platform': 'releases/10.3.1',
			'intermediate': 'releases/10.3.1'
		],
		'rc_or_release': [
			'platform': 'releases/10.3.1',
			'intermediate': 'releases/10.3.1'
		]
	]
	
	final DEFAULT_REPOSITORIES_ROOT_URL = 'https://software.dlr.de/updates/rce/10.x/repositories/'
	
	private maven
	private buildType     // snapshot, rc, release
	private buildVariant  // full product, core only, ...
	private repositoryUrlSuffixSet  // snapshot vs. rc_or_release
	private defaultUrlRoot
	private SecondStageSetup secondStageSetup
	
	RCEBuildPreprocessor(mavenIntegration) {
		maven = mavenIntegration
		secondStageSetup = new SecondStageSetup()
	}

	def configure() {
		buildType = maven.getProperty('rce.maven.buildType') ?: 'snapshot'
		switch (buildType) {
			case 'snapshot':
				repositoryUrlSuffixSet = 'snapshot'
				break
			case 'rc':
			case 'release':
				repositoryUrlSuffixSet = 'rc_or_release'
				break
			default:
				maven.abortBuild("Unknown RCE build type '${buildType}'")
		}
		println("Build type is '$buildType', using repository suffix set '$repositoryUrlSuffixSet'")
		
		buildVariant = maven.getProperty('rce.maven.buildVariant')
		println("Running build variant '$buildVariant'")
		
		defaultUrlRoot = maven.getProperty('rce.maven.repositories.default.rootUrl') ?: DEFAULT_REPOSITORIES_ROOT_URL
		
		configureSecondStage()
		def secondStageArgs = secondStageSetup.getFinalString()
		
		maven.setProperty('rce.maven.internal.secondStageArguments', secondStageArgs)
		println("Running second-stage Maven with arguments string '${secondStageArgs}'")
	}
	
	def configureSecondStage() {

		def projectRoot = maven.getProperty('rce.maven.preprocessor.projectRoot')
		def secondStagePomPath = maven.getProperty('rce.maven.preprocessor.secondStagePomPath')
		
		def setup = secondStageSetup  // convenience shortcut
		setup.setPomFile("${projectRoot}${secondStagePomPath}")  // POM file to run second stage from
		
		buildVariant += ':default'  // set default value for empty build options
		def parts = buildVariant.split(':')
		def buildScope = parts[0]
		// TODO rename to "buildVariant" for clarity?
		def buildOptions = parts[1]
		
		setup.setBuildScope(buildScope)
		setup.setGoals('clean package')  // default goals; may be overridden by build scope or options
		setup.addCustomArgument("-Drce.maven.buildType=$buildType")  // forward build type for tagging
		
		switch (buildScope) {
			case 'nothing':
			case 'core.minimalSubset':
			case 'core':
			case 'coreAndComponents':
				addRepository(setup, 'foundation', 'platform')
				break;
			case 'intermediateRepo':
				addRepository(setup, 'foundation', 'platform')
				break;
			case 'helpResourcesOnly':
				setup.setGoals('clean generate-resources process-resources')
				addRepository(setup, 'foundation', 'platform')
				break;
			case 'product.usingIntermediateRepo':
				addRepository(setup, 'foundation', 'intermediate')
				setup.addCustomArgument('-Drce.maven.assembleProducts -Drce.maven.createProductArchives')
				break;
			case 'product.singleStep':
				addRepository(setup, 'foundation', 'platform')
				setup.addCustomArgument('-Drce.maven.assembleProducts -Drce.maven.createProductArchives')
				break;
			case 'coreAndComponentTests':
				// TODO add "with reporting" flag
				addRepository(setup, 'foundation', 'platform')
				setup.setGoals('clean integration-test')
				break;
			default:
				maven.abortBuild("Unknown RCE build scope '${buildScope}'")
		}
		
		switch(buildOptions) {
			case 'default':
				break
			case 'skipDocumentation':
				setup.setSkipDocumentation(true, true)
				break
			case 'withTests':
			case 'withExtendedTests':
				if (buildOptions == 'withExtendedTests') {
					setup.addCustomArgument('-Drce.tests.runExtended')
				}
				setup.setSkipDocumentation(true, true)
				setup.setGoals('clean verify')
				break
			case 'withTestsAndReporting':
				setup.addCustomArgument('-Drce.maven.generateCoverageReport -Drce.maven.collectJQAData')
				setup.addCustomArgument("-Drce.maven.preprocessor.projectRoot=${projectRoot}")
				setup.setSkipDocumentation(true, true)
				setup.setGoals('clean verify')
				break
			case 'withJqaAnalysisAndReport':
				setup.addCustomArgument('-Drce.maven.collectJQAData')
				// despite using the "verify" goal, do not compile or run tests
				setup.addCustomArgument('-Dmaven.test.skip=true')
				setup.addCustomArgument("-Drce.maven.preprocessor.projectRoot=${projectRoot}")
				setup.setSkipDocumentation(true, true)
				setup.setGoals('clean verify')
				break
			case 'updateJqaAnalysisAndReport':
				// do not clean or recollect, as this will delete the previous scan data
				setup.addCustomArgument('-Drce.maven.preserveGlobalData')
				setup.addCustomArgument("-Drce.maven.preprocessor.projectRoot=${projectRoot}")
				setup.setSkipDocumentation(true, true)
				break
			case 'printEnvironment':
				// replace the actual second-stage build with a system properties dump
				setup.setGoals('help:system')
				break
			default:
				maven.abortBuild("Unknown RCE build option '${buildOptions}'")
		}
	}
	
	// TODO document the two ids
	def addRepository(setup, String targetId, String sourceId = null) {
		setup.addCustomArgument(createCommandLinePartForRepository(targetId, sourceId))
	}
	
	// TODO document the two ids
	def createCommandLinePartForRepository(String targetId, String sourceId = null) {
		sourceId = sourceId ?: targetId
		def url = determineRepositoryUrl(sourceId)
		return "-Drce.maven.repositories.${targetId}.url=${url}"
	}

	def determineRepositoryUrl(String id) {
		def explicitUrl = maven.getProperty("rce.maven.repositories.${id}.url")
		if (explicitUrl) {
			return explicitUrl
		} else {
			def rootUrl = maven.getProperty("rce.maven.repositories.${id}.rootUrl") ?: defaultUrlRoot
			def baseUrl = "${rootUrl}${repositoryUrlSegments[id]}"  // without trailing slash
			
			def buildId = maven.getProperty("rce.maven.repositories.${id}.buildId")
			if (!buildId) {
				// default version
				return "${baseUrl}/${defaultRepositoryUrlSuffixes[repositoryUrlSuffixSet][id]}"
			} else {
				// explicit version
				return "${baseUrl}/${buildId}"
			}
		}
	}
}
