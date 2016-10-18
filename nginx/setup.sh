#!/bin/bash

GU_KEYS="${HOME}/.gu/keys"
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NGINX_HOME=$(nginx -V 2>&1 | grep 'configure arguments:' | sed 's#.*conf-path=\([^ ]*\)/nginx\.conf.*#\1#g')

sudo mkdir -p ${NGINX_HOME}/sites-enabled
sudo ln -fs ${DIR}/contributions.conf ${NGINX_HOME}/sites-enabled/contributions.conf
aws s3 cp s3://identity-local-ssl/contribute-thegulocal-com-exp2017-07-19-bundle.crt ${GU_KEYS}/ --profile membership
aws s3 cp s3://identity-local-ssl/contribute-thegulocal-com-exp2017-07-19.key ${GU_KEYS}/ --profile membership

sudo ln -fs ${GU_KEYS}/ ${NGINX_HOME}/keys

sudo nginx -s stop
sudo nginx

