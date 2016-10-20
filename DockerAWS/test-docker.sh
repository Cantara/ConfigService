#!/bin/bash
# This file will build a Docker image, create a data volume container and run an application container
# from the image. Use it to test that your Docker configuration builds successfully.

# Change these
IMAGE_NAME=configservice-cw-test
IMAGE_NAME_DATA=$IMAGE_NAME-data

docker rm -v -f $IMAGE_NAME_DATA
docker rm -v -f $IMAGE_NAME

docker build -t $IMAGE_NAME .

docker create --name $IMAGE_NAME-data $IMAGE_NAME

docker run -d --name $IMAGE_NAME \
    --volumes-from $IMAGE_NAME_DATA \
    -e AWS_CLOUDWATCH_LOGGING_ENABLED="true" \
    -e AWS_LOG_GROUP="/cantara/stream/test" \
    -e AWS_ACCESS_KEY_ID="***" \
    -e AWS_SECRET_ACCESS_KEY="***" \
    $IMAGE_NAME

echo "Starting instance. Do \"docker exec -it $IMAGE_NAME bash -c 'cd /home/configservice; exec /bin/bash'\" to get shell"


