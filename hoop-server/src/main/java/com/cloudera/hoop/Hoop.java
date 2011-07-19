/*
 * Copyright (c) 2011, Cloudera, Inc. All Rights Reserved.
 *
 * Cloudera, Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the
 * License.
 */
package com.cloudera.hoop;

import com.cloudera.hoop.fs.FSAppend;
import com.cloudera.hoop.fs.FSCreate;
import com.cloudera.hoop.fs.FSDelete;
import com.cloudera.hoop.fs.FSFileStatus;
import com.cloudera.hoop.fs.FSHomeDir;
import com.cloudera.hoop.fs.FSListStatus;
import com.cloudera.hoop.fs.FSMkdirs;
import com.cloudera.hoop.fs.FSOpen;
import com.cloudera.hoop.fs.FSRename;
import com.cloudera.hoop.fs.FSSetOwner;
import com.cloudera.hoop.fs.FSSetPermission;
import com.cloudera.hoop.fs.FSSetReplication;
import com.cloudera.hoop.fs.FSSetTimes;
import com.cloudera.lib.service.Groups;
import com.cloudera.lib.service.Hadoop;
import com.cloudera.lib.service.HadoopException;
import com.cloudera.lib.service.Instrumentation;
import com.cloudera.lib.service.ProxyUser;
import com.cloudera.lib.servlet.FileSystemReleaseFilter;
import com.cloudera.lib.servlet.HostnameFilter;
import com.cloudera.lib.wsrs.InputStreamEntity;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.AccessControlException;
import java.security.Principal;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

@Path("")
public class Hoop {
  private static Logger AUDIT_LOG = LoggerFactory.getLogger("hoopaudit");

  @GET
  @Path("favicon.ico")
  @Produces(MediaType.TEXT_PLAIN)
  public String faviconTrap() {
    return "";
  }

  @GET
  @Path("/")
  @Produces(MediaType.APPLICATION_JSON)
  public Response root(@Context Principal user,
                       @QueryParam(GetOpParam.NAME) @DefaultValue(GetOpParam.DEFAULT) GetOpParam get,
                       @QueryParam(FilterParam.NAME) @DefaultValue(FilterParam.DEFAULT) FilterParam filter,
                       @QueryParam(DoAsParam.NAME) @DefaultValue(DoAsParam.DEFAULT) DoAsParam doAs)
    throws IOException, HadoopException {
    return get(user, new FsPathParam(""), get, new OffsetParam(OffsetParam.DEFAULT),
               new LenParam(LenParam.DEFAULT), filter, doAs);
  }

  private String getEffectiveUser(Principal user, String doAs) throws IOException {
    String effectiveUser = user.getName();
    if (doAs != null && !doAs.equals(user.getName())) {
      ProxyUser proxyUser = HoopServer.get().get(ProxyUser.class);
      proxyUser.validate(user.getName(), HostnameFilter.get(), doAs);
      effectiveUser = doAs;
      AUDIT_LOG.info("Proxy user [{}] DoAs user [{}]", user.getName(), doAs);
    }
    return effectiveUser;
  }

  private <T> T fsExecute(Principal user, String doAs, Hadoop.FileSystemExecutor<T> executor)
    throws IOException, HadoopException {
    String hadoopUser = getEffectiveUser(user, doAs);
    Hadoop hadoop = HoopServer.get().get(Hadoop.class);
    Configuration conf = HoopServer.get().get(Hadoop.class).getDefaultConfiguration();
    return hadoop.execute(hadoopUser, conf, executor);
  }

  private FileSystem createFileSystem(Principal user, String doAs) throws IOException, HadoopException {
    String hadoopUser = getEffectiveUser(user, doAs);
    Hadoop hadoop = HoopServer.get().get(Hadoop.class);
    Configuration conf = HoopServer.get().get(Hadoop.class).getDefaultConfiguration();
    FileSystem fs = hadoop.createFileSystem(hadoopUser, conf);
    FileSystemReleaseFilter.setFileSystem(fs);
    return fs;
  }

  @GET
  @Path("{path:.*}")
  @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
  public Response get(@Context Principal user,
                      @PathParam("path") @DefaultValue("") FsPathParam path,
                      @QueryParam(GetOpParam.NAME) @DefaultValue(GetOpParam.DEFAULT) GetOpParam op,
                      @QueryParam(OffsetParam.NAME) @DefaultValue(OffsetParam.DEFAULT) OffsetParam offset,
                      @QueryParam(LenParam.NAME) @DefaultValue(LenParam.DEFAULT) LenParam len,
                      @QueryParam(FilterParam.NAME) @DefaultValue(FilterParam.DEFAULT) FilterParam filter,
                      @QueryParam(DoAsParam.NAME) @DefaultValue(DoAsParam.DEFAULT) DoAsParam doAs)
    throws IOException, HadoopException {
    Response response = null;
    path.makeAbsolute();
    MDC.put("op", op.value().name());
    switch (op.value()) {
      case DATA: {
        //Invoking the command directly using an unmanaged FileSystem that is released by the
        //FileSystemReleaseFilter
        FSOpen command = new FSOpen(path.value());
        FileSystem fs = createFileSystem(user, doAs.value());
        InputStream is = command.execute(fs);
        AUDIT_LOG.info("[{}] offset [{}] len [{}]", new Object[]{path, offset, len});
        InputStreamEntity entity = new InputStreamEntity(is, offset.value(), len.value());
        response = Response.ok(entity).type(MediaType.APPLICATION_OCTET_STREAM).build();
        break;
      }
      case STATUS: {
        FSFileStatus command = new FSFileStatus(path.value());
        Map json = fsExecute(user, doAs.value(), command);
        AUDIT_LOG.info("[{}]", path);
        response = Response.ok(json).type(MediaType.APPLICATION_JSON).build();
        break;
      }
      case LIST: {
        FSListStatus command = new FSListStatus(path.value(), filter.value());
        JSONArray json = fsExecute(user, doAs.value(), command);
        if (filter.value() == null) {
          AUDIT_LOG.info("[{}]", path);
        }
        else {
          AUDIT_LOG.info("[{}] filter [{}]", path, filter.value());
        }
        response = Response.ok(json).type(MediaType.APPLICATION_JSON).build();
        break;
      }
      case HOMEDIR: {
        FSHomeDir command = new FSHomeDir();
        JSONObject json = fsExecute(user, doAs.value(), command);
        AUDIT_LOG.info("");
        response = Response.ok(json).type(MediaType.APPLICATION_JSON).build();
        break;
      }
      case INSTRUMENTATION: {
        if (!path.value().equals("/")) {
          throw new UnsupportedOperationException(
            MessageFormat.format("Invalid path for {0}={1}, must be '/'",
                                 GetOpParam.NAME, GetOpParam.Values.INSTRUMENTATION));
        }
        Groups groups = HoopServer.get().get(Groups.class);
        List<String> userGroups = groups.getGroups(user.getName());
        if (!userGroups.contains(HoopServer.get().getAdminGroup())) {
          throw new AccessControlException("User not in Hoop admin group");
        }
        Instrumentation instrumentation = HoopServer.get().get(Instrumentation.class);
        Map snapshot = instrumentation.getSnapshot();
        response = Response.ok(snapshot).build();
        break;
      }
    }
    return response;
  }

  @DELETE
  @Path("{path:.*}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response delete(@Context Principal user,
                         @PathParam("path") FsPathParam path,
                         @QueryParam(DeleteRecursiveParam.NAME) @DefaultValue(DeleteRecursiveParam.DEFAULT)
                         DeleteRecursiveParam recursive,
                         @QueryParam(DoAsParam.NAME) @DefaultValue(DoAsParam.DEFAULT) DoAsParam doAs)
    throws IOException, HadoopException {
    path.makeAbsolute();
    MDC.put("op", "DELETE");
    AUDIT_LOG.info("[{}] recursive [{}]", path, recursive);
    FSDelete command = new FSDelete(path.value(), recursive.value());
    JSONObject json = fsExecute(user, doAs.value(), command);
    return Response.ok(json).type(MediaType.APPLICATION_JSON).build();
  }


  @PUT
  @Path("{path:.*}")
  @Consumes({"*/*"})
  @Produces({MediaType.APPLICATION_JSON})
  public Response put(InputStream is,
                      @Context Principal user,
                      @PathParam("path") FsPathParam path,
                      @QueryParam(PutOpParam.NAME) PutOpParam op,
                      @QueryParam(ToPathParam.NAME) @DefaultValue(ToPathParam.DEFAULT) ToPathParam toPath,
                      @QueryParam(OwnerParam.NAME) @DefaultValue(OwnerParam.DEFAULT) OwnerParam owner,
                      @QueryParam(GroupParam.NAME) @DefaultValue(GroupParam.DEFAULT) GroupParam group,
                      @QueryParam(PermissionParam.NAME) @DefaultValue(PermissionParam.DEFAULT)
                      PermissionParam permission,
                      @QueryParam(ReplicationParam.NAME) @DefaultValue(ReplicationParam.DEFAULT)
                      ReplicationParam replication,
                      @QueryParam(ModifiedTimeParam.NAME) @DefaultValue(ModifiedTimeParam.DEFAULT)
                      ModifiedTimeParam modifiedTime,
                      @QueryParam(AccessTimeParam.NAME) @DefaultValue(AccessTimeParam.DEFAULT)
                      AccessTimeParam accessTime,
                      @QueryParam(DoAsParam.NAME) @DefaultValue(DoAsParam.DEFAULT) DoAsParam doAs)
    throws IOException, HadoopException {
    Response response = null;
    path.makeAbsolute();
    if (op == null) {
      throw new UnsupportedOperationException(MessageFormat.format("Missing [{0}] parameter", PutOpParam.NAME));
    }
    MDC.put("op", op.value().name());
    switch (op.value()) {
      case APPEND: {
        FSAppend command = new FSAppend(is, path.value());
        fsExecute(user, doAs.value(), command);
        AUDIT_LOG.info("[{}]", path);
        response = Response.ok().type(MediaType.APPLICATION_JSON).build();
        break;
      }
      case RENAME: {
        FSRename command = new FSRename(path.value(), toPath.value());
        JSONObject json = fsExecute(user, doAs.value(), command);
        AUDIT_LOG.info("[{}] to [{}]", path, toPath);
        response = Response.ok(json).type(MediaType.APPLICATION_JSON).build();
        break;
      }
      case SETOWNER: {
        FSSetOwner command = new FSSetOwner(path.value(), owner.value(), group.value());
        fsExecute(user, doAs.value(), command);
        AUDIT_LOG.info("[{}] to (O/G)[{}]", path, owner.value() + ":" + group.value());
        response = Response.ok().build();
        break;
      }
      case SETPERMISSION: {
        FSSetPermission command = new FSSetPermission(path.value(), permission.value());
        fsExecute(user, doAs.value(), command);
        AUDIT_LOG.info("[{}] to [{}]", path, permission.value());
        response = Response.ok().build();
        break;
      }
      case SETREPLICATION: {
        FSSetReplication command = new FSSetReplication(path.value(), replication.value());
        JSONObject json = fsExecute(user, doAs.value(), command);
        AUDIT_LOG.info("[{}] to [{}]", path, replication.value());
        response = Response.ok(json).build();
        break;
      }
      case SETTIMES: {
        FSSetTimes command = new FSSetTimes(path.value(), modifiedTime.value(), accessTime.value());
        fsExecute(user, doAs.value(), command);
        AUDIT_LOG.info("[{}] to (M/A)[{}]", path, modifiedTime.value() + ":" + accessTime.value());
        response = Response.ok().build();
        break;
      }
    }
    return response;
  }


  @POST
  @Path("{path:.*}")
  @Consumes({"*/*"})
  @Produces({MediaType.APPLICATION_JSON})
  public Response post(InputStream is,
                       @Context Principal user,
                       @PathParam("path") FsPathParam path,
                       @QueryParam(PostOpParam.NAME) @DefaultValue(PostOpParam.DEFAULT) PostOpParam op,
                       @QueryParam(OverwriteParam.NAME) @DefaultValue(OverwriteParam.DEFAULT)
                       OverwriteParam override,
                       @QueryParam(ReplicationParam.NAME) @DefaultValue(ReplicationParam.DEFAULT)
                       ReplicationParam replication,
                       @QueryParam(BlockSizeParam.NAME) @DefaultValue(BlockSizeParam.DEFAULT)
                       BlockSizeParam blockSize,
                       @QueryParam(PermissionParam.NAME) @DefaultValue(PermissionParam.DEFAULT)
                       PermissionParam permission,
                       @QueryParam(DoAsParam.NAME) @DefaultValue(DoAsParam.DEFAULT) DoAsParam doAs)
    throws IOException, HadoopException {
    Response response = null;
    path.makeAbsolute();
    MDC.put("op", op.value().name());
    switch (op.value()) {
      case CREATE: {
        FSCreate command = new FSCreate(is, path.value(), permission.value(), override.value(),
                                        replication.value(), blockSize.value());
        URI uri = fsExecute(user, doAs.value(), command);
        AUDIT_LOG.info("[{}] permission [{}] override [{}] replication [{}] blockSize [{}]",
                       new Object[]{path, permission, override, replication, blockSize});
        response = Response.created(uri).type(MediaType.APPLICATION_JSON).build();
        break;
      }
      case MKDIRS: {
        FSMkdirs command = new FSMkdirs(path.value(), permission.value());
        JSONObject json = fsExecute(user, doAs.value(), command);
        AUDIT_LOG.info("[{}] permission [{}]", path, permission.value());
        response = Response.ok(json).type(MediaType.APPLICATION_JSON).build();
        break;
      }
    }
    return response;
  }

}
