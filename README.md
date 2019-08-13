# ConfigService

![Build Status](https://jenkins.quadim.ai/buildStatus/icon?job=ConfigService) - [![Project Status: Active – The project has reached a stable, usable state and is being actively developed.](http://www.repostatus.org/badges/latest/active.svg)](http://www.repostatus.org/#active) [![Known Vulnerabilities](https://snyk.io/test/github/Cantara/ConfigService/badge.svg)](https://snyk.io/test/github/Cantara/ConfigService)

The goal of ConfigService is to provide the server-component of a controlled application instance regime, where both software upgrades and corresponding configurations of home-built java applications and services are managed. This should solve the update and configuration pain-point in micro-service architectures and for client installs of homegrown applications.

This component is as of now *work-in*progress*, and some of the main goals and discussions can be found on our wiki:  https://wiki.cantara.no/display/JAU/Java-Auto-Update+and+ConfigService+Home

## Install and use Standalone

Scripts and procedures can be found on our wiki: 
https://wiki.cantara.no/display/JAU/Installation+and+getting+started+with+ConfigService

## Install and use in a Docker container.
There are three Docker builds available for ConfigService

1. [Alpine Linux with bundled application](DockerAlpine/README.md). Minimal image where the Docker image acts as the deployment unit.
2. [Ubuntu without bundled application](Docker/README.md). Pull scripts inside Docker container which downloads application after container has started.
3. [Ubuntu with AWS log agent](DockerAWS/README.md). Same as option 2, but with AWS dependency

See the respective READMEs for details on each Docker setup.

### Quickstart with Alpine Linux and Postgres persistence
_Note that Postgres is not included inside the container. Therefore you need to create an empty database beforehand
and update the parameters to match your database endpoint_
```
docker run -d -p 80:8086 --name configservice \
-e persistence.type=postgres \
-e postgres.url=jdbc:postgresql://CHANGE_TO_YOUR_DB:5432/configservice \
-e postgres.username=YOUR_DB_USER \
-e postgres.password=YOUR_DB_PASS \
-e login.user=readonlyuser \
-e login.password=CHANGETHIS \
-e login.admin.user=admin \
-e login.admin.password=CHANGETHISASWELL \
cantara/configservice-alpine
```

Verify that the application is running
```
curl localhost/jau/health
```

## Configuration
For persistence ConfigService supports Postgres and MapDb (although deprecated!)

See [properties file](src/main/resources/application.properties) for settings to override.

## Verify that the application is running

## How to use ConfigService.

### Add a new application

```
curl -u admin:configservice  -i -X POST -H "Content-Type:application/json"   -d '{ "artifactId": "myApplication" }'  https://whydahdev.cantara.no/jau/application

# Return  {"id":"0e139a12-57c0-4a48-8999-7f32c63ff9ad","artifactId":"myApplication"}
```

### Add a new application configuration

```
curl -u admin:configservice -vX POST https://whydahdev.cantara.no/jau/application/0e139a12-57c0-4a48-8999-7f32c63ff9ad/config  -d @myApplicationConfig.json --header "Content-Type: application/json"

```

Example file: myApplicationConfig.json
```
{
	"id": "f9e14326-b9df-46ba-826f-afad3392cf54",
	"name": "whydah-dropwizard-demo-1.0",
	"lastChanged": "2016-06-27T22:05:18.994Z",
	"downloadItems": [{
		"url": "http://mvnrepo.cantara.no/service/local/repositories/releases/content/no/cantara/dropwizard-hello-world-application/1.0/dropwizard-hello-world-application-1.0.jar",
		"username": null,
		"password": null,
		"metadata": {
			"groupId": "no.cantara",
			"artifactId": "dropwizard-hello-world-application",
			"version": "1.0",
			"packaging": "jar",
			"lastUpdated": null,
			"buildNumber": null
		}
	}],
	"configurationStores": [{
		"fileName": "hello-world.yml",
		"properties": {
			"version": "0.8-beta-5-SNAPSHOT"
		}
	}],
	"eventExtractionConfigs": [{
		"groupName": "jau",
		"tags": [{
			"tagName": "jau",
			"regex": ".*",
			"filePath": "logs/jau.log"
		}]
	}],
	"startServiceScript": "java -jar dropwizard-hello-world-application-1.0.jar"
}
```


### Update a specific client

```
curl -u admin:conservice  -i -X PUT -H "Content-Type:application/json"   -d '{"clientId":"bed9e97b-2090-4fe0-bfac-ab44252151e6","applicationConfigId":"b2435492-e011-4d15-b2a3-815395608fa7","autoUpgrade":true}'  https://whydahdev.cantara.no/jau/client/bed9e97b-2090-4fe0-bfac-ab44252151e6```
```

### Rollback a specific client

```
curl -u admin:conservice  -i -X PUT -H "Content-Type:application/json"   -d '{"clientId":"bed9e97b-2090-4fe0-bfac-ab44252151e6","applicationConfigId":"b2435492-e011-4d15-b2a3-815395608fa7","autoUpgrade":true}'  https://whydahdev.cantara.no/jau/client/bed9e97b-2090-4fe0-bfac-ab44252151e6```
```


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
