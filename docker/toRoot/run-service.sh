#!/bin/bash

./update-service.sh
tail -f /var/log/cron.log
