# ConfigService

The goal of ConfigService is to provide the server-component of a controlled application instance regime, where both software upgrades and corresponding configurations of home-built java applications and services are managed. This should solve the update and configuration pain-point in micro-service architectures and for client installs of homegrown applications.

This component is as of now *work-in*progress*, and some of the main goals and discussions can be found on our wiki:  https://wiki.cantara.no/display/JAU/Java-Auto-Update+and+ConfigService+Home


## Install and use in a Docker container.

### Install or upgrade Docker 

https://docs.docker.com/installation/ubuntulinux/

```
wget -qO- https://get.docker.com/ | sh
```

###  Install data volume container and application TODO 
```
sudo docker pull cantara/configservice
sudo docker create -v /data --name configservice-data cantara/configservice 
sudo docker run -d -p 80:7000 --volumes-from configservice-data --name configservice3107 cantara/configservice
sudo docker run -d -p 80:7000 --name configservice3107 cantara/configservice
```


### Check that application is up 

http://localhost/jau/serviceconfig/query?clientid=clientid1


## Backup 

See https://docs.docker.com/userguide/dockervolumes/#backup-restore-or-migrate-data-volumes


## Development 

### Build and run for development

```
sudo docker build -t cantara/configservice .
sudo docker run -d -p 80:7000 --name configservice3107 cantara/configservice
```

* To stop and remove all containers: 
```
sudo docker stop $(sudo docker ps -a -q) && sudo docker rm $(sudo docker ps -a -q)
```

* To log in to take a look: 
```
sudo docker exec -it configservice3107 bash
```

## How to use ConfigService.

### Add a new software version

### Add a new software configuration

### Update a specific client

### Rollback a specific client

### Update all clients of an application


### Browse active client status

