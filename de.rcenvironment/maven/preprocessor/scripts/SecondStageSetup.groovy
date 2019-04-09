/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 *
 * Author: Robert Mischke
 */

class SecondStageSetup {

	private String pomFile, arguments = '', buildScope, goals
	private boolean skipHelpGeneration = false, skipGuidesGeneration = false
	
	public void setPomFile(file) {
		pomFile = file
	}
	
	public void setGoals(goals) {
		this.goals = goals
	}
	
	public void setBuildScope(scope) {
		buildScope = scope
	}
	
	public void setSkipDocumentation(boolean skipHelp, boolean skipGuides) {
		skipHelpGeneration = skipHelp
		skipGuidesGeneration = skipGuides
	}
	
	public void addCustomArgument(part) {
		arguments += " $part"
	}
	
	public void addProperty(key, value) {
		arguments += " -D$key=$value"
	}
	
	public String getFinalString() {
		String finalArguments = arguments
		if (skipHelpGeneration) finalArguments += ' -P !generateHelpFromDocbook'
		if (skipGuidesGeneration) finalArguments += ' -Drce.maven.skipDocumentation'
		return "-f $pomFile -Drce.maven.buildScope=${buildScope}$finalArguments -B $goals"
	}

}
