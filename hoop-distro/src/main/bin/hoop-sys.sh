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


function print() {
  if [ "${HOOP_SILENT}" != "true" ]; then
    echo "$@"
  fi
}

# if HOOP_HOME is already set warn it will be ignored
#
if [ "${HOOP_HOME}" != "" ]; then
  echo "WARNING: current setting of HOOP_HOME ignored"
fi

print

# setting HOOP_HOME to the installation dir, it cannot be changed
#
export HOOP_HOME=${BASEDIR}
hoop_home=${HOOP_HOME}
print "Setting HOOP_HOME:          ${HOOP_HOME}"

# if the installation has a env file, source it
# this is for native packages installations
#
if [ -e "${HOOP_HOME}/bin/hoop-env.sh" ]; then
  print "Sourcing:                    ${HOOP_HOME}/bin/hoop-env.sh"
  source ${HOOP_HOME}/bin/HOOP-env.sh
  grep "^ *export " ${HOOP_HOME}/bin/hoop-env.sh | sed 's/ *export/  setting/'
fi

# verify that the sourced env file didn't change HOOP_HOME
# if so, warn and revert
#
if [ "${HOOP_HOME}" != "${hoop_home}" ]; then
  print "WARN: HOOP_HOME resetting to ''${HOOP_HOME}'' ignored"
  export HOOP_HOME=${hoop_home}
  print "  using HOOP_HOME:        ${HOOP_HOME}"
fi

if [ "${HOOP_CONFIG}" = "" ]; then
  export HOOP_CONFIG=${HOOP_HOME}/conf
  print "Setting HOOP_CONFIG:        ${HOOP_CONFIG}"
else
  print "Using   HOOP_CONFIG:        ${HOOP_CONFIG}"
fi
hoop_config=${HOOP_CONFIG}

# if the configuration dir has a env file, source it
#
if [ -e "${HOOP_CONFIG}/hoop-env.sh" ]; then
  print "Sourcing:                    ${HOOP_CONFIG}/hoop-env.sh"
  source ${HOOP_CONFIG}/hoop-env.sh
  grep "^ *export " ${HOOP_CONFIG}/hoop-env.sh | sed 's/ *export/  setting/'
fi

# verify that the sourced env file didn't change HOOP_HOME
# if so, warn and revert
#
if [ "${HOOP_HOME}" != "${hoop_home}" ]; then
  echo "WARN: HOOP_HOME resetting to ''${HOOP_HOME}'' ignored"
  export HOOP_HOME=${hoop_home}
fi

# verify that the sourced env file didn't change HOOP_CONFIG
# if so, warn and revert
#
if [ "${HOOP_CONFIG}" != "${hoop_config}" ]; then
  echo "WARN: HOOP_CONFIG resetting to ''${HOOP_CONFIG}'' ignored"
  export HOOP_CONFIG=${hoop_config}
fi

if [ "${HOOP_LOG}" = "" ]; then
  export HOOP_LOG=${HOOP_HOME}/logs
  print "Setting HOOP_LOG:           ${HOOP_LOG}"
else
  print "Using   HOOP_LOG:           ${HOOP_LOG}"
fi

if [ ! -f ${HOOP_LOG} ]; then
  mkdir -p ${HOOP_LOG}
fi

if [ "${HOOP_TEMP}" = "" ]; then
  export HOOP_TEMP=${HOOP_HOME}/temp
  print "Setting HOOP_TEMP:           ${HOOP_TEMP}"
else
  print "Using   HOOP_TEMP:           ${HOOP_TEMP}"
fi

if [ ! -f ${HOOP_TEMP} ]; then
  mkdir -p ${HOOP_TEMP}
fi

if [ "${HOOP_HTTP_PORT}" = "" ]; then
  export HOOP_HTTP_PORT=14000
  print "Setting HOOP_HTTP_PORT:     ${HOOP_HTTP_PORT}"
else
  print "Using   HOOP_HTTP_PORT:     ${HOOP_HTTP_PORT}"
fi

if [ "${HOOP_ADMIN_PORT}" = "" ]; then
  export HOOP_ADMIN_PORT=`expr $HOOP_HTTP_PORT +  1`
  print "Setting HOOP_ADMIN_PORT:     ${HOOP_ADMIN_PORT}"
else
  print "Using   HOOP_ADMIN_PORT:     ${HOOP_ADMIN_PORT}"
fi

if [ "${HOOP_HTTP_HOSTNAME}" = "" ]; then
  export HOOP_HTTP_HOSTNAME=`hostname -f`
  print "Setting HOOP_HTTP_HOSTNAME: ${HOOP_HTTP_HOSTNAME}"
else
  print "Using   HOOP_HTTP_HOSTNAME: ${HOOP_HTTP_HOSTNAME}"
fi

if [ "${CATALINA_BASE}" = "" ]; then
  export CATALINA_BASE=${HOOP_HOME}/tomcat
  print "Setting CATALINA_BASE:       ${CATALINA_BASE}"
else
  print "Using   CATALINA_BASE:       ${CATALINA_BASE}"
fi

if [ "${CATALINA_OUT}" = "" ]; then
  export CATALINA_OUT=${HOOP_LOG}/catalina.out
  print "Setting CATALINA_OUT:        ${CATALINA_OUT}"
else
  print "Using   CATALINA_OUT:        ${CATALINA_OUT}"
fi

if [ "${CATALINA_PID}" = "" ]; then
  export CATALINA_PID=${HOOP_HOME}/tomcat/temp/hoop.pid
  print "Setting CATALINA_PID:        ${CATALINA_PID}"
else
  print "Using   CATALINA_PID:        ${CATALINA_PID}"
fi

print
