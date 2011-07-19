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
package com.cloudera.lib.server;

import com.cloudera.lib.lang.ClassUtils;
import com.cloudera.lib.util.Check;
import com.cloudera.lib.util.XConfiguration;
import org.apache.hadoop.conf.Configuration;
import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Server {
  private Logger log;

  public static final String CONF_SERVICES = "services";
  public static final String CONF_SERVICES_EXT = "services.ext";
  public static final String CONF_STARTUP_STATUS = "startup.status";

  public enum Status {
    UNDEF(false, false),
    BOOTING(false, true),
    HALTED(true, true),
    ADMIN(true, true),
    NORMAL(true, true),
    SHUTTING_DOWN(false, true),
    SHUTDOWN(false, false);

    private boolean settable;
    private boolean operational;

    Status(boolean settable, boolean operational) {
      this.settable = settable;
      this.operational = operational;
    }

    public boolean isOperational() {
      return operational;
    }
  }

  public static final String DEFAULT_LOG4J_PROPERTIES = "default-log4j.properties";

  private Status status;
  private String name;
  private String homeDir;
  private String configDir;
  private String logDir;
  private String tempDir;
  private XConfiguration config;
  private Map<Class, Service> services = new LinkedHashMap<Class, Service>();

  public Server(String name, String homeDir) {
    this(name, homeDir, null);
  }

  public Server(String name, String homeDir, String configDir, String logDir, String tempDir) {
    this(name, homeDir, configDir, logDir, tempDir, null);
  }

  public Server(String name, String homeDir, XConfiguration config) {
    this(name, homeDir, homeDir + "/conf", homeDir + "/log", homeDir + "/temp", config);
  }

  public Server(String name, String homeDir, String configDir, String logDir, String tempDir, XConfiguration config) {
    this.name = Check.notEmpty(name, "name").trim().toLowerCase();
    this.homeDir = Check.notEmpty(homeDir, "homeDir");
    this.configDir = Check.notEmpty(configDir, "configDir");
    this.logDir = Check.notEmpty(logDir, "logDir");
    this.tempDir = Check.notEmpty(tempDir, "tempDir");
    checkAbsolutePath(homeDir, "homeDir");
    checkAbsolutePath(configDir, "configDir");
    checkAbsolutePath(logDir, "logDir");
    checkAbsolutePath(tempDir, "tempDir");
    if (config != null) {
      this.config = new XConfiguration();
      XConfiguration.copy(config, this.config);
    }
    status = Status.UNDEF;
  }

  private String checkAbsolutePath(String value, String name) {
    if (!value.startsWith("/")) {
      throw new IllegalArgumentException(
        MessageFormat.format("[{0}] must be an absolute path [{1}]", name, value));
    }
    return value;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) throws ServerException {
    Check.notNull(status, "status");
    if (status.settable) {
      if (status != this.status) {
        Status oldStatus = this.status;
        this.status = status;
        for (Service service : services.values()) {
          try {
            service.serverStatusChange(oldStatus, status);
          }
          catch (Exception ex) {
            log.error("Service [{}] exception during status change to [{}] -server shutting down-,  {}",
                      new Object[]{service.getInterface().getSimpleName(), status, ex.getMessage(), ex});
            destroy();
            throw new ServerException(ServerException.ERROR.S11, service.getInterface().getSimpleName(),
                                      status, ex.getMessage(), ex);
          }
        }
      }
    }
    else {
      throw new IllegalArgumentException("Status [" + status + " is not settable");
    }
  }

  protected void ensureOperational() {
    if (!getStatus().isOperational()) {
      throw new IllegalStateException("Server is not running");
    }
  }

  public void init() throws ServerException {
    if (status != Status.UNDEF) {
      throw new IllegalStateException("Server already initialized");
    }
    status = Status.BOOTING;
    verifyDir(homeDir);
    verifyDir(tempDir);
    Properties serverInfo = new Properties();
    try {
      InputStream is = ClassUtils.getResource(name + ".properties");
      serverInfo.load(is);
      is.close();
    }
    catch (IOException ex) {
      throw new RuntimeException("Could not load server information file: " + name + ".properties");
    }
    initLog();
    log.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++");
    log.info("Server [{}] starting", name);
    log.info("  Built information:");
    log.info("    Version           : {}", serverInfo.getProperty(name + ".version", "undef"));
    log.info("    Source Repository : {}", serverInfo.getProperty(name + ".source.repository", "undef"));
    log.info("    Source Revision   : {}", serverInfo.getProperty(name + ".source.revision", "undef"));
    log.info("    Built by          : {}", serverInfo.getProperty(name + ".build.username", "undef"));
    log.info("    Built timestamp   : {}", serverInfo.getProperty(name + ".build.timestamp", "undef"));
    log.info("  Runtime information:");
    log.info("    Home   dir: {}", homeDir);
    log.info("    Config dir: {}", (config == null) ? configDir : "-");
    log.info("    Log    dir: {}", logDir);
    log.info("    Temp   dir: {}", tempDir);
    initConfig();
    log.debug("Loading services");
    List<Service> list = loadServices();
    try {
      log.debug("Initializing services");
      initServices(list);
      log.info("Services initialized");
    }
    catch (ServerException ex) {
      log.error("Services initialization failure, destroying initialized services");
      destroyServices();
      throw ex;
    }
    Status status = Status.valueOf(getConfig().get(getPrefixedName(CONF_STARTUP_STATUS), Status.NORMAL.toString()));
    setStatus(status);
    log.info("Server [{}] started!, status [{}]", name, status);
  }

  private void verifyDir(String dir) throws ServerException {
    File file = new File(dir);
    if (!file.exists()) {
      throw new ServerException(ServerException.ERROR.S01, dir);
    }
    if (!file.isDirectory()) {
      throw new ServerException(ServerException.ERROR.S02, dir);
    }
  }

  protected void initLog() throws ServerException {
    verifyDir(logDir);
    LogManager.resetConfiguration();
    File log4jFile = new File(configDir, name + "-log4j.properties");
    if (log4jFile.exists()) {
      PropertyConfigurator.configureAndWatch(log4jFile.toString(), 10 * 1000); //every 10 secs
      log = LoggerFactory.getLogger(Server.class);
    }
    else {
      Properties props = new Properties();
      try {
        InputStream is = ClassUtils.getResource(DEFAULT_LOG4J_PROPERTIES);
        props.load(is);
      }
      catch (IOException ex) {
        throw new ServerException(ServerException.ERROR.S03, DEFAULT_LOG4J_PROPERTIES, ex.getMessage(), ex);
      }
      PropertyConfigurator.configure(props);
      log = LoggerFactory.getLogger(Server.class);
      log.warn("Log4j [{}] configuration file not found, using default configuration from classpath", log4jFile);
    }
  }

  protected void initConfig() throws ServerException {
    verifyDir(configDir);
    File file = new File(configDir);
    Configuration defaultConf;
    String defaultConfig = name + "-default.xml";
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    InputStream inputStream = classLoader.getResourceAsStream(defaultConfig);
    if (inputStream == null) {
      log.warn("Default configuration file not available in classpath [{}]", defaultConfig);
      defaultConf = new XConfiguration();
    }
    else {
      try {
        defaultConf = new XConfiguration(inputStream);
      }
      catch (IOException ex) {
        throw new ServerException(ServerException.ERROR.S03, defaultConfig, ex.getMessage(), ex);
      }
    }

    if (config == null) {
      XConfiguration siteConf;
      File siteFile = new File(file, name + "-site.xml");
      if (!siteFile.exists()) {
        log.warn("Site configuration file [{}] not found in config directory", siteFile);
        siteConf = new XConfiguration();
      }
      else {
        if (!siteFile.isFile()) {
          throw new ServerException(ServerException.ERROR.S05, siteFile.getAbsolutePath());
        }
        try {
          log.debug("Loading site configuration from [{}]", siteFile);
          inputStream = new FileInputStream(siteFile);
          siteConf = new XConfiguration(inputStream);
        }
        catch (IOException ex) {
          throw new ServerException(ServerException.ERROR.S06, siteFile, ex.getMessage(), ex);
        }
      }

      config = new XConfiguration();
      XConfiguration.copy(siteConf, config);
    }

    XConfiguration.injectDefaults(defaultConf, config);

    for (String name : System.getProperties().stringPropertyNames()) {
      String value = System.getProperty(name);
      if (name.startsWith(getPrefix() + ".")) {
        config.set(name, value);
        if (name.endsWith(".password") || name.endsWith(".secret")) {
          value = "*MASKED*";
        }
        log.info("System property sets  {}: {}", name, value);
      }
    }

    log.debug("Loaded Configuration:");
    log.debug("------------------------------------------------------");
    for (Map.Entry<String, String> entry : config) {
      String name = entry.getKey();
      String value = config.get(entry.getKey());
      if (name.endsWith(".password") || name.endsWith(".secret")) {
        value = "*MASKED*";
      }
      log.debug("  {}: {}", entry.getKey(), value);
    }
    log.debug("------------------------------------------------------");
  }

  private void loadServices(Class[] classes, List<Service> list) throws ServerException {
    for (Class klass : classes) {
      try {
        Service service = (Service) klass.newInstance();
        log.debug("Loading service [{}] implementation [{}]", service.getInterface(),
                  service.getClass());
        if (!service.getInterface().isInstance(service)) {
          throw new ServerException(ServerException.ERROR.S04, klass, service.getInterface().getName());
        }
        list.add(service);
      }
      catch (ServerException ex) {
        throw ex;
      }
      catch (Exception ex) {
        throw new ServerException(ServerException.ERROR.S07, klass, ex.getMessage(), ex);
      }
    }
  }

  protected List<Service> loadServices() throws ServerException {
    try {
      Map<Class, Service> map = new LinkedHashMap<Class, Service>();
      Class[] classes = getConfig().getClasses(getPrefixedName(CONF_SERVICES));
      Class[] classesExt = getConfig().getClasses(getPrefixedName(CONF_SERVICES_EXT));
      List<Service> list = new ArrayList<Service>();
      loadServices(classes, list);
      loadServices(classesExt, list);

      //removing duplicate services, strategy: last one wins
      for (Service service : list) {
        if (map.containsKey(service.getInterface())) {
          log.debug("Replacing service [{}] implementation [{}]", service.getInterface(),
                    service.getClass());
        }
        map.put(service.getInterface(), service);
      }
      list = new ArrayList<Service>();
      for (Map.Entry<Class, Service> entry : map.entrySet()) {
        list.add(entry.getValue());
      }
      return list;
    }
    catch (RuntimeException ex) {
      throw new ServerException(ServerException.ERROR.S08, ex.getMessage(), ex);
    }
  }

  protected void initServices(List<Service> services) throws ServerException {
    for (Service service : services) {
      log.debug("Initializing service [{}]", service.getInterface());
      checkServiceDependencies(service);
      service.init(this);
      this.services.put(service.getInterface(), service);
    }
    for (Service service : services) {
      service.postInit();
    }
  }

  protected void checkServiceDependencies(Service service) throws ServerException {
    if (service.getServiceDependencies() != null) {
      for (Class dependency : service.getServiceDependencies()) {
        if (services.get(dependency) == null) {
          throw new ServerException(ServerException.ERROR.S10, service.getClass(), dependency);
        }
      }
    }
  }

  protected void destroyServices() {
    List<Service> list = new ArrayList<Service>(services.values());
    Collections.reverse(list);
    for (Service service : list) {
      try {
        log.debug("Destroying service [{}]", service.getInterface());
        service.destroy();
      }
      catch (Throwable ex) {
        log.error("Could not destroy service [{}], {}",
                  new Object[]{service.getInterface(), ex.getMessage(), ex});
      }
    }
    log.info("Services destroyed");
  }

  public void destroy() {
    ensureOperational();
    destroyServices();
    log.info("Server [{}] shutdown!", name);
    log.info("======================================================");
    if (!Boolean.getBoolean("test.circus")) {
      LogManager.shutdown();
    }
    status = Status.SHUTDOWN;
  }

  public String getName() {
    return name;
  }

  public String getPrefix() {
    return getName();
  }

  public String getPrefixedName(String name) {
    return getPrefix() + "." + Check.notEmpty(name, "name");
  }

  public String getHomeDir() {
    return homeDir;
  }

  public String getConfigDir() {
    return configDir;
  }

  public String getLogDir() {
    return logDir;
  }

  public String getTempDir() {
    return tempDir;
  }

  public XConfiguration getConfig() {
    return config;

  }

  @SuppressWarnings("unchecked")
  public <T> T get(Class<T> serviceKlass) {
    ensureOperational();
    Check.notNull(serviceKlass, "serviceKlass");
    return (T) services.get(serviceKlass);
  }

  public void setService(Class<? extends Service> klass) throws ServerException {
    ensureOperational();
    Check.notNull(klass, "serviceKlass");
    if (getStatus() == Status.SHUTTING_DOWN) {
      throw new IllegalStateException("Server shutting down");
    }
    try {
      Service newService = klass.newInstance();
      Service oldService = services.get(newService.getInterface());
      if (oldService != null) {
        try {
          oldService.destroy();
        }
        catch (Throwable ex) {
          log.error("Could not destroy service [{}], {}",
                    new Object[]{oldService.getInterface(), ex.getMessage(), ex});
        }
      }
      newService.init(this);
      services.put(newService.getInterface(), newService);
    }
    catch (Exception ex) {
      log.error("Could not set service [{}] programmatically -server shutting down-, {}", klass, ex);
      destroy();
      throw new ServerException(ServerException.ERROR.S09, klass, ex.getMessage(), ex);
    }
  }

}
