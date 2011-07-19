-----------------------------------------------------------------------------
Hoop, Hadoop HDFS over HTTP

Hoop is a server that provides a REST HTTP gateway to HDFS with full
read & write capabilities.

Hoop is distributed under Apache License 2.0.

Hoop can be used to transfer data between clusters running different
versions of Hadoop (overcoming RPC versioning issues), for example using
Hadoop DistCP.

Hoop can be used to access data in HDFS on a cluster behind of a firewall
(the Hoop server acts as a gateway and is the only system that is allowed
to cross the firewall into the cluster).

Hoop can be used to access data in HDFS using HTTP utilities (such as curl
and wget) and HTTP libraries Perl from other languages than Java.

-----------------------------------------------------------------------------
Documentation

  http://cloudera.github.com/hoop

-----------------------------------------------------------------------------
Getting started (download, build, install, configure, run):

  http://cloudera.github.com/hoop/docs/latest/ServerSetup.html

-----------------------------------------------------------------------------
Maven information

  Group Id: com.cloudera.hoop
  Artifact Id: hoop-client
  Available Versions: -
  Type: jar

  Repository: https://repository.cloudera.com/content/repositories/releases

-----------------------------------------------------------------------------
If you have any questions/issues, please send an email to:

  cdh-user@cloudera.org

-----------------------------------------------------------------------------
