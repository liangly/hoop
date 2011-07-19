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
package com.cloudera.hoop.client.fs;

import com.cloudera.alfredo.client.AuthenticatedURL;
import com.cloudera.alfredo.client.Authenticator;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PositionedReadable;
import org.apache.hadoop.fs.Seekable;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.util.Progressable;
import org.apache.hadoop.util.ReflectionUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Hoop implementation of the Hadoop FileSystem.
 * <p/>
 * This implementation allows a user to access HDFS over HTTP via a Hoop server.
 */
public class HoopFileSystem extends FileSystem {

  private AuthenticatedURL.Token authToken = new AuthenticatedURL.Token();
  private URI uri;
  private Path workingDir;
  private String doAs;

  /**
   * Convenience method that creates a <code>HttpURLConnection</code> for the Hoop file system operations.
   * <p/>
   * This methods performs and injects any needed authentication credentials.
   *
   * @param method the HTTP method.
   * @param params the query string parameters.
   * @param path the file path
   * @return a <code>HttpURLConnection</code> for the Hoop server, authenticated and ready to use for
   * the specified path and file system operation.
   * @throws IOException
   */
  private HttpURLConnection getConnection(String method, Map<String, String> params, Path path) throws IOException {
    params.put("doas", doAs);
    Class<? extends Authenticator> klass =
      getConf().getClass("hoop.authenticator.class", HoopKerberosAuthenticator.class, Authenticator.class);
    Authenticator authenticator = ReflectionUtils.newInstance(klass, getConf());
    try {
      StringBuilder sb = new StringBuilder();
      String separator = "?";
      for (Map.Entry<String, String> entry : params.entrySet()) {
        sb.append(separator).append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(),
                                                                                         "UTF8"));
        separator = "&";
      }
      path = makeQualified(path);
      URL url = new URL(path + sb.toString());
      HttpURLConnection conn = new AuthenticatedURL(authenticator).openConnection(url, authToken);
      conn.setRequestMethod(method);
      if (method.equals("POST") || method.equals("PUT")) {
        conn.setDoOutput(true);
      }
      return conn;
    }
    catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  /**
   * Convenience method that JSON Parses the <code>InputStream</code> of a <code>HttpURLConnection</code>.
   *
   * @param conn the <code>HttpURLConnection</code>.
   * @return the parsed JSON object.
   * @throws IOException thrown if the <code>InputStream</code> could not be JSON parsed.
   */
  private static Object jsonParse(HttpURLConnection conn) throws IOException {
    try {
      JSONParser parser = new JSONParser();
      return parser.parse(new InputStreamReader(conn.getInputStream()));
    }
    catch (ParseException ex) {
      throw new IOException("JSON parser error, " + ex.getMessage(), ex);
    }
  }

  /**
   * Validates the status of an <code>HttpURLConnection</code> against an expected HTTP
   * status code. If the current status code is not the expected one it throws an exception
   * with a detail message using Server side error messages if available.
   *
   * @param conn the <code>HttpURLConnection</code>.
   * @param expected the expected HTTP status code.
   * @throws IOException thrown if the current status code does not match the expected one.
   */
  private static void validateResponse(HttpURLConnection conn, int expected) throws IOException {
    int status = conn.getResponseCode();
    if (status != expected) {
      try {
        JSONObject json = (JSONObject) jsonParse(conn);
        throw new IOException(MessageFormat.format("HTTP status [{0}], {1} - {2}", json.get("status"),
                                                   json.get("reason"), json.get("message")));
      }
      catch (IOException ex) {
        if (ex.getCause() instanceof IOException) {
          throw (IOException) ex.getCause();
        }
        throw new IOException(MessageFormat.format("HTTP status [{0}], {1}", status, conn.getResponseMessage()));
      }
    }
  }

  /**
   * Called after a new FileSystem instance is constructed.
   *
   * @param name a uri whose authority section names the host, port, etc. for this FileSystem
   * @param conf the configuration
   */
  @Override
  public void initialize(URI name, Configuration conf) throws IOException {
    UserGroupInformation ugi = UserGroupInformation.getCurrentUser();
    doAs = ugi.getUserName();
    super.initialize(name, conf);
    try {
      uri = new URI(name.getScheme() + "://" + name.getHost() + ":" + name.getPort());
    }
    catch (URISyntaxException ex) {
      throw new IOException(ex);
    }
  }

  /**
   * Returns a URI whose scheme and authority identify this FileSystem.
   *
   * @return the URI whose scheme and authority identify this FileSystem.
   */
  @Override
  public URI getUri() {
    return uri;
  }

  /**
   * Hoop subclass of the <code>FSDataInputStream</code>.
   * <p/>
   * This implementation does not support the
   * <code>PositionReadable</code> and <code>Seekable</code> methods.
   */
  private static class HoopFSDataInputStream extends FilterInputStream implements Seekable, PositionedReadable {

    protected HoopFSDataInputStream(InputStream in, int bufferSize) {
      super(new BufferedInputStream(in, bufferSize));
    }

    @Override
    public int read(long position, byte[] buffer, int offset, int length) throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public void readFully(long position, byte[] buffer, int offset, int length) throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public void readFully(long position, byte[] buffer) throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public void seek(long pos) throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public long getPos() throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean seekToNewSource(long targetPos) throws IOException {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Opens an FSDataInputStream at the indicated Path.
   * </p>
   * IMPORTANT: the returned <code><FSDataInputStream/code> does not support the
   * <code>PositionReadable</code> and <code>Seekable</code> methods.
   * 
   * @param f the file name to open
   * @param bufferSize the size of the buffer to be used.
   */
  @Override
  public FSDataInputStream open(Path f, int bufferSize) throws IOException {
    Map<String, String> params = new HashMap<String, String>();
    HttpURLConnection conn = getConnection("GET", params, f);
    validateResponse(conn, HttpURLConnection.HTTP_OK);
    return new FSDataInputStream(new HoopFSDataInputStream(conn.getInputStream(), bufferSize));
  }

  /**
   * Hoop subclass of the <code>FSDataOutputStream</code>.
   * <p/>
   * This implementation closes the underlying HTTP connection validating the Http connection status
   * at closing time.
   */
  private static class HoopFSDataOutputStream extends FSDataOutputStream {
    private HttpURLConnection conn;
    private int closeStatus;

    public HoopFSDataOutputStream(HttpURLConnection conn, OutputStream out, int closeStatus, Statistics stats)
      throws IOException {
      super(out, stats);
      this.conn = conn;
      this.closeStatus = closeStatus;
    }

    @Override
    public void close() throws IOException {
      try {
        super.close();
      }
      finally {
        validateResponse(conn, closeStatus);
      }
    }

  }

  /**
   * Converts a <code>FsPermission</code> to a Unix string symbolic representation (ie: '-rwxr--r--')
   * @param p the permission.
   * @return the Unix string symbolic reprentation.
   */
  private String permissionToString(FsPermission p) {
    return (p == null) ? "default" : "-" + p.getUserAction().SYMBOL + p.getGroupAction().SYMBOL +
                                     p.getOtherAction().SYMBOL;
  }

  /**
   * Opens an FSDataOutputStream at the indicated Path with write-progress
   * reporting.
   * <p/>
   * IMPORTANT: The <code>Progressable</code> parameter is not used.
   *
   * @param f the file name to open
   * @param permission
   * @param overwrite if a file with this name already exists, then if true,
   *   the file will be overwritten, and if false an error will be thrown.
   * @param bufferSize the size of the buffer to be used.
   * @param replication required block replication for the file.
   * @param blockSize
   * @param progress
   * @throws IOException
   * @see #setPermission(Path, FsPermission)
   */  @Override
  public FSDataOutputStream create(Path f, FsPermission permission, boolean overwrite, int bufferSize,
                                   short replication, long blockSize, Progressable progress) throws IOException {
    Map<String, String> params = new HashMap<String, String>();
    params.put("op", "create");
    params.put("overwrite", Boolean.toString(overwrite));
    params.put("replication", Short.toString(replication));
    params.put("blocksize", Long.toString(blockSize));
    params.put("permission", permissionToString(permission));
    HttpURLConnection conn = getConnection("POST", params, f);
    try {
      OutputStream os = new BufferedOutputStream(conn.getOutputStream(), bufferSize);
      return new HoopFSDataOutputStream(conn, os, HttpURLConnection.HTTP_CREATED, statistics);
    }
    catch (IOException ex) {
      validateResponse(conn, HttpURLConnection.HTTP_CREATED);
      throw ex;
    }
  }


  /**
   * Append to an existing file (optional operation).
   * <p/>
   * IMPORTANT: The <code>Progressable</code> parameter is not used.
   *
   * @param f the existing file to be appended.
   * @param bufferSize the size of the buffer to be used.
   * @param progress for reporting progress if it is not null.
   * @throws IOException
   */
  @Override
  public FSDataOutputStream append(Path f, int bufferSize, Progressable progress) throws IOException {
    Map<String, String> params = new HashMap<String, String>();
    params.put("op", "append");
    HttpURLConnection conn = getConnection("PUT", params, f);
    try {
      OutputStream os = new BufferedOutputStream(conn.getOutputStream(), bufferSize);
      return new HoopFSDataOutputStream(conn, os, HttpURLConnection.HTTP_OK, statistics);
    }
    catch (IOException ex) {
      validateResponse(conn, HttpURLConnection.HTTP_OK);
      throw ex;
    }
  }

  /**
   * Renames Path src to Path dst.  Can take place on local fs
   * or remote DFS.
   */
  @Override
  public boolean rename(Path src, Path dst) throws IOException {
    Map<String, String> params = new HashMap<String, String>();
    params.put("op", "rename");
    params.put("to", dst.toString());
    HttpURLConnection conn = getConnection("PUT", params, src);
    validateResponse(conn, HttpURLConnection.HTTP_OK);
    JSONObject json = (JSONObject) jsonParse(conn);
    return (Boolean) json.get("rename");
  }

  /**
   * Delete a file.
   * @deprecated Use delete(Path, boolean) instead
   */
  @ Deprecated
  @Override
  public boolean delete(Path f) throws IOException {
    return delete(f, false);
  }

  /** Delete a file.
   *
   * @param f the path to delete.
   * @param recursive if path is a directory and set to
   * true, the directory is deleted else throws an exception. In
   * case of a file the recursive can be set to either true or false.
   * @return  true if delete is successful else false.
   * @throws IOException
   */
  @Override
  public boolean delete(Path f, boolean recursive) throws IOException {
    Map<String, String> params = new HashMap<String, String>();
    params.put("recursive", Boolean.toString(recursive));
    HttpURLConnection conn = getConnection("DELETE", params, f);
    validateResponse(conn, HttpURLConnection.HTTP_OK);
    JSONObject json = (JSONObject) jsonParse(conn);
    return (Boolean) json.get("delete");
  }

  /**
   * List the statuses of the files/directories in the given path if the path is
   * a directory.
   *
   * @param f
   *          given path
   * @return the statuses of the files/directories in the given patch
   * @throws IOException
   */
  @Override
  public FileStatus[] listStatus(Path f) throws IOException {
    Map<String, String> params = new HashMap<String, String>();
    params.put("op", "list");
    HttpURLConnection conn = getConnection("GET", params, f);
    validateResponse(conn, HttpURLConnection.HTTP_OK);
    JSONArray json = (JSONArray) jsonParse(conn);
    FileStatus[] array = new FileStatus[json.size()];
    for (int i = 0; i < json.size(); i++) {
      array[i] = createFileStatus((JSONObject) json.get(i));
    }
    return array;
  }

  /**
   * Set the current working directory for the given file system. All relative
   * paths will be resolved relative to it.
   *
   * @param new_dir
   */
  @Override
  public void setWorkingDirectory(Path new_dir) {
    workingDir = new_dir;
  }

  /**
   * Get the current working directory for the given file system
   * @return the directory pathname
   */
  @Override
  public Path getWorkingDirectory() {
    if (workingDir == null) {
      workingDir = getHomeDirectory();
    }
    return workingDir;
  }

  /**
   * Make the given file and all non-existent parents into
   * directories. Has the semantics of Unix 'mkdir -p'.
   * Existence of the directory hierarchy is not an error.
   */
  @Override
  public boolean mkdirs(Path f, FsPermission permission) throws IOException {
    Map<String, String> params = new HashMap<String, String>();
    params.put("op", "mkdirs");
    params.put("permission", permissionToString(permission));
    HttpURLConnection conn = getConnection("POST", params, f);
    validateResponse(conn, HttpURLConnection.HTTP_OK);
    JSONObject json = (JSONObject) jsonParse(conn);
    return (Boolean) json.get("mkdirs");
  }

  /**
   * Return a file status object that represents the path.
   *
   * @param f The path we want information from
   * @return a FileStatus object
   * @throws FileNotFoundException when the path does not exist;
   *         IOException see specific implementation
   */
  @Override
  public FileStatus getFileStatus(Path f) throws IOException {
    Map<String, String> params = new HashMap<String, String>();
    params.put("op", "status");
    HttpURLConnection conn = getConnection("GET", params, f);
    validateResponse(conn, HttpURLConnection.HTTP_OK);
    JSONObject json = (JSONObject) jsonParse(conn);
    return createFileStatus(json);
  }

  /**
   * Return the current user's home directory in this filesystem.
   * The default implementation returns "/user/$USER/".
   */
  @Override
  public Path getHomeDirectory() {
    Map<String, String> params = new HashMap<String, String>();
    params.put("op", "homedir");
    try {
      HttpURLConnection conn = getConnection("GET", params, new Path("/"));
      validateResponse(conn, HttpURLConnection.HTTP_OK);
      JSONObject json = (JSONObject) jsonParse(conn);
      return new Path((String) json.get("homeDir"));
    }
    catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Set owner of a path (i.e. a file or a directory).
   * The parameters username and groupname cannot both be null.
   *
   * @param p The path
   * @param username If it is null, the original username remains unchanged.
   * @param groupname If it is null, the original groupname remains unchanged.
   */
  @Override
  public void setOwner(Path p, String username, String groupname) throws IOException {
    Map<String, String> params = new HashMap<String, String>();
    params.put("op", "setowner");
    params.put("owner", username);
    params.put("group", groupname);
    HttpURLConnection conn = getConnection("PUT", params, p);
    validateResponse(conn, HttpURLConnection.HTTP_OK);
  }

  /**
   * Set permission of a path.
   *
   * @param p
   * @param permission
   */
  @Override
  public void setPermission(Path p, FsPermission permission) throws IOException {
    Map<String, String> params = new HashMap<String, String>();
    params.put("op", "setpermission");
    params.put("permission", permissionToString(permission));
    HttpURLConnection conn = getConnection("PUT", params, p);
    validateResponse(conn, HttpURLConnection.HTTP_OK);
  }

  /**
   * Set access time of a file
   *
   * @param p The path
   * @param mtime Set the modification time of this file.
   *              The number of milliseconds since Jan 1, 1970.
   *              A value of -1 means that this call should not set modification time.
   * @param atime Set the access time of this file.
   *              The number of milliseconds since Jan 1, 1970.
   *              A value of -1 means that this call should not set access time.
   */
  @Override
  public void setTimes(Path p, long mtime, long atime) throws IOException {
    Map<String, String> params = new HashMap<String, String>();
    params.put("op", "settimes");
    params.put("mtime", Long.toString(mtime));
    params.put("atime", Long.toString(atime));
    HttpURLConnection conn = getConnection("PUT", params, p);
    validateResponse(conn, HttpURLConnection.HTTP_OK);
  }

  /**
   * Set replication for an existing file.
   *
   * @param src file name
   * @param replication new replication
   * @throws IOException
   * @return true if successful;
   *         false if file does not exist or is a directory
   */
  @Override
  public boolean setReplication(Path src, short replication) throws IOException {
    Map<String, String> params = new HashMap<String, String>();
    params.put("op", "setreplication");
    params.put("replication", Short.toString(replication));
    HttpURLConnection conn = getConnection("PUT", params, src);
    validateResponse(conn, HttpURLConnection.HTTP_OK);
    JSONObject json = (JSONObject) jsonParse(conn);
    return (Boolean) json.get("setReplication");
  }

  /**
   * Creates a <code>FileStatus</code> object using a JSON file-status payload
   * received from a Hoop server.
   *
   * @param json a JSON file-status payload received from a Hoop server
   * @return the corresponding <code>FileStatus</code>
   */
  private FileStatus createFileStatus(JSONObject json) {
    Path path = new Path((String) json.get("path"));
    boolean isDir = (Boolean) json.get("isDir");
    long len = (Long) json.get("len");
    String owner = (String) json.get("owner");
    String group = (String) json.get("group");
    FsPermission permission = FsPermission.valueOf((String) json.get("permission"));
    long aTime = (Long) json.get("accessTime");
    long mTime = (Long) json.get("modificationTime");
    long blockSize = (Long) json.get("blockSize");
    short replication = (short) (long) (Long) json.get("replication");
    return new FileStatus(len, isDir, replication, blockSize, mTime, aTime, permission, owner, group, path);
  }

}
