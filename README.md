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

### Add a new application

```
curl -u admin:conservice  -i -X POST -H "Content-Type:application/json"   -d '{ "artifactId": "myApplication" }'  https://whydahdev.cantara.no/jau/application

# Return  {"id":"0e139a12-57c0-4a48-8999-7f32c63ff9ad","artifactId":"myApplication"}
```

### Add a new application configuration

```
curl -u admin:conservice -vX POST https://whydahdev.cantara.no/jau/application/0e139a12-57c0-4a48-8999-7f32c63ff9ad/config  -d @myApplicationConfig.json --header "Content-Type: application/json"

```

### Update a specific client

### Rollback a specific client

### Update all clients of an application

### Browse clients 

```
curl -u admin:conservice  https://whydahdev.cantara.no/jau/client/
```

### Browse active client status for an application

```
curl -u admin:conservice  https://whydahdev.cantara.no/jau/application/cantara-demo/status
```

## Example application configuration 

```
{
  "name": "hello-world_0.1-SNAPSHOT",
  "lastChanged": "2016-03-09T07:50:18.994Z",
  "downloadItems": [
    {
      "url": "repository-url/hello-world-0.1-SNAPSHOT.jar",
      "username": "basic-auth-username",
      "password": "basic-auth-password",
      "metadata": {
        "groupId": "com.example",
        "artifactId": "hello-world-service",
        "version": "0.1-SNAPSHOT",
        "packaging": "jar",
        "lastUpdated": null,
        "buildNumber": null
      }
    }
  ],
  "configurationStores": [
    {
      "fileName": "helloworld_overrides.properties",
      "properties": {
        "hello.world.message": "Hello World"
      }
    }
  ],
  "eventExtractionConfigs" : [ {
     "groupName" : "hw-agent",
     "tags" : [ {
       "tagName" : "jau",
       "regex" : ".*",
       "filePath" : "logs/jau.log"
     }, {
       "tagName" : "agent",
       "regex" : ".*",
       "filePath" : "logs/hwagent.log"
     } ]
   } ],

  ],
  "startServiceScript": "java -jar hello-world-0.1-SNAPSHOT.jar"
}
```
