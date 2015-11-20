#!/bin/bash
# Script to start Java application.
# DO NOT CALL THIS MANUALLY! Intended for use by supervisord only.

date +" --- RUNNING $(basename $0) %Y-%m-%d_%H:%M:%S"

APP=configservice.jar
JAVA_PARAMS="-Dlogback.configurationFile=./logback-default.xml -Xms128m -Xmx1024m"
source config_override/service_override.properties # this might override JAVA_PARAMS and version to run

START_APP_COMMAND="/usr/bin/java $JAVA_PARAMS -jar $APP"

echo "Starting $APP"
$START_APP_COMMAND
