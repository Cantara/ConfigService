#!/bin/bash
# This file will build a Docker image, create a data volume container and run an application container
# from the image. Use it to test that your Docker configuration builds successfully.

# Change these
PROJECT_NAME=projectname
IMAGE_NAME=docker-dev-test
IMAGE_NAME_DATA=$IMAGE_NAME-data

docker rm -v -f $IMAGE_NAME
docker build -t $IMAGE_NAME .

docker create --name $IMAGE_NAME-data $IMAGE_NAME

docker run -d --name $IMAGE_NAME --volumes-from $IMAGE_NAME_DATA $IMAGE_NAME

echo "Starting instance. Do 'docker exec -it $IMAGE_NAME bash' to get shell"


