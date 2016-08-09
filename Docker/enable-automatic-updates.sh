#!/bin/bash
#Run this script if you wish to run this container with automatic updates to version specified in service_override.properties
#If this script has not been run, you have to manually run ./update-service and ./stop-service to deploy a new version of configservice.

crontab -u configservice /etc/cron.d/configservice