Docker image for running ConfigService.
=======================================
This image will by default pack the latest ConfigService jar file (SNAPSHOT).
--build-arg DOCKER_TAG=ConfigService-0.5.1 may be used to pack a specific release-version. The format matches the git tag format.
The curl logic may be replaced by simple copies to get local jar-files into the image locally.

Building with latest SNAPSHOT version
```
docker build -t cantara/configservice-alpine .
```

Build with specific application version
```
docker build -t cantara/configservice-alpine . --build-arg DOCKER_TAG=configservice-0.8-beta-12
```

## Configuration

#### Logging
Where to put logs. See config_override/logback.xml
* LOGBACK_CANTARA_LEVEL (Loglevel of no.cantara logs. Defaults to info if not set)

#### Application properties
The configuration can be overridden by passing a file with the `--env-file` command when running the image.
E.g:
```
--env-file application_override.properties
```
Alternatively, the properties can be overridden by passing them one by one.
E.g:
```
-e login.user=user -e login.password=configservice
```

The image also supports configuration through AWS Parameter Store. To enable add the follow env variables when running the image:
```
-e AWS_PARAMETER_STORE_ENABLED=true -e AWS_PARAMETER_STORE_OUTPUTPATH=/path/in/paramstore
```