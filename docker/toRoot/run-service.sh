#!/bin/bash

./update-service.sh
crontab $HOME/crontab
tail -f /var/log/cron.log
