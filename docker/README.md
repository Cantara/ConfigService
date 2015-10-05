# Running the Docker container

## How it works
* Docker Hub can be used to automatically generate a docker build
* The Java-application itself is [downloaded from Nexus](http://mvnrepo.cantara.no/content/repositories/releases/no/cantara/jau/configservice/) by a script in the docker build
* The version to download is specified when running the Docker image
* The configuration override of the application is volume mounted when running the Docker image

## Prerequisites
* Docker daemon running

## Set up

### Initial install

#### Building config
Create a configuration file, e.g. `configservice.config_override.properties`. Have a look at
[config.properties](../src/main/resources/config.properties) and make sure to override needed properties.

#### Creating Docker-instance
* Make sure the config file exists
* Skip `--restart=always` if doing this locally to avoid it to start with your computer
* Replace SNAPSHOT with another version if wanted

```bash
CONFIG_FILE="$(pwd)/configservice.config_override.properties"
VERSION=SNAPSHOT
#VERSION=0.3 # example for specifying version
sudo docker run -d -p 80:8086 --name configservice --restart=always -e "CONFIGSERVICE_VERSION=$VERSION" -v "$CONFIG_FILE:/home/configservice/config_override.properties" itcapra/cantara-configservice:latest
```

Connecting to instance for debugging:
```bash
sudo docker exec -it -u configservice configservice bash

# in docker instance:
cd ~
ls -l logs
```

### Upgrading without touching docker config
```bash
sudo docker exec -it -u configservice configservice bash
cd ~
export CONFIGSERVICE_VERSION=0.3 # replace this version number
./update-service.sh
pkill java # kill the running version, causing new to be started
```

## Testing docker locally
See [test-docker.sh](test-docker.sh).

This script can be run with `./test-docker.sh local` to also run `mvn package` and use jar from development.
