#!/bin/sh
DIR="$(cd "$(dirname "$0")" && pwd)"
CLASSPATH="$DIR/gradle/wrapper/gradle-wrapper.jar"
exec java -Dorg.gradle.appname="gradlew" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
