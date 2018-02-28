#!/bin/bash

set -e

docker build -t cantara/configservice-alpine .
docker run -it --rm -p 80:8086 cantara/configservice
