#!/bin/bash
# this script make sure the main application is available and runs it

APP=configservice.jar

while [ ! -f "$APP" ]; do
    echo "Fetching application from remote server"
    ./update-service.sh

    if [ ! -f "$APP" ]; then
        >&2 echo "Retrieving application failed, trying again in 30 sec"
        sleep 30
    fi
done

/usr/bin/java -Dlogback.configurationFile=./logback.xml -jar "$APP"
