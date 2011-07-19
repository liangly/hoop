#!/bin/bash
#
#  Licensed to Cloudera, Inc. under one or more contributor license
#  agreements.  See the NOTICE file distributed with this work for
#  additional information regarding copyright ownership.  Cloudera,
#  Inc. licenses this file to you under the Apache License, Version
#  2.0 (the "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
# Copyright (c) 2010 Cloudera, inc.
#

# resolve links - $0 may be a softlink
PRG="${0}"

while [ -h "${PRG}" ]; do
  ls=`ls -ld "${PRG}"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "${PRG}"`/"$link"
  fi
done

BASEDIR=`dirname ${PRG}`
BASEDIR=`cd ${BASEDIR}/..;pwd`

source ${BASEDIR}/bin/hoop-sys.sh

# The Java System property 'hoop.http.port' it is not used by Hoop,
# it is used in Tomcat's server.xml configuration file
#
print "Using   CATALINA_OPTS:       ${CATALINA_OPTS}"

catalina_opts="-Dhoop.home.dir=${HOOP_HOME}";
catalina_opts="${catalina_opts} -Dhoop.config.dir=${HOOP_CONFIG}";
catalina_opts="${catalina_opts} -Dhoop.log.dir=${HOOP_LOG}";
catalina_opts="${catalina_opts} -Dhoop.temp.dir=${HOOP_TEMP}";
catalina_opts="${catalina_opts} -Dhoop.admin.port=${HOOP_ADMIN_PORT}";
catalina_opts="${catalina_opts} -Dhoop.http.port=${HOOP_HTTP_PORT}";
catalina_opts="${catalina_opts} -Dhoop.http.hostname=${HOOP_HTTP_HOSTNAME}";

print "Adding to CATALINA_OPTS:     ${catalina_opts}"

export CATALINA_OPTS="${CATALINA_OPTS} ${catalina_opts}"

${BASEDIR}/tomcat/bin/catalina.sh "$@"

