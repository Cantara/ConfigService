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
COPY toRoot/* $HOME/
RUN chown -R $USER:$USER $HOME
RUN chmod 755 $HOME/*.sh

RUN mkdir $HOME/db
COPY config_override/* $HOME/config_override/
RUN chown -R $USER:$USER $HOME

#Configure crontab
ADD crontab /etc/cron.d/configservice
RUN chmod 0644 /etc/cron.d/configservice
RUN touch /var/log/cron.log
RUN chown $USER /var/log/cron.log
RUN crontab -u $USER /etc/cron.d/configservice

#Configure logrotate
COPY logrotate/logrotate.conf /etc/logrotate.conf
COPY logrotate/logrotate.d/* /etc/logrotate.d/

#Download application
RUN su - $USER -c "$HOME/update-service.sh"

VOLUME ["$HOME/config_override", "$HOME/db", "$HOME/logs"]

EXPOSE 8086
CMD /usr/bin/supervisord
