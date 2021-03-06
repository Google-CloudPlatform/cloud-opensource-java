#!/bin/bash

# Fail on any error.
set -e
# Display commands being run.
set -x

cd github/cloud-opensource-java

# M2_HOME is not used since Maven 3.5.0 https://maven.apache.org/docs/3.5.0/release-notes.html
mkdir -p ${HOME}/.m2
cp settings.xml ${HOME}/.m2

./mvnw -V -B -ntp clean install javadoc:jar

cd gradle-plugin
./gradlew build publishToMavenLocal
