# Running the Docker container 

## How it works
* The configuration override of the application is volume mounted when running the Docker image

## Prerequisites
* Docker daemon running (see https://wiki.cantara.no/display/FPP/Docker+cheat+sheet)

## Set up

### Initial install

#### Building config
Create a configuration file, e.g. `config_override.properties`.

#### Creating Docker-instance
* Make sure the config file exists
* Skip `--restart=always` if doing this locally to avoid it to start with your computer.

Connecting to instance for debugging:
```bash
docker exec -it -u configservice configservice bash -c 'cd /home/configservice; exec "${SHELL:-sh}"'
```

## Testing docker locally
See [test-docker.sh](test-docker.sh).

This script can be run with `./test-docker.sh local` to also run `mvn package` and use jar from development.
