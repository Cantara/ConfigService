FROM cantara/zulu
MAINTAINER Erik Drolshammer <erik-dev@fjas.no>

ENV USER=configservice
ENV HOME=/home/$USER
RUN useradd -d "$HOME" -u 500 -m -s /bin/bash $USER

RUN apt-get update && apt-get install -y --no-install-recommends \
		supervisor cron logrotate \
	&& rm -rf /var/lib/apt/lists/*

## Set up start of services
RUN mkdir -p /var/log/supervisor
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf

## Configure application
COPY application_scripts/* $HOME/
COPY CRON $HOME/
RUN chmod 755 $HOME/*.sh

##Directories to persist for data volumes
RUN mkdir $HOME/logs
RUN mkdir $HOME/db
COPY config_override_templates/* $HOME/config_override/

##Configure crontab
ADD crontab /etc/cron.d/configservice
RUN chmod 0644 /etc/cron.d/configservice
RUN touch /var/log/cron.log
RUN chown $USER /var/log/cron.log
RUN crontab -u $USER /etc/cron.d/configservice

##Configure logrotate
COPY logrotate_config/logrotate.conf /etc/logrotate.conf
COPY logrotate_config/logrotate.d/* /etc/logrotate.d/

#Permissions
RUN chown -R $USER:$USER $HOME

##Download application
RUN su - $USER -c "$HOME/update-service.sh"

VOLUME ["$HOME/config_override", "$HOME/db", "$HOME/logs"]

EXPOSE 8086
CMD /usr/bin/supervisord
