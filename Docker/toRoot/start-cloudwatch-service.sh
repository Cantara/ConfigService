#!/bin/bash
# Script to start aws logs push
# DO NOT CALL THIS MANUALLY! Intended for use by supervisord only.

date +" --- RUNNING $(basename $0) %Y-%m-%d_%H:%M:%S --- "
set -x

if [ "$AWS_CLOUDWATCH_LOGGING_ENABLED" = "true" ]; then
    # Enables aws cloudwatch plugin in aws cli
    aws configure set plugins.cwlogs cwlogs
    # Sets container id used by as part of log-stream-name
    CONTAINER_ID="$(cat /proc/self/cgroup | grep docker | grep -o -E '[0-9a-f]{64}' | head -n 1 | cut -c1-12)"
    #replaces log-group and log-stream name in aws-cloudwatch.conf
    sed -i -e "s/{CONTAINER_ID}/$CONTAINER_ID/g" config_override/aws-cloudwatch.conf
    sed -i -e "s/{AWS_LOG_GROUP}/$AWS_LOG_GROUP/g" config_override/aws-cloudwatch.conf

    START_APP_COMMAND="aws logs push --region=eu-west-1 --config-file config_override/aws-cloudwatch.conf"
    echo "Starting aws logs push"
    $START_APP_COMMAND
fi
