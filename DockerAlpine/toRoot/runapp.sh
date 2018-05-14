#!/bin/bash

APP=app.jar
JAVA_PARAMS="-Dlogback.configurationFile=./config_override/logback-default.xml -Xms64m -Xmx512m"

date +" --- RUNNING $(basename $0) %Y-%m-%d_%H:%M:%S --- "
set -x

if [ "$AWS_PARAMETER_STORE_ENABLED" = "true" ]; then
    python GetPropertiesFromParameterStore.py $AWS_PARAMETER_STORE_PATH /config_override/application_override.properties
    if [ $? -eq 0 ]; then
        echo 'GetPropertiesFromParameterStore.py exited successfully!'
    else
        echo 'GetPropertiesFromParameterStore.py exited with error (non null exit code)'
        exit 1
    fi
fi

set +x

java $JAVA_PARAMS $JAVA_PARAMS_OVERRIDE -jar $APP
