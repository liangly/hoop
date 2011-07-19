#!/bin/bash
#
# Copyright (c) 2010 Cloudera Inc. All rights reserved.
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License. See accompanying LICENSE file.
#

# Set Hoop specific environment variables here.

# Settings for the Embedded Tomcat that runs Hoop
# Java System properties for Hoop should be specified in this variable
#
# export CATALINA_OPTS=

# Hoop logs directory
#
# export HOOP_LOG=${HOOP_HOME}/logs

# Hoop temporary directory
#
# export HOOP_TEMP=${HOOP_HOME}/temp

# The HTTP port used by Hoop
#
# export HOOP_HTTP_PORT=14000

# The Admin port used by Hoop
#
# export HOOP_ADMIN_PORT=`expr ${HOOP_HTTP_PORT} + 1`

# The hostname Hoop server runs on
#
# export HOOP_HTTP_HOSTNAME=`hostname -f`
