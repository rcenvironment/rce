#!/bin/bash

DEFAULT_BUILD_VARIANT=product.singleStep

cd de.rcenvironment
echo Current MAVEN_OPTS: $MAVEN_OPTS
echo Current MAVEN_CLI_OPTS: $MAVEN_CLI_OPTS
mvn $MAVEN_CLI_OPTS -Drce.maven.buildVariant=$DEFAULT_BUILD_VARIANT
