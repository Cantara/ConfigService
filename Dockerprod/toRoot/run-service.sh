#!/bin/bash

APP=configservice.jar
JAVA_PARAMS="-Dlogback.configurationFile=./logback.xml -Xms128m -Xmx1024m"
source config_override/service_override.properties # this might override JAVA_PARAMS and version to run

START_APP_COMMAND="/usr/bin/java $JAVA_PARAMS -jar $APP"

./update-service.sh

echo "Starting $APP"
$START_APP_COMMAND
