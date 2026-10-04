#!/bin/bash
set -e
PROJ=/Users/davey/WorkBuddy/2026-09-14-13-30-04/ru_urok1/RuLearnApp
TOOL=/Users/davey/WorkBuddy/2026-09-14-13-30-04/ru_urok1/tooling
export ANDROID_HOME=$HOME/Library/Android/sdk

# locate extracted JDK 21
JDKDIR=$(find "$TOOL" -maxdepth 1 -type d -name 'jdk*' | head -1)
export JAVA_HOME="$JDKDIR/Contents/Home"
# locate gradle
GRADLEDIR=$(find "$TOOL" -maxdepth 1 -type d -name 'gradle-8.9*' | head -1)
GRADLE="$GRADLEDIR/bin/gradle"

echo "JAVA_HOME=$JAVA_HOME"
echo "GRADLE=$GRADLE"
"$JAVA_HOME/bin/java" -version
cd "$PROJ"
"$GRADLE" assembleDebug --stacktrace 2>&1 | tail -80
