FROM cantara/zulu
MAINTAINER Erik Drolshammer <erik-dev@fjas.no>

ENV USER=configservice
ENV HOME=/home/$USER
RUN useradd -d "$HOME" -u 500 -m -s /bin/bash $USER

RUN apt-get update && apt-get install -y --no-install-recommends \
		cron supervisor \
	&& rm -rf /var/lib/apt/lists/*

## Set up start of services
RUN mkdir -p /var/log/supervisor
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf

## Configure application
COPY toRoot/* $HOME/
RUN chown -R $USER:$USER $HOME
RUN chmod 755 $HOME/*.sh

ADD crontab /etc/cron.d/configservice
RUN chmod 0644 /etc/cron.d/configservice

RUN touch /var/log/cron.log
RUN chown $USER /var/log/cron.log
RUN crontab -u $USER /etc/cron.d/configservice

EXPOSE 8086
CMD /usr/bin/supervisord
