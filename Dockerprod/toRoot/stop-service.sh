#!/bin/bash
# Kills a running process based on $APP variable specified in config_override/service_override.properties

#Print timestamp with some whitespace for readability in log
printf "\n"
date +"%Y-%m-%d_%H:%M:%S"

SERVICE_OVERRIDE=config_override/service_override.properties
source $SERVICE_OVERRIDE

if [ -f $SERVICE_OVERRIDE ]; then
    if [ -n "$APP" ]; then
        #Find app name and kill by PID so we don't accidentally kill any other java program
        kill `ps -ef | grep $APP | grep -v grep | awk '{print $2}'`
        if [ "$?" = "0" ]; then
            echo "$APP was killed. Supervisor should restart it."
        else
            echo "Could not kill '$APP'. Maybe it is not running?"
        fi
    else
        echo "APP variable not set. Cannot find application to kill"
    fi
else
    echo "Cannot find $SERVICE_OVERRIDE file. Don't know what to kill!"
fi