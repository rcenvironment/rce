/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 *
 * Author: Robert Mischke
 */

class SecondStageSetup {

	private String pomFile, arguments = '', buildScope, goals
	
	public void setPomFile(file) {
		pomFile = file
	}
	
	public void setGoals(goals) {
		this.goals = goals
	}
	
	public void setBuildScope(scope) {
		buildScope = scope
	}
	
	public void addCustomArgument(part) {
		arguments += " $part"
	}
	
	public void addProperty(key, value) {
		arguments += " -D$key=$value"
	}
	
	public String getFinalString() {
		return "-f $pomFile -Drce.maven.buildScope=${buildScope}$arguments -B $goals"
	}

}
