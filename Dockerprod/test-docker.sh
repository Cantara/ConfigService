#!/bin/bash

docker build -t configservice-dev .

CONFIG_FILE="$(dirname $(pwd))/config_override.properties"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "config_override.properties in parent directory is missing"
    exit 1
fi

LOCAL=""
if [ "$1" == "local" ]; then
    (cd .. && mvn package) || exit 2
    JAR=$(cd ../target && ls -1t *.jar | head -1)
    LOCAL="-v $(dirname $(pwd))/target/$JAR:/home/configservice/configservice.jar"
fi

echo "Starting instance. Do 'docker exec -it configservice-dev bash' to get shell"

CONFIG="-v $CONFIG_FILE:/home/configservice/config_override.properties"
docker run --rm -p 8086:8086 --name configservice-dev $CONFIG $LOCAL configservice-dev

