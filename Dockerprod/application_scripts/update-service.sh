#!/bin/bash
# Script to download deployment unit from a Maven artifact repository.
# How to upgrade application when using release versions (when running SNAPSHOTs, a
# cron job will run the scripts for you):
## Change version in config_override/service_override.properties
## ./update-service.sh
## ./stop-service.sh
## supervisord will automatically start the service again

#Timestamp with some whitespace for readability in log
date +" --- RUNNING $(basename $0) %Y-%m-%d_%H:%M:%S --- "

# Default repos, override in service_override.properties
releaseRepo=http://mvnrepo.cantara.no/content/repositories/releases
snapshotRepo=http://mvnrepo.cantara.no/content/repositories/snapshots

function create_or_replace_symlink() {
  if [ -h $artifactId.jar ]; then
     unlink $artifactId.jar
  fi
  ln -s $jarfile $artifactId.jar
  echo "Updated symlink '$artifactId.jar' to point to '$jarfile'."
}

# Copy template if service_override.properties does not exist.
SERVICE_OVERRIDE=config_override/service_override.properties
if [ -f $SERVICE_OVERRIDE ]; then
    echo "Using $SERVICE_OVERRIDE."
else
  echo "No $SERVICE_OVERRIDE found. Copying service_override.properties_template"
  # TODO check if service_override.properties and exit if it does not.
  cp config_override/service_override.properties_template $SERVICE_OVERRIDE
fi
source $SERVICE_OVERRIDE # this might override variables


# Copy logback-default.xml if file does not exist.
LOGBACK_FILE=config_override/logback.xml
if [ -f $LOGBACK_FILE ]; then
    echo "Using $LOGBACK_FILE."
else
  echo "No $LOGBACK_FILE found. Copying logback-default.xml"
  cp config_override/logback-default.xml $LOGBACK_FILE
fi


if [[ $version == *SNAPSHOT* ]]; then
   echo Note: If the artifact version contains "SNAPSHOT", the latest snapshot version is downloaded, ignoring the version before SNAPSHOT.
   path="$snapshotRepo/$groupId/$artifactId"
   version=`curl $curlAuth -s "$path/maven-metadata.xml" | grep "<version>" | sed "s/.*<version>\([^<]*\)<\/version>.*/\1/" | tail -n 1`
   echo "Found version=$version from metadata $path/maven-metadata.xml"
   build=`curl $curlAuth -s "$path/$version/maven-metadata.xml" | grep '<value>' | head -1 | sed "s/.*<value>\([^<]*\)<\/value>.*/\1/"`
   jarfile="$artifactId-$build.jar"
   url="$path/$version/$jarfile"
else #A specific release version
   path="$releaseRepo/$groupId/$artifactId"
   url=$path/$version/$artifactId-$version.jar
   jarfile=$artifactId-$version.jar
fi

shaUrl=$url.sha1
shaFromWeb=$(wget $shaUrl -q -O -)
if [ -f $APP ]; then
  localSha=$(sha1sum $jarfile | awk '{print $1}')
else
  echo "No local app file found"
  localSha=-1
fi

if [ -z "$shaFromWeb" ]; then
    echo "Could not find version in remote repo! $version might be a non-existant version?"
    exit 1;
fi

if [ "$shaFromWeb" == "$localSha" ]; then
  runningApplication=$(basename $(readlink -f $artifactId.jar))
  if [ "$runningApplication" == "$jarfile" ]; then
    echo "Newest version is running. Not doing anything."
    exit 1;
  else
    echo "Got newest version locally, but it's not the one running. Updating symlink."
    create_or_replace_symlink
  fi
else #Download new version and update symlink
  echo Downloading $url
  wget -O $jarfile -q -N $url

  create_or_replace_symlink

  # Delete old jar files
  jar=$artifactId*.jar
  nrOfJarFilesToDelete=`ls $jar -A1t | tail -n +6 | wc -l`
  if [[ $nrOfJarFilesToDelete > 0 ]]; then
      echo Deleting $nrOfJarFilesToDelete old jar files. Keep the 4 newest + the symlink.
      ls $jar -A1t | tail -n +6 | xargs rm
  fi
fi