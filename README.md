# configservice
https://wiki.cantara.no/display/dev/ConfigService

## Install and use 

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

